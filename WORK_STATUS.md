# WORK_STATUS.md — Trạng thái Thực tế và Kế hoạch Phát triển BiVoca

## Nghiệm thu Home Phase A và Login Phase B — 2026-10-29 — USER_ACCEPTED_PASS

- Người dùng xác nhận trực tiếp nghiệm thu PASS cả Home (Phase A) và Login (Phase B) sau khi xem kết quả/ảnh. Thay thế trạng thái chờ duyệt của các bản ghi bàn giao bên dưới; giữ nguyên lịch sử và kết quả test.
- Giới hạn kỹ thuật không đổi: Google/password success/SMTP E2E chưa chạy trong Phase B; màn nhỏ dùng measurement instrumentation, chưa có AVD nhỏ riêng. Nghiệm thu UI không nâng các kiểm tra chưa chạy thành PASS.
- Đã rà soát 37 file (33 Android source/resource/test + 4 tài liệu), đối chiếu hash với bản kiểm thử cuối, kiểm tra diff/secret và loại runtime-evidence/build/APK/Skill untracked khỏi phạm vi. Không sửa thêm giao diện hoặc nghiệp vụ.
- Người dùng duyệt danh sách 37 file, cập nhật ghi nhận nghiệm thu và cho phép commit/push lên `origin/feature/initial-import`. Không đổi sang main, không reset/clean hoặc gom thay đổi khác. Kết quả Git được bàn giao trong chat; các mục CHƯA COMMIT/CHƯA PUSH bên dưới thuộc thời điểm báo cáo cũ.

## Login theo thiết kế đã duyệt — Phase B — 2026-10-29 — READY_FOR_ACCEPTANCE

- Người dùng đã nghiệm thu Home Phase A trực tiếp trong chat ngày2026-10-29; đã ghi bổ sung vào báo cáo Home, giữ nguyên hồ sơ bàn giao ban đầu. Tiếp tục Phase B đúng kế hoạch được duyệt, không chỉnh lại Home.
- Chỉ XML Login/theme entry và resource riêng: mascot/wordmark/slogan trái, divider, title Đăng nhập phải cùng hàng; thêm subtitle; form màu navy/blue/cyan, input min56dp, CTA52dp, Google→footer32dp; cuộn khi hẹp/ngang/IME. Không sửa Java app, LoginViewModel/AuthApi/Repository/SessionManager/Credential Manager hoặc Register/Forgot/Backend/DB.
- APK-r giữ dữ liệu; build cuối14s PASS (7 executed/71 up-to-date). Unit84/84 đã executed ở lượt đầu Phase B, lượt cuối UP-TO-DATE; lint0 lỗi/428 warnings. Instrumentation cuối bàn phím gắn đáy4/4 PASS23.219s; font1.3 Login1/1 PASS10.567s. Resource Login28 cấu hình, hồi quy Home layout14 cấu hình; chưa AVD nhỏ riêng.
- Ảnh phát hiện Gboard nổi che form dù isDisplayed PASS; đã tăng test IME bottom>0 và tọa độ control trên mép bàn phím, chạy lại PASS/đọc ảnh. Font trả1.0, Gboard trả chế độ nổi ban đầu, AVD size/density/config hash không đổi. Người dùng tự đăng xuất trước test; cuối phiên đã có session nên giữ nguyên, không logout để chụp lại.
- Không gửi POST auth/challenge/reset hoặc ghi học/SQL để kiểm thử. Google/password success/SMTP E2E NOT_RUN; không suy từ ảnh Home thành Google E2E PASS. Source so với Home đã duyệt2 sửa/4 mới,421/423 file gốc giữ hash. Git feature/initial-import; giữ toàn bộ Phase A/Skill untracked, CHƯA COMMIT/CHƯA PUSH.
- [Báo cáo Login](docs/reports/LOGIN_APPROVED_PHASE_B_REPORT.md), evidence local `android-app/runtime-evidence/login-approved-20261029/` có comparison và ảnh font10-docked/font13-docked. Login chờ người dùng duyệt UI; không mở rộng sang màn khác.

## Home theo demo cuối đã duyệt — Phase A — 2026-10-29 — READY_FOR_ACCEPTANCE

- Người dùng duyệt Home trước, Login sau; chỉ triển khai Home Java/XML với theme/resource riêng. Header mascot/wordmark/slogan, streak `N Day`, avatar mở Drawer phải; Daily Goal gọn/CTA blue; SRS bốn nhóm màu thật/Info48dp; leaderboard Top3 và hàng riêng khi tài khoản không ở Top3; bốn tab cố định. Không sửa Login/Backend/DB/Topics/Flashcard/auth/nghiệp vụ.
- Home gọi endpoint leaderboard hiện có với limit100, màn bảng xếp hạng riêng giữ50. Dùng rank API; không có rank thì `—` và tên/XP thật, đúng phương án đã được duyệt. Không ghi dữ liệu học để tạo ảnh.
- Build cuối PASS25s (24 executed/54 up-to-date), unit84/84 executed, lint0 lỗi/425 warnings. Instrumentation cuối4/4 PASS83.474s (Home+Topics), font1.3 Home1/1 PASS30.961s. Resource measurement14 tổ hợp có320/360dp và landscape; không chạy AVD nhỏ riêng. APK-r giữ dữ liệu/session; font trả1.0, size/density/config AVD giữ nguyên.
- Giữ log FAIL do mất focus/Launcher ANR/app khác và ADB ngắt khi Emulator tắt; sau khi người dùng tạm ngừng thao tác và khởi động lại AVD, các lượt cuối PASS. Không bỏ assertion hoặc sửa nghiệp vụ để né lỗi. Đã xem ảnh thật font1.0/1.3 và tạo comparison; chưa tuyên bố UI giống100% hay được người dùng nghiệm thu.
- Source14 sửa/14 resource mới,395/409 file gốc giữ hash; Backend/Admin/Auth/Topics/Flashcard không đổi. Git hiện có: `feature/initial-import`, base7dabeae; không stage/commit/push, giữ các Skill untracked. [Báo cáo](docs/reports/HOME_APPROVED_PHASE_A_REPORT.md); evidence local `android-app/runtime-evidence/home-approved-20261029/`. Chờ duyệt Home; Phase B Login chưa triển khai.

## Gói full source gửi AI khác — 2026-10-28

- Theo yêu cầu bàn giao: tạo `docs/exports/BiVocal_FULL_PROJECT_2026-10-28.zip` (75.750.597 byte, 664 entries), gồm source/test/resources/config mẫu của Android, Backend, Admin, dataset/quality/license, tài liệu và design. Có START_HERE.md, PROMPT_FOR_GEMINI.txt, MANIFEST.json và danh sách loại trừ; giữ Java/XML/MVVM và Online-Only, ghi đúng UI M16 mới nhất. Snapshot tài liệu trong ZIP được lấy trước bản ghi đóng gói này.
- Loại .env/secrets, local.properties/keystore, live DB/backup, tài khoản và dữ liệu học, screenshots/logs runtime, node_modules/build/cache và archive cũ. 20 file có biến đổi ở bản xuất (ẩn thông tin cục bộ/bổ sung tên cấu hình mẫu); không sửa source ứng dụng gốc. Dataset giữ nguyên file hiện có, không chạy pipeline/export DB hay import.
- PASS: ZIP CRC, tên entry duy nhất và SHA-256 cho toàn bộ file theo manifest; SHA-256 ZIP có sidecar. Đây là full source/dataset, KHÔNG phải backup PostgreSQL production 20.000 thẻ; tài liệu giải thích seed gốc chỉ300, một số script lịch sử cần artifact riêng đã loại. Build/unit/runtime NOT_RUN vì chỉ đóng gói; không đổi trạng thái M16.5. Không có Git repository; CHƯA COMMIT/CHƯA PUSH.

## M16.5 — Thẻ dài hơn, ví dụ không nền/viền — 2026-10-28

- Chỉ sửa activity_flashcard.xml: margin dưới card36→12dp; chiều cao responsive tự tăng24dp, giữ nguyên vị trí nút80% và thứ tự đánh giá dưới. Ví dụ bỏ background drawable nên không còn nền/viền; giữ padding/chữ/audio. Source khác giữ nguyên theo hash, không sửa nghiệp vụ hoặc ghi học/DB.
- Build/lint/test APK PASS18s,12 executed/62 up-to-date; lint0 lỗi392warnings. Unit NOT_RUN cho lượt XML-only (84 PASS thuộc lượt trước). Instrumentation3/3 PASS31.220s, gồm đồng bộ hai nút/ví dụ/24 tổ hợp size-font; APK-r giữ dữ liệu. Không đổi AVD/font, không Review/Quiz/Favorite. [Ảnh mới](android-app/runtime-evidence/m16-5-flashcard/example-transparent-20261028/actual/flashcard-back-review.png); chờ duyệt UI, CHƯA COMMIT/CHƯA PUSH.

## M16.5 — Nút lật trên, khung ghi nhớ dưới — 2026-10-28

- Đổi thứ tự: Flashcard → Lật thẻ/Xem mặt trước → khung đánh giá. Hai mặt cùng chiều cao card và cùng tọa độ/kích thước nút (80% vùng khả dụng); Browse dùng INVISIBLE cho Trước/Tiếp ở mặt sau để giữ chỗ, NEW/REVIEW vẫn GONE. Giữ nội dung/card borderless example và SRS. Quy tắc back+24dp trước đó được thay bằng chiều cao chung theo yêu cầu đồng bộ mới.
- Chỉ3 file UI/test: activity_flashcard.xml, FlashcardActivity.java, FlashcardFrontendUiTest.java; hash source khác không đổi. Build12s PASS,84 unit, lint0 lỗi391warnings; instrumentation3/3 PASS30.227s, có assertion cùng X/Y/width/height và rating nằm dưới. APK-r giữ dữ liệu, không gửi Review/Quiz/Favorite, không đổi DB/AVD/font. Runtime font1.3 riêng không chạy lại; XML24 cấu hình có font1.3 PASS. [Ảnh mới](android-app/runtime-evidence/m16-5-flashcard/actions-order-20261028/actual/flashcard-back-review.png); chờ duyệt, CHƯA COMMIT/CHƯA PUSH.

## M16.5 — Bỏ viền ví dụ, nới cả hai mặt thẻ — 2026-10-28

- Bỏ stroke fc16_example, giữ nền/bo góc. Mặt trước đưa tâm Lật thẻ từ75% xuống80% vùng khả dụng nên card dài hơn; mặt sau thêm24dp, có thể nới theo đáy ví dụ tới tối đa96dp bổ sung, nội dung đặc biệt dài vẫn cuộn. Giữ header/metadata phải/margin48–36 và toàn bộ callback SRS.
- Chỉ3 file source sửa: FlashcardActivity.java, fc16_example.xml, FlashcardFrontendUiTest.java; source VM/API/Repository/Backend/Home/Topics/Auth giữ nguyên qua hash. Build cuối13s PASS (19 executed/59 up-to-date),84 unit PASS, lint0 lỗi391warnings; instrumentation cuối3/3 PASS30.612s, có kiểm tra ví dụ không bị cắt và nút quay mặt trước hiển thị trọn. APK-r giữ dữ liệu; không gửi rating/Quiz/Favorite. Font1.0, không đổi AVD. Runtime font hệ thống1.3 chưa chạy lại riêng; đo XML gồm font1.3 PASS.
- Đã xem ảnh hai mặt mới tại runtime-evidence/m16-5-flashcard/example-borderless-20261028/actual-final/; bản thử tăng48dp trước đó khiến nút sát/cắt đáy qua quan sát ảnh nên đã giảm phần nới tối thiểu xuống24dp và thêm assertion. [Ảnh mặt trước](android-app/runtime-evidence/m16-5-flashcard/example-borderless-20261028/actual-final/flashcard-front.png), [mặt sau](android-app/runtime-evidence/m16-5-flashcard/example-borderless-20261028/actual-final/flashcard-back-review.png). Chờ duyệt UI, CHƯA COMMIT/CHƯA PUSH, không tự DONE.

## M16.5 — Header, thẻ gọn, khoảng cách và metadata phải — 2026-10-28

- Theo phản hồi liên tiếp: title22sp/subtitle14sp/mode11sp căn giữa đối xứng; progress14sp/bar10dp. Thẻ trước tính chiều cao theo vùng khả dụng để tâm Lật thẻ khoảng75%, min280dp và vẫn cuộn; mặt sau dùng phần còn lại để đọc ví dụ. Margin trên48dp/dưới36dp (gấp3 mức16/12 cũ). Khung đánh giá nền111726/viền334155. Nghĩa trái, Loại từ/Thông tin phải (3:2); thiếu metadata ẩn cột phải.
- Chỉ4 file source UI/test sửa: FlashcardActivity, activity_flashcard.xml, fc16_review.xml, FlashcardFrontendUiTest. ViewModel/API/Repository/Backend/Admin/Auth/Home/Topics giữ nguyên theo SHA-256; không gửi rating/Quiz/Favorite, không sửa DB. APK cài-r giữ dữ liệu.
- Bản cuối metadata-right: build/unit/lint/test APK PASS17s (24 task thực thi/54 up-to-date),84/84 unit, lint0 lỗi/391 warnings; instrumentation3/3 PASS30.448s, có24 tổ hợp đo size/font và xác minh tâm nút75%. Font1.3 runtime trước bước cột phải PASS1/1,55.781s; cột phải bản cuối có đo font1.3 nhưng chưa chạy lại riêng runtime font hệ thống1.3. Font trả1.0, không đổi size/density AVD. FAIL phép đo center ban đầu do getLocationInWindow trên XML chưa attach, đã sửa test cộng tọa độ ancestor; log giữ nguyên.
- [Ảnh mới nhất](docs/exports/M16_5_UI_POLISH_2026-10-28.zip), [báo cáo cập nhật](docs/reports/M16_5_FLASHCARD_FRONTEND_REPORT.md); evidence polish-20261028/. Chờ duyệt UI, không tự nâng DONE. CHƯA COMMIT/CHƯA PUSH (không có repository).

## M16.5 — Ảnh gửi báo cáo — 2026-10-28

- Theo yêu cầu chụp ảnh mới: capture Emulator bằng FlashcardFrontendUiTest, 3/3 PASS29.394s; lấy6 PNG gốc (front/back/new/empty due/landscape/landscape actions), kèm chú thích và SHA-256. [ZIP ảnh](docs/exports/M16_5_SCREENSHOTS_2026-10-28.zip). Không build lại hoặc sửa source; không gửi rating/Quiz/Favorite, không thay cấu hình AVD/clear data. Không có ảnh completion từ phiên học thật; M16.5 vẫn chờ duyệt, không tự nâng DONE. Không commit/push.

## M16.5 — Flashcard Frontend — 2026-10-27 — READY_FOR_ACCEPTANCE

- Người dùng duyệt triển khai sau bước review ZIP. Tái sử dụng FlashcardActivity/ViewModel: hai mặt, word/example TTS, metadata thật, review hai mức, progress theo acknowledgement và trạng thái hoàn thành toàn queue. Giữ NEW_TODAY/REVIEW_TODAY, phân trang100, Favorite/Quiz qua menu, settings Việt trước/cỡ chữ/auto speech/reduce motion; Drawer dùng opt-in header riêng. Không sửa Backend/Admin/Auth/Home/Topics/API/Repository/DB hoặc đưa Room vào runtime.
- Source:6 file sửa/10 mới, SHA-256 trước/sau trong evidence; source ngoài phạm vi giữ nguyên. Build app/unit/lint/test APK PASS25s, unit84/84, lint0 lỗi/393 warnings. Guard giữ APK PASS; app/test cài-r, không clear/uninstall.
- Runtime:6 test case khác nhau có PASS trên app source cuối (5/6 lượt regression, test gesture lỗi được sửa và chạy lại1/1); thêm Food runtime font1.3 PASS1/1. Có ảnh front/back/new/empty due/landscape/action; thử cuộn thật tới nút. Log FAIL giữ nguyên: thiếu session ban đầu và helper scrollTo/tọa độ gesture ở system inset. Chỉ sửa test tọa độ, không sửa AVD để né kiểm tra. Font trả1.0, size/density/config AVD giữ nguyên.
- Không gửi Review/Quiz/Favorite trên production để dựng ảnh; completion E2E có ghi và due queue có thẻ NOT_RUN (queue thật đang rỗng). Unit xác minh completion chỉ sau ack; đo XML24 cấu hình có màn320dp/font1.3. Chưa đánh giá trực tiếp chất lượng giọng đọc và chưa snapshot DB mới. Chờ người dùng duyệt UI, không M16.6. [Báo cáo, lệnh, file và ảnh](docs/reports/M16_5_FLASHCARD_FRONTEND_REPORT.md). Git không có repository: CHƯA COMMIT/CHƯA PUSH.

## M16.4 phản hồi — tiêu đề lớn và khôi phục viền danh sách — 2026-10-27

- Theo yêu cầu mới: CHỦ ĐỀ/LOẠI TỪ in hoa, 16sp; hai danh sách có lại viền1dp #334155. Dải chip vẫn không viền, All cố định và viewport4 hàng giữ nguyên. Chỉ sửa topics_frontend.xml/activity_topics.xml.
- PASS: assembleDebug/lintDebug/assembleDebugAndroidTest (5s), APK install-r giữ dữ liệu; resource instrumentation1/1 (6 tổ hợp size/font, có scale1.3). Unit và full runtime không chạy lại cho lượt đổi text/style này; kết quả83unit và2/2UI trước thuộc lượt bốn hàng. Log build-large-headings.txt/instrumentation-large-headings.txt trong evidence borderless-four-rows/. Chờ duyệt UI, CHƯA COMMIT/CHƯA PUSH.

## M16.4 phản hồi — bỏ viền/bốn hàng — 2026-10-27

- Bỏ viền ba container (chip/semantic/POS), giữ card/chip hiện có; hai viewport giảm tối đa 5 → 4 hàng, nhãn Chủ đề/Loại từ. Sáu file UI/test đổi, source khác giữ nguyên theo SHA-256. Không đổi API/DB/auth/Home/AVD.
- PASS mới: build/unit/lint/test APK (11s), unit83/83, lint0 lỗi/393 warnings; Topics instrumentation2/2 (48.720s), xác minh bốn hàng và cuộn. APK -r giữ dữ liệu. Không chạy lại runtime font hệ thống1.3 riêng trong lượt này; resource measurement vẫn có scale1.3. [Báo cáo/ảnh mới](docs/reports/M16_4_TOPICS_FRONTEND_REPORT.md), evidence borderless-four-rows/. Chờ duyệt UI, CHƯA COMMIT/CHƯA PUSH.

## M16.4 phản hồi — Kho từ vựng và hai khung năm hàng — 2026-10-27

- Người dùng xác nhận hai khung riêng, mỗi khung tối đa năm hàng/cuộn nội bộ, trang ngoài cuộn tới Loại từ. Đã đổi title Kho từ vựng, bỏ shortcut tra từ, tạo khung chip trong suốt và giữ Tất cả đứng yên ngoài dải chip cuộn ngang. Giữ avatar/bốn tab/API/count thật.
- Sửa 5 file Topics UI/test, thêm 1 drawable; source khác giữ nguyên theo SHA-256. Nội dung/catalog vẫn 36 nhóm; không sửa Backend/DB/auth hoặc gửi Review/Quiz/Favorite. [Báo cáo/ảnh mới](docs/reports/M16_4_TOPICS_FRONTEND_REPORT.md), evidence m16-4-topics/framed-lists/.
- PASS cuối: build/unit/lint/test APK, 83/83 unit, lint 0 lỗi/393 warnings; Topics instrumentation 2/2 mặc định + 1/1 font1.3, gồm swipe chip cố định/inner và outer scroll/5-row viewport/landscape/Food/Drawer. Font trả1.0, AVD giữ nguyên, APK install-r giữ dữ liệu.
- Giữ bằng chứng FAIL đã xử lý: padding style minSdk, luồng mở Food bị chuyển Login trước khi người dùng đăng nhập lại, viewport font1.3 chưa cập nhật (sửa post sau layout). Không sửa authentication để né lỗi. READY_FOR_ACCEPTANCE/chờ duyệt, CHƯA COMMIT/CHƯA PUSH; không M16.5.

## M16.4 Topics Frontend — 2026-10-27 — READY_FOR_ACCEPTANCE

- Triển khai theo topics_demo.html/yêu cầu M16.4: header số nhóm thật/avatar phải, search local, sáu chip, card native/vector, semantic trước POS, bốn tab cố định. Không dùng 10 topic/30 từ hoặc Mastery demo. API trước/sau trả cùng 36 nhóm (29 semantic/7 POS), thuộc tính và count không đổi.
- Reuse TopicsViewModel/Repository/model/API và Flashcard pagination; trích component tab/avatar từ Home, thêm LearningBottomNavigation dùng chung; controller chỉ hỗ trợ header Topics riêng. Home giữ bố cục và hành vi cũ, không sửa card/nghiệp vụ. Source SHA-256: 11 file sửa/33 mới, đa số icon/resource; Backend/Admin/auth/VM/Repository/paging giữ nguyên.
- PASS mới trên source cuối: assembleDebug/testDebugUnitTest/lintDebug/assembleDebugAndroidTest, unit 83/83, lint 0 lỗi/391 warnings. Instrumentation 4/4 (Topics + hồi quy Home) và 1/1 Topics runtime font 1.3; Topics đo 6 cấu hình size/font, có landscape thật. Food mở đúng code và tải <=100 thẻ/trang; search/empty/POS/recreate/Drawer/shortcut/Home↔Topics PASS.
- APK install -r giữ session/data, guard giữ APK đã xác minh. AVD config/size/density không đổi, font trả 1.0. Không submit Review/Quiz/Favorite, không ghi/migration DB. Chưa snapshot checksum user tables; không ép lỗi mạng runtime. HTML file:// bị browser policy chặn, đã so thông số source với ảnh Android; chưa có ảnh demo riêng để đối chiếu pixel.
- [Báo cáo và ảnh cuối](docs/reports/M16_4_TOPICS_FRONTEND_REPORT.md), evidence android-app/runtime-evidence/m16-4-topics/. Chờ người dùng duyệt ảnh, không đánh dấu milestone PASS và không M16.5. Git không có repository, CHƯA COMMIT/CHƯA PUSH. M16.3 được người dùng lấy làm baseline DONE trong yêu cầu chuyển sang M16.4; không sửa ngược các mục nghiệm thu lịch sử.

## M16.3 Home Final UI Polish — 2026-10-27

- Người dùng duyệt hướng bố cục mới, cho phép polish header/SRS/nhãn leaderboard. Đã tăng slogan 12sp/max 2 dòng, header thích ứng dưới 440dp; SRS nền #111726/viền 1dp #334155/radius 20dp, title 16sp, Info touch 48dp, CTA động “Ôn N từ đến hạn”; đổi tiêu đề Home leaderboard thành XP. Giữ Daily Goal/Drawer phải/bốn tab/thứ tự/logic SRS. Không sửa Backend/DB/auth hoặc M16.4.
- PASS mới: build/unit/lint/test APK; unit 81/81, lint 0 errors/398 warnings. Instrumentation APK cuối 2/2 ở font mặc định (33.408s), runtime font 1.3 thêm 1/1 (34.170s); 12 cấu hình đo layout gồm màn 320dp. APK install -r, không clear/uninstall; guard keepInstalledApks=true/uninstallIncompatibleApks=false đã xác minh. Font khôi phục 1.0, hash AVD config và size/density giữ nguyên.
- Đã xem ảnh top/SRS/leaderboard/Drawer mặc định và font 1.3; API thật, không submit Review/Quiz. SHA-256 source chỉ ra 11 file Home UI/test thay đổi, source Backend/Admin/auth/HomePresentation/Drawer controller giữ nguyên. Không lấy snapshot DB mới. Màn nhỏ là instrumentation measurement, chưa chạy AVD nhỏ riêng; due=0 không ép trên tài khoản thật.
- [Báo cáo/file/lệnh/ảnh cuối](docs/reports/M16_3_HOME_FINAL_POLISH_REPORT.md). READY_FOR_ACCEPTANCE/chờ duyệt polish, CHƯA COMMIT/CHƯA PUSH (không có repository). Lỗi script chẩn đoán guard/đường dẫn đã sửa và ghi rõ trong báo cáo, không che thành kết quả test ứng dụng.

## Bàn giao Home M16.3 cho ChatGPT — 2026-10-27

- Tạo [báo cáo tổng hợp mới nhất](docs/reports/M16_3_HOME_REPORT_FOR_CHATGPT.md), thêm mục lục README. Ghi rõ header thương hiệu/avatar phải, SRS bốn nhóm, bốn tab, bỏ Quick Stats và footer; phân biệt bằng chứng kiểm thử lịch sử với các lượt sửa gần nhất.
- Theo yêu cầu: không chụp ảnh mới, không chạy lại build/test hoặc sửa source/DB; báo cáo có danh sách 3–4 ảnh người dùng tự chụp và lời dẫn gửi ChatGPT. M16.3 vẫn READY_FOR_ACCEPTANCE/chờ duyệt UI, không tự nâng DONE. Git không có repository, CHƯA COMMIT/CHƯA PUSH.
- Kiểm tra tài liệu PASS: UTF-8 hợp lệ và 48 liên kết nội bộ tồn tại trong ba file; SHA-256 activity_home.xml/HomeActivity.java khớp bản sau lượt bỏ footer. Build/unit/lint/runtime NOT_RUN trong lượt chỉ viết báo cáo.

## Bỏ card động viên cuối Home — 2026-10-27

- Theo ảnh phản hồi mới, bỏ riêng card mascot/slogan cuối nội dung Home; thương hiệu/slogan ở header vẫn giữ. Xóa binding mascot footer trong HomeActivity; HomeResponsiveUiTest kiểm tra tới card leaderboard cuối cùng thay footer cũ. Không đổi Drawer/avatar phải, bốn tab hoặc dữ liệu.
- PASS: :app:assembleDebug :app:assembleDebugAndroidTest (7 giây, task compile/package chạy thật), APK chính install -r thành công. Unit/lint/instrumentation: NOT_RUN cho thay đổi xóa UI nhỏ này; test APK chỉ biên dịch, không cài. Git chưa có repository; đối chiếu nội dung/SHA-256 trước–sau, CHƯA COMMIT/CHƯA PUSH. M16.3 vẫn chờ duyệt UI.

## Home header mascot/slogan + avatar Drawer — 2026-10-27

- Theo yêu cầu trực tiếp kèm hai ảnh: bỏ avatar/lời chào bên trái, thay bằng mascot chính thức và slogan “Mỗi ngày 15 phút, bứt phá vốn từ cùng BiVocal!”. Giữ streak; bên phải thay hamburger bằng avatar. Avatar mở Drawer cũ, không mở Profile trực tiếp; Profile/Cài đặt vẫn trong Drawer. Footer và bốn tab giữ nguyên.
- Sửa activity_home.xml, binding/render UI trong HomeActivity và hai Home UI test để bỏ tham chiếu lời chào/avatar trái. Reuse bitmap crop gốc và các ID imageHomeAvatar/textHomeAvatar, NavigationDrawerController không đổi SHA-256. Không sửa Backend/DB/auth.
- PASS: :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest (1m35s, compile/package/lint chạy thật); lint 0 errors/397 warnings. APK chính install -r PASS. Runtime PASS: logo/slogan ở trái, avatar phải, bấm avatar mở panel Drawer END với Hồ sơ/Cài đặt, Back đóng về Home. Evidence local: android-app/runtime-evidence/m16-3-home/header-brand-avatar/.
- Unit/instrumentation thực thi: NOT_RUN cho chỉnh UI cục bộ này; chỉ biên dịch test APK, không cài lại E2E/test theo phạm vi dọn app trước đó. Git không có repository; đã đối chiếu source/SHA-256 trước–sau, CHƯA COMMIT/CHƯA PUSH. Việc chỉnh header TECHNICAL_COMPLETE; M16.3 vẫn chờ người dùng duyệt UI.

## Home bốn tab — 2026-10-27

- Runtime PASS: UI dump trên Emulator xác nhận đúng 4 mục; chiều rộng lần lượt 251/251/252/252px (sai khác 1px do làm tròn). Home đang mở; chỉ còn package chính com.example.bivoca được cài.

- Theo yêu cầu trực tiếp: bỏ Hồ sơ ở bottom navigation; giữ Trang chủ / Chủ đề / Ôn tập / Trắc nghiệm chia đều bằng layout_weight. Hồ sơ vẫn qua avatar và Drawer. Không đổi API, dữ liệu hoặc luồng học.
- Sửa activity_home.xml, bỏ binding homeTabProfile trong HomeActivity và lượt kiểm tra tab này trong HomeRuntimeUiTest. Đối chiếu nội dung/SHA-256 trước–sau; Git báo không có repository, chưa xác định nhánh/diff/lịch sử. CHƯA COMMIT/CHƯA PUSH.
- PASS: :app:assembleDebug và :app:assembleDebugAndroidTest (16 giây, task compile/package chạy thật, configuration cache tái sử dụng). APK chính install -r thành công; không cài lại gói E2E/test đã gỡ. Unit/lint/instrumentation: NOT_RUN cho chỉnh sửa UI nhỏ này; các số test trước là lịch sử. M16.3 vẫn chờ duyệt UI.

## Tích hợp hướng dẫn Codex cá nhân hóa — 2026-10-26

- **Phê duyệt:** người dùng trả lời “duyệt” cho kế hoạch ghép `_codex_import` sau phân tích chỉ đọc. Phạm vi chỉ là tài liệu/cấu hình trợ lý; CHƯA COMMIT/CHƯA PUSH, không sửa code ứng dụng hoặc database.
- **Trạng thái:** `TECHNICAL_COMPLETE` cho việc ghép file và kiểm tra tĩnh. Không thay đổi nghiệm thu milestone; M16.1–M16.3 vẫn chờ duyệt UI. Tự nạp hướng dẫn/skill trong phiên Codex mới: `NOT_RUN`.
- **File:** ghép `AGENTS.md`; tạo `context.md`, `memory.md`, `.agents/skills/bivoca-quality-handoff/SKILL.md`, `agents/openai.yaml` trong thư mục skill và `.gitignore` gốc; thêm liên kết tại `README.md`. Bản nhập gốc giữ nguyên, không chép template đè lịch sử.
- **Nội dung:** giữ nguyên 8 nguyên tắc bắt buộc; bổ sung quyền tự chủ/Git/nghiệm thu và guard giữ APK; đính chính Gradle 9.5.0 so với AGP 9.3.3, cách Repository dùng Retrofit và phạm vi unit/HTTP so với DB integration. Bỏ hồ sơ học tập và đường dẫn máy cá nhân khỏi cấu hình chia sẻ.
- **Kiểm tra PASS:** Python kiểm tra UTF-8, code fence, 67 liên kết nội bộ trong 7 file cấu hình/tài liệu, cấu trúc YAML đơn giản của frontmatter/UI metadata và mẫu secret/thông tin cá nhân. Đây là kiểm tra cấu trúc tĩnh, không phải xác nhận cơ chế nạp skill của Codex.
- **Bảo vệ ignore PASS:** `rg --files --hidden --no-require-git` xác minh 7 đường dẫn nhập/xác thực/cấu hình máy/evidence bị ẩn và 8 đường dẫn hướng dẫn/skill/mẫu môi trường vẫn hiển thị. `.gitignore` không gỡ dữ liệu đã bị Git theo dõi hoặc bí mật trong lịch sử; ảnh/archives chưa được kiểm chứng đầy đủ.
- **Đối chiếu:** snapshot SHA-256 của 493 file hiện hữu; chỉ thay `AGENTS.md`, `README.md` và bổ sung mục bàn giao này. Source/config ứng dụng, báo cáo và `_codex_import` giữ nguyên; 8 nguyên tắc gốc và toàn bộ nội dung `WORK_STATUS.md` trước phiên được bảo toàn.
- **Giới hạn:** đã chạy `quick_validate.py` nhưng `BLOCKED` do thiếu PyYAML ở cả Python mặc định và runtime đi kèm; không tự cài dependency. Build/test Android/Backend/Emulator: `NOT_RUN`, vì không đổi ứng dụng.
- **Git:** gốc và các module chưa được Git nhận diện là repository; chưa xác định nhánh/remote/tracked files, không `git init`. Không có quyền commit/push từ việc duyệt tích hợp này.
- **Bàn giao tiếp:** mở phiên Codex mới tại thư mục chứa `AGENTS.md`, yêu cầu chỉ đọc và xác nhận đã đọc context/memory, tìm thấy `bivoca-quality-handoff`, hiểu giới hạn Git và giữ trạng thái M16. Xác định checkout Git thực trước mọi commit/push; có thể chạy lại validator đầy đủ khi PyYAML sẵn có.

## M16.3 Home Frontend — 2026-10-25 (KỸ THUẬT PASS, CHỜ DUYỆT UI)

- 26/10/2026: card SRS đổi theo ảnh mới thành tổng từ/% thuần thục, thanh 4 màu, lưới 2×2 và CTA ôn đến hạn. Mapping hiển thị: Biết mới 0–1, Đang ôn 2–3, Nhớ tốt 4, Thuần thục 5; dữ liệu schedule thật, không đổi thuật toán Backend. Build/lint PASS, 81/81 unit + 1/1 responsive instrumentation (8 cấu hình) PASS; APK cài -r. Tài khoản hiện hành 0 từ/0 đến hạn; nút ôn vô hiệu hóa đúng, help dialog PASS. Evidence: runtime-evidence/m16-3-home/srs-four-groups/.

- 26/10/2026: theo yêu cầu mới, bỏ hàng Quick Stats (Tổng XP / Ngày liên tục / Từ đã học) khỏi Home và binding liên quan; giữ badge streak trên header, SRS, Profile và dữ liệu Backend. Leaderboard vẫn preview tối đa 3 người, màn đầy đủ tải tối đa 50, chưa phân trang.

- Visual refinement: đã so sánh demo/Android trước/sau bằng ảnh thật, chỉnh menu/avatar, tỷ lệ font stats/SRS, tile icon, màu viền, badge accuracy, icon CTA/tab và mascot footer. Ảnh home-before-after-comparison.png trong runtime-evidence/m16-3-home/refinement/. Chưa khẳng định giống 100%; Roboto/emoji/viewport khác, leaderboard thực chỉ có 2 người.
- Refinement build/unit/lint PASS (81/81 unit, 0 lint errors/380 warnings), 2/2 UI tests PASS; giữ AVD size/density và toàn bộ nghiệp vụ. Chi tiết sai lệch còn lại trong đầu báo cáo M16.3.

- Home native theo home_demo.html và hai ảnh mới: avatar trái mở Profile, menu phải dùng Drawer hiện có, Daily Goal → stats → SRS → leaderboard → mascot; 5 tab cố định.
- Dùng API thật qua các ViewModel hiện có. SRS mức 5 / 2–4 / 0–1, tổng là reviewedCards; không cộng favorite-only vào từ đã học. Leaderboard hiện chỉ có tổng XP, ghi rõ chưa hỗ trợ theo tuần/giải đấu.
- Build/unit/lint PASS; 81/81 unit, 2/2 instrumentation PASS. Responsive 8 tổ hợp size/font; runtime font 1.3 và landscape có ảnh. Tám hành động điều hướng mở Activity thật, không gửi Review/Quiz.
- Snapshot READ ONLY trước/sau runtime: 23 bảng checksum không đổi, giữ 20.000 Flashcard/38 topics. Không sửa Backend/DB/auth M16.1–2. APK cài -r giữ dữ liệu.
- Báo cáo: [M16.3](docs/reports/M16_3_HOME_FRONTEND_REPORT.md). Chưa triển khai M16.4; các giới hạn weekly API/error-state/native 2400 ghi rõ trong báo cáo.

## M16.2 Register & Forgot UI — 2026-10-25 (KỸ THUẬT PASS, CHỜ DUYỆT UI)

- Theo phản hồi mới: bỏ gạch chân slogan dùng chung ở Login/Register/Forgot; giữ màu và vị trí hiện tại.

- Bỏ nút Back phía trên logo ở Register/Forgot theo yêu cầu; giữ liên kết quay lại bên dưới. Build Debug + test APK PASS, 5/5 UI instrumentation tests PASS. Emulator điều chỉnh mật độ 420 → 370 dpi (native vẫn 1080x1920) để tăng vùng nội dung; khởi động lại sau lỗi System UI khi đổi mật độ.
- Sau reboot, kiểm thử runtime bổ sung `normalFontTallViewportShowsAuthWithoutScrolling` PASS 1/1 (không skip): cả Register và Forgot có chiều cao nội dung <= viewport, scrollY=0 ở font mặc định. Khi bàn phím mở/font lớn vẫn giữ khả năng cuộn.

- Chỉnh nhãn theo phản hồi: “TẠO TÀI KHOẢN”; Forgot header “QUÊN MẬT KHẨU?”, heading dưới khóa “Khôi phục mật khẩu”. Build PASS, APK đã cài -r và UI dump xác minh.

- Register/Forgot đồng bộ Minimalist Dark Mode M16.1; tái sử dụng input/gradient/theme, Back riêng, header/slogan responsive, Forgot có khóa phẳng và Google callout.
- Build, 76/76 unit tests, lint 0 errors/322 warnings PASS. Bộ 5 instrumentation UI test PASS, có IME thực, recreation form và đo layout 320/411/731dp với font scale 1.3.
- APK cài -r giữ dữ liệu; ảnh thật trong android-app/runtime-evidence/m16-2-auth/. Native AVD giữ 1080x1920/density 420.
- 11 file Login/auth được đối chiếu SHA-256 không đổi. Không sửa Backend/DB/API hoặc tạo user/gửi reset thật.
- Báo cáo và giới hạn runtime: [M16.2](docs/reports/M16_2_REGISTER_FORGOT_UI_REPORT.md). Không triển khai M16.3.

## M16.1 Login UI — 2026-10-25 (CHỜ DUYỆT GIAO DIỆN)

- Slogan được gạch chân, đổi cyan nhạt #7DD3FC, căn giữa trong riêng cột mascot + BiVocal, tránh tràn sang divider/heading. assembleDebug PASS, đã cài -r và xác minh bounds trên Emulator.

- Điều chỉnh theo người dùng: “Quên mật khẩu?” nằm dưới ô mật khẩu, căn phải, margin 6dp. assembleDebug PASS; cài -r và UI dump xác nhận vị trí trên Emulator.

- APK Header + Labels đã được cài cập nhật trên Emulator sau phản hồi người dùng. UI dump xác nhận slogan và label Tài khoản hiển thị; không clear data.

- Cập nhật Header + Labels: mascot 48dp, brand 26sp, heading 20sp, slogan 13sp và label Tài khoản 14sp; spacing form thu gọn. Build/unit/lint PASS. Theo yêu cầu chỉ build, không chụp ảnh/cài APK mới; runtime evidence cũ thuộc bản spacing trước.

- UI Login theo ảnh mẫu và spacing fix đã build/cài giữ dữ liệu; footer cách Google 32dp, toàn khối căn giữa và cuộn khi thiếu chiều cao.
- Build/lint PASS, unit result 76/76; đã chụp Emulator dọc, landscape cuộn và font scale 1,3. Docked IME cuối chưa nghiệm thu ổn định.
- Auth/Backend/DB và các màn khác giữ nguyên. Báo cáo: [M16.1](docs/reports/M16_1_LOGIN_UI_IMPLEMENTATION_REPORT.md).

> Tổ chức tài liệu 2026-10-25: các báo cáo đã chuyển vào `docs/reports/`. Xem [mục lục báo cáo](README.md). Nội dung báo cáo và source không thay đổi.

**Ngày cập nhật:** 2026-10-24  
**Phương pháp xác minh:** M8 theo bằng chứng OPPO trong báo cáo M8; M9 theo source, typecheck/test/build, PostgreSQL thật, HTTP probes và trình duyệt Admin; M10 theo source, build/unit/lint, PostgreSQL riêng và thao tác thật trên OPPO; M11 theo ảnh mẫu, build/unit/lint, OPPO và AVD Pixel 6 API 35; M13.8 theo audit production chỉ đọc, DB cô lập, build/test/instrumentation và E2E AVD API 35; M14 theo backup/restore, migration Up/Down/Up, DB integration, build/test và AVD Credential Manager thật. Kết quả từng milestone được ghi riêng bên dưới.

---

## 1. DONE (ĐÃ HOÀN THÀNH VÀ XÁC MINH CÓ BẰNG CHỨNG)

### Backend
- [x] **M15 Daily Learning Goals & Progress — DONE trên production:** backup 5.816.580 byte
  restore-check PASS; production áp dụng duy nhất migration M15, tạo đúng 12 learned rows
  `LEGACY` với ngày NULL. Counts và checksum 8 bảng bảo vệ khớp trước/sau migration: 5.000
  Flashcard, 19 topic, 5 user, 3 Google identity, 14 progress, 12 Quiz, 2 score, 5 XP.
  Backend typecheck/build và 338/338 tests PASS; DB integration 1/1 chỉ chạy trên database cô lập.
  Backend cổng 3000 health PASS. APK Debug cài `adb install -r`; Google Login/JWT, Home 0/10,
  Progress 7 ngày, Settings goal 10 và Profile/30 XP đều PASS bằng API production read-only.
  Android build, 75/75 unit và lint PASS; 3/3 instrumentation thuộc nghiệm thu cô lập.
  Không chạy Review/Quiz hoặc đổi goal trên production. Xem `M15_DAILY_GOALS_PROGRESS_REPORT.md`.
- [x] **M13.8 Quiz Quality & Online-Only — DONE:** Quiz loại prompt English đa nghĩa và
  đáp án Việt có thành phần nghĩa trùng, ưu tiên nhiễu cùng POS/CEFR, không lặp câu và
  báo đúng số câu an toàn. Production audit: 926 eligible nguồn, 851 câu an toàn, 25 lượt
  lấy mẫu/chủ đề, 0 vi phạm trùng ID/prompt/option/meaning. Android đã bỏ Room khỏi source
  set/runtime và dùng Retrofit trực tiếp; lỗi mạng + Retry nghiệm thu thật trên AVD. Backend
  324/324 + DB integration 1/1; Android 73/73, lint 0 lỗi, instrumentation 3/3. Production
  giữ nguyên 5.000 cards, 19 topics, 3 users, 14 progress, 12 Quiz, 2 scores, 5 XP.
  Xem `M13_8_QUIZ_QUALITY_ONLINE_ONLY_REPORT.md`.
- [x] **M13.7 Kho 5.000 Flashcard — DONE trên production:** backup + restore-check PASS;
  staging và production cùng đạt đúng 5.000 Flashcard, 19 chủ đề. Đã thêm 4.642 thẻ,
  giữ nguyên hash 358 thẻ cũ và giữ nguyên 3 users, 10 progress, 8 Quiz, 2 score, 4 XP.
  Quality gate có kiểm tra trường bắt buộc, nguồn, CEFR/POS, định dạng, trùng chuẩn hóa và
  nghĩa gần trùng; 930 trường hợp chưa chắc chắn giữ ở hàng chờ, 18.758 bị loại, exact
  duplicate sau cùng bằng 0. Admin API stats/list HTTP 200; Android online/search/Quiz và
  Room offline trên AVD PASS. Backend 321/321, pipeline 9/9, Admin 43/43, Android build/unit
  và 8/8 instrumentation PASS. Xem `M13_7_5000_FLASHCARDS_REPORT.md`.
- [x] **M13.6 Vocabulary Review & Direct Publishing — DONE trên production:** backup custom
  `flashcard_english` đã restore-check PASS và được giữ tại
  `%LOCALAPPDATA%\BiVoca\backups`; hai migration M13.6 đã apply. Queue production nạp
  idempotent 24.388 ứng viên (23.990 Pending, 58 Approved, 340 Rejected). Admin preview
  58 insert/0 skip/0 block và publish thành công: 10→16 topics, 300→358 Flashcards.
  Hash 300 thẻ gốc không đổi; 3 users, 10 progress, 8 Quiz, 2 score, 4 XP không đổi.
  Admin hiển thị 58 Published; API và Android Room production nhận thẻ mới. Backend
  321/321 + M13.6 DB 4/4, Admin 43/43, Android 96/96/lint từ lượt M13.6 và build/unit
  production PASS. Xem `M13_6_VOCABULARY_REVIEW_AND_PUBLISH_REPORT.md`.
- [x] **M13 CEFR Dataset Integration Phase 1 — DONE trên DB riêng:** audit 7.988 dòng nguồn,
  đối chiếu 300 Flashcard: 293 exact, 0 uncertain, 7 unmatched; 99 nhãn nguồn khác nhãn hiện tại
  được ghi nhận nhưng không ghi đè. Import idempotent hai lần trên
  `bivoca_m13_20261021_d4e222639863`, 300 audit rows, 0 level update, hash Flashcard giữ nguyên.
  Guard từ chối `--apply` vào `flashcard_english`; snapshot DB vận hành không đổi. Backend
  317/317 tests, dataset 10/10 tests, M13 mapping 4/4 tests, typecheck/build PASS. Xem
  `M13_CEFR_DATASET_INTEGRATION_REPORT.md`.
- [x] **M1: Read-only API** (Topics & Flashcards public endpoints).
- [x] **M2: Authentication** (Register, Login, JWT, middleware `authenticate`).
- [x] **M3: Progress API** (Review, Favorite, Summary).
- [x] **M4: Quiz API** (Questions generator, Answer grading & auto progress update, History, Summary).
- [x] **M5: Gamification** (XP calculating, Streak counting, Leaderboard ranking).
- [x] **M6A: Admin API** (Dashboard, tạo/sửa Topics và Flashcards, thay trạng thái card, xem users và đổi role có kiểm soát; không DELETE).
- [x] **M6B: Admin Web UI** (React 19 + Vite dashboard hoàn chỉnh tại `http://localhost:5173`).
- [x] **Kiểm thử Backend:** `npm run typecheck` PASS | `npm test` 311/311 tests PASS.
- [x] **M9 Integration & Security — DONE:** migration M6A đã apply; backend 311/311, Admin 42/42, 68 HTTP checks thật, 5/5 integration chỉ đọc DB gốc, 10/10 integration trên DB riêng PASS. Sửa lỗi Admin mất phiên khi restore gặp network/5xx; không sửa Backend/Android/schema/dataset. Xem `M9_INTEGRATION_SECURITY_REPORT.md`.
- [x] **M10 Smart Spaced Repetition — DONE:** lịch đúng/sai dùng chung Review/Quiz, API due/schedule, Android ôn tập hôm nay và Progress; backend 313/313, Android 60/60, DB riêng 6/6 integration files, OPPO E2E PASS. Sau chấp thuận riêng, đã sao lưu và migration DB vận hành, khởi động lại backend/Admin; kiểm tra API M10 read-only và 5/5 integration trên DB vận hành PASS. Xem `M10_SPACED_REPETITION_REPORT.md`.

### Android App
- [x] **Kiến trúc hiện hành từ M13.8: Online-Only:** Android → Retrofit → Express API →
  PostgreSQL. Room/NetworkFirst/Offline banner/Settings Data không còn trong source set hoặc
  APK. Source M8 được giữ ngoài source set tại `android-app/historical-room/` làm bằng chứng
  lịch sử; không tham gia runtime.
- [x] **M8 Room Offline Cache — DONE:** Nghiệm thu OPPO CPH2557 Android 15: 5/5 Room tests PASS; Online FOOD/ANIMALS → ngắt Backend/restart → cache, lật thẻ/điều hướng/xoay, khóa thao tác online → reconnect/refresh PASS. Chi tiết sự cố runner và cấu hình bảo vệ ở `M8_ROOM_OFFLINE_REPORT.md`.
- [x] **M11 Login UI — DONE trên OPPO thật:** Giao diện trời đêm BiVocal đã đối chiếu `M11_LOGIN_REFERENCE.png`, dùng logo ngang tách từ chính ảnh mẫu; build, 60/60 unit tests và lint 0 lỗi PASS. APK cài bằng `adb install -r`, giữ dữ liệu; OPPO đã kiểm tra form, lỗi nhập liệu, nút mắt giữ nội dung, Register, bàn phím, xoay ngang giữ email/mật khẩu, cuộn và lỗi đăng nhập từ Backend thật. Người dùng tự đăng nhập, thấy Trang chủ rồi chủ động Đăng xuất/quay lại; không cung cấp mật khẩu. Liên kết Forgot đã chuyển sang màn mới trong M11 tiếp theo. Login nay cũng đã được chụp và kiểm điều hướng trên AVD API 35; ảnh logo ngang là cutout từ mockup vì không có asset ngang gốc. Xem `M11_LOGIN_UI_REPORT.md` và `M11_EMULATOR_ACCEPTANCE_REPORT.md`.
- [x] **M11 Login/Register UI — DONE trên Emulator:** AVD Pixel 6 API 35 Google APIs x86_64, 1080×2400. Backend health PASS và `adb reverse tcp:3000 tcp:3000` hoạt động. Login/Register dọc, IME+cuộn, validation, hai nút mắt, xoay ngang giữ state và Register → Login PASS; 73/73 unit tests, lint 0 lỗi/186 cảnh báo. Không tạo tài khoản thử trên DB vận hành. Xem `M11_EMULATOR_ACCEPTANCE_REPORT.md` và `M11_REGISTER_UI_REPORT.md`.
- [x] **M11 Navigation Drawer — DONE trên Emulator:** Một Drawer dùng chung đã tích hợp Home, Topics, Flashcard thường/M10, Quiz, Progress, Leaderboard và Profile; đúng tám mục, trạng thái chọn, scrim/Back/đóng/chọn lại màn hiện tại, xoay ngang có cuộn và Logout clear task đều PASS. Build PASS, **76/76 unit tests**, lint 0 lỗi/191 cảnh báo; Login/Register/Forgot có 0 control Drawer. Ảnh runtime tại `android-app/app/build/reports/m11-navigation/`; xem `M11_NAVIGATION_DRAWER_REPORT.md`.
- [x] **M11 Home UI + Navigation Position Fix — DONE trên Emulator:** Home/Tổng quan được dựng Dark Night Sky bằng XML, dùng API thật cho tên, XP, streak, trackedCards và dueNow; loading/error/retry và sáu luồng chức năng PASS. `NavigationDrawerController` cung cấp Top App Bar/hamburger dùng chung ngoài ScrollView và xử lý Insets một lần. Bounds trên 7 màn portrait giống hệt `[42,138][168,264]` (48×48dp, lệch 0dp); landscape, offline/retry và auth không có Drawer PASS. Build PASS, **76/76 tests**, lint 0 lỗi/178 cảnh báo. Xem `M11_HOME_NAVIGATION_FIX_REPORT.md`.
- [x] **M11 UI Redesign màn 4–10 + Quiz Result E2E — DONE trên Emulator:** hoàn tất Quiz thật trên PostgreSQL riêng, không ghi DB vận hành; hai lượt đủ 10 câu, result 9/10, gửi trùng, mất mạng/retry, xoay, làm lại, Home refresh XP/progress và đối chiếu Quiz/progress/XP/streak đều PASS. Android build debug/E2E, **91/91 tests**, lint 0 lỗi/268 cảnh báo; Backend **313/313 tests** và hai DB integration suites PASS. Snapshot DB vận hành trước/sau trùng hoàn toàn. Xem `M11_QUIZ_RESULT_E2E_REPORT.md`.
- [x] **Xác thực:** `LoginActivity` & `RegisterActivity` kết nối API thật, lưu phiên vào `SessionManager`.
- [x] **Trang chủ:** `HomeActivity` hiển thị username, điều hướng tới Chủ đề, Quiz trắc nghiệm, Hồ sơ cá nhân, Tiến độ học tập và Đăng xuất.
- [x] **Danh sách chủ đề:** `TopicsActivity` tải 10 chủ đề từ API `/topics` với emoji trực quan.
- [x] **Flashcard học từ (Nâng cấp toàn diện):**
  - Mặt trước (Từ vựng, Phiên âm IPA, Loại từ, Cấp độ CEFR). Chạm vào thẻ để lật thẻ (Flip Card).
  - Mặt sau (Nghĩa tiếng Việt, Câu ví dụ tiếng Anh & tiếng Việt).
  - Nút Yêu thích (Favorite - `❤️` / `🤍`): Lưu trực tiếp vào API `PUT /api/v1/progress/:flashcardId/favorite`.
  - Bộ nút ôn tập (Review): "Chưa nhớ" / "Đã nhớ" gửi kết quả tới API `POST /api/v1/progress/:flashcardId/review` và tự động chuyển sang thẻ tiếp theo.
  - Tích hợp phát âm từ TextToSpeech.
  - Nút Trước/Tiếp, nút mở trực tiếp bài Quiz của chủ đề.
- [x] **Quiz trắc nghiệm:** `QuizActivity` & `QuizViewModel` tải câu hỏi 4 đáp án theo chủ đề hoặc ngẫu nhiên, gửi đáp án lên server, hiển thị kết quả đúng/sai kèm highlight, màn hình tổng kết kèm nút chơi lại.
- [x] **Hồ sơ cá nhân & Thành tích (Profile & Gamification):**
  - Model: `GamificationStats.java`, `User.java`.
  - API: `GamificationApi.java` (`GET /gamification/me`), `AuthApi.java` (`GET /auth/me`).
  - Repository & ViewModel: `ProfileRepository.java`, `ProfileViewModel.java`.
  - Giao diện: `activity_profile.xml` (thông tin cá nhân, avatar, badge cấp độ, 3 thẻ XP/Streak/Kỷ lục, nút Đăng xuất).
  - Activity: `ProfileActivity.java`.
- [x] **Màn hình Thống kê tiến độ (Progress):**
  - Model: `ProgressSummary.java`, `ProgressDto.java`, `ReviewRequest.java`, `FavoriteRequest.java`.
  - API: `ProgressApi.java` (`GET /api/v1/progress/summary`).
  - Repository & ViewModel: `ProgressRepository.java`, `ProgressViewModel.java`.
  - Giao diện: `activity_progress.xml` (Tỷ lệ ghi nhớ %, Kho từ: Đã học, Đã ôn tập, Đã thuộc lòng, Yêu thích; Chi tiết ôn tập: Tổng lượt ôn tập, Số lần đã nhớ, Số lần chưa nhớ).
  - Activity: `ProgressActivity.java`.
  - Đã tích hợp lối vào từ `HomeActivity` ("Tiến độ học tập").
- [x] **Bảng xếp hạng Leaderboard:** `LeaderboardActivity` và các lớp liên quan đã có sẵn, được kiểm tra và dùng lại; Home mở màn hình gọi `GET /api/v1/gamification/leaderboard?limit=50` qua client có JWT. Màn hình hiển thị hạng, tên, XP, cấp độ và streak theo API; có loading, danh sách rỗng, lỗi, thử lại và giữ request qua xoay màn hình.
- [x] **Xử lý JWT hết hạn tập trung:** `AuthInterceptor` chỉ xóa phiên khi request gửi Bearer token và nhận HTTP 401, đồng thời chỉ xóa nếu token bị từ chối vẫn trùng phiên đang lưu. Điều hướng về Login chỉ một lần; 403, lỗi mạng và 5xx không xóa phiên.
- [x] **Kiểm thử Android & Cài đặt thiết bị thật:**
  - `.\gradlew.bat assembleDebug` PASS.
  - `.\gradlew.bat testDebugUnitTest` PASS.
  - Đã kết nối điện thoại thật `CPH2557` (OPPO), cấu hình `adb reverse tcp:3000 tcp:3000`.
  - `.\gradlew.bat installDebug` cài đặt thành công (`Installed on 1 device: 'CPH2557 - 15'`).
  - Cập nhật 2026-10-14: `assembleDebug` PASS; `testDebugUnitTest` PASS 20/20; `lintDebug` PASS (0 lỗi, 195 cảnh báo toàn project).
  - OPPO CPH2557: mở Leaderboard từ phiên đăng nhập đã có và nhận danh sách API thật; quay lại Home → Topics → Food → Flashcard thành công.
  - Backend trả HTTP 401 khi gọi leaderboard không token. Empty response, network failure, HTTP 401/403 và chính sách 403/5xx được kiểm tra bằng unit tests; không làm token tài khoản thật hết hạn giả.

---

## 2. IN PROGRESS / SẴN SÀNG PHÁT TRIỂN TIẾP
- **M14 Google Sign-In & Password Recovery — GOOGLE_PRODUCTION_DONE / PASSWORD_RECOVERY_LOCAL_DONE / PASSWORD_RECOVERY_PUBLIC_HTTPS_PENDING:** source Backend +
  Android + migration đã hoàn thiện; Backend typecheck/build, 335/335 tests và 1/1 PostgreSQL
  integration PASS; Android Debug/E2E build, 75/75 unit, lint 0 lỗi và 3/3 instrumentation PASS.
  Backup production 5.615.598 byte restore-check PASS; DB cô lập migration Up/Down/Up PASS và
  snapshot 5.000 cards/19 topics/3 users/14 progress/12 Quiz/2 scores/5 XP được bảo toàn.
  Migration M14 đã áp dụng production sau backup/restore-check. Backend cổng 3000 đã restart
  đúng build mới. Google Sign-In production trên AVD đã PASS: token thật, liên kết user 55,
  logout/login lại, JWT, Home và restart giữ session; 3 users/1 identity, không tạo trùng, user
  giữ 10 progress/9 Quiz/30 XP; force-stop/relaunch giữ session. Production giữ 5.000 cards,
  19 topics, 3 users, 14 progress, 12 Quiz, 2 scores và 5 XP history. Gmail SMTP verify PASS,
  email thật đã nhận và local reset E2E thành công. Auth version tăng 1→2, 0 active reset token,
  JWT cũ 401, JWT hiện hành 200 và đăng nhập mật khẩu mới/Home PASS. Script nhập SMTP an toàn
  nằm tại `maintenance/configure-smtp.ps1`; chỉ HTTPS public còn chờ. App Password đầu tiên bị
  Gmail từ chối `EAUTH/535`; App Password mới đã khắc phục và không bị ghi log.
  Kiểm tra bổ sung tài khoản Google-only xác nhận Forgot trả HTTP 200 an toàn nhưng không tạo
  token/email vì tài khoản không có mật khẩu BiVoca. Backend và Android nay giải thích phải dùng
  `Tiếp tục với Google`; snapshot hiện có 4 users/2 identities/1 passwordless user, 0 active
  reset token và toàn bộ dữ liệu học giữ nguyên. Backend 335/335 tests; Android build/unit/lint
  PASS; backend cổng 3000 và APK Debug đã được triển khai lại.
  Xem `M14_GOOGLE_SIGNIN_PASSWORD_RECOVERY_REPORT.md`.
  Hotfix 2026-10-23 xác nhận APK Debug gọi cổng 3000 đang chạy `dist` cũ nên challenge trả
  HTTP 404; APK E2E gọi cổng 3001 trả HTTP 200. Android không còn hiển thị thẳng
  `Route not found`; build E2E, 75/75 unit và lint 0 lỗi PASS. Google E2E đã hoàn tất.
- M8, M9 và M10 giữ nguyên DONE. Lượt ôn có ghi trên OPPO ở M10 thực hiện với DB riêng; người dùng cần đăng nhập lại tài khoản ở DB vận hành sau khi phiên thử nghiệm được đăng xuất.
- **M11 Quên mật khẩu — mốc UI lịch sử:** màn Dark Night Sky đã nghiệm thu từ M11. Phần
  API/email trước đây còn thiếu nay đã được triển khai trong source M14; SMTP thật và migration
  production vẫn chờ cấu hình/phê duyệt. Xem `M11_FORGOT_PASSWORD_REPORT.md` và
  `M14_GOOGLE_SIGNIN_PASSWORD_RECOVERY_REPORT.md`.

---

## 3. KNOWN ISSUES & ENVIRONMENT NOTES (LƯU Ý MÔI TRƯỜNG)
- **Cổng 3000 & 5173:** Đang chạy ổn định phục vụ Android và Web. Backend listening on 127.0.0.1:3000, Web UI at http://localhost:5173.
- **Kết nối điện thoại thật:** Đã cấu hình `adb reverse tcp:3000 tcp:3000`. Khi rút cáp cắm lại thiết bị, chạy lại lệnh:
  `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:3000 tcp:3000`
- **Android Emulator:** AVD `BiVoca_API35_Pixel6` đã tạo và có `hw.keyboard=yes`; Android nhận bàn phím máy tính, đồng thời vẫn hiện bàn phím ảo. Từ 2026-10-16, Retrofit tự dùng `10.0.2.2:3000` trên Emulator nên không cần `adb reverse` sau khi khởi động lại. Điện thoại thật vẫn dùng `127.0.0.1:3000` qua reverse USB.

## Cập nhật kết nối Emulator sau khi khởi động lại — 2026-10-16

- Sửa nguyên nhân app mất Backend sau restart: `ApiBaseUrl` chọn `10.0.2.2` cho Android Emulator và giữ `127.0.0.1` cho thiết bị thật.
- Hai Retrofit client dùng chung resolver; Backend/API/DB không thay đổi.
- `assembleDebug`, `testDebugUnitTest`, `lintDebug` PASS; **82/82 tests**, lint 0 issue.
- AVD cold boot, `adb reverse --remove-all`, danh sách reverse trống; request thật từ Emulator tới `GET http://10.0.2.2:3000/api/v1/topics` trả **HTTP 200**.
- Windows Startup đã có `BIPASSION Local Admin.lnk`; launcher nền tiếp tục tự bật Backend/Admin sau khi đăng nhập. Xem `ANDROID_EMULATOR_CONNECTION_FIX_REPORT.md`.

---

## Cập nhật bổ sung M7C — 2026-10-13

- `[x]` Luồng Home → Topics → chọn Food mở Flashcard với `topicCode` và `topicName` thật.
- `[x]` Android gọi API public `GET /api/v1/topics/{code}/flashcards`; response map `id`, `english`, `vietnamese`, `pronunciation`, `partOfSpeech`, `example`, `exampleVi`, `level`. API không cần đăng nhập riêng.
- `[x]` OPPO CPH2557 (Android 15): build, cài APK và kiểm tra thật: Food tải 30 thẻ; `apple` → Next `bread` → Previous `apple`.
- `[x]` `assembleDebug`, `testDebugUnitTest` (14/14 PASS), `installDebug`; `lintDebug` hoàn thành (0 lỗi, 194 cảnh báo lint).
- `[ ]` Chưa xác nhận bằng tai người dùng rằng âm thanh TTS nghe được. Google Text-to-Speech có trên máy; phép thử âm thanh tự động bị gián đoạn trước khi xác nhận trạng thái hoàn tất. Không đánh dấu kiểm thử âm thanh PASS.
- Phạm vi M7C trong lần xác minh này không gồm Quiz, Room hay thay đổi backend.

## Cập nhật Leaderboard + JWT hết hạn — 2026-10-14

- Kiểm tra và dùng lại các lớp Leaderboard hiện có, không tạo Activity/repository/viewmodel trùng.
- Backend `gamification.route.ts` áp dụng `authenticate` cho toàn router. Leaderboard nhận `limit` 1–100, mặc định 20; response gồm `rank`, `userId`, `username`, `fullName`, `avatarUrl`, `totalXp`, `level`, `currentStreak`, `longestStreak`. Android gọi với `limit=50`.
- Trên OPPO CPH2557 Android 15: Home → Leaderboard hiển thị danh sách từ API thật → quay lại Home → Topics → Food → Flashcard.
- Tạo `AuthSessionExpiry` để xử lý 401 tập trung và bảo vệ token mới nếu một request cũ bị từ chối sau khi đăng nhập lại.
- `assembleDebug` PASS; `testDebugUnitTest` 20/20 PASS; `lintDebug` PASS (0 lỗi, 195 warnings). Kiểm thử unit cover JSON mapping, empty response, network failure, HTTP 401/403 và không xóa phiên với 403/5xx.
- Backend unauthenticated probe trả HTTP 401. Không giả lập token hết hạn trên tài khoản thật; TTS không được kiểm tra lại trong lượt này.

## Cập nhật M8 Room Offline Cache — 2026-10-14

- **DONE:** Đã nghiệm thu thực tế trên OPPO CPH2557 Android 15, ngoài build/unit tests. ADB ban đầu unauthorized và được người dùng cấp quyền USB debugging.
- Room **2.8.5**, Java annotationProcessor, database v1 `bivoca-learning-cache.db`, export schema. Thêm `data/local/{LearningCache,RoomLearningCache,LearningDatabase,LearningCacheDao,TopicEntity,FlashcardEntity,CacheSyncEntity}.java`; chỉ lưu nội dung public và thời điểm sync.
- Mở rộng hai repository có sẵn; `NetworkFirstLoader` và `CacheDispatchers` thực hiện Network First, Cache Fallback trên worker. Transaction lưu snapshot + marker; null phân biệt với cache rỗng; flashcard tách theo `topicCode`. Lỗi ghi Room vẫn trả dữ liệu API hợp lệ.
- Chỉnh Topic/Flashcard models, hai ViewModels/Activities, TopicAdapter, hai layout và `offline_strings.xml`; giữ vị trí thẻ, hủy request cũ, không tải trùng khi xoay. Offline khóa Favorite/Review/Quiz; Review thất bại không tự chuyển thẻ.
- API Topics thực tế không có `flashcardCount`; ẩn nhãn số lượng không có dữ liệu. Topics chuyển sang client public đúng contract; không sửa Authentication/JWT.
- Lượt triển khai: `:app:assembleDebug` PASS; `:app:testDebugUnitTest` **56/56 PASS**; `:app:lintDebug` PASS **0 lỗi/195 cảnh báo**; `:app:assembleDebugAndroidTest` PASS. Lượt nghiệm thu: `:app:connectedDebugAndroidTest` **5/5 PASS**, 0 failure/error/skipped; 5 test duy nhất chạy thành công hai lần.
- OPPO Online: 10 topics, FOOD30 (`apple`, `bread`), ANIMALS30 (`dog`, `cat`). Ngắt reverse3000, force-stop/relaunch: Topics/Flashcard đọc Room cùng banner Offline; chuyển chủ đề đúng, FAMILY chưa cache báo lỗi. Lật, Previous/Next, xoay ngang/dọc giữ `bread` 2/30; Favorite/Review/Quiz disabled, Review không chuyển thẻ. TTS callback `Đã phát âm.`, chưa xác nhận âm thanh bằng tai.
- Khôi phục reverse, retry Flashcard/Topics và mở ANIMALS: API online, banner tắt, marker Room cập nhật. Snapshot offline không đổi; snapshot sau guarded instrumentation vẫn giữ nguyên. Reverse cuối `tcp:3000 tcp:3000`, rotation `lock 0` đúng ban đầu.
- **Sự cố nghiệm thu:** Gradle lần instrumentation đầu tự gỡ app sau test, làm mất phiên local. Đã cài lại bằng `-r`, người dùng đăng nhập lại. Chỉ sửa `android-app/gradle.properties` thêm `android.injected.androidTest.leaveApksInstalledAfterRun=true`; preflight xác nhận `keepInstalledApks=true`, `uninstallIncompatibleApks=false`, lần chạy có guard PASS và giữ app/phiên/cache. Một yêu cầu chạy lại trước preflight bị approval review chặn; sau khi có bằng chứng được cho phép, không còn thao tác bị chặn chờ xử lý.
- Không sửa Backend/PostgreSQL/Admin/dataset, không migration/reset/xóa user data. Cache không TTL/tự tải toàn bộ; không cache hoặc đồng bộ tiến độ cá nhân; TTS offline phụ thuộc voice cài sẵn.
- Danh sách file đầy đủ, bằng chứng kiểm thử, kịch bản OPPO và hướng dẫn tiếp nhận: `M8_ROOM_OFFLINE_REPORT.md`.

## Cập nhật M9 Backend, PostgreSQL, Admin Web & Security — 2026-10-14

- **DONE trong phạm vi M9 local:** đủ build/test, migration/catalog, HTTP và PostgreSQL integration có bằng chứng; không dựa riêng trên build.
- Đọc lại docs/source và xác minh `flashcard_english` PostgreSQL 18.6: 5 migration, 10 topics/300 cards, 3 users (1 ADMIN/2 USER). M6A đã apply; sửa tài liệu login dùng email và Admin update dùng PUT, bổ sung hai endpoint progress/history.
- Backend typecheck/build PASS, 311/311 unit/HTTP tests với repository doubles PASS. Integration chỉ đọc DB gốc 5/5 PASS; 68/68 probes GET trên server 3000 PASS.
- Sau chấp thuận của người dùng, tạo DB mới `bivoca_m9_20261014_5fc9558e413d`, seed/apply 5 migration chỉ ở đó. Auth/Progress/Quiz/Gamification/Admin lifecycle + read-only integration 10/10 PASS; concurrency và rollback 500 đúng. Exact UUID/ID cleanup xong; DB test giữ lại với 10 topics/300 cards/5 migration, 0 dữ liệu người dùng/học tập.
- Browser thật: người dùng đăng nhập ADMIN; Dashboard 3 users/300 Active cards/3 answers/33,33%; topics FOOD, card filters FOOD/Active/A1 và trang 2; form validation; users/detail 4 progress/3 Quiz/10 XP, khóa tự đổi role; stats range/topic và null accuracy; leaderboard; restore qua navigation; Logout, Back và truy cập protected URL bị chặn về Login PASS.
- Lỗi thực tế: AuthSession.restore bắt mọi lỗi và logout, làm mất token với network/500/503. Red tests 3 FAIL trước sửa; sửa tối thiểu 3 source Admin, thêm 10 tests (32 → 42), không bỏ test cũ. Final 42/42 PASS, build/format PASS. Có test late restore401 không hủy login mới và generic403/5xx không invalid session.
- Đối chiếu cuối: số bản ghi/role và SHA-256 topics/cards DB gốc khớp baseline. 20 log files được scan, không thấy secret/JWT/hash/connection URI theo patterns đã kiểm tra. Browser ghi nhận 0 error/0 warning trong tối đa 20 log gần nhất.
- Mutation UI không được lưu lên DB gốc; UI behavior có fetch-fixture tests, mutation API có PostgreSQL riêng thật. Chưa làm browser E2E cho mọi mutation/expired/network injection; không giả lập hết hạn phiên thật. Không có thay đổi Android, không chạy lại điện thoại; M8 DONE và guard giữ APK giữ nguyên.
- Báo cáo: `M9_INTEGRATION_SECURITY_REPORT.md`; evidence trong `backend/.m9-verification/`, `admin-web/.m9-verification/`. Tab kiểm thử kết thúc ở Login sau Logout; server 3000/5173 vẫn phục vụ.

## M11 UI MÀN 4–10 + AVATAR — 2026-10-15 (LỊCH SỬ: E2E KHI ĐÓ CÒN CHỜ)

- Topics, Flashcard thường/M10, Quiz, Progress, Leaderboard và Profile đã đồng bộ Dark Night Sky; giữ shared top bar/Drawer và API/M8/M10/business logic. Topics có filter local; Flashcard có flip nhẹ; Quiz khóa gửi trùng; Leaderboard highlight đúng session userId.
- Backend không có endpoint cập nhật avatar. Android dùng SAF `OpenDocument`, preview xác nhận/hủy, decode sampling và lưu app-private `files/avatars/user_<userId>.jpg`; Home/Profile/Drawer dùng chung, reset được và cách ly tài khoản.
- AVD API 35: online read, Room offline Topics/FOOD, Review offline refusal, rotation, navigation và avatar persistence PASS. `assembleDebug` PASS; **80/80 unit tests PASS**; lint 0 lỗi/185 cảnh báo. Có 11 screenshot thật trong `android-app/app/build/reports/m11-complete-ui/`.
- Chưa đánh dấu M11 UI Redesign DONE vì chưa được phép gửi Quiz answer trên DB vận hành, nên chưa có `06-quiz-result.png`. Xem `M11_UI_REDESIGN_4_TO_10_REPORT.md`. M8/M9/M10 giữ DONE.

## M11.1 QUIZ RESULT E2E — 2026-10-17 (DONE)

- Phần Quiz Result còn chờ ở mục lịch sử trên đã hoàn tất bằng DB riêng `bivoca_m11_20261017_87faf608e230`; role DB test có quyền tối thiểu, 10 topics/300 cards/6 migrations. Tài khoản test được tạo qua Auth API thật; không ghi secret vào tài liệu.
- AVD API 35 chạy package E2E riêng ở `10.0.2.2:3001`. Hai lượt Quiz 10 câu đạt 9/10; UI khóa gửi trùng, network error không tự retry, retry thủ công tăng đúng một bản ghi, xoay giữ state, Làm lại không ghi trước khi chọn và Home refresh dữ liệu sau khi quay về.
- Kết quả cuối DB test/API: 21 Quiz results, 18 đúng/3 sai, 21 progress/reviews, accuracy 85,71%, 180 XP, level 2, streak 1. Màn result mới có total/correct/wrong/accuracy và đúng theme; 11 ảnh thật ở `android-app/app/build/reports/m11-quiz-e2e/`.
- Android `assembleDebug` + `assembleE2e` PASS, **91/91 unit tests**, lint **0 lỗi/268 cảnh báo**. Backend typecheck/build PASS, **313/313 tests**; Quiz và Gamification database integration đều **1/1 PASS**.
- `flashcard_english` trước/sau giữ 3 users, 8 quiz results, 10 progress, 2 scores, 2 XP history, 10 topics, 300 cards, 6 migrations; hash topics/cards không đổi. Backend test cổng 3001 đã dừng, DB test giữ làm bằng chứng; Backend vận hành cổng 3000 giữ nguyên.
- API answer chưa có idempotency key, vì vậy timeout mất response sau commit là giới hạn còn lại; Android không tự retry. Xem `M11_QUIZ_RESULT_E2E_REPORT.md`. **M11 UI Redesign DONE**; M8/M9/M10/M12/M12.1 giữ trạng thái cũ.

## M11.2 AVATAR RIGHT DRAWER & UNIFIED BACK — 2026-10-17 (DONE)

- Shared authenticated top bar bỏ hamburger: Home chỉ có avatar phải; màn chức năng có Back trái, title giữa, avatar phải. Avatar dùng `LocalAvatarStore` theo userId hoặc ký tự đầu username; Home không còn avatar trùng mở Profile.
- Drawer duy nhất chuyển sang `GravityCompat.END`, rộng 84%/max 360dp; đủ 9 mục, selected state, X/scrim/System Back, offline và logout clear task PASS. Settings Detail dùng shared chrome, không Back trùng.
- Quiz Back khóa khi SUBMITTING, hỏi xác nhận nếu lượt đang làm có đáp án và không tự submit/XP. Flashcard giữ vị trí/mặt lật khi mở đóng Drawer. Home tên dài ở font 1.3 xuống dòng và không bị trăng che.
- Android Debug/E2E build PASS, **96/96 unit tests**, lint **0 lỗi/269 cảnh báo**. APK cuối cài `-r`; AVD API35 portrait/landscape, avatar ảnh/fallback, navigation và offline PASS. Đủ 15 ảnh tại `android-app/app/build/reports/m11-2-avatar-right-navigation/`.
- Không sửa Backend/PostgreSQL/Room/M10/M12. `flashcard_english` cuối `unchanged=true`; Backend 3000 health `ok/connected`, test server 3001 đã dừng. Xem `M11_2_AVATAR_RIGHT_DRAWER_NAVIGATION_REPORT.md`.

## M12 SETTINGS — 2026-10-17 (DONE)

- Thêm Settings trong Drawer và đủ sáu nhóm Học tập, Âm thanh, Nhắc học, Giao diện, Dữ liệu, Giới thiệu theo Dark Night Sky. SharedPreferences tách cấu hình theo user và device; runtime đã xác minh giữ cấu hình sau force-stop/relaunch.
- Flashcard áp dụng English/Vietnamese First, auto pronunciation, speech rate, cỡ chữ và reduce motion. Daily goal chưa có X/Y vì Backend/Android không có chỉ số “thẻ mới hôm nay” tin cậy. Emulator thiếu TTS engine nên error path PASS, chưa xác nhận âm thanh bằng tai.
- Reminder dùng 7 inexact weekly AlarmManager alarms, quyền Android 13+, kiểm tra session/user/day trước khi hiển thị, phục hồi sau boot/timezone/time change, hủy khi tắt/logout. Notification thật và reboot reconcile PASS; cuối lượt reminder trả về tắt/20:00/7 ngày.
- Data Settings đọc 10 topics/150 flashcards/430.3 KB từ Room thực tế; xóa chỉ ba bảng public cache và được test bằng DB in-memory, không xóa app data thật.
- `assembleDebug`, `testDebugUnitTest` **89/89**, `lintDebug` **0 lỗi/268 cảnh báo**, `assembleDebugAndroidTest` PASS. Instrumentation Room **6/6** và notification **1/1** PASS. APK/test APK cài `adb install -r` trên `emulator-5554`.
- 12 ảnh Emulator ở `android-app/app/build/reports/m12-settings/`; báo cáo đầy đủ: `M12_SETTINGS_REPORT.md`. Backend/PostgreSQL/Admin/dataset không đổi; M8–M11 giữ nguyên trạng thái trước đó.

## M12.1 SETTINGS UI/UX POLISH — 2026-10-17 (DONE; USER AUDIO VERIFICATION PENDING)

- Settings dùng color/state resource cyan/blue thống nhất cho radio, switch, day chip và action; radio/nhãn nằm cùng hàng. Reminder days chuyển thành hai hàng chip có check/outline và contentDescription đầy đủ; TimePicker dùng dark theme với action cyan.
- Data stats được căn label/value; Previous/Next Flashcard có state enabled/disabled/pressed rõ và không còn màu tím. Không đổi Settings persistence, ReminderScheduler/Receiver, Room, nghiệp vụ Flashcard, Backend hoặc PostgreSQL.
- Emulator API 35 xác minh đủ sáu nhóm Settings, status ngày 7→6→7, Room 10 topics/150 cards/526.5 KB, Flashcard `apple`/`bread`, force-stop persistence, landscape và font scale 1.3. Cấu hình cuối phục hồi auto TTS off, rate 1.0, goal 10, front EN, reminder off/all days.
- Google TTS engine khởi tạo; callback manual 1.0x/0.8x và auto 1.2x PASS. Chưa có người nghe âm thanh từ loa nên giữ **USER AUDIO VERIFICATION PENDING**.
- `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug` PASS; **89/89 tests**, lint **0 lỗi/268 cảnh báo** bằng baseline. 12 ảnh bắt buộc và 2 ảnh accessibility ở `android-app/app/build/reports/m12-ui-polish/`. Xem `M12_1_SETTINGS_UI_POLISH_REPORT.md`.

## M13 LARGE VOCABULARY + CEFR — 2026-10-21 (STAGING ACCEPTED; PRODUCTION CHƯA TRIỂN KHAI)

- Đã audit CEFR 7.988 dòng và SQLite Anh–Việt 44.212.224 byte/104.829 words/158.307 definitions; lưu attribution MIT và CC BY-SA 4.0. Pipeline 24.388 ứng viên chỉ tự duyệt **58** thẻ chất lượng; 23.990 cần review, 340 rejected. Mục tiêu 5.000 **chưa đạt**, không hạ chuẩn hoặc tạo nghĩa/ví dụ giả.
- Migration provenance/source/topic-many-to-many, importer transaction/idempotent và rollback failure injection PASS trên DB riêng `bivoca_m13_large_20261021_b06075c1d427`. Staging cuối 16 topics/358 cards; import lần hai inserted 0; 0 FK violation/duplicate source record. Production `flashcard_english` giữ 10/300 và checksum trước/sau không đổi.
- Backend thêm catalog count, search English/Vietnamese, CEFR/topic filter và paging; Quiz chỉ nhận imported card approved+eligible. Typecheck/build PASS, **320/320 tests**, DB integration **4/4**. Admin regression **42/42** và build PASS.
- Android thêm topic counts, Room migration 1→2, Vocabulary Search paging và UI nối API thật. Build/unit/lint PASS (**96/96**, lint 0 lỗi); Room instrumentation **8/8**. Emulator API35 xác minh 16 topics, thẻ A1–B2 mới, flip/Previous/Next/search/Quiz, offline Room, reconnect, Settings và navigation; đủ 16 ảnh thật.
- **M13.1–M13.5 DONE trên staging**. Production chưa migration/import/deploy; rollout cần phê duyệt riêng và phải đồng bộ Backend trước Android. Tài khoản E2E staging/credential tạm đã xóa. Xem `M13_LARGE_VOCABULARY_IMPLEMENTATION_REPORT.md`.
# M15.5 Vocabulary 20.000 — 2026-10-25 (REPAIR V2; PRODUCTION BLOCKED)

- Repair v2 checksum khớp; ba script read-only **10/10 PASS**. 57 hard block được match đúng
  English+POS+nghĩa gốc và loại hết; không dùng ID staging hoặc hai shortlist 15K chưa duyệt.
- VNEDICT substring được đổi thành `independentLexicalOverlap`; `semanticVerified=false`. Pipeline
  và publisher nay fail-closed, chỉ cho publish khi có semantic verification nguồn rõ ràng.
- Artifact preflight vẫn đủ 22.000 nhưng có **0 semantic verified**, còn 1.466 REVIEW từ Repair v2
  và 7.903 candidate mới ngoài triage cũ. Số thẻ bổ sung có thể triển khai an toàn: **0**.
- Pipeline 19/19, Backend typecheck/build PASS. Không chạy module không liên quan.
- Production giữ 5.000 cards/19 topics và toàn bộ user data; không backup deployment/import
  checkpoint. M15.5 chưa DONE, không chuyển M16. Xem mục 19 báo cáo M15.5.

# M15.5 Vocabulary 20.000 — 2026-10-25 (DATA_IMPORT_DONE; SEMANTIC_REVIEW_PENDING)

- Theo chính sách mới đã được phê duyệt, production `flashcard_english` đã import theo checkpoint
  10K → 15K → **20K**, 20 topics; lần chạy lại idempotent chèn 0 hàng.
- 15.000 thẻ mới là `IMPORTED_UNVERIFIED`, provenance `NEEDS_REVIEW`, `quality_verified=false`,
  Quiz-eligible=0 và Quiz leak=0. Không tuyên bố đã kiểm duyệt nghĩa.
- Backup/restore-check PASS; checksum 5.000 thẻ gốc và users/progress/Quiz/scores/XP khớp.
- Backend 338/338, typecheck/build PASS. Android build/unit/lint PASS; APK `-r` trên Emulator đọc
  production 20K và hiển thị nhãn “Chưa kiểm duyệt ngữ nghĩa”. Evidence/report ở mục 20 của
  `M15_5_VOCABULARY_20000_REPORT.md`.

# M15.6 Topic Reclassification — 2026-10-25 (DONE)

- Production giữ đúng 20.000 Flashcard; 14.329 thẻ `GENERAL_VOCABULARY` đã chuyển hết: 1.471 vào topic ngữ nghĩa, 12.858 vào nhóm POS, còn 0 chưa phân nhóm.
- Database có 38 topic (36 topic có dữ liệu được API/Android hiển thị); General còn 0 thẻ và bị ẩn. Noun 6.000, Verb 3.381, Adjective 2.889 là ba nhóm lớn nhất.
- Backup/restore-check và staging cô lập PASS. Production apply idempotent; checksum nội dung thẻ ngoài topic, users, progress, Quiz, scores, XP và provenance khớp baseline. 15.000 thẻ M15.5 giữ `IMPORTED_UNVERIFIED`, Quiz eligible 0.
- Backend typecheck/build và 338/338 tests PASS; Admin 43/43 và build PASS. Android build/lint, 76/76 unit tests PASS; Emulator xác minh nhóm UI, General ẩn, Noun `1 / 6000`, flip/Next/Previous và search.
- Báo cáo: `M15_6_TOPIC_RECLASSIFICATION_REPORT.md`. M16 chưa được triển khai.

# M15.7 Semantic Topic Enrichment — 2026-10-25 (DONE)

- Chỉ xử lý 12.858 thẻ POS từ M15.6. Rule local theo English + nghĩa Việt + POS, morphology có giới hạn, false-positive exclusions và audit override đã chuyển thêm **426** thẻ; còn 12.432 ở POS vì chưa đủ căn cứ.
- Primary semantic tăng 7.142 → 7.568 (35,71% → 37,84% toàn kho). Tổng giữ 20.000 cards/38 topics; General 0; 15.000 imported-unverified vẫn Quiz-eligible 0.
- Staging cô lập, transaction rollback test thực tế, verify và idempotency PASS. Backup production restore-check PASS; protected checksum nội dung/users/progress/Quiz/scores/XP/provenance khớp.
- Backend typecheck/build, 338/338 tests PASS. Android source không đổi; build/lint và 76/76 tests PASS. Production API paging Noun 5.729/58 trang PASS. APK `-r` tới Login; authenticated UI count chưa nghiệm thu lại và được ghi đúng trong báo cáo.
- Báo cáo: `M15_7_SEMANTIC_TOPIC_ENRICHMENT_REPORT.md`. M16 chưa triển khai.

