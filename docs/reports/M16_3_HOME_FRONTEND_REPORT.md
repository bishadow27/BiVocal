# M16.3 — BiVocal Home Frontend

## Final UI Polish — 27/10/2026

Theo phạm vi đã duyệt, tăng slogan 12sp/tối đa hai dòng với header responsive; SRS nền #111726/viền #334155/radius 20dp, tiêu đề 16sp, Info vùng chạm 48dp và CTA “Ôn N từ đến hạn”; leaderboard đổi tên XP. Giữ bố cục mới/bốn tab và logic API. Build/unit/lint cuối PASS: 81/81 unit, 0 lỗi/398 cảnh báo lint; instrumentation cuối 2/2 mặc định + 1/1 font 1.3 PASS. AVD size/density/config không đổi, font trả 1.0, APK cài -r giữ phiên. Chi tiết file, lệnh, giới hạn và ảnh: [báo cáo Final Polish](M16_3_HOME_FINAL_POLISH_REPORT.md). Bố cục được người dùng duyệt, bản polish vẫn READY_FOR_ACCEPTANCE/chờ duyệt UI cuối.

## Bỏ footer động viên — 27/10/2026

Theo phản hồi kèm ảnh mới nhất, đã bỏ card mascot/slogan ở cuối nội dung Home. Header mới vẫn giữ logo/slogan; avatar phải mở Drawer và bốn tab không đổi. Xóa block footer trong activity_home.xml, binding homeMotivationMascot và cập nhật kiểm tra phần tử cuối trong HomeResponsiveUiTest sang leaderboard. Build assembleDebug/assembleDebugAndroidTest PASS (7 giây); APK chính cài -r giữ dữ liệu. Unit/lint/instrumentation NOT_RUN ở lượt xóa UI này. Không sửa Backend/database, chưa commit/push. Các mô tả có footer ở dưới là lịch sử đã được yêu cầu mới thay thế.

## Header thương hiệu và avatar mở Drawer — 27/10/2026

Theo hai ảnh phản hồi mới: avatar/lời chào bên trái được thay bằng mascot gốc + slogan “Mỗi ngày 15 phút, bứt phá vốn từ cùng BiVocal!” (BiVocal cyan/bold). Bên phải giữ streak, thay nút hamburger bằng avatar tròn của tài khoản. Bấm avatar vẫn mở Drawer bên phải như menu cũ; không mở Profile trực tiếp. Hồ sơ/Cài đặt vẫn trong Drawer; footer và bốn tab không đổi.

Chỉ sửa activity_home.xml, phần bind/render HomeActivity và hai UI test đang tham chiếu textWelcome/buttonHomeProfile. Controller không sửa và SHA-256 giữ nguyên; reuse imageHomeAvatar/textHomeAvatar và cùng bitmap crop mascot gốc. Header min-height 64dp, slogan wrap theo chiều rộng và tăng chiều cao khi cần; không hardcode Y/đổi AVD.

PASS: assembleDebug, lintDebug, assembleDebugAndroidTest (1m35s, tác vụ chạy thật; 0 lỗi/397 cảnh báo lint toàn dự án). Chỉ cài APK chính install -r, giữ dữ liệu. Runtime đã xác nhận mascot/slogan trái, avatar phải, click mở đúng Drawer END và có mục Hồ sơ/Cài đặt, Back đóng về Home. Ảnh/XML tại runtime-evidence/m16-3-home/header-brand-avatar/. Unit và instrumentation thực thi NOT_RUN ở lượt này; test APK chỉ biên dịch, không cài lại biến thể thử nghiệm đã gỡ. Không chỉnh nghiệp vụ/API/Backend/database; chưa commit/push vì không có Git repository được xác định. M16.3 vẫn chờ người dùng duyệt giao diện.

Các mô tả header avatar trái/lời chào và menu hamburger trong phần lịch sử bên dưới đã được thay thế bởi yêu cầu mới này.

## Bottom navigation bốn mục — 27/10/2026

Theo phản hồi mới, bỏ tab Hồ sơ; thanh dưới chỉ còn Trang chủ / Chủ đề / Ôn tập / Trắc nghiệm chia đều. Avatar và Drawer vẫn mở Profile. Build assembleDebug + assembleDebugAndroidTest PASS (16 giây); chỉ cài APK chính install -r, giữ dữ liệu và không cài lại gói E2E/test đã dọn. Unit/lint/instrumentation NOT_RUN cho thay đổi nhỏ này. Sửa XML Home, binding Java và bỏ tham chiếu tab cũ trong runtime test. Nội dung năm tab ở các mục trước là lịch sử; M16.3 vẫn chờ duyệt giao diện.

## Card SRS theo ảnh mới — 26/10/2026

Đã đổi riêng card tiến độ thành “Độ thuộc từ vựng (SRS)”: nền charcoal/viền xám, tổng từ màu cam, phần trăm thuần thục, thanh bốn màu, legend 2×2 và hàng ôn đến hạn với pill xanh. Link Chi tiết vẫn mở ProgressActivity; nút ? giải thích cách nhóm; CTA delegate luồng Review hiện có. Quick Stats vẫn được bỏ theo yêu cầu trước.

- Biết mới (xanh dương): mức 0–1; Đang ôn (tím): mức 2–3; Nhớ tốt (xanh lá): mức 4; Thuần thục (cam): mức 5.
- Chỉ ánh xạ các bucket memoryLevels do progress/schedule trả về, vốn chỉ đếm review_count>0. Tổng bốn nhóm phải bằng reviewedCards; favorite-only không được cộng vào.
- Phần trăm = số thẻ mức 5 / tổng thẻ đã ôn, làm tròn số nguyên; không phải accuracy của các lượt trả lời. Tổng bằng 0 thì 0%; API chưa có/không hợp lệ thì —.
- Số cần ôn dùng dueNow thật. dueNow=0 hiển thị “Chưa có từ đến hạn”/0 từ và khóa CTA; không điền số 307 từ trong ảnh mẫu. Khi dueNow>0, CTA mở EXTRA_REVIEW_TODAY qua hành động Review đang có. Không thay lịch/SRS/XP/Quiz hoặc dữ liệu PostgreSQL.
- Thanh dùng trọng số số thẻ thực của từng nhóm; nhóm 0 ẩn segment, tổng 0 để thanh rỗng. Không ép vẽ đủ bốn màu khi chưa có dữ liệu.

File: thêm res/layout/view_home_srs.xml, res/values/home_srs.xml và 10 drawable home_srs_* cho nền/màu; sửa activity_home.xml, HomeActivity.java, HomePresentation.java, HomePresentationTest.java. Không sửa ViewModel/Repository/API/Backend, các màn khác hoặc điều hướng đã nghiệm thu.

Kiểm thử: assembleDebug/testDebugUnitTest/lintDebug/assembleDebugAndroidTest PASS; **81/81 unit**, lint **0 errors/391 warnings**. HomeResponsiveUiTest **1/1 PASS** (8 tổ hợp kích thước/font, gồm 320×640dp và scale 1.3). APK app/test cài -r, không clear data. Runtime đã mở card và help dialog thật; tài khoản hiện tại Phúc Duy có 0 thẻ đã ôn/0 từ đến hạn nên cả bốn ô là 0, CTA khóa đúng. Chưa nhấn thử CTA ở tài khoản có due>0 trong lượt này; không tạo lượt học để dựng số liệu.

Ảnh và UI dump mới: android-app/runtime-evidence/m16-3-home/srs-four-groups/home-srs.png, home-srs.xml, help.xml. Những mô tả/ảnh card ba nhóm và accuracy ở các phần lịch sử bên dưới đã được thay thế bởi mục này. Không đổi size/density Emulator.

## Cập nhật theo phản hồi — 26/10/2026

Bỏ hoàn toàn hàng ba ô Quick Stats khỏi Home cùng binding Java; Daily Goal đi thẳng tới card SRS với khoảng cách 16dp. Giữ badge streak trên header, Profile và dữ liệu XP/học tập. HomeRuntimeUiTest chuyển kiểm tra số liệu sang badge streak còn hiển thị. Các mô tả và ảnh Quick Stats bên dưới là lịch sử trước thay đổi này.

assembleDebug và assembleDebugAndroidTest PASS (1m13s); APK cài install -r thành công, không clear data. Không chạy lại bộ unit/instrumentation cho thao tác bỏ khối UI này; các số test ở phần lịch sử bên dưới thuộc lượt refinement trước.

Hành vi leaderboard hiện tại: Home preview hai người đứng đầu và user hiện tại nếu có trong top 50, không lặp user, tối đa ba hàng. Màn “Tất cả” dùng RecyclerView cuộn tối đa 50 người có total_xp>0. Chưa có phân trang hoặc endpoint lấy hạng riêng của người ngoài top 50. Không sửa cơ chế này trong lượt bỏ Quick Stats.

## Visual Fidelity Refinement — 25/10/2026

Trạng thái: **đã tinh chỉnh và kiểm thử, chờ người dùng duyệt trực quan**. Không tuyên bố khớp 100%.

### Đối chiếu trước khi sửa

Đã mở lại HTML, chụp Home đang chạy và tạo `refinement/home-demo-before-comparison.png` trước khi sửa. So sánh bằng ảnh thực tế, không dùng unit test làm bằng chứng hình thức.

Ảnh `refinement/home-before-after-comparison.png` gồm ba cột Demo / Android trước / Android sau; có hai vùng đối chiếu: phần trên và leaderboard/footer/tab. Ảnh chỉ cắt frame trình duyệt/system bars và co theo cùng chiều rộng, giữ tỉ lệ; không sửa nội dung, không bơm số demo vào app. Bản demo và Emulator có chiều rộng logic/vị trí cuộn khác nhau, nên không coi đây là phép đo pixel tuyệt đối. Script tái tạo: `refinement/render-comparison.cjs`.

### Sai lệch đã sửa

| Khu vực | Trước | Sau |
|---|---|---|
| Header | Avatar viền dày, greeting 12sp, menu chỉ có nét | Avatar gradient không viền, greeting 11sp, khoảng cách avatar/tên sát HTML; menu có khung 36dp bo 12dp trong target chạm 48dp |
| Daily Goal | Badge chữ thường, dòng trạng thái không nhấn màu, thiếu icon CTA | Badge đậm; phần “N từ nữa” cyan/bold; CTA có tia sét và căn giữa nhóm icon/text; line box số tiến độ 42dp, badge tối thiểu 22dp gần line-height HTML |
| Stats | Số 20sp, nhãn 12sp | Số 17sp, nhãn 11sp theo HTML; ba cột cân giữa, min-height 100dp, padding 12dp/gap 10dp/radius 16dp |
| SRS | Emoji trần, ô viền cùng màu, số 20sp | Tile icon 32dp, viền ô xanh lá/vàng/cyan; label 11sp, số 18sp, mức 10sp, ô min-height 80dp; accuracy có badge xanh |
| Leaderboard | Cúp không có tile, hạng chỉ là số, avatar còn viền cũ | Tile cúp tím, huy chương theo rank thật của người khác, viền theo hạng; tên user hiện tại cyan, avatar gradient; meta 10sp. Rút gọn subtitle nhưng vẫn ghi rõ tổng XP/chưa có tuần |
| Mascot | Card radius 16dp, chữ BiVocal chưa nhấn | Radius 18dp, border #1E293B, mascot gốc 44dp, BiVocal cyan/bold |
| Bottom tabs | Review là mũi tên, Quiz là tia sét | SVG clock/check-circle đúng HTML, padding ngang 16dp; 5 tab và điều hướng giữ nguyên |

Bỏ includeFontPadding ở text styles riêng Home để baseline sát trình duyệt hơn. Giữ Roboto có sẵn, không thêm font tải ngoài/giấy phép mới. Padding nội dung 24dp, card gap 16dp, radius chính 20dp, nút 52dp và gradient giữ nguyên.

### Những khác biệt còn lại

- HTML dùng Inter, Android dùng Roboto; độ rộng chữ, line-height, anti-aliasing và emoji hệ điều hành còn khác. Card/header không thể coi là giống pixel tuyệt đối.
- Emulator vẫn **1080×1920, override 370dpi đã có từ trước**; không đổi size/density trong lượt refinement. Bản HTML có frame 412px với border 7px và vùng trong khoảng 398px. Khi chuẩn hóa toàn chiều rộng, các giá trị dp/sp cố định trên Emulator rộng hơn sẽ trông nhỏ hơn demo.
- Backend thực trả hai người có XP. Giữ logic preview top 2 + user hiện tại, không lặp user hoặc dựng hàng thứ ba giả. Layout hỗ trợ ba hàng khi user hiện tại nằm ngoài top 2; ảnh production lần này có hai hàng.
- Không giả số 7/10, 1.250 XP, ba màu SRS theo tỉ lệ mẫu hoặc giải đấu tuần. Tài khoản thật có 0/10, 30 XP, streak 1, 9 thẻ đã ôn và 27,3% accuracy. SRS thực đang dồn vào mức 0–1 nên thanh chỉ hiện cyan; đó là dữ liệu thật.
- Phần mức SRS và mô tả leaderboard tiếp tục khác demo để đúng hợp đồng Backend. Không thêm “+1 từ” trên CTA vì bấm nút chỉ mở luồng học.

### File thay đổi trong lượt refinement

- res/layout/activity_home.xml, item_home_leaderboard.xml.
- res/values/home_frontend.xml.
- ui/home/HomeActivity.java: chỉ thêm spans/màu của phần trình bày, medal/border theo rank; không đổi API, điều hướng hoặc thuật toán.
- res/drawable/home16_icon_review.xml, home16_icon_quiz.xml.
- Thêm home16_avatar.xml, home16_menu_button.xml, home16_icon_bolt.xml, home16_srs_icon_tile.xml, home16_cup_icon_tile.xml, home16_motivation.xml, home16_srs_mastered_cell.xml, home16_srs_remembering_cell.xml, home16_srs_new_cell.xml, home16_rank_gold.xml, home16_rank_silver.xml.
- HomeRuntimeUiTest.java: chờ Home có window focus/layout ổn định và transition trước thao tác kế tiếp; chỉ sửa đồng bộ harness.
- Tài liệu và evidence refinement. Không sửa HomePresentation, NavigationDrawerController, Auth, SessionManager, các ViewModel/Repository, Flashcard/Quiz/Topics hoặc Backend/DB.

### Nghiệm thu refinement

- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest`: PASS.
- Unit **81/81**, 0 failures; lint **0 errors/380 warnings** toàn dự án.
- HomeResponsiveUiTest + HomeRuntimeUiTest: **2/2 PASS**, 46.956 giây, không skip. Log tại refinement/instrumentation.txt.
- Responsive 8 tổ hợp 320×640 / 411×731 / 412×915 / 731×411dp × font 1.0/1.3 PASS; đây là kiểm tra clipping/bottom nav, không phải kết luận giống demo.
- Vòng runtime đầu vướng thời điểm click sau animation khi quay lại Home; đã chỉnh thời gian chờ/window focus trong test harness, không đổi route production. Lần nghiệm thu cuối PASS.
- APK cài `adb install -r`; giữ phiên và dữ liệu. Chỉ đọc API/mở màn hình, không gửi đáp án hoặc ghi lượt học.
- Ảnh mới tại refinement/captured/: home-top.png, home-srs.png, home-leaderboard.png, home-bottom-navigation.png, home-drawer-open.png; ảnh so sánh và ảnh trước/sau nằm trực tiếp trong refinement/.
- Không chạy SQL/migration/import hoặc thay nội dung dữ liệu trong lượt này.

Dừng ở M16.3 để người dùng duyệt ảnh so sánh.



Ngày: 25/10/2026. Trạng thái: **FRONTEND_TECHNICAL_PASS — chờ người dùng duyệt giao diện**.
APK Debug đã cài bằng `adb install -r`; phiên đăng nhập và dữ liệu ứng dụng giữ nguyên. Không triển khai M16.4.

## Thiết kế tham chiếu và kết quả

Dùng đúng `C:\Users\phuc\Downloads\home_demo.html` và hai ảnh mới người dùng gửi (đã sao chép vào `android-app/runtime-evidence/m16-3-home/reference/`). Gói ZIP Home/Figma cũ trong Downloads có bố cục khác, không được dùng làm thiết kế chính.

Home native Java/XML, không WebView. Thứ tự: top bar → Daily Goal → ba ô XP/streak/từ đã học → SRS → Leaderboard → mascot. Thanh 5 tab cố định bên dưới; nội dung cuộn riêng. Không còn các card shortcut Topics/Quiz/Review trùng với tab.

- Avatar trái mở Profile thật; tên từ tài khoản hiện hành, có ảnh local theo user hoặc chữ cái đầu.
- Menu phải mở chính NavigationDrawerController cũ từ cạnh END. Không có nút Back trên Home; các màn khác vẫn dùng top bar cũ.
- Palette #090D16/#111726/#334155, cyan #38BDF8, blue #2563EB. Card 20dp, stats 16dp; nút gradient 52dp/radius 14dp, padding ngang 24dp, khoảng cách card 16dp.
- Daily Goal: padding 20dp, phần trăm thật, số đã học 28sp, phần mục tiêu 16sp, thanh tiến độ 10dp.
- Cả nút học và các tab chỉ điều hướng; không tự tăng XP/từ đã học.
- Mascot crop từ asset gốc bivocal_wordmark.png như Login; không dùng ảnh bị lỗi trong HTML.
- Font Roboto hệ thống và emoji Android có hình dạng khác trình duyệt. Menu/tabs dùng vector native. Kích thước chạm menu/avatar 48dp, lớn hơn icon minh họa để dễ thao tác.

## Audit components và hợp đồng dữ liệu

Base Debug Emulator: `http://10.0.2.2:3000/api/v1/`. Các request dưới đây đều GET có JWT từ cơ chế hiện hành; không ghi token vào log/report.

| Thành phần | API / nguồn hiện có | Quy tắc hiển thị |
|---|---|---|
| Tên tài khoản | auth/me; ProfileViewModel | fullName, rồi username; fallback SessionManager |
| Avatar | LocalAvatarStore theo user ID | Ảnh local hoặc chữ đầu; không tự tải avatar URL chưa có cơ chế |
| XP/streak | gamification/me | totalXp/currentStreak thực |
| Daily Goal | learning/today | newWordsLearned/dailyGoal/remaining/completionPercent/completed |
| Từ đã học | progress/summary | reviewedCards; loại favorite-only khỏi số từ đã học |
| SRS | progress/schedule | memoryLevels[0..5], reviewedCards, dueNow |
| Độ chính xác | progress/summary | accuracy của các lượt ôn; null → “Chưa có dữ liệu” |
| Leaderboard | gamification/leaderboard?limit=50 | rank/userId/totalXp/streak/level từ Backend |
| Các tab | Activity hiện có | Topics, Flashcard REVIEW_TODAY, Quiz, Profile |
| Nút học | Flashcard NEW_TODAY | Học lượng còn lại; hoàn thành mục tiêu thì đổi nhãn “Học thêm từ mới” |
| Chi tiết/Tất cả | ProgressActivity/LeaderboardActivity | Điều hướng thật |

Home tiếp tục tái sử dụng ProfileViewModel, ProgressViewModel và thêm binding tới LeaderboardViewModel hiện có. Không tạo Home Repository/ViewModel trùng chức năng. ProgressViewModel hiện còn gọi learning/history để phục vụ màn Progress; không đổi hợp đồng đó. Home không tải catalog 20.000 thẻ; leaderboard giới hạn 50, render tối đa top 2 và người hiện tại nếu có trong kết quả, không nhân đôi nếu user đã ở top 2.

### SRS và các khác biệt nghiệp vụ bắt buộc so với số minh họa

Backend hiện định nghĩa masteredCards bằng memory_level=5, không phải mức 4–5 trong HTML.
Ba nhóm native:
- Thành thạo: mức 5.
- Đang nhớ: mức 2–4.
- Mới học: mức 0–1, chỉ tính thẻ đã có review_count>0.

Tổng bằng reviewedCards của schedule. Nếu mảng thiếu/âm hoặc tổng không khớp, hiển thị chưa có dữ liệu, không tự bù số. Thanh ba màu lấy trọng số từ đúng ba tổng; không có review thì thanh rỗng. trackedCards có thể bao gồm thẻ chỉ yêu thích, nên không dùng để làm tổng “Từ đã học”.

Backend có leaderboard **tổng XP**, chưa có API theo tuần, giải đấu Kim Cương, thăng hạng hoặc khoảng cách tới top 3 theo tuần. Giữ card và tiêu đề thiết kế, nhưng subtitle ghi rõ **“Hiện xếp theo tổng XP · chưa hỗ trợ theo tuần”**. Không giả giải đấu/rank/XP. Nếu người hiện tại không có trong top trả về, ghi rõ chưa có trong danh sách; không tự dựng rank của họ. Chưa triển khai Backend weekly leaderboard trong M16.3.

## Loading, empty và error

- Chưa nhận số liệu: “—”/loading, không dùng số mẫu.
- Goal chưa có hoặc đang lỗi: không cho mở học bằng mục tiêu mặc định giả.
- Khi completed=true: thông báo hoàn thành, đổi nhãn học thêm; không tự ghi nhận học.
- Không có SRS/accuracy: trạng thái trống rõ ràng.
- Leaderboard rỗng: “Bảng xếp hạng đang được cập nhật”; lỗi API hiện thông báo và có thể chạm để tải lại.
- Lỗi các API Home hiện error container và nút Thử lại, gọi lại các ViewModel. Khi quay lại Home refresh API, ViewModel/ScrollView giữ trạng thái theo vòng đời.
- Không ép lỗi mạng bằng cách tắt Backend production trong lần nghiệm thu này; error/empty/completed UI được triển khai nhưng chưa có runtime riêng cho tất cả nhánh.

## File source thêm/chỉnh sửa

Chỉnh:
1. android-app/app/src/main/java/com/example/bivoca/ui/home/HomeActivity.java
2. android-app/app/src/main/java/com/example/bivoca/ui/navigation/NavigationDrawerController.java — nhánh HOME dùng header native của Home; nhánh các màn khác giữ cách cũ.
3. android-app/app/src/main/res/layout/activity_home.xml
4. android-app/app/src/main/AndroidManifest.xml — theme riêng Home.

Thêm:
- ui/home/HomePresentation.java — chia nhóm SRS và chọn preview leaderboard, không sửa thuật toán Backend.
- res/layout/item_home_leaderboard.xml
- res/values/home_frontend.xml
- res/drawable/home16_badge.xml, home16_daily_progress.xml, home16_primary.xml
- res/drawable/home16_row.xml, home16_row_self.xml, home16_srs_cell.xml, home16_stat.xml, home16_streak.xml, home16_track.xml
- res/drawable/home16_icon_home.xml, home16_icon_topics.xml, home16_icon_review.xml, home16_icon_quiz.xml, home16_icon_profile.xml, home16_icon_menu.xml
- src/test/java/com/example/bivoca/ui/home/HomePresentationTest.java
- src/androidTest/java/com/example/bivoca/HomeResponsiveUiTest.java
- src/androidTest/java/com/example/bivoca/HomeRuntimeUiTest.java

Tài liệu: báo cáo này, WORK_STATUS.md, ANDROID_CONTEXT.md, README.md. Thêm evidence trong runtime-evidence/m16-3-home.

Không sửa Login/Register/Forgot, JWT/SessionManager, Google Sign-In, Repository/API/Backend, schema/migration, mapping M15.7, XP/SRS/Daily Learning/Quiz scoring hoặc Admin.

## Build và kiểm thử

Lệnh trong android-app, JAVA_HOME dùng JBR của Android Studio:
```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

- assembleDebug PASS.
- Unit **81/81 PASS**, 0 failures (76 cũ + 5 HomePresentation).
- lintDebug PASS, 0 errors / 377 warnings tổng dự án; chi tiết trong lint-results-debug.xml.
- assembleDebugAndroidTest PASS.
- Cài cả app/test APK bằng adb install -r; không uninstall/clear data.
- Lệnh instrumentation:
```powershell
adb -s emulator-5554 shell am instrument -w -r -e class com.example.bivoca.HomeResponsiveUiTest,com.example.bivoca.HomeRuntimeUiTest com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner
```
Kết quả cuối **2/2 PASS**, không skip, 42.075 giây; log instrumentation.txt.

Responsive: inflate/measure layout Android thật ở 320×640, 411×731, 412×915, 731×411dp, mỗi size với font 1.0 và 1.3 (**8 tổ hợp PASS**). Kiểm tra text không cắt chiều cao, bottom tabs không tràn/không di chuyển khi nội dung cuộn, footer tới được. Phát hiện và sửa stats/SRS cell bị ép chiều cao trong vòng kiểm thử đầu; bản cuối dùng wrap_content.

Runtime: Emulator API 35 native 1080×1920; density override **370 đã có từ lượt chỉnh auth trước M16.3**, không thay resolution/density để làm Home vừa ảnh. Ngoài kiểm tra layout 411×731/412×915, đã chụp runtime font 1.3 dọc và landscape 1920×1080. Font/rotation tạm thời đã trả về 1.0, user_rotation=0, accelerometer_rotation=1. Chưa chạy một AVD native 1080×2400 riêng.

Runtime test dùng phiên thật do người dùng tự đăng nhập:
- Daily Goal 0/10, 0%; XP 30, streak 1.
- 9 từ đã review; SRS 0 thành thạo / 0 đang nhớ / 9 mới học; accuracy 27.3%.
- Leaderboard thực có user hiện tại #1/30 XP và tài khoản kế tiếp #2/10 XP; user hiện tại được highlight.
- Đối chiếu XP, số từ học, tổng phân bố SRS với đối tượng API do ViewModel thật nhận được.
- Topics, Review mode, Quiz, bottom Profile, avatar Profile, SRS Chi tiết, leaderboard Tất cả, Daily New Learning đều mở đúng Activity; mode extras đã kiểm tra.
- Drawer mở cạnh phải và đóng được; Home không có shared top bar thứ hai.
- Bottom tabs cố định, nội dung cuộn độc lập.
- Không chọn đáp án, không nhấn Đã nhớ/Chưa nhớ, không thay favorite/goal hoặc liên kết tài khoản.
- Crash buffer trống; lastanr báo không có ANR từ khi boot. Giới hạn nhận định trong các thao tác đã thực hiện.

## Bằng chứng và toàn vẹn dữ liệu

Thư mục `android-app/runtime-evidence/m16-3-home/`:
- home-top.png
- home-srs.png
- home-leaderboard.png
- home-bottom-navigation.png
- home-drawer-open.png
- home-font-130.png
- home-landscape-font-130.png
- instrumentation.txt, db-before-runtime.json, db-after-runtime.json, db-integrity.json, crash-buffer.txt.

Ảnh là screenshot Android chạy thật, không ghép dữ liệu demo. Khi một card đã nằm trọn trong viewport, scrollTo có thể không đổi vị trí giữa hai ảnh.

Snapshot DB trước/sau **nghiệm thu runtime** được lấy bằng transaction READ ONLY, chỉ lưu count/checksum MD5 từng bảng, không xuất record người dùng/secret:
- 23 bảng kiểm tra, **0 bảng đổi checksum**.
- Flashcards **20.000**, topics **38** (36 nhóm có dữ liệu theo M15.7).
- Bao gồm users/Google identities, progress, XP/scores, quiz, Daily Learning, nguồn và nội dung flashcards.
- Không chạy migration, import, INSERT/UPDATE/DELETE hoặc tạo tài khoản test. Không sửa dữ liệu để làm số liệu đẹp như demo.

## Giới hạn bàn giao

Frontend và tích hợp các API hiện có đã PASS, chờ duyệt hình ảnh. Weekly leaderboard/league chưa được Backend hỗ trợ; không tuyên bố tính năng tuần hoàn tất. Chưa runtime riêng các trạng thái completed goal, leaderboard rỗng, Backend down/retry trong M16.3 vì không sửa dữ liệu hoặc dừng Backend production để dựng tình huống. Native AVD 2400 chưa chạy riêng; có đo layout 412×915dp.

Dừng ở M16.3.

