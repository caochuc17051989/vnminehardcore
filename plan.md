# Kế hoạch: Nâng bậc lính và thiết kế lại Pháp Sư Băng

## Mục tiêu

- Khi lính đánh thuê nâng bậc, đặt level về 1 và EXP về 0; chỉ số được tính lại theo bậc mới ở level 1. Nếu lưu thất bại, hoàn tác bậc, level, EXP, vật phẩm và chỉ số.
- Thiết kế lại `frost_mage` với snowball bay ổn định làm đòn cơ bản và tám kỹ năng: Ice Spike, Blizzard, Frost Nova, Ice Wall, Frost Shield, Frost Step, Frost Walker và Ice Age.
- Cấu hình tất cả thông số trong `armies.yml`; giữ nguyên các class lính khác.
- Block do kỹ năng tạo ra chỉ tồn tại tạm thời và phải được hoàn nguyên. Ice Age không biến đổi địa hình.

## Triển khai

1. **Nâng bậc:** cập nhật `upgradeTier` để snapshot/rollback `tier`, `level`, `experience` và inventory. Khi thành công, đặt level 1/EXP 0, xóa cooldown cũ, cập nhật chỉ số và hiển thị. Giữ nguyên `originalTier`, trang bị và trạng thái triển khai.
2. **Đăng ký kỹ năng:** mở rộng xác thực cấu hình, luồng cast, tên và mô tả GUI; thêm cấu hình riêng cho từng kỹ năng. Chỉ đổi bộ mặc định của `frost_mage`.
3. **Tấn công/khống chế:** snowball bay thẳng có damage khi trúng; Ice Spike phóng đòn băng; Blizzard gây damage theo nhịp và Slowness trong vùng; Frost Nova làm chậm mạnh và đặt băng tạm dưới mục tiêu; Ice Wall tạo hàng băng tạm cao 2–3 block ở vị trí trống. Không ghi đè block rắn hoặc block do người chơi đặt.
4. **Phòng thủ/tiện ích/tối thượng:** Frost Shield cấp Absorption và làm chậm kẻ tấn công trong thời gian hiệu lực; Frost Step cho NPC lướt áp sát mục tiêu mà không xuyên vật cản; Frost Walker dùng enchant trên boots nếu mob hỗ trợ, nếu không thì mô phỏng bằng băng tạm có hoàn nguyên; Ice Age áp hiệu ứng đóng băng và particles trên vùng rộng, không đổi block bề mặt. Hủy task và dọn hiệu ứng/block tạm khi hết giờ hoặc plugin dừng.
5. **Cân bằng/cấu hình:** damage mặc định 3.0 ở level 1, tăng 0.1 mỗi level; cooldown giảm 0.1 giây mỗi level, tối thiểu 1 giây; range mặc định 5 block, tăng 1 block tại level 5, 10, 15, 20, 25 và 30. Ice Age bắt đầu bán kính 15 block và tăng theo cùng nhịp. Duration tăng theo level cho hiệu ứng phù hợp; duration của block/cooldown ngắn giữ giới hạn cân bằng. Tách scaling riêng khỏi multiplier skill chung để tránh nhân chồng. Mọi khóa mới có chú thích VI/EN về công dụng, mặc định và giới hạn.
6. **Tài liệu/version:** cập nhật README và CHANGELOG; nâng version minor từ `1.39.2` lên `1.40.0` trong `pom.xml` và kiểm tra metadata version liên quan. Không đóng gói JAR nếu chưa được yêu cầu.

## File dự kiến

- `VnMineHardcore/src/main/java/com/vnmine/hardcore/managers/MercenaryManager.java`: nâng bậc, cấu hình/validate/cast skill, projectile và damage handler, scaling, GUI, cleanup.
- `VnMineHardcore/src/main/resources/armies.yml`: bộ skill và tham số mặc định của `frost_mage`.
- `VnMineHardcore/README.md`: hướng dẫn nâng bậc, kỹ năng và đường dẫn cấu hình.
- `CHANGELOG.md`, `VnMineHardcore/pom.xml`: ghi nhận feature và version.

## Kiểm tra

1. Biên dịch Maven bằng `mvn -q -DskipTests compile`; chưa thấy thư mục test trong dự án.
2. Kiểm tra YAML và tải lại cấu hình; khóa thiếu dùng default, giá trị ngoài giới hạn được xử lý/log đúng.
3. Thử nâng lính level 30 lên bậc mới: level 1, EXP 0, chỉ số đúng; xác minh rollback nếu lưu thất bại.
4. Kiểm tra Pháp Sư Băng tại level 1/5/6/30 về đường bay snowball, damage, range, cooldown, thời lượng và giới hạn mục tiêu.
5. Xác minh block tạm hoàn nguyên đúng, không ghi đè block người chơi; reload/disable không để task hoặc block sót; xác nhận Frost Walker vanilla hoặc fallback NPC.

## Quyết định

- Triển khai đủ tám kỹ năng; snowball là đòn đánh cơ bản.
- Frost Step được chuyển thành cú lướt NPC áp sát mục tiêu.
- Block tạo ra chỉ tạm thời, có snapshot và hoàn nguyên; Ice Age chỉ gây hiệu ứng, không thay đổi địa hình.
- Dùng Bukkit/Paper API, không thực thi lệnh console hay dùng `/setblock`.
- Cooldown, damage, range, duration và giới hạn AoE đều cấu hình được; AoE và hiệu ứng có giới hạn để chống cộng dồn quá mức.
