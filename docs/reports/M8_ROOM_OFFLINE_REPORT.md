# Báo cáo bàn giao M8 Room Offline Cache cho BiVoca

Ngày xác minh: **2026-10-14**. Thư mục dự án: `C:\Users\phuc\OneDrive\Documents\New project`.

**Trạng thái: DONE.** M8 đã nghiệm thu trên **OPPO CPH2557, Android 15**: 5/5 Room instrumentation tests PASS, luồng API thật → ngắt ADB reverse → khởi động lại app → học từ Room → khôi phục kết nối và refresh PASS. Build, 56 unit tests và lint PASS từ lượt triển khai trước. Có sự cố Gradle gỡ app ở lần instrumentation đầu làm mất phiên cục bộ; đã cài lại, người dùng đăng nhập lại và xác minh cấu hình giữ APK bằng lần chạy test sau. Chi tiết kết quả và sự cố ở mục 7.

## 1. Kết quả triển khai

Android có bộ nhớ đệm Room cho danh sách chủ đề và flashcard theo mã chủ đề. Hai repository hiện có dùng chiến lược **Network First, Cache Fallback**: dữ liệu API hợp lệ được ghi xuống Room trên luồng riêng; khi mất kết nối hoặc backend tạm thời không khả dụng, ứng dụng đọc snapshot đã lưu và báo Offline.

Topics và Flashcard có thông báo chưa có cache, trạng thái danh sách rỗng, banner Offline và nút thử kết nối lại. Flashcard giữ cơ chế lật thẻ, trước/sau, metadata, TTS và vị trí thẻ qua xoay màn hình. Favorite, Review và nút mở Quiz bị khóa khi đang xem cache. Nếu mạng mất sau khi đã tải dữ liệu online, lỗi ghi Favorite/Review hiện thông báo; Review lỗi giữ nguyên thẻ, không tự chuyển như thể đã lưu thành công.

Không sửa backend, PostgreSQL, migration, dataset, Admin Web hoặc các module Authentication, JWT, Quiz, Gamification, Leaderboard, Profile/Progress. Chỉ chỉnh cách FlashcardActivity gọi/hiển thị kết quả ProgressRepository hiện có ở lượt triển khai. Lượt nghiệm thu chỉ sửa cấu hình Gradle để giữ APK sau instrumentation. Không chạy lệnh reset/xóa tài khoản hoặc dữ liệu server; sự cố mất phiên cục bộ do bước dọn dẹp Gradle được ghi rõ ở mục 7.

## 2. Contract và khác biệt so với tài liệu cũ

- Android vẫn dùng Java 11, XML, MVVM, Retrofit, LiveData và SavedStateHandle. AGP **9.3.3**, Gradle wrapper **9.5.0**, compile/target SDK **37**, min SDK **24**. Một số hướng dẫn gọi 9.3.3 là phiên bản Gradle; đó là AGP.
- `GET /api/v1/topics` là public, trả `id`, `code`, `name`, `description`, `imageUrl`. **Không trả `flashcardCount`**. Model cũ vẫn giữ getter để tương thích; Room chỉ lưu trường thật và TopicAdapter ẩn nhãn số lượng thay vì hiển thị 0 không có căn cứ.
- `GET /api/v1/topics/{code}/flashcards` là public, trả `id`, `english`, `vietnamese`, `pronunciation`, `partOfSpeech`, `example`, `exampleVi`, `level`. Các trường metadata có thể null.
- Backend hiện liên kết flashcard bằng `topic_id`. Cache vẫn dùng khóa kép `(topicCode, id)` để snapshot của chủ đề này không ghi đè chủ đề khác, kể cả nếu API sau này cho phép một ID ở nhiều chủ đề.
- `TopicRepository` trước đây dùng authenticated client dù endpoint public; M8 chuyển repository này sang `RetrofitClient` công khai, giống FlashcardRepository. Chính sách JWT không thay đổi.
- Probe API thật trên máy trong lượt M8: **10 chủ đề**, FOOD **30 thẻ**, thẻ đầu `apple`, response đúng tám trường đã nêu. Đây là kiểm tra API từ máy tính, chưa phải bằng chứng Android Offline.

## 3. Thiết kế Room

Room **2.8.5**, xử lý annotation bằng Java `annotationProcessor`; không chuyển source sang Kotlin, không thêm Robolectric. Phiên bản ổn định và hỗ trợ Java được đối chiếu tại [Android Room releases](https://developer.android.com/jetpack/androidx/releases/room). Room min SDK 23 tương thích min SDK 24 của app; Gradle đã resolve và compile dependency thành công.

Database `bivoca-learning-cache.db`, phiên bản **1**, gồm:

| Bảng | Khóa và dữ liệu | Mục đích |
| --- | --- | --- |
| `cached_topics` | PK `id`, unique `code`; name, description, imageUrl, position | Snapshot danh sách chủ đề, giữ thứ tự API |
| `cached_flashcards` | PK `(topicCode, id)`; tám trường DTO và position | Snapshot flashcard riêng từng chủ đề |
| `cache_sync` | PK `scope`, `syncedAt` dạng epoch milliseconds | Phân biệt chưa tải với tải thành công danh sách rỗng |

Scope là `topics` hoặc `flashcards/{topicCode}`. Không có marker nghĩa là chưa cache (`null`); có marker nhưng không có row nghĩa là cache rỗng hợp lệ (`[]`). Mỗi lần tải thành công thay toàn bộ snapshot của đúng scope trong cùng transaction với marker; bản ghi đã bị bỏ khỏi response không còn trong snapshot đó. Transaction đọc giữ marker và rows nhất quán. Các trường nullable được giữ nguyên null.

Database singleton mở lazily từ worker. `CacheDispatchers` dùng một executor tuần tự cho thao tác cache và Handler Main Looper để trả kết quả. Không có `allowMainThreadQueries`, không có destructive migration. Schema đã export tại `android-app/app/schemas/com.example.bivoca.data.local.LearningDatabase/1.json`. Khi nâng phiên bản DB sau này phải thêm migration tường minh và kiểm thử migration; xem [hướng dẫn Android](https://developer.android.com/training/data-storage/room/migrating-db-versions).

Room chỉ giữ nội dung học công khai; không chứa mật khẩu, JWT, Favorite, Review, Quiz, XP hay tiến độ cá nhân. Cache dùng chung trên thiết bị vì nội dung public, không bị xóa khi đổi tài khoản.

## 4. Luồng tải và xử lý lỗi

1. ViewModel gọi repository hiện có; Retrofit gọi API public thật.
2. Repository kiểm tra body, trường bắt buộc, ID/code hợp lệ và trùng ID/code trước khi dùng. Danh sách rỗng là response hợp lệ.
3. API thành công: chụp danh sách bất biến, ghi cache trên worker rồi trả dữ liệu API với `fromCache=false`. Lỗi ghi Room không làm mất kết quả API; lần offline sau có thể vẫn thấy snapshot cũ hoặc chưa có cache.
4. IOException kết nối, timeout và HTTP **500/502/503/504**: đọc scope tương ứng. Cache hợp lệ trả `fromCache=true`, kể cả `[]`. Thiếu cache hoặc đọc cache lỗi trả thông báo tiếng Việt và lựa chọn thử lại.
5. HTTP nghiệp vụ/auth như **400/401/403/404**, JSON sai, body sai contract, EOF/Protocol/TLS lỗi: báo lỗi, không dùng cache để che lỗi, không ghi đè snapshot tốt đã lưu.
6. Người dùng nhấn **Thử kết nối lại**: tải lại API; thành công cập nhật cache và bỏ trạng thái Offline. Không có bộ đồng bộ chạy nền hay hàng đợi ghi dữ liệu người dùng.

`NetworkFirstLoader` chỉ dùng chung chính sách thực thi cho hai repository; không tạo repository trùng chức năng. Request bị hủy được kiểm tra trước tác vụ disk và trước callback main. ViewModel có `requestVersion` để loại kết quả cũ; `ensureTopicsLoaded`/`ensureFlashcardsLoaded` tránh tải lại khi Activity được tạo lại. Flashcard giữ index trong SavedStateHandle và giới hạn index theo kích thước response mới.

## 5. Danh sách file

Các đường dẫn Java dưới đây nằm trong `android-app/app/src/main/java/com/example/bivoca/`.

**Thêm mới:**

- `data/local/LearningCache.java`: contract cache; null khác danh sách rỗng.
- `data/local/RoomLearningCache.java`: mapping model/entity và truy cập Room từ worker.
- `data/local/LearningDatabase.java`: singleton Room v1.
- `data/local/LearningCacheDao.java`: transaction đọc/thay snapshot và marker.
- `data/local/TopicEntity.java`, `FlashcardEntity.java`, `CacheSyncEntity.java`.
- `data/repository/CacheDispatchers.java`, `NetworkFirstLoader.java`.
- `android-app/app/src/main/res/values/offline_strings.xml`.
- `android-app/app/schemas/.gitkeep` và `schemas/com.example.bivoca.data.local.LearningDatabase/1.json`.
- `android-app/app/src/test/java/com/example/bivoca/data/repository/LearningRepositoryTest.java`.
- `android-app/app/src/test/java/com/example/bivoca/ui/topics/TopicsViewModelTest.java`.
- `android-app/app/src/androidTest/java/com/example/bivoca/data/local/RoomLearningCacheTest.java`.
- `M8_ROOM_OFFLINE_REPORT.md`.

**Chỉnh sửa:**

- `android-app/app/build.gradle.kts`: Room dependencies và schema export javac.
- `android-app/gradle.properties`: thêm `android.injected.androidTest.leaveApksInstalledAfterRun=true` trong lượt nghiệm thu, ngăn Gradle gỡ APK và dữ liệu sau test.
- `data/model/Topic.java`, `Flashcard.java`: constructor phục vụ mapping cache, giữ getter cũ.
- `data/repository/TopicRepository.java`, `FlashcardRepository.java`: network/cache và nguồn dữ liệu trong callback.
- `ui/topics/TopicsViewModel.java`, `TopicsActivity.java`, `TopicAdapter.java`.
- `ui/flashcard/FlashcardViewModel.java`, `FlashcardActivity.java`.
- `android-app/app/src/main/res/layout/activity_topics.xml`, `activity_flashcard.xml`.
- `android-app/app/src/test/java/com/example/bivoca/ui/flashcard/FlashcardViewModelTest.java`.
- `WORK_STATUS.md`, `ANDROID_CONTEXT.md`.

## 6. Kết quả build và kiểm thử tự động

Thực thi trong thư mục `android-app` ngày 2026-10-14:

| Lệnh | Kết quả thực tế |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug` | PASS; APK debug được tạo |
| `.\gradlew.bat :app:testDebugUnitTest` | PASS **56 tests, 0 failure, 0 error** |
| `.\gradlew.bat :app:lintDebug` | PASS **0 lỗi, 195 cảnh báo**, bằng tổng trước M8 |
| `.\gradlew.bat :app:assembleDebugAndroidTest` | PASS; APK instrumentation biên dịch thành công |

Ba lệnh đầu chạy trong một invocation Gradle, kết thúc `BUILD SUCCESSFUL in 3m 55s`; build instrumentation thành công trong 36 giây. Lần chạy đầu trong sandbox không truy cập mạng được (`SocketException: Permission denied: getsockopt`); chạy lại Gradle ngoài sandbox thành công. Cảnh báo javac API deprecated còn tồn tại; không sửa hàng loạt cảnh báo ngoài phạm vi.

Phân bố unit test: repository cache **26**, FlashcardViewModel **12**, TopicsViewModel **5**, các suite hiện hữu khác **13**. Tổng **56**, tăng **36** so với 20 trước M8. Bao phủ API→cache, offline có/không cache, cache rỗng, tách chủ đề, metadata null, lỗi đọc/ghi, lỗi contract, hủy request trước/sau disk và callback main, phản hồi cũ, retry, vị trí thẻ và vòng đời. Fake transport/cache chỉ có trong test, không thay API thật của app.

Năm instrumentation tests Room dùng database **in-memory riêng**: phân biệt missing/empty; nullable roundtrip và thứ tự; thay snapshot; độc lập từng chủ đề; rollback rows cùng metadata khi transaction lỗi. **5/5 PASS trên OPPO** ở lượt nghiệm thu. Test code không đụng database cài đặt; bước dọn dẹp của Gradle ở lần đầu đã gỡ APK ngoài dự kiến. Lần chạy sau với cấu hình giữ APK PASS và giữ nguyên phiên/cache, xem mục 7.

Bằng chứng máy tạo:

- `android-app/app/build/test-results/testDebugUnitTest/TEST-*.xml`.
- `android-app/app/build/reports/tests/testDebugUnitTest/index.html`.
- `android-app/app/build/reports/lint-results-debug.html` và `.xml`.
- `android-app/app/build/outputs/apk/debug/app-debug.apk`.
- `android-app/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

## 7. Nghiệm thu thực tế trên OPPO

Thiết bị **CPH2557, Android 15**, serial `9TPRGIPFPRO7ZTNV`. Ban đầu ADB báo `unauthorized`; người dùng chấp nhận USB debugging, sau đó thiết bị ở trạng thái `device`. APK được cài bằng `adb install -r` và app ban đầu giữ phiên đăng nhập. Kết quả cuối dựa trên UI hierarchy, ảnh màn hình đã xem và bản sao database Room public; không đọc/lưu JWT hoặc mật khẩu.

### 7.1 Kết quả từng bước

| Bước | Kết quả quan sát | Trạng thái |
| --- | --- | --- |
| Thiết bị và cài APK | ADB nhận CPH2557; `install -r` trả `Success` | PASS |
| Room instrumentation | Cả 5 test PASS, 0 failure/error/skipped; chạy lại sau sửa cấu hình cũng 5/5 PASS | PASS |
| Online Topics | Backend thật trả 10 chủ đề; Android hiển thị danh sách và Room lưu 10 rows | PASS |
| Online FOOD | 30 thẻ; `apple` → Next `bread` → Previous `apple`; lật hiện `quả táo` và ví dụ | PASS |
| Online ANIMALS | 30 thẻ; từ đầu là `dog`, từ thứ hai là `cat`, đúng API thật | PASS |
| Ngắt Backend và restart | Xóa reverse cổng 3000, force-stop, mở lại qua Login → Home → Topics; Topics và FOOD hiện cache cùng banner Offline | PASS |
| Cache theo chủ đề | FOOD vẫn `apple`/`bread`, ANIMALS vẫn `dog`/`cat`; mỗi chủ đề 30 rows riêng | PASS |
| Chủ đề chưa cache | FAMILY báo chưa có từ vựng lưu, không hiện card của chủ đề khác | PASS |
| Lật và Previous/Next offline | FOOD lật `bread` → `bánh mì` cùng ví dụ; điều hướng đúng, không crash | PASS |
| Xoay màn hình | Ở `bread` 2/30: lock landscape, cuộn xem từ, trả portrait vẫn `bread` 2/30 | PASS |
| Favorite/Review/Quiz offline | UI nodes `enabled=false`; bấm nút Review đã khóa vẫn 2/30, không báo lưu thành công | PASS |
| TTS | Bấm phát âm khi không truy cập Backend, callback hiện `Đã phát âm.` | PASS callback; chưa xác nhận âm thanh bằng tai |
| Kết nối lại | Restore reverse, bấm retry Flashcard: banner tắt, `bread` vẫn 2/30, nút online được bật. Retry Topics: banner tắt, danh sách hiển thị. Mở lại ANIMALS nhận API | PASS |
| Refresh Room | `syncedAt` của Topics, FOOD, ANIMALS đều tăng; snapshot đúng nội dung API | PASS |
| Khôi phục môi trường | Reverse cuối `UsbFfs tcp:3000 tcp:3000`; xoay màn hình cuối `lock 0`, đúng giá trị ban đầu | PASS |

Snapshot Room trước và trong Offline giống nhau: 10 Topics, FOOD30, ANIMALS30, markers không đổi. Sau reconnect, markers thay đổi như sau (epoch milliseconds):

| Scope | Trước reconnect | Sau reconnect |
| --- | ---: | ---: |
| `topics` | 1791954879790 | 1791955678004 |
| `flashcards/FOOD` | 1791954942493 | 1791955668995 |
| `flashcards/ANIMALS` | 1791955002433 | 1791955683235 |

Snapshot sau reconnect còn có FAMILY30 với marker mới; FAMILY chưa có marker lúc thử Offline trước đó. Cache bổ sung scope hợp lệ không làm thay đổi/nhầm FOOD hoặc ANIMALS. Sau lần instrumentation có cấu hình bảo vệ, toàn bộ snapshot Room giống snapshot reconnect, và app mở thẳng Home bằng phiên đang có.

Không sửa nội dung Backend để tạo chênh lệch dữ liệu. Bằng chứng refresh là request API thành công, trạng thái UI trở lại online, nội dung khớp API và thời điểm sync mới. Logcat còn trong buffer ở cuối thử Offline chỉ ghi nhận GET ANIMALS/FAMILY public, không có request ghi dữ liệu cá nhân trong phần log đó. Kiểm tra AndroidRuntime trên process cuối không thấy `FATAL EXCEPTION`; đây không phải khẳng định về mọi log trong toàn bộ lịch sử thiết bị.

### 7.2 Sự cố và thay đổi tối thiểu

**Sự cố Gradle:** lần `connectedDebugAndroidTest` đầu PASS 5 test nhưng Gradle gỡ gói ứng dụng sau test. `pm list packages` không còn app; cài lại và mở launcher dẫn tới Login. Phiên cục bộ đã mất. Đã thông báo cho người dùng; người dùng đăng nhập lại trực tiếp trên điện thoại. Các test Room chỉ dùng DB in-memory, không có lệnh reset hoặc thay đổi dữ liệu Backend.

Thêm một thuộc tính vào `android-app/gradle.properties`:

```properties
android.injected.androidTest.leaveApksInstalledAfterRun=true
```

Thuộc tính này được dùng trong [cấu hình AndroidX chính thức](https://android.googlesource.com/platform/frameworks/support/+/672b13fff068f1210c1e28230278c8702ce3485b/gradle.properties). Đọc bytecode **AGP 9.3.3 đang cài** bằng `javap` xác nhận task lấy `keepInstalledApks` từ thuộc tính này và chuyển giá trị đảo sang `uninstallApksAfterTest`. Preflight task thực tế trả `keepInstalledApks=true`, `uninstallIncompatibleApks=false`. Init script dừng trước test nếu một trong hai điều kiện không đúng.

Lần yêu cầu chạy lại trước khi có bằng chứng này bị **automatic approval review từ chối** vì nguy cơ gỡ dữ liệu. Sau preflight và kiểm tra bytecode, lần chạy có guard được cho phép: 5/5 PASS, app vẫn cài, phiên vẫn mở Home, cache không đổi. Không có lần chạy thiết bị nào sau đó bị chặn còn chờ xử lý.

**Harness nghiệm thu:** các lần đầu có assertion sai về tiền tố `Thẻ` (UI thật là `1 / 30`), từ đầu ANIMALS (`dog`, không phải `cat`) và đòi word hiện ngay ở viewport ngang. Đã đối chiếu API/UI thật, chỉnh expectation và cuộn layout trước khi kiểm tra; chạy lại đầy đủ Offline PASS. Một assertion cache đòi chỉ có đúng hai scope cũng được sửa thành kiểm tra đúng hai scope mục tiêu, vì snapshot sau reconnect có thêm FAMILY hợp lệ. Đây là sửa script thu bằng chứng, không sửa logic M8 để ép test PASS.

Không tìm thấy lỗi chức năng Room/Repository/ViewModel cần sửa trong lượt nghiệm thu. Source thay đổi chỉ có `android-app/gradle.properties`; ba tài liệu trạng thái được cập nhật.

### 7.3 Lệnh đã thực thi

Trong thư mục `android-app`, biến ADB trỏ tới `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`. Các lệnh thiết bị đều chọn serial cụ thể bằng `-s 9TPRGIPFPRO7ZTNV`:

```powershell
adb devices -l
adb -s 9TPRGIPFPRO7ZTNV install -r app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.bivoca.data.local.RoomLearningCacheTest'
```

Lệnh install chạy hai lần, đều `Success`: lần cài M8 và lần cài lại sau cleanup. Lệnh instrumentation đầu `BUILD SUCCESSFUL in 50s`, 5 tests/0 failure. Một yêu cầu chạy lại bị approval review từ chối trước khi chạy. Các lệnh xác minh guard và chạy lại được thực thi sau đó:

```powershell
.\gradlew.bat :app:help -I app/build/reports/m8-acceptance/verify-guard.gradle --no-configuration-cache
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.bivoca.data.local.RoomLearningCacheTest' '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true' -I app/build/reports/m8-acceptance/verify-guard.gradle --no-configuration-cache
```

Preflight `BUILD SUCCESSFUL in 9s`. Test có guard `BUILD SUCCESSFUL in 16s`, **5 tests/0 failure/0 error/0 skipped**. Có **5 test duy nhất, chạy thành công hai lần**, không cộng thành 10 test khác nhau.

Các thao tác Online/Offline/rotation/refresh dùng những lệnh sau qua helper PowerShell; mỗi tap lấy tọa độ từ node/bounds thực tế của UI:

```powershell
adb -s 9TPRGIPFPRO7ZTNV reverse tcp:3000 tcp:3000
adb -s 9TPRGIPFPRO7ZTNV reverse --remove tcp:3000
adb -s 9TPRGIPFPRO7ZTNV reverse --list
adb -s 9TPRGIPFPRO7ZTNV shell am force-stop com.example.bivoca
adb -s 9TPRGIPFPRO7ZTNV shell am start -n com.example.bivoca/.ui.auth.LoginActivity
adb -s 9TPRGIPFPRO7ZTNV shell cmd window user-rotation lock 1
adb -s 9TPRGIPFPRO7ZTNV shell cmd window user-rotation lock 0
adb -s 9TPRGIPFPRO7ZTNV shell uiautomator dump /sdcard/m8-window.xml
adb -s 9TPRGIPFPRO7ZTNV shell screencap -p /sdcard/m8-screen.png
adb -s 9TPRGIPFPRO7ZTNV shell pm list packages com.example.bivoca
```

Thực thi `offline.ps1`, `reconnect.ps1` trong `app/build/reports/m8-acceptance/`; script có `finally` khôi phục reverse và thiết lập xoay ban đầu. Dùng `run-as` chỉ sao chép database **public learning cache** bằng binary stream; không sao chép SharedPreferences/session. `python app/build/reports/m8-acceptance/check_cache.py` PASS: so sánh snapshot offline, marker refresh và cache giữ nguyên sau guarded test. Chi tiết đầy đủ lệnh ADB gồm tọa độ tap/swipe, dump/pull, logcat và copy cache được lưu ở `commands.log`.

### 7.4 Bằng chứng và file của lượt nghiệm thu

- `android-app/app/build/outputs/androidTest-results/connected/debug/TEST-CPH2557 - 15-_app-.xml`: 5/0/0/0.
- `android-app/app/build/reports/androidTests/connected/debug/index.html`.
- `android-app/app/build/reports/m8-acceptance/`: `commands.log`, `offline.log`, `reconnect.log`, `cache-verification.json`, ảnh/XML các bước `01`–`19`, bốn bản sao Room public và scripts/helper thu bằng chứng. Đây là output kiểm thử trong thư mục build, không phải source production mới.
- File source/config sửa trong lượt này: **`android-app/gradle.properties`**.
- File tài liệu sửa: **`M8_ROOM_OFFLINE_REPORT.md`, `WORK_STATUS.md`, `ANDROID_CONTEXT.md`**.

## 8. Giới hạn và việc tiếp theo

- Chỉ những chủ đề/thẻ đã tải thành công mới có cache. Cần có phiên đăng nhập sẵn để qua luồng Home/Topics hiện tại; M8 không tạo chế độ đăng nhập offline mới.
- Cache không có TTL hoặc tự tải toàn bộ. Dữ liệu có thể cũ khi offline; online refresh thay snapshot của scope vừa tải. Không xóa cache flashcard của scope khác chỉ vì danh sách Topics thay đổi.
- Offline là trạng thái **đang dùng cache sau lỗi tải**, không phải bộ giám sát mạng liên tục. Mạng mất khi đang xem dữ liệu online sẽ được nhận biết ở request kế tiếp.
- Favorite/Review/Quiz và dữ liệu cá nhân cần API; M8 không lưu hàng đợi để gửi sau.
- TTS giữ implementation cũ; phát âm khi mất Internet còn phụ thuộc voice tiếng Anh offline đã cài trên thiết bị. Không khẳng định âm thanh đã nghe được nếu chỉ quan sát callback.
- Nghiệm thu M8 đã PASS. Phép thử thiết bị ngắt kết nối Backend bằng ADB reverse; không bật chế độ máy bay để kiểm tra toàn bộ Internet. Âm thanh TTS bằng tai và thiết bị/phiên bản Android khác chưa được nghiệm thu trong lượt này. Không triển khai M9/M10.

## 9. Ghi chú cho Codex tiếp nhận

Đọc `AGENTS.md`, `WORK_STATUS.md`, `ANDROID_CONTEXT.md` và báo cáo này, sau đó đọc source trước khi sửa. **M8 DONE** theo nghiệm thu OPPO tại mục 7; không viết lại storage/repository/UI. Khi chạy lại instrumentation trên thiết bị có dữ liệu, phải giữ thuộc tính `android.injected.androidTest.leaveApksInstalledAfterRun=true`, kiểm tra task thực tế trước và cài bằng `-r`. Không ghi/log mật khẩu, token hoặc `.env`. Chỉ nhận milestone tiếp theo khi người dùng giao việc; không tự mở rộng M9/M10.
