# M16.5 — Flashcard Frontend

## Cập nhật cuối — thẻ dài hơn và ví dụ trong suốt (28/10/2026)

- `activity_flashcard.xml` là source duy nhất đổi: margin dưới card36→12dp để card dài thêm24dp, giữ cùng vị trí/kích thước nút hai mặt ở80% vùng khả dụng. Gỡ background khung ví dụ, không nền màu hoặc viền, giữ chữ/padding/audio. Khung ghi nhớ vẫn ở dưới; nghiệp vụ không đổi. Hash source khác giữ nguyên.
- Build `:app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest` PASS18s (12 executed/62 up-to-date), lint0 lỗi392warnings; unit **NOT_RUN** cho thay đổi XML-only,84 PASS là lượt trước. `FlashcardFrontendUiTest` **3/3 PASS31.220s**, gồm24 tổ hợp đo size/font, vị trí/kích thước nút giữa hai mặt, ví dụ không bị cắt và luồng API thật. APK/app-test cài-r, không clear data, không đổi AVD/font, không Review/Quiz/Favorite/DB. Runtime font hệ thống1.3 riêng chưa chạy lại; measurement có1.3.
- [Ảnh mặt trước mới nhất](../../android-app/runtime-evidence/m16-5-flashcard/example-transparent-20261028/actual/flashcard-front.png) · [Mặt sau mới nhất](../../android-app/runtime-evidence/m16-5-flashcard/example-transparent-20261028/actual/flashcard-back-review.png). Evidence example-transparent-20261028/ chứa build/instrumentation/summary và before/after/diff hashes. Chờ người dùng duyệt, chưa commit/push; các bản bên dưới là lịch sử.

## Thứ tự nút mới nhất — 28/10/2026

- Theo phản hồi mới: nút Xem mặt trước nằm trên khung Bạn có nhớ từ này không. Card hai mặt cùng chiều cao; nút Lật thẻ/Xem mặt trước cùng tọa độ và kích thước, tâm ở80% vùng khả dụng. Browse giữ chỗ Trước/Tiếp bằng INVISIBLE trên mặt sau; NEW/REVIEW giữ GONE. Thay quy tắc back+24dp trước đó để tránh nút nhảy vị trí; không đổi callback SRS.
- Sửa3 file `activity_flashcard.xml`, `FlashcardActivity.java`, `FlashcardFrontendUiTest.java`; source ngoài phạm vi giữ nguyên theo hash. Build bốn task PASS12s,26 executed/52 up-to-date, unit84 PASS, lint0 lỗi391warnings. Instrumentation **3/3 PASS30.227s**, gồm cùng X/Y/width/height hai nút và panel đánh giá nằm dưới; đo XML24 tổ hợp có font1.3. Runtime font hệ thống1.3 riêng NOT_RUN trong lượt này. App/test cài-r; không Review/Quiz/Favorite, không đổi DB/AVD, font1.0.
- [Ảnh mặt sau mới nhất](../../android-app/runtime-evidence/m16-5-flashcard/actions-order-20261028/actual/flashcard-back-review.png) · [Mặt trước](../../android-app/runtime-evidence/m16-5-flashcard/actions-order-20261028/actual/flashcard-front.png) · [Học từ mới](../../android-app/runtime-evidence/m16-5-flashcard/actions-order-20261028/actual/new-today.png). Evidence actions-order-20261028/ có build/instrumentation/summary/source hashes. Chờ duyệt UI, chưa commit/push; các phần bên dưới là lịch sử trước thay đổi thứ tự này.

## Cập nhật mới nhất — bỏ viền ví dụ và nới cả hai mặt (28/10/2026)

- Khung ví dụ bỏ viền, giữ nền/bo góc. Mặt trước dài hơn bằng cách đưa tâm Flip xuống80% vùng khả dụng (trước75%); mặt sau thêm24dp và tự cân theo đáy ví dụ, cap phần thêm96dp. Ví dụ rất dài vẫn cuộn; không hứa mọi độ dài đều vừa một màn. Giữ header, hai cột metadata, khoảng cách48/36dp và nghiệp vụ SRS.
- Source đổi3 file: `FlashcardActivity.java`, `fc16_example.xml`, `FlashcardFrontendUiTest.java`. Build cuối bốn task app/unit/lint/test APK PASS13s,19 executed/59 up-to-date;84 unit PASS, lint0 lỗi391warnings. Instrumentation **3/3 PASS30.612s**, gồm front80%, ví dụ thực tế không bị card cắt, nút trở lại hiện trọn, lật/navigation/rotation/menu/queues và24 phép đo XML có font1.3. Runtime font hệ thống1.3 riêng chưa chạy lại trên bản này. APK/test APK cài-r, không clear dữ liệu hoặc đổi AVD/font1.0; không gửi rating/Quiz/Favorite.
- Evidence `android-app/runtime-evidence/m16-5-flashcard/example-borderless-20261028/`: build-final.txt, instrumentation-final.txt, summary.json, source-before/after/diff.json. Hash chỉ có3 file UI/test; VM/API/Repository/Backend/Admin/Auth/Home/Topics không đổi. Bản thử back+48dp đã được nhìn thấy cắt mép nút trên ảnh, đã giảm tối thiểu+24dp và thêm assertion bảo vệ trước bàn giao; ảnh/log bản thử được giữ.
- [Mặt chưa lật mới](../../android-app/runtime-evidence/m16-5-flashcard/example-borderless-20261028/actual-final/flashcard-front.png) · [Mặt đã lật mới](../../android-app/runtime-evidence/m16-5-flashcard/example-borderless-20261028/actual-final/flashcard-back-review.png) · [Học từ mới](../../android-app/runtime-evidence/m16-5-flashcard/example-borderless-20261028/actual-final/new-today.png).

Chờ người dùng duyệt UI; chưa commit/push, không tự nâng DONE. Các mô tả75% và ZIP bên dưới là các bản trước.

## UI cập nhật cuối ngày28/10/2026

- Header lớn hơn và đúng tâm: title22sp, vị trí14sp, mode11sp ở hàng dưới; hai bên chừa khoảng đối xứng cho Back/menu. Progress14sp/bar10dp.
- Thẻ trước co theo vùng màn hình khả dụng: tâm nút Lật thẻ khoảng75% từ trên, min280dp, không đổi AVD; mặt sau giữ vùng đọc ví dụ và cuộn được. Khoảng cách trên16→48dp, dưới12→36dp theo yêu cầu gấp3.
- Khung đánh giá chuyển nền#111726/viền#334155 đồng bộ thẻ; giữ hai nút và callback SRS. Nghĩa tiếng Việt cột trái, Loại từ/Thông tin cột phải căn phải, tỉ lệ3:2; tự xuống dòng và ẩn cột metadata khi thiếu. Không thay nghiệp vụ hoặc làm Khám phá chỉ đọc.
- Source chỉ4 file: `FlashcardActivity.java`, `activity_flashcard.xml`, `fc16_review.xml`, `FlashcardFrontendUiTest.java`. SHA-256 trước/sau tại `runtime-evidence/m16-5-flashcard/polish-20261028/`; VM/API/Repository/Backend/Admin/Auth/Home/Topics không đổi. Không gửi POST Review/Quiz/Favorite hoặc sửa DB để nghiệm thu.
- Bản cuối `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` PASS17s (24 executed/54 up-to-date), unit task thực thi:84/84, lint0 lỗi/391 warnings. App/test APK cài bằng install-r, guard keepInstalledApks=true/uninstallIncompatibleApks=false đã xác minh; không clear data.
- `FlashcardFrontendUiTest` bản cuối **3/3 PASS**,30.448s:24 tổ hợp đo XML, header center, tâm Flip75%, API thật/Food/Trước-Tiếp/lật/recreation/landscape/gesture/Drawer/NEW queue/empty due. Log `instrumentation-metadata-right.txt`, build `build-metadata-right.txt`. Lượt margin-only trước đó build31s có unit UP-TO-DATE, không gọi là chạy unit mới.
- Runtime font hệ thống1.3 trước bước metadata-right **1/1 PASS**,55.781s; đã trả1.0. Metadata-right cuối đã đo font1.3 qua resource test, chưa chạy lại riêng runtime font hệ thống1.3. Source cuối vẫn size1080×1920/density420 override370; không đổi AVD. Lỗi test center đầu tiên1/3 FAIL do getLocationInWindow trả vị trí0 trên XML chưa attach; sửa phép đo bằng ancestor offsets, không sửa UI để né kiểm tra. Giữ log FAIL và các lượt PASS sau đó.

[ZIP6 ảnh mới nhất](../exports/M16_5_UI_POLISH_2026-10-28.zip) · [Mặt trước](../exports/M16_5_UI_POLISH_2026-10-28/flashcard-front.png) · [Mặt sau metadata phải](../exports/M16_5_UI_POLISH_2026-10-28/flashcard-back-review.png) · [Học từ mới](../exports/M16_5_UI_POLISH_2026-10-28/new-today.png).

Trạng thái chờ duyệt UI. Không tự nâng DONE, không commit/push. Các kết quả và ảnh bên dưới phản ánh các bản trước, được giữ làm lịch sử.

**Ảnh mới ngày28/10/2026:** [Tải ZIP6 ảnh Emulator gốc + chú thích](../exports/M16_5_SCREENSHOTS_2026-10-28.zip). Luợt capture3/3 PASS29.394s; không sửa source/build/DB và không tạo lượt học để dựng ảnh hoàn thành. Các kết quả triển khai ngày27 bên dưới được giữ nguyên.

Ngày: 2026-10-27. Phạm vi triển khai được người dùng duyệt sau bước phân tích bộ `BiVocal_M16_5_Flashcard_Design_Review.zip`.

## Kết quả triển khai

- Flashcard Java/XML native: header Back/tên/nhãn chế độ, tiến độ thật, hai mặt trong cùng một card, phát âm từ và câu ví dụ, hai nút đánh giá sau khi lật. Palette/radius/type theo bộ thiết kế đã cung cấp.
- Giữ ba luồng khám phá chủ đề, NEW_TODAY và REVIEW_TODAY. Khám phá giữ Trước/Tiếp và phân trang 100 thẻ; Favorite/Quiz/Drawer chuyển vào menu tùy chọn. Phiên học/ôn không có nút bỏ qua đánh giá.
- Giữ request SRS `correct: false/true`. Cả hai kết quả chỉ bỏ thẻ khỏi hàng chờ sau phản hồi hợp lệ từ Backend. Loading/chặn gửi trùng/lỗi giữ thẻ hiện tại tiếp tục dùng ViewModel hiện có.
- Thêm bộ đếm phiên trong ViewModel: tổng hàng chờ ban đầu và số phản hồi thành công. Màn hoàn thành chỉ xuất hiện khi hàng chờ của phiên thực sự hết, không hiện cho danh sách rỗng ngay từ đầu. Không hiển thị XP, thời gian, mức thành thạo giả. Hoàn thành phiên không đồng nghĩa đã thuộc mọi từ.
- Giữ tùy chọn mặt Việt trước, cỡ chữ học, tốc độ/auto TTS và reduce motion. Ví dụ/IPA/POS/CEFR/cảnh báo chưa kiểm duyệt lấy từ dữ liệu thực tế; thiếu thì không tự tạo.
- Card cuộn nội dung; trang ngoài cuộn khi không đủ chiều cao. Không đổi size/density/cấu hình AVD. Không có tab preview, WebView, bốn mức Anki hoặc bottom navigation trong Flashcard.

## File thay đổi

Đường dẫn bên dưới tính từ gốc project; baseline mã băm nằm tại `android-app/runtime-evidence/m16-5-flashcard/source-before.json`.

Sửa:

1. `android-app/app/src/main/java/com/example/bivoca/ui/flashcard/FlashcardActivity.java` — binding/state/menu/TTS và UI hai mặt/hoàn thành.
2. `android-app/app/src/main/java/com/example/bivoca/ui/flashcard/FlashcardViewModel.java` — bộ đếm phiên, không đổi thuật toán SRS/API.
3. `android-app/app/src/main/res/layout/activity_flashcard.xml` — bố cục mới.
4. `android-app/app/src/main/AndroidManifest.xml` — chỉ entry Flashcard dùng palette/status bar của theme hiện có.
5. `android-app/app/src/main/java/com/example/bivoca/ui/navigation/NavigationDrawerController.java` — opt-in `attachSession`, không thêm top bar thứ hai; bỏ qua cặp avatar header không có trên Flashcard. Các route khác giữ nhánh xử lý cũ.
6. `android-app/app/src/test/java/com/example/bivoca/ui/flashcard/DueReviewViewModelTest.java` — xác minh bộ đếm theo acknowledgement, lỗi/retry/trùng/recreation/reload và đánh giá false.

Thêm:

- `android-app/app/src/main/res/layout/view_flashcard_complete.xml`.
- `android-app/app/src/main/res/values/flashcard_frontend.xml`.
- Bảy drawable `android-app/app/src/main/res/drawable/fc16_{back,more,chip,example,review,metric,check}.xml`.
- `android-app/app/src/androidTest/java/com/example/bivoca/FlashcardFrontendUiTest.java`.
- Báo cáo này; cập nhật WORK_STATUS, ANDROID_CONTEXT và mục lục README.

Bộ thiết kế được giải nén tại [docs/design/m16-5-flashcard-review-20261027](../design/m16-5-flashcard-review-20261027/). Không sửa Backend/Admin/Auth/Home/Topics, Repository hoặc API. Không truy vấn/ghi/migration PostgreSQL; không gửi Review/Quiz/Favorite để tạo dữ liệu nghiệm thu.

## Kiểm thử đã chạy

Evidence: [runtime-evidence/m16-5-flashcard](../../android-app/runtime-evidence/m16-5-flashcard/).

| Kiểm tra | Kết quả thực tế |
|---|---|
| `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` | PASS, build source app cuối 25s, 28 task thực thi/50 up-to-date; unit task thực thi. Build đầu24s; lần chỉ thêm test menu9s có unit UP-TO-DATE, không tính là chạy lại unit |
| Android unit | **84/84 PASS**, 0 failure/error/skipped |
| Lint | **0 errors, 393 warnings**; cảnh báo chưa được xử lý rộng ngoài phạm vi |
| Guard instrumentation dry-run | PASS: `keepInstalledApks=true`, `uninstallIncompatibleApks=false`; leaveApksInstalledAfterRun=true giữ nguyên |
| Cài app/test APK bằng `adb install -r` | PASS cả hai, giữ dữ liệu; không uninstall/clear data |
| Đo XML bốn trạng thái | PASS: 24 tổ hợp = 4 trạng thái × 3 viewport (320×640, 412×915, 731×411dp) × font1/1.3; dữ liệu fixture chỉ dùng đo layout |
| Runtime Flashcard API thật | PASS 3/3 trước khi bổ sung kiểm tra cuộn; trên source app cuối, layout + queue PASS, kiểm tra Food/rotation/audio/menu/gesture PASS sau sửa tọa độ test. Xem lịch sử chạy bên dưới |
| Hồi quy Home/Topics | PASS 3/3: Home responsive, Topics responsive và Topics API/filter/navigation/Food/Drawer/landscape |
| Runtime font hệ thống1.3 | PASS1/1, 35.719s: Food front/back/recreation/landscape/gesture/Trước-Tiếp/menu. Font đã trả1.0; không tuyên bố mọi luồng được chạy riêng ở font1.3 |
| Backend `/api/v1/health` | PASS, status ok/database connected, cổng3000; không chạy lại Backend test vì không sửa Backend |

Runtime đã xác minh Food có502 thẻ, bộ nhớ chỉ giữ tối đa100 thẻ của trang hiện tại; front/back/IPA/meaning/example bind đúng model, Trước/Tiếp hoạt động, recreation/landscape giữ mặt đang lật. NEW_TODAY tải10 thẻ và chỉ hiện hai action khi lật; REVIEW_TODAY thực tế rỗng, hiển thị thông báo chưa đến hạn, không hiện màn hoàn thành giả. Menu mở Drawer END và đóng lại được. Có gọi nút TTS từ/câu khi khả dụng; không khẳng định đã nghe và đánh giá chất lượng giọng đọc.

Lịch sử instrumentation (không che kết quả FAIL):

- `instrumentation-first.txt`: **1 PASS / 2 FAIL**, fail tại precondition chưa có phiên đăng nhập; đã mở Login. Sau khi quan sát Emulator vào Home, tiếp tục bằng session thật, không tạo phiên giả.
- `instrumentation-runtime.txt`: **3/3 PASS**, 40.279s, bản trước bổ sung kiểm tra khả năng cuộn tới action landscape.
- `instrumentation-final.txt`: **5 PASS / 1 FAIL**, 84.578s; generic Espresso `scrollTo()` không đưa nút vào viewport. Ba bài hồi quy Home/Topics và hai bài Flashcard layout/queue đã PASS trên source ứng dụng cuối.
- `instrumentation-gesture.txt`: **0 PASS / 1 FAIL**, 21.294s; thao tác swipe ban đầu đặt vào mép vật lý thuộc vùng system inset/cutout, chưa chạm tới action.
- `instrumentation-gesture-insets.txt`: **1/1 PASS**, 35.639s; sửa **test**, không sửa source ứng dụng để né kiểm tra. Swipe thật bắt đầu trong lề24dp của nội dung, tránh inset hệ thống; kiểm tra nút hiển thị toàn bộ, rồi tiếp tục Back/Next/Previous/Drawer. Tổng cộng **6 test case khác nhau có bằng chứng PASS trên source app cuối**, từ lượt5/6 và lượt1/1; không ghi thành một lượt6/6 sạch.
- `instrumentation-font-1.3.txt`: **1/1 PASS** bổ sung trên font hệ thống1.3; font trả1.0 trong `finally`.

Build test cuối sau sửa tọa độ: `:app:assembleDebugAndroidTest :app:lintDebug` PASS10s (8 task thực thi/51 up-to-date). Source ứng dụng/APK chính giữ nguyên so với build25s; chỉ test APK đổi. Mã băm app APK và tổng unit/lint lưu trong `verification-summary.json`.

Lệnh chính, chạy từ `android-app` với JAVA_HOME là JBR Android Studio:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest --dry-run --no-configuration-cache -I runtime-evidence/m16-3-home/final-polish/test-guards.init.gradle
```

Lệnh ADB dùng SDK local, thiết bị `emulator-5554`:

```text
adb devices -l
adb -s emulator-5554 install -r android-app/app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r android-app/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r -e class com.example.bivoca.FlashcardFrontendUiTest com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 shell am start -n com.example.bivoca/.ui.auth.LoginActivity
adb -s emulator-5554 shell am instrument -w -r -e class com.example.bivoca.FlashcardFrontendUiTest,com.example.bivoca.HomeResponsiveUiTest,com.example.bivoca.TopicsUiTest com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 shell am instrument -w -r -e class com.example.bivoca.FlashcardFrontendUiTest#realTopicFrontBackPagingRotationAndAudioControls com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 shell settings put system font_scale 1.3
# Chạy lại method realTopicFrontBackPagingRotationAndAudioControls, rồi khôi phục trong finally:
adb -s emulator-5554 shell settings put system font_scale 1.0
```

## Ảnh Android thật

- [Mặt trước Food](../../android-app/runtime-evidence/m16-5-flashcard/actual-final/flashcard-front.png).
- [Màn đang mở để người dùng duyệt, font đã trả1.0](../../android-app/runtime-evidence/m16-5-flashcard/ready-for-review.png).
- [Mặt sau và hai action](../../android-app/runtime-evidence/m16-5-flashcard/actual-final/flashcard-back-review.png).
- [Học từ mới](../../android-app/runtime-evidence/m16-5-flashcard/actual-final/new-today.png).
- [Ôn hôm nay — hàng chờ rỗng](../../android-app/runtime-evidence/m16-5-flashcard/actual-final/review-today.png).
- [Landscape phần trên](../../android-app/runtime-evidence/m16-5-flashcard/actual-final/flashcard-landscape.png) và [cuộn đến các nút](../../android-app/runtime-evidence/m16-5-flashcard/actual-final/flashcard-landscape-actions.png).
- Font1.3: [mặt trước](../../android-app/runtime-evidence/m16-5-flashcard/actual-font-1.3/flashcard-front.png), [mặt sau](../../android-app/runtime-evidence/m16-5-flashcard/actual-font-1.3/flashcard-back-review.png), [landscape cuộn tới action](../../android-app/runtime-evidence/m16-5-flashcard/actual-font-1.3/flashcard-landscape-actions.png).

AVD giữ size1080×1920, density420/override370 như trước. `config.ini` SHA-256 so trước/sau giữ nguyên; không đổi độ phân giải/density để làm đẹp ảnh. Font trả1.0 sau test.

Kiểm tra bàn giao tài liệu: UTF-8 đọc được, các liên kết nội bộ README/báo cáo tồn tại. Hash source cuối:6 sửa/10 mới/0 xóa; không có thay đổi trong Backend/Admin/Auth/Home/Topics/data API/Repository. Bản APK chính cuối giữ đúng SHA-256 trong verification-summary; chỉ test APK thay đổi khi sửa helper tọa độ. Đã mở Food trên Emulator bằng Topics/Search để người dùng xem trực tiếp, không gửi rating.

Đã xem ảnh Android thật và đối chiếu với PNG/HTML source trong bộ thiết kế. Không tuyên bố pixel-perfect: dữ liệu/độ dài thực tế khác demo, có cảnh báo nguồn/CEFR, menu tác vụ cũ và dòng trạng thái TTS; NEW_TODAY giữ hai đánh giá SRS thật. Browser chặn local HTML trước đó nên không tuyên bố đã render lại HTML trong browser; dùng source và ảnh demo được cung cấp.

## Giới hạn và nghiệm thu còn chờ

- REVIEW_TODAY có thẻ đến hạn: **NOT_RUN** vì hàng chờ tài khoản đang dùng rỗng; layout và common queue acknowledgement được kiểm tra bằng instrumentation đo XML và unit tests.
- Hàng chờ hoàn thành sau POST thật: **NOT_RUN** trên tài khoản production để tránh tạo lượt học. Unit test xác minh acknowledgement/completion; đo layout không thay thế nghiệm thu E2E có ghi.
- Chưa nghe trực tiếp chất lượng giọng đọc TTS; có tái sử dụng engine/ngôn ngữ/tốc độ và thêm action đọc câu ví dụ.
- Bộ đếm giữ khi Activity recreation trong cùng ViewModel, không phục hồi phiên cũ sau process death. Tải lại queue sẽ bắt đầu bộ đếm phiên mới; không ghép số cũ vào hàng chờ mới.
- Khác demo có chủ đích: dùng hai nút SRS thật ở NEW_TODAY thay nút “Đã hiểu” tự gán kết quả; thông số/metadata/độ dài card phụ thuộc dữ liệu API và thiết bị; menu giữ Favorite/Quiz/Drawer thật. Chưa tuyên bố giống thiết kế 100%.
- Chưa có Git repository tại thư mục làm việc: không xác định được nhánh/diff/lịch sử; dùng SHA-256 kiểm tra phạm vi. **CHƯA COMMIT / CHƯA PUSH**.

Trạng thái: **READY_FOR_ACCEPTANCE — chờ người dùng duyệt UI; các giới hạn E2E có ghi nêu rõ phía trên**. Không tự chuyển M16.6 hoặc đổi trạng thái nghiệm thu các milestone trước.
