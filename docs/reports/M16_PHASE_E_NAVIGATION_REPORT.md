# M16 Phase E — Unified Learning Bottom Navigation

Ngày: 02/11/2026. Trạng thái: **READY_FOR_ACCEPTANCE**; chưa phải nghiệm thu PASS của milestone.

## Phạm vi và đầu vào

Triển khai theo yêu cầu Phase E do người dùng gửi: A–D đã được người dùng nghiệm thu, Google Sign-In đã khôi phục. Ghi nhận này dựa trên yêu cầu hiện tại, không tự sửa lịch sử nghiệm thu cũ. File báo cáo Phase D hiện chứa yêu cầu xem bốn ảnh của người dùng; giữ nguyên file đó, không phục hồi/ghi đè nội dung.

Chỉ thay phần navigation Android Java/XML và test liên quan. Không thêm Activity/ViewModel/Repository/API hoặc dependency. Không sửa nghiệp vụ, Backend, Admin, database, Auth, TTS, Settings, SRS, XP, Room hoặc giao diện nội dung A–D.

## Thiết kế và route

Tái sử dụng LearningBottomNavigation, NavigationRoute/Policy và NavigationDrawerController. Bốn cột bằng nhau, nền Home đã duyệt #0D1320, active #22C7F8, inactive #B7C3D4, icon22dp, label12sp; active Bold/inactive Normal; min-height64dp và vùng chạm tối thiểu48dp. Flashcard/Quiz thêm include cố định dưới vùng nội dung cuộn; giữ xử lý system insets hiện có. Home XML/content không đổi; chuyển cấu hình icon/kích thước/màu navbar từ code Home vào XML dùng chung với cùng giá trị.

| Màn/context | Tab chọn | Khi chạm tab |
|---|---|---|
| Home | Trang chủ | HomeActivity; cùng Home là no-op |
| Topics | Chủ đề | TopicsActivity; giữ filter/query |
| Topic Detail | Chủ đề | Quay về TopicsActivity đã có |
| Flashcard BROWSE | Chủ đề | Quay về Topics; mở lại cùng topic để tiếp tục Browse |
| Flashcard REVIEW_TODAY | Ôn tập | FlashcardActivity với REVIEW_TODAY |
| Flashcard NEW_TODAY | Trang chủ | HomeActivity; CTA học mới thật mở lại queue NEW hiện có |
| Quiz | Trắc nghiệm | QuizActivity; tab/Drawer re-entry giữ topic/session hiện tại |

Điều hướng dùng REORDER_TO_FRONT + SINGLE_TOP và bỏ CLEAR_TOP để không hủy các Activity học nằm trên đích. Không dùng NEW_TASK/CLEAR_TASK trong router. Android mô tả hành vi cờ tại [Intent](https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_REORDER_TO_FRONT). Explicit Back vẫn theo cơ chế đóng màn cũ, khác thao tác chuyển tab; Back đóng Drawer trước. Không thêm tab Hồ sơ; avatar/Drawer bên phải vẫn hoạt động.

## Bảo toàn phiên và guard

- Flashcard dùng keyed instances của **FlashcardViewModel hiện có** trong cùng Activity ViewModelStore: Browse theo topic, Review theo mode, NEW theo mode. Lưu UI riêng từng context: mặt thẻ/card ID, Browse ACK/error, auto-spoken ID và vị trí cuộn. Chuyển context dựng lại UI, không thay VM/queue cũ. NEW key không phụ thuộc remaining/limit trên Home, tránh tạo queue khác khi Home tính lại; limit mới chỉ áp dụng khi người dùng chủ động reload sau đó.
- Quiz dùng QuizViewModel hiện có theo topic; factory mới mới gọi initIfNeeded. Cùng context hoặc tab không phát động khởi tạo mới. Giữ câu hỏi/current index/last answer bằng VM, lưu vị trí cuộn theo context. CTA Detail truyền topicCode; no-extra tab quay lại quiz đang có.
- Router và Drawer cùng đi qua guard trên navbar. Flashcard kiểm tra cả pending hiện có của Activity và VM. Quiz tái sử dụng QuizBackPolicy: đang submit thì chặn; cần xác nhận thì giữ đúng hành động tab/Drawer/Back yêu cầu, chấp nhận mới chạy hành động đó. Navbar bị khóa/mờ khi pending/submitting. Không tự nộp đáp án và không thay logic ACK/error Phase D.
- Cache phiên giữ qua tab/context switch và configuration recreation trong cùng tiến trình/Activity store. Không bổ sung persistence queue khi process death, không đưa Room trở lại. Explicit Back đóng Activity có thể kết thúc store như trước; không cam kết giữ phiên qua force-stop/process death.

## File thêm/sửa

Các đường dẫn dưới đây tính từ gốc project; manifest SHA256 trước/sau nằm trong evidence.

| File | Thay đổi |
|---|---|
| android-app/app/src/main/java/com/example/bivoca/ui/navigation/NavigationRoute.java | NEW_TODAY |
| android-app/app/src/main/java/com/example/bivoca/ui/navigation/NavigationRoutePolicy.java | selected mapping/session keys |
| android-app/app/src/main/java/com/example/bivoca/ui/navigation/LearningBottomNavigation.java | shared style, retained routing, guard |
| android-app/app/src/main/java/com/example/bivoca/ui/navigation/NavigationDrawerController.java | dùng cùng router/guard |
| android-app/app/src/main/java/com/example/bivoca/ui/home/HomeActivity.java | dùng shared styling/routing, giữ nội dung Home |
| android-app/app/src/main/java/com/example/bivoca/ui/topics/TopicsActivity.java | retained Detail routing |
| android-app/app/src/main/java/com/example/bivoca/ui/topics/TopicDetailActivity.java | retained CTA/routing và onNewIntent |
| android-app/app/src/main/java/com/example/bivoca/ui/flashcard/FlashcardActivity.java | context/UI retention, navbar/pending guard |
| android-app/app/src/main/java/com/example/bivoca/ui/quiz/QuizActivity.java | context retention, navbar và existing exit policy |
| android-app/app/src/main/res/layout/view_learning_bottom_navigation.xml | thống nhất theo Home |
| android-app/app/src/main/res/layout/activity_flashcard.xml | navbar dưới content cuộn |
| android-app/app/src/main/res/layout/activity_quiz.xml | navbar dưới content cuộn |
| android-app/app/src/test/java/com/example/bivoca/ui/navigation/NavigationRoutePolicyTest.java | mapping/identity regression |
| android-app/app/src/androidTest/java/com/example/bivoca/LearningBottomNavigationUiTest.java | **thêm mới**, runtime GET-only + layout matrix |
| android-app/app/src/androidTest/java/com/example/bivoca/FlashcardFrontendUiTest.java | lấy VM thực đang gắn với Activity |
| android-app/app/src/androidTest/java/com/example/bivoca/TopicsUiTest.java | helper lấy đúng VM Flashcard |
| android-app/app/src/androidTest/java/com/example/bivoca/TopicDetailUiTest.java | helper lấy đúng VM Flashcard/Quiz |
| docs/reports/M16_PHASE_E_NAVIGATION_REPORT.md | báo cáo này, thêm mới |
| WORK_STATUS.md | append bàn giao, giữ byte lịch sử |

16 file Android có trước được sửa, một test mới; hai file bàn giao. Không dùng git add toàn bộ.

## Kiểm thử

Build bằng JBR Android Studio, Gradle wrapper9.5/AGP9.3.3, Debug com.example.bivoca, API37; Emulator emulator-5554. Cài APK/app-test bằng install -r, không uninstall/clear. Guard leaveApks=true, keepInstalledApks=true, uninstallIncompatible=false đã xác minh trước instrumentation.

| Kiểm tra | Kết quả và bằng chứng |
|---|---|
| assembleDebug + testDebugUnitTest + lintDebug + assembleDebugAndroidTest | PASS, build-final-identity.log:9s,19 executed/59 up-to-date; unit chạy lại sau thay đổi Java |
| Unit | PASS110/110,25 suites,0 failure/error; gồm route/NEW limit identity, QuizBackPolicy và state tests có trước |
| Lint | PASS0error,436warning; không gọi là sạch warning. Phase D ghi435warning; hiện có cảnh báo TooDeepLayout ở hierarchy Flashcard sau khi thêm wrapper cố định navbar. Chưa refactor hierarchy nội dung đã nghiệm thu để giảm warning |
| Runtime navigation, native1080×2400/density420/font1.0 | PASS2/2,78.557s, verification-font1.log |
| 412×915dp/font1.3 (1082×2402px,density420, làm tròn) | PASS2/2,80.651s, standard-font13.log |
| 320×640dp/font1.0 (840×1680px,density420) | PASS2/2,79.755s, small-font1.log |
| 320×640dp/font1.3 | PASS2/2,78.245s, small-font13.log |
| 412×915dp/font1.0 | PASS2/2,78.967s, standard-font1.log |
| Hồi quy A/C/D | PASS16/16,135.46s; HomeRuntime1 + HomeResponsive1 + Topics2 + TopicDetail5 + FlashcardFrontend7, regression-a-c-d.log |

Mỗi lượt navigation có: Home cùng tab không tạo mới; Home→Topics→Food Detail→Browse; Browse thẻ2/mặt sau trở lại giữ nguyên VM/list reference/index/face; Review→Quiz→Review giữ VM/queue/counters; CTA NEW thật trên Home giữ VM/list/face qua Review/Home; queryFood/filterSemantic giữ khi quay lại; Quiz câu hỏi đầu/answered0 giữ qua tab và rotation; tất cả5 màn xoay portrait→landscape→portrait giữ VM; Drawer phải và Back đóng trước; kiểm tra tối đa1 Activity sống/mỗi loại trước/sau luồng.

Bài layout matrix inflate5 XML ×3size (320×640,412×915,731×411) ×2font =30 cấu hình mỗi lượt. Kiểm tra4cột/vùng chạm/icon22/label12/chữ không cắt và navbar sát đáy. Runtime kiểm tra selected/màu/typeface/navbar không bị system bar che. 320/font1.3 có nhãn Trắc nghiệm xuống2dòng, không cắt chữ; giữ min-height/measurement tự nhiên của navbar Home hiện có. Nội dung dài cuộn phía trên navbar.

Các FAIL trung gian được giữ: pre-red guard chưa copy nên lần đó không phải RED; red-unit compile thiếu feature; red-new-limit-identity test bắt NEW key lệ thuộc limit; testAPK compile lỗi lambda helper và instrumentation helper gặp Typeface null (inactive default Normal hợp lệ). Đã sửa và chạy lại; xem log tương ứng, không xóa bằng chứng FAIL. Review source độc lập không phát hiện lỗi cần sửa; góp ý dùng CTA NEW thật đã đưa vào regression.

## Ảnh runtime và TasteSkill

Evidence ignored: docs/exports/m16-phase-e-navigation-2026-11-02/. Mỗi nhóm font1, standard-font1, standard-font13, small-font1, small-font13 có home.png, topics.png, topic-detail.png, flashcard-front.png, flashcard.png (Browse mặt sau), review-today.png, new-today.png, quiz.png, home-rotated.png. Đây là ảnh UI Android/API thật; không mockup/AI. Ảnh Home chứa thông tin tài khoản/leaderboard hiện có, chỉ lưu evidence cục bộ ignored, không đưa vào memory/skill hoặc Git.

| Skill | Đã áp dụng |
|---|---|
| design-taste-frontend | Đối chiếu hierarchy/content và một navbar dùng chung5màn; cố định navbar tách nội dung cuộn; giữ thiết kế A–D |
| gpt-taste | Kiểm tra nhãn/contrast cyan-active, secondary-inactive, Bold/Normal qua assertion và ảnh thật ở hai font |
| high-end-visual-design | Đồng nhất icon22dp, label12sp, alignment4cột và touch≥48dp; kiểm tra wrap trên320dp, không thêm effect/font/gradient |
| bivoca-quality-handoff | Baseline/hash/guard APK, test theo phạm vi, phân biệt testPASS với nghiệm thu, append WORK_STATUS và công bố giới hạn |

Đánh giá theo native Java/XML; không áp dụng layout web/AIDA/GSAP, không tự thiết kế lại hoặc chấm điểm thị giác giả.

## Bảo vệ dữ liệu và giới hạn

- Integrity trước bàn giao: baseline450file,434 nguyênhash,16 sửa đúng whitelist,1 mới,0unexpected; .env nguyênhash. Home XML, Auth/Login/Register/Forgot, API/DTO/repository/VM/business source, Backend/Admin giữ nguyên. Mọi thay đổi/untracked có trước được giữ; không restore báo cáo D của người dùng.
- GET health/database connected, GET flashcards total20000, GET topics38; catalog hiển thị36 =29semantic+7POS theo API hiện có, không nhầm với tổng38. Không SQL sửa dữ liệu; không migration/seed/import/reset/restore/M15.7 Apply. Count qua API xác nhận tổng, không phải checksum toàn bộ từng hàng database.
- Người dùng tự đăng nhập Google lại khi JWT cũ hết hạn và xác nhận vào Home; agent không logout/clear/reset. GET auth/me200/user hợp lệ; token chỉ dùng RAM, không in/lưu. Mở Home trực tiếp qua adb bị Android chặn vì non-exported (đúng manifest); mở lại qua launcher chính thức đã vào Home. Không thay configGoogle/client/dependency hoặc thao tác Cloud trong Phase E; chưa chạy lại chooser E2E sau cùng.
- **NOT_RUN:** Review có queue không rỗng/pending write/ACK/error sau POST; user hiện không có từ đến hạn (runtimeReview0/0), nên chỉ xác minh retention của queue rỗng/counters thật. Không tạo dữ liệu để giả có Review.
- **NOT_RUN:** Quiz đã trả lời/selected-result/CONFIRM_EXIT và SUBMITTING E2E vì cần POST Quiz Answer vào DB test cô lập chưa được duyệt. Unit QuizBackPolicy và sourceguard được kiểm tra; runtime chỉ QUESTION/answered0/BackLEAVE. Không coi các trường hợp ghi này là PASS.
- Không đếm HTTP request bằng network spy; object identity/queue identity và code ensureLoaded/factory xác nhận không khởi tạo lại khi re-entry trong luồng đã chạy. Không cam kết tất cả request nền không lặp (Home vẫn refresh GET theo onResume có trước).
- Không kiểm thử process death/thiết bị thật/release hoặc TTS bằng tai; không sửa để mở rộng phạm vi. Không POST Review/QuizAnswer/Favorite, không thay dữ liệu cá nhân học tập.
- Emulator khôi phục1080×2400,density420,font1.0,rotation1/0; xác minh ở emulator-restored.txt. Git HEADa4a9801, staging rỗng; **CHƯA STAGE/CHƯA COMMIT/CHƯA PUSH**.

Dừng tại READY_FOR_ACCEPTANCE để người dùng xem ảnh và nghiệm thu; không triển khai milestone khác.

## Ảnh để nghiệm thu

- [home,412×915/font1.0](../exports/m16-phase-e-navigation-2026-11-02/standard-font1/phase-e-navigation/home.png)
- [topics,412×915/font1.0](../exports/m16-phase-e-navigation-2026-11-02/standard-font1/phase-e-navigation/topics.png)
- [topic-detail,412×915/font1.0](../exports/m16-phase-e-navigation-2026-11-02/standard-font1/phase-e-navigation/topic-detail.png)
- [flashcard,412×915/font1.0](../exports/m16-phase-e-navigation-2026-11-02/standard-font1/phase-e-navigation/flashcard.png)
- [quiz,412×915/font1.0](../exports/m16-phase-e-navigation-2026-11-02/standard-font1/phase-e-navigation/quiz.png)
