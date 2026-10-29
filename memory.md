# memory.md — Quyết định và bài học lâu dài BiVoca

## 2026-10-27 — Kho từ vựng, chip cố định và hai khung danh sách

- **Quyết định mới hơn, nguồn yêu cầu trực tiếp:** CHỦ ĐỀ/LOẠI TỪ in hoa và lớn hơn (16sp), hiện lại viền hai danh sách; khung chip vẫn không viền. Giữ giới hạn4 hàng và cách cuộn. Thay thế phần bỏ viền danh sách/nhãn thường ở cập nhật ngay dưới.

- **Cập nhật được duyệt từ yêu cầu trực tiếp mới:** bỏ viền bao ngoài ba khung, giới hạn mỗi danh sách **4 hàng thay 5**, nhãn **Chủ đề**/**Loại từ**. Giữ cách cuộn và All cố định. Thay thế thông số năm hàng ở quyết định ban đầu bên dưới.

- **Loại:** quyết định được duyệt; **Nguồn:** yêu cầu kèm ba ảnh và câu trả lời “Đúng, hai khung riêng” trong chat.
- M16.4 dùng tiêu đề Kho từ vựng; bỏ shortcut tra từ trên header, giữ ô tìm chủ đề. Khung chip trong suốt, Tất cả cố định trái; các chip khác cuộn ngang.
- Hai khung Chủ đề/Loại từ riêng, mỗi khung tối đa năm hàng và cuộn nội bộ; trang ngoài vẫn cuộn để chuyển giữa hai khung. Thay mô tả một danh sách của M16.4 cũ; không đồng nghĩa đã nghiệm thu bản UI mới.

## 2026-10-27 — Home Final UI Polish

- **Loại:** quyết định được duyệt; **Nguồn:** yêu cầu trực tiếp “M16.3 HOME FINAL UI POLISH”. Người dùng duyệt hướng bố cục mascot/slogan trái, streak/avatar phải mở Drawer, Daily Goal, SRS bốn nhóm, leaderboard và bốn tab. Không khôi phục Quick Stats/tab Hồ sơ/card slogan cuối trang.
- Leaderboard Home dùng tên “Bảng xếp hạng XP” và “Xếp theo tổng XP tích lũy”; không đặt lại tên tuần khi chưa có dữ liệu tuần. Slogan không gạch chân, tối đa hai dòng; SRS giữ mapping 0–1/2–3/4/5. Đây là duyệt hướng/phạm vi sửa, chưa phải nghiệm thu bản polish cuối.

## 2026-10-27 — Nhận diện thương hiệu ở Home header

- **Cập nhật cùng ngày, nguồn người dùng kèm ảnh:** bỏ card mascot/slogan ở cuối nội dung Home; chỉ giữ phần thương hiệu/slogan trên header. Không đưa card footer trở lại từ mô tả M16.3 lịch sử.

- **Loại:** quyết định được duyệt; **Nguồn:** yêu cầu trực tiếp của người dùng với hai ảnh tham chiếu trong chat.
- Trái Home header là mascot gốc và slogan trong card động viên; bỏ lời chào/avatar trái. Phải là avatar người dùng thay nút hamburger, bấm mở Drawer hiện có. Giữ badge streak và bốn tab. Thay thế bố cục header M16.3 trước; không đồng nghĩa người dùng đã nghiệm thu toàn milestone.

## 2026-10-27 — Home dùng bốn tab

- **Loại:** quyết định được duyệt; **Nguồn:** người dùng yêu cầu trực tiếp bỏ Hồ sơ khỏi thanh dưới trong chat.
- Bottom navigation Home chỉ có Trang chủ, Chủ đề, Ôn tập, Trắc nghiệm. Hồ sơ tiếp tục mở qua avatar/Drawer. Thay thế yêu cầu năm tab trước của M16.3 trong đúng phạm vi thanh Home; không xác nhận nghiệm thu toàn milestone.

> Trí nhớ bằng file, được đọc theo `AGENTS.md`. Không lưu tiến độ từng task, thông tin cá nhân hoặc secret. Quyết định mới không sửa ngược lịch sử nghiệm thu.

## 2026-10-26 — Tích hợp bộ hướng dẫn dự án

- **Loại:** quyết định được duyệt.
- **Nguồn:** người dùng trả lời “duyệt” sau báo cáo phân tích và kế hoạch tích hợp `_codex_import` trong chat này.
- **Phạm vi:** ghép `AGENTS.md`, tạo bối cảnh/trí nhớ và skill tại gốc project; loại thông tin cá nhân không cần thiết, bổ sung bảo vệ Git và bàn giao.
- **Giới hạn:** không cấp quyền commit/push, migration, sửa code ứng dụng, triển khai production hoặc xác nhận nghiệm thu các màn hình đang chờ duyệt.
- **Quy tắc làm việc:** tiếng Việt ngắn gọn; chủ động việc nhỏ trong yêu cầu; thay đổi lớn cần phương án và phê duyệt đúng phạm vi. Giữ các nguyên tắc kỹ thuật cũ, không nhân bản lớp hoặc đưa mock vào runtime.
- **Git:** phải có quyền hành động và nhánh feature được chỉ định trước commit/push; hồ sơ nhập không tự cấp quyền.
- **Nghiệm thu:** phân biệt kết quả test với nghiệm thu người dùng; không đổi DONE/PASS cũ, không tự nâng milestone đang chờ duyệt. Tiến độ ghi ở `WORK_STATUS.md`.

## M13.8 — Online-Only, giữ M8 làm lịch sử

- **Loại:** đã kiểm chứng từ source/cấu hình và hồ sơ; không tái chạy runtime trong phiên tích hợp.
- **Nguồn:** [Android](ANDROID_CONTEXT.md), [M13.8](docs/reports/M13_8_QUIZ_QUALITY_ONLINE_ONLY_REPORT.md), `android-app/app/build.gradle.kts` và `android-app/historical-room/`.
- **Bài học:** Room M8 đã nghiệm thu ở thời điểm cũ nhưng không còn là runtime hiện hành; không tái đưa cache/offline vào app vì đọc nhầm lịch sử. Không xóa mã lịch sử để đồng bộ tài liệu.

## M8 — Bảo vệ dữ liệu khi chạy instrumentation

- **Loại:** bài học được ghi nhận trong báo cáo lịch sử; cấu hình hiện có đã được đọc ngày 26/10/2026.
- **Nguồn:** [Android, mục bảo vệ dữ liệu instrumentation](ANDROID_CONTEXT.md), [gradle.properties](android-app/gradle.properties).
- **Nguyên nhân theo hồ sơ:** runner từng gỡ ứng dụng và làm mất phiên local.
- **Quy tắc:** giữ `android.injected.androidTest.leaveApksInstalledAfterRun=true`; xác minh `keepInstalledApks=true`, `uninstallIncompatibleApks=false` trước test trên thiết bị có dữ liệu. Không tự uninstall/clear data để sửa test.

## 2026-10-26 — Giữ các phản hồi UI M16 hiện có

- **Loại:** quyết định người dùng được ghi trong tài liệu hiện hữu; không xác nhận nghiệm thu UI mới.
- **Nguồn:** đầu [WORK_STATUS.md](WORK_STATUS.md), mục M16.2 và các cập nhật M16 trong `docs/reports/`.
- **Quyết định:** Register/Forgot không có Back phía trên logo; slogan chung không gạch chân. Các phản hồi mới ưu tiên hơn mô tả cũ còn Back/gạch chân.
- **Giới hạn:** M16.1–M16.3 vẫn chờ duyệt giao diện; phê duyệt bộ hướng dẫn không thay thế nghiệm thu đó.

## Cách bảo trì

- Chỉ thêm điều tái sử dụng được, với loại, nguồn, ngày nếu biết và giới hạn kiểm chứng. Quyết định bị thay cần thêm lý do và mốc thay thế.
- Không coi giả định về người dùng, quyền Git, môi trường hoặc số test trong `_codex_import` là sự thật đã duyệt.
- Không sao chép nhật ký task hoặc toàn bộ tài liệu kỹ thuật vào đây; dẫn nguồn hiện có để tránh nhiều bản mâu thuẫn.
