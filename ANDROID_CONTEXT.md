# ANDROID_CONTEXT.md — Chi tiết Kỹ thuật Ứng dụng Android BiVoca

## M16.5 — Khoảng cách thẻ và ví dụ trong suốt (2026-10-28)

- Mới nhất chỉ XML: flashcardViewport marginBottom12 thay36; công thức resize80% không đổi nên card dài thêm24dp và nút không đổi vị trí giữa hai mặt. Background khung ví dụ gỡ hẳn, không nền/viền; padding14 và audio giữ nguyên. Drawable fc16_example còn là resource chưa dùng, không xóa source ngoài phạm vi.
- Build/lint/test APK PASS18s, lint0 lỗi392warnings; unit không chạy lại cho XML-only. Instrumentation3/3 PASS31.220s gồm24 phép đo layout, X/Y/size nút đồng bộ và API thật; không gửi rating/Quiz/Favorite. Cài-r, không đổi DB/AVD/font. Source khác giữ hash, evidence example-transparent-20261028/. Chờ duyệt, không tự DONE/commit/push.

## M16.5 — Thứ tự hành động mới nhất (2026-10-28)

- flashcardControls đứng trước reviewSection; rating có marginTop12 thay marginBottom12. Cả hai mặt dùng cùng resize80%, bỏ nhánh back+24/cap96 để vị trí nút không nhảy khi lật. Browse prev/next mặt sau INVISIBLE giữ chiều rộng, mặt trước VISIBLE; NEW/REVIEW GONE như cũ. Nội dung dài vẫn cuộn trong card/trang.
- Build12s/unit84/lint0 lỗi391warnings PASS; FlashcardFrontendUiTest3/3 PASS30.227s, kiểm tra nút cùng X/Y/width/height giữa hai mặt, rating phía dưới, ví dụ và nút không bị cắt trên dữ liệu thật. Cài-r, không ghi học/DB, không đổi AVD; font1.0. Chưa chạy riêng runtime font1.3 cho lượt này, measurement24 tổ hợp gồm font1.3 PASS. 3 file UI/test, source còn lại giữ hash; evidence actions-order-20261028/. Chờ duyệt, không tự DONE/commit/push.

## M16.5 — Cập nhật hai mặt và ví dụ (2026-10-28)

- Mới nhất: fc16_example bỏ viền, giữ fill0C1321/radius14. resizeCardViewport front dùng0.80 thay0.75, back tăng24dp và cân theo exampleBottom với cap phần thêm96dp; min280, card/page vẫn cuộn. Không đổi margin48/36, metadata hai cột hoặc callbacks SRS. Hai mặt đều được nới, front Flip xuống nhẹ. Test thêm xác minh exampleSection nằm trọn viewport ở dữ liệu thật và nút trở lại không bị cắt.
- 3 file UI/test đổi; build cuối13s/unit84/lint0 lỗi391warnings PASS; FlashcardFrontendUiTest3/3 PASS30.612s,24 tổ hợp resource gồm font1.3. Runtime font hệ thống1.3 riêng NOT_RUN cho bản này. Cài-r giữ dữ liệu, không ghi Review/Quiz/Favorite/DB, không đổi AVD/font1.0. Evidence example-borderless-20261028/ có before/after/hash và ảnh actual-final. Bản thử back+48dp đã quan sát cắt nút đáy, sửa thành+24dp; giữ ảnh/log cũ làm lịch sử. Chờ duyệt, không tự DONE/commit/push.

## M16.5 — Phản hồi UI mới nhất (2026-10-28)

- Header FrameLayout có hai nút48dp và cột giữa margin đối xứng56dp, title22/subtitle14/mode11; mode xuống dưới số thẻ để tên chủ đề đúng tâm màn hình. Progress text14/bar10.
- flashcardViewport bỏ weight giãn kín màn. GlobalLayout listener coalesce post resizeCardViewport: mặt trước đưa tâm Flip về75% chiều cao khả dụng, min280dp; back/empty/complete dùng vùng còn lại. Không hardcode Y hoặc đổi AVD; card/page vẫn cuộn. Margin trên48/dưới36 theo yêu cầu gấp3. Listener/callback gỡ ở onDestroy.
- Metadata chuyển sang cột phải, nghĩa trái theo tỉ lệ3:2, wrap theo nội dung; cột thiếu thông tin GONE. Khung rating đổi nền111726/viền334155, giữ màu hai lựa chọn và toàn bộ callback SRS cũ.
- Bản cuối4 file UI/test, VM/API/Repository/Backend/Home/Topics/Auth giữ nguyên. Build17s/unit84/lint0 lỗi391warnings; instrumentation3/3 PASS30.448s, đo24 cấu hình có320dp/font1.3. Runtime font1.3 trước cột phải1/1 PASS55.781s, đã trả1.0; chưa lặp runtime font1.3 riêng trên cột phải mới. Ghi rõ lỗi test center detached XML đã sửa, không che log FAIL. Không gửi review/quiz/favorite, không clear/uninstall. [Ảnh mới](docs/exports/M16_5_UI_POLISH_2026-10-28.zip), [report](docs/reports/M16_5_FLASHCARD_FRONTEND_REPORT.md). Chờ duyệt, không tự DONE.

## M16.5 — Flashcard native (2026-10-27, chờ duyệt UI)

- Dùng FlashcardActivity hiện có; XML scroll trang/card, header Back/title/mode/progress, front/back, hai rating sau reveal, completion thay content khi toàn queue được API xác nhận. Không WebView/4 mức Anki/bottom nav. Theme chỉ đổi entry Flashcard, dùng palette hiện có; không đổi Home/Topics. NavigationDrawerController.attachSession opt-in không thêm shared top bar, avatar header null-safe; menu giữ Drawer/Favorite/Quiz browse.
- FlashcardViewModel thêm LiveData sessionTotal/sessionProcessed. Reset khi tải queue mới, tăng processed chỉ trong submitDueReview ack hợp lệ; failed/pending/stale callback không tăng. Ban đầu queue rỗng không phải completed. Counters giữ qua Activity recreation trong cùng VM; process death tải queue mới, không phục hồi số cũ. Không sửa correct:boolean hoặc Backend SRS/XP/Daily Goal.
- Giữ paging100 browse, next/previous, NEW_TODAY limit hiện có, REVIEW_TODAY, Settings/TTS engine. Câu ví dụ có audio qua cùng TTS/speech rate; thiếu ví dụ ẩn, không sinh nội dung. Giữ contentStatus warning nếu API cung cấp. Pending hiển thị đang lưu, khóa rating/flip/navigation trong card; lỗi giữ card.
- Build app/unit/lint/test APK PASS25s;84/84 unit, lint0 lỗi/393 warnings. XML4 state×3 size×2font PASS. App cuối có6 test case instrumentation đạtPASS qua lượt5/6 và lượt sửa helpergesture1/1; thêm font1.3 runtime1/1. Food502 thẻ, tối đa100/trang; lật/recreate/landscape/Trước-Tiếp/menu/NEW queue/empty due hoạt động. Giữ log FAIL ban đầu (chưa login, scrollTo và gesture vào system inset), không đổi app để làm test bỏ qua. Không uninstall/clear, font trả1, AVD config/size/density giữ nguyên.
- Completion sau POST thật và REVIEW_TODAY có thẻ chưa chạy trên production; không submit learning/Quiz/Favorite hoặc giả dữ liệu để chụp. Chưa nghe trực tiếp đánh giá TTS. READY_FOR_ACCEPTANCE/chờ duyệt, không M16.6; [báo cáo/ảnh/lệnh](docs/reports/M16_5_FLASHCARD_FRONTEND_REPORT.md). Evidence runtime-evidence/m16-5-flashcard/, có source-before/after/diff và verification-summary.

## M16.4 — Topics native (2026-10-27, chờ duyệt ảnh)

- Cập nhật cuối: heading CHỦ ĐỀ/LOẠI TỪ 16sp; khôi phục strokeWidth1dp cho Topics16SectionFrame, còn filter frame không viền. Vẫn4 hàng/cuộn độc lập. Build/lint PASS, resource measurement instrumentation1/1 PASS; APK-r đã cài. Không chạy lại unit/full runtime cho đổi style này.

- Phản hồi mới nhất: container chip/semantic/POS không viền, nhãn Chủ đề/Loại từ. TopicAdapter.viewportHeight dùng min(4,count), XML height khởi tạo384dp, sau đó thay bằng chiều cao bốn row đo thật. TopicsUiTest kiểm tra bốn hàng; bản mới build/unit/lint PASS83/83, 0 lỗi/393 warnings, instrumentation2/2. Không đổi source ngoài sáu file UI/test Topics; APK-r giữ dữ liệu. Các mô tả năm hàng bên dưới là lịch sử đã được thay thế.

- Phản hồi mới nhất: title Kho từ vựng, bỏ buttonOpenVocabularySearch khỏi header; giữ search field. All chip ngoài ChipGroup/HorizontalScrollView, sync chọn bằng onClick chung. Hai adapter TopicAdapter(showSectionHeadings=false) cho hai khung semantic/POS, header/count bên ngoài list; NestedScrollView trang ngoài và RecyclerView nội bộ nestedScrolling=false/disallowIntercept khi chạm. TopicAdapter đo row theo font/width, chuẩn hóa chiều cao và viewport min(5,count) hàng. Resize post sau layout, không set height trong callback layout đồng bộ. Font1.3 đã bắt và sửa lỗi viewport chiều cao cũ. Build/unit/lint PASS 83 unit/0 lỗi/393 warnings, Topics UI tests2/2 + font1.3 runtime1/1 PASS. Evidence framed-lists/, font và AVD đã trả nguyên trạng. Mô tả một list/shortcut phía dưới là lịch sử trước yêu cầu mới.

- TopicsActivity/TopicAdapter dùng palette Home, search local theo name/code/description và TopicFilter.Group (ALL/SEMANTIC/PART_OF_SPEECH/LIFE/WORK/NATURE). Category chip chỉ là mapping code topic trong UI, không sửa flashcard_topics. Catalog thật 36 nhóm; không cộng các count thành tổng Flashcard duy nhất.
- Tái sử dụng TopicsViewModel/TopicRepository/TopicApi/Topic model; không thay request/catalog hoặc Flashcard paging (100/trang). Header riêng có avatar cùng ID imageHomeAvatar/textHomeAvatar/buttonHomeMenu. NavigationDrawerController bỏ shared top bar chỉ cho HOME/TOPICS, giữ END Drawer/insets. Không Back ở Topics.
- view_learning_bottom_navigation + LearningBottomNavigation dùng chung Home/Topics; giữ bốn ID/tab/flags và màu active theo route. view_navigation_avatar trích từ Home header actions; Home không đổi card/dữ liệu. Resource Topics: topics_frontend.xml, item_topic_chip, vector topics16_*; RecyclerView section semantic/POS, count API thật, không Mastery badge giả.
- Build/unit/lint/test APK cuối PASS; 83 unit, 0 lint errors/391 warnings. TopicsUiTest + HomeResponsiveUiTest + HomeRuntimeUiTest 4/4, Topics runtime font1.3 thêm1/1 PASS. Đo Topics 320/412/731dp ở font1/1.3, runtime landscape có ảnh. APK -r, font về1, AVD giữ nguyên. Error/loading dùng state cũ; chưa ép lỗi mạng runtime.
- [Báo cáo/ảnh/giới hạn](docs/reports/M16_4_TOPICS_FRONTEND_REPORT.md); evidence runtime-evidence/m16-4-topics/. READY_FOR_ACCEPTANCE, chưa M16.5; không thay Backend/DB/auth/algorithms.

## M16.3 — Home native theo demo (2026-10-25)

- 27/10 Final Polish mới nhất: header tách resource view_home_header (default và w440dp) + actions chung; slogan 12sp/max 2, màn hẹp chuyển slogan xuống hàng đủ rộng. SRS dùng Home16 palette/radius 20, title16/secondary12, Info ImageButton 48dp giữ ID; due row selector enabled/disabled, nhãn lấy dueNow, mũi tên chỉ khi enabled. Leaderboard Home tên XP/subtitle tổng XP. Giữ bốn tab và không footer/Quick Stats. Build/unit/lint cuối PASS 81/81, 0 lỗi/398 warnings; UI tests 2/2 + runtime font1.3 1/1 PASS, đo 12 cấu hình. AVD không đổi/font trả 1.0; app cài -r, Home đang mở và giữ session. Chi tiết [Final Polish](docs/reports/M16_3_HOME_FINAL_POLISH_REPORT.md); chờ người dùng duyệt bản cuối, chưa M16.4.

- 27/10 mới nhất: xóa card mascot/slogan cuối Home và binding homeMotivationMascot; mascot chỉ bind homeHeaderMascot. Test responsive cập nhật lastCard=cardHomeLeaderboard. Header thương hiệu, avatar mở Drawer và bốn tab giữ nguyên. Build app/test-APK PASS; chỉ cài APK chính -r.

- 27/10 mới nhất: Home header trái dùng homeHeaderMascot + home16_motivation; bỏ textWelcome/buttonHomeProfile. buttonHomeMenu đổi từ ImageButton hamburger thành FrameLayout chứa imageHomeAvatar/textHomeAvatar (chuyển sang phải). Controller giữ nguyên, tự bind LocalAvatarStore/chữ đầu và mở Drawer END qua ID cũ. HomeActivity bind cùng mascot bitmap cho header/footer; không thêm ViewModel/API. Header slogan wrap_content, không giới hạn dòng, có thể tăng chiều cao khi màn nhỏ/font lớn. Build/lint/test-APK compile PASS, manual avatar→Drawer→Back PASS; chưa chạy lại instrumentation. Các mô tả avatar trái/hamburger phải ở dưới là lịch sử đã bị yêu cầu mới thay thế.

- 27/10: phản hồi mới đổi bottom navigation từ 5 sang 4 tab, bỏ homeTabProfile khỏi XML/binding/runtime test. Bốn tab weight=1 chia đều; Profile vẫn từ buttonHomeProfile và Drawer. Build app/test APK PASS; chỉ cài APK chính -r, không cài lại các biến thể thử nghiệm vừa gỡ. Các mô tả 5 tab bên dưới là lịch sử.

- 26/10: SRS mới trong view_home_srs.xml + home_srs.xml/home_srs_* drawable, activity_home chỉ include. HomePresentation.srsGroups trả 4 nhóm theo thứ tự [0+1, 2+3, 4, 5]; tổng vẫn reviewedCards. % thuần thục = mức 5 / reviewedCards, khác accuracy. HomeActivity chỉ render nhóm/segment/dueNow, CTA delegate nút Review có sẵn, help giải thích mapping. Nhóm rỗng không vẽ segment; thiếu dữ liệu dùng —. 81 unit, lint 0 lỗi/391 warnings, responsive 1/1 PASS. Runtime tài khoản mới 0 từ/0 đến hạn, chưa kiểm thử nhấn CTA khi due>0 trong lượt này; nguồn điều hướng Review giữ nguyên.

- Visual refinement tiếp theo: font stats 17/11sp, SRS 18/11/10sp, includeFontPadding=false riêng Home; menu có inset shape 36dp trong target 48dp; CTA MaterialButton custom gradient + iconGravity=textStart, giữ ID/listener. Avatar gradient Home riêng, SRS border/tile/badge và medal rank dùng dữ liệu thật. Không đổi điều hướng/VM/Repository.
- Build/unit/lint PASS, 81/81 unit + 2/2 instrumentation; test harness chờ focus/animation khi quay về Home. AVD không đổi. Ảnh Demo–Before–After cùng evidence tại runtime-evidence/m16-3-home/refinement/; chờ người dùng duyệt trực quan, không khẳng định pixel-perfect.

- HomeActivity tái sử dụng ProfileViewModel/ProgressViewModel/LeaderboardViewModel. Không thêm Home Repository/ViewModel. Layout activity_home.xml + home_frontend.xml và home16_* drawable; theme riêng Theme.BiVoca.Home.
- NavigationDrawerController chỉ thay nhánh HOME: không chèn shared top bar cũ, avatar trái mở Profile, hamburger phải mở Drawer END cũ. Insets system bars/cutout vẫn áp dụng, bottom tabs nằm ngoài ScrollView. Các màn khác giữ cơ chế cũ.
- HomePresentation chia memoryLevels thành mức 5 / 2–4 / 0–1, đếm reviewedCards; accuracy từ summary. Preview leaderboard top 2 + current user không trùng, giữ rank thật. Backend chưa có weekly/league, subtitle ghi rõ bảng tổng XP.
- Build/lint PASS, 81/81 unit; HomeResponsiveUiTest và HomeRuntimeUiTest 2/2 PASS qua Backend thật. Đo 320x640/411x731/412x915/731x411dp với font 1.0/1.3. Runtime portrait/font 1.3/landscape có ảnh, không đổi density/resolution trong M16.3; override 370 dpi là trạng thái đã có từ lượt trước.
- Read-only checksums 23 bảng trước/sau runtime không đổi. Chỉ điều hướng/GET, không submit Review/Quiz, không sửa auth/Backend/DB. Báo cáo: docs/reports/M16_3_HOME_FRONTEND_REPORT.md; evidence android-app/runtime-evidence/m16-3-home/. Chờ duyệt UI.

## M16.2 — Register & Forgot Password (2026-10-25)

- activity_register/forgot_password dùng theme M16.1, login_ui_field/primary; styles/text mới trong auth_ui.xml. Header riêng hai màn dùng view_auth_brand_header.xml, crop mascot gốc; title auto-size 12–20sp/max 2 dòng, slogan gạch chân #7DD3FC. Back ở hàng trên.
- Register giữ 5 field/validation/2 mắt, footer ngay sau button; Forgot giữ API và phản hồi chung, thêm lock 64dp/callout Google/back outline. Activity chỉ đổi binding UI/mascot/loading, ViewModel/Repository/AuthApi/SessionManager nguyên trạng.
- 76/76 unit + 5/5 AuthScreensUiTest PASS; build/lint PASS, 0 errors/322 warnings. Header sửa đo chiều cao slogan khi font 1.3.
- Resource measurement 320x640, 411x731, 411x914, 731x411dp ở scale 1.0/1.3 PASS. Runtime native 1080x1920 và Forgot landscape/font 1.3 có ảnh; Register IME scroll/recreation/navigation có instrumentation. Chưa chạy AVD native 1080x2400 riêng hoặc request đăng ký/reset thật.
- Không thay Login: protected-files-verification.json xác nhận SHA-256 11 file giữ nguyên. APK cài -r, cấu hình thử font/rotation đã trả về ban đầu; không sửa AVD config.
- Báo cáo: [M16.2](docs/reports/M16_2_REGISTER_FORGOT_UI_REPORT.md); evidence: android-app/runtime-evidence/m16-2-auth/. Chờ duyệt UI, không chuyển M16.3.

## M16.1 — Login reference UI và spacing fix (2026-10-25)

- Display Emulator đã chỉnh native 1080x1920, density 420 trong config.ini của AVD; bỏ wm size override cũ gây letterbox. showDeviceFrame=no, skin.name=1080x1920. Khởi động lại và adb wm size xác nhận Physical size 1080x1920. Cấu hình gốc lưu ở config-before-compact-display-20261025.ini cạnh config.ini. App data giữ nguyên, reverse 3000 đã khôi phục.

- Sau phản hồi Emulator còn bản cũ: đã cài APK Header + Labels bằng adb install -r, mở lại Login và xác minh UI dump có slogan + Tài khoản. Dữ liệu ứng dụng giữ nguyên.

- Bản Header + Labels mới nhất: mascot 48dp, BiVocal 26sp, heading 20sp, slogan 13sp, label Tài khoản/mật khẩu 14sp; chỉ đổi XML/resources. Build/unit/lint PASS. Người dùng yêu cầu chỉ build và thông báo, không cần ảnh; chưa nghiệm thu runtime phiên bản này.

- Login có theme riêng, nền #090D16, field 52dp, nút gradient và icon Google; mascot crop từ asset chính thức. Auth logic giữ nguyên.
- Toàn khối header/form/Google/footer căn giữa trong ScrollView fillViewport; không còn spacer giãn, footer cách Google 32dp.
- Build/unit/lint PASS, kết quả unit 76/76; lint 0 errors/290 warnings. APK cài -r giữ dữ liệu.
- Emulator có ảnh dọc, landscape cuộn đến footer và font scale 1,3. Docked IME cuối chưa xác minh ổn định. Chờ người dùng duyệt giao diện.
- Báo cáo: [M16.1](docs/reports/M16_1_LOGIN_UI_IMPLEMENTATION_REPORT.md). Ảnh cuối: android-app/runtime-evidence/m16-1-login/08-login-spacing-fixed.png.

> Các báo cáo milestone nằm trong `docs/reports/`; xem [mục lục](README.md). Tên báo cáo không kèm thư mục ở các mục lịch sử được tra trong thư mục này.

**Ngày cập nhật:** 2026-10-24  
**Kiến trúc hiện hành từ M13.8: Online-Only.** Android gọi Retrofit → Express API → PostgreSQL; Room/NetworkFirst không còn trong source set, dependency hoặc APK. M8 vẫn là mốc lịch sử đã nghiệm thu; source Room cũ được giữ ngoài source set tại `android-app/historical-room/`. Xem `M13_8_QUIZ_QUALITY_ONLINE_ONLY_REPORT.md`.

**M13.8 Android (2026-10-23): DONE.** Debug build, 73/73 unit tests, lint 0 lỗi và 3/3 instrumentation PASS. E2E AVD API 35 trên Backend/DB cô lập: Login, Home, 19 Topics, Flashcard/flip/Previous/Next/rotation, Search, Quiz 10 câu, Result, Progress/XP, Backend down/error và Retry đều PASS. Settings không còn Data/Offline. APK được cài bằng `adb install -r`, không clear dữ liệu.

**M14 Android (2026-10-23): GOOGLE_PRODUCTION_DONE / PASSWORD_RECOVERY_LOCAL_DONE / PASSWORD_RECOVERY_PUBLIC_HTTPS_PENDING.** Login có Credential Manager
`GetSignInWithGoogleOption`, challenge/nonce Backend và chỉ lưu Session sau JWT BiVoca. Profile
có liên kết Google với password re-auth; logout clear Credential Manager state. Forgot gọi API
thật, không giả thành công khi SMTP thiếu. Debug/E2E build, 75/75 unit, lint 0 lỗi/275 warning,
3/3 instrumentation PASS. Google E2E thật đã PASS trên AVD: liên kết account vào user 55,
logout/login lại trên production, Home, JWT và force-stop/relaunch giữ session; đúng user 55 với
10 progress/9 Quiz/30 XP, không tạo user trùng. `connectedDebugAndroidTest` 3/3 PASS sau deploy.
Password Recovery local đã gửi/nhận Gmail thật và reset thành công; JWT cũ bị thu hồi, đăng nhập
mật khẩu mới và Home/API PASS. HTTPS public còn thiếu. Ảnh:
`android-app/M14_google_production_home.png`. Xem
`M14_GOOGLE_SIGNIN_PASSWORD_RECOVERY_REPORT.md`.

**M15 Android (2026-10-24): DONE / PRODUCTION_DEPLOYED.** Home
có card mục tiêu thật, progress bar, nút Học từ mới và số SRS đến hạn. New Learning dùng API
phân trang giới hạn, chỉ ghi nhận sau Đã nhớ/Chưa nhớ. Progress có custom View biểu đồ từ mới và
lượt ôn 7 ngày; Settings đọc/ghi goal 5/10/15/20 qua Backend và rollback UI khi lỗi. Debug/E2E
build, 75/75 unit và lint PASS; 3/3 instrumentation PASS trên DB cô lập. AVD E2E cô lập:
0/10 → học `apple` → 1/10 → Settings 5 → 1/5; force-stop giữ dữ liệu. Sau migration production,
APK Debug được cài bằng `adb install -r`, không clear data. Google Account Chooser/login/JWT,
Home 0/10, Progress 7 ngày, Settings goal đồng bộ 10 và Profile 30 XP đều PASS qua cổng 3000;
không Review/Quiz/đổi goal trên production. Ảnh production: `android-app/M15_production_google_home.png`,
`android-app/M15_production_progress.png`, `android-app/M15_production_settings_goal.png`,
`android-app/M15_production_profile.png`. Các cải tiến khoảng trống Home, chú giải biểu đồ,
lịch sử 7 ngày, nhãn UTC và giải thích tỷ lệ ghi nhớ được ghi cho M17. Xem
`M15_DAILY_GOALS_PROGRESS_REPORT.md`.

**M14 Google-only Forgot clarification (2026-10-23):** tài khoản tạo bằng Google không có mật
khẩu BiVoca nên không thể dùng Quên mật khẩu. Forgot UI nay nói rõ email phải thuộc tài khoản có
mật khẩu và hướng dẫn Google-only user quay lại chọn `Tiếp tục với Google`. APK Debug đã cài lại
giữ dữ liệu; nội dung mới hiển thị đầy đủ trên AVD. `assembleDebug`, `testDebugUnitTest` và
`lintDebug` PASS.

**M14 hotfix `Route not found`:** Debug resolve tới `10.0.2.2:3000`, E2E override tới
`10.0.2.2:3001`. Cổng 3000 đang là Backend `dist` cũ và trả 404 trước Credential Manager;
cổng 3001 trả challenge 200. `AuthRepository` nay ánh xạ lỗi Google sang thông báo tiếng Việt
theo HTTP/error code, không hiện message kỹ thuật của server. APK E2E mới đã cài, ADB xác nhận
request challenge POST/200 và Google Account UI đã mở. Build E2E, 75/75 unit, lint 0 lỗi PASS.
Sau liên kết, Google login trả 200; `auth/me`, progress và gamification đều 200. Home giữ đúng
30 XP/10 từ đã học, DB giữ 3 users và đúng một Google identity.

**M13.6 Android production (2026-10-23): PASS.** `assembleDebug testDebugUnitTest` BUILD SUCCESSFUL; bộ M13.6 trước đó có 96/96 unit tests và lint 0 lỗi. APK cài `adb install -r` trên AVD API 35, giữ phiên `duyphuc`. Topics nhận đủ 16 chủ đề production; Health hiển thị 39 thẻ. Bản sao Room + WAL + SHM xác minh 16 topic metadata và 9 Flashcard Health mới ID > 300, gồm `diarrhoea` 310 và `therapy` 347. Staging trước đó cũng PASS 16 topics/358 cards, hai nghĩa `breakfast`, Quiz `Câu x / y`, offline Room và reconnect. Xem `M13_6_VOCABULARY_REVIEW_AND_PUBLISH_REPORT.md`.

**M13.7 Android production (2026-10-23): PASS.** PostgreSQL có đúng 5.000 Flashcard và
19 chủ đề. AVD API 35 cài APK bằng `adb install -r`; Topics nhận 19 chủ đề, Daily Life
595 thẻ, search `singular` trả đúng 1 kết quả, Quiz Daily Life tải 10 câu. Room snapshot có
19 topic metadata, 664 thẻ đã cache và 594 thẻ mới ID > 364. Sau khi tháo reverse,
force-stop/relaunch, Topics và Daily Life `1/595` vẫn đọc được Room; Next sang `2/595`.
Reverse đã khôi phục. Android build/unit PASS và 8/8 instrumentation PASS. Xem
`M13_7_5000_FLASHCARDS_REPORT.md`.

---

## 1. CẤU HÌNH DỰ ÁN & SDK
- **Package Name / Namespace:** `com.example.bivoca`
- **Application ID:** `com.example.bivoca`
- **Ngôn ngữ:** Java (Java 11 source/target compatibility)
- **Compile SDK:** 37
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 37
- **Android Gradle Plugin (AGP):** 9.3.3
- **Gradle wrapper:** 9.5.0
- **Lưu trữ local hiện hành:** SessionManager/SharedPreferences và avatar app-private; không Room database
- **Quyền Manifest:** `android.permission.INTERNET`, query intent `android.intent.action.TTS_SERVICE`
- **Cấu hình mạng:** `android:usesCleartextTraffic="true"` (cho phép gọi HTTP `127.0.0.1:3000` thông qua adb reverse)

---

## 2. KIẾN TRÚC VÀ CẤU TRÚC GÓI (PACKAGE STRUCTURE)

```
com.example.bivoca
├── MainActivity.java                  # Màn hình khởi động điều hướng
│
├── data/
│   ├── api/
│   │   ├── AuthApi.java               # POST /auth/register, /auth/login, GET /auth/me
│   │   ├── TopicApi.java              # GET /topics
│   │   ├── FlashcardApi.java          # GET /topics/{code}/flashcards
│   │   ├── QuizApi.java               # GET /quiz/questions, POST /quiz/answer
│   │   ├── GamificationApi.java       # GET /gamification/me, /gamification/leaderboard
│   │   ├── ProgressApi.java           # GET /progress/:id, POST /progress/:id/review, PUT /progress/:id/favorite, GET /progress/summary
│   │   ├── RetrofitClient.java        # Retrofit công khai (không kèm token)
│   │   ├── AuthenticatedRetrofitClient.java # Retrofit có AuthInterceptor đính kèm JWT
│   │   └── AuthInterceptor.java       # OkHttp Interceptor tự thêm "Authorization: Bearer <token>"
│   │
│   ├── model/
│   │   ├── User.java                  # id, username, email, fullName, avatarUrl, createdAt
│   │   ├── LoginRequest.java, RegisterRequest.java
│   │   ├── LoginResponse.java         # user, accessToken
│   │   ├── Topic.java                 # API: id, code, name, description, imageUrl; getter count cũ không được API cấp dữ liệu
│   │   ├── Flashcard.java             # id, english, vietnamese, pronunciation, partOfSpeech, example, exampleVi, level
│   │   ├── QuizQuestion.java, QuizOption.java # Câu hỏi và lựa chọn đáp án trắc nghiệm
│   │   ├── QuizAnswerRequest.java, QuizAnswerResponse.java # Gửi đáp án và nhận kết quả chấm
│   │   ├── GamificationStats.java     # totalXp, currentStreak, longestStreak
│   │   ├── LeaderboardUser.java       # rank, username/fullName, totalXp, level, streak
│   │   ├── ProgressDto.java           # flashcardId, box, reviewCount, correctCount, wrongCount, isFavorite, lastReviewedAt
│   │   ├── ProgressSummary.java       # trackedCards, reviewedCards, favoriteCards, masteredCards, totalReviews, accuracy
│   │   ├── ReviewRequest.java         # correct (boolean)
│   │   └── FavoriteRequest.java       # isFavorite (boolean)
│   │
│   └── repository/
│       ├── AuthRepository.java        # Đăng ký, đăng nhập
│       ├── TopicRepository.java       # Retrofit API trực tiếp; callback lỗi khi mất mạng
│       ├── FlashcardRepository.java   # Retrofit API trực tiếp theo topic
│       ├── VocabularyRepository.java  # Search/phân trang API trực tiếp
│       ├── QuizRepository.java        # Tải câu hỏi và gửi kết quả trắc nghiệm
│       ├── ProfileRepository.java     # Lấy thông tin user và gamification stats
│       ├── LeaderboardRepository.java # Lấy leaderboard và xử lý trạng thái API/mạng
│       └── ProgressRepository.java    # Lấy tiến độ thẻ, lưu kết quả ôn tập, toggle yêu thích, lấy tổng kết tiến độ
│
├── ui/
│   ├── auth/
│   │   ├── LoginActivity.java, LoginViewModel.java
│   │   └── RegisterActivity.java, RegisterViewModel.java
│   ├── home/
│   │   └── HomeActivity.java          # Chào mừng username, điều hướng sang Topics, Quiz, Profile, Progress, Logout
│   ├── topics/
│   │   ├── TopicsActivity.java, TopicsViewModel.java, TopicAdapter.java # RecyclerView 10 chủ đề kèm emoji
│   │   └── (item_topic.xml)
│   ├── flashcard/
│   │   ├── FlashcardActivity.java     # Lật thẻ (mặt trước/mặt sau), TTS phát âm, Favorite (❤️), Đánh giá nhớ (Chưa nhớ / Đã nhớ), nút mở Quiz
│   │   └── FlashcardViewModel.java    # LiveData, SavedStateHandle chống mất trạng thái khi xoay
│   ├── quiz/
│   │   ├── QuizActivity.java          # Màn hình trắc nghiệm 4 đáp án, tính điểm, highlight đúng/sai
│   │   └── QuizViewModel.java         # State machine: LOADING -> QUESTION -> SUBMITTING -> RESULT -> FINISHED
│   ├── profile/
│   │   ├── ProfileActivity.java       # Màn hình hồ sơ cá nhân & thành tích gamification (XP, Streak, Kỷ lục)
│   │   └── ProfileViewModel.java      # Quản lý thông tin user và gamification stats
│   ├── progress/
│   │   ├── ProgressActivity.java      # Màn hình thống kê tiến độ học tập (Tỷ lệ ghi nhớ %, từ đã học, đã thuộc, yêu thích...)
│   │   └── ProgressViewModel.java     # Quản lý dữ liệu tổng kết tiến độ từ backend
│   └── leaderboard/
│       ├── LeaderboardActivity.java   # Danh sách, loading/empty/error/retry
│       ├── LeaderboardAdapter.java    # Hạng, tên, XP, level, streak
│       └── LeaderboardViewModel.java  # Giữ state khi xoay, hủy request
│
└── utils/
    ├── SessionManager.java            # SharedPreferences, xóa JWT có so khớp
    └── AuthSessionExpiry.java          # Xử lý 401 tập trung, điều hướng Login một lần
```

---

## 3. DANH SÁCH MÀN HÌNH & LUỒNG ĐIỀU HƯỚNG HIỆN CÓ

| Activity | Khai báo Manifest | Trạng thái Code | Mô tả |
| :--- | :---: | :---: | :--- |
| `LoginActivity` | ✅ (Launcher) | Hoàn chỉnh | Đăng nhập tài khoản, lưu JWT vào SessionManager |
| `RegisterActivity`| ✅ | Hoàn chỉnh | Đăng ký tài khoản mới qua backend |
| `HomeActivity` | ✅ | Hoàn chỉnh | Trang chủ: lời chào username, nút Học chủ đề, nút Làm quiz, nút Hồ sơ cá nhân, nút Tiến độ học tập, nút Đăng xuất |
| `TopicsActivity` | ✅ | Hoàn chỉnh | Hiển thị 10 chủ đề tải từ API thật `/topics`, click mở Flashcard |
| `FlashcardActivity`| ✅ | Hoàn chỉnh | Thẻ lật từ vựng (Front: Anh/Phiên âm; Back: Việt/Ví dụ), Nút Favorite (❤️), Ôn tập ("Chưa nhớ" / "Đã nhớ" auto-advance), TextToSpeech UK/US, nút "Luyện tập Quiz →" |
| `QuizActivity` | ✅ | Hoàn chỉnh | Làm trắc nghiệm theo chủ đề hoặc ngẫu nhiên, chấm điểm trực tiếp qua API backend |
| `ProfileActivity` | ✅ | Hoàn chỉnh | Xem thông tin tài khoản, avatar, cấp độ, tổng XP, chuỗi ngày streak, kỷ lục streak và Đăng xuất |
| `ProgressActivity`| ✅ | Hoàn chỉnh | Thống kê chi tiết: Tỷ lệ nhớ %, Số từ đã học/đã ôn tập/đã thuộc lòng/yêu thích, Số lượt ôn tập đúng/sai |

- **Luồng hoạt động đã hoàn thiện:**
  1. `LoginActivity` → `HomeActivity`
  2. `HomeActivity` → `TopicsActivity` → `FlashcardActivity` → `QuizActivity`
  3. `HomeActivity` → `QuizActivity` (bài kiểm tra tổng hợp)
  4. `HomeActivity` → `ProfileActivity` (hồ sơ và thành tích)
  5. `HomeActivity` → `ProgressActivity` (thống kê tiến độ)
  6. `HomeActivity` → `LeaderboardActivity` (xếp hạng từ API thật)

---

## 4. M8 ROOM OFFLINE CACHE (LỊCH SỬ, KHÔNG CÒN LÀ KIẾN TRÚC HIỆN HÀNH)

**DONE tại thời điểm M8:** implementation, build/unit/lint và nghiệm thu thực tế trên OPPO CPH2557 Android 15 PASS. Từ M13.8, các lớp và schema dưới đây đã chuyển sang `android-app/historical-room/`, ngoài mọi Android source set; dependency Room, offline banner và Settings Data đã gỡ. Nội dung còn lại trong mục này chỉ ghi lại bằng chứng lịch sử. Xem `M8_ROOM_OFFLINE_REPORT.md` và `M13_8_QUIZ_QUALITY_ONLINE_ONLY_REPORT.md`.

- Hai repository dùng API public qua `RetrofitClient`, nhận Context trong Activity factory để khởi tạo `RoomLearningCache` lazy. TopicsViewModel là ViewModel có repository injection; FlashcardViewModel giữ SavedStateHandle. Kiến trúc MVVM Java/XML giữ nguyên.
- Room `bivoca-learning-cache.db` v1 có `cached_topics`, `cached_flashcards` PK `(topicCode,id)`, `cache_sync`. Snapshot giữ thứ tự API và nullable metadata. Marker `topics`/`flashcards/{code}` phân biệt chưa tải (`null`) và rỗng hợp lệ (`[]`). Transaction đọc/ghi bảo vệ rows và marker; không DB query trên main, không destructive migration.
- API hợp lệ → worker ghi cache → main callback `onSuccess(list,false)`; lỗi ghi cache vẫn trả API. Mạng/timeout/HTTP500502503504 → đọc cache → `onSuccess(list,true)`; thiếu/hỏng cache báo lỗi. HTTP nghiệp vụ/auth, lỗi JSON/contract/protocol/TLS không fallback.
- UI dùng LiveData offline/loading/error/data/index. `ensureLoaded` tránh tải trùng khi xoay; cancel + requestVersion loại kết quả cũ; SavedStateHandle giữ vị trí thẻ. Banner Offline có nút kết nối lại; Favorite/Review/Quiz bị khóa, không fetch progress khi đọc cache. Các lỗi ghi sau khi mất mạng được báo, Review lỗi không chuyển thẻ.
- Không cache user data, không hàng đợi đồng bộ, không auto-download/TTL. Public cache dùng chung trên thiết bị; session vẫn theo luồng đăng nhập cũ. TTS giữ nguyên, cần voice cài sẵn để phát âm hoàn toàn offline.
- Schema export: `android-app/app/schemas/com.example.bivoca.data.local.LearningDatabase/1.json`. Nâng DB version sau này phải bổ sung migration có kiểm thử.
- Kiểm thử M8: 56 unit tests PASS (26 repository cache, 12 flashcard VM, 5 topics VM, 13 khác); build/lint PASS, 0 lỗi/195 warnings từ lượt triển khai. Năm Room instrumentation tests trên OPPO dùng DB in-memory riêng: **5/5 PASS, 0 failure/error/skipped**, chạy thành công hai lần để kiểm tra cấu hình giữ APK.
- OPPO Online: 10 topics, FOOD30 (`apple`/`bread`), ANIMALS30 (`dog`/`cat`). Ngắt `adb reverse tcp:3000`, force-stop/relaunch: Topics/Flashcard đọc cache riêng, banner Offline; FAMILY chưa tải báo thiếu cache. Lật thẻ, trước/sau và xoay giữ `bread` 2/30; Favorite/Review/Quiz disabled. TTS callback `Đã phát âm.`, chưa xác nhận âm thanh bằng tai.
- Reconnect và retry: banner tắt, dữ liệu API, nút online bật, Room `syncedAt` Topics/FOOD/ANIMALS tăng. Snapshot trước/trong offline giống nhau; sau guarded instrumentation giữ nguyên cache. Reverse và rotation được khôi phục (`tcp:3000`, `lock 0`). Phép thử ngắt Backend, không ngắt toàn bộ Internet thiết bị.
- **Bảo vệ dữ liệu khi instrumentation:** `android-app/gradle.properties` bắt buộc giữ `android.injected.androidTest.leaveApksInstalledAfterRun=true`. Lần test đầu không có thuộc tính này đã bị Gradle cleanup gỡ app và mất phiên local; đã cài lại, người dùng đăng nhập lại. Sau sửa, xác minh task `keepInstalledApks=true`, `uninstallIncompatibleApks=false`, chạy lại 5 tests PASS, app còn cài và mở Home bằng phiên cũ, cache không đổi. Không bỏ thuộc tính này khi bàn giao/chạy lại tests trên điện thoại có dữ liệu.

## 5. LEADERBOARD VÀ JWT HẾT HẠN (ĐÃ XÁC MINH)

- Backend áp dụng `authenticate` cho `/gamification/*`. Leaderboard nhận `limit` 1–100 (mặc định 20), response gồm `rank`, `userId`, `username`, `fullName`, `avatarUrl`, `totalXp`, `level`, `currentStreak`, `longestStreak`. Android gọi `GET /api/v1/gamification/leaderboard?limit=50` qua `AuthenticatedRetrofitClient`.
- Màn hình hiện có được dùng lại, có loading/rỗng/lỗi/thử lại. ViewModel giữ dữ liệu khi xoay màn hình và hủy call khi bị dọn.
- `AuthInterceptor` chỉ xóa phiên khi request có Bearer JWT nhận HTTP 401 và JWT vẫn trùng phiên đang lưu. Điều hướng Login chạy một lần; đăng nhập mới reset cờ. Không xóa phiên vì 403, lỗi mạng, timeout hoặc 5xx.
- Kiểm tra: `assembleDebug` PASS; `testDebugUnitTest` 20/20 PASS; `lintDebug` PASS (0 lỗi, 195 cảnh báo). Unit tests cover DTO JSON, empty response, network failure, HTTP 401/403 và policy 403/5xx.
- OPPO CPH2557 Android 15: cài APK, Leaderboard hiển thị danh sách API thật, quay lại Home và mở Flashcard Food. Endpoint không token trả HTTP 401. Không làm token người dùng hết hạn giả; TTS không được thử lại trong lượt này.

## 6. M10 ÔN TẬP NGẮT QUÃNG — 2026-10-14 (DONE)

- Home thêm nút **Ôn tập hôm nay** mở lại `FlashcardActivity` ở chế độ due; không tạo Activity/Room cache mới. `ProgressApi`/`ProgressRepository` gọi `/progress/due?limit=300` và `/progress/schedule` qua AuthenticatedRetrofitClient.
- `FlashcardViewModel` giữ danh sách due và trạng thái ghi Review qua xoay màn hình: không gửi trùng khi pending; chỉ bỏ thẻ sau response thành công; lỗi giữ nguyên thẻ và cho thử lại; callback cũ bị bỏ qua. Trạng thái rỗng/lỗi/retry và hoàn thành lượt ôn có riêng; dữ liệu due không vào Room public. Flashcard thường và M8 vẫn dùng cache như cũ.
- Card M10 hiển thị `memoryLevel` lấy từ API; không có trường `box` ở Android hiện tại. Progress hiển thị đến hạn, sắp đến hạn 24 giờ và đã ôn trong ngày UTC theo API; số liệu cũ vẫn giữ.
- `:app:assembleDebug`, `:app:testDebugUnitTest` **60/60 PASS**, `:app:lintDebug` PASS (0 lỗi, 203 cảnh báo). Guard `android.injected.androidTest.leaveApksInstalledAfterRun=true` vẫn có; chưa chạy instrumentation trong M10.
- OPPO CPH2557 đã kết nối lại, `adb install -r` APK M10 `Success` và giữ data. Trên tài khoản `duy` của DB M10 riêng, Home → due rỗng, FOOD `apple` trả lời sai hẹn 10 phút, đưa lịch đến hạn trong DB test, lật/xoay rồi trả lời đúng hẹn 1 ngày. `bread` xác minh ngắt reverse khi đang trả lời: giữ thẻ/báo lỗi/không ghi DB; nối lại và retry: hoàn thành, ghi đúng một lần. Progress 0 đến hạn/2 sắp đến hạn/2 đã ôn hôm nay UTC; DB có 2 progress và 10 XP không trùng award.
- Hồi quy M8 sau force-stop/relaunch khi offline: Topics và FOOD/FAMILY đọc Room, banner Offline, Favorite khóa; due yêu cầu kết nối, reconnect + Retry hoạt động. Không chạy instrumentation M10; TTS chưa được xác nhận âm thanh bằng tai. Cuối lượt đã đăng xuất phiên DB thử nghiệm, reverse trở về `tcp:3000 tcp:3000`. Tài khoản `duy` và tiến độ được giữ trong DB M10 riêng. Sau chấp thuận của người dùng, DB vận hành đã sao lưu và migration M10; backend/Admin cổng 3000/5173 đang chạy, API M10 trên DB vận hành smoke PASS chỉ đọc. Người dùng cần đăng nhập lại tài khoản ở DB vận hành để dùng app. Xem `M10_SPACED_REPETITION_REPORT.md`.

## 7. M11 LOGIN UI — 2026-10-14 (DONE TRÊN OPPO)

- Trong lượt thiết kế Login, chỉ Login đổi sang nền đêm navy XML với trăng/sao, logo ngang `bivocal_wordmark.png` tách từ ảnh mẫu đã duyệt (bản gốc khác chỉ có icon vuông), header logo | ĐĂNG NHẬP, ô email/mật khẩu có icon/viền focus, nút mắt, quên mật khẩu, nút pill xanh và liên kết Register. Theme riêng Login giữ thanh hệ thống tối; insets/IME và ScrollView hỗ trợ màn nhỏ. Register được thiết kế lại ở mục 9; Home và các màn khác không đổi.
- Backend `POST /auth/login` yêu cầu **email/password**, nên nhãn là Email thay vì Tên đăng nhập trên mô tả ảnh. Backend không có reset-password API; liên kết quên mật khẩu hiện mở màn Forgot của M11 tiếp theo, nơi ghi rõ chưa thể gửi email. Login vẫn qua `AuthRepository`, `SessionManager`/JWT cũ. `LoginViewModel` kiểm tra email hợp lệ, không gửi trùng khi loading.
- Sau chỉnh theo `M11_LOGIN_REFERENCE.png`, `:app:assembleDebug` PASS, `:app:testDebugUnitTest` **60/60 PASS**, `:app:lintDebug` PASS 0 lỗi/195 cảnh báo. OPPO cài lại bằng `adb install -r`, giữ dữ liệu; chụp `oppo-login-final.png` 1080×2400 và kiểm tra form, lỗi nhập liệu, mắt giữ nội dung, Register, bàn phím, xoay ngang giữ email/mật khẩu, cuộn, lỗi Auth thật. Người dùng tự đăng nhập và xác nhận thấy Trang chủ rồi chủ động rời Home. Liên kết Forgot đã chuyển sang màn mới trong M11 tiếp theo. Ngày 2026-10-15, Login cũng PASS hiển thị và điều hướng trên AVD Pixel 6 API 35; logo ngang là cutout từ ảnh mẫu, có thể thay bằng asset brand gốc sau. Xem `M11_LOGIN_UI_REPORT.md` và `M11_EMULATOR_ACCEPTANCE_REPORT.md`.

## 8. M11 QUÊN MẬT KHẨU — 2026-10-14 (UI CÓ, BACKEND CHƯA CÓ)

- `LoginActivity` nay mở `ForgotPasswordActivity` thay cho Toast. Màn mới dùng `Theme.BiVoca.Login`, nền/trăng/sao/logo/ô nhập/nút xanh của Login, `ScrollView` và system bar/IME insets; `Quay lại đăng nhập` dùng `finish()`.
- `ForgotPasswordViewModel` kiểm tra email trống/sai định dạng và trả LiveData thông báo. Email hợp lệ vẫn không thể gửi: Backend Auth chỉ có `/register`, `/login`, `/me`, không reset password API hoặc email service. UI nói rõ không có email nào được gửi; không tạo request, token hay trạng thái thành công giả.
- `:app:assembleDebug` PASS, `:app:testDebugUnitTest` **60/60 PASS**, `:app:lintDebug` PASS 0 lỗi/197 cảnh báo. APK cài `adb install -r` giữ dữ liệu trên OPPO; Login → Forgot PASS, ảnh OPPO thật tại `android-app/app/build/reports/m11-forgot/oppo-forgot-initial.png`. ADB mất kết nối trước khi xác minh trọn vẹn validation, quay lại, bàn phím và xoay trên OPPO. AVD API 35 nay đã có nhưng lượt nghiệm thu 2026-10-15 chỉ bao phủ Login/Register; Forgot chưa được nghiệm thu lại. Người dùng chọn phát triển API reset password sau; chi tiết tại `M11_FORGOT_PASSWORD_REPORT.md`.

## 9. M11 REGISTER UI — 2026-10-15 (DONE TRÊN EMULATOR)

- `RegisterActivity` và layout Register đã dùng lại `Theme.BiVoca.Login`, navy/trăng/sao/logo ngang/input/eye/button của Login; năm trường theo ảnh `M11_REGISTER_REFERENCE.png`, `ScrollView`/IME insets và `adjustResize`, hai mắt mật khẩu, lỗi ngay tại trường và liên kết về Login cũ. Không đổi giao diện Login.
- Backend Auth `POST /register` thực tế nhận `username`, `email`, `password`, `fullName?` và trả `user` + `accessToken`; Android vẫn báo thành công và quay về Login, không lưu JWT đăng ký. `RegisterViewModel` nay cho phép `fullName` rỗng, validation theo Backend và chặn gửi trùng; `AuthRepository` phân biệt lỗi 409 email/username, có thông báo mạng/server. Không đổi Backend/DB/API contract.
- Đã tạo AVD `BiVoca_API35_Pixel6`: Pixel 6, Android API 35 Google APIs x86_64, 1080×2400, RAM 2048 MB. `emulator-5554` xuất hiện ở trạng thái `device`; Backend health `ok`/database `connected`; `adb reverse tcp:3000 tcp:3000` giải quyết lỗi Emulator không kết nối được `127.0.0.1:3000`.
- Nghiệm thu thật trên Emulator PASS: Login → Register → Login, đối chiếu bố cục dọc, bàn phím và ScrollView, lỗi validation ngay tại trường, hai nút hiện/ẩn mật khẩu, xoay ngang không crash và giữ state trường đang nhập. Ảnh nằm tại `android-app/app/build/reports/m11-emulator/01-login.png` đến `05-register-landscape.png`.
- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` PASS; **73/73 unit tests**, 0 failure/error/skipped; lint 0 lỗi/186 cảnh báo. Không tạo account trong DB vận hành; đăng ký thành công end-to-end cần DB test riêng được phép. Xem `M11_REGISTER_UI_REPORT.md` và `M11_EMULATOR_ACCEPTANCE_REPORT.md`.

## 10. M11 NAVIGATION DRAWER — 2026-10-15 (DONE TRÊN EMULATOR)

- `NavigationDrawerController` bọc nội dung Activity hiện có bằng một `DrawerLayout` lúc chạy và inflate một `view_navigation_drawer.xml`; các Activity giữ nguyên lớp cha, ViewModel, lifecycle và layout nội dung. `NavigationRoutePolicy` ánh xạ Flashcard thường → mục Chủ đề nhưng không coi Topics là cùng màn; M10 dùng route Ôn tập hôm nay riêng.
- Đã tích hợp Home, Topics, Flashcard thường/M10, Quiz, Progress, Leaderboard và Profile. Menu có Tổng quan, Chủ đề, Ôn tập M10, Quiz tổng hợp, Progress, Leaderboard, Profile và Logout; chọn màn hiện tại chỉ đóng Drawer. Logout dùng `SessionManager.logout()` và clear task về Login, không xóa Room cache.
- `:app:assembleDebug` PASS; `:app:testDebugUnitTest` **76/76 PASS**; `:app:lintDebug` 0 lỗi/191 cảnh báo. APK cài `adb install -r` trên AVD API 35; Login/Register/Forgot có 0 control Drawer theo UIAutomator.
- Nghiệm thu runtime bằng tài khoản người dùng trên `emulator-5554`: bảy tuyến chức năng mở đúng Activity; selected state đúng ở Home/Topics/Review/Quiz/Progress/Leaderboard/Profile; Flashcard thường chọn Topics và Flashcard M10 chọn Review. Chạm scrim, Back và chọn lại mục hiện tại đều đóng Drawer đúng. Xoay ngang không crash; ScrollView đưa Logout vào vùng truy cập. Logout về Login, Back không quay lại màn đã xác thực và relaunch vẫn ở Login. Ảnh tại `android-app/app/build/reports/m11-navigation/`; xem `M11_NAVIGATION_DRAWER_REPORT.md`.

## 11. M11 HOME UI + NAVIGATION POSITION FIX — 2026-10-15 (DONE TRÊN EMULATOR)

- `HomeActivity` vẫn là Tổng quan, nay dùng Dark Night Sky bằng XML: lời chào fullName/username, avatar mặc định, XP, streak, trackedCards, dueNow, Continue Learning, bốn chức năng và mascot. Dữ liệu qua `ProfileViewModel`/`ProgressViewModel` và API thật; không mock, không tính XP/phần trăm ở Android. Offline hiển thị lỗi và Retry, điều hướng vẫn hoạt động.
- Nguyên nhân hamburger lệch là cấu trúc/padding/insets riêng của từng layout, gồm Home/Flashcard đặt nút trong ScrollView và ba Activity cộng inset riêng. `NavigationDrawerController` nay inflate `view_navigation_top_bar.xml` chung ngoài phần cuộn, chuẩn 16dp/48dp, và áp system/cutout insets một lần; ba listener trùng ở Flashcard/Progress/Leaderboard đã bỏ. Drawer controller, route và business logic giữ nguyên.
- AVD API 35 portrait 1080×2400/420dpi: Home, Topics, Flashcard, Quiz, Progress, Leaderboard và Profile đều có hamburger bounds `[42,138][168,264]`, sai lệch 0dp; landscape Home `[42,73][168,199]`. Các card Home mở đúng Activity, Drawer hoạt động, offline/retry giữ vị trí và Login/Register/Forgot có 0 control Drawer.
- `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug` PASS; **76/76 tests**, lint 0 lỗi/178 cảnh báo. APK cuối cài `adb install -r`; sau khi kiểm tra Logout/auth, AVD được khởi động lại từ snapshot giữ phiên và smoke test lại Home/thẻ Topics, nên app hiện ở Home. Ảnh/CSV tại `android-app/app/build/reports/m11-home-navigation/`; xem `M11_HOME_NAVIGATION_FIX_REPORT.md`.

## 12. M11 AUTHENTICATED DARK UI + LOCAL AVATAR — 2026-10-15

- Bảy màn authenticated còn lại dùng Dark Night Sky bằng Java/XML hiện có; không đổi MVVM/Retrofit/Room/SavedStateHandle. Shared top bar và Drawer tiếp tục do `NavigationDrawerController` quản lý.
- `TopicFilter` lọc snapshot theo name, chuẩn hóa hoa/thường/dấu và `đ`. Flashcard chỉ thêm animation UI. Quiz vẫn nhận chấm điểm từ backend. Progress/Leaderboard chỉ đổi presentation; current row so sánh `SessionManager.userId`.
- `LocalAvatarStore` lưu JPEG app-private theo userId, decode SAF URI bằng bounds/sample và ghi temp rồi rename. `ProfileActivity` dùng `OpenDocument`, giữ pending URI qua recreation, preview/confirm/cancel/reset. Home refresh trong `onResume`; Drawer refresh khi `open()`.
- Backend có `avatarUrl` đọc trong DTO nhưng không có endpoint upload/update; avatar không server sync và không vào Room public cache.
- AVD `emulator-5554`: build PASS, **80/80 tests**, lint 0 errors/185 warnings. Online read, M8 offline Topics/FOOD, due offline refusal, rotation và avatar persistence PASS. Quiz Result E2E chưa chạy vì cần quyền ghi DB vận hành; M11 tổng thể còn IN PROGRESS. Xem `M11_UI_REDESIGN_4_TO_10_REPORT.md`.

## 13. KẾT NỐI BACKEND ỔN ĐỊNH TRÊN EMULATOR — 2026-10-16

- `ApiBaseUrl.current()` nhận diện Android Emulator và trả `http://10.0.2.2:3000/api/v1/`; thiết bị thật giữ `http://127.0.0.1:3000/api/v1/` để tiếp tục dùng ADB reverse qua USB.
- `RetrofitClient` và `AuthenticatedRetrofitClient` dùng chung URL resolver. Không hard-code URL riêng trong hai client.
- Emulator không còn cần `adb reverse`; reverse vẫn là yêu cầu riêng của điện thoại thật khi gọi Backend local.
- Windows Startup đã có launcher `BIPASSION Local Admin.lnk`, tự bật Backend/Admin từ bản build sau khi đăng nhập Windows. PostgreSQL service phải sẵn sàng như cấu hình hiện có.
- Verification: AVD API 35 cold boot, reverse trống, APK cài `-r`, request từ Emulator tới `10.0.2.2:3000/api/v1/topics` trả HTTP 200; build/unit/lint PASS, 82/82 tests.
- Báo cáo: `ANDROID_EMULATOR_CONNECTION_FIX_REPORT.md`.

## 14. M12 SETTINGS — 2026-10-17 (DONE)

- Drawer có route `SETTINGS`; `SettingsActivity` dùng shared navigation chrome và `SettingsDetailActivity` tái sử dụng cho study/audio/reminder/display/data/about. Java/XML, Dark Night Sky, ScrollView và system bar insets giữ kiến trúc hiện tại.
- `SettingsPreferenceManager`: prefs theo user cho daily goal, flashcard front, auto pronunciation, rate và reminder; prefs thiết bị cho text size/reduce motion. Mặc định 10, EN, auto off, 1.0x, reminder off 20:00/all days, standard/reduce off.
- `FlashcardActivity` đọc settings trong `onResume`: VI front đảo đúng english/vietnamese nhưng mặt sau vẫn giữ IPA/metadata/example; TTS luôn đọc English, set rate, auto một lần/thẻ; reduce motion và system animator disabled bỏ hiệu ứng lật.
- `ReminderScheduler` dùng 7 `setWindow` weekly alarms; `ReminderReceiver` kiểm tra session/user/day, tạo channel và notification chung, reconcile sau boot/timezone/time change. Home reconcile sau login, Drawer logout cancel trước khi xóa session.
- `CacheSettingsRepository` + DAO đếm và transaction chỉ xóa `cached_topics`, `cached_flashcards`, `cache_sync`; không xóa token/avatar/user data. Runtime cuối đọc 10 topics/150 cards/430.3 KB.
- Build/unit/lint: PASS, 89/89 tests, lint 0 lỗi/268 cảnh báo. Instrumentation: Room 6/6 + Reminder 1/1 PASS. AVD API 35 permission, notification thật, reboot restore và cancel PASS. Emulator không có TTS engine nên âm thanh nghe được chưa xác minh.
- Bằng chứng và giới hạn: `M12_SETTINGS_REPORT.md`; ảnh ở `android-app/app/build/reports/m12-settings/`.

## 15. M12.1 SETTINGS UI/UX POLISH — 2026-10-17

- Giữ `SettingsActivity` + `SettingsDetailActivity`; chỉ bổ sung shared style và color state list trong `res/values`/`res/color`. Các control Settings phải dùng `SettingsRadioOption`, `SettingsSwitch`, `SettingsDayChip` và `SettingsActionButton` để tránh màu Material tím mặc định.
- Day chip reminder xếp hai hàng, dùng indicator check/outline và contentDescription tên ngày đầy đủ. TimePicker dùng `Theme.BiVoca.TimePicker`; action dialog được đặt cyan khi show.
- Flashcard Previous/Next dùng `flashcard_nav_*` state lists; disabled vẫn đọc được, enabled cyan, pressed có surface riêng. Không đổi navigation hay business logic.
- AVD `emulator-5554` có Google TTS (`com.google.android.tts`/`GoogleTtsService`). Callback 0.8x, 1.0x, 1.2x và auto khi chuyển thẻ PASS; âm thanh nghe trực tiếp vẫn **USER AUDIO VERIFICATION PENDING**.
- Final validation: build PASS, 89/89 unit tests, lint 0 errors/268 warnings; preference cuối auto off, rate 1.0, goal 10, EN front, reminder off/all days. Evidence: `M12_1_SETTINGS_UI_POLISH_REPORT.md` và `android-app/app/build/reports/m12-ui-polish/`.

## 16. M11.1 QUIZ RESULT E2E — 2026-10-17 (DONE)

- `QuizActivity` giữ state machine/ViewModel/Repository hiện có và bổ sung màn tổng kết total/correct/wrong/accuracy qua `QuizResultSummary`; `activity_quiz.xml` dùng Dark Night Sky và nút cyan. Android vẫn nhận kết quả đúng/sai từ Backend, không tự chấm hoặc tự tính XP.
- `HomeActivity.onResume()` tải lại profile/progress sau lần hiển thị đầu để XP, streak và tracked cards cập nhật khi quay về từ Quiz.
- `ApiBaseUrl.current()` ưu tiên `BuildConfig.API_BASE_URL_OVERRIDE` khi có. Build type `e2e` dùng package `com.example.bivoca.e2e` và `10.0.2.2:3001`; debug/release override rỗng nên giữ resolver Emulator/thiết bị thật hiện có.
- PostgreSQL test riêng `bivoca_m11_20261017_87faf608e230`, role tối thiểu, 10 topics/300 cards/6 migrations. Hai lượt Quiz đầy đủ và network retry thủ công cuối tạo 21 results, 18 đúng/3 sai, 21 progress/reviews, 180 XP và streak 1. API history/summary/gamification/progress/schedule khớp DB.
- UI duplicate lock, pre-send network failure, manual retry, rotation, Back, Làm lại, Home refresh và navigation regression PASS. Giới hạn: endpoint answer chưa có idempotency key cho trường hợp mất response sau commit; app không tự retry.
- Final validation: `assembleDebug` + `assembleE2e` PASS, **91/91 unit tests**, lint **0 errors/268 warnings**; Backend typecheck/build, **313/313 tests** và hai DB integration suites PASS.
- `flashcard_english` chỉ đọc và snapshot cuối `unchanged=true`; Backend test 3001 đã dừng, Backend vận hành 3000 giữ nguyên, DB test/evidence giữ lại. 11 ảnh runtime: `android-app/app/build/reports/m11-quiz-e2e/`; báo cáo: `M11_QUIZ_RESULT_E2E_REPORT.md`.

## 17. M11.2 AVATAR RIGHT DRAWER & UNIFIED BACK — 2026-10-17 (DONE)

- `NavigationDrawerController` vẫn là controller duy nhất, nay tạo shared top bar Back–title–avatar, mở/đóng Drawer bằng `GravityCompat.END`, width 84%/max 360dp và xử lý Back theo thứ tự Drawer → custom screen policy → back stack/fallback Home.
- Home không có Back và đã bỏ avatar riêng. `AvatarLabel` lấy ký tự đầu username; ảnh/fallback ở Top Bar và Drawer đọc `LocalAvatarStore` theo userId, refresh trong lifecycle và ngay sau Profile save/reset.
- `QuizBackPolicy`: SUBMITTING khóa Back, lượt có đáp án hỏi xác nhận, chưa trả lời/FINISHED rời bình thường. Settings Detail gắn shared controller và bỏ inset riêng. Flashcard/Quiz có title động trong shared top bar.
- Home chừa vùng chữ khỏi trăng; AVD font 1.3 với tên `M11 Quiz E2E` xuống hai dòng không overlap. Portrait/landscape, Drawer X/scrim/System Back, 9 mục/selected state, Flashcard state, offline session và logout clear task PASS.
- Final: `assembleDebug`, `assembleE2e`, `testDebugUnitTest`, `lintDebug` PASS; **96/96 tests**, lint **0 errors/269 warnings**. APK Debug cài `-r`; font scale/rotation phục hồi 1.0/auto/portrait. 15 ảnh: `android-app/app/build/reports/m11-2-avatar-right-navigation/`; báo cáo `M11_2_AVATAR_RIGHT_DRAWER_NAVIGATION_REPORT.md`.

## 18. M13 LARGE VOCABULARY CLIENT — 2026-10-21 (STAGING ACCEPTED)

- `TopicApi` dùng `/topics/catalog`; `Topic`/`TopicEntity` giữ `flashcardCount`. Room schema 1→2 có migration explicit thêm cột count mặc định 0 và schema JSON; không destructive migration.
- Thêm `VocabularyApi`, `VocabularyRepository`, `FlashcardPage` và màn `VocabularySearchActivity` Java/XML theo MVVM. Search English/Vietnamese, lọc CEFR, page 20, Previous/Next; không tải toàn bộ dataset vào bộ nhớ và không kết nối PostgreSQL trực tiếp.
- Topics hiển thị count thật cho 16 topic staging và mở Vocabulary Search. Home dùng mô tả “Học theo các chủ đề”. Quiz/Flashcard/Settings/Drawer giữ luồng cũ.
- Build Debug/E2E, unit, lint PASS: **96/96 unit tests**, lint 0 lỗi. `connectedAndroidTest` **8/8** trên `BiVoca_API35_Pixel6`; cài E2E bằng `adb install -r` giữ session/Room.
- Runtime staging cổng 3001: FOOD 43, thẻ mới A1–B2, flip/Previous/Next/search/Quiz result PASS; tắt Backend rồi force-stop vẫn đọc 16 topics/FOOD43 từ Room; Retry sau reconnect trở lại online. Settings ghi 16 topics/55 cached cards/325,5 KB; navigation regression không crash.
- 16 ảnh thật: `app/build/reports/m13-large-vocabulary/`. Production Backend chưa có migration/API M13, nên không phát hành Android build này trước khi Backend production được triển khai có phê duyệt riêng. Xem `M13_LARGE_VOCABULARY_IMPLEMENTATION_REPORT.md`.

## 19. M13.8 ONLINE-ONLY + QUIZ QUALITY — 2026-10-23 (DONE)

- Kiến trúc runtime: Android Java/XML MVVM → Retrofit → `/api/v1/` → Express → PostgreSQL. `TopicRepository`, `FlashcardRepository`, `VocabularyRepository` và `QuizRepository` không đọc/ghi Room.
- `TopicsViewModel` và `FlashcardViewModel` không còn offline state. Topics/Flashcard/Search hiển thị loading, empty, lỗi mạng chuẩn và retry; mất mạng không đăng xuất và không hiện dữ liệu cache cũ.
- Settings hiện có Học tập, Âm thanh, Nhắc học, Giao diện, Giới thiệu. Data/Room stats/clear cache đã bỏ.
- Room dependency/annotation processor/schema/source đã ra khỏi app source set. Mã M8 lịch sử ở `android-app/historical-room/`; `OnlineOnlyArchitectureTest` xác minh APK không chứa `LearningDatabase`, `RoomLearningCache` hoặc `NetworkFirstLoader`.
- Quiz Android vẫn gửi answer qua API, khóa thao tác khi `SUBMITTING` và đọc kết quả server. Lỗi thiếu câu an toàn hiển thị số lượng khả dụng; lỗi mạng dùng thông báo chung. Endpoint chưa có idempotency key cho trường hợp response bị mất sau commit.
- AVD API 35 E2E với package `.e2e`, Backend 3001 và DB cô lập: Login/Home/19 Topics; ANIMALS 30 cards, flip/Previous/Next/rotation; Search `dog`; Quiz 10 câu + Result; Progress 10 lượt/20 XP; Backend down báo lỗi và giữ session; Backend up + Retry tải lại; Settings không còn Offline. Hai ảnh tại root: `M13_8_ANDROID_ONLINE.png`, `M13_8_ANDROID_CONNECTION_ERROR.png`.
- Final validation: `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest` PASS; 73/73 unit tests, lint 0 lỗi/270 warnings, instrumentation 3/3 PASS. Backend 324/324 và Quiz DB integration 1/1 PASS. Production chỉ đọc và giữ 5.000 cards/19 topics/3 users/14 progress/12 Quiz/2 scores/5 XP.
# M15.5 Vocabulary 20k — 2026-10-25

- Không thiết kế lại UI. Vocabulary Search tiếp tục phân trang 20 thẻ/trang theo `topicCode`;
  runtime Online-Only và không tải toàn bộ `GENERAL_VOCABULARY` vào RAM. Android đã xử lý an toàn
  IPA/example nullable của artifact mới.
- Lần kiểm tra cuối `assembleDebug`, `testDebugUnitTest`, `lintDebug` BUILD SUCCESSFUL; unit-test
  task dùng lại kết quả **75/75** đã có. Bộ runtime evidence staging 20k trước đó vẫn nằm tại
  `android-app/runtime-evidence/`; không có bộ ảnh mới sau rebuild VNEDICT.
- Không cài APK hoặc nghiệm thu production vì quality gate ngôn ngữ còn 21/500 hàng cần review.
  Production vẫn 5.000 cards/19 topics; không tuyên bố Android production 20k PASS.

## M15.5 DataFix validation — 2026-10-25

- Vòng DataFix không sửa Android. `assembleDebug`, `testDebugUnitTest`, `lintDebug` BUILD
  SUCCESSFUL với Gradle 9.5; phần lớn task up-to-date.
- `adb devices -l` không có thiết bị/Emulator, nên không cài APK hoặc tạo runtime evidence mới.
- Quality gate hậu-fix vẫn FAIL 24/500; production không import và Android production vẫn dùng
  API 5.000 cards/19 topics. Không tuyên bố runtime 20K production PASS.

## M15.5 production 20K runtime — 2026-10-25

- `Flashcard` maps API `contentStatus`; Flashcard/Search display “Chưa kiểm duyệt ngữ nghĩa” for
  `IMPORTED_UNVERIFIED`. Online-only paging remains 20 items/page; no Room reintroduction.
- `testDebugUnitTest`, `assembleDebug`, `lintDebug` BUILD SUCCESSFUL. Debug APK installed with
  `adb install -r`; reverse 3000 retained.
- Emulator API 35 preserved session, loaded production Topics/Search, found 5 `society` results
  including imported rows with the visible pending-review label. Screenshot:
  `app/build/reports/m15_5_android_20000.png`.

## M15.6 grouped topics and paged flashcards — 2026-10-25

- `Topic.topicGroup` và `TopicAdapter` chia catalog thành “HỌC THEO CHỦ ĐỀ” trước “HỌC THEO LOẠI TỪ”; General và topic rỗng bị Backend loại khỏi catalog. Topics hiển thị count thật và vẫn có search.
- `FlashcardViewModel`/Repository dùng `/flashcards?topicCode=...&page=...&limit=100` cho topic lớn. Previous/Next đổi trang khi qua biên, giữ global position/total và không tải cả topic vào RAM; Review/Favorite giữ logic cũ.
- Final `assembleDebug`, `testDebugUnitTest`, `lintDebug` PASS: 76/76 tests, lint 0 lỗi/289 cảnh báo. APK cài `-r` trên `emulator-5554`, reverse 3000 giữ nguyên.
- Runtime production PASS: semantic group đứng trước POS, General ẩn, Noun 6.000, flip `abacus` → `Bàn tính.`, Next/Previous `1↔2 / 6000`, search `abacus` trả đúng nhãn unverified. Evidence ở `app/build/reports/m15_6_*.png`.

## M15.7 semantic topic enrichment — 2026-10-25

- Không sửa Android source. Contract M15.6 giữ nguyên: semantic trước POS, server-side paging limit 100, Previous/Next qua trang, không Room và không tải toàn topic.
- Production API sau enrichment: Noun 5.729/58 trang, trang cuối 29; General ẩn; 36 nhóm hiển thị. Các mapping mẫu `projection`→Arts và `kneecap`→Health đọc được qua API.
- `testDebugUnitTest`, `assembleDebug`, `lintDebug` PASS: 76/76 tests, lint 0 lỗi/289 cảnh báo. APK Debug cài `adb install -r`, reverse 3000 PASS.
- Phiên Emulator cuối ở Login nên chưa tái nghiệm thu Topics authenticated/count 5.729 trên UI; không tuyên bố UI runtime PASS. Xem `M15_7_SEMANTIC_TOPIC_ENRICHMENT_REPORT.md`.

