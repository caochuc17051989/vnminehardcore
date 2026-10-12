# Changelog

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