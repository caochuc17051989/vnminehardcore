# VnMineHardcore

Plugin tăng độ khó sinh tồn cho Minecraft Paper 1.21.4. Người chơi không bị ban khi chết.

## Tính năng chính

- ☠ **Tử vong**: lưu thống kê, broadcast và phát âm thanh; không ban người chơi
- ⚔ **Sát thương nhân hệ số** (quái, ngã, lửa, nổ, PvP...)
- 🍔 **Hệ thống đói** (hao hụt nhanh, ngộ độc thức ăn sống, nghẹn khi ăn nhanh)
- 💧 **Hệ thống khát nước** (uống từ nguồn, chai, xô; nước tự nhiên gây độc)
- 🌍 **Môi trường khắc nghiệt** (nhiệt độ, sương mù, chặn ngủ, chóng mặt, sợ hầm)
- 🌋 **Thiên tai tự động** (Blood Moon, Meteor, Storm, Solar Flare, Plague, Tornado, Eclipse)
- ☀️🌙 **Điều khiển ngày/đêm** (cấu hình riêng thời lượng ban ngày và ban đêm)
- ⛏️💥 **Kiểm soát đào quặng** (thay vật phẩm rơi, gãy cúp và nổ theo block/cúp)
- 🏷 **Đổi tên hiển thị sau khi chết** (tùy chỉnh format trong config)

## Lệnh

| Lệnh | Mô tả | Quyền |
|------|-------|-------|
| `/vnstats` | Xem thống kê cá nhân (số lần chết, thời gian sống, quái giết) | `vnmine.hardcore.stats` |
| `/vnhardcore` | Xem trạng thái plugin | `vnmine.hardcore.admin` |
| `/vn death reset <player>` | Reset số lần chết của player về 0 | `vnmine.hardcore.admin` |
| `/vnevent` | Gọi thiên tai thủ công (xem danh sách) | `vnmine.hardcore.admin` |
| `/vnevent <id> <warning(s)> <duration(s)>` | Gọi thiên tai cụ thể | `vnmine.hardcore.admin` |
| `/vnboss <id> <level> <warning(s)> <duration(s)>` | Gọi boss theo level; nhấn Tab khi nhập ID để xem/gợi ý boss | `vnmine.hardcore.admin` |
| `/fly` | Bật/tắt bay khi vẫn ở chế độ Sinh tồn | `vnmine.hardcore.admin` |
| `/nodame` | Miễn sát thương nhưng vẫn có thể bị quái chọn làm mục tiêu (kiểm tra boss) | `vnmine.hardcore.admin` |
| `/vnreload` | Tải lại file cấu hình config.yml | `vnmine.hardcore.admin` |
| `/vnhelp` | Hiển thị hướng dẫn tất cả lệnh | `vnmine.hardcore.stats` |

Ví dụ gọi boss Wither level 3, cảnh báo 30 giây, tồn tại 120 giây:
`/vnboss wither 3 30 120`.

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
  arrow-barrage:
    enabled: true
    damage: 6.0
    multiply: 5
    cooldown-seconds: 12
    radius: 7.0
    velocity: 1.5
    spread: 1.4
```

### Disasters - Thiên tai

```yaml
disasters:
  # [VI] Bật hệ thống thiên tai tự động
  # [EN] Enable automatic disaster system
  enabled: true

  # [VI] Thời gian tối thiểu giữa các lần kiểm tra thiên tai (giây)
  # [EN] Minimum seconds between disaster checks
  min-interval-seconds: 1200

  # [VI] Thời gian cảnh báo trước khi thiên tai xảy ra (giây)
  # [EN] Warning time in seconds before a disaster strikes
  warning-seconds: 60

  # [VI] Các thiên tai được cấu hình theo object
  # [EN] Disasters configured as objects
  types:
    # [VI] Blood Moon - Trăng máu, tăng độ khó sinh sản quái vật
    # [EN] Blood Moon - Blood red moon, increases mob spawn difficulty
    blood-moon:
      # [VI] Bật/tắt thiên tai này
      # [EN] Enable/disable this disaster
      enabled: true
      # [VI] Tên hiển thị
      # [EN] Display name
      name: "Trăng Máu"
      # [VI] Thời gian kéo dài (giây)
      # [EN] Duration in seconds
      duration-seconds: 300
      # [VI] Tỷ lệ spawn quái vật tăng gấp (1.0 = tắt, 2.0 = gấp đôi)
      # [EN] Mob spawn rate multiplier (1.0 = off, 2.0 = double)
      spawn-multiplier: 2.0

    # [VI] Mega Storm - Bão lớn, gió mạnh và sét
    # [EN] Mega Storm - Large storm with strong winds and lightning
    mega-storm:
      enabled: true
      name: "Bão Rực Rỡ"
      duration-seconds: 180
      # [VI] Tần suất sét (giây)
      # [EN] Lightning frequency (seconds)
      lightning-frequency-seconds: 5
      # [VI] Sát thương từ sét (tim)
      # [EN] Lightning damage (hearts)
      lightning-damage: 4.0

    # [VI] Solar Flare - Tia nắng, gây thiêu đốt
    # [EN] Solar Flare - Solar rays causing burning
    solar-flare:
      enabled: true
      name: "Tia Nắng"
      duration-seconds: 120
      # [VI] Sát thương mỗi giây (tim)
      # [EN] Damage per second (hearts)
      damage-per-second: 1.0
```

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
Đặt `time-control.enabled: false` để giữ chu kỳ ngày/đêm vanilla.
Nether luôn được loại khỏi chức năng quản lý thời gian: plugin không thay đổi
thời gian hoặc gamerule của world Nether. Các world khác không có world clock
sẽ được bỏ qua và ghi cảnh báo.

### Tương thích Paper 26

Đã biên dịch và khởi động plugin thành công trên Paper 26.1.2 build 74,
26.2 build 133 và 26.3 build 175. Paper 26 yêu cầu Java 25 trở lên.

### Block Drop, Tool Break & Explosion - Hiệu ứng khi đào

Phần `block-drop-settings` cho phép đặt tỷ lệ 0-100 riêng cho từng block và
cúp. Ba công tắc cấp cao (`replace-enabled`, `break-enabled`,
`explode-enabled`) có thể tắt riêng từng loại hiệu ứng; từng rule cũng có
công tắc `enabled`. `drop-replace.replace-block` là vật phẩm thay cho drop
vanilla khi trúng tỷ lệ. `explode` hỗ trợ cấu hình sức mạnh, phá block, lửa,
sát thương người chơi, thông báo và tỷ lệ. Chạy `/vnreload` sau khi sửa config.

## Yêu cầu

- Minecraft Paper 1.21+
- Java 21+

## Cài đặt

1. Tải file `.jar` từ bản phát hành (Release)
2. Đặt file vào thư mục `plugins/` của server
3. Khởi động lại server
4. Chỉnh sửa `plugins/VnMineHardcore/config.yml` theo nhu cầu
5. Gõ `/vnreload` để áp dụng thay đổi

## Build từ mã nguồn

```bash
cd VnMineHardcore
mvn clean package
```

File `.jar` sẽ được tạo trong thư mục `target/`.