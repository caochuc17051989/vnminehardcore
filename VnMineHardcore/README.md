# VnMineHardcore

Plugin tăng độ khó sinh tồn cho Minecraft Paper 26.3. Người chơi không bị ban khi chết.

Yêu cầu runtime: Java 25+ và ProtocolLib `5.5.0-SNAPSHOT` dev-build có hỗ trợ Minecraft 26.3 ([official builds](https://github.com/dmulloy2/ProtocolLib/releases/tag/dev-build)). Plugin không tải hoặc đóng gói ProtocolLib thay server.

## Tính năng chính

- ☠ **Tử vong**: lưu thống kê, broadcast và phát âm thanh; không ban người chơi
- 🤝 **Lính đánh thuê**: Shift + chuột phải vào lính của bạn để mở kho trang bị ngay lập tức
- ⚔ **Sát thương nhân hệ số** (quái, ngã, lửa, nổ, PvP...)
- 🍔 **Hệ thống đói** (hao hụt nhanh, ngộ độc thức ăn sống, nghẹn khi ăn nhanh)
- 💧 **Hệ thống khát nước** (uống từ nguồn, chai, xô; nước tự nhiên gây độc)
- 🌍 **Môi trường khắc nghiệt** (nhiệt độ, sương mù, chặn ngủ, chóng mặt, sợ hầm, giới hạn mưa)
- 🌋 **Thiên tai tự động** (Blood Moon, Meteor, Storm, Solar Flare, Plague, Tornado, Earthquake, Eclipse và các thiên tai theo dimension)
- ☀️🌙 **Điều khiển ngày/đêm** (cấu hình riêng thời lượng ban ngày và ban đêm)
- ⛏️💥 **Kiểm soát đào quặng** (thay vật phẩm rơi, gãy cúp và nổ theo block/cúp)
- 🏷 **Đổi tên hiển thị sau khi chết** (tùy chỉnh format trong config)

## Lệnh

| Lệnh | Mô tả | Quyền |
|------|-------|-------|
| `/vnstats` | Xem thống kê cá nhân (chỉ người chơi) | `vnmine.command.stats` |
| `/vnhardcore` | Xem trạng thái plugin | `vnmine.command.status` |
| `/vn death reset <player>` | Reset số lần chết của player về 0 | `vnmine.command.death.reset` |
| `/vnevent [<id> <warning-giây> <duration-giây>\|stop]` | Xem, gọi hoặc dừng thiên tai | `vnmine.command.disasters` |
| `/vnboss <id> <level> <warning-giây> <duration-giây>` | Gọi boss theo level | `vnmine.command.bosses` |
| `/vnboss kill` | Kết thúc boss đang hoạt động, không điểm và không loot | `vnmine.command.bosses` |
| `/reward` | Nhận phần thưởng xếp hạng boss đang chờ (cần chỗ trống trong inventory) | `vnmine.command.reward` |
| `/scoreboard reset` | Xóa điểm mùa và phần thưởng chờ (bí danh: `/vnscoreboard`, `/sb`) | `vnmine.command.scoreboard.reset` |
| `/vntime set <day\|night> <phút> [world]` | Đặt thời lượng ngày/đêm toàn cục hoặc riêng cho world đang tải | `vnmine.command.time.manage` |
| `/vnday set <ngày>` | Đặt world hiện tại về bình minh của ngày trong game tương ứng (chỉ người chơi) | `vnmine.command.day.manage` |
| `/tp <user> <world>` | Đưa người chơi đang trực tuyến đến điểm spawn của world | `vnmine.command.teleport.other` |
| `/vnspawn set\|go\|protect\|list\|remove` | Quản lý spawn và vùng bảo vệ | `vnmine.command.spawn.manage` |
| `/vnrespawn` | Chọn hồi sinh khi đang chờ sau khi chết | `vnmine.command.respawn` |
| `/vnleave` | Thoát máy chủ khi đang chờ sau khi chết | `vnmine.command.leave` |
| `/vndeathhistory [player]` | Mở lịch sử chết của người chơi online; admin có thể chọn snapshot và khôi phục sau bước xác nhận | `vnmine.command.death.history` |
| `/quatanthu` | Xin túi quà tân thủ nếu chưa nhận trước đây và chưa sở hữu lính đánh thuê | Không yêu cầu quyền riêng |
| `/fly`, `/nodame`, `/spawn` | Tiện ích quản trị dành cho người chơi | `vnmine.command.fly`, `.nodame`, `.spawn` |
| `/gm0`, `/gm1`, `/gm2`, `/gm3` | Chuyển game mode (chỉ người chơi) | `vnmine.command.gamemode` |
| `/vnreload` | Tải lại config và các hệ thống | `vnmine.command.reload` |
| `/vnhelp`, `/vn help` | Hiển thị hướng dẫn lệnh | `vnmine.command.help`, `.utility` |
| `/vnmenu` hoặc phím `F` | Mở menu chức năng; phím F thay thao tác đổi tay phụ | Không yêu cầu quyền riêng |
| `/vngroup ...` | Tạo/xóa nhóm, xem nhóm, cấp/thu hồi quyền nhóm | `vnmine.command.groups.manage` |
| `/vnuser ...` | Xem/gán nhóm, cấp/thu hồi quyền cá nhân, reset người chơi | `vnmine.command.users.manage` |
| `/vnarmy ...` | Thuê/quản lý lính, đổi chế độ AI và trang bị | `vnmine.command.army` |

Ví dụ gọi boss Wither level 3, cảnh báo 30 giây, tồn tại 120 giây:
`/vnboss wither 3 30 120`.
Các lệnh có tham số hỗ trợ nhấn Tab để gợi ý ID, tên người chơi, world, ID
bảo vệ và các giá trị phổ biến cho từng vị trí tham số. `/vnspawn protect X`
dùng bán kính ngang X/Z tính từ block hiện tại và bảo vệ ở mọi độ cao; dữ liệu
được lưu trong `plugins/VnMineHardcore/spawns-protection.yml`.

### Nhóm và quyền (`permission.yml`)

Mặc định, nhóm `user` chỉ có các lệnh cơ bản; nhóm `admin` có toàn bộ quyền
plugin. Dùng `/vngroup list`, `/vngroup info <group>`,
`/vngroup create <group>`, `/vngroup delete <group>` và
`/vngroup permission <add|remove> <group> <permission>` để quản lý nhóm.
Dùng `/vnuser info <player>`, `/vnuser group <player> <group>`,
`/vnuser permission <add|remove> <player> <permission>` hoặc
`/vnuser reset <player>` để quản lý người chơi. Người chơi phải online hoặc đã
có bản ghi trong file để được chọn; tên UUID ổn định, còn tên chỉ dùng để tra cứu.

Các permission node được tách theo chức năng và liệt kê trong `plugin.yml`.
Ví dụ: `vnmine.command.disasters`, `vnmine.command.spawn.manage`,
`vnmine.command.users.manage`; node bypass vùng bảo vệ là
`vnmine.spawnprotection.bypass`. OP và console luôn có toàn quyền.
`vnmine.command.army` mặc định cấp cho người chơi; quyền
`vnmine.command.army.admin` mặc định chỉ dành cho OP để cấp/xóa và xem quân đội
của người khác. `F` mở menu chính thay thao tác đổi tay phụ; cấu hình tại
`menus.main` và `menus.admin` trong `config.yml`.
Menu admin có các thao tác trực tiếp cho quân đội, thiên tai/boss, thời gian/ngày,
spawn protection, teleport, tiện ích người chơi, nhóm/quyền và lịch sử chết.
Khôi phục inventory và xóa lính qua GUI đều có bước xác nhận.
Sau khi chỉnh `permission.yml` thủ công, chạy `/vnreload`.
Nhóm/quyền không hỗ trợ wildcard, phủ định hoặc kế thừa; quyền nhóm và quyền
cá nhân được cộng lại, vì vậy xóa quyền cá nhân không thể phủ định quyền được
cấp bởi nhóm. Nhóm mặc định áp dụng cho người mới và `/vnuser reset`; đổi
`default-group` không tự chuyển người đã có nhóm được lưu.

### Lính đánh thuê (`armies.yml`)

`plugins/VnMineHardcore/armies.yml` là file tinh chỉnh AI và cấu hình lính: giới
hạn đội quân (mặc định 10), tầm tìm mục tiêu, chu kỳ AI, chỉ số, kỹ năng, EXP,
thời gian hồi phục, vũ khí ban đầu và vật phẩm nâng bậc. Mười class mặc định:
Xạ Thủ (`archer`), Vệ Binh (`guardian`), Y Sư (`medic`), Cuồng Chiến
(`berserker`), Trinh Sát (`ranger`), Thánh Kỵ (`paladin`), Pháp Sư Lửa
(`pyromancer`), Tu sĩ rừng (`druid`), Sát Thủ (`assassin`) và Pháp Sư Băng
(`frost_mage`); mỗi class có 4–6
skill được cấu hình riêng. Lính chọn vũ khí tốt nhất giữa tay chính và 36 ô
kho; chỉ dùng vũ khí mặc định nếu kho không có món dùng được. Gear được giữ qua
nghỉ, chết và triệu hồi lại. Mô tả skill và action bar dùng tên tiếng Việt.
Xạ Thủ có `VOLLEY` (số tên, tốc độ, độ tỏa, sát thương); Trinh Sát dùng nỏ,
phát hiện mục tiêu trong 48 block và bắn xa 32 block theo mặc định (gấp đôi
Xạ Thủ), tiêu thụ đạn trong kho nếu có và dùng Spectral Arrow miễn phí khi hết
đạn. Skill `ILLUMINATION` bắn loạt tên lửa lên trời, tạo đuốc tạm quanh mục tiêu
trong 30 giây; số tên tăng thêm mỗi 5 level. Các thông số nằm trong
`classes.ranger` và `classes.ranger.skill-settings.ILLUMINATION`. Vệ Binh có
`POWER_STRIKE` (sát thương cộng thêm) và `TAUNT` (bán kính kéo quái khỏi chủ);
Y Sư có `CLEANSE_OWNER` (cooldown và danh sách hiệu ứng được gỡ). Tất cả thông
số nằm trong `classes.<class>.skill-settings.<SKILL>` và có thể chỉnh riêng.
Mặc định lính và VEX không gây sát thương lên người chơi hoặc quân của người
chơi khác chỉ vì phát hiện họ hay bị họ tấn công. Ngoại lệ: sau khi chủ nhân
trực tiếp đánh đúng mục tiêu, lính có thể tấn công mục tiêu đó trong 15 giây.
Đặt `pvp.allow-owner-commanded-targets: false` trong `armies.yml` để chặn hoàn
toàn sát thương PvP từ lính và VEX.
Scaling skill chỉnh tại `progression.skill-power-per-level`,
`progression.max-skill-power-multiplier` và
`progression.skill-cooldown-reduction-at-max-level-seconds`.
Cuồng Chiến thiên về áp sát/khiêu khích; Trinh Sát bắn loạt và hỗ trợ chủ;
Thánh Kỵ đỡ đòn; Pháp Sư Lửa gây sát thương/đốt và làm chậm; Tu sĩ rừng khống chế,
hồi máu, thanh tẩy; Sát Thủ hút máu và cuồng nộ khi nguy cấp; Pháp Sư Băng
bắn loạt và làm chậm mục tiêu. Dùng ID class trong lệnh
`/vnarmy hire <loại> <bậc>` hoặc menu GUI.
Tu sĩ rừng gọi tối đa một VEX mỗi lần cast; cứ mỗi 5 level tăng sức chứa thêm
1 VEX, bắt đầu từ 1 ở level 1 (mặc định tối đa 7 ở level 30). Tùy chỉnh giới
hạn và thứ tự ưu tiên AI tại `classes.druid.skill-settings.SUMMON_VEXES` trong
`armies.yml`. VEX ưu tiên mục tiêu Tu sĩ rừng đang đánh, kẻ tấn công Tu sĩ rừng,
kẻ tấn công chủ nhân, rồi đi theo Tu sĩ rừng. Chủ nhân thấy thanh HP riêng của từng VEX dạng ô đầy/rỗng, không kèm số HP;
thanh màu xanh lá có biểu tượng đổi theo trạng thái tấn công, theo Tu sĩ hoặc
đứng yên. Khi không có mục tiêu để đánh, VEX quay về và được giữ trong phạm vi
5 block quanh Tu sĩ rừng bằng cách bay theo từ từ; khi cho Tu sĩ rừng nghỉ, toàn
bộ VEX do lính đó triệu hồi cũng bị xóa. Màu và biểu tượng tùy chỉnh tại `status-display.vex-health-color` và
`status-display.vex-action-icons` trong `armies.yml`. Kỵ sĩ tự lên ngựa plugin quản lý;
slot gear riêng lưu giáp ngựa tốt nhất và
`classes.paladin.horse-health`/`classes.paladin.horse-speed` cấu hình máu/tốc độ
ngựa (tốc độ mặc định bằng mức phi nước đại tối đa). Khi ngựa chết, Kỵ sĩ tiếp
tục đi bộ, ngựa không tự hồi sinh kể cả sau khi khởi động lại; cho Kỵ sĩ nghỉ
rồi xuất chiến lại để tạo ngựa thay thế. Lính sẽ chủ động truy đuổi
mục tiêu mà chủ nhân vừa đánh, kể cả khi mục tiêu ở ngoài tầm nhìn trực tiếp của lính;
lệnh tấn công trực tiếp hết hiệu lực sau 15 giây nếu không được làm mới.
Token phải khớp class và bậc, rồi bị tiêu thụ khi thuê. Token boss có class riêng
qua `army-token-class` trong từng `bosses/<id>.yml`; bậc token bằng level boss.
Token chung từ lệnh admin vẫn dùng được cho mọi class.

```text
/vnarmy gui
/vnarmy list [user]                             # user khác cần quyền admin
/vnarmy hire <loại> <bậc>
/vnarmy recall [ID]
/vnarmy rest [ID]
/vnarmy rename <ID> <Tên mới>
/vnarmy attack | defend | protect
/vnarmy gear <ID>
/vnarmy mute <ID>
/vnarmy remove <ID>
/vnarmy remove <user> <ID>                     # admin
/vnarmy clear <user>                           # admin
/vnarmy give <người chơi> token <bậc>              # admin
/vnarmy give <người chơi> army <loại> <bậc>        # admin
/vnarmy token give <người chơi> <loại> <bậc>       # token theo lớp, tương thích cũ
```

Hồ sơ và trang bị được lưu trong `plugins/VnMineHardcore/mercenaries.yml`.
Lính đang triển khai được gọi lại khi chủ nhân vào game; khi nghỉ, thoát game
hoặc server dừng, entity được thu hồi nhưng hồ sơ/trang bị vẫn giữ. `recall`
đối soát UUID và PersistentData tag trước khi tạo entity thay thế để hạn chế
nhân bản. AI ưu tiên hành động theo `ai.priorities` trong `armies.yml`: ATTACK
ưu tiên mục tiêu chủ đã đánh, mối đe dọa lên chủ, hostile gần, tìm kiếm, quay về
chủ và tuần tra; DEFEND ưu tiên hostile gần, tìm kiếm và tuần tra; PROTECT ưu
tiên áp sát chủ, đánh mob đang nhắm chủ, trả đũa và tìm hostile gần. Thứ tự có
thể chỉnh; AI không chủ động chọn người chơi, lính hoặc đệ của chủ khác.

GUI cho phép chọn lớp và bậc trước khi thuê; tooltip nêu mô tả skill tiếng Việt,
thông số/cooldown, yêu cầu XP/vật liệu và chỉ số HP/giáp/sát thương. Hai thanh
Thanh hiển thị ba dòng chỉ dành cho chủ nhân: vạch HP, `❤: HP hiện tại/tối đa ✦ EXP: hiện tại/yêu cầu`, rồi `[Tên chủ] Tên lính [Bậc X] [Level: Y]`; người chơi khác vẫn
chỉ thấy tên lính. Bật/tắt tại `status-display.enabled`, độ dài vạch HP chỉnh tại
`status-display.bar-length`; `status-display.see-through` điều khiển khả năng nhìn xuyên tường.
`experience-orb-pickup.enabled` và
`experience-orb-pickup.radius-blocks` điều khiển việc thu EXP cho chủ.
Menu đội quân mở chi tiết từng lính, đổi chế
độ, mute, xóa, cho nghỉ/triệu hồi và mở kho 36 ô cùng trang bị. Vũ khí và giáp
được tự nhận diện; vũ khí/giáp của lính không mất độ bền, cung dùng vô hạn loại
mũi tên đặt trong kho và thức ăn được tiêu thụ để hồi máu. Lính lên cấp nhờ hạ
quái hoặc tham gia hạ mục tiêu cùng chủ/đồng đội; chỉ số và sức mạnh skill tăng
theo cấu hình. Sức mạnh skill tăng tuyến tính (mặc định tối đa 2.5 lần), cooldown
giảm đều tối đa 3 giây ở cấp 30. Lính nhặt EXP orb gần nhất cho chủ online; orb
không đủ điều kiện vẫn tồn tại bình thường. Level tối đa là 30. Đạt cấp 30 có thể nhấn **Nâng bậc** trong GUI
chi tiết; mỗi bậc nâng tiêu thụ vật phẩm khai báo ở
`classes.<class>.tier-upgrades.<bậc-đích>.materials`, tối đa thêm 3 bậc so với
bậc gốc khi thuê.

Lính bị hạ sẽ không mất hồ sơ/trang bị mà chuyển sang **Hồi phục** (mặc định
300 giây); `/vnarmy list` hiển thị Xuất chiến/Nghỉ ngơi/Hồi phục cùng bậc, cấp,
EXP hiện tại/yêu cầu và tên riêng. `/vnarmy rename <ID> <Tên mới>` đổi tên.
`recall` và `rest` tác động cả đội; thêm ID để chỉ gọi/nghỉ một lính. Lệnh thuê
liệt kê rõ token, cấp kinh nghiệm và từng vật phẩm đang thiếu.

Token boss có class theo `army-token-class` trong file boss và bậc bằng level
boss, rơi với xác suất mặc định 30% (`bosses/_settings.yml`, khóa
`army-token-drop.chance`) khi có người chơi gây sát thương boss. Mỗi file boss
có `experience` riêng cho EXP cơ bản; EXP tăng theo
level bằng `experience-per-level-multiplier` trong `_settings.yml`. Mọi người
chơi gây sát thương nhận EXP boss; lính đang xuất chiến của họ nhận cùng lượng
EXP. Quy tắc vật phẩm/điểm boss hiện hành vẫn dùng ngưỡng đóng góp riêng.
`give ... army` cấp lính trực tiếp và vẫn tôn trọng giới hạn quân số;
`give ... token` cấp token chung bậc.

Tên hiển thị trên entity dùng tiền tố `[Tên chủ]` xanh lá sáng, đậm với chủ;
người chơi khác thấy tiền tố xanh da trời sáng, đậm. ProtocolLib cung cấp metadata
riêng theo người xem. Dữ liệu
đội quân/kho đồ lưu tại `plugins/VnMineHardcore/mercenaries.yml`. AI không chọn
mục tiêu bị che khuất khỏi tầm nhìn. `ai.priorities` và `ai.wander-radius-blocks`
chỉnh thứ tự hành động/phạm vi tuần tra. Kỵ sĩ sử dụng ngựa do plugin quản lý,
không chiếm mount của người chơi/lính khác; ô gear riêng lưu giáp ngựa tốt nhất.
`classes.paladin.horse-health`/`classes.paladin.horse-speed` chỉnh HP/tốc độ
ngựa. Khi ngựa chết, Kỵ sĩ đi bộ cho đến khi nghỉ và được xuất chiến lại.

`plugins/VnMineHardcore/tooltips.yml` định nghĩa nhắc nhở chức năng định kỳ:
`settings.interval-seconds` đặt khoảng thời gian và `settings.mode` chọn
`random` hoặc `sequential`; các dòng tooltip nằm ở cuối file. Dùng `/vnreload`
sau khi chỉnh cấu hình.

Để tránh xung đột, chỉ dùng một hệ thống làm nguồn cấp quyền cho node
`vnmine.*`; permission từ plugin khác có thể cộng thêm quyền. Chỉ cấp
`vnmine.command.groups.manage`/`vnmine.command.users.manage` cho nhân viên đáng
tin cậy. OP luôn có toàn quyền và không thể bị giới hạn bởi group.

## Cấu hình (config.yml)

Mưa được điều khiển trong `environment.rain-control`: mặc định mỗi lần bắt đầu
mưa có 40% xác suất được chấp nhận; thời lượng tối đa là 5 phút và plugin chọn
ngẫu nhiên từ 1 đến 5 phút để mưa có thể tạnh sớm. Có thể chỉnh
`chance-percent`, `max-duration-minutes`,
`random-duration.enabled` và `random-duration.minimum-minutes`.

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

Lịch sử chết lưu tối đa 10 snapshot đầy đủ gồm 36 ô kho, giáp và tay phụ trong
`plugins/VnMineHardcore/death-history.yml`. Admin có thể dùng
`/vndeathhistory` để chọn người chơi online hoặc `/vndeathhistory <player>` để
mở trực tiếp lịch sử. Chọn lần chết sẽ hiển thị bước xác nhận; khôi phục sẽ thay
thế inventory hiện tại của người chơi đang online bằng dữ liệu đã lưu. Số lượng
snapshot có thể điều chỉnh từ 1 đến 10 bằng `death.history.max-records-per-player`.

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
mộ để đầu dựng thẳng trên mặt bia. Nếu người chơi chết trong nước, mộ được đặt
trên mặt nước và dùng block hỗ trợ tạm khi cần; block hỗ trợ được dọn cùng mộ.
Có thể tắt tính năng hoặc chọn vật liệu block hỗ trợ đặc trong
`graves.water-surface-placement` của `config.yml`.
`/quatanthu` chỉ cấp túi khi người chơi chưa từng nhận quà và hiện không sở hữu
lính đánh thuê, kể cả lính đang nghỉ hoặc hồi phục.

### Admin testing

`/fly` bật/tắt bay khi người quản trị vẫn ở Survival. `/nodame` bật/tắt miễn
nhiễm sát thương cho người chạy lệnh; quái vẫn có thể chọn người đó làm mục tiêu.
Trạng thái `/nodame` sẽ tự tắt khi người chơi thoát máy chủ.

### Spawn protection

`/vnspawn protect <X>` tạo khu vực hình tròn bán kính X block theo mặt phẳng
X/Z quanh block hiện tại; khu vực áp dụng ở mọi độ cao nhưng chỉ trong world
được tạo. Người chơi mặc định không thể phá hoặc đặt block bên trong. OP và
người có `vnmine.spawnprotection.bypass` vẫn được phá/đặt; vụ nổ, lửa, dòng
chất lỏng và block do thiên tai không thể làm thay đổi vùng này. Dữ liệu và bộ
đếm ID được lưu tại `plugins/VnMineHardcore/spawns-protection.yml`, nên vẫn còn
sau khi khởi động lại.

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
dõi. Các lệnh quản trị cần node riêng theo chức năng, được cấp từ
`permission.yml` (nhóm `admin` mặc định có toàn bộ quyền plugin).

### Thiên tai Chorus Explosion

Chorus Explosion mặc định tắt (`enabled: false`) trong
`plugins/VnMineHardcore/disasters/chorus-explosion.yml`. Khi được bật, điều
kiện `worlds` và bộ lọc trên từng action hỗ trợ tên world thật (ví dụ
`world_the_end`) cũng như tên dimension (ví dụ `the_end`); cấu hình mặc định
chỉ cho phép thiên tai này ảnh hưởng `world_the_end`.

### Boss level & skills

`bosses/_settings.yml` configures level ranges and spawn success chance by
in-game day. The default success chance grows from 15% on day 0 to 100% on day
400; boss levels progress from 1 to 10 over the same period. The applicable
threshold is the last `min-day` not greater than the current day, and the boss
randomly chooses a level inside that threshold's range. In each boss file,
`hp-by-level` maps levels to explicit maximum HP; missing levels use the nearest
lower configured level, then scale the base `hp` by 1.5 per additional level.
Skill sections under `skills` can be enabled individually and tune their
base damage, projectile count multiplier, cooldown, area, velocity, and spread.
Configured skill damage scales by boss level; projectile skills use
`boss-level * multiply` projectiles, capped at 40 per cast.
The creeper's `charged-explosion` skill calls a visual lightning strike,
damages nearby living entities using `damage` and `radius` without self-damage, then
keeps the creeper charged for `duration-seconds`. Its explosion power is
configured separately for normal and charged states; boss creeper explosions
do not break blocks and the boss survives each explosion. Values are in
`bosses/creeper_boss.yml`.
Available skill IDs are `arrow-barrage` (skeleton), `fireball-volley`
(dragon/ghast), `stomp` (giant), `ground-slam` (zombie), `charged-explosion`
(creeper), `shadow-strike` (enderman), `dive-bomb` (phantom), `charge`
(ravager), `venom-web` (spider), `shockwave` (vindicator), `potion-volley`
(witch), and `skull-volley` (wither).

Eight bosses also support the extended skills and traits configured in their
individual `bosses/*.yml` files: Enderman (`void-snatch`, `end-rift`,
`hydro-phobic-aura`), Ravager (`siege-charge`, `roar-weakness`,
`thorns-armor`), Phantom (`dive-bomb`, `phantom-minions`, `nocturnal-cloak`),
Ghast (`fireball-volley`, `fiery-drag`, `fireball-shield`), Spider
(`cobweb-trap`, `venom-pounce`, `brood-release`), Vindicator (`axe-spin`,
`axe-throw`, `enrage`), Witch (`potion-rain`, `disarm-magic`,
`witchcraft-drain`), and Giant (`ground-slam`, `boulder-toss`, `giant-armor`).
Terrain-changing skills are controlled by `terrain-enabled`, and temporary
blocks are restored after their configured duration or when the encounter ends.
Ravager block breaking uses the `break-blocks`, `max-break-blocks`, and
`breakable-blocks` allowlist settings.

`reward-damage-threshold-percent` in `bosses/_settings.yml` defaults to 50.
Each player's actual damage is accumulated against the boss's maximum HP; only
players who deal strictly more than the configured percentage qualify for
leaderboard points. Their contribution remains eligible if the boss dies from
another cause, but `/vnboss kill` and lifetime expiry always end the encounter
without points or loot.

Global boss skill cast feedback is configured in `bosses/_settings.yml`:
`skill-cast-feedback.console-log` logs successful casts to the console,
`skill-cast-feedback.broadcast-to-ops` sends notices only to online OPs, and
`skill-cast-feedback.broadcast-to-players` broadcasts translated skill names
to online non-operator players. OP and regular-player broadcasts are separate;
all options default to `true`. Bosses are persistent and
glowing for visibility. Grounded mobs use native AI/pathfinding; the Giant
uses slow custom movement because its vanilla AI does not reliably pursue targets.

```yaml
# bosses/_settings.yml
level-progression:
  enabled: true
  thresholds:
    - {min-day: 0, min-level: 1, max-level: 1}
    - {min-day: 25, min-level: 1, max-level: 2}
    - {min-day: 50, min-level: 2, max-level: 3}
    - {min-day: 400, min-level: 9, max-level: 10}

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
level 1 đến level 10. Mỗi cấp có vũ khí và sách di vật; mỗi boss cũng có cấu
hình `armor-set` tạo mũ, áo giáp, quần và ủng với xác suất mặc định 45% cho
từng món. Vật liệu chuyển từ sắt ở level 1 sang kim cương ở level 4 và
netherite ở level 7. Enchant của vũ khí và giáp tăng theo cấp; cấp thấp bắt đầu
dưới mức tự nhiên, còn cấp cao có thể over-enchant đến cấp 10. Tên/lore theo
từng boss được quản lý tập trung trong `plugins/VnMineHardcore/story.yml`.
Mỗi item dùng `story-id` để liên kết tới `artifacts.<story-id>.lore`; có thể
thêm chương mới bằng cách thêm ID duy nhất và danh sách lore vào file này.
Các cấp 1–2 dùng hậu tố `-common` với tên và câu chuyện giản dị hơn; từ cấp 3
trở lên dùng tuyến truyện phát triển theo sức mạnh của di vật.
Lore hỗ trợ `{level}` để hiển thị cấp vật phẩm; mỗi dòng không quá 30 ký tự,
tối đa 6 dòng. Cung Skeleton level 1 có thể rơi với Power III, Flame I và
Infinity I; xác suất rơi 65%, Infinity vẫn cần mang ít nhất một mũi tên;
trong file boss có thể tùy chỉnh `material`, `name`,
`min-amount`, `max-amount`, `chance`, `enchantments` (hỗ trợ over-enchant)
hoặc `book-enchantments` (sách có enchant để áp dụng vào item). Boss tự nhiên
có thể lên level 10 từ ngày 400 theo `bosses/_settings.yml`; `/vnboss` vẫn cho
phép gọi boss ở level được chọn. Khi boss xuất hiện, server broadcast loot có
thể rơi ở level đó và tỉ lệ từng món. Mọi boss tồn tại mặc định 20 phút;
bộ đếm roll boss kế tiếp tạm dừng trong khi cảnh báo hoặc boss đang hoạt động.

`scoreboard.yml` cấu hình chu kỳ reset (`reset.every-game-days`), sidebar,
thời gian title top 1, chu kỳ nhắc và quà của từng hạng top 1–10. Top 5 người
đủ ngưỡng damage nhận tổng cộng `10 × level` điểm, chia theo tỉ lệ damage
trong nhóm đủ điều kiện. Điểm và quà
đang chờ được lưu trong `scoreboard-data.yml`. Người chơi nhận quà bằng
`/reward`; nếu inventory không đủ chỗ, quà vẫn chờ và có thể nhận lại sau khi
dọn túi. Boss không nhận sát thương từ môi trường (bao gồm sát thương rơi) hoặc
skill/đòn đánh do chính boss tạo ra; loot và điểm thông thường vẫn theo ngưỡng
đóng góp, còn token triệu hồi có thể rơi khi có người chơi gây sát thương.
Chạy `/vnreload` để nạp cấu hình.

### Disasters - Thiên tai

Thiên tai không nằm trong `config.yml`. Cấu hình chung ở
`plugins/VnMineHardcore/disasters/_settings.yml`; mỗi loại thiên tai có file
riêng `plugins/VnMineHardcore/disasters/<id>.yml`.

`chat-description` có thể tiếp tục tồn tại trong file thiên tai để ghi chú cấu
hình, nhưng không còn được phát trong chat. Thông báo chỉ hiển thị world bị
ảnh hưởng, lấy từ `conditions.worlds` và chuyển thành tên các world đang tải;
cùng điều kiện đó cũng giới hạn những world mà action của sự kiện có thể tác
động. Các file cũ được bổ sung khóa còn thiếu nhưng giữ nguyên giá trị tùy chỉnh.

Trọng số `chance` của các thiên tai đang bật mặc định bằng nhau (5). Hệ thống
lọc trước các loại phù hợp với world/giờ hiện tại rồi mới roll trọng số, tránh
thiên tai bị thiên lệch do điều kiện dimension. `chorus-explosion` vẫn mặc định
tắt để tránh dịch chuyển người chơi ngoài ý muốn.

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

Dự án nhắm tới Paper 26.3 và Java 25+ cho server. Mã nguồn biên dịch với
Paper API `26.3.build.175-stable`; `api-version` trong `plugin.yml` là `26.3`.

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

- Paper 26.3
- Java 25+ (runtime server)
- Paper API `26.3.build.175-stable` dùng làm compile baseline

## Cài đặt

1. Tải file `.jar` từ bản phát hành (Release)
2. Đặt file vào thư mục `plugins/` của server
3. Khởi động lại server
4. Chỉnh sửa `plugins/VnMineHardcore/config.yml` theo nhu cầu
5. Gõ `/vnreload` để áp dụng thay đổi

## Changelog

### v1.9.1
- Đóng gói lại JAR từ resource hiện tại để khai báo đầy đủ các lệnh trong `plugin.yml`, tránh lỗi không tìm thấy command khi khởi động.

### v1.10.0
- Thêm `/vngroup` và `/vnuser` để quản lý group, phân quyền từng lệnh và quyền riêng theo người chơi trong `permission.yml`.
- Nhóm `user` mặc định chỉ dùng lệnh cơ bản; nhóm `admin` có đầy đủ quyền plugin.
- Bảo vệ cả block đặt, lửa, dòng chất lỏng, vụ nổ và thay đổi block do thiên tai; OP hoặc người có `vnmine.spawnprotection.bypass` được phá/đặt trong vùng.

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