# M16.6 — Light Colorful Frontend Report

Ngày kiểm chứng: 02/11/2026. Trạng thái: **READY_FOR_ACCEPTANCE**, chờ người dùng xem ảnh và nghiệm thu. Không tự đánh dấu DONE/PASS milestone, không tiếp tục M17.

## 1. Kết quả và phạm vi

Đã chuyển các màn Android hiện có sang Light Colorful / Editorial Modern theo master task M16.6 và PNG refined trong ZIP người dùng cung cấp. Đây là yêu cầu mới sau khi M17.1 đã hoàn tác; không khôi phục palette/font/CSS của M17.1. Native Java/XML/MVVM, không WebView hoặc dữ liệu demo.

**Lưu ý nghiệm thu:** bộ UI responsive đã PASS, có một case phải chạy lại sau timeout focus; kiểm thử điều hướng từng gặp Quiz GET timeout. Lượt kiểm tra routing toàn luồng gần nhất: **FAIL — Quiz timeout hoặc lỗi được ghi nguyên văn trong log**. Không kết luận mạng/Backend ổn định chỉ vì một lượt chạy lại PASS. Chưa thay tầng mạng hoặc Backend để che lỗi.

| Màn/phần | Thực hiện | Bằng chứng/phạm vi |
| --- | --- | --- |
| Auth | Login/Register/Forgot; mint brand band, white fields, Cobalt CTA | Auth layout matrix + 4 runtime tests trước đăng nhập; Google do người dùng tự chọn → Home. |
| Home | Logo/mascot gốc, slogan BVP800, mint goal/navy CTA, 4 ô SRS pastel, XP thật | GET-only runtime, cuộn, Drawer, font1/1.3, responsive. |
| Topics/Detail | Khung trắng, icon pastel theo semantic/POS, All cố định, 2 danh sách độc lập; metadata/preview thật | Catalog/filter/search/empty/retry state layout; Detail bounded preview/recreation/routing. Landscape header/search cạnh nhau. |
| Flashcard | Hai mặt trắng/navy, ví dụ mint tint, flip/rating/status/session completion theo token | 7 runtime/layout tests; paging100→101→100; hai mặt/scroll/rotation/TTS callback; không chấm thẻ. |
| Quiz | Question Cobalt/chữ trắng, đúng mint/sai coral, result theo token | Câu hỏi GET thật và routing; không trả lời. Còn timeout GET không ổn định; xem mục giới hạn. |
| Account | Profile, Progress/chart, lifetime-XP Leaderboard, Settings/menu + 5 phần chi tiết | Màn thật và API thật; không lưu form/toggle/reminder hoặc đổi tài khoản. Streak dùng vector gốc. |
| Search | Từ, nghĩa, IPA/POS/level/example từ API | Tìm apple/táo, kết quả rỗng thật; không ghi Favorite. |
| Navigation | 4 tab, white surface, vector22dp, label12sp có gutter/wrap, right Drawer | Giữ routing/guard/VM/queue/context M16; actual insets5màn, rotation + 4 cấu hình. |

Không có Activity Word Detail hoặc Favorites standalone trong source phục dựng. Không tạo màn/luồng mới; Search giữ các trường từ vựng hiện có, nút Favorite hiện có chỉ được restyle và không bấm trong kiểm thử. Settings giữ menu và các màn chi tiết thay vì gộp thành form demo.

## 2. Design tokens và typography

| Token | Giá trị |
| --- | --- |
| canvas | #F7F7F2 |
| surface | #FFFFFF |
| text | #212A37 |
| secondary_text | #718090 |
| small_text | #5E6B79 |
| border | #E4E7E7 |
| cobalt | #2855D5 |
| cobalt_soft | #E7EDFF |
| coral | #F29A7E |
| coral_soft | #FCE7E0 |
| mint | #B9E5D3 |
| mint_soft | #E3F5EB |
| lavender | #B4A3E7 |
| lavender_soft | #F0ECFA |
| honey | #F0C76C |
| honey_soft | #FFF3D2 |
| mint_ink | #246B55 |
| coral_ink | #9B422B |
| lavender_ink | #66509F |
| honey_ink | #805C10 |
| on_primary | #FFFFFF |
| scrim | #66212A37 |
| ripple | #142855D5 |

Nền F7F7F2, card trắng, navy212A37, Cobalt2855D5; pastel chỉ làm nền icon/badge/khối nội dung. Token tập trung tại light_colorful_tokens.xml; alias resource cũ trỏ về các role này, không tạo hệ token thứ hai. Spacing4/8/12/16/24/32dp, card20dp, goal/SRS24dp, input12dp, button14dp; vùng chạm tối thiểu48dp, tab≥64dp. Không rải gradient/shadow; ưu tiên border nhẹ và khoảng trắng.

Be Vietnam Pro được đóng gói local400/500/600/700/800, family + TextAppearance dùng chung, không tải font runtime. Nguồn [Google Fonts chính thức](https://github.com/google/fonts/tree/main/ofl/bevietnampro), OFL kèm res/raw/be_vietnam_pro_ofl.txt. Android và AppCompat đều khai báo fontFamily; chart/navbar động dùng file font thật. Không italic tiếng Việt, tracking âm hoặc fake-bold.

Kiểm thử Activity thật từng phát hiện heading800 bị rơi về400 do fontFamily AppCompat kế thừa; đã sửa cả hai thuộc tính và test actual RegisterActivity xác nhận800/noFakeBold. Không dùng kết quả inflate detached để thay cho kiểm chứng Activity thật. Glyph sheet là test harness typography riêng, không phải màn sản phẩm/dữ liệu nghiệp vụ giả.

Chuỗi kiểm tra cho đủ5weight:

> Tiếng Việt: ă â ê ô ơ ư đ  
> Sắc, huyền, hỏi, ngã, nặng.  
> Đăng ký tài khoản – Chủ đề – Trắc nghiệm – Thuần thục.  
> Nguyễn, Trường, Hướng dẫn, Từ vựng.

Độ tương phản tính từ token: navy/canvas13.47:1, white/Cobalt6.27:1; small_text/canvas5.07:1; các semantic ink trên soft5.5–5.63:1. Token secondary718090 vẫn giữ đúng spec nhưng chỉ3.76:1 trên canvas, nên nhãn nhỏ dùng5E6B79. Không tuyên bố toàn bộ màu pastel đạt AA cho chữ nhỏ; chúng dùng làm nền, đi với ink đậm.

## 3. Ảnh Emulator và đối chiếu demo

Tất cả ảnh sản phẩm dưới đây là Android Emulator thật, không ghép mock. Mẫu refined được lưu riêng trong reference để đối chiếu. Ảnh account cục bộ có thể chứa thông tin tài khoản; toàn bộ runtime-evidence được Git ignore và không đưa vào tài liệu trí nhớ/skill.

| Ảnh | Liên kết |
| --- | --- |
| Login thật, trước khi người dùng đăng nhập Google | [Login](../../android-app/runtime-evidence/m16-6-light-colorful/auth/login/login-top.png) |
| Home bản cuối | [Home](../../android-app/runtime-evidence/m16-6-light-colorful/matrix/412-font1/insets-HomeActivity-portrait.png) |
| Topics đầy đủ / POS / Detail | [Topics](../../android-app/runtime-evidence/m16-6-light-colorful/topics-detail/topics/topics-top.png) · [POS](../../android-app/runtime-evidence/m16-6-light-colorful/topics-detail/topics/topics-pos.png) · [Detail](../../android-app/runtime-evidence/m16-6-light-colorful/topics-detail/detail/detail-portrait.png) |
| Flashcard Browse hai mặt font1 | [Front](../../android-app/runtime-evidence/m16-6-light-colorful/flashcard/font1/flashcard-front.png) · [Back](../../android-app/runtime-evidence/m16-6-light-colorful/flashcard/font1/flashcard-back-review.png) |
| Flashcard320/font1.3 | [Front](../../android-app/runtime-evidence/m16-6-light-colorful/flashcard/font13-small/flashcard-front.png) · [Back](../../android-app/runtime-evidence/m16-6-light-colorful/flashcard/font13-small/flashcard-back-review.png) · [Sau cuộn](../../android-app/runtime-evidence/m16-6-light-colorful/flashcard/font13-small/flashcard-back-content-end.png) |
| Quiz trạng thái thật | [Quiz trong matrix](../../android-app/runtime-evidence/m16-6-light-colorful/matrix/412-font1/insets-QuizActivity-landscape.png) — trạng thái tại thời điểm chụp; routing cuối bị timeout, không dùng ảnh cũ thay kết quả mới. |
| Profile / Progress / Leaderboard | [Profile](../../android-app/runtime-evidence/m16-6-light-colorful/account/font1/profile.png) · [Progress](../../android-app/runtime-evidence/m16-6-light-colorful/account/font1/progress.png) · [Leaderboard](../../android-app/runtime-evidence/m16-6-light-colorful/account/font1/leaderboard.png) |
| Settings / Search / Auth phụ | [Settings](../../android-app/runtime-evidence/m16-6-light-colorful/account/font1/settings.png) · [Search](../../android-app/runtime-evidence/m16-6-light-colorful/account/font1/search-english.png) · [Register](../../android-app/runtime-evidence/m16-6-light-colorful/account/font1/register.png) · [Forgot](../../android-app/runtime-evidence/m16-6-light-colorful/account/font1/forgot.png) |
| Dấu tiếng Việt tại320/font1.3 | [Top](../../android-app/runtime-evidence/m16-6-light-colorful/account/font13-small/vietnamese-glyphs-top.png) · [Bottom](../../android-app/runtime-evidence/m16-6-light-colorful/account/font13-small/vietnamese-glyphs-bottom.png) |
| Home320/font1.3 / Topics landscape | [Home small](../../android-app/runtime-evidence/m16-6-light-colorful/matrix/320-font13/insets-HomeActivity-portrait.png) · [Topics ngang](../../android-app/runtime-evidence/m16-6-light-colorful/matrix/320-font13/insets-TopicsActivity-landscape.png) |

Khác biệt có chủ ý so với demo: dùng Be Vietnam Pro theo master task thay chữ serif trong PNG; dữ liệu goal/XP/streak/queue/topic/quiz là API thật, không chép số liệu demo; không thêm remember-me/DEMO label/prefill; giữ Settings menu/detail, metadata và các control đã nghiệm thu. Home dùng wordmark/mascot gốc. Nội dung dài được cuộn, không cắt để ép giống một ảnh tĩnh.

Nhãn tab12sp được giữ nguyên cỡ, thêm gutter và tối đa2dòng để không dính nhau ở font1.3. Topics landscape thu header/search/filter thành một toolbar ngang và giảm padding section để dành chỗ cho danh sách; subtitle tổng nhóm chỉ ẩn ở landscape vì số nhóm vẫn có trên chip/section. Portrait và toàn bộ ID/control/data/routing giữ nguyên. Ảnh mặt sau có tên legacy back-review là Browse với cụm đánh giá hiện có, không phải bằng chứng đã gửi Review.

Ở màn nhỏ/font1.3, minimum card thích nghi160–280dp thay clamp280dp cứng để giữ nút lật nhìn thấy; hai mặt giữ cùng bounds. Từ/IPA/nghĩa/ví dụ dài vẫn đọc bằng cuộn trong card, rating/status bằng cuộn trang ngoài. Màn đủ chỗ giữ vị trí lật gần80% như trước; không đổi cỡ chữ để ép nội dung vừa ảnh.

## 4. Skills và review đã áp dụng

- ui-ux-pro-max: kiểm tra hierarchy, độ tương phản,48dp target, phân vai màu trạng thái và checklist typography. Python không có trên máy nên không chạy search script; dùng quy tắc ưu tiên/pro-rules đã đọc, không giả kết quả tìm kiếm.
- android-mobile-frontend-design: improve-mode trên Activity hiện có, giữ brand/logo, xử lý tiếng Việt, font1.3, IME/insets, cảnh quan ngang và màn nhỏ. Không thiết kế lại nghiệp vụ.
- android-viewsystem-foundations: theme/font AppCompat, XML/ViewBinding IDs, variant layout-land và kiểm tra lifecycle/recreation.
- bivoca-quality-handoff: snapshot/hash, guard giữ APK, phân biệt runtime thật và layout fixture, giới hạn dữ liệu, bàn giao READY_FOR_ACCEPTANCE.
- Superpowers: lập kế hoạch A–F, chẩn đoán lỗi bằng evidence trước sửa, kiểm chứng trước kết luận và reviewer độc lập. Hai lượt review không có Critical/Important; Minor TimePicker font đã xử lý.

frontend-design-pro/design-system/ui-styling/brand và FRONTEND_SKILL_POLICY.md không tìm thấy tại các nguồn local đã kiểm tra; không cài thêm. Không ghi tên skill thiếu như đã sử dụng.

## 5. Build, test, lint và lỗi đã xử lý

Lệnh Android: gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest; install/update bằng adb install -r. Guard: leaveApksInstalledAfterRun=true, keepInstalledApks=true, uninstallIncompatibleApks=false (log dry-run fresh). Không uninstall/clear/wipe.

Build cuối **PASS**; unit **110/110 PASS**, failures0, skipped0. Lint **0 error / 467 warning**. Unit được thực thi ở build có thay đổi Java; các build chỉ XML có task UP-TO-DATE, ghi trong log, không cộng thành lượt unit mới. Cảnh báo lint vẫn còn (UnusedResources, HardcodedText, SetTextI18n, accessibility/overdraw...), chưa xử lý toàn bộ ngoài phạm vi này.

| Kiểm tra | Kết quả thực tế | Log |
| --- | --- | --- |
| A build/unit | PASS build | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/phase-a-build.log) |
| A glyph5weights | PASS 1 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/phase-a-glyph.log) |
| B layout matrix | PASS 2 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/phase-b-layout-tests.log) |
| B runtime4 | PASS 4 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/phase-b-runtime.log) |
| Auth font + Home responsive | PASS 3 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/phase-c-logo-and-auth-runtime.log) |
| D Flashcard7 | PASS 7 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/phase-d-runtime.log) |
| E screens + glyph4 | PASS 4 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/phase-e-runtime.log) |
| Small account + glyph4 | PASS 4 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/final-small-account-and-glyph.log) |
| Hồi quy responsive16 trước clamp compact | PASS 16 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/r2-regression.log) |
| Topics + navbar fixture sau compact toolbar | PASS 3 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/compact-topics-runtime.log) |
| 412×915 font1 bản cuối | PASS 1 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/r4-412-font1.log) |
| 412×915 font1.3 bản cuối | PASS 1 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/r4-412-font13.log) |
| 320×640 font1 bản cuối | PASS 1 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/r4-320-font1.log) |
| 320×640 font1.3 lần đầu — chờ Quiz focus | FAIL | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/r4-320-font13.log) |
| 320×640 font1.3 chạy lại nguyên source | PASS 1 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/r4-320-font13-retry.log) |
| Flashcard7/font1 bản cuối | PASS 7 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/r3-normal-flashcard.log) |
| Flashcard7/320/font1.3 bản cuối | PASS 7 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/r3-small-flashcard.log) |
| Routing GET-only lần cuối (trước compact toolbar) | FAIL | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/r3-routing-get-only.log) |
| Account/typography bản cuối | PASS 4 | [log](../../android-app/runtime-evidence/m16-6-light-colorful/logs/handoff-account.log) |

Không cộng các lần chạy lặp thành số test duy nhất. Actual-insets chạy5màn ở từng412×915/320×640,font1/1.3 và xoayngang; fixture matrix chọn đúng orientation để dùng layout-land. Auth có kiểm thử keyboard/validation/recreation, form không được submit thành công trong test; Google do người dùng trực tiếp chọn tài khoản.

Lịch sử FAIL được giữ trong logs:

1. Heading Activity800→400: sửa fontFamily Android+AppCompat; test Activity thật PASS.
2. Logo Home bị cắt ở320/font1.3: dùng wordmark asset gốc fitStart; layout/runtime PASS.
3. Lint14UseAppTint: chuyển sang app:tint cho AppCompatImageView; build/lint sau sửa PASS.
4. Topics inner swipe từng làm outerY1078 trong lượt batch đầu: bổ sung kiểm chứng outerY=0 trước gesture và ảnh trước/sau; không sửa logic cuộn production. Isolated test và hai batch hồi quy16 cuối PASS; nguyên nhân thời điểm của batch đầu chưa kết luận hoàn toàn.
5. Nhãn nav quá sát và Topics landscape320 bị cắt10px: test riêng tái hiện cả2FAIL; sửa gutter/wrap và landscape header. Test sau sửa2/2 PASS, tiếp tục matrix/batch bản cuối như bảng trên.
6. Quiz GET timeout không ổn định: có phản hồi200 khoảng1–9giây, xen SocketTimeoutException; log lỗi ứng dụng ghi “Máy chủ phản hồi quá lâu. Hãy thử lại.” Hai lượt matrix cũ bị chặn tại Quiz; một lượt412/font1.3 đã PASS. Không đổi timeout, tự retry trong test hoặc sửa SQL/VM/Repo để biến đỏ thành xanh. Bằng chứng timings chỉ xác định lỗi tải request, chưa phân định nguyên nhân sâu ở thiết bị/tầng mạng/server. Cần phiên xử lý riêng nếu người dùng duyệt phạm vi đó.
7. Test xoay/focus từng timeout; kiểm thử cuối dùng tư thế thiết bị cố định user_rotation0/auto-rotate0 để điều khiển xoay từng Activity, sau đó trả1/0. Vẫn có1lượt320/font1.3 không lấy được focus Quiz: không có crash trong log đã lọc, JWTauth/me200. Chạy lại đúng cấu hình và nguyên source PASS46.573s. Ghi rõ tính không ổn định của automation/transition; chưa kết luận nguyên nhân gốc và không tuyên bố đã sửa lỗi này. Giữ cả log FAIL/PASS, không tăng timeout hoặc sửa routing production để che lỗi.
8. Flashcard320/font1.3: test thật phát hiện Return action is not visible với clamp280dp. Sửa minimum card thích nghi theo roomForFlip, giữ giới hạn160dp và cuộn; test vẫn bắt cùng bounds giữa hai mặt và nút lật hiển thị đủ. Kết quả7test bản cuối ở font1 và320/font1.3 ghi trong bảng trên.

9. Rà ảnh bàn giao phát hiện hai card còn contrast sai: Leaderboard icon/subtitle Cobalt trùng nền và Progress heading trùng nền/percentage navy khó đọc. Đã đổi nền Leaderboard sang HoneySoft, Progress sang LavenderSoft và subtitle sang small_text, giữ toàn bộ giá trị/API. [Build/lint cuối PASS](../../android-app/runtime-evidence/m16-6-light-colorful/logs/final-contrast-cards-build.log); unit UP-TO-DATE sau thay đổi chỉ tint XML. Chụp lại UI thật qua Home → Drawer → màn đích → Back, không submit form. Ảnh trước sửa được giữ riêng, gallery chính trỏ ảnh đã sửa. Không chạy lại toàn bộ matrix cho hai màu nền này; có build/lint và kiểm chứng ảnh thực tế.

## 6. Giới hạn chưa kiểm chứng

- E2E ghi Review/ACK/pending/error/completion, Quiz Answer/result/exit sau khi đã trả lời và Favorite: **NOT_RUN** trên database phục dựng. Màu/layout các state này được restyle, có fixture/unit kiểm tra phần đã có; không dùng fixture làm ảnh nghiệp vụ đã hoàn thành. Cần database test cô lập được duyệt để kiểm tra ghi.
- Loading/error/empty: có layout/state tests, local Auth validation, Search/Topics empty từ GET thật; chưa cố ý làm hỏng Backend/mạng để chụp mọi lỗi thật của mọi màn. Quiz timeout là lỗi thật quan sát được. Không khẳng định đã chạy đủ mọi trạng thái server.
- TTS giữ nguyên logic; test gọi/callback không thay thế đánh giá chất lượng âm thanh/độ lag bằng tai.
- Chưa kiểm tra thiết bị vật lý, talkback đầy đủ, các OEM/time-picker rendering ngoài API37; theme TimePicker đã đồng bộ font nhưng chưa chụp dialog native riêng.
- Không có WordDetail/Favorites standalone để migration; không thêm tính năng mới ngoài phạm vi.

## 7. Bảo toàn dữ liệu, Auth và Git

Snapshot đầu phiên454file. Thay đổi UI/test/font:203file (181sửa, 22thêm), không mất baselinefile;0thay đổi ngoài whitelist,0hash lệch bản đã áp dụng. Backend72file, Admin37file,9Repository,11ViewModel,8Api interface,6AuthJava, manifest/build và backend.env giữ nguyên hash. Không sửa dependency/ClientID/Google flow/API contract/Room/SRS/XP/TTS.

GET cuối: PostgreSQL connected,20000Flashcard/38Topics; catalog36 (29semantic/7POS). JWT Google hiện có GET auth/me200, không xuất token/email/mật khẩu. Đây là đối chiếu count qua API, không phải checksum toàn bộ nội dung database.

Không migration/seed/import/reset/restore/M15.7Apply; không POST Review/QuizAnswer/Favorite; không sửa users/role/password. Emulator trả về1080×2400,density420,font1,rotation1/0; giữ phiên, mở lại Home. HEADa4a9801/feature/initial-import không đổi, staging rỗng; không commit/push/reset/clean/checkout-overwrite. Toàn bộ thay đổi/untracked có trước được bảo toàn.

## 8. Danh sách file thay đổi và bàn giao

| Phase | Số file |
| --- | --- |
| A | 17 |
| B | 30 |
| C | 112 |
| D | 16 |
| E | 28 |

Danh sách dưới đây so với snapshot đầu M16.6, không dùng git diff HEAD để gom công việc M16 cũ. Hash trước/sau trong [source-integrity.json](../../android-app/runtime-evidence/m16-6-light-colorful/source-integrity.json). Ngoài source/test này, thêm báo cáo hiện tại và append WORK_STATUS.md; ảnh/log nằm trong runtime-evidence ignored.

| Phase | Thao tác | Đường dẫn từ gốc project |
| --- | --- | --- |
| D | Sửa | `android-app/app/src/androidTest/java/com/example/bivoca/FlashcardFrontendUiTest.java` |
| C | Sửa | `android-app/app/src/androidTest/java/com/example/bivoca/HomeResponsiveUiTest.java` |
| C | Sửa | `android-app/app/src/androidTest/java/com/example/bivoca/LearningBottomNavigationUiTest.java` |
| C | Sửa | `android-app/app/src/androidTest/java/com/example/bivoca/TopicsUiTest.java` |
| D | Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/flashcard/FlashcardActivity.java` |
| C | Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/home/HomeActivity.java` |
| E | Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/leaderboard/LeaderboardAdapter.java` |
| C | Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/navigation/LearningBottomNavigation.java` |
| C | Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/navigation/NavigationDrawerController.java` |
| E | Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/progress/WeeklyActivityChartView.java` |
| E | Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/quiz/QuizActivity.java` |
| C | Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/topics/TopicAdapter.java` |
| C | Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/topics/TopicDetailActivity.java` |
| C | Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/topics/TopicPresentation.java` |
| C | Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/topics/TopicsActivity.java` |
| C | Sửa | `android-app/app/src/main/res/color/flashcard_nav_background.xml` |
| C | Sửa | `android-app/app/src/main/res/color/flashcard_nav_text.xml` |
| B | Sửa | `android-app/app/src/main/res/color/login_button_tint.xml` |
| C | Sửa | `android-app/app/src/main/res/color/nav_item_text.xml` |
| E | Sửa | `android-app/app/src/main/res/color/settings_action_background.xml` |
| E | Sửa | `android-app/app/src/main/res/color/settings_switch_track.xml` |
| C | Sửa | `android-app/app/src/main/res/color/topics16_chip_background.xml` |
| C | Sửa | `android-app/app/src/main/res/color/topics16_chip_text.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/auth_arrow_back.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/auth_google_callout.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/auth_info.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/auth_security_lock.xml` |
| E | Sửa | `android-app/app/src/main/res/drawable/avatar_circle_background.xml` |
| E | Sửa | `android-app/app/src/main/res/drawable/dark_card_background.xml` |
| D | Sửa | `android-app/app/src/main/res/drawable/fc16_back.xml` |
| D | Sửa | `android-app/app/src/main/res/drawable/fc16_check.xml` |
| D | Sửa | `android-app/app/src/main/res/drawable/fc16_chip.xml` |
| D | Sửa | `android-app/app/src/main/res/drawable/fc16_example.xml` |
| D | Sửa | `android-app/app/src/main/res/drawable/fc16_metric.xml` |
| D | Sửa | `android-app/app/src/main/res/drawable/fc16_more.xml` |
| D | Sửa | `android-app/app/src/main/res/drawable/fc16_review.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_avatar.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_badge.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_cup_icon_tile.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_daily_progress.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_icon_bolt.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_icon_home.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_icon_menu.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_icon_profile.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_icon_quiz.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_icon_review.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_icon_topics.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_menu_button.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_motivation.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_primary.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_rank_gold.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_rank_silver.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_row.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_row_self.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_srs_cell.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_srs_icon_tile.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_srs_mastered_cell.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_srs_new_cell.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_srs_remembering_cell.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_stat.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_streak.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home16_track.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_approved_dot.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_approved_due.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_approved_primary.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_approved_progress.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_approved_quiz.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_approved_self.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_avatar_background.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_error_background.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_footer_background.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_icon_background.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_srs_cell_fresh.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_srs_cell_good.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_srs_cell_mastered.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_srs_cell_practicing.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_srs_color_fresh.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_srs_color_good.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_srs_color_mastered.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_srs_color_practicing.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_srs_due_badge.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_srs_due_row.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/home_stat_background.xml` |
| D | Sửa | `android-app/app/src/main/res/drawable/ic_flashcard_speaker.xml` |
| E | Sửa | `android-app/app/src/main/res/drawable/ic_launcher_background.xml` |
| E | Sửa | `android-app/app/src/main/res/drawable/ic_launcher_foreground.xml` |
| E | Sửa | `android-app/app/src/main/res/drawable/ic_notification_bivocal.xml` |
| E | Sửa | `android-app/app/src/main/res/drawable/ic_settings_chip_checked.xml` |
| E | Sửa | `android-app/app/src/main/res/drawable/ic_settings_chip_unchecked.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_approved_field.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_approved_primary.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_eye.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_eye_off.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_field_background.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_lock.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_moon.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_moon_glow.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_night_background.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_star.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_ui_field.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_ui_primary.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/login_user.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/nav_item_background.xml` |
| D | Sửa | `android-app/app/src/main/res/drawable/phase_d_progress.xml` |
| D | Sửa | `android-app/app/src/main/res/drawable/phase_d_status_check.xml` |
| D | Sửa | `android-app/app/src/main/res/drawable/phase_d_status_error.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/register_field_error.xml` |
| B | Sửa | `android-app/app/src/main/res/drawable/register_mail.xml` |
| E | Sửa | `android-app/app/src/main/res/drawable/settings_card_background.xml` |
| E | Sửa | `android-app/app/src/main/res/drawable/settings_day_chip_background.xml` |
| E | Sửa | `android-app/app/src/main/res/drawable/settings_option_background.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_animal.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_arts.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_book.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_buildings.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_chevron.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_clothing.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_communication.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_filter_frame.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_finance.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_food.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_group_badge.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_health.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_law.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_nature.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_people.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_school.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_science.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_search.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_search_field.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_shopping.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_sports.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_technology.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_transport.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_travel.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_weather.xml` |
| C | Sửa | `android-app/app/src/main/res/drawable/topics16_work.xml` |
| D | Sửa | `android-app/app/src/main/res/layout/activity_flashcard.xml` |
| B | Sửa | `android-app/app/src/main/res/layout/activity_forgot_password.xml` |
| C | Sửa | `android-app/app/src/main/res/layout/activity_home.xml` |
| E | Sửa | `android-app/app/src/main/res/layout/activity_leaderboard.xml` |
| B | Sửa | `android-app/app/src/main/res/layout/activity_login.xml` |
| E | Sửa | `android-app/app/src/main/res/layout/activity_main.xml` |
| E | Sửa | `android-app/app/src/main/res/layout/activity_profile.xml` |
| E | Sửa | `android-app/app/src/main/res/layout/activity_progress.xml` |
| E | Sửa | `android-app/app/src/main/res/layout/activity_quiz.xml` |
| B | Sửa | `android-app/app/src/main/res/layout/activity_register.xml` |
| E | Sửa | `android-app/app/src/main/res/layout/activity_settings.xml` |
| E | Sửa | `android-app/app/src/main/res/layout/activity_settings_detail.xml` |
| C | Sửa | `android-app/app/src/main/res/layout/activity_topics.xml` |
| C | Sửa | `android-app/app/src/main/res/layout/activity_topic_detail.xml` |
| E | Sửa | `android-app/app/src/main/res/layout/activity_vocabulary_search.xml` |
| E | Sửa | `android-app/app/src/main/res/layout/item_leaderboard_user.xml` |
| C | Sửa | `android-app/app/src/main/res/layout/item_topic.xml` |
| C | Sửa | `android-app/app/src/main/res/layout/item_topic_preview.xml` |
| E | Sửa | `android-app/app/src/main/res/layout/item_vocabulary_search.xml` |
| B | Sửa | `android-app/app/src/main/res/layout/view_auth_brand_header.xml` |
| C | Sửa | `android-app/app/src/main/res/layout/view_home_header.xml` |
| C | Sửa | `android-app/app/src/main/res/layout/view_home_srs.xml` |
| C | Sửa | `android-app/app/src/main/res/layout/view_learning_bottom_navigation.xml` |
| C | Sửa | `android-app/app/src/main/res/layout/view_navigation_avatar.xml` |
| C | Sửa | `android-app/app/src/main/res/layout/view_navigation_drawer.xml` |
| C | Sửa | `android-app/app/src/main/res/layout/view_navigation_top_bar.xml` |
| C | Sửa | `android-app/app/src/main/res/layout-w440dp/view_home_header.xml` |
| B | Sửa | `android-app/app/src/main/res/values/auth_ui.xml` |
| A | Sửa | `android-app/app/src/main/res/values/colors.xml` |
| D | Sửa | `android-app/app/src/main/res/values/flashcard_frontend.xml` |
| B | Sửa | `android-app/app/src/main/res/values/forgot_colors.xml` |
| C | Sửa | `android-app/app/src/main/res/values/home_approved.xml` |
| C | Sửa | `android-app/app/src/main/res/values/home_frontend.xml` |
| C | Sửa | `android-app/app/src/main/res/values/home_srs.xml` |
| C | Sửa | `android-app/app/src/main/res/values/home_styles.xml` |
| B | Sửa | `android-app/app/src/main/res/values/login_approved.xml` |
| B | Sửa | `android-app/app/src/main/res/values/login_ui.xml` |
| E | Sửa | `android-app/app/src/main/res/values/settings_styles.xml` |
| A | Sửa | `android-app/app/src/main/res/values/themes.xml` |
| C | Sửa | `android-app/app/src/main/res/values/topics_frontend.xml` |
| C | Sửa | `android-app/app/src/main/res/values/topics_phase_c.xml` |
| A | Sửa | `android-app/app/src/main/res/values-night/themes.xml` |
| B | Sửa | `android-app/app/src/main/res/values-v27/login_theme.xml` |
| C | Sửa | `android-app/app/src/test/java/com/example/bivoca/ui/topics/TopicPresentationTest.java` |
| A | Thêm | `android-app/app/src/androidTest/java/com/example/bivoca/LightColorfulUiTest.java` |
| A | Thêm | `android-app/app/src/main/res/font/be_vietnam_pro.xml` |
| A | Thêm | `android-app/app/src/main/res/font/be_vietnam_pro_bold.ttf` |
| A | Thêm | `android-app/app/src/main/res/font/be_vietnam_pro_extrabold.ttf` |
| A | Thêm | `android-app/app/src/main/res/font/be_vietnam_pro_medium.ttf` |
| A | Thêm | `android-app/app/src/main/res/font/be_vietnam_pro_regular.ttf` |
| A | Thêm | `android-app/app/src/main/res/font/be_vietnam_pro_semibold.ttf` |
| A | Thêm | `android-app/app/src/main/res/raw/be_vietnam_pro_ofl.txt` |
| A | Thêm | `android-app/app/src/main/res/values/dimens.xml` |
| A | Thêm | `android-app/app/src/main/res/values/light_colorful_tokens.xml` |
| A | Thêm | `android-app/app/src/main/res/values/light_window_compat.xml` |
| A | Thêm | `android-app/app/src/main/res/values/styles.xml` |
| A | Thêm | `android-app/app/src/main/res/values-v27/light_window_compat.xml` |
| A | Thêm | `android-app/app/src/main/res/values-v29/light_window_compat.xml` |
| B | Thêm | `android-app/app/src/main/res/drawable/light_auth_banner.xml` |
| C | Thêm | `android-app/app/src/main/res/drawable/light_home_goal_action.xml` |
| C | Thêm | `android-app/app/src/main/res/drawable/light_nav_marker.xml` |
| C | Thêm | `android-app/app/src/main/res/drawable/light_srs_tile.xml` |
| D | Thêm | `android-app/app/src/main/res/drawable/light_example_panel.xml` |
| E | Thêm | `android-app/app/src/androidTest/java/com/example/bivoca/LightColorfulScreensUiTest.java` |
| E | Thêm | `android-app/app/src/main/res/drawable/light_rank_badge.xml` |
| C | Thêm | `android-app/app/src/main/res/layout-land/activity_topics.xml` |

**Dừng tại READY_FOR_ACCEPTANCE.** Người dùng đối chiếu Home/Topics/Flashcard/Auth và ảnh font1.3 với refined demo, quyết định nghiệm thu; lỗi Quiz timeout và kiểm thử ghi cần xử lý/duyệt riêng. CHƯA COMMIT/CHƯA PUSH.
