# BiVocal — Báo cáo Home M16.3 để gửi ChatGPT

> Cập nhật sau bản bàn giao: đã thực hiện Final UI Polish theo yêu cầu mới ngày 27/10/2026. Đọc [báo cáo và ảnh mới nhất](M16_3_HOME_FINAL_POLISH_REPORT.md): slogan 12sp/hai dòng, Info 48dp, SRS đồng bộ nền/viền và CTA động, leaderboard tên XP; unit 81/81, instrumentation 3/3 lượt PASS. Những giới hạn “chưa chạy lại test header” và nhãn leaderboard tuần trong bản tổng hợp bên dưới là lịch sử trước polish.

Ngày tổng hợp: 27/10/2026. Phạm vi: Home M16.3 và các chỉnh sửa theo phản hồi gần nhất. Bản này tổng hợp source hiện tại và bằng chứng trong báo cáo kỹ thuật; không chạy lại ứng dụng, build, test hoặc chụp ảnh trong lượt viết báo cáo.

## 1. Bối cảnh

- Android dùng Java/XML, MVVM, Retrofit; dữ liệu thật qua Backend Express/JWT. Runtime Online-Only, không đưa Room trở lại.
- Hồ sơ gần nhất ghi production có 20.000 Flashcard và 38 nhóm/chủ đề. Đây là số liệu lịch sử đã ghi nhận, không phải truy vấn database mới trong lượt này.
- Home được triển khai từ `home_demo.html` và ảnh demo, sau đó thay đổi theo yêu cầu trực tiếp của người dùng. Những quyết định mới dưới đây được ưu tiên hơn hình demo cũ.
- Trạng thái giao diện: **READY_FOR_ACCEPTANCE — chờ người dùng duyệt**. Không tuyên bố giống demo 100% hoặc toàn milestone đã DONE.

## 2. Giao diện hiện tại và những phần đã làm

| Khu vực | Trạng thái mới nhất |
|---|---|
| Header trái | Mascot chính thức và slogan “Mỗi ngày 15 phút, bứt phá vốn từ cùng BiVocal!”. Chữ BiVocal được nhấn màu cyan. Đã bỏ avatar, tên và lời chào bên trái. |
| Header phải | Giữ badge số ngày liên tục; thay nút hamburger bằng avatar người dùng. Bấm avatar mở Drawer bên phải như menu cũ. |
| Daily Goal | Hiển thị số từ mới, mục tiêu, phần trăm, thanh tiến độ và nút học từ mới từ API thật. Giữ nghiệp vụ M15. |
| Quick Stats | Đã bỏ hàng ba ô Tổng XP / Ngày liên tục / Từ đã học theo yêu cầu. Không xóa dữ liệu thống kê phía Backend. |
| SRS | Đổi thành card “Độ thuộc từ vựng (SRS)”, tổng từ, phần trăm thuần thục, thanh bốn màu, bốn ô chú giải và nút ôn đến hạn. |
| Leaderboard | Giữ preview thứ hạng/XP thật, làm nổi bật người hiện tại khi có trong kết quả. “Tất cả” mở màn bảng xếp hạng hiện có. |
| Footer mascot/slogan | Đã bỏ card ở cuối Home. Logo/slogan chỉ còn trên header. |
| Thanh dưới | Cố định bốn tab chia đều: Trang chủ, Chủ đề, Ôn tập, Trắc nghiệm. Đã bỏ tab Hồ sơ. |
| Hồ sơ/Cài đặt | Vẫn truy cập qua Drawer mở từ avatar bên phải; không tạo luồng điều hướng mới. |

Home giữ nền tối, card bo góc, màu cyan/blue và nút gradient. Nội dung giữa cuộn được; header và thanh tab nằm ngoài vùng cuộn. Các lượt sửa Home không thay đổi kích thước hoặc density AVD để làm giống ảnh demo.

## 3. Cách đọc card SRS

| Nhóm hiển thị | Mức nhớ do Backend trả về | Màu |
|---|---|---|
| Biết mới | 0–1 | Xanh dương |
| Đang ôn | 2–3 | Tím |
| Nhớ tốt | 4 | Xanh lá |
| Thuần thục | 5 | Cam |

- Nhóm chỉ đếm thẻ đã có lượt ôn theo API, không cộng thẻ chỉ được Favorite.
- Phần trăm thuần thục = số thẻ mức 5 / tổng thẻ đã ôn, làm tròn số nguyên. Đây không phải tỷ lệ trả lời đúng.
- Tổng bằng 0 thì hiển thị 0%; dữ liệu thiếu/không hợp lệ hiển thị dấu “—”. Nhóm có số lượng 0 không ép vẽ một đoạn màu giả.
- Số cần ôn lấy từ `dueNow`. Khi bằng 0, hiển thị chưa có từ đến hạn và vô hiệu hóa CTA.
- Nút “?” mở giải thích; “Chi tiết” mở Progress; CTA ôn dùng luồng Review Today đang có. Không sửa thuật toán SRS, XP hoặc lịch ôn.

## 4. Dữ liệu thật và giới hạn bảng xếp hạng

Home tái sử dụng ProfileViewModel, ProgressViewModel và LeaderboardViewModel; không tạo Repository/ViewModel trùng. Daily Goal dùng `learning/today`, SRS dùng `progress/schedule`, streak dùng `gamification/me`, bảng xếp hạng dùng `gamification/leaderboard?limit=50`, dưới prefix `/api/v1/`.

- Home preview tối đa ba hàng: hai người đứng đầu và người hiện tại nếu có trong danh sách API; không lặp hàng khi người hiện tại đã ở top hai.
- Màn “Tất cả” dùng danh sách cuộn, hiện tải tối đa 50 người có XP; chưa có phân trang hoặc endpoint lấy hạng riêng của người ngoài top 50.
- Backend hiện xếp theo tổng XP tích lũy. Tiêu đề còn chữ “tuần”, nhưng chú thích ghi rõ chưa có xếp hạng tuần; không tạo giải đấu hoặc điểm tuần giả.
- Home không tải toàn bộ 20.000 Flashcard vào RAM, không gán số liệu minh họa của demo vào ứng dụng thật.

## 5. Các file chính đã thêm/chỉnh trong quá trình M16.3

Các đường dẫn dưới đây tính từ gốc project; đây là danh sách phạm vi chính, không phải Git diff của lượt viết báo cáo.

- `android-app/app/src/main/res/layout/activity_home.xml`: cấu trúc Home, header, Daily Goal, leaderboard, bốn tab; bỏ Quick Stats/footer.
- `android-app/app/src/main/res/layout/view_home_srs.xml`: card SRS bốn nhóm.
- `android-app/app/src/main/res/layout/item_home_leaderboard.xml`: hàng preview bảng xếp hạng.
- `android-app/app/src/main/res/values/home_frontend.xml`, `home_srs.xml`, các drawable `home16_*`/`home_srs_*`: text, style, màu, nền và icon.
- `android-app/app/src/main/java/com/example/bivoca/ui/home/HomeActivity.java`: binding, render dữ liệu và tái sử dụng các hành động hiện có.
- `android-app/app/src/main/java/com/example/bivoca/ui/home/HomePresentation.java`: nhóm SRS và lựa chọn hàng preview.
- `NavigationDrawerController.java`: tích hợp Home header ở giai đoạn đầu M16.3; lượt chuyển avatar sang phải gần nhất giữ nguyên controller.
- `AndroidManifest.xml`: theme Home riêng ở giai đoạn đầu M16.3.
- `HomePresentationTest.java`, `HomeResponsiveUiTest.java`, `HomeRuntimeUiTest.java`: kiểm tra trình bày, responsive và điều hướng; đã bỏ tham chiếu UI cũ.

Không sửa Backend, PostgreSQL, authentication, Google Sign-In, Login/Register/Forgot, nội dung Flashcard hoặc thuật toán học trong các lượt tinh chỉnh Home này. APK được cài cập nhật bằng `adb install -r`, giữ dữ liệu ứng dụng.

## 6. Kiểm thử đã ghi nhận — phân biệt từng phiên

| Phiên | Lệnh/thao tác và kết quả được ghi nhận |
|---|---|
| Refinement 25/10 | `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` PASS; unit 81/81; lint 0 lỗi/380 cảnh báo; HomeResponsiveUiTest + HomeRuntimeUiTest 2/2 PASS. Đây là bản trước các phản hồi mới. |
| SRS bốn nhóm 26/10 | Build/unit/lint/test-APK PASS; unit 81/81, lint 0 lỗi/391 cảnh báo; responsive instrumentation 1/1 PASS với 8 tổ hợp size/font. Đã kiểm tra trạng thái 0 từ/0 đến hạn và dialog giải thích. |
| Bốn tab 27/10 | `:app:assembleDebug :app:assembleDebugAndroidTest` PASS; APK chính cài `-r`; UI dump xác nhận bốn tab chia đều. Unit/lint/instrumentation thực thi: NOT_RUN cho lượt này. |
| Header thương hiệu/avatar 27/10 | `:app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest` PASS; lint 0 lỗi/397 cảnh báo. Runtime mở Drawer từ avatar và Back về Home PASS. Unit/instrumentation thực thi: NOT_RUN. |
| Bỏ footer 27/10 — thay đổi app cuối cùng | `:app:assembleDebug :app:assembleDebugAndroidTest` PASS trong 7 giây; APK chính cài `-r`. Unit/lint/instrumentation thực thi: NOT_RUN. Test APK chỉ biên dịch, không cài lại. |
| Lượt tạo báo cáo này | Build/unit/lint/runtime: NOT_RUN theo phạm vi chỉ viết tài liệu; không chụp ảnh mới. |

Trong nghiệm thu M16.3 ban đầu, snapshot chỉ đọc của 23 bảng trước/sau runtime ghi nhận không đổi checksum. Đây là bằng chứng lịch sử, không phải kiểm tra DB mới sau mỗi thay đổi UI.

## 7. Phần còn chờ và giới hạn

- Người dùng duyệt bố cục Home hiện tại qua ảnh mới hoặc quan sát trên Emulator.
- Chưa chạy lại toàn bộ unit/instrumentation sau hai thay đổi header và bỏ footer gần nhất. Build test APK không đồng nghĩa đã chạy test.
- Chưa kiểm thử runtime CTA SRS ở trạng thái `dueNow > 0` trong lượt đổi card; không tạo lượt học giả để dựng số liệu.
- Responsive đã được kiểm tra ở các phiên trước, nhưng chưa nghiệm thu lại đầy đủ màn nhỏ/font 1.3 cho header mới nhất.
- Ảnh Demo–Before–After và ảnh runtime cũ có thể còn Quick Stats, năm tab, avatar trái hoặc footer; không dùng chúng làm ảnh bản hiện tại.
- Git tại thư mục gốc báo không có repository; chưa xác định nhánh/diff/lịch sử. CHƯA COMMIT/CHƯA PUSH.

## 8. Bạn cần tự chụp gì để gửi ChatGPT?

Chụp bản đang cài mới nhất, không cần dựng số liệu mẫu hoặc thao tác học để tăng số.

1. **Home phần trên:** thấy mascot/slogan bên trái, streak/avatar bên phải và toàn bộ Daily Goal.
2. **Toàn bộ card SRS:** thấy tổng từ, phần trăm, thanh màu, bốn ô và hàng ôn đến hạn. Có thể gộp với ảnh 1 nếu màn hình hiển thị đủ.
3. **Home cuộn xuống cuối:** thấy leaderboard và bốn tab dưới; xác nhận không còn card mascot/slogan cuối trang.
4. **Drawer đang mở:** bấm avatar bên phải rồi chụp menu, thấy các mục Hồ sơ/Cài đặt vẫn còn.

Thông thường cần 3–4 ảnh. Không cần chụp Login/Register/Forgot hoặc toàn bộ các chủ đề cho lần góp ý Home này.

## 9. Nội dung gửi kèm cho ChatGPT

“Đây là báo cáo Home M16.3 mới nhất của BiVocal và ảnh ứng dụng thực tế. Hãy đọc báo cáo rồi nhận xét bố cục, khoảng cách, typography và khả năng sử dụng. Quyết định hiện tại là logo/slogan trái, avatar phải mở Drawer, SRS bốn nhóm, bốn tab dưới, không Quick Stats và không card slogan cuối trang. Đừng đề xuất khôi phục bố cục cũ chỉ vì ảnh demo có các phần đó. Phân biệt dữ liệu API thật với số minh họa; chưa thay đổi Backend hoặc thuật toán học. Hãy chỉ ra các chỉnh sửa UI cụ thể để tôi giao lại Codex.”

Nguồn chi tiết: [báo cáo M16.3](M16_3_HOME_FRONTEND_REPORT.md), [tiến độ](../../WORK_STATUS.md), [bối cảnh Android](../../ANDROID_CONTEXT.md). Lượt bàn giao này chỉ tạo báo cáo và cập nhật mục lục/tiến độ, không sửa source ứng dụng.
