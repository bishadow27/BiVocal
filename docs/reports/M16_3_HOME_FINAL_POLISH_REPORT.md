# M16.3 — Home Final UI Polish

Ngày: 27/10/2026. Trạng thái: **READY_FOR_ACCEPTANCE**, chờ người dùng duyệt hình thức bản cuối. Hướng bố cục đã được người dùng duyệt; không đồng nghĩa đã nghiệm thu bản polish mới.

## Kết quả trước/sau

| Phần | Trước | Sau |
|---|---|---|
| Slogan header | 11sp, không giới hạn dòng | 12sp, tối đa 2 dòng, không gạch chân. Dưới 440dp tách slogan thành hàng bên dưới để có đủ chiều rộng; từ 440dp giữ mascot/slogan trái, streak/avatar phải cùng hàng. |
| Card SRS | Nền #121212, viền #2D3035, radius 18dp | Nền #111726, viền 1dp #334155, radius 20dp. Tiêu đề 16sp, sans-serif-medium; chữ phụ tối thiểu 12sp. |
| Giải thích SRS | Ký tự “?” nhỏ | Icon Info cyan tái sử dụng asset `auth_info`, vùng chạm 48×48dp, giữ dialog giải thích. |
| CTA ôn | Nhãn chung và badge số | Hàng gradient cao tối thiểu 52dp, nhãn động “Ôn N từ đến hạn”, mũi tên. API thực tại lúc nghiệm thu trả 7 từ đến hạn. |
| Leaderboard Home | “Bảng xếp hạng tuần” | “Bảng xếp hạng XP”; subtitle “Xếp theo tổng XP tích lũy”. Hàng và nút Tất cả giữ nguyên. |

Giữ Daily Goal, thứ tự card, Drawer bên phải mở từ avatar, bốn tab cố định. Không đưa Quick Stats, tab Hồ sơ hoặc card slogan cuối trang trở lại. SRS vẫn nhóm mức 0–1 / 2–3 / 4 / 5; thanh màu lấy trọng số API. `dueNow=0` vẫn disabled/chưa có từ đến hạn; không tạo tỷ lệ giả.

## File ứng dụng đã sửa

Đường dẫn tính từ gốc project:

1. `android-app/app/src/main/res/layout/activity_home.xml` — thay block header bằng include; Daily Goal và các card còn lại giữ nguyên.
2. `android-app/app/src/main/res/layout/view_home_header.xml` — thêm header cho màn hẹp.
3. `android-app/app/src/main/res/layout-w440dp/view_home_header.xml` — thêm header hàng ngang cho màn rộng.
4. `android-app/app/src/main/res/layout/view_home_header_actions.xml` — thêm resource dùng chung streak/avatar, giữ ID/listener cũ.
5. `android-app/app/src/main/res/layout/view_home_srs.xml` — nền, viền, font, Info và CTA.
6. `android-app/app/src/main/res/values/home_srs.xml` — nhãn CTA có tham số, chữ phụ 12sp, ký tự mũi tên.
7. `android-app/app/src/main/res/values/home_frontend.xml` — hai nhãn leaderboard.
8. `android-app/app/src/main/res/drawable/home_srs_due_row.xml` — selector enabled gradient/disabled.
9. `android-app/app/src/main/java/com/example/bivoca/ui/home/HomeActivity.java` — chỉ đổi nhãn CTA từ `dueNow` và ẩn mũi tên khi disabled.
10. `android-app/app/src/androidTest/java/com/example/bivoca/HomeResponsiveUiTest.java` — mở rộng 12 cấu hình, kiểm tra slogan đủ nội dung/tối đa hai dòng và Info 48dp.
11. `android-app/app/src/androidTest/java/com/example/bivoca/HomeRuntimeUiTest.java` — đối chiếu bốn nhóm/CTA với API, kiểm tra Info mở/đóng dialog.

SHA-256 trước/sau xác nhận source Backend, Admin, auth, HomePresentation và NavigationDrawerController không thay đổi. Không chạy migration, import hoặc lệnh ghi PostgreSQL; không submit Review/Quiz. Không truy vấn checksum DB mới trong lượt chỉ sửa UI này.

## Kiểm thử thực tế

| Kiểm tra/lệnh | Kết quả |
|---|---|
| `adb devices -l` | PASS — Emulator đang kết nối. |
| `GET http://127.0.0.1:3000/api/v1/health` | PASS — HTTP 200. |
| `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --rerun-tasks` | PASS, 1m38s; 78 task thực thi. Unit 81/81, 0 lỗi; lint lượt đầu 0 lỗi/400 cảnh báo. |
| Cùng bốn task, sau khi dọn hai resource warning | PASS, 10s; 25 task thực thi/53 up-to-date. Unit chạy lại 81/81; lint chạy lại 0 lỗi/398 cảnh báo. |
| Gradle `connectedDebugAndroidTest --dry-run` với init script kiểm tra guard | PASS — `testRunnerFactory.keepInstalledApks=true`, `uninstallIncompatibleApks=false`. Dry-run không được tính là test thực thi. |
| `adb install -r app-debug.apk` và `adb install -r app-debug-androidTest.apk` | PASS — giữ app data; không uninstall/clear data. |
| `adb shell am instrument -w -r -e class com.example.bivoca.HomeResponsiveUiTest,com.example.bivoca.HomeRuntimeUiTest com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner` | PASS **2/2**, 33.408s trên APK cuối. |
| Cùng runner, chỉ `HomeRuntimeUiTest`, font hệ thống tạm thời 1.3 | PASS **1/1**, 34.170s. Font đã khôi phục về 1.0 trong `finally`. |

Responsive: 320×640, 411×731, 412×915, 440×915, 467×830 và 731×411dp, mỗi kích thước ở font 1.0/1.3. Đây là đo layout thật bằng instrumentation trên Emulator hiện tại, không phải đã chạy sáu AVD khác nhau. Kiểm tra chữ không bị cắt dọc, slogan đủ nội dung, tab cố định và cuộn tới card cuối.

Runtime dùng tài khoản đang có và Backend thật: Home tải API, số bốn nhóm/CTA khớp schedule; Info hoạt động; avatar mở Drawer END; các điều hướng hiện có mở đúng Activity. Không gửi lượt học/Quiz để dựng ảnh. Trạng thái `dueNow=0` không được ép vào tài khoản thật trong lượt này; giữ nguyên điều kiện disabled trong source và bằng chứng runtime lịch sử.

Các lỗi công cụ đã xử lý: script guard đầu tiên đọc nhầm vị trí thuộc tính AGP, đã đối chiếu source AGP và chuyển sang TestRunnerFactory; wrapper nghiệm thu ban đầu tính sai đường dẫn APK và thử mở Home không exported, đã sửa đường dẫn/khởi chạy qua launcher. Các lần đó không xóa app/data; không phải FAIL của unit/instrumentation ứng dụng. Bộ test cuối 3/3 lượt PASS.

## Ảnh Android cuối cùng

Ảnh chụp nguyên trạng Emulator/API thật, không phải fixture hoặc ảnh demo:

- [Home top](../../android-app/runtime-evidence/m16-3-home/final-polish/home-top.png)
- [SRS](../../android-app/runtime-evidence/m16-3-home/final-polish/home-srs.png)
- [Leaderboard](../../android-app/runtime-evidence/m16-3-home/final-polish/home-leaderboard.png)
- [Drawer](../../android-app/runtime-evidence/m16-3-home/final-polish/home-drawer-open.png)
- [Home font 1.3](../../android-app/runtime-evidence/m16-3-home/final-polish/font-1.3/home-top.png)
- [SRS font 1.3](../../android-app/runtime-evidence/m16-3-home/final-polish/font-1.3/home-srs.png)
- [Leaderboard font 1.3](../../android-app/runtime-evidence/m16-3-home/final-polish/font-1.3/home-leaderboard.png)
- [Drawer font 1.3](../../android-app/runtime-evidence/m16-3-home/final-polish/font-1.3/home-drawer-open.png)

Đã xem lại ảnh: header không đè avatar, slogan hai dòng ở font 1.3; SRS/legend/CTA đọc đủ; tab vẫn bốn mục. Top và SRS ở font mặc định có thể gần giống nhau vì toàn bộ card SRS đã nằm trong viewport.

AVD vẫn physical 1080×1920, density vật lý 420/override 370 như trước; hash `config.ini` không đổi. Không đổi size/density để làm đẹp ảnh. App đang mở lại Home, phiên đăng nhập được giữ.

Evidence cùng thư mục ảnh: `build.txt`, `build-final.txt`, `test-guards.txt`, `instrumentation-final.txt`, `instrumentation-font-1.3.txt`, `verification.json`, `source-before.json`, `source-changes.json`; script nghiệm thu `run-home-acceptance.ps1` và guard `test-guards.init.gradle`.

Giới hạn: chưa chạy thiết bị thật hoặc AVD màn nhỏ riêng; màn nhỏ được kiểm tra bằng resource measurement. Backend unit/DB integration NOT_RUN vì không sửa Backend. Chưa tự xác nhận người dùng đã duyệt UI cuối, không triển khai M16.4. Git không có repository được xác định; CHƯA COMMIT/CHƯA PUSH.
