# M16.6 — Drawer / Settings / Reminder refinement

Trạng thái: **READY_FOR_ACCEPTANCE**. Ngày kiểm chứng: 03/11/2026 (Asia/Saigon). Chưa nghiệm thu toàn bộ M16.6, không tiếp tục milestone khác.

## 1. Phạm vi và kết quả

- Drawer bên phải còn đúng Hồ sơ cá nhân / Cài đặt / Đăng xuất, bỏ logo/mascot/wordmark và sáu mục dư. Avatar chữ cái 72dp, lời chào lấy username thật từ phiên, X 48dp, gutters24dp, row tối thiểu56dp/gap12dp. Giữ width84% max360dp, scrim/animation và nguyên thân xử lý logout.
- Cài đặt có một tiêu đề trên shared top bar và bốn card Học tập / Âm thanh / Nhắc học / Giới thiệu. Bỏ Display card/route/group sau audit dependency; giữ getters textSize/reduceMotion đang được Flashcard sử dụng. Card wrap/min82dp tránh cắt nội dung ở font1.3.
- Nhắc học dùng đúng mô tả đã yêu cầu; toggle card CobaltSoft, card giờ/ngày trắng, native spinner24h, nút Lưu56dp/radius14dp, lưới ngày4→3→2 cột theo độ rộng/font. Icon vector cùng nét; checked có dấu chọn và màu, không dựa riêng vào màu.
- ReminderViewModel giữ saved/draft riêng và SavedStateHandle qua recreation. Thay switch/giờ/ngày không ghi preferences hoặc reschedule. Một worker commit bốn trường cùng lúc, chỉ báo thành công sau commit=true rồi gọi scheduler cũ. Bật+0ngày bị chặn; lỗi lưu giữ draft, rollback đúng bốn khóa/presence cũ, không đụng cài đặt khác.
- Back/system Back/Drawer destination khi dirty có Ở lại/Bỏ thay đổi; khóa sửa/rời khi đang lưu hoặc chờ quyền. Permission denial không thay saved schedule. Scheduler exception sau persistence có cảnh báo riêng “đã lưu nhưng chưa cập nhật lịch”, không tuyên bố notification đã gửi.
- Guard ID dương khi render avatar tránh LocalAvatarStore.load(-1) gây crash khi phiên mất. Đây là thay đổi render trong Controller; SessionManager/JWT/Auth/Google Sign-In không sửa.

## 2. Design System và Skills đã áp dụng

Giữ canvas #F7F7F2, surface #FFFFFF, primary text #212A37, secondary #718090, border #E4E7E7, cobalt #2855D5, soft #E7EDFF, mint #B9E5D3, coral #F29A7E, lavender #B4A3E7. Dùng ink nhỏ hiện có #5E6B79 và coral ink #9B422B cho text cần tương phản. Không thay font assets/themes toàn ứng dụng.

| Skill thực đọc | Cách áp dụng thực tế |
|---|---|
| android-mobile-frontend-design | Chế độ cải thiện màn hiện có; giữ luồng/native controls; card wrap và responsive day-grid; kiểm tra font lớn, insets và scroll thay vì cố nhét nội dung. |
| android-viewsystem-foundations | Java/XML, Activity/ViewModel/LiveData, SavedStateHandle, observe theo lifecycle; reuse cùng checkbox khi đổi cột, giữ state/listeners; dirty guard và shared Drawer. |
| ui-ux-pro-max | Dùng rule tables/checklist vì Python không sẵn; kiểm tra hierarchy một title, touch48dp, gap8–12dp, checked icon, feedback gần Save, disabled khi lưu, text tiếng Việt5weights. Không giả kết quả CLI search. |
| bivoca-quality-handoff | Tách PASS lệnh test khỏi nghiệm thu; GET-only, hash715file baseline, lưu RED/GREEN, giữ APK/data/phiên, append WORK_STATUS. |
| Superpowers TDD / planning / executing / verification / requesting-code-review | RED Save thiếu trên APK cũ; unit rollback và validation; matrix phát hiện crash thật; một review độc lập; sửa lỗi rồi chạy lại suite và matrix. |

design-system / ui-styling / brand: không tìm thấy SKILL.md trong project/local/plugin roots đã audit; không cài thêm. Tái sử dụng token/resources M16.6, không tự nhận đã dùng các skill thiếu.

Roboto/serif/italic/negative tracking không được thêm. Be Vietnam Pro400/500/600/700/800 lấy từ font assets cục bộ sẵn có; native picker được gán cùng family. 10chuỗi bắt buộc được chụp ở font1.3 trên5 trang typography có nhãn TEST, không hardcode username vào sản phẩm. Kiểm tra trực quan dấu/glyph/cắt chữ trên ảnh cuối.

Giới hạn contrast: Profile giữ #718090 theo yêu cầu (khoảng4:1 trên trắng), chưa đạt4.5:1 cho chữ thường nhỏ; không tự đổi màu đã chốt. Small descriptions/day labels dùng ink hiện có đậm hơn. Không thêm dark theme hoặc kiểm thử tablet ngoài phạm vi.

## 3. Build, test và runtime

Lệnh từ android-app, JAVA_HOME=Android Studio JBR, ANDROID_HOME=SDK cục bộ; không sửa local.properties/dependency.

| Kiểm tra | Kết quả | Bằng chứng/giới hạn |
|---|---|---|
| :app:assembleDebug | PASS | fixed-build.log; task đã chạy lại sau sửa grid, không chỉ dùng build cũ. |
| :app:testDebugUnitTest | PASS116/116 | 26suites, gồm6test mới cho immutable draft, time/day/account validation, commit4keys, rollback khi disk failure cập nhật memory, presence và cài đặt không liên quan. Lượt sau grid Java sửa UI, task unit có phần UP-TO-DATE; lượt unit116 thực chạy đã lưu ở green-build.log. |
| :app:lintDebug | PASS | 0errors,449warnings toàn app (gồm debt hardcoded text/overdraw/layout hiện có); không gọi là “không warning”. |
| :app:assembleDebugAndroidTest | PASS | fixed-build.log, install-r app + testAPK Success; keepInstalledApks=true/uninstallIncompatibleApks=false theo guard hiện có, không dùng Gradle uninstall. |
| Functional instrumentation | PASS | final-functional.log: OK5tests;4test thực thi chức năng,1method durability chỉ hoạt động khi có stage và được chạy riêng bên dưới. |
| Local schedule save | PASS | draft chưa ghi; Save=true/21:37/days42 → status3ngày, đúng ngày T2/T4/T6; PendingIntent FLAG_NO_CREATE quan sát đúng các ngày, disabledSave xóa registrations; reopen/recreate đọc đúng. finally khôi phục bốn khóa/presence và reconcile lịch gốc. |
| Durable process reopen | PASS | write/read là2invocation AndroidJUnitRunner riêng, mỗi lượt1test; PID target giữa hai lượt không còn. Đọc22:41/days42 ở tiến trình mới rồi khôi phục đúng baseline từ cache backup4keys, xóa đúng file backup. Không force-stop/clear/uninstall. |
| Dirty Back/Drawer Logout cancel | PASS | Ở lại giữ draft/phiên; Bỏ thay đổi không ghi. Không thực hiện logout thật; nguyên thân logout và SessionManager giữ hash. |
| Runtime permission granted | PASS | POST_NOTIFICATIONS=granted/UserSet hiện có; không pm grant/revoke. |
| Permission denied/global notifications off branches | PASS mô phỏng tham số ViewModel | Kiểm tra save(false,false)→permissionResult(false,false), save(true,false) không ghi preferences. Không giả nhận đã thấy OS dialog denial trên máy hiện có quyền. |
| Notification thực sự đến đúng giờ | NOT TESTED | Scheduler cũ dùng AlarmManager.setWindow, cửa sổ15phút; không ép broadcast/fire notification hoặc claim delivery. |
| API /health, /auth/me và count | PASS | GET-only, JWT chỉ trong RAM; DBconnected,20.000Flashcard,38Topics,catalog36mục hiển thị. Catalog36 không phải số topic trong database. |

Matrix sau sửa:

| Configuration | Kết quả |
|---|---|
| phone412-font1 | PASS |
| phone412-font13 | PASS |
| phone320-font1 | PASS |
| phone320-font13 | PASS |
| landscape412-font1 | PASS |
| landscape320-font13 | PASS |

412×915dp dùng override1082×2402px/density420 (làm tròn),320×640dp dùng840×1680px; landscape xoay cùng device. Scroll đưa đủ7day chips vào viewport, min48dp/bounds checks PASS. Font1.3 có ảnh Drawer/Settings/Reminder và5fontweights.

### Lỗi đã phát hiện và sửa

- RED đầu: Save control chưa tồn tại trên APK cũ, test FAIL đúng assertion mong đợi.
- Lượt UI đầu bị hết JWT, Save/username tests FAIL; user tự chọn Google đăng nhập lại, không reset mật khẩu/tạo user. Bốn test rerun PASS. Giữ ui-run.log và ui-run2.log, không xóa lịch sử FAIL.
- Matrix RED: giảm GridLayout.columnCount khi children còn giữ grid indices4cột gây IllegalArgumentException. Review độc lập xác nhận cùng lỗi Important. Sửa bằng tháo/re-add đúng7View hiện có khi đổi count, gán row/col spec phù hợp khi grid trống, giữ checked/listeners/focus. Sau sửa build/unit/lint/testAPK và toàn bộ matrix PASS. Không dispatch vòng review thứ hai.
- Review không có thêm Critical/Important/Minor có bằng chứng. Chưa tái hiện session đổi ngay giữa worker và main.post; permission-pending qua process death/OS denial UI chưa kiểm chứng. Đây là giới hạn, không tự mở rộng Auth.

## 4. Ảnh Android thật

[Mở gallery đầy đủ](../exports/m16-6-drawer-settings-reminder/gallery.html).

| Ảnh bắt buộc | File |
|---|---|
| Drawer3mục | [drawer.png](../exports/m16-6-drawer-settings-reminder/final-functional/drawer.png) |
| Settings4mục | [settings.png](../exports/m16-6-drawer-settings-reminder/final-functional/settings.png) |
| Reminder trước Save | [reminder-before-save.png](../exports/m16-6-drawer-settings-reminder/final-functional/reminder-before-save.png) |
| Reminder sau Save | [reminder-after-save.png](../exports/m16-6-drawer-settings-reminder/final-functional/reminder-after-save.png) |
| Enabled nhưng0ngày | [reminder-invalid-days.png](../exports/m16-6-drawer-settings-reminder/final-functional/reminder-invalid-days.png) |
| Font1.3 | [Reminder](../exports/m16-6-drawer-settings-reminder/phone412-font13/reminder-top.png), [day grid](../exports/m16-6-drawer-settings-reminder/phone320-font13/reminder-days.png);5trang glyphs trong gallery. |

Tất cả ảnh do UiAutomation chụp màn hình Emulator thật, không imagegen/mock. Giờ/ngày21:37–T2/T4/T6 trong ảnh là cấu hình test được lưu thật và đã phục hồi lịch gốc sau test; không phải lịch mới giữ lại cho người dùng. Gallery là tài liệu cục bộ, không đổi giao diện app/backend.

## 5. File thêm/sửa trong phiên này

24file source/resource/test, cộng báo cáo này và append WORK_STATUS.md. Không bao gồm các thay đổi M16.6/Quiz optimization đã có trước.

| Loại | File |
|---|---|
| Sửa | `android-app/app/src/androidTest/java/com/example/bivoca/LightColorfulScreensUiTest.java` |
| Sửa | `android-app/app/src/androidTest/java/com/example/bivoca/TopicDetailUiTest.java` |
| Thêm | `android-app/app/src/androidTest/java/com/example/bivoca/settings/ReminderRefinementUiTest.java` |
| Thêm | `android-app/app/src/main/java/com/example/bivoca/settings/ReminderSchedule.java` |
| Sửa | `android-app/app/src/main/java/com/example/bivoca/settings/SettingsPreferenceManager.java` |
| Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/navigation/NavigationDrawerController.java` |
| Thêm | `android-app/app/src/main/java/com/example/bivoca/ui/settings/ReminderViewModel.java` |
| Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/settings/SettingsActivity.java` |
| Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/settings/SettingsDetailActivity.java` |
| Thêm | `android-app/app/src/main/res/color/reminder_day_text.xml` |
| Thêm | `android-app/app/src/main/res/drawable/refinement_card_background.xml` |
| Thêm | `android-app/app/src/main/res/drawable/refinement_logout.xml` |
| Thêm | `android-app/app/src/main/res/drawable/refinement_settings.xml` |
| Thêm | `android-app/app/src/main/res/drawable/refinement_user.xml` |
| Thêm | `android-app/app/src/main/res/drawable/reminder_check.xml` |
| Thêm | `android-app/app/src/main/res/drawable/reminder_day_background.xml` |
| Thêm | `android-app/app/src/main/res/drawable/reminder_toggle_background.xml` |
| Sửa | `android-app/app/src/main/res/layout/activity_settings.xml` |
| Sửa | `android-app/app/src/main/res/layout/activity_settings_detail.xml` |
| Sửa | `android-app/app/src/main/res/layout/view_navigation_drawer.xml` |
| Thêm | `android-app/app/src/main/res/layout/view_settings_reminder.xml` |
| Thêm | `android-app/app/src/main/res/values/refinement_strings.xml` |
| Thêm | `android-app/app/src/main/res/values/reminder_refinement_styles.xml` |
| Thêm | `android-app/app/src/test/java/com/example/bivoca/settings/ReminderPersistenceTest.java` |

## 6. Bảo toàn và bàn giao

- Hash715file baseline tracked/untracked không bị ignore: chỉ9file có trước thuộc phạm vi đã duyệt thay đổi; file mới liệt kê riêng. Kiểm tra từng applied hash; không ghi đè thay đổi ngoài snapshot.
- Backend/Admin/Home/Topics/Flashcard/Quiz/Auth/font assets/ReminderScheduler/ReminderReceiver/SessionManager và backend/.env không đổi. Logout body giống hệt trước. Render avatar guard không sửa logic xác thực.
- Database qua GET giữ20.000Flashcard/38Topics. Không SQL/migration/seed/import/restore/reset/M15.7, không Review/QuizAnswer/Favorite/goal writes; reminder chỉ là local SharedPreferences đã được phép test, phục hồi nguyên trạng. Không logout/clear data/wipe/uninstall.
- Emulator trở về kích thước native1080×2400,density420,font1.0,rotationauto1/user0; múi giờ Asia/Bangkok giữ nguyên. Quyền notification/local-network không đổi. Cache backup durability đã xóa sau restore. Phiên JWT hiện tại hợp lệ tại finalGETcheck; hết hạn về sau vẫn theo Auth cũ.
- HEAD/branch/index giữ nguyên: feature/initial-import,HEADa4a9801,staging trống. CHƯA COMMIT/CHƯA PUSH; không reset/clean/checkout. Evidence chứa username/ảnh tài khoản chỉ trong docs/exports bị Gitignore, không đưa vào memory/skill.
- User xem Drawer/Settings/Reminder trên Emulator và gallery, thử lịch mong muốn rồi Save để nghiệm thu. Không tự xác nhận toàn M16.6 DONE hoặc triển khai bước khác.
