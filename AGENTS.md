# AGENTS.md — Quy tắc bắt buộc cho AI Coding Agent trên BiVoca

## 1. NGUYÊN TẮC BẮT BUỘC (CARDINAL RULES)
1. **Đọc source code trước khi sửa:** Không suy đoán API endpoint, tên trường dữ liệu, cấu trúc cơ sở dữ liệu hay hành vi chức năng. Luôn đọc file code, route, schema trước.
2. **Không rewrite phần đã chạy:** Giữ nguyên các phần code, database schema, migration và chức năng đã hoạt động nếu không có lỗi kỹ thuật hoặc yêu cầu cụ thể.
3. **Không tạo lớp trùng lặp:** Không tạo thêm Activity, Repository, Model, Service hoặc ViewModel trùng với lớp hiện có.
4. **Không thay đổi kiến trúc:**
   - Backend: Route → Controller → Service → Repository → PostgreSQL.
   - Android: MVVM (Activity + ViewModel + LiveData + Repository + Retrofit), sử dụng Java và XML. Không chuyển sang Kotlin hoặc Jetpack Compose.
5. **Backend là cổng giao tiếp duy nhất:** Android tuyệt đối không kết nối trực tiếp PostgreSQL. Mọi dữ liệu phải qua REST API `/api/v1/`.
6. **Không dùng dữ liệu giả (mock):** Không thay thế API thật bằng mock data để tạo giao diện giả hoàn thành. Nếu API chưa có, phải hoàn thiện backend hoặc ghi nhận chưa kết nối.
7. **Bảo mật tuyệt đối:** Không commit, không log, không in ra màn hình mật khẩu, token JWT, connection string hay bí mật môi trường trong `.env`.
8. **Kiểm thử trước khi kết luận:** Không tuyên bố tính năng hoàn thành nếu chưa chạy lệnh build/test thực tế có bằng chứng xác thực.

---

## 2. CẤU TRÚC VÀ ĐƯỜNG DẪN DỰ ÁN
- **Thư mục gốc:** thư mục chứa file `AGENTS.md` này; không phụ thuộc tên tài khoản hoặc đường dẫn máy cá nhân.
- **Các module:**
  - `backend`: REST API Node.js/Express 5, TypeScript strict, pg.
  - `android-app`: Ứng dụng Android Java/XML, Gradle wrapper 9.5.0, AGP 9.3.3, compile/target SDK 37, module `:app`.
  - `admin-web`: Giao diện quản trị React 19, Vite, TailwindCSS.
  - `dataset`: Dữ liệu gốc 10 chủ đề, 300 từ vựng (CSV, SQL).
  - `maintenance`: Script tự động khởi động và bảo trì local admin trên Windows.

---

## 3. LỆNH VẬN HÀNH VÀ KIỂM THỬ

### Backend
Chạy từ thư mục gốc project; đọc lại `backend/package.json` trước khi dùng.
```powershell
Set-Location .\backend
npm run typecheck    # Kiểm tra kiểu TypeScript (phải PASS không lỗi)
npm test             # Unit/HTTP tests; ghi số test từ kết quả thực tế, không cố định 311
npm run dev          # Chạy dev server tại 127.0.0.1:3000
npm run migrate:up   # Chỉ chạy khi migration và database đích đã được cho phép
```
PostgreSQL integration dùng các script `test:*integration` riêng theo phạm vi; phải đọc guard và xác minh database đích, không mặc định `npm test` đã kiểm thử database thật.

### Android App
Chạy từ thư mục gốc project; xác minh wrapper/module/variant trước khi dùng.
```powershell
Set-Location .\android-app
.\gradlew.bat assembleDebug       # Biên dịch kiểm tra lỗi toàn bộ dự án
.\gradlew.bat testDebugUnitTest   # Chạy bộ unit test của Android
```

### Kết nối thiết bị Android thật với Backend local
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:3000 tcp:3000
```
Emulator được `ApiBaseUrl` chọn `http://10.0.2.2:3000/api/v1/`; build `e2e` override cổng 3001. Thiết bị thật dùng `127.0.0.1:3000` qua ADB reverse. Đây là cấu hình source, không chứng minh server hoặc thiết bị đang chạy.

---

## 4. QUY TRÌNH PHÁT TRIỂN TÍNH NĂNG MỚI
1. Tra cứu route, controller, service và schema ở `backend/src/modules/`.
2. Xác định request/response DTO (`camelCase`).
3. Tìm và tái sử dụng Model tương ứng trong `android-app/app/src/main/java/com/example/bivoca/data/model/`; chỉ tạo nếu chưa có.
4. Khai báo API interface trong `data/api/`.
5. Mở rộng Repository hiện có trong `data/repository/`, sử dụng `AuthenticatedRetrofitClient` hoặc `RetrofitClient` để tạo API interface; Repository không kế thừa Retrofit client.
6. Tạo ViewModel quản lý state bằng LiveData.
7. Tạo Layout XML và Activity tương ứng, đăng ký vào `AndroidManifest.xml`.
8. Chạy `.\gradlew.bat assembleDebug` để đảm bảo code biên dịch sạch.
9. Cập nhật `WORK_STATUS.md`.

---

## 5. ĐỌC BỐI CẢNH VÀ PHỐI HỢP

- Đầu phiên đọc `context.md`, `memory.md`, đầu `WORK_STATUS.md` và tài liệu kỹ thuật liên quan: `ANDROID_CONTEXT.md`, `BACKEND_CONTEXT.md`, `PROJECT_CONTEXT.md`, báo cáo trong `docs/reports/`. File thiếu thì báo rõ, không bịa nội dung.
- `context.md` là bản đồ bối cảnh và nguồn dẫn; `memory.md` lưu quyết định/bài học lâu dài; `WORK_STATUS.md` lưu tiến độ và bàn giao. Hai file trí nhớ được đọc theo hướng dẫn này, không phải bộ nhớ tự động của Codex.
- Source/cấu hình xác định hiện trạng kỹ thuật; quyết định người dùng đã duyệt xác định yêu cầu. Ghi chú cũ không ghi đè cập nhật mới. Khi mâu thuẫn, nêu nguồn/ngày và phần chưa chắc; không tự sửa quyết định nghiệp vụ.
- Trước khi sửa, kiểm tra `git status --short --branch`, `git diff`, `git diff --cached`, `git log -5 --oneline` nếu có repository. Không ghi đè hoặc hoàn tác thay đổi của người dùng/AI khác; chỉ dừng phần có xung đột chưa rõ chủ sở hữu.
- Nếu Git báo không có repository: ghi rõ chưa xác định được nhánh/diff/lịch sử, dùng nội dung và mã băm trước/sau cho phạm vi đã được duyệt. Không tự `git init`, tạo remote hoặc đoán nhánh; việc ghép tài liệu đã duyệt vẫn có thể tiếp tục.
- Codex/Antigravity cùng dùng các tài liệu bàn giao; không coi bản nhập trong `_codex_import/` là cấu hình đang áp dụng.

## 6. PHẠM VI TỰ CHỦ VÀ GIT

- Việc nhỏ, cục bộ, có thể hoàn tác: được sửa/test trong phạm vi yêu cầu. Không tự refactor rộng hoặc đổi UI đã chốt.
- Thay đổi lớn về UI/UX, nghiệp vụ, authentication, API contract, database/schema/migration, kiến trúc, công nghệ/dependency quan trọng, nhiều module hoặc triển khai/phát hành: trình phương án, file/module, rủi ro, kiểm thử và cách quay lại; chờ duyệt trước khi sửa. Thay đổi UI lớn cần mô tả/preview. Áp dụng phê duyệt đã có đúng phạm vi, không hỏi lại; phê duyệt không mở rộng quyền ngoài phạm vi đó hoặc bỏ các nguyên tắc ở mục 1.
- Chỉ tự commit/push việc nhỏ khi người dùng đã cho phép hành động đó và chỉ định rõ nhánh feature cho phạm vi công việc; phải kiểm tra diff, test, branch và remote đích. Chỉ định tên nhánh riêng lẻ không thay thế quyền push.
- Chưa có quyền hoặc chưa rõ nhánh: hoàn thành phần đã được giao, báo CHƯA COMMIT/CHƯA PUSH. Không tự tạo/đổi nhánh để lách giới hạn; không push `main`/`master` hoặc nhánh bảo vệ.
- Stage đúng file/hunk; không gom thay đổi của AI khác bằng `git add -A`. Không tự merge/rebase nhánh chính, force-push, `git reset --hard`, `git clean -fd`, xóa branch hoặc đổi lịch sử.
- Duyệt tích hợp tài liệu không đồng nghĩa cho phép migration, sửa production, phát hành APK hoặc commit/push. Thay đổi lớn cần quyền Git riêng nếu phê duyệt chưa bao gồm.

## 7. KIỂM THỬ VÀ NGHIỆM THU

- Android ưu tiên Emulator; kiểm tra thiết bị thật bằng `adb devices -l`, không mặc định có AVD đang chạy. Chọn build/unit/lint/instrumentation/runtime theo thay đổi; chỉ sửa tài liệu/skill thì kiểm tra cấu trúc, YAML, liên kết và nội dung phù hợp, không cần build ứng dụng.
- Giữ `android.injected.androidTest.leaveApksInstalledAfterRun=true`. Trước instrumentation trên thiết bị có dữ liệu, xác minh `keepInstalledApks=true`, `uninstallIncompatibleApks=false`; không tự uninstall/clear data, reset DB hoặc xóa dữ liệu để làm test chạy. Cài cập nhật giữ dữ liệu theo hướng dẫn nghiệm thu hiện có.
- Backend/API phải kiểm thử phù hợp Backend và tích hợp liên quan; build Android riêng không chứng minh Backend đúng. Không dùng database vận hành cho test có ghi khi chưa có quyền.
- Mỗi kiểm tra ghi `PASS`, `FAIL`, `NOT_RUN` hoặc `BLOCKED`, lệnh/thao tác, bằng chứng và giới hạn; phân biệt chạy lại thật với kết quả cache/up-to-date và bằng chứng lịch sử.
- `TECHNICAL_COMPLETE`: việc nhỏ đạt kiểm tra kỹ thuật phù hợp. Tính năng lớn/milestone tối đa `READY_FOR_ACCEPTANCE` cho tới khi người dùng xác nhận nghiệm thu `PASS`. `PASS` của một lệnh test không phải `PASS` của milestone.
- Không đổi lịch sử DONE/PASS, tự tạo ngày nghiệm thu hoặc nâng trạng thái chỉ vì build thành công. M16.1–M16.3 đang chờ duyệt UI theo hồ sơ hiện có; tích hợp bộ hướng dẫn không thay đổi trạng thái này. M8 Room là lịch sử, không khôi phục vào runtime Online-Only từ M13.8.

## 8. BẢO MẬT, TRÍ NHỚ VÀ BÀN GIAO

- Không đưa thông tin học tập cá nhân, tên tài khoản, đường dẫn máy, email, ảnh tài khoản hoặc dữ liệu xác thực vào các file trí nhớ/skill khi không cần cho công việc. Khi rà soát secret, chỉ báo file/vị trí/loại, không in giá trị.
- Trước commit/push kiểm tra cả file được theo dõi, log, ảnh, archive và evidence; `.gitignore` không gỡ file đã bị theo dõi hoặc bí mật trong lịch sử. Không tự mở/đổ toàn bộ `.env` ra output.
- Mỗi mục trí nhớ ghi loại (quyết định được duyệt/đã kiểm chứng/cần xác minh), nguồn và ngày nếu biết. Không sao chép hồ sơ dự thảo thành quyết định đã duyệt; khi thay quyết định, bổ sung lý do/lịch sử thay vì âm thầm xóa.
- Cuối phiên bổ sung bàn giao vào `WORK_STATUS.md` khi phạm vi cho phép, giữ nguyên nội dung lịch sử. Trả lời tiếng Việt ngắn gọn: kết quả, lý do, file liên quan, kiểm thử và giới hạn, cách nghiệm thu phù hợp, Git/bàn giao; không ép hướng dẫn Emulator cho công việc chỉ sửa tài liệu.
- Với kiểm thử/nghiệm thu/bàn giao BiVoca, đọc và áp dụng `.agents/skills/bivoca-quality-handoff/SKILL.md`. Skill hỗ trợ quy trình, không cấp thêm quyền sửa hoặc Git.
