# Changelog

## [v1.38.2] - 2026-10-16

### Fixed
- Cho AI lính tìm mục tiêu theo danh sách ưu tiên kể cả khi mục tiêu đang khuất tầm nhìn, để Pathfinder tự tìm đường; Trinh Sát chỉ dừng khi đã ở trong tầm bắn và nhìn thấy mục tiêu.

## [v1.38.1] - 2026-10-16

### Fixed
- Cho Trinh Sát tiếp tục chọn và tấn công mục tiêu theo AI của chế độ khi đang bảo vệ chủ, thay vì chỉ ưu tiên đi theo chủ nhân.
- Tự bổ sung skill `ILLUMINATION` một lần vào cấu hình Ranger cũ, đồng thời giữ quyền tùy chỉnh danh sách skill sau khi nâng cấp.

## [v1.38.0] - 2026-10-16

### Added
- Thêm `/vndeathhistory [player]` và menu admin xem tối đa 10 snapshot inventory khi chết; khôi phục thay thế túi đồ, giáp và tay phụ sau bước xác nhận.
- Thêm bước xác nhận trước khi xóa lính bằng GUI quản lý.
- Thêm skill Soi Sáng cho Trinh Sát, bắn tên lửa tạo đuốc tạm 30 giây; thêm cấu hình số tên, tầm tỏa, tốc độ và hồi chiêu.

### Changed
- Đặt mộ trên mặt nước khi chết dưới nước, dùng block hỗ trợ tạm và khôi phục block gốc khi mộ bị dọn.
- Trinh Sát dùng nỏ, chủ động tìm mục tiêu theo AI ưu tiên chung, có tầm phát hiện/bắn xa gấp đôi Xạ Thủ; tiêu thụ tên trong kho hoặc dùng Spectral Arrow miễn phí khi hết.
- Chặn `/quatanthu` nếu người chơi đã nhận quà trước đây hoặc hiện sở hữu lính đánh thuê.

## [v1.37.0] - 2026-10-16

### Fixed
- Chặn lính đánh thuê và VEX gây sát thương lên người chơi/lính của chủ khác, trừ khi chủ nhân trực tiếp đánh đúng mục tiêu trong thời hạn 15 giây; thêm cấu hình để tắt hoàn toàn ngoại lệ PvP.
- Khi ngựa Kỵ sĩ chết, lưu trạng thái qua khởi động lại và không tự tạo ngựa thay thế; nghỉ Kỵ sĩ sẽ xóa ngựa còn sống và cho phép tạo ngựa mới ở lần xuất chiến tiếp theo.

## [v1.36.0] - 2026-10-16

### Fixed
- Giới hạn mục tiêu người chơi do chủ nhân chủ động đánh trong 15 giây; lính không tiếp tục đuổi theo mục tiêu PvP cũ và có thể truy đuổi mục tiêu chủ nhân bắn trúng kể cả khi lính chưa nhìn thấy.
- Xóa VEX được Tu sĩ rừng triệu hồi khi lính nghỉ; VEX quay về bằng đường bay chậm thay vì dịch chuyển giật cục.
- Dịch chuyển cả ngựa của Kỵ sĩ cùng lính; thêm cấu hình tốc độ ngựa mặc định bằng mức phi nước đại tối đa.

## [v1.35.0] - 2026-10-16

### Changed
- Đổi thanh HP VEX sang màu xanh lá và thêm biểu tượng hành động AI cập nhật theo trạng thái tấn công, theo Tu sĩ hoặc đứng yên; màu và biểu tượng có thể tùy chỉnh trong `armies.yml`.
- Khi nghỉ, VEX quay về gần Tu sĩ rừng và được giữ trong bán kính tối đa 5 block.

## [v1.34.0] - 2026-10-16

### Added
- Thêm thanh HP riêng dạng ô đầy/rỗng cho từng VEX của Tu sĩ rừng và AI ưu tiên mục tiêu Tu sĩ đang đánh, kẻ tấn công Tu sĩ, kẻ tấn công chủ nhân, rồi theo Tu sĩ.
- VEX của Tu sĩ rừng tăng sức chứa thêm một con mỗi 5 level, bắt đầu từ một con ở level 1; thêm cấu hình giới hạn và thứ tự ưu tiên AI.

## [v1.33.4] - 2026-10-16

### Fixed
- Buộc mọi lính Cuồng Chiến có cấu hình cũ `PIGLIN_BRUTE` được tạo và khôi phục dưới dạng `ZOMBIFIED_PIGLIN`; ghi log cảnh báo khi gặp cấu hình cũ.

## [v1.33.3] - 2026-10-16

### Fixed
- Chuyển Cuồng Chiến sang ZOMBIFIED_PIGLIN để loại bỏ nguyên nhân biến hình ở Overworld; tự cập nhật cấu hình mặc định cũ và thay entity đang chạy sau khi lưu trang bị.

## [v1.33.2] - 2026-10-16

### Fixed
- Bảo toàn vật phẩm lính đang mang khi chặn sự kiện biến hình; khôi phục ô bị trống từ dữ liệu đã lưu ở tick kế tiếp và tái áp dụng miễn nhiễm zombification định kỳ.

## [v1.33.1] - 2026-10-16

### Fixed
- Ngăn lính đánh thuê thuộc nhóm Piglin, bao gồm Cuồng Chiến, biến thành Zombified Piglin ở Overworld; áp dụng cả khi lính được tải lại từ chunk.

## [v1.33.0] - 2026-10-16

### Changed
- Hiển thị tên chủ, tên lính, bậc và level ở dòng cuối của thanh trạng thái.

## [v1.32.0] - 2026-10-16

### Added
- Thêm cấu hình `status-display.see-through` để điều khiển khả năng nhìn thanh HP xuyên tường; mặc định bật.

## [v1.31.0] - 2026-10-16

### Changed
- Thanh lính đánh thuê hiển thị hai dòng: vạch HP riêng và dòng tim/HP/dấu sao/EXP.

## [v1.30.3] - 2026-10-16

### Fixed
- Dùng `IntList` cho packet `ENTITY_DESTROY` theo đúng kiểu `entityIds` trên Paper 26.3; giữ `int[]` cho packet `MOUNT`.

## [v1.30.2] - 2026-10-16

### Fixed
- Sửa metadata `TEXT_DISPLAY`: dùng `Component` không bọc Optional, billboard đúng index 15 và opacity đầy đủ tại index 26.
- Sửa modifier entity ID của packet `MOUNT`/`ENTITY_DESTROY` cho schema 26.3 dùng `int[]`, tránh lỗi `FieldAccessException` khi gọi lính.

## [v1.30.0] - 2026-10-16

### Added
- Chuyển thanh HP/EXP lính đánh thuê sang `TextDisplay` ảo qua ProtocolLib; chỉ chủ nhân nhìn thấy, người khác chỉ thấy tên lính.
- Thêm token triệu hồi theo class cấu hình riêng từng boss; bậc token khớp level boss.
- Thêm thanh HP/EXP phía trên lính, tự thu EXP orb cho chủ online, cấu hình ưu tiên AI theo mode và bán kính tuần tra.
- Thêm cơ chế Kỵ sĩ tự tìm mount do plugin quản lý, lưu/chọn giáp ngựa tốt nhất và cấu hình HP riêng cho ngựa.
- VEX của Tu sĩ rừng tăng giới hạn theo level, mỗi lần cast chỉ gọi thêm một con; summon dùng chung bộ lọc mục tiêu của lính.

### Changed
- Đồng bộ compile target với Paper `26.3.build.175-stable` và Java 25; yêu cầu ProtocolLib runtime `5.5.0-SNAPSHOT`.
- Việt hóa tên class/skill và hiển thị mô tả, thông số, cooldown thực tế theo level; sức mạnh tăng tuyến tính, cooldown giảm tối đa vài giây.
- Lính chọn vũ khí tốt nhất giữa tay chính và kho; chỉ cấp vũ khí mặc định khi không có vũ khí phù hợp hơn.
- AI phân thứ tự trả đũa, tìm mục tiêu, theo chủ và tuần tra; không chủ động tấn công lính/đệ của người chơi khác.

### Fixed
- Chặn biến hình ở ưu tiên sự kiện cao và lưu gear trước khi hủy transform.

## [v1.29.0] - 2026-10-14

### Changed
- Giảm sát thương của các boss xuống mức tối đa 10 damage và giới hạn phạm vi skill trong khoảng 3-5 block để cân bằng combat.
- Nâng cấp build mới cho phiên bản 1.29.0.

## [v1.28.0] - 2026-10-14

### Fixed
- Chặn biến hình của mercenary để giữ nguyên item trên người và bảo toàn gear khi entity đổi form.
- Cập nhật AI lính đánh thuê: chủ động tiêu diệt hostile mob trong 50 block, không tấn công đồng minh/người chơi khác ngoài trường hợp chủ nhân đã tấn công trước, và giữ dải tấn công an toàn với đội cùng chủ.
- Đồng bộ và việt hóa giao diện thông báo cho người dùng sang tiếng Việt.

## [v1.27.1] - 2026-10-14

### Fixed
- Chặn biến hình của mercenary để giữ nguyên item trên người và bảo toàn gear khi entity đổi form.
- Cập nhật AI lính đánh thuê: chủ động tiêu diệt hostile mob trong 50 block, không tấn công đồng minh/người chơi khác ngoài trường hợp chủ nhân đã tấn công trước, và giữ dải tấn công an toàn với đội cùng chủ.

## [v1.27.0] - 2026-10-14

### Added
- Thêm túi quà tân thủ `BLACK_BUNDLE`, `/quatanthu` để nhận túi lần đầu, menu chọn token triệu hồi lính đánh thuê bậc 1 và lưu trạng thái chỉ nhận một lần duy nhất theo người chơi.

## [v1.24.2] - 2026-10-14

### Fixed
- Bỏ âm thanh ting phát cùng thông báo lính đánh thuê sử dụng kỹ năng; giữ particle và action bar.

## [v1.24.1] - 2026-10-14

### Fixed
- Thông tin menu lính hiển thị vũ khí phù hợp theo cấu hình class thay vì vũ khí hiện đang cầm.

## [v1.24.0] - 2026-10-14

### Added
- Hiển thị loại vũ khí tay chính trong thông tin chi tiết lính đánh thuê.

### Fixed
- Bảo toàn vật phẩm trong kho lính khi đóng giao diện, kể cả thao tác chuyển nhanh hoặc vật phẩm nằm ở ô giao diện.
- Chặn cháy nắng trên toàn bộ lính đánh thuê và ngựa đang chở Kỵ sĩ.

## [v1.23.0] - 2026-10-14

### Changed
- Kỵ sĩ dùng `IRON_SPEAR` khi runtime hỗ trợ, tự dự phòng sang TRIDENT khi không có Material tương ứng; hiệu ứng lao giáo có thêm hoạt ảnh/âm thanh dễ nhận biết.
- Tắt riêng âm thanh `ENTITY_EVOKER_PREPARE_ATTACK` của Evoker lính đánh thuê; các âm thanh Evoker khác vẫn theo trạng thái mute hiện tại.

## [v1.22.1] - 2026-10-14

### Fixed
- Chặn VEX tiếp tục đánh mục tiêu bị cấm và chặn sát thương lên chủ nhân/lính đồng minh.
- Sửa trang bị TRIDENT cho Kỵ sĩ, skill lao giáo khi cưỡi ngựa và dọn ngựa cùng Kỵ sĩ khi nghỉ/thoát game/chết.

## [v1.22.0] - 2026-10-14

### Fixed
- Chặn hành vi panic/flee của mercenary dùng model Villager để giữ custom AI chiến đấu.
- Chặn biến thể zombification cho mercenary Piglin/Piglin Brute và loại bỏ Vex do Evoker sinh ra khỏi quân lính.
- Chuyển class Kỵ sĩ sang model Zombie cưỡi ngựa, đổi tên hiển thị thành "Kỵ sĩ" và bổ sung skill lao giáo khi đang cưỡi.

## [v1.21.0] - 2026-10-14

### Changed
- Menu gọi boss của admin hiển thị skull đúng loại boss hoặc spawn egg tương ứng; dùng barrier khi Minecraft không cung cấp cả skull lẫn spawn egg (hiện gồm Wither và Giant).

## [v1.20.1] - 2026-10-14

### Fixed
- Kỹ năng `VOLLEY` dùng đúng loại mũi tên trong kho lính, bao gồm tên phổ và tên tẩm hiệu ứng; mũi tên vẫn không bị tiêu hao.

## [v1.20.0] - 2026-10-14

### Added
- Hiển thị tiền tố chủ lính xanh lá sáng, đậm với chủ và xanh da trời sáng, đậm với người xem khác; yêu cầu ProtocolLib 5.5.0-SNAPSHOT dev-build hỗ trợ Paper 26.3.

### Changed
- Mô tả đúng điều kiện rơi token boss: roll 30% khi có người chơi gây sát thương boss.

## [v1.19.0] - 2026-10-13

### Added
- Mở rộng quân đội lên 10 class với Pháp Sư Lửa, Druid, Sát Thủ và Pháp Sư Băng; mỗi class có vũ khí, chỉ số, skill và chi phí nâng bậc riêng.
- Thêm skill `FIRE_BOLT`, `ROOT_TARGET`, `LIFESTEAL`, `BERSERK`; thông số và cooldown có thể tùy chỉnh trong `armies.yml`.

## [v1.18.0] - 2026-10-13

### Added
- Nâng giới hạn đội lính mặc định lên 10; tách bậc thuê khỏi cấp farm tối đa 30 và hiển thị EXP/trạng thái đầy đủ.
- Thêm `/vnarmy rename <ID> <Tên mới>`, recall/rest theo ID, nâng bậc bằng nút GUI và chi phí vật phẩm theo class/bậc.
- Thêm hồi phục lính sau khi bị hạ, EXP boss theo loại/level và EXP cho người gây sát thương cùng lính đang xuất chiến.
- Thêm trang bị mặc định, tự nhận diện vũ khí/giáp, kho vật phẩm bền vững, mũi tên vô hạn và thức ăn hồi máu.
- Mỗi class mặc định có 4–5 kỹ năng; hiệu ứng cast, sức mạnh theo level và thông số chiến đấu có thể cấu hình trong `armies.yml`.
- Bổ sung `VOLLEY` cho Xạ Thủ, `POWER_STRIKE`/`TAUNT` cho Vệ Binh và `CLEANSE_OWNER` cho Y Sư; cấu hình riêng cooldown, sát thương, số/tốc độ/độ tỏa tên, bán kính và danh sách hiệu ứng.
- Vũ khí mặc định được ghi vào trang bị ngay lúc triệu hồi; gear đã tùy chỉnh của người chơi không bị ghi đè.
- Thêm ba class lính: Cuồng Chiến (`berserker`), Trinh Sát (`ranger`) và Thánh Kỵ (`paladin`), mỗi class có 4–5 skill, vũ khí, chỉ số và chi phí nâng bậc riêng trong `armies.yml`.
- Thêm `tooltips.yml` với nhắc nhở định kỳ theo thứ tự hoặc ngẫu nhiên; tinh chỉnh AI vẫn ở `armies.yml`.
- Đặt tỉ lệ token boss mặc định 30% và thêm dòng EXP cơ bản riêng cho từng boss.

### Changed
- Bỏ dòng chat mô tả tác động/cách phòng tránh thiên tai; thông báo world bị ảnh hưởng vẫn giữ.
- Lính đánh thuê bỏ qua sát thương môi trường và không nhận mục tiêu khuất tầm nhìn.

## [v1.17.0] - 2026-10-13

### Added
- Thêm menu chức năng mở bằng F hoặc `/vnmenu`, menu admin và luồng GUI thuê/cấp lính theo lớp, tier.
- Tách tier triệu hồi khỏi level farm; lính nhận XP khi hạ quái, tăng chỉ số theo level và lưu tiến trình riêng.
- Thêm `/vnarmy remove`, `/vnarmy clear`, `/vnarmy list <user>` và bật/tắt âm thanh riêng từng lính.
- GUI hiển thị HP, giáp, sát thương, skill, thông số và cooldown; chọn tier có tooltip requirement.

### Fixed
- Sửa lệch vị trí click GUI, ngăn gear mất độ bền, chặn lính phản công chủ và loại mục tiêu đang bay.
- Lính bám mục tiêu chủ vừa đánh, ngừng khi mục tiêu chết; tự dịch chuyển theo ngưỡng cấu hình.
- Recall triệu hồi cạnh chủ; damage boss và loot do lính gây ra được tính cho chủ nhân.

### Added
- Boss có thể rơi lệnh triệu hồi lính cấp trùng với cấp boss; bật/tắt và chỉnh tỉ lệ trong `bosses/_settings.yml`.
- Thêm `/vnarmy gui` để xem đội quân, đổi mode, nghỉ/triệu hồi từng lính và mở giao diện trang bị.
- Thêm `/vnarmy give <player> token <level>` và `/vnarmy give <player> army <class> <level>`; lệnh cấp lính trực tiếp vẫn tuân giới hạn quân số.
- Thêm skill lính `RANGED_ATTACK`, `BUFF_ALLIES`, `SHIELD_OWNER`; cooldown, mục tiêu, tầm và hiệu ứng có thể tùy chỉnh theo lớp trong `armies.yml`.

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