# M16.4 — BiVocal Topics Frontend

## Phản hồi cuối — tiêu đề lớn, khôi phục viền hai danh sách (27/10/2026)

Đã đổi **CHỦ ĐỀ / LOẠI TỪ** thành in hoa, tăng 12→16sp và khôi phục viền1dp #334155 cho hai danh sách. Dải chip vẫn không viền; All cố định và mỗi danh sách tối đa4 hàng giữ nguyên. Chỉ sửa topics_frontend.xml/activity_topics.xml. Build/lint/test-APK PASS (5s), APK install-r giữ dữ liệu; resource instrumentation **1/1 PASS** (6 tổ hợp size/font). Không chạy lại unit/full runtime hoặc chụp ảnh mới cho lượt nhỏ này; các ảnh/kết quả phía dưới thuộc bản trước khi tăng tiêu đề/hiện lại viền. Log: build-large-headings.txt và instrumentation-large-headings.txt tại evidence borderless-four-rows/. Chờ duyệt UI, chưa commit/push.

## Cập nhật mới nhất — bỏ viền, bốn hàng, nhãn ngắn (27/10/2026)

- Theo yêu cầu mới: bỏ viền bao ngoài dải chip và hai khung danh sách; mỗi danh sách có viewport tối đa **4 hàng**, ít kết quả hơn thì co lại. Nhãn đổi thành **Chủ đề** và **Loại từ**. Giữ viền của từng card/chip, chip Tất cả cố định và cách cuộn độc lập đã chốt.
- Sửa 6 file: `topics_frontend.xml`, `topics16_filter_frame.xml`, `activity_topics.xml`, `TopicsActivity.java`, `TopicAdapter.java`, `TopicsUiTest.java`. Hàm đo viewport đổi từ năm sang bốn hàng, vẫn đo theo width/font; không sửa source khác theo SHA-256.
- Bốn task Gradle build/unit/lint/test APK PASS mới (11s; 26 task thực thi/52 up-to-date). Unit **83/83**, lint **0 lỗi/393 cảnh báo**. Cài app/test APK bằng `adb install -r`; Topics instrumentation **2/2 PASS**, 48.720s, có kiểm tra bốn hàng ở cả hai list, swipe và điều hướng; resource measurement gồm font1.3/màn nhỏ. Không chạy lại riêng runtime font hệ thống1.3 trong lượt nhỏ này; bằng chứng font1.3 của bản năm hàng bên dưới là lịch sử.
- [Ảnh mới](../../android-app/runtime-evidence/m16-4-topics/borderless-four-rows/topics-top.png), [hai phần danh sách](../../android-app/runtime-evidence/m16-4-topics/borderless-four-rows/topics-two-frames.png), [loại từ](../../android-app/runtime-evidence/m16-4-topics/borderless-four-rows/topics-pos.png). Evidence/log/mã băm ở `android-app/runtime-evidence/m16-4-topics/borderless-four-rows/`. Không đổi Backend/DB/Home/auth hoặc AVD; dữ liệu ứng dụng giữ nguyên. Chờ duyệt UI, chưa commit/push.

## Cập nhật theo ảnh phản hồi — Kho từ vựng, hai khung cuộn (27/10/2026)

Người dùng xác nhận **hai khung riêng**, mỗi khung tối đa năm hàng và cuộn bên trong; trang ngoài vẫn cuộn để chuyển từ Chủ đề tới Loại từ. Phần dưới báo cáo là lịch sử trước thay đổi này.

- Tiêu đề mới **Kho từ vựng**, bỏ shortcut tra từ trên header. Giữ ô tìm chủ đề và avatar Drawer.
- Dải chip có khung nền trong suốt/viền mờ. **Tất cả cố định bên trái**, các chip khác nằm trong HorizontalScrollView bên phải; chỉ một lựa chọn active.
- Tách hai RecyclerView trong hai khung semantic/POS. Header/count từng khung giữ riêng; chiều cao viewport tính bằng năm hàng đo theo width/font thực tế (ít kết quả hơn thì co lại). Khi font lớn hoặc màn thấp, cuộn trang ngoài để xem các phần của khung; không thu nhỏ chữ để ép toàn bộ hai khung vào một màn hình.
- Vuốt trong danh sách chỉ cuộn danh sách đó. Cuộn trang qua phần header/khoảng lề ngoài danh sách để tới khung dưới. Có scrollbar và snap hàng; không tải Flashcard để tính chiều cao.

**Phạm vi source mới:** sửa `activity_topics.xml`, `topics_frontend.xml`, `TopicsActivity.java`, `TopicAdapter.java`, `TopicsUiTest.java`; thêm drawable `topics16_filter_frame.xml`. Source Home/Backend/Admin/Auth, API/model/Repository/ViewModel/paging giữ nguyên theo SHA-256. Không migration/reset hoặc gửi Review/Quiz/Favorite. Catalog vẫn 36 nhóm.

**Kiểm tra cuối:** cùng bốn task `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` PASS (22s; 19 task thực thi/59 up-to-date); unit **83/83**, lint **0 lỗi/393 cảnh báo**. APK cài `adb install -r`, giữ dữ liệu. `TopicsUiTest` trên APK cuối **2/2 PASS** (60.217s); runtime test ở font 1.3 thêm **1/1 PASS** (44.745s). Có kiểm tra viewport năm hàng, vị trí Tất cả không đổi sau swipe ngang, inner swipe không đổi scrollY trang, outer swipe tới POS, tìm/empty/Food/Drawer/recreation/landscape và sáu cấu hình đo layout. Font đã trả 1.0, hash AVD config không đổi.

**Lỗi đã xử lý, không bỏ qua kết quả thất bại:** lint ban đầu FAIL vì padding style API 26, đã đổi sang padding từng phía cho minSdk 24; runtime ban đầu bị chuyển Login khi mở Food (không thu HTTP status để khẳng định nguyên nhân), người dùng tự đăng nhập lại; sau đó test font 1.3 phát hiện viewport còn chiều cao cũ, đã sửa cập nhật kích thước bằng post sau layout và chờ layout ổn định trong test. Log thất bại được giữ bên cạnh log PASS cuối.

Ảnh mới, thay cho các ảnh bố cục cũ:

- [Kho từ vựng phần đầu](../../android-app/runtime-evidence/m16-4-topics/framed-lists/topics-top.png)
- [Tất cả cố định khi vuốt chip](../../android-app/runtime-evidence/m16-4-topics/framed-lists/topics-fixed-all.png)
- [Hai khung Chủ đề/Loại từ](../../android-app/runtime-evidence/m16-4-topics/framed-lists/topics-two-frames.png)
- [Khung Loại từ](../../android-app/runtime-evidence/m16-4-topics/framed-lists/topics-pos.png)
- [Font 1.3](../../android-app/runtime-evidence/m16-4-topics/framed-lists/font-1.3/topics-top.png)
- [Landscape font 1.3](../../android-app/runtime-evidence/m16-4-topics/framed-lists/font-1.3/topics-landscape.png)

Evidence/log/script/mã băm: `android-app/runtime-evidence/m16-4-topics/framed-lists/`. Trạng thái **READY_FOR_ACCEPTANCE**, chờ duyệt UI mới; CHƯA COMMIT/CHƯA PUSH, không M16.5.

---

Ngày: 27/10/2026. **READY_FOR_ACCEPTANCE — phần kỹ thuật đã kiểm thử, chờ người dùng duyệt ảnh.** Không triển khai M16.5.

## Giao diện và dữ liệu

- Topics native Java/XML theo cấu trúc/thông số `topics_demo.html`: nền #090D16, card #111726/viền #334155/radius 18dp, padding màn 24dp, khoảng cách card 12dp, icon box 48dp, tên 16sp, mô tả/count 12sp.
- Header có tiêu đề, số nhóm API thật, shortcut tra từ và avatar mở Drawer bên phải. Không Back/hamburger trái.
- Search cao 46dp/radius 14dp, lọc tức thời trên catalog theo tên, code và mô tả Việt, không gọi API theo từng ký tự.
- Chip cuộn ngang: Tất cả, Chủ đề, Loại từ, Đời sống, Công việc & Số, Thiên nhiên. Ba chip đầu lấy số nhóm từ API; mapping ba chip sau chỉ lọc các topic code chính xác đang có, không phân loại lại Flashcard hoặc ghi DB.
- Danh sách RecyclerView có section semantic trước POS, số thẻ thật và vector local; không emoji, badge Mastery hoặc tiến độ giả. Có ripple pressed, loading, empty/error và Retry.
- Bottom navigation dùng chung với Home, đúng bốn tab; Chủ đề active. Danh sách cuộn độc lập. Tìm kiếm/chip được giữ khi Activity recreation.

API `GET /api/v1/topics/catalog` trước/sau nghiệm thu đều có **36 nhóm: 29 semantic + 7 POS**; ID/code/name/description/count/group không đổi. General Vocabulary/POS Phrase rỗng không được API trả về. Tổng cộng các count là 22.147 quan hệ thẻ–nhóm, **không phải 22.147 Flashcard duy nhất**, vì thẻ có thể có chủ đề phụ. Header không lấy tổng đó làm số Flashcard.

## Component tái sử dụng và file

Giữ TopicsViewModel, TopicRepository, Topic model/TopicApi, FlashcardActivity/ViewModel và cơ chế phân trang hiện hành. Không thêm Activity/Repository/ViewModel trùng.

| Phạm vi | File sửa/thêm, tính từ gốc project |
|---|---|
| Topics UI | Sửa `android-app/app/src/main/java/com/example/bivoca/ui/topics/TopicsActivity.java`, `TopicAdapter.java`, `TopicFilter.java`; `res/layout/activity_topics.xml`, `item_topic.xml`. |
| Resource Topics | Thêm `res/values/topics_frontend.xml`, `res/layout/item_topic_chip.xml`, hai selector `res/color/topics16_chip_*.xml`, 25 drawable `topics16_*` gồm 21 icon topic, Search/Chevron và hai nền. Các resource ở `android-app/app/src/main/`. |
| Navigation chung | Thêm `ui/navigation/LearningBottomNavigation.java`, `res/layout/view_learning_bottom_navigation.xml`, `view_navigation_avatar.xml`. Sửa `NavigationDrawerController.java` để Topics dùng header riêng nhưng giữ Drawer/insets cũ. |
| Home trong phạm vi cho phép | Chỉ sửa `ui/home/HomeActivity.java`, `res/layout/activity_home.xml`, `view_home_header_actions.xml` để dùng component tab/avatar đã tách; giữ ID, kích thước và hành vi cũ, gồm flags điều hướng Quiz. Không sửa nội dung/card/logic Home. |
| Manifest | Sửa `android-app/app/src/main/AndroidManifest.xml`: Topics dùng theme tối hiện có và adjustResize. |
| Tests | Mở rộng `src/test/java/com/example/bivoca/ui/topics/TopicFilterTest.java`; thêm `src/androidTest/java/com/example/bivoca/TopicsUiTest.java`, đều dưới `android-app/app/`. |

Đối chiếu SHA-256: **11 file hiện hữu sửa, 33 file mới**, không thiếu/xóa source. Danh sách đầy đủ ở [source-changes.json](../../android-app/runtime-evidence/m16-4-topics/source-changes.json). Các source Backend/Admin/Auth/SessionManager, model/API/Repository/ViewModel Topics, Flashcard paging và HomePresentation giữ nguyên.

## Kiểm thử thực tế

Lệnh Gradle chạy từ `android-app`; ADB chạy với `-s emulator-5554`. Log trong thư mục evidence bên dưới.

| Kiểm tra | Kết quả |
|---|---|
| `adb devices -l` | PASS — Emulator kết nối. |
| `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --rerun-tasks` | PASS, 1m14s, 78 task thực thi. Unit **83/83**, 0 fail/error. |
| Cùng bốn task sau khi thêm instrumentation | PASS, 8s; 8 task chạy/70 up-to-date, unit khi đó up-to-date. |
| Cùng bốn task trên source cuối | PASS, 22s; 15 task chạy/63 up-to-date. Unit chạy lại **83/83**; lint chạy lại **0 lỗi/391 cảnh báo** toàn dự án. |
| `:app:connectedDebugAndroidTest --dry-run --no-configuration-cache -I runtime-evidence/m16-3-home/final-polish/test-guards.init.gradle` | PASS: keepInstalledApks=true, uninstallIncompatibleApks=false. Đây chỉ là kiểm tra guard, không tính là test thực thi. |
| `adb install -r` app-debug.apk và app-debug-androidTest.apk | PASS, giữ phiên/dữ liệu, không uninstall/clear data. |
| `adb shell am instrument -w -r -e class com.example.bivoca.TopicsUiTest,com.example.bivoca.HomeResponsiveUiTest,com.example.bivoca.HomeRuntimeUiTest com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner` | APK cuối **4/4 PASS**, 66.407s. Lượt đầu trước chỉnh flags component cũng 4/4 PASS, 69.079s. |
| Cùng runner, `-e class com.example.bivoca.TopicsUiTest#realCatalogFiltersNavigationAndScreenshots`, font hệ thống 1.3 | **1/1 PASS**, 30.845s. Font được trả 1.0 trong finally. |

Topics instrumentation kiểm tra:

- Adapter hiển thị đủ ID catalog và số nhóm thật; tab Chủ đề active, Home inactive.
- POS filter, giữ chip sau recreate, search Food và empty search.
- Bấm Food mở Flashcard với đúng code `FOOD`; API thật tải được trang có tối đa 100 thẻ, không tải toàn bộ chủ đề/kho vào RAM.
- Shortcut mở VocabularySearchActivity; avatar mở đúng Drawer END; Home ↔ Topics hoạt động.
- Runtime landscape thật ở font 1.0 và 1.3; bottom tab vẫn cố định.
- Đo resource/layout Topics ở 320×640, 412×915, 731×411dp × font 1.0/1.3: **6 tổ hợp PASS**. Dữ liệu fixture chỉ dùng trong phép đo layout test, không thay API của Activity chạy thật. Hồi quy Home còn kiểm tra 12 tổ hợp riêng.

Không ép Backend lỗi/ngắt mạng trong lượt này: trạng thái error/loading được nối lại ViewModel/Repository hiện có; unit tests kiểm tra lỗi/retry, nhưng ảnh runtime lỗi mạng riêng là **NOT_RUN**. Không tuyên bố tất cả trạng thái mạng đã nghiệm thu trực quan.

## Ảnh Emulator cuối

| Mặc định | Font 1.3 |
|---|---|
| [Phần đầu, tab active](../../android-app/runtime-evidence/m16-4-topics/topics-top.png) | [Phần đầu](../../android-app/runtime-evidence/m16-4-topics/font-1.3/topics-top.png) |
| [Phần giữa](../../android-app/runtime-evidence/m16-4-topics/topics-middle.png) | [Phần giữa](../../android-app/runtime-evidence/m16-4-topics/font-1.3/topics-middle.png) |
| [Nhóm POS](../../android-app/runtime-evidence/m16-4-topics/topics-pos.png) | [Nhóm POS](../../android-app/runtime-evidence/m16-4-topics/font-1.3/topics-pos.png) |
| [Tìm Food](../../android-app/runtime-evidence/m16-4-topics/topics-search.png) | [Tìm Food](../../android-app/runtime-evidence/m16-4-topics/font-1.3/topics-search.png) |
| [Không có kết quả](../../android-app/runtime-evidence/m16-4-topics/topics-empty.png) | [Không có kết quả](../../android-app/runtime-evidence/m16-4-topics/font-1.3/topics-empty.png) |
| [Drawer](../../android-app/runtime-evidence/m16-4-topics/topics-drawer.png) | [Drawer](../../android-app/runtime-evidence/m16-4-topics/font-1.3/topics-drawer.png) |
| [Landscape](../../android-app/runtime-evidence/m16-4-topics/topics-landscape.png) | [Landscape](../../android-app/runtime-evidence/m16-4-topics/font-1.3/topics-landscape.png) |

Đã xem lại ảnh phần đầu/giữa/POS/search/empty/Drawer và font lớn/landscape: icon render rõ, không bị đè header, chữ tên có thể xuống dòng khi cần, mô tả ellipsize một dòng đúng yêu cầu. [Ảnh hồi quy Home](../../android-app/runtime-evidence/m16-4-topics/home-regression.png) giữ bố cục M16.3; số liệu có thể thay đổi theo API thật.

## Đối chiếu demo và giới hạn

- So với HTML: giữ cấu trúc header → search → pill → section/card → bottom bar, palette và các kích thước được chỉ định. Card min-height 84dp, icon 48dp, padding ngang 24dp, gap 12dp, search 46dp; font lớn làm card cao hơn để không cắt tên.
- Khác biệt có chủ đích: 36 nhóm theo thứ tự catalog thay vì 10 topic Food đứng đầu; avatar phải thay hamburger trái; không “300 từ”/“30 từ mỗi topic”/badge đã nhớ giả; bổ sung chip semantic/POS; chỉ bốn tab, không Hồ sơ.
- Icon outline vector thay emoji HTML để nhất quán Android; Roboto theo app hiện hành thay font Inter tải từ web. Chip hiển thị cao 32dp nhưng có vùng chạm tối thiểu của Material.
- Chỉ nhận HTML, không có ảnh demo Topics riêng trong lượt này. Browser tool chặn protocol `file://`; không dùng đường vòng để render. Vì vậy đã đối chiếu source/thông số HTML với ảnh Android, **chưa có phép so sánh pixel bằng ảnh demo render** và không khẳng định giống 100%.
- Emulator thực vẫn native 1080×1920/density 420, override 370 như trước; hash AVD config không đổi. Không đổi size/density cho ảnh; chỉ tạm tăng font và xoay Activity để thử, sau đó khôi phục.
- Không thay Backend/Admin/PostgreSQL, không migration/import/reset, không sửa mapping M15.7/Quiz/SRS/XP/auth hoặc submit Review/Quiz/Favorite trong test. Catalog trước/sau khớp; không chạy snapshot checksum bảng người dùng hoặc truy vấn lại tổng Flashcard DB, nên không gọi đây là nghiệm thu DB mới. Baseline 20.000 Flashcard/38 topic DB theo hồ sơ trước được giữ ngoài phạm vi sửa.
- Chưa chạy điện thoại thật/AVD nhỏ riêng; màn nhỏ đã đo bằng instrumentation trên Emulator hiện tại. Git tại gốc không có repository được nhận diện, chưa xác định nhánh/diff/lịch sử; **CHƯA COMMIT/CHƯA PUSH**.

Evidence: `android-app/runtime-evidence/m16-4-topics/` chứa build logs, guard, instrumentation, ảnh, catalog trước/sau, mã băm và script `run-acceptance.ps1`. Dừng tại M16.4 để người dùng duyệt ảnh.
