# Kế hoạch chi tiết cân bằng boss, skill và set giáp

## Phạm vi và quy ước

- Bao gồm 13 boss. Điều chỉnh HP, chiến đấu/skill và set giáp. Giữ nguyên drops thường, `level-drops`, tỷ lệ rơi và token lính. Ender Dragon hiện tắt và sẽ giữ nguyên `enabled: false`.
- Damage trong bảng là điểm HP thô trước giảm trừ giáp (2 điểm = 1 tim). Nội suy tuyến tính mỗi cấp từ Lv1 đến Lv10. Không hit đơn lẻ nào vượt 8 HP (4 tim) và tổng damage trực tiếp mỗi cast nhiều hit/projectile không quá 12 HP; hiệu ứng damage-over-time phải có thời lượng/cooldown giới hạn.
- Token lính vẫn là class đã khai báo cho từng boss; bậc/token level tiếp tục khớp level boss như cơ chế hiện tại.
- HP đặt riêng cho từng boss để không lặp cùng một đường cong: Lv1 khoảng 900–1.700, Lv7 khoảng 9.100–11.800 (quanh mục tiêu 10.000), Lv10 khoảng 17.700–20.000. Boss thủ lĩnh tăng cao hơn; boss nhẹ có Lv10 thấp hơn một chút.
- Mỗi món có tên riêng kèm cấp và lore 4 dòng như dưới; không dùng `§0` hoặc màu xám quá tối `§8` trên set. Bonus 2/3/4 món là mốc tổng đang hoạt động, không cộng trùng. Nhận diện qua PDC/tag riêng, không dựa vào tên/lore; cập nhật/gỡ khi đổi trang bị, login, chết/hồi sinh và reload.
- Enchant theo cấp: Lv1 có 2 dòng bậc I; Lv2 mở dòng 3 bậc I; Lv3 mở dòng 4 bậc I; trần là 4 dòng/món. Chestplate và boots dùng enchant đầu tiên trong danh sách slot làm enchant chủ đạo: Lv4/5/6/7 lần lượt lên II/III/IV/V (V được phép vượt max vanilla); Lv8/9/10 lần lượt nâng enchant kế tiếp theo danh sách slot thêm một bậc mỗi level đến max vanilla hoặc V. Helmet/leggings và các dòng phụ nâng theo max vanilla. Không vượt 4 dòng và không ghép enchant bảo vệ loại trừ nhau. Mọi trần/công tắc vượt vanilla phải configurable, có mặc định và chú thích song ngữ.
- Tỷ lệ rơi set đổi thành 10% độc lập cho từng món; không đổi tỷ lệ của drop thường/rare/token. Hiệu ứng có giới hạn, không bất tử/miễn nhiễm tuyệt đối.

## HP: bảng riêng từng boss theo 10 cấp

Các số lần lượt là Lv1 → Lv10; cột `hp` mặc định của boss bằng giá trị Lv1. Profile mạnh hơn có Lv7 quanh/vượt 10.000; profile nhẹ có Lv10 thấp hơn 20.000 để giữ khác biệt:

| Boss | HP Lv1, Lv2, Lv3, Lv4, Lv5, Lv6, Lv7, Lv8, Lv9, Lv10 |
|---|---|
| Creeper | 1.200, 2.500, 4.000, 5.500, 7.000, 8.500, 10.000, 13.000, 16.100, 19.200 |
| Ender Dragon (disabled) | 1.700, 3.300, 5.000, 6.700, 8.400, 10.100, 11.800, 15.100, 18.400, 20.000 |
| Enderman | 1.150, 2.450, 3.950, 5.450, 6.950, 8.450, 9.950, 12.900, 16.000, 19.000 |
| Ghast | 1.300, 2.700, 4.300, 5.900, 7.500, 9.100, 10.700, 13.900, 17.100, 20.000 |
| Giant | 1.500, 2.900, 4.400, 5.900, 7.400, 8.900, 10.400, 13.600, 16.800, 20.000 |
| Phantom | 1.050, 2.300, 3.700, 5.100, 6.500, 7.900, 9.300, 12.200, 15.200, 18.300 |
| Ravager | 1.400, 2.850, 4.500, 6.150, 7.800, 9.450, 11.100, 14.300, 17.600, 20.000 |
| Skeleton | 1.000, 2.200, 3.600, 5.000, 6.400, 7.800, 9.200, 12.000, 15.000, 18.000 |
| Spider | 900, 2.100, 3.500, 4.900, 6.300, 7.700, 9.100, 11.900, 14.800, 17.700 |
| Vindicator | 1.250, 2.600, 4.150, 5.700, 7.250, 8.800, 10.350, 13.500, 16.800, 19.800 |
| Witch | 1.150, 2.500, 4.000, 5.500, 7.000, 8.500, 9.900, 13.000, 16.200, 19.500 |
| Wither | 1.600, 3.100, 4.700, 6.300, 7.900, 9.500, 11.100, 14.300, 17.600, 20.000 |
| Zombie | 1.100, 2.400, 3.900, 5.400, 6.900, 8.400, 9.800, 12.800, 15.900, 19.000 |

Phải bỏ cap `MAX_BOSS_HEALTH = 1024`, xác minh giới hạn max-health của entity/Paper rồi mới đặt current health. Kiểm tra boss bar, phase, sát thương đóng góp/thưởng và xử lý HP cấu hình sai; nếu runtime không thể áp dụng thì log lỗi rõ ràng, không cap im lặng.

## Hồ sơ chiến đấu và loot hiện tại (loot giữ nguyên)

Mỗi hàng damage là đòn thường và các skill chính theo thứ tự Lv1 → Lv10; dùng nội suy tuyến tính mỗi level. Với volley, số ghi sau dấu `/` là giới hạn tổng damage cả cast. Skill nguy hiểm có cảnh báo/đường né.

### Loot theo level hiện có — giữ nguyên toàn bộ

Mỗi boss hiện có hai vật phẩm cấp trong mỗi level: di vật chính (65%) và sách/mật lục (30%); level 1–10, cùng enchant/lore/cấp vật liệu đang cấu hình không bị sửa. Vật liệu di vật chính theo mốc:

| Boss | Di vật chính hiện có theo level | Sách đi kèm |
|---|---|---|
| Creeper | Kiếm Lôi Tâm: Iron Sword L1–2, Diamond Sword L3–5, Netherite Sword L6–10 | Mật Lục Lôi Tâm / Lôi Tâm Hủy Diệt |
| Ender Dragon | Long Kiếm Cổ: Iron Sword L1–2, Diamond Sword L3–5, Netherite Sword L6–10 | Mật Lục Long Kiếm / Long Kiếm Hư Không |
| Enderman | Cuốc Mắt End: Iron Pickaxe L1–2, Diamond Pickaxe L3–5, Netherite Pickaxe L6–10 | Ghi Chép / Mật Lục Mắt Tận Cùng |
| Ghast | Cung Ghast/Khúc Cung Than Khóc: Bow L1–7; L8–10 thay di vật chính bằng Enchanted Book | Sổ Ghast / Mật Lục Khúc Cung |
| Giant | Rìu Địa Mạch: Iron Axe L1–2, Diamond Axe L3–5, Netherite Axe L6–10 | Sổ/Mật Lục Địa Mạch |
| Phantom | Cung Đêm/Mộng Ảnh Vĩnh Dạ: Bow L1–7; L8–10 thay di vật chính bằng Enchanted Book | Sổ/Mật Lục Cung Đêm |
| Ravager | Rìu Chiến Trường: Iron Axe L1–2, Diamond Axe L3–5, Netherite Axe L6–10 | Sổ/Mật Lục Chiến Trường |
| Skeleton | Cung Lửa Vạn Cốt/Cung Xương/Vạn Cốt Câm Lặng: Bow L1–7; L8–10 thay di vật chính bằng Enchanted Book | Sổ/Mật Lục Vạn Cốt |
| Spider | Ủng Tơ Nhện/Tơ Độc Hoàng Hôn: Iron Boots L1–2, Diamond Boots L3–5, Netherite Boots L6–10 (đây là loot level riêng, không tự đổi thành set) | Sổ/Mật Lục Tơ Nhện |
| Vindicator | Rìu Đỏ/Phán Quyết Đỏ: Iron Axe L1–2, Diamond Axe L3–5, Netherite Axe L6–10 | Sổ/Mật Lục Rìu Phán Quyết |
| Witch | Dược Thư/Dược Thư Trăng Máu: Enchanted Book ở cả hai slot mỗi level | Hai sách riêng theo từng level |
| Wither | Kiếm Wither/Lưỡi Hái Ba Đầu Hư Vô: Iron Sword L1–2, Diamond Sword L3–5, Netherite Sword L6–10 | Sổ/Mật Lục Wither |
| Zombie | Kiếm Mộ Sâu/Lưỡi Hái Mộ Sâu: Iron Sword L1–2, Diamond Sword L3–5, Netherite Sword L6–10 | Sổ/Mật Lục Mộ Sâu |

Các vật phẩm trên là loot quý theo level chứ không phải armor-set, nên giữ nguyên cả vật liệu, tên, enchant, lore, số lượng và tỷ lệ. Các danh sách “Drops giữ” ở dưới là drop thường; armor-set rơi 10% độc lập cho mỗi mảnh. Theo xác suất độc lập, cơ hội nhận cả bốn món trong một lần hạ boss là 0,01%; đây là trade-off của mức 10%/món.

### Creeper Boss — `creeper_boss`
- **Combat:** melee 4→7; Charged Explosion 6→12 tổng/cast, mỗi hit không quá 8 (không cộng nổ thường để one-shot); báo trước 2 giây, phạm vi damage tối đa 4 block, cooldown ≥20 giây. Kiểm tra nguồn explosion và knockback.
- **Drops giữ:** Gunpowder 20–50 (100%), Diamond 4–8 (40%), Iron Ingot 10–20 (60%), Emerald 8–16 (50%), TNT 2–6 (50%), Enchanted Book 1–2 (25%), Firework Rocket 5–15 (40%), Creeper Head 1 (15%). **Token:** Berserker.
- **Set – Giáp Lôi Tâm (10% mỗi món):** Mũ Tâm Chấn / Áo Lôi Tâm / Quần Lôi Tâm / Ủng Lôi Tâm. Enchant ưu tiên mũ Blast Protection, Unbreaking, Respiration, Aqua Affinity; áo Blast Protection, Unbreaking, Thorns; quần Blast Protection, Swift Sneak, Unbreaking; ủng Blast Protection, Feather Falling, Depth Strider, Unbreaking.
- **Lore:** “§7Tôi dưới tiếng sét Creeper.” / “§7Tơ thép tản chấn nổ.” / “§7Lõi xanh giữ vững bước chân.” / “§eLôi Tâm không khuất phục.”
- **Bonus:** 2 món giảm 8% damage nổ; 3 món giảm 14% và knockback nổ 15%; 4 món giảm tổng 20% damage nổ.

### Ender Dragon — `ender_dragon` (giữ `enabled: false`)
- **Combat:** melee 6→8; Fireball Volley 4→8 mỗi cầu, 2–3 cầu/cast, tổng không quá 12; báo trước hướng/vùng impact ít nhất 1 giây. Không kích hoạt boss.
- **Drops giữ:** Dragon Egg 1 (15%), Elytra 1 (35%), Diamond 8–15 (80%), Emerald 15–25 (70%), Iron Ingot 20–40 (70%), Shulker Shell 2–4 (60%), Enchanted Book 1–3 (40%), End Crystal 1–3 (30%), Chorus Fruit 5–15 (50%), Dragon Breath 1–3 (30%). **Token:** Paladin.
- **Set – Giáp Long Vảy (10% mỗi món):** Mũ Long Vảy / Áo Long Vảy / Quần Long Vảy / Ủng Long Vảy. Mũ Fire Protection, Respiration, Unbreaking; áo Fire Protection, Unbreaking, Thorns; quần Fire Protection, Swift Sneak, Unbreaking; ủng Fire Protection, Feather Falling, Soul Speed, Unbreaking.
- **Lore:** “§7Vảy rồng tôi giữa long viêm.” / “§7Lớp giáp giữ nhiệt End.” / “§7Dấu cánh khép đường nứt.” / “§eLong Vảy che chở kẻ chinh phục.”
- **Bonus:** 2 món giảm 8% damage lửa; 3 món giảm 14% và thời gian cháy 20%; 4 món giảm tổng 20%, không cấp Fire Resistance vĩnh viễn.

### Enderman Boss — `enderman_boss`
- **Combat:** melee 4→8; Void Snatch 3→6/cast, chỉ trong 12 block, LOS (tầm nhìn) khi bắt đầu cast, dịch chuyển ≤4 block, slow nhẹ ≤1 giây và không cast nối tiếp lên cùng mục tiêu. End Rift không gây damage, warning 30→40 tick và có đường thoát. Bỏ đuổi khi mất LOS 5 giây hoặc xa điểm neo 30 block; không teleport bám theo vô hạn.
- **Drops giữ:** Ender Pearl 8–20 (90%), Diamond 4–8 (40%), Iron Ingot 10–18 (50%), Emerald 8–15 (50%), Obsidian 5–15 (60%), Ender Chest 1 (20%), Enchanted Book 1 (25%), Chorus Fruit 5–12 (40%). **Token:** Ranger.
- **Set – Giáp Mắt Tận Cùng (10% mỗi món):** Mũ Mắt Tím / Áo Khe Nứt / Quần Bước Không Gian / Ủng Lối End. Mũ Protection, Respiration, Unbreaking; áo Protection, Unbreaking, Thorns; quần Protection, Swift Sneak, Unbreaking; ủng Protection, Feather Falling, Soul Speed, Depth Strider.
- **Lore:** “§7Dệt từ tàn tích cổng End.” / “§7Mắt tím soi khe dịch chuyển.” / “§7Bước chân tìm đường trở lại.” / “§eKhông gian không thể giam cầm chủ nhân.”
- **Bonus:** 2 món giảm 10% sát thương rơi; 3 món giảm 15% thời lượng Slowness/Weakness; 4 món giảm 20% damage của hit kế tiếp sau khi nhận đòn lớn (cooldown 30 giây), không teleport/tăng tốc vô hạn.

### Ghast Khổng Lồ — `ghast`
- **Combat:** melee 3→5; Fireball 4→7 mỗi quả, tối đa 2–3 quả và 12 damage/cast; split còn ≤3 quả con, mỗi quả 2→3, tổng tính chung. Fiery Drag kéo ≤2 giây, terrain magma/lửa ≤3 giây, báo hiệu vị trí.
- **Drops giữ:** Ghast Tear 3–8 (80%), Magma Cream 3–6 (70%), Diamond 3–6 (40%), Iron Ingot 8–15 (50%), Emerald 5–10 (40%), Gunpowder 10–20 (60%), Enchanted Book 1 (25%), Fire Charge 2–5 (40%), Blaze Rod 2–5 (35%). **Token:** Pyromancer.
- **Set – Giáp Tro Than Khóc (10% mỗi món):** Mũ Tro Khóc / Áo Than Hồng / Quần Tàn Lửa / Ủng Bước Nether. Mũ Fire Protection, Respiration, Unbreaking; áo Fire Protection, Unbreaking, Thorns; quần Protection, Swift Sneak, Unbreaking; ủng Fire Protection, Feather Falling, Soul Speed, Unbreaking.
- **Lore:** “§7Rèn từ tro Ghast Nether.” / “§7Than hồng tản sức nóng.” / “§7Bước qua magma không nao núng.” / “§eTiếng khóc hóa thành lá chắn.”
- **Bonus:** 2 món giảm 10% damage lửa; 3 món giảm 15% damage magma/thời gian cháy; 4 món giảm tổng 22% lửa/magma, không miễn nhiễm lava.

### Khổng Lồ — `giant_boss`
- **Combat:** melee 5→8; Ground Slam 5→8; Boulder Toss 6→8. Slam báo trước 1 giây, đường sóng ngắn/rõ, mỗi người chỉ nhận một hit/cast; Boulder báo quỹ đạo và không xuyên block.
- **Drops giữ:** Rotten Flesh 20–50 (100%), Diamond 2–6 (60%). **Token:** Berserker.
- **Set – Giáp Địa Mạch (10% mỗi món):** Mũ Mạch Đá / Áo Thành Cổ / Quần Địa Tầng / Ủng Trụ Núi. Mũ Protection, Respiration, Unbreaking; áo Protection, Unbreaking, Thorns; quần Protection, Swift Sneak, Unbreaking; ủng Protection, Feather Falling, Unbreaking, Depth Strider.
- **Lore:** “§7Đúc từ quặng dưới thành cổ.” / “§7Địa tầng đỡ chấn động.” / “§7Từng đinh giáp giữ vững núi.” / “§eĐịa Mạch không lay chuyển.”
- **Bonus:** 2 món giảm 5% damage melee; 3 món giảm 10% knockback; 4 món giảm tổng 12% damage trực tiếp, không giảm void/damage liên tục.

### Phantom Boss — `phantom_boss`
- **Combat:** melee 4→7; Dive Bomb 5→8; blindness ≤1 giây quanh điểm va chạm, có âm thanh/particle báo trước. Minion tối đa 2–3 và không cộng dồn; cooldown ≥40 giây.
- **Drops giữ:** Phantom Membrane 5–15 (90%), Diamond 3–6 (35%), Iron Ingot 8–15 (50%), Emerald 5–12 (40%), Enchanted Book 1 (25%), Feather 10–25 (60%), Ghast Tear 1–3 (30%), Experience Bottle 5–15 (50%), Elytra 1 (5%). **Token:** Frost Mage.
- **Set – Giáp Mộng Ảnh (10% mỗi món):** Mũ Mộng Ảnh / Áo Cánh Đêm / Quần Lướt Gió / Ủng Hạ Cánh Êm. Mũ Protection, Respiration, Unbreaking; áo Protection, Unbreaking, Feather Falling; quần Protection, Swift Sneak, Unbreaking; ủng Feather Falling, Depth Strider, Soul Speed, Unbreaking.
- **Lore:** “§7May từ màng cánh đêm.” / “§7Giữ người khỏi cú lao bất ngờ.” / “§7Mộng ảnh dẫn bước qua bóng tối.” / “§eHạ cánh êm giữa trời đêm.”
- **Bonus:** 2 món giảm 10% damage rơi; 3 món giảm thêm 10% damage Phantom; 4 món giảm 30% thời lượng blindness boss gây ra. Không cấp bay.

### Ravager Boss — `ravager_boss`
- **Combat:** melee 5→8; Siege Charge 5→8 mỗi hit, tối đa 12/cast, wind-up 1 giây, ≤8 block phá/lần, dùng allowlist hiện tại, knockback giảm. Roar không damage; Weakness ≤2 giây.
- **Drops giữ:** Leather 15–35 (90%), Diamond 5–10 (45%), Iron Ingot 12–25 (65%), Emerald 10–20 (60%), Saddle 1–2 (40%), Enchanted Book 1 (25%), Iron Block 2–5 (35%), Beef 10–20 (50%). **Token:** Paladin.
- **Set – Giáp Chiến Trường (10% mỗi món):** Mũ Tiền Tuyến / Áo Chiến Trường / Quần Xung Kích / Ủng Giữ Thành. Mũ Protection, Unbreaking, Respiration; áo Protection, Unbreaking, Thorns; quần Protection, Swift Sneak, Unbreaking; ủng Protection, Feather Falling, Depth Strider, Unbreaking.
- **Lore:** “§7Thép tôi từ chiến địa cũ.” / “§7Đinh giáp đỡ cú tông.” / “§7Vết xước nhắc người giữ đội hình.” / “§eChiến Trường thuộc về người trụ vững.”
- **Bonus:** 2 món giảm 6% melee; 3 món giảm 12% knockback; 4 món giảm 15% damage charge. Không phản damage thụ động.

### Skeleton Boss — `skeleton_boss`
- **Combat:** melee 3→6; Arrow Barrage 3→6 mỗi tên, 3–4 tên, tổng ≤12/cast; tản đạn giảm ở cự ly gần, báo trước 0,8 giây.
- **Drops giữ:** Bone 10–25 (90%), Arrow 15–40 (80%), Diamond 3–7 (35%), Iron Ingot 8–18 (60%), Emerald 8–15 (50%), Bow 1 (35%), Enchanted Book 1 (25%), Flint and Steel 1–2 (30%), Skeleton Skull 1–2 (20%). **Token:** Archer.
- **Set – Giáp Vạn Cốt (10% mỗi món):** Mũ Hộ Cốt / Áo Xương Chiến Binh / Quần Bước Vạn Cốt / Ủng Cung Thủ. Mũ Projectile Protection, Respiration, Unbreaking; áo Projectile Protection, Unbreaking, Thorns; quần Projectile Protection, Swift Sneak, Unbreaking; ủng Projectile Protection, Feather Falling, Depth Strider, Unbreaking.
- **Lore:** “§7Ghép từ xương chiến binh xưa.” / “§7Mũi tên không dễ xuyên qua.” / “§7Dấu khắc thuộc Vạn Cốt.” / “§eXương tàn vẫn giữ đội hình.”
- **Bonus:** 2 món giảm 8% projectile damage; 3 món giảm 15% projectile knockback; 4 món tổng giảm 16% projectile damage, không chặn toàn bộ tên.

### Spider Boss — `spider_boss`
- **Combat:** melee 3→6; Venom Web 1→2 damage, Slowness nhẹ ≤2 giây, chỉ trong 5 block và không xuyên block. Cobweb Trap ≤4 giây, báo vùng và có thể phá/thoát. Pounce 4→8, telegraph cú nhảy, cooldown ≥12 giây. Brood 2–3 con thay vì 4–6, không triệu hồi liên tiếp. Truy vết native melee/projectile để sửa hit tầm xa tận gốc.
- **Drops giữ:** String 15–35 (90%), Spider Eye 5–12 (80%), Diamond 3–6 (35%), Iron Ingot 8–15 (50%), Emerald 5–12 (40%), Cobweb 3–8 (50%), Enchanted Book 1 (20%), Fermented Spider Eye 1–3 (30%). **Token:** Assassin.
- **Set – Giáp Tơ Hoàng Hôn (10% mỗi món):** Mũ Mắt Nhện / Áo Tơ Hoàng Hôn / Quần Thợ Săn Hang / Ủng Bước Tơ. Mũ Protection, Respiration, Aqua Affinity, Unbreaking; áo Protection, Unbreaking, Thorns; quần Protection, Swift Sneak, Unbreaking; ủng Protection, Depth Strider, Feather Falling, Unbreaking.
- **Lore:** “§7Dệt bằng tơ cứng trong hang.” / “§7Mặt giáp giữ nọc nhện.” / “§7Nút thắt ánh Hoàng Hôn.” / “§eSợi cuối dẫn về Hoàng Hôn.”
- **Bonus:** 2 món giảm 10% poison damage; 3 món giảm 20% thời lượng Slowness/Cobweb; 4 món tổng giảm 25% độc/khống chế tơ, không miễn nhiễm hoàn toàn.

### Vindicator Boss — `vindicator_boss`
- **Combat:** melee 4→8; Axe Spin 2→4 mỗi hit, tối đa 3 hit/chu kỳ (≤12 tổng) và có khoảng nghỉ; shield disable ≤2 giây. Axe Throw 4→8, báo quỹ đạo, tầm ≤16 block, không xuyên tường.
- **Drops giữ:** Iron Axe 1–2 (60%), Emerald 10–20 (80%), Diamond 3–7 (35%), Iron Ingot 8–16 (50%), Gold Ingot 5–10 (40%), Enchanted Book 1 (25%), Diamond Sword 1 (15%). **Token:** Guardian.
- **Set – Giáp Phán Quyết Đỏ (10% mỗi món):** Mũ Thẩm Phán / Áo Phán Quyết / Quần Hộ Vệ / Ủng Truy Kích. Mũ Protection, Respiration, Unbreaking; áo Protection, Unbreaking, Thorns; quần Protection, Swift Sneak, Unbreaking; ủng Protection, Feather Falling, Depth Strider, Unbreaking.
- **Lore:** “§7Tôi trong lò làng đổ nát.” / “§7Thép đỏ giữ lời thề.” / “§7Đỡ vững trước lưỡi rìu.” / “§ePhán quyết dành cho kẻ đứng vững.”
- **Bonus:** 2 món giảm 6% melee; 3 món giảm 12% thời gian shield disable; 4 món tăng 10% hiệu quả block và giảm thêm 8% damage khi đang blocking.

### Witch Boss — `witch_boss`
- **Combat:** melee 3→6; Potion Rain 3→6 mỗi bình, 3–4 bình, tổng ≤12/cast; báo vùng rơi, tránh dồn nhiều potion cùng lúc. Disarm/Fatigue ≤2 giây. Witchcraft Drain có giới hạn hồi máu mỗi cast, không hút vô hạn từ môi trường.
- **Drops giữ:** Potion 5–15 (80%), Glowstone Dust 10–25 (80%), Diamond 3–6 (35%), Iron Ingot 8–15 (50%), Emerald 10–20 (60%), Redstone 15–30 (70%), Enchanted Book 1–2 (30%), Sugar 5–10 (40%), Spider Eye 3–8 (40%), Gunpowder 5–10 (40%). **Token:** Druid.
- **Set – Giáp Trăng Máu (10% mỗi món):** Mũ Bình Nguyền / Áo Trăng Máu / Quần Phù Chú / Ủng Sương Thuốc. Mũ Protection, Respiration, Unbreaking; áo Protection, Unbreaking, Thorns; quần Protection, Swift Sneak, Unbreaking; ủng Fire Protection, Feather Falling, Depth Strider, Unbreaking.
- **Lore:** “§7Khâu dưới ánh Trăng Máu.” / “§7Vải giữ dấu độc dược.” / “§7Phù chú không khuất tầm nhìn.” / “§eSương thuốc tan trước bình minh.”
- **Bonus:** 2 món giảm 10% thời lượng debuff; 3 món giảm 12% potion damage; 4 món giảm 20% thời lượng potion/poison, không xóa hiệu ứng ngay lập tức.

### Wither Huyền Thoại — `wither`
- **Combat:** melee 5→8; Skull Volley 4→7 mỗi skull, 2–3 skull, tổng direct damage ≤12/cast; Wither effect tối đa 3 giây mỗi hit và có cooldown trước khi áp lại. Skull báo hướng/đường bay có thể né.
- **Drops giữ:** Netherite Ingot 2–5 (100%), Diamond 5–10 (80%), Emerald 10–20 (70%), Iron Ingot 15–30 (60%), Gold Ingot 10–20 (60%), Diamond Block 1–3 (50%), Wither Skeleton Skull 1–2 (40%), Enchanted Book 1–2 (30%), Nether Star 1 (20%), Beacon 1 (10%). **Token:** Medic.
- **Set – Giáp Ba Đầu Hư Vô (10% mỗi món):** Mũ Hư Vô / Áo Ba Đầu / Quần Tro Đen / Ủng Tàn Tích. Mũ Blast Protection, Respiration, Unbreaking; áo Protection, Unbreaking, Thorns; quần Blast Protection, Swift Sneak, Unbreaking; ủng Blast Protection, Feather Falling, Soul Speed, Unbreaking.
- **Lore:** “§7Rèn giữa tro tàn Hư Vô.” / “§7Thép chặn mảnh sọ bay.” / “§7Khắc cách chống lời nguyền.” / “§eBa Đầu canh giữ cổng cuối.”
- **Bonus:** 2 món giảm 8% Wither damage; 3 món giảm 20% thời lượng Wither; 4 món tổng giảm 15% damage Wither/skull explosion, không miễn nhiễm Wither.

### Zombie Boss — `zombie_boss`
- **Combat:** melee 4→7; Ground Slam 4→8, báo trước 1 giây, knockback giới hạn, mỗi cast chỉ gây damage một lần/người. Kiểm tra native melee và tốc độ di chuyển để tránh hit từ xa.
- **Drops giữ:** Rotten Flesh 10–25 (90%), Iron Ingot 10–20 (70%), Diamond 3–7 (35%), Emerald 8–15 (50%), Gold Ingot 5–12 (50%), Carrot 5–10 (40%), Potato 5–10 (40%), Enchanted Book 1 (20%), Iron Sword 1 (30%), Iron Helmet 1 (25%). **Token:** Guardian.
- **Set – Giáp Mộ Sâu (10% mỗi món):** Mũ Mộ Sâu / Áo Kẻ Canh Mộ / Quần Đất Cổ / Ủng Hồi Sinh. Mũ Protection, Respiration, Unbreaking; áo Protection, Unbreaking, Thorns; quần Protection, Swift Sneak, Unbreaking; ủng Protection, Feather Falling, Depth Strider, Unbreaking.
- **Lore:** “§7Đúc từ sắt dưới mộ sâu.” / “§7Giữ vững trước bầy xác sống.” / “§7Đất cổ không níu chân người sống.” / “§eBình minh vẫn chờ phía trước.”
- **Bonus:** 2 món giảm 8% undead damage; 3 món giảm 12% undead melee; 4 món hồi 1 tim sau khi hạ mob undead (cooldown 10 giây; không tính lính/đệ do người chơi triệu hồi).

## Các bước thực hiện và xác minh

1. **HP:** sửa `BossEventManager.java` để bỏ giới hạn 1.024 an toàn, thêm bảng HP riêng cho 13 boss và xác minh max-health entity/Paper, boss bar, phase, damage contribution/thưởng. Giá trị cấu hình không hợp lệ phải log rõ.
2. **Combat/skill:** kiểm tra đường damage melee native, projectile, explosion, potion và plugin skill; điều chỉnh YAML và Java theo bảng. Giới hạn từng hit và tổng cast, thêm warning/đường thoát. Enderman cần LOS, giới hạn chase, tầm/cooldown teleport và ngắt target khi mất dấu.
3. **Set engine:** cấu hình enchant theo boss/slot/level; gắn PDC, đếm set pieces, áp/gỡ bonus 2/3/4 không cộng lặp; đặt chance 10% riêng từng armor piece. Kiểm tra đổi đồ, login, chết/hồi sinh, reload.
4. **Nội dung:** sửa chỉ `armor-set` và lore set trong `story.yml`; không sửa drops, level-drops, token hay enabled của Dragon. Đảm bảo không màu đen trong tên/lore set.
5. **Tài liệu/kiểm tra:** README giải thích HP, skill, token, set/enchant và bonus 2/3/4; cập nhật changelog; kiểm tra YAML, damage từng level/trần cast, từng bonus mốc, drop ngoài set không đổi và Dragon vẫn disabled. Không tạo JAR phát hành nếu chưa được yêu cầu.

## File dự kiến và căn cứ mã hiện tại

- Java: `VnMineHardcore/src/main/java/com/vnmine/hardcore/managers/BossEventManager.java`, `ConfigManager.java`, và listener/helper cần thiết để theo dõi trang bị.
- Cấu hình: `VnMineHardcore/src/main/resources/bosses/_settings.yml` cùng 13 file boss.
- Nội dung/tài liệu: `VnMineHardcore/src/main/resources/story.yml`, `VnMineHardcore/README.md`, `CHANGELOG.md`.
- `resolveBossHp` hiện cap ở `MAX_BOSS_HEALTH = 1024` dù YAML có HP cao hơn. Nhiều skill dùng `damage * level` và volley nhiều projectiles. Enderman `void-snatch` hiện có thể cast không LOS, dịch chuyển ra sau và gây Slowness 60 tick amplifier 9. Bonus set giáp hiện chưa được triển khai; Spider đang dùng màu đen `§0`.
- Các damage/bonus ở trên là mục tiêu đề xuất để duyệt, không phải số liệu đã được kiểm thử trong runtime. Trước khi triển khai cần đối chiếu event damage và giới hạn API để bảo đảm con số gameplay thực tế khớp kế hoạch.
