# M14 — Google Sign-In & Password Recovery

**Ngày kiểm tra:** 2026-10-23  
**Trạng thái:** `GOOGLE_PRODUCTION_DONE / PASSWORD_RECOVERY_LOCAL_DONE / PASSWORD_RECOVERY_PUBLIC_HTTPS_PENDING`  
**Database vận hành:** migration M14 đã áp dụng và kiểm tra  

## 1. Kết quả hiện tại

Source M14 đã hoàn thiện cho Backend, PostgreSQL migration, Android Google Credential
Manager, liên kết tài khoản, Forgot Password, SMTP sender và trang đặt lại mật khẩu.
Migration đã qua Up/Down/Up và integration test trên database cô lập. Google Sign-In E2E đã
PASS trên Emulator với tài khoản thật, liên kết an toàn vào user hiện có, JWT BiVoca và Home.
Google Sign-In production và Password Recovery local đã hoàn tất. Chưa dùng trạng thái
`M14_DONE` vì reset URL vẫn là localhost HTTP, chưa có HTTPS public cho điện thoại/máy ngoài.

### Triển khai production — 2026-10-23

- Backup `flashcard_english_before_m14_20261023152956.dump` tồn tại, kích thước
  **5.615.598 byte**, SHA-256
  `6A9D3B30707A2FAAD672C5778D455C64CF9F5C781680DFC3B931F87D51151AF9`.
  Restore-check mới vào database cô lập `bivoca_m14_restore_final_20261023` PASS; toàn bộ counts
  và năm checksum users/progress/quiz/scores/xp khớp baseline backup.
- Migration `1792702800000_add_federated_auth_and_password_recovery` đã chạy đúng một lần trên
  `flashcard_english`. Nullable password, `auth_version`, ba bảng M14, FK, unique constraint và
  index đều PASS. Ngay sau migration, counts và checksum dữ liệu cũ không đổi.
- Launcher cũ được dừng bằng `node maintenance/local-admin.mjs --stop`, sau đó khởi động sạch.
  Backend production hiện chạy `node backend/dist/server.js` trên cổng 3000. Health 200,
  Google challenge 200, body Google thiếu 400 và token thử sai 401; không còn 404.
- Trên APK Debug, người dùng đăng nhập password và chủ động liên kết Google với user 55. Sau đó
  logout → Google Account Chooser → đăng nhập Google lại → Home PASS. Force-stop/relaunch vẫn
  giữ session. PostgreSQL giữ **3 users / 1 identity / 1 linked user**; linked user giữ
  **10 progress / 9 Quiz / 30 XP**, không tạo tài khoản trùng. Ảnh:
  `android-app/M14_google_production_home.png`.
- Snapshot production cuối: **5.000 cards / 19 topics / 3 users / 14 progress / 12 Quiz /
  2 scores / 5 XP / 1 ADMIN / 0 passwordless user**.

### Hotfix `Route not found` — 2026-10-23

Nguyên nhân đã được tái hiện bằng HTTP và ADB, không suy đoán:

- APK Debug (`com.example.bivoca`) dùng `http://10.0.2.2:3000/api/v1/` trên Emulator.
  `POST /auth/google/challenge` tại cổng 3000 trả HTTP 404,
  `ROUTE_NOT_FOUND`. Tiến trình cổng 3000 là `node backend/dist/server.js`, do Windows launcher
  quản lý và đã nạp bản build cũ trước M14 vào bộ nhớ.
- APK E2E (`com.example.bivoca.e2e`) dùng
  `http://10.0.2.2:3001/api/v1/`. Tiến trình cổng 3001 là `tsx src/server.ts` trên database
  `bivoca_m14_20261023`; challenge trả HTTP 200. `POST /auth/google` với body rỗng trả HTTP 400
  `VALIDATION_ERROR`; token thử sai đúng định dạng trả HTTP 401 `GOOGLE_TOKEN_INVALID`.
  Điều này xác nhận route, controller, service và Google Web Client ID đều đang được nạp.
- Lỗi 404 xảy ra ở request xin challenge, trước khi Credential Manager nhận challenge và trước
  khi có Google ID Token.
- Production `flashcard_english` chưa có migration M14. Vì vậy tiến trình cổng 3000 không được
  restart sang build M14 trong hotfix này; làm vậy trước migration có thể gây lỗi schema cho auth.

Android đã sửa `AuthRepository` để ánh xạ riêng endpoint thiếu, lỗi mạng, OAuth chưa cấu hình,
ID Token/challenge không hợp lệ, yêu cầu liên kết, rate limit và lỗi máy chủ sang thông báo tiếng
Việt. Chuỗi kỹ thuật từ response Backend như `Route not found` không còn được hiển thị trực tiếp.
APK E2E mới đã cài bằng `adb install -r`. ADB ghi nhận chính xác
`POST http://10.0.2.2:3001/api/v1/auth/google/challenge` → HTTP 200, sau đó mở giao diện Google
thật để thêm/chọn tài khoản. Ảnh: `android-app/M14_hotfix_google_account.png`.

Lần chọn tài khoản đầu tiên sau restart Backend trả HTTP 401. Dữ liệu cho thấy challenge chưa
được consume, nên lỗi nằm trong token verifier. `google-token.service.ts` được bổ sung mã lỗi
an toàn theo nhóm audience/time/claims/nonce mà không log token. Web Client ID của Android và
Backend đều khớp cấu hình đã duyệt. Lần thử kế tiếp xác minh token thành công và trả HTTP 409
`GOOGLE_ACCOUNT_LINK_REQUIRED`, vì email Google đã thuộc một tài khoản BiVoca có password.
Không có user hay identity mới được tạo ở nhánh 409.

Người dùng đăng nhập tài khoản BiVoca và liên kết Google trong Profile. Sau đó nghiệm thu:

- identity Google liên kết đúng user ID 55; `users=3`, `identities=1`, một Google subject duy nhất;
- logout đưa về Login và xóa credential state của ứng dụng;
- Google challenge HTTP 200, Google login HTTP 200, Android nhận JWT BiVoca và vào Home;
- `auth/me`, progress summary/schedule và gamification đều HTTP 200;
- force-stop rồi mở lại vẫn vào Home bằng session đã lưu;
- user ID 55 giữ 10 progress, 9 Quiz và 30 XP; không tạo tài khoản/identity trùng.

Ảnh kết quả: `android-app/M14_google_relogin_home.png`.

## 2. Kiến trúc xác thực

- Android dùng Credential Manager `1.6.0` và Google ID `1.2.1`, với
  `GetSignInWithGoogleOption` cho nút riêng.
- Android dùng Web OAuth Client ID làm server client ID; Debug/E2E Android Client ID và SHA
  đã được cấu hình trên Google Cloud theo thông tin người dùng cung cấp.
- Backend tạo challenge 5 phút, trả nonce ngẫu nhiên và chỉ lưu SHA-256 của nonce. Google ID
  Token được xác minh bằng `google-auth-library`, gồm signature, audience, issuer/expiry do thư
  viện kiểm tra, `sub`, email verified và nonce.
- Backend cấp JWT BiVoca cũ, thêm claim `av` (auth version). Password reset tăng `auth_version`,
  làm JWT cũ mất hiệu lực trên middleware bảo vệ.
- `user_auth_identities` dùng `(provider, provider_subject)` làm định danh. Email trùng tài khoản
  cũ không tự gộp; liên kết yêu cầu JWT hiện hành, nhập lại mật khẩu và Google email trùng.
- Google-only user có `password_hash = NULL`, role mặc định `USER`; không tạo mật khẩu giả và
  không nhận role/user ID từ Android.

## 3. Endpoint mới

- `POST /api/v1/auth/google/challenge`
- `POST /api/v1/auth/google`
- `POST /api/v1/auth/google/link` (Bearer JWT + mật khẩu xác minh lại)
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `GET /reset-password` (responsive, same-origin)

Trang reset gửi `Cache-Control: no-store`, `Referrer-Policy: no-referrer`, CSP hạn chế,
`X-Content-Type-Options: nosniff`; token được bỏ khỏi thanh địa chỉ trước request và không có
third-party script.

## 4. PostgreSQL migration và an toàn dữ liệu

Migration: `backend/migrations/1792702800000_add_federated_auth_and_password_recovery.sql`.

- `users.password_hash` cho phép NULL; thêm `auth_version >= 1`.
- Thêm `user_auth_identities`, `google_auth_challenges`, `password_reset_tokens` với unique,
  FK cascade và index cần thiết.
- Reset token ngẫu nhiên 32 byte, chỉ lưu SHA-256, hết hạn 20 phút, một lần sử dụng; cập nhật
  password, auth version và vô hiệu hóa các token còn lại trong cùng transaction.
- Down migration từ chối chạy nếu còn Google-only user, tránh ép NOT NULL làm mất dữ liệu.

Backup mới được giữ cục bộ tại:

`C:\Users\phuc\AppData\Local\BiVoca\backups\flashcard_english_before_m14_20261023152956.dump`

Kích thước: 5.615.598 byte. Restore vào `bivoca_m14_20261023` PASS. Migration trên bản sao:
Up → Down → Up PASS. Integration test tạo/xóa đúng user/challenge riêng và snapshot sau cleanup
khớp baseline.

| Snapshot | Cards | Topics | Users | Progress | Quiz | Scores | XP | M14 schema |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| `flashcard_english` sau production deploy | 5.000 | 19 | 3 | 14 | 12 | 2 | 5 | Đầy đủ |
| `bivoca_m14_20261023` sau test | 5.000 | 19 | 3 | 14 | 12 | 2 | 5 | Đầy đủ |

## 5. Password Recovery và email

`EmailSender` đọc `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_SECURE`,
`MAIL_FROM`, `PUBLIC_RESET_URL`; không hardcode credential. Response Forgot Password giống nhau
cho email có/không tồn tại. Google-only account không tự tạo password và không gửi reset.

Source gửi email và reset page đã hoàn thiện, nhưng `.env` hiện thiếu SMTP. Trên Emulator,
Forgot Password gọi Backend thật cổng 3001 và nhận đúng lỗi cấu hình; Android không báo thành
công giả. Để nghiệm thu email thật cần SMTP và URL HTTPS public ở môi trường triển khai.

`EmailSender` hiện bắt buộc STARTTLS khi `SMTP_SECURE=false` (cấu hình Gmail cổng 587), có
3 unit tests cho guard thiếu cấu hình, STARTTLS/HTML escaping và TLS cổng 465. Script
`maintenance/configure-smtp.ps1` cho phép nhập Gmail và Google App Password bằng prompt ẩn,
không in password; functional test trên file `.env` tạm PASS và file tạm đã xóa. Production
`POST /api/v1/auth/forgot-password` hiện trả HTTP 503 `SMTP_CONFIGURATION_REQUIRED`, đúng trạng
thái thật. `GET /reset-password` trả HTTP 200; URL localhost chỉ dùng được trên cùng máy.

Sau khi người dùng nhập cấu hình Gmail cục bộ, Backend đã được restart sạch và nạp đủ 7/7 biến.
Kiểm tra thật tới `smtp.gmail.com:587` đã kết nối được nhưng Gmail trả `EAUTH`, lệnh
`AUTH PLAIN`, mã **535**; App Password hiện tại chưa được Gmail chấp nhận. Request Forgot vì
vậy trả HTTP 500 an toàn, không báo gửi thành công giả. Chưa có email được gửi/nhận và chưa thực
hiện reset mật khẩu. Script `backend/scripts/verify-smtp.ts` (`npm run smtp:verify`) chỉ in mã
chẩn đoán an toàn, không in email, password, nội dung thư hoặc token.

Người dùng sau đó tạo App Password mới. `npm run smtp:verify` trả `SMTP_VERIFY_OK`; Backend
production được restart và Forgot Password trả HTTP 200. Người dùng xác nhận nhận email thật,
mở link trên cùng máy Windows và reset thành công. PostgreSQL xác nhận `auth_version=2`,
**0 active reset token** và **2 token đã used**. Kiểm tra HTTP production bằng token tạm chỉ
trong bộ nhớ: JWT `av=1` trả 401 `AUTH_TOKEN_INVALID`, JWT `av=2` trả 200; token không được
in hoặc lưu. Database xác nhận password login sau lần Google identity login gần nhất. Emulator
đang ở Home; auth/progress/gamification HTTP 200 và dữ liệu học không đổi.

### 5.1. Làm rõ tài khoản chỉ đăng nhập bằng Google

Người dùng tạo thêm một tài khoản bằng Google trên AVD rồi nhập chính email đó ở màn Quên mật
khẩu. Đây là tài khoản Google-only có `password_hash = NULL`, nên không có mật khẩu BiVoca để
đặt lại. Backend vẫn trả HTTP 200 giống email không tồn tại nhằm tránh lộ trạng thái tài khoản,
nhưng không tạo reset token và không gửi email. Snapshot sau thao tác có 4 users, 2 Google
identities, 1 tài khoản không có password và 0 active reset token; 5.000 Flashcard, 19 topics,
14 progress, 12 Quiz, 2 scores và 5 XP history được giữ nguyên.

Thông báo chung của Forgot Password đã được sửa để nêu rõ: email có tài khoản mật khẩu sẽ nhận
hướng dẫn; tài khoản tạo bằng Google phải quay lại và chọn `Tiếp tục với Google`. Màn Android
cũng giải thích điều này trước khi gửi yêu cầu. Backend đã build lại, restart cổng 3000 và APK
Debug đã cài bằng `adb install -r`; giao diện thực tế trên AVD hiển thị đầy đủ nội dung mới.
Backend typecheck/build và 335/335 tests PASS; Android assembleDebug, testDebugUnitTest và
lintDebug PASS.

## 6. Android

- Login giữ Dark Night Sky và thêm `HOẶC` + `Tiếp tục với Google`.
- Challenge/nonce giữ trong `LoginViewModel`; khóa bấm trùng, xử lý cancel/no credential/Play
  Services/network/backend/account-link-required; chỉ lưu Session sau response Backend thành công.
- Forgot Password gọi API thật, có validation/loading/network/config error và quay lại Login.
- Profile có `Liên kết tài khoản Google`; yêu cầu nhập lại mật khẩu, sau đó Credential Manager,
  rồi gọi endpoint có JWT. Response thành công cập nhật JWT trong `SessionManager`.
- Logout xóa phiên BiVoca và gọi `clearCredentialStateAsync`; không xóa Google account/avatar.

Ảnh runtime:

- `android-app/M14_login_e2e.png`: Login + nút Google.
- `android-app/M14_google_prompt2.png`: giao diện Google Sign-In thật trên Emulator.
- `android-app/M14_forgot.png`: Forgot Password Dark Night Sky.

## 7. Kiểm thử đã chạy

### Backend

- `npm run typecheck`: PASS.
- `npm run build`: PASS.
- `npm test`: **335/335 PASS**, gồm challenge/replay, email collision, re-auth link,
  generic Forgot response, SMTP guard, token hash/one-time, JWT revocation và rate limit.
- `DB_NAME=bivoca_m14_20261023 M14_INTEGRATION_ALLOW_WRITES=1 npm run test:m14:integration`:
  **1/1 PASS** trên PostgreSQL thật, chạy lại sau hotfix.
- Migration bản sao: Up/Down/Up PASS; sau chỉnh Down guard, Down/Up cuối PASS.
- `GET /api/v1/health` cổng 3001: HTTP 200; reset page headers kiểm tra thực tế PASS.

### Android

- `:app:assembleDebug`: PASS.
- `:app:assembleE2e`: PASS.
- `:app:testDebugUnitTest`: **75/75 PASS** (thêm kiểm tra không rò chuỗi kỹ thuật và ánh xạ lỗi Google).
- `:app:lintDebug`: **0 errors / 275 warnings**.
- `:app:connectedDebugAndroidTest`: **3/3 PASS** trên AVD API 35 sau production deploy.
- E2E APK cài bằng `adb install -r`; Login, Google button, Google Sign-In screen, cancel và
  Forgot navigation/validation/SMTP error được kiểm tra thật.
- Google E2E thật: link account, logout, login lại, JWT, Home và session restore đều PASS.

## 8. File chính đã thêm/sửa

Backend: auth route/controller/service/repository/schema/types, token middleware/service,
Google verifier, email sender, rate limiter, reset page, environment/server, migration M14,
M14 unit/integration tests, snapshot và backup/restore script, package manifests.

Android: `LoginActivity`, `LoginViewModel`, `ForgotPasswordActivity/ViewModel`, `ProfileActivity`,
`ProfileViewModel`, `AuthApi`, `AuthRepository`, Google/Forgot DTOs, Credential state helper,
Login/Profile/Forgot layouts/strings và Gradle dependencies/config.

Hotfix sửa trực tiếp `backend/src/modules/auth/google-token.service.ts`,
`android-app/app/src/main/java/com/example/bivoca/data/repository/AuthRepository.java` và
`android-app/app/src/test/java/com/example/bivoca/data/repository/AuthRepositoryRegistrationErrorTest.java`.

Production deployment bổ sung `backend/tests/email.service.test.ts`, cập nhật
`backend/src/modules/auth/email.service.ts`, `backend/scripts/m14-snapshot.ts` và thêm
`maintenance/configure-smtp.ps1`.

## 9. Việc còn lại để DONE

1. Cấu hình HTTPS `PUBLIC_RESET_URL` trước khi dùng reset link từ điện thoại hoặc máy ngoài.

Google Sign-In production và Password Recovery local đã hoàn tất. HTTPS public giữ
`PASSWORD_RECOVERY_PUBLIC_HTTPS_PENDING`; chưa đánh dấu `M14_DONE` toàn bộ.
