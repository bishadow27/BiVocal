# M16 Phase C — Phục dựng Topic List và Topic Detail

Ngày kiểm tra: 01/11/2026. Trạng thái: **READY_FOR_ACCEPTANCE — chờ người dùng nghiệm thu ảnh/UI**.

Phạm vi được duyệt trong chat: bảng 9.2 của `M16_RECOVERY_AUDIT.md`, bổ sung yêu cầu palette, bộ lọc, preview và kiểm thử. Không triển khai Phase D hoặc đồng bộ lại toàn bộ Bottom Navigation.

## Kết quả phục dựng

| Yêu cầu | Kết quả / bằng chứng |
|---|---|
| Java/XML, MVVM, tái sử dụng lớp dữ liệu | PASS. Dùng TopicRepository/FlashcardRepository, Topic/Flashcard/FlashcardPage và Retrofit hiện có; không thêm API, Repository hay dependency |
| Palette | PASS. Nền #070B12, card #121A26, border #243246, CTA #2457E6, accent #22C7F8; ba tiêu đề Kho từ vựng/CHỦ ĐỀ/LOẠI TỪ cyan |
| Tất cả cố định; chip còn lại cuộn | PASS runtime: vị trí All không đổi khi kéo chip ngang |
| Hai danh sách độc lập, All tối đa 4 hàng mỗi danh sách | PASS runtime: kiểm tra chiều cao bằng min(4,count) × chiều cao hàng, cuộn riêng và cuộn trang tới POS |
| Semantic/POS/Life/Work/Nature dùng phần chiều cao còn lại | PASS runtime; POS không giữ margin/khung Semantic; Semantic/POS được kiểm tra thêm khi landscape ở cả font 1.0/1.3 |
| List → Detail | PASS: tìm Food, chọn topic rồi mở Detail với đúng topicCode |
| Header Back/tên/avatar và Drawer bên phải | PASS runtime: mở END Drawer, quay List bằng Back, tab Chủ đề hoặc mục Drawer |
| Metadata thật | PASS: tên/mô tả/số thẻ lấy từ catalog thật; icon/glyph mapping tái sử dụng từ List. FOOD hiện trả 489 thẻ; đây không phải tổng Flashcard toàn kho |
| Preview page=1, limit=20, tối đa 3 thẻ hợp lệ | PASS unit + runtime: đối chiếu ID và nội dung ba thẻ đầu hợp lệ từ phản hồi GET thật; bỏ null/ID sai/thiếu EN-VI/ID trùng; không xét quá 20 items |
| Warning IMPORTED_UNVERIFIED chỉ theo thẻ hiển thị | PASS unit: exact status của displayed cards; thẻ thứ tư, thẻ invalid/duplicate không tạo warning |
| Metadata/preview loading/error/retry độc lập | PASS unit và layout instrumentation: retry một kênh không tải lại kênh kia; lỗi preview vẫn giữ metadata/CTA |
| Stale callback/recreation | PASS: version/cancel riêng từng kênh, đổi code/onCleared vô hiệu hóa callback cũ; recreation giữ ViewModel/list và không request trùng |
| CTA Flashcard | PASS: đúng topicCode, không NEW_TODAY/REVIEW_TODAY, mở BROWSE hiện có; không sửa Flashcard Phase D |
| CTA Quiz | PASS: truyền topicCode, tải câu hỏi bằng GET hiện có; không chọn/nộp đáp án, không tự quyết điều kiện |
| Điều hướng tối thiểu | Thêm TOPIC_DETAIL; selected Chủ đề nhưng bấm Chủ đề vẫn về List. Không sửa style/navbar chung, Home hay Auth |

## Kiểm thử đã chạy

| Kiểm tra | Kết quả | Bằng chứng / giới hạn |
|---|---|---|
| RED trước implementation | EXPECTED_FAIL | Hai test class mới không compile do lớp tính năng chưa tồn tại; đây là RED compile, không mô tả thành assertion failure |
| assembleDebug | PASS | APK Debug đã build và cài cập nhật install -r |
| testDebugUnitTest | PASS — 106/106, 0 failure/error | Lượt đầu thực thi 106 test; các lượt cuối UP-TO-DATE vì không đổi Java unit/production liên quan |
| lintDebug | PASS — 0 error, 430 warning | Còn warning, không coi là lint sạch warning. Lượt đầu gặp NewApi windowLightNavigationBar trong theme mới, đã bỏ thuộc tính thừa và chạy lint lại đạt |
| assembleDebugAndroidTest | PASS | Test APK được biên dịch lại sau bổ sung kiểm tra filtered landscape/small width |
| Guard giữ dữ liệu | PASS | leaveApksInstalledAfterRun=true, testRunnerFactory.keepInstalledApks=true, uninstallIncompatibleApks=false. Dry-run chỉ xác minh guard, không tính là test thực thi |
| Emulator API 37, font 1.0 | PASS — 7/7, 78.187 giây | TopicDetailUiTest + TopicsUiTest, cài cả hai APK bằng install -r |
| Emulator API 37, font 1.3 | PASS — 7/7, 72.188 giây | Cùng bộ test, portrait/landscape, recreation và routing |
| Runtime màn nhỏ 320×640dp, font 1.3 | PASS — 2/2, 52.161 giây | Hai test GET-only thực tế List/Detail, cả portrait/landscape; tạm wm size 840×1680 ở density420, sau đó reset về physical1080×2400 |
| Ma trận layout nhỏ/font | PASS | Unattached XML Views 320×640, 412×915, 731×411dp × font1.0/1.3; valid/warning/empty/error Detail và card List. Fixtures chỉ trong test, không đưa vào runtime/ảnh nghiệm thu |
| GET /api/v1/health | PASS | status ok, database connected |
| GET /api/v1/topics | PASS | 38 topics |
| GET /api/v1/topics/catalog | PASS | 36 nhóm có thẻ đang hoạt động; 29 semantic + 7 POS. Hai topic không có thẻ active không nằm trong catalog |
| GET /api/v1/flashcards?limit=1 | PASS | total 20000 |
| GET /api/v1/flashcards?topicCode=FOOD&page=1&limit=20 | PASS | page1, limit20, 20 items; UI tối đa3 |

Lệnh build cuối: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` — BUILD SUCCESSFUL, 8s, 8 executed/70 up-to-date. Lượt sửa theme trước đó cũng BUILD SUCCESSFUL 1m3s, 44 executed/34 up-to-date.

Instrumentation dùng `adb shell am instrument -w -r -e class com.example.bivoca.TopicDetailUiTest,com.example.bivoca.TopicsUiTest com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner`. Lượt màn nhỏ chỉ chọn hai test runtime, không chạy lại fixture không cần thiết. Không chạy toàn suite instrumentation có tác động ngoài phạm vi.

## Ảnh Android thực tế

Toàn bộ ảnh dưới đây lấy từ Android UiAutomation trên Emulator đã cài APK, sử dụng API thật. Không dùng ảnh demo/mock. Ảnh Drawer được chụp sau khi đóng để tránh đưa thông tin tài khoản vào bằng chứng.

- [Kho từ vựng — All, font1.0](../exports/m16-phase-c-recovery-2026-11-01/font1/topics/topics-top.png)
- [POS — không có khoảng trống Semantic, font1.0](../exports/m16-phase-c-recovery-2026-11-01/font1/topics/topics-pos.png)
- [Topic Detail — font1.0](../exports/m16-phase-c-recovery-2026-11-01/font1/detail/detail-portrait.png)
- [Preview — font1.0](../exports/m16-phase-c-recovery-2026-11-01/font1/detail/detail-preview.png)
- [Detail — font1.3](../exports/m16-phase-c-recovery-2026-11-01/font13/detail/detail-portrait.png)
- [POS — font1.3](../exports/m16-phase-c-recovery-2026-11-01/font13/topics/topics-pos.png)
- [Landscape — font1.3](../exports/m16-phase-c-recovery-2026-11-01/font13/topics/topics-landscape.png)
- [Màn nhỏ — Detail font1.3](../exports/m16-phase-c-recovery-2026-11-01/small-font13/detail/detail-portrait.png)
- [Màn nhỏ — POS font1.3](../exports/m16-phase-c-recovery-2026-11-01/small-font13/topics/topics-pos.png)

Các ảnh còn lại cùng thư mục: search, empty, chip fixed All, cuộn giữa danh sách, hai frame, sau Drawer và Detail landscape.

## File thêm/sửa

Chi tiết đủ từng file, loại thay đổi và SHA256 trước/sau nằm trong [source-manifest.csv](../exports/m16-phase-c-recovery-2026-11-01/source-manifest.csv): **25 file Android: 15 sửa, 10 thêm**.

| Nhóm | File (tính từ android-app/app/src/) | Lý do |
|---|---|---|
| List/presentation | main/java/com/example/bivoca/ui/topics/TopicsActivity.java; TopicAdapter.java; TopicPresentation.java (mới) | Routing Detail, đo row/viewport theo data-width, share icon/glyph/color |
| Detail | main/java/com/example/bivoca/ui/topics/TopicDetailActivity.java; TopicDetailViewModel.java; TopicPreviewAdapter.java (mới) | MVVM, independent state, bounded preview, Drawer và CTA |
| Layout | main/res/layout/activity_topics.xml; item_topic.xml; activity_topic_detail.xml (mới); item_topic_preview.xml (mới) | Hai khung độc lập, palette và Detail native |
| Resource | main/res/values/topics_frontend.xml; topics_phase_c.xml (mới); main/res/color/topics16_chip_background.xml; topics16_chip_text.xml; main/res/drawable/topics16_search_field.xml; topics16_group_badge.xml | Màu đã chốt, theme/strings riêng không thay A/B |
| Routing | main/AndroidManifest.xml; main/java/com/example/bivoca/ui/navigation/NavigationRoute.java; NavigationRoutePolicy.java; LearningBottomNavigation.java | Đăng ký Detail, selected Topics khác current Detail; không restyle navbar |
| Unit | test/java/com/example/bivoca/ui/topics/TopicDetailViewModelTest.java (mới,18); TopicPresentationTest.java (mới,3); test/java/com/example/bivoca/ui/navigation/NavigationRoutePolicyTest.java (+1) | Bounded/warning/stale/retry/recreation/mapping/routing |
| Instrumentation | androidTest/java/com/example/bivoca/TopicsUiTest.java; TopicDetailUiTest.java (mới) | API thật, navigation, filter-height, font/rotation/small/layout states |
| Bàn giao | docs/reports/M16_PHASE_C_RECOVERY_REPORT.md (mới); WORK_STATUS.md (chỉ bổ sung); docs/exports/m16-phase-c-recovery-2026-11-01/ (mới) | Báo cáo, ảnh và log/hash; giữ nguyên lịch sử |

NavigationDrawerController không cần sửa vì attachSession hiện có đã đáp ứng. Không tạo drawable mới trùng icon hiện có.

## Bảo toàn phạm vi và dữ liệu

Baseline gồm 447 file có SHA256 và bản sao trước sửa, không chứa .env/session. Trước bổ sung handoff: 432 file nguyên hash; 15 file cũ thay đổi đúng whitelist; không có thay đổi bất ngờ. 10 file Android mới được kiểm tra không trùng. Các file Home/Login/Register/Forgot, Flashcard/Quiz, Repository/API/DTO, Backend/Admin, cấu hình build và debug manifest mạng cục bộ giữ nguyên baseline. WORK_STATUS được bổ sung sau đó, toàn bộ byte nội dung có trước được giữ.

Không migration/seed/import/reset/restore/M15.7 Apply. Không POST Review/Quiz Answer/Favorite. Không logout, clear data, uninstall hay wipe Emulator. Không thay credential/Auth để xử lý HTTP400: người dùng nhập lại thông tin và xác nhận vào Trang chủ trước kiểm thử. Không lưu/in mật khẩu hoặc token vào báo cáo/log.

Giữ mọi thay đổi/untracked có trước Phase C, bao gồm backend/package*, maintenance và các file audit M15.6/M15.7. Không reset/clean/checkout ghi đè, không stage/commit/push; HEAD vẫn a4a9801 (commit audit có trước Phase C).

## Giới hạn và nghiệm thu

- Lint còn 430 warning cần đánh giá ở phạm vi riêng; không mở rộng Phase C để sửa hàng loạt code cũ.
- Error/empty/warning và retry/callback được xác minh bằng unit + unattached layout fixtures; chưa chủ động làm hỏng API thật hoặc thay dữ liệu để tạo ảnh lỗi/warning. Ảnh runtime minh họa phản hồi hiện có, không giả lập lỗi trong màn đang chạy.
- Font1.0/1.3 đã chạy ở độ phân giải Emulator gốc; 320×640dp chạy runtime font1.3 và đo XML cả hai font. Chưa thử thiết bị Android vật lý hoặc OEM khác.
- Landscape thấp cần cuộn trang để tiếp cận một hàng hoàn chỉnh; không ép bốn hàng vừa mọi màn hình hoặc thay AVD để tạo ảnh đẹp. Các filter một khung có fallback tối thiểu một hàng và cuộn trang.
- Recreation trong cùng process đã kiểm tra; process death/token hết hạn/cắt mạng thật chưa được E2E riêng.
- Quiz chỉ kiểm tra vào màn/tải câu hỏi; không kiểm tra chấm đáp án/XP/SRS/Favorite hoặc ghi dữ liệu trên database phục dựng.
- Số lượng GET xác nhận 20.000/38 trước và sau; đây không phải chứng minh checksum từng record dữ liệu. Các thao tác kiểm thử không gọi API ghi học tập.

Người dùng kiểm tra ảnh và ứng dụng: Kho từ vựng → chọn topic → Detail → Flashcard/Quiz; All/POS/Semantic và Drawer/Back. Chưa nghiệm thu milestone tự động. **Dừng tại Phase C, chờ duyệt; không tự sang Phase D.**
