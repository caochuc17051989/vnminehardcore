# VnMineHardcore

Plugin tăng độ khó sinh tồn cho Minecraft Paper 26.x. Người chơi không bị ban khi chết.

## Tính năng chính

- ☠ **Tử vong**: lưu thống kê, broadcast và phát âm thanh; không ban người chơi
- ⚔ **Sát thương nhân hệ số** (quái, ngã, lửa, nổ, PvP...)
- 🍔 **Hệ thống đói** (hao hụt nhanh, ngộ độc thức ăn sống, nghẹn khi ăn nhanh)
- 💧 **Hệ thống khát nước** (uống từ nguồn, chai, xô; nước tự nhiên gây độc)
- 🌍 **Môi trường khắc nghiệt** (nhiệt độ, sương mù, chặn ngủ, chóng mặt, sợ hầm)
- 🌋 **Thiên tai tự động** (Blood Moon, Meteor, Storm, Solar Flare, Plague, Tornado, Earthquake, Eclipse và các thiên tai theo dimension)
- ☀️🌙 **Điều khiển ngày/đêm** (cấu hình riêng thời lượng ban ngày và ban đêm)
- ⛏️💥 **Kiểm soát đào quặng** (thay vật phẩm rơi, gãy cúp và nổ theo block/cúp)
- 🏷 **Đổi tên hiển thị sau khi chết** (tùy chỉnh format trong config)

## Lệnh

| Lệnh | Mô tả | Quyền |
|------|-------|-------|
| `/vnstats` | Xem thống kê cá nhân (chỉ người chơi) | `vnmine.hardcore.stats` |
| `/vnhardcore` | Xem trạng thái plugin | `vnmine.hardcore.admin` |
| `/vn death reset <player>` | Reset số lần chết của player về 0 | `vnmine.hardcore.admin` |
| `/vnevent` | Xem danh sách thiên tai | `vnmine.hardcore.admin` |
| `/vnevent <id> <warning-giây> <duration-giây>` | Gọi thiên tai; có gợi ý ID và tham số | `vnmine.hardcore.admin` |
| `/vnboss` | Xem danh sách boss | `vnmine.hardcore.admin` |
| `/vnboss <id> <level> <warning-giây> <duration-giây>` | Gọi boss theo level; có gợi ý ID và tham số | `vnmine.hardcore.admin` |
| `/reward` | Nhận phần thưởng xếp hạng boss đang chờ (cần chỗ trống trong inventory) | Không cần |
| `/scoreboard reset` | Xóa điểm mùa và phần thưởng chờ (bí danh: `/vnscoreboard`, `/sb`) | `vnmine.hardcore.admin` |
| `/vntime set <day\|night> <phút> [world]` | Đặt thời lượng ngày/đêm toàn cục hoặc riêng cho world đang tải | `vnmine.hardcore.admin` |
| `/vnday set <ngày>` | Đặt world hiện tại về bình minh của ngày trong game tương ứng (chỉ người chơi) | `vnmine.hardcore.admin` |
| `/tp <user> <world>` | Đưa người chơi đang trực tuyến đến điểm spawn của world | `vnmine.hardcore.admin` |
| `/vnspawn set` | Đặt spawn của world hiện tại tại vị trí và hướng đang đứng | `vnmine.hardcore.admin` |
| `/vnspawn go <world>` | Dịch chuyển bản thân đến spawn của world đang tải | `vnmine.hardcore.admin` |
| `/vnspawn protect <X>` | Bảo vệ bán kính tròn X/Z quanh vị trí hiện tại, xuyên suốt độ cao world | `vnmine.hardcore.admin` |
| `/vnspawn list` | Liệt kê ID, world, tọa độ tâm và bán kính các khu bảo vệ | `vnmine.hardcore.admin` |
| `/vnspawn remove <ID>` | Xóa khu vực bảo vệ theo ID | `vnmine.hardcore.admin` |
| `/vnevent stop` | Dừng ngay thiên tai đang cảnh báo hoặc đang diễn ra | `vnmine.hardcore.admin` |
| `/vnrespawn` | Chọn hồi sinh khi đang chờ sau khi chết | Không cần |
| `/vnleave` | Thoát máy chủ khi đang chờ sau khi chết | Không cần |
| `/fly` | Bật/tắt bay trong Survival (chỉ người chơi) | `vnmine.hardcore.admin` |
| `/nodame` | Bật/tắt miễn sát thương để thử boss (chỉ người chơi) | `vnmine.hardcore.admin` |
| `/spawn` | Về điểm spawn world hiện tại (chỉ người chơi) | `vnmine.hardcore.admin` |
| `/gm0`, `/gm1`, `/gm2`, `/gm3` | Chuyển game mode (chỉ người chơi) | `vnmine.hardcore.admin` |
| `/vnreload` | Tải lại config và các hệ thống | `vnmine.hardcore.admin` |
| `/vnhelp` | Hiển thị hướng dẫn tất cả lệnh | `vnmine.hardcore.stats` |
| `/vn help` | Hiển thị hướng dẫn các lệnh `/vn` | `vnmine.hardcore.stats` |

Ví dụ gọi boss Wither level 3, cảnh báo 30 giây, tồn tại 120 giây:
`/vnboss wither 3 30 120`.
Các lệnh có tham số hỗ trợ nhấn Tab để gợi ý ID, tên người chơi, world, ID
bảo vệ và các giá trị phổ biến cho từng vị trí tham số. `/vnspawn protect X`
dùng bán kính ngang X/Z tính từ block hiện tại và bảo vệ ở mọi độ cao; dữ liệu
được lưu trong `plugins/VnMineHardcore/spawns-protection.yml`.

## Cấu hình (config.yml)

### Rename - Đổi tên hiển thị sau khi chết

```yaml
rename:
  # Bật/tắt tự động đổi tên hiển thị sau khi chết
  enabled: true

  # Định dạng tên hiển thị.
  # Các placeholder có sẵn:
  #   <name>   - Tên gốc của người chơi (ví dụ: hoanbh)
  #   <count>  - Số lần chết (số thường, không padding, ví dụ: 1, 15, 200)
  #   000      - Dãy số 0: đại diện cho số lần chết có padding (thêm số 0 ở đầu).
  #              Số lượng chữ số 0 = độ dài tối thiểu của số.
  #              Ví dụ với số lần chết = 3:
  #                000 -> 003
  #                0000 -> 0003
  #                00 -> 03
  #              Ví dụ với số lần chết = 15:
  #                000 -> 015
  #                0000 -> 0015
  #
  # Ví dụ các định dạng mẫu:
  #   "<name> đã chết 000 lần"  → hoanbh đã chết 001 lần
  #   "<name>_000"              → hoanbh_001
  #   "000 | <name>"            → 001 | hoanbh
  #   "<name> (<count>)"        → hoanbh (1)
  #   "0000"                    → 0001 (chỉ hiện số)
  name-structure: "<name> ☠ <count>"
```

**Lưu ý:** Chức năng này chỉ thay đổi **tên hiển thị** trong chat và tab list (khi bấm phím Tab), **không** thay đổi tên thật của người chơi (`player.getName()`).

### Death & Respawn - Hồi sinh sau khi chết

Sau khi chết, người chơi có thể chọn hồi sinh hoặc thoát game bằng hai nút chat
hoặc lệnh `/vnrespawn` và `/vnleave`. Sau khi chọn hồi sinh,
`death.respawn-protection` mặc định chờ 10 giây; trong thời gian này người chơi
bị giới hạn trong bán kính 10 block quanh vị trí hồi sinh và không thể tương
tác, mở/sửa inventory, nhặt hoặc thả đồ, hay tấn công. Tiêu đề màn hình đếm
ngược cho biết khi nào các thao tác được mở lại. Người chơi được miễn nhiễm
sát thương trong lúc chờ và thêm số giây đặt ở
`death.respawn-protection.damage-immunity-seconds` sau khi được tự do tương
tác. Sau khi hết thời gian giới hạn di chuyển/tương tác, người chơi được di
chuyển tự do ngay cả khi miễn nhiễm sát thương vẫn còn. Quái vật không thể
chọn họ làm mục tiêu trong toàn bộ thời gian miễn nhiễm. Chạy `/vnreload` sau
khi thay đổi các thiết lập này.

```yaml
death:
  respawn-protection:
    enabled: true
    delay-seconds: 10
    movement-radius-blocks: 10
    damage-immunity-seconds: 10
```

### Grave structures - Cấu trúc mộ

`graves-structure.yml` chứa cả cấu trúc `full` và `mini`. Đặt
`mini-graves: true` để dùng mộ mini, hoặc `false` để dùng mộ full. Khi nâng
cấp, plugin tự bổ sung các cấu trúc/khóa còn thiếu vào file hiện có và giữ
nguyên các giá trị tùy chỉnh. Bia mộ full dùng `PLAYER_WALL_HEAD` hướng về thân
mộ để đầu dựng thẳng trên mặt bia.

### Admin testing

`/fly` bật/tắt bay khi người quản trị vẫn ở Survival. `/nodame` bật/tắt miễn
nhiễm sát thương cho người chạy lệnh; quái vẫn có thể chọn người đó làm mục tiêu.
Trạng thái `/nodame` sẽ tự tắt khi người chơi thoát máy chủ.

### Spawn protection

`/vnspawn protect <X>` tạo khu vực hình tròn bán kính X block theo mặt phẳng
X/Z quanh block hiện tại; khu vực áp dụng ở mọi độ cao nhưng chỉ trong world
được tạo. Người chơi không thể phá block bên trong. Dữ liệu và bộ đếm ID được
lưu tại `plugins/VnMineHardcore/spawns-protection.yml`, nên vẫn còn sau khi
khởi động lại.

```text
/vnspawn set
/vnspawn go world_the_end
/vnspawn protect 25
/vnspawn list
/vnspawn remove 1
/tp Steve world_the_end
/vnevent stop
```

Lệnh `/tp <user> <world>` đưa người chơi đang trực tuyến đến spawn của world.
`/vnspawn set` đặt spawn của world hiện tại theo vị trí và hướng người gọi;
`/vnspawn go <world>` đưa người gọi đến spawn của world đang tải. Tên world
được gợi ý bằng Tab.
`/vnevent stop` hủy cảnh báo hoặc thiên tai đang chạy, dọn các task/thực thể
do thiên tai tạo và khôi phục thời tiết, thời gian cùng hiệu ứng đang được theo
dõi. Các lệnh quản trị này cần `vnmine.hardcore.admin`.

### Thiên tai Chorus Explosion

Chorus Explosion mặc định tắt (`enabled: false`) trong
`plugins/VnMineHardcore/disasters/chorus-explosion.yml`. Khi được bật, điều
kiện `worlds` và bộ lọc trên từng action hỗ trợ tên world thật (ví dụ
`world_the_end`) cũng như tên dimension (ví dụ `the_end`); cấu hình mặc định
chỉ cho phép thiên tai này ảnh hưởng `world_the_end`.

### Boss level & skills

`bosses/_settings.yml` configures level ranges by in-game day. The applicable
threshold is the last `min-day` not greater than the current day, and the boss
randomly chooses a level inside that threshold's range. In each boss file,
`hp-by-level` maps levels to explicit maximum HP; missing levels use the nearest
lower configured level, then scale the base `hp` by 1.5 per additional level.
Skill sections under `skills` can be enabled individually and tune their
base damage, projectile count multiplier, cooldown, area, velocity, and spread.
Configured skill damage scales by boss level; projectile skills use
`boss-level * multiply` projectiles, capped at 40 per cast.
The creeper's `charged-explosion` skill calls a visual lightning strike,
damages the boss and nearby living entities using `damage` and `radius`, then
keeps the creeper charged for `duration-seconds`. Its explosion power is
configured separately for normal and charged states; boss creeper explosions
do not break blocks and the boss survives each explosion. Values are in
`bosses/creeper_boss.yml`.
Available skill IDs are `arrow-barrage` (skeleton), `fireball-volley`
(dragon/ghast), `stomp` (giant), `ground-slam` (zombie), `charged-explosion`
(creeper), `shadow-strike` (enderman), `dive-bomb` (phantom), `charge`
(ravager), `venom-web` (spider), `shockwave` (vindicator), `potion-volley`
(witch), and `skull-volley` (wither).

```yaml
# bosses/_settings.yml
level-progression:
  enabled: true
  thresholds:
    - {min-day: 0, min-level: 1, max-level: 1}
    - {min-day: 50, min-level: 2, max-level: 3}
    - {min-day: 100, min-level: 3, max-level: 4}

# Example in an individual bosses/<boss>.yml
hp-by-level:
  1: 120
  2: 180
skills:
  charged-explosion:
    enabled: true
    damage: 12.0
    radius: 6.0
    duration-seconds: 5
    explosion-power: 3.0
    charged-explosion-power: 6.0
    explosion-cooldown-seconds: 4
  arrow-barrage:
    enabled: true
    damage: 6.0
    multiply: 5
    cooldown-seconds: 12
    radius: 7.0
    velocity: 1.5
    spread: 1.4
```

### Loot boss theo level và bảng xếp hạng

Mỗi file `bosses/<id>.yml` có mục `level-drops` riêng cho boss đó, đầy đủ từ
level 1 đến level 10. Mỗi cấp có hai di vật hiếm, truyền thuyết tối đa 4 dòng
và số dòng enchant tăng theo level; có thể tùy chỉnh `material`, `name`, `lore`,
`min-amount`, `max-amount`, `chance`, `enchantments` (hỗ trợ over-enchant)
hoặc `book-enchantments` (sách có enchant để áp dụng vào item). Boss tự nhiên
có thể lên level 10 từ ngày 400 theo `bosses/_settings.yml`; `/vnboss` vẫn cho
phép gọi boss ở level được chọn. Khi boss xuất hiện, server broadcast loot có
thể rơi ở level đó và tỉ lệ từng món. Mọi boss tồn tại mặc định 20 phút;
bộ đếm roll boss kế tiếp tạm dừng trong khi cảnh báo hoặc boss đang hoạt động.

`scoreboard.yml` cấu hình chu kỳ reset (`reset.every-game-days`), sidebar,
thời gian title top 1, chu kỳ nhắc và quà của từng hạng top 1–10. Top 5 người
đánh boss nhận tổng cộng `10 × level` điểm, chia theo tỉ lệ damage. Điểm và quà
đang chờ được lưu trong `scoreboard-data.yml`. Người chơi nhận quà bằng
`/reward`; nếu inventory không đủ chỗ, quà vẫn chờ và có thể nhận lại sau khi
dọn túi. Chạy `/vnreload` để nạp cấu hình.

### Disasters - Thiên tai

Thiên tai không nằm trong `config.yml`. Cấu hình chung ở
`plugins/VnMineHardcore/disasters/_settings.yml`; mỗi loại thiên tai có file
riêng `plugins/VnMineHardcore/disasters/<id>.yml`.

Trong từng file thiên tai, `chat-description` là nội dung tác động/gợi ý
phòng tránh được hiển thị trong chat lúc cảnh báo và lúc bắt đầu. Danh sách
world hiển thị được lấy từ `conditions.worlds`, chuyển thành tên các world
đang tải; cùng điều kiện đó cũng giới hạn những world mà action của sự kiện
có thể tác động. Có thể chỉnh cả hai mục trong file riêng của từng thiên tai.
Các file cũ được bổ sung khóa còn thiếu nhưng giữ nguyên giá trị tùy chỉnh.

### Time Control - Thời lượng ngày và đêm

Trong `config.yml`, `time-control.global.day-duration-minutes` và
`time-control.global.night-duration-minutes` đặt thời lượng toàn cục theo phút
thực tế cho mọi world trừ Nether. Có thể ghi đè riêng trong
`time-control.worlds.<world-name>`; mỗi giá trị bị bỏ trống sẽ kế thừa giá trị
global tương ứng. Ví dụ:

```yaml
time-control:
  enabled: true
  global:
    day-duration-minutes: 10
    night-duration-minutes: 10
  worlds:
    world_resource:
      day-duration-minutes: 6
    world_the_end:
      night-duration-minutes: 20
```

Thiết lập mặc định 10 phút cho mỗi pha giữ tốc độ mặc định của Minecraft.
Đặt `time-control.enabled: false` để giữ chu kỳ ngày/đêm vanilla. Có thể đổi
hai giá trị global bằng `/vntime set day <phút>` và
`/vntime set night <phút>`; thay đổi được lưu ngay vào `config.yml`. Các world
đã có override riêng vẫn giữ thời lượng đã cấu hình. Bỏ tham số world trong
lệnh để chỉnh global; thêm tên world đang tải để chỉ chỉnh world đó:
`/vntime set day 6 world_resource`.
Lệnh `/vnday set <ngày>` chỉ dành cho người chơi và đặt đồng hồ thời gian đầy
đủ của world hiện tại về bình minh ngày được chọn (ngày 0 là lúc tạo world).
Nether luôn được loại khỏi chức năng quản lý thời gian: plugin không thay đổi
thời gian hoặc gamerule của world Nether. Các world khác không có world clock
sẽ được bỏ qua và ghi cảnh báo.

### Phiên bản tương thích

Dự án nhắm tới Paper 26.x và Java 25+ cho server. Mã nguồn hiện biên dịch với
Paper API 1.21.4 để giữ tương thích API như mô tả trong `BUILD_GUIDE.md`.
`api-version` trong `plugin.yml` hiện vẫn là `1.21`.

`config.yml` và các file boss/disaster giữ nguyên giá trị tùy chỉnh khi plugin
cập nhật. Vì vậy, nếu máy chủ đã có file
`plugins/VnMineHardcore/disasters/chorus-explosion.yml` từ phiên bản trước,
hãy đặt `enabled: false` thủ công nếu muốn áp dụng mặc định mới; giá trị
`enabled: true` được admin cấu hình có chủ ý sẽ không bị ghi đè.

### Block Drop, Tool Break & Explosion - Hiệu ứng khi đào

Phần `block-drop-settings` cho phép đặt tỷ lệ 0-100 riêng cho từng block và
cúp. Ba công tắc cấp cao (`replace-enabled`, `break-enabled`,
`explode-enabled`) có thể tắt riêng từng loại hiệu ứng; từng rule cũng có
công tắc `enabled`. `drop-replace.replace-block` là vật phẩm thay cho drop
vanilla khi trúng tỷ lệ. `explode` hỗ trợ cấu hình sức mạnh, phá block, lửa,
sát thương người chơi, thông báo và tỷ lệ. Chạy `/vnreload` sau khi sửa config.

## Yêu cầu

- Paper 26.x
- Java 25+ (runtime server)
- Paper API 1.21.4 dùng làm compile baseline

## Cài đặt

1. Tải file `.jar` từ bản phát hành (Release)
2. Đặt file vào thư mục `plugins/` của server
3. Khởi động lại server
4. Chỉnh sửa `plugins/VnMineHardcore/config.yml` theo nhu cầu
5. Gõ `/vnreload` để áp dụng thay đổi

## Changelog

### v1.9.0
- Thêm `/vnspawn set` để đặt spawn world hiện tại và `/vnspawn go <world>` để tới spawn của world đang tải; hỗ trợ gợi ý Tab tên world.

### v1.8.0
- Thêm `/tp <user> <world>` và nhóm lệnh `/vnspawn protect|list|remove`; lưu khu bảo vệ trong `spawns-protection.yml`.
- Thêm `/vnevent stop` để dừng cảnh báo hoặc thiên tai đang hoạt động.
- Động đất hất người chơi ngoài trời theo phương ngang và dọc; lực có thể chỉnh trong file thiên tai.
- Mỗi file thiên tai có `chat-description`; chat lúc cảnh báo/bắt đầu hiển thị tác động, gợi ý phòng tránh và world bị ảnh hưởng.
- Sửa so khớp world/dimension cho điều kiện thiên tai; Chorus Explosion tắt mặc định.
- Cập nhật phiên bản Paper mục tiêu trong tài liệu lên 26.x.

### v1.7.0
- `/vntime set <day|night> <phút> [world]`: bỏ `[world]` để đổi mặc định toàn cục,
  hoặc ghi tên world đang tải để đặt override riêng.
- `/vnday set <ngày>` đặt world hiện tại về bình minh của ngày đó; ngày 0 là lúc
  thế giới được tạo.
- Mỗi boss có loot quý riêng cho level 1–10, lore truyền thuyết tối đa 4 dòng
  và số dòng enchant tăng dần theo level; HP cũng được cấu hình đến level 10.
  Boss tự nhiên có thể đạt level 10 từ ngày 400.
- Thời gian tồn tại mặc định của mọi boss là 20 phút; không roll boss mới trong
  thời gian đang cảnh báo hoặc boss khác còn hoạt động.

### v1.6.0
- Thêm `/scoreboard reset` để xóa điểm mùa và phần thưởng boss đang chờ nhận.
- Thêm `/vntime set day <phút>` và `/vntime set night <phút>`; tự lưu cấu hình
  global và giữ nguyên override riêng từng world.
- `/vnhelp` liệt kê toàn bộ lệnh plugin; nhấn Tab để gợi ý tham số.

## Build từ mã nguồn

```bash
cd VnMineHardcore
mvn clean package
```

File `.jar` sẽ được tạo trong thư mục `target/`.