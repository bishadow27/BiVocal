# M12.1 SETTINGS UI/UX POLISH, TTS ACCEPTANCE VÀ REGRESSION REPORT

Ngày nghiệm thu: **2026-10-17**  
Thiết bị: **BiVoca_API35_Pixel6**, Android API 35, serial `emulator-5554`  
Phạm vi: Android Java/XML. Backend, PostgreSQL, Admin Web, dataset và nghiệp vụ M1–M10 không thay đổi.

## 1. Trạng thái trước chỉnh sửa

M12 đã có đủ sáu nhóm Settings, lưu cấu hình theo người dùng, Reminder, Room cache statistics và áp dụng cấu hình vào Flashcard. Bộ ảnh baseline nằm trong `android-app/app/build/reports/m12-settings/`. Chức năng đã hoạt động nhưng các control native còn màu tím, nhãn radio cách xa nút, danh sách ngày nhắc quá dài, TimePicker sáng/tím và nút Previous/Next thiếu tương phản.

Baseline kỹ thuật được xác nhận từ source và báo cáo M12: 89 unit tests, lint 0 error/268 warning, Room instrumentation 6/6 và Reminder instrumentation 1/1.

## 2. Các vấn đề UI xác định

- RadioButton, CheckBox, MaterialSwitch và action của TimePicker chưa theo cyan/blue BiVocal.
- Radio và nhãn tách thành hai thành phần khiến hàng lựa chọn rời rạc.
- Bảy ngày nhắc xếp dọc, chiếm nhiều chiều cao.
- Selected/unselected day chỉ dựa nhiều vào màu.
- Các dòng thống kê Room khó quét nhanh.
- Previous bị mờ quá mức khi disabled; Next dùng màu tím lệch hệ màu.
- State pressed/focused/disabled chưa được mô tả nhất quán bằng color state list.

## 3. Các vấn đề đã sửa

- Tạo bộ color state list dùng chung cho control, text, switch, action button và Flashcard navigation.
- Chuyển radio thành hàng đầy đủ có thể nhấn, nhãn nằm sát indicator, chiều cao và khoảng cách đồng nhất.
- Chuyển ngày nhắc thành hai hàng chip `T2–T5` và `T6–CN`.
- Thêm dấu check/outline để trạng thái ngày không phụ thuộc riêng vào màu.
- Dùng dark TimePicker theme, định dạng 24 giờ và action Hủy/OK màu cyan.
- Căn nhãn/giá trị Room theo từng dòng.
- Làm rõ các state enabled, disabled và pressed của Previous/Next.
- Giữ nguyên Activity, SettingsPreferenceManager, ReminderScheduler, Room và nghiệp vụ Flashcard.

## 4. File mới

- `android-app/app/src/main/res/color/settings_control_tint.xml`
- `android-app/app/src/main/res/color/settings_option_text.xml`
- `android-app/app/src/main/res/color/settings_switch_thumb.xml`
- `android-app/app/src/main/res/color/settings_switch_track.xml`
- `android-app/app/src/main/res/color/settings_day_chip_text.xml`
- `android-app/app/src/main/res/color/settings_action_text.xml`
- `android-app/app/src/main/res/color/settings_action_background.xml`
- `android-app/app/src/main/res/color/settings_action_stroke.xml`
- `android-app/app/src/main/res/color/flashcard_nav_text.xml`
- `android-app/app/src/main/res/color/flashcard_nav_background.xml`
- `android-app/app/src/main/res/color/flashcard_nav_stroke.xml`
- `android-app/app/src/main/res/drawable/settings_chip_indicator.xml`
- `android-app/app/src/main/res/drawable/ic_settings_chip_checked.xml`
- `android-app/app/src/main/res/drawable/ic_settings_chip_unchecked.xml`

## 5. File chỉnh sửa

- `android-app/app/src/main/java/com/example/bivoca/ui/settings/SettingsDetailActivity.java`
- `android-app/app/src/main/res/layout/activity_settings_detail.xml`
- `android-app/app/src/main/res/layout/activity_flashcard.xml`
- `android-app/app/src/main/res/values/colors.xml`
- `android-app/app/src/main/res/values/settings_styles.xml`
- `android-app/app/src/main/res/values/strings.xml`
- `android-app/app/src/main/res/values/themes.xml`
- `WORK_STATUS.md`
- `ANDROID_CONTEXT.md`
- `M12_SETTINGS_REPORT.md`

Không tạo Activity, Repository, ViewModel, Service hay database mới.

## 6. Resource màu và style

Các màu `bivocal_night`, `bivocal_surface`, `bivocal_blue`, `bivocal_cyan`, text chính/phụ, border và danger được đặt trong `colors.xml`. Style dùng chung gồm `SettingsRadioOption`, `SettingsSwitch`, `SettingsDayChip`, `SettingsActionButton`, `SettingsStatLabel` và `SettingsStatValue`. Selector bao phủ checked, enabled/disabled, pressed và focused theo loại control.

## 7. RadioButton, CheckBox và Switch

- Study, Audio và Display dùng `SettingsRadioOption`; cả hàng nhấn được và nhãn đi cùng radio.
- Switch thumb/track có cyan khi bật và xanh xám khi tắt/disabled.
- Day CheckBox dùng chip indicator riêng, có check mark khi chọn và outline khi bỏ chọn.
- Không còn màu tím mặc định trên các control thuộc phạm vi Settings.

## 8. Thiết kế Day Chip

Chip chia thành hai hàng cân đối để giữ vùng chạm tối thiểu và không ép bảy mục vào một hàng. Nhãn ngắn hiển thị `T2`…`CN`; `contentDescription` cung cấp tên đầy đủ Thứ Hai…Chủ Nhật. Runtime đã thử 7/7 và bỏ T2 còn 6/7; status đổi đúng thành `6 ngày/tuần`, sau đó cấu hình được phục hồi 7/7.

## 9. TimePicker

`Theme.BiVoca.TimePicker` dùng nền tối và accent cyan. `SettingsDetailActivity` mở dialog theo theme này và đặt action Hủy/OK màu cyan trong `OnShowListener`. Picker vẫn là component Android chuẩn, dùng 24 giờ và giữ nguyên giá trị giờ/phút. Ảnh `05-settings-timepicker-dark.png` xác nhận dialog tối, chữ rõ và không còn action tím.

## 10. Nút Flashcard

Previous/Next dùng ba selector riêng cho text, background và stroke. Ở thẻ đầu `apple`, Previous disabled vẫn đọc được bằng màu xám xanh; Next enabled có cyan rõ. Ở thẻ `bread`, cả Previous và Next đều enabled và có độ tương phản đúng. Không đổi vị trí, điều hướng thẻ, flip, Favorite, Review hoặc Quiz.

## 11. Môi trường TTS

- Package engine: `com.google.android.tts`.
- Service: `GoogleTtsService`.
- Version quan sát: `googletts.google-speech-apk_20240319.00_p0.617183227`.
- Android `TextToSpeech` khởi tạo thành công; Flashcard hiển thị trạng thái sẵn sàng.
- Từ kiểm thử lấy từ dữ liệu thật: `Food → apple`, sau đó `bread`.

## 12. Kết quả TTS

| Hạng mục | Kết quả |
|---|---|
| Initialization | PASS |
| Manual pronunciation 1.0x | PASS callback `Đã phát âm.` |
| Manual pronunciation 0.8x | PASS callback `Đã phát âm.` |
| Speech rate 1.2x | PASS callback khi chuyển thẻ |
| Auto pronunciation | PASS: chuyển `apple` → `bread` phát đúng một lần theo trạng thái UI |
| Tắt auto pronunciation | PASS và persistence được khôi phục |
| Lifecycle/persistence | PASS sau force-stop/relaunch |
| Âm thanh nghe bằng tai | **USER AUDIO VERIFICATION PENDING** |

Callback thành công chỉ chứng minh engine hoàn tất utterance, không chứng minh loa đã phát âm thanh nghe được. Không có thiết bị thật được dùng trong lượt này.

Cuối nghiệm thu đã phục hồi: auto pronunciation `false`, speech rate `1.0`, daily goal `10`, Flashcard front `EN`, reminder `false`, reminder days `127` (7 ngày).

## 13. Build

Lệnh đã chạy tại `android-app`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Kết quả: **BUILD SUCCESSFUL**. APK được cài bằng `adb install -r`, giữ dữ liệu ứng dụng.

## 14. Unit tests

- Tests: **89**
- Failures: **0**
- Errors: **0**
- Skipped: **0**

Không xóa hoặc bỏ qua test cũ. UI polish không thay đổi nghiệp vụ nên không thêm test chỉ lặp lại selector resource.

## 15. Lint

- Errors: **0**
- Warnings: **268**
- Baseline M12: **268 warnings**
- Warning mới do M12.1: **0**

Các warning cũ ngoài phạm vi không được sửa lan rộng.

## 16. Emulator acceptance

- `adb devices -l`: `emulator-5554` ở trạng thái `device`.
- Settings Main đủ sáu nhóm; Study, Audio, Reminder, Display, Data và About mở đúng.
- Radio, switch, day chip, TimePicker và Back/scroll được thao tác trên UI thật.
- Data đọc Room thật: 10 chủ đề, 150 Flashcard, 526.5 KB tại thời điểm chụp; không xóa cache.
- Flashcard kiểm tra `apple` và `bread`, Previous/Next và TTS.
- Persistence xác nhận bằng force-stop/relaunch và preference riêng của user; không đọc/xóa session.
- Reminder status phản ánh số ngày thật; cuối lượt tắt, 20:00, đủ 7 ngày. Không thấy alarm trùng trong phép thử thay đổi ngày; scheduling logic không bị sửa.
- Landscape kiểm tra bằng `adb shell cmd window user-rotation lock 1`, ảnh thật không crash; sau đó trả về `free`.
- System font scale 1.3 hiển thị Login rõ, không cắt control; sau đó trả về 1.0. M12 giới hạn cỡ chữ trong Settings ở phạm vi đã thiết kế, còn tùy chọn Large chỉ áp dụng nội dung Flashcard như trước.

Trong lần thử kết hợp font scale và rotation bằng system settings, emulator từng hiện `Process system isn't responding`. Emulator được reboot, app data giữ nguyên; hai kiểm tra được chạy lại riêng bằng cách ổn định và đều cho ảnh. Không có crash BiVoca được ghi nhận.

## 17. Screenshots Before/After

Baseline: `android-app/app/build/reports/m12-settings/`  
M12.1: `android-app/app/build/reports/m12-ui-polish/`

| Trước M12 | Sau M12.1 | Điểm đối chiếu |
|---|---|---|
| `01-settings-main.png` | `01-settings-main-polished.png` | card/icon/spacing/state |
| `02-settings-study.png` | `02-settings-study-polished.png` | radio và nhãn cùng hàng |
| `03-settings-audio.png` | `03-settings-audio-polished.png` | switch/rate cyan |
| `04-settings-reminder.png` | `04-settings-reminder-polished.png` | day chip hai hàng |
| `05-settings-reminder-time.png` | `05-settings-timepicker-dark.png` | dark picker/cyan action |
| `06-settings-display.png` | `06-settings-display-polished.png` | radio/switch nhất quán |
| `07-settings-data.png` | `07-settings-data-polished.png` | label/value và danger action |
| `08-settings-about.png` | `08-settings-about-polished.png` | typography/spacing |
| `09-flashcard-english-first.png` | `09-flashcard-first-button.png` | Previous disabled/Next enabled |
| `10-flashcard-vietnamese-first.png` | `10-flashcard-next-button.png` | Previous/Next enabled |
| `04-settings-reminder.png` | `11-reminder-selected-days.png` | bỏ chọn một ngày có indicator |
| `12-navigation-drawer.png` | `12-navigation-drawer-regression.png` | Drawer không hồi quy |

Ảnh bổ sung `13-landscape-regression.png` và `14-large-font-regression.png` ghi nhận kiểm tra accessibility. Tất cả ảnh là screencap trực tiếp từ emulator, không dùng mockup AI.

## 18. Accessibility

- Vùng chạm radio, switch, chip và action tối thiểu 48dp theo layout.
- Text chính/phụ và disabled state có độ tương phản rõ trên navy.
- Day chip có check/outline ngoài màu sắc.
- Các chip có `contentDescription` đầy đủ; icon điều hướng dùng mô tả hiện có.
- ScrollView bảo vệ nội dung trên màn nhỏ và khi cỡ chữ tăng.
- Portrait, landscape và font scale 1.3 không làm app crash; ảnh bổ sung lưu trong evidence folder.

## 19. Regression

- Drawer → Settings → Detail giữ nguyên.
- SharedPreferences giữ daily goal, front side, auto TTS, rate, reminder và display settings.
- ReminderScheduler/Receiver, AlarmManager, session isolation và boot reconciliation không sửa.
- Room cache chỉ được đọc; không xóa dữ liệu.
- Flashcard data, flip, Previous/Next, Review, Favorite, Quiz và M10 không đổi nghiệp vụ.
- Backend/PostgreSQL/Admin/dataset không có thay đổi hoặc migration.
- Build, 89/89 unit tests và lint 0 error xác nhận regression tĩnh.

## 20. Lỗi chưa sửa

- **USER AUDIO VERIFICATION PENDING:** chưa có người nghe trực tiếp xác nhận âm thanh phát ra loa.
- Daily goal vẫn không có X/Y vì không có dữ liệu “thẻ mới hôm nay” đáng tin cậy.
- Tùy chọn Large vẫn chỉ áp dụng cho nội dung Flashcard theo phạm vi M12.
- Các warning lint cũ còn 268, không thuộc phạm vi polish.
- M11 Quiz Result E2E vẫn là tồn đọng riêng, không xử lý trong M12.1.

## 21. Khác biệt với giao diện mong muốn

- TimePicker dùng dialog Android chuẩn được theme tối, không tạo picker tùy biến. Dial giữ hình thức hệ thống nhưng nền/chữ/action tương thích Dark Night Sky.
- Day chip dùng hai hàng thay vì một hàng để bảo đảm vùng chạm và khả năng đọc trên Pixel 6.
- Không thêm gradient, animation hoặc component mới ngoài resource state cần thiết.

## 22. Kết luận nghiệm thu

**M12.1 SETTINGS UI/UX POLISH: DONE.**

Giao diện Settings và nút Flashcard đã đồng bộ Dark Night Sky, không còn màu tím ngoài ý muốn trong phạm vi, day chip và TimePicker đã được làm gọn/tối, persistence và regression đều PASS. Build PASS, 89/89 unit tests PASS, lint 0 error và đủ 12 screenshot bắt buộc.

Nghiệm thu TTS tự động độc lập xác nhận initialization, callback, ba tốc độ và auto pronunciation. Trạng thái âm thanh nghe được giữ đúng là **USER AUDIO VERIFICATION PENDING** cho đến khi người dùng nghe trực tiếp.
