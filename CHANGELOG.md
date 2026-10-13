# Changelog

## [v1.15.0] - 2026-10-13

### Added
- Thêm nền tảng `/vnarmy`: thuê lính theo token, XP và vật liệu; dữ liệu, chế độ và trang bị được lưu bền vững.
- Thêm các lệnh recall/rest, attack/defend/protect, danh sách quân và giao diện trang bị.
- Thêm lớp Xạ Thủ, Vệ Binh, Y Sư; hỗ trợ AI chiến đấu, hồi máu chủ nhân/đồng minh và buff.
- Thêm `armies.yml` để cấu hình số lượng, chi phí, cấp, lớp lính, AI và kỹ năng.

## [v1.14.0] - 2026-10-13

### Added
- Đặt xác suất mưa mặc định 40%, thời lượng tối đa 5 phút và chọn ngẫu nhiên để mưa có thể kết thúc sớm.
- Thêm ngưỡng đóng góp sát thương boss 50%; người vượt ngưỡng được tính điểm/loot dù boss chết bởi nguyên nhân khác, trừ `/vnboss kill`.
- Thêm bộ giáp bốn món cho cả 13 boss, vật liệu/enchant tăng theo cấp và lore truyện riêng trong `story.yml`.
- Tăng xác suất rơi vũ khí/sách boss, cân enchant từ cấp dưới chuẩn đến over-enchant ở level cao.
- Tăng dần xác suất spawn boss theo ngày đến 100% và cân level từ 1 đến 10.
- Cân đều trọng số thiên tai đang bật và chọn ngẫu nhiên trong nhóm phù hợp điều kiện.
- Việt hóa tên skill boss trong thông báo, bổ sung broadcast cho toàn bộ người chơi.
- Làm nổi bật boss bằng viền phát sáng, bật persistence và ghi log khi entity boss mất hiệu lực.
- Giant có chuyển động đuổi mục tiêu và đi lang thang chậm hơn.
- Boss chết không có người chơi vượt ngưỡng sát thương sẽ được ghi log và không nhận điểm/drop.
- Ghi console log và thông báo skill cast riêng cho OP; có thể bật/tắt trong `bosses/_settings.yml`.

### Fixed
- Creeper không còn bị bộ xử lý kẹt giữ nguyên vận tốc dọc và bay tăng độ cao; khôi phục gravity và chỉ dùng pathfinding cho mob.
- Các boss mob giữ AI/navigation vanilla trong truy đuổi và đi lang thang; bỏ ép quay về tâm ngẫu nhiên gây ngắt hành vi.
- Enderman có hiệu ứng cổng và âm thanh khi dùng Void Snatch; chỉ đánh dấu cast thành công khi teleport hợp lệ.
- Ground Slam không còn gây sát thương nhiều lần khi các vùng sóng chồng lấn; khối tạm không ghi đè thay đổi của người chơi.
- Ender Dragon không còn bị ép bay tới điểm ngẫu nhiên mới mỗi chu kỳ AI.

## [v1.13.1] - 2026-10-13

### Fixed
- Chặn sát thương Ground Slam lặp trong cùng một lần cast và bảo vệ chỉnh sửa của người chơi với khối tạm.
- Giảm điều khiển cưỡng bức chuyển động Ender Dragon để tránh đường bay giật.

## [v1.13.0] - 2026-10-13

### Added
- Thêm `/vnboss kill` để kết thúc boss hiện tại mà không trao điểm hoặc loot.
- Thêm skill và trait cấu hình cho Enderman, Ravager, Phantom, Ghast, Spider, Vindicator, Witch và Giant.
- Thêm cấu hình terrain hazard tạm thời và allowlist block cho Ravager phá địa hình.

### Changed
- Chỉ trao điểm tiêu diệt boss khi người chơi hạ boss trước khi hết thời gian; timeout/despawn không được điểm.
- Giữ AI mặc định của boss; Creeper tiếp tục truy đuổi mục tiêu nhưng không tự kích nổ vanilla.
- Bảo vệ spawn region khỏi thay đổi địa hình do skill boss.

## [v1.12.0] - 2026-10-12

### Added
- Cung Skeleton level 1 luôn rơi cung Flame I và Infinity I.

### Changed
- Rút gọn lore di vật trong `story.yml` còn tối đa 30 ký tự mỗi dòng và 6 dòng mỗi item, giữ liên kết giữa các chương boss.

### Fixed
- Boss không còn bị dịch chuyển lên cao khi thoát kẹt; Creeper không còn tự nhận sát thương từ skill sét.

## [v1.11.1] - 2026-10-12

### Fixed
- Mưa axit chỉ gây sát thương và hiệu ứng khi người chơi thực sự ở ngoài trời có mưa.

## [v1.11.0] - 2026-10-12

### Added
- Thêm kho truyện `story.yml` cho di vật boss; tên và lore cấp thấp được cân chỉnh theo sức mạnh vật phẩm.

### Fixed
- Boss miễn nhiễm sát thương môi trường và sát thương từ skill tự tạo; chỉ rơi item khi người chơi đã gây sát thương.

## [v1.10.0] - 2026-10-12

### Added
- Thêm quản lý nhóm/quyền trong `permission.yml` qua `/vngroup` và `/vnuser`, mặc định có nhóm `user` và `admin`.
- Tách quyền lệnh theo chức năng để có thể cấp riêng quyền xem trạng thái, quản lý thiên tai/boss, spawn, thời gian, game mode và quản lý người dùng.
- Thêm quyền `vnmine.spawnprotection.bypass` cho OP/nhân viên được ủy quyền.
- Thêm `story.yml` làm kho lore tập trung cho các di vật hiếm boss, kết nối thành vũ trụ truyện chung và hỗ trợ mở rộng bằng `story-id`; cấp thấp dùng tên/lore giản dị hơn.

### Fixed
- Spawn protection chặn cả phá/đặt block, cháy, dòng chất lỏng, thay đổi block do entity và phá block do vụ nổ.
- Các thao tác đặt, đốt, thay thế, nhấc block và nổ trực tiếp từ thiên tai bỏ qua block nằm trong vùng bảo vệ.
- Boss miễn nhiễm sát thương môi trường (kể cả sát thương rơi) và sát thương từ skill tự tạo; mọi drop chỉ rơi khi có người chơi gây sát thương.

## [v1.9.1] - 2026-10-12

### Fixed
- Đóng gói lại JAR từ resource hiện tại để `plugin.yml` chứa đầy đủ lệnh `vnspawn` và `tp`, tránh lỗi `getCommand(...)` trả về `null` khi khởi động.

## [v1.9.0] - 2026-10-12

### Added
- `/vnspawn set` đặt spawn của world hiện tại tại vị trí và hướng đang đứng.
- `/vnspawn go <world>` dịch chuyển người gọi đến spawn của world đang tải; hỗ trợ gợi ý tên world bằng Tab.

## [v1.8.0] - 2026-10-12

### Added
- `/tp <user> <world>` dịch chuyển người chơi đang trực tuyến đến spawn của world; Tab gợi ý người chơi và world.
- `/vnspawn protect <X>`, `/vnspawn list`, `/vnspawn remove <ID>` quản lý vùng bảo vệ theo world, lưu trong `spawns-protection.yml`.
- `/vnevent stop` hủy cảnh báo hoặc thiên tai đang hoạt động, dừng các tác vụ liên quan và khôi phục trạng thái world được theo dõi.
- Earthquake hất người chơi ngoài trời theo phương ngang và dọc; vận tốc có thể chỉnh trong `disasters/earthquake.yml`.
- Mỗi file thiên tai có `chat-description` tùy chỉnh; chat hiển thị tác động, gợi ý phòng tránh và tên world tại lúc cảnh báo/bắt đầu.

### Fixed
- Bộ lọc thiên tai nhận diện đúng tên world và tên dimension; điều kiện `conditions.worlds` hiện được áp dụng.
- Chorus Explosion tắt mặc định để tránh khởi chạy tự động ngoài ý muốn.

## [v1.7.0] - 2026-10-12

### Added
- `/vntime set <day|night> <phút> [world]` đặt thời lượng toàn cục hoặc riêng cho world đang tải
- `/vnday set <ngày>` đặt world hiện tại về bình minh của ngày trong game tương ứng
- Loot hiếm và cấu hình HP riêng cho boss level 1–10; lore truyền thuyết tối đa 4 dòng, số dòng enchant tăng dần theo cấp
- Thời gian tồn tại mặc định của mọi boss là 20 phút; tạm dừng roll boss tiếp theo khi đang cảnh báo hoặc có boss hoạt động

## [v1.6.0] - 2026-10-12

### Added
- `/scoreboard reset` xóa điểm, mùa và phần thưởng boss đang chờ nhận
- `/vntime set day <phút>` và `/vntime set night <phút>` thay đổi, lưu thời lượng mặc định toàn cục; giữ nguyên override theo world
- `/vnhelp` liệt kê mọi lệnh plugin; Tab gợi ý ID và giá trị cho các tham số lệnh

## [v1.5.0] - 2026-10-12

### Added
- Creeper Boss có skill tự gọi sét, gây sát thương lên bản thân và sinh vật trong bán kính cấu hình, rồi chuyển sang trạng thái charged trong thời gian cấu hình
- Creeper Boss phát nổ thường/charged mạnh hơn tương ứng nhưng không tự bị xóa; vụ nổ không phá block

## [v1.4.0] - 2026-10-12

### Added
- Loot hiếm theo level trong file riêng của từng boss; hỗ trợ lore, enchant thường/over-enchant và sách lưu enchant để áp dụng lên vật phẩm
- Broadcast danh sách loot và tỉ lệ khi boss xuất hiện
- Bảng xếp hạng damage trực tiếp và điểm boss theo mùa; top 5 chia `10 × level` điểm theo damage gây ra
- Reset theo ngày trong game, thưởng cấu hình cho top 10, lệnh `/reward`, nhắc nhận quà, kiểm tra inventory và title chúc mừng top 1
- `scoreboard.yml` cho cấu hình mùa/sidebar/phần thưởng và `scoreboard-data.yml` cho lưu trạng thái

## [v3.0.0] - 2025-07-12

### Added
- Config tách rời: Boss và Disaster không còn nằm trong `config.yml`
- Thêm `bosses/*.yml`: Mỗi boss 1 file, thêm boss mới chỉ cần thêm file
- Thêm `disasters/*.yml`: Mỗi disaster 1 file, thêm disaster mới chỉ cần thêm file
- Action system: Config-driven cho disaster, cho phép custom hành vi hoàn toàn từ file
- Weighted target system trong disaster actions (player, mob, ground, trees,...)
- Condition system cho disaster (only-night, only-day, worlds, weather)
- Boss immunity config (sunlight_burn, fire, poison,...) trong boss file

### Changed
- `config.yml`: Giảm từ 1481 dòng xuống ~721 dòng (bỏ sections boss-events, disasters, safe-zone)
- `ConfigManager.java`: Thêm load từ file rời (`bosses/*.yml`, `disasters/*.yml`, `disasters/_settings.yml`)
- `BossEventManager.java`: Sửa `EntityCombustEvent` để tôn trọng boss immunity

### Removed
- Config section `boss-events` (khoảng 592 dòng) → chuyển sang `bosses/*.yml`
- Config section `disasters` (khoảng 239 dòng) → chuyển sang `disasters/*.yml` + `_settings.yml`
- Config section `disasters.safe-zone` → chuyển vào `disasters/_settings.yml`
- Config section `disasters.messages` → chuyển vào `disasters/_settings.yml`

### Technical Debt
- `BossEventManager.java`: ~760 dòng (giữ nguyên, logic spawn/drop/AI không đổi)
- `DisasterManager.java`: ~1082 dòng (cần refactor action engine nếu disaster mới)
- `ConfigManager.java`: ~530 dòng (thêm ~150 dòng cho external config loader)

### File Stats

#### Deleted from config.yml
| Feature | Issues (lines) removed |
|---------|----------------------|
| boss-events | ~592 dòng (1 section, 9 bosses) |
| disasters | ~239 dòng (1 section, 14 disasters) |
| disasters.safe-zone | ~15 dòng |
| disasters.messages | ~19 dòng |

#### Added to bosses/*.yml
| File | Features/Issues |
|------|----------------|
| `_settings.yml` | General settings (interval, spawn-radius) |
| `wither.yml` | 1 boss + immunity + drops (7 items) |
| `ender_dragon.yml` | 1 boss + immunity + drops (10 items) |
| `ghast.yml` | 1 boss + immunity + drops (9 items) |
| `zombie_boss.yml` | 1 boss + immunity + drops (10 items) |
| `skeleton_boss.yml` | 1 boss + immunity + drops (9 items) |
| `spider_boss.yml` | 1 boss + immunity + drops (8 items) |
| `creeper_boss.yml` | 1 boss + immunity + drops (8 items) |
| `enderman_boss.yml` | 1 boss + immunity + drops (8 items) |
| `witch_boss.yml` | 1 boss + immunity + drops (9 items) |
| `ravager_boss.yml` | 1 boss + immunity + drops (7 items) |
| `vindicator_boss.yml` | 1 boss + immunity + drops (7 items) |
| `phantom_boss.yml` | 1 boss + immunity + drops (8 items) |

#### Added to disasters/*.yml
| File | Features/Issues |
|------|----------------|
| `_settings.yml` | Global settings, safe-zone, messages |
| `blood-moon.yml` | 1 disaster + 3 actions (spawn, potion, actionbar) |
| `meteor.yml` | 1 disaster + 2 actions (lightning, explosion) |
| `mega-storm.yml` | 1 disaster + 4 actions (lightning, damage, potion, weather) |
| `solar-flare.yml` | 1 disaster + 3 actions (damage, fire, potion) |
| `plague.yml` | 1 disaster + 1 action (potion effects) |
| `tornado.yml` | 1 disaster + 1 action (velocity) |
| `eclipse.yml` | 1 disaster + 3 actions (time, potion, spawn) |
| `earthquake.yml` | 1 disaster + 2 actions (potion, falling_block) |
| `inferno-storm.yml` | 1 disaster + 4 actions (damage, fire, place_block, spawn) |
| `soul-eruption.yml` | 1 disaster + 4 actions (damage, potion, explosion, spawn) |
| `lava-geyser.yml` | 1 disaster + 3 actions (damage, fire, place_block) |
| `end-surge.yml` | 1 disaster + 2 actions (spawn, potion) |
| `void-storm.yml` | 1 disaster + 2 actions (damage, potion) |
| `chorus-explosion.yml` | 1 disaster + 2 actions (damage, teleport) |