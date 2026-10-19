# Kế hoạch: Rework Pháp Sư Lửa

## Mục tiêu

Thay bộ kỹ năng mặc định hiện tại của `pyromancer` bằng sáu kỹ năng theo tài liệu: Hỏa Cầu, Thiêu Đốt, Hỏa Bão, Hỏa Tường, Thiên Thạch Lửa và Phượng Hoàng Tái Sinh. Tận dụng hệ AI, cooldown, scaling, bảo vệ mục tiêu và GUI hiện có; mọi tham số mới có thể tùy chỉnh trong `armies.yml`.

## Hiện trạng và quyết định

- `pyromancer` hiện dùng `RANGED_ATTACK`, `FIRE_BOLT`, `ROOT_TARGET`, `BUFF_OWNER`; `FIRE_BOLT` là đòn gây sát thương tức thời và đốt, chưa phải projectile.
- Skill được khai báo/xác thực và thi triển trong `MercenaryManager`; GUI đọc mô tả và cooldown từ cùng cấu hình.
- Đã có kỹ năng đặc thù Pháp Sư Băng cùng hệ thống block/hiệu ứng/task tạm thời, nhưng phải giữ nguyên hành vi và các thay đổi chưa commit hiện có cho Frost Mage.
- Thay bộ mặc định bằng đúng sáu skill riêng (`FLAME_BOLT`, `IGNITE`, `FLAME_NOVA`, `FIRE_WALL`, `METEOR_STRIKE`, `PHOENIX_REBIRTH`); giữ nguyên `FIRE_BOLT` và các skill tổng quát để tương thích với class/cấu hình khác.
- Hỏa Tường dùng hàng block rắn tạm thời, có hoàn nguyên; chỉ đặt tại ô an toàn, không ghi đè block hiện hữu. Mặc định dùng MAGMA_BLOCK có hiệu ứng lửa, hàng ngắn 5 block, cao 3 block và tồn tại 3 giây.
- Thiên Thạch và Phượng Hoàng tự nhắm quanh mục tiêu AI hiện tại. Giá trị damage, nhịp/tổng sát thương DoT, bán kính lây và kích thước AoE chưa có trong tài liệu sẽ dùng mặc định cân bằng, cấu hình được và scaling cấp lính hiện có; cooldown mặc định theo tài liệu.
- Dùng kiểm tra mục tiêu PvE/PvP và quan hệ đồng minh hiện có; skill không được làm tổn thương chủ nhân, lính cùng chủ hay mục tiêu bị bảo vệ.
- Cấu hình cũ được di trú an toàn: chỉ đổi bộ skill nếu khớp bộ mặc định cũ, không ghi đè tùy chỉnh của quản trị viên.

## Các bước thực hiện

1. **Định nghĩa skill và di trú cấu hình**
   - Bổ sung sáu ID mới vào whitelist skills/settings; giới hạn các skill này cho `pyromancer`.
   - Cập nhật cấu hình mặc định trong `armies.yml`, đầy đủ chú thích VI/EN, cooldown lần lượt 2/6/10/12/25/90 giây và các giá trị cân bằng/cấu hình được.
   - Thêm di trú idempotent cho file `armies.yml` đang dùng, giữ nguyên thiết lập tùy chỉnh và legacy `FIRE_BOLT` ở class khác.
2. **Cài đặt hành vi kỹ năng**
   - Hỏa Cầu: projectile bay đến mục tiêu, gây damage phép và Slowness tương đương 20% trong 3 giây.
   - Thiêu Đốt: DoT theo nhịp 2 giây trong 8 giây; mỗi lần gây damage kiểm tra lại mục tiêu hợp lệ, hiệu ứng lây giới hạn theo bán kính/số mục tiêu cấu hình để tránh vòng lây vô hạn.
   - Hỏa Bão: damage AoE và knockback quanh pháp sư, có giới hạn bán kính/số mục tiêu.
   - Hỏa Tường: tạo hàng block lửa tạm theo hướng giữa pháp sư và mục tiêu, gây damage có giới hạn theo nhịp; chặn phá, đặt, piston và các thay đổi block liên quan theo cơ chế bảo vệ block tạm; khôi phục block gốc khi hết hạn/reload/disable.
   - Thiên Thạch Lửa: cảnh báo/hiệu ứng rơi tại vị trí mục tiêu AI lúc thi triển, gây damage AoE và hiệu ứng giữ chân 2 giây.
   - Phượng Hoàng Tái Sinh: hồi 30% máu tối đa; áp dụng miễn sát thương 2 giây bằng cơ chế có thời hạn/dọn dẹp; sau đó lao về phía mục tiêu AI và gây damage AoE bán kính 8 block cùng hiệu ứng đốt.
3. **Hoàn thiện UX và vòng đời**
   - Thêm tên tiếng Việt, mô tả, damage/range/duration/cooldown vào GUI và thông báo cast theo pattern hiện hành.
   - Dọn task, hiệu ứng miễn sát thương, DoT và block tạm an toàn khi lính nghỉ/chết, reload hoặc plugin tắt; không để trạng thái miễn sát thương tồn tại ngoài thời gian cấu hình.
4. **Tài liệu và phiên bản**
   - Cập nhật hướng dẫn mercenary trong `VnMineHardcore/README.md` và `CHANGELOG.md`.
   - Nâng phiên bản patch hiện tại `1.40.1` lên minor kế tiếp `1.41.0` trong `pom.xml`; không đóng gói JAR nếu chưa được yêu cầu.
   - Không chỉnh sửa hoặc hoàn tác các thay đổi chưa commit liên quan đến Pháp Sư Băng/mercenary đang có trong worktree.

## File dự kiến

- `VnMineHardcore/src/main/java/com/vnmine/hardcore/managers/MercenaryManager.java`: đăng ký, validate, thi triển, damage/DoT/khống chế, dọn trạng thái và GUI.
- `VnMineHardcore/src/main/resources/armies.yml`: sáu skill và thông số mặc định có chú thích song ngữ.
- `VnMineHardcore/README.md`: hướng dẫn cấu hình Pháp Sư Lửa.
- `CHANGELOG.md`, `VnMineHardcore/pom.xml`: ghi nhận tính năng và version.

## Kiểm chứng

1. Biên dịch Maven bằng `mvn -q -DskipTests compile`; không tạo JAR.
2. Kiểm tra cấu hình mặc định và di trú file cũ: skill mới hợp lệ, giá trị tùy chỉnh được giữ, skill `FIRE_BOLT` cũ ở class khác không đổi.
3. Kiểm tra từng skill về cooldown, damage, tầm/bán kính, DoT, Slowness, knockback, root, hồi máu và miễn sát thương.
4. Xác minh mục tiêu đồng minh/PvP vẫn tuân thủ luật hiện có; block tạm hoàn nguyên, không ghi đè địa hình; task/trạng thái được dọn khi nghỉ lính, reload và disable.
