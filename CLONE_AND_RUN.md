# Cài BiVocal trên Windows

Bản development gồm Android (Java/XML), backend Node.js, admin web và PostgreSQL. Các lệnh dưới đây chạy từ PowerShell. Chỉ import SQL vào **database mới, rỗng**; không chạy lên database có dữ liệu.

## 1. Cài công cụ và lấy mã nguồn

- Git for Windows, Node.js 22.12 trở lên và npm.
- PostgreSQL 18 có `psql` và hỗ trợ ICU (bản cài Windows thông thường).
- Android Studio, Android SDK Platform 37, build tools, platform tools và Emulator. Dùng JDK đi kèm Android Studio cho Gradle. Android Studio có thể yêu cầu cài thêm SDK khi đồng bộ.

```powershell
git clone --branch feature/initial-import https://github.com/bishadow27/BiVocal.git
cd BiVocal
```

Nếu repository là private, chủ repository cần cấp quyền GitHub cho người clone. Đăng nhập GitHub khi Git yêu cầu.

## 2. Tạo database development

Khởi động dịch vụ PostgreSQL. Trong pgAdmin, tạo database mới tên `bivocal_dev`, encoding UTF8. Import lần lượt `dataset/development/schema.sql` và `dataset/development/vocabulary.sql` bằng `psql`; tệp dữ liệu dùng lệnh `COPY FROM stdin` nên không chạy bằng Query Tool.

Ví dụ PowerShell, điều chỉnh đường dẫn `psql.exe` theo nơi cài và nhập mật khẩu PostgreSQL khi được hỏi:

```powershell
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
& $psql -h 127.0.0.1 -U postgres -d bivocal_dev -v ON_ERROR_STOP=1 -f dataset/development/schema.sql
& $psql -h 127.0.0.1 -U postgres -d bivocal_dev -v ON_ERROR_STOP=1 -f dataset/development/vocabulary.sql
& $psql -h 127.0.0.1 -U postgres -d bivocal_dev -c 'SELECT (SELECT count(*) FROM topics) AS topics, (SELECT count(*) FROM flashcards) AS flashcards, (SELECT count(*) FROM flashcard_topics) AS links;'
```

Kỳ vọng 38 chủ đề, 20.000 flashcard, 23.043 liên kết. Schema snapshot đã bao gồm Quiz V2 và lịch sử migration tại thời điểm xuất; **không chạy lại `dataset/schema.sql`, seed 300 từ hoặc `npm run migrate:up` để khởi tạo snapshot này**. Migration Quiz V2 trong source có guard dành riêng database vận hành, không dùng để khởi tạo máy mới. Các lần import phải bắt đầu lại với database mới rỗng, không import đè database có sẵn.

Snapshot chỉ chứa nội dung từ vựng, provenance, cấu hình học và lịch sử migration; không chứa tài khoản hay tiến độ người dùng. Xem [nguồn và giấy phép dữ liệu](dataset/development/README.md).

## 3. Cấu hình và chạy backend

Mở PowerShell ở gốc dự án:

```powershell
cd backend
Copy-Item .env.example .env
npm ci
```

Chỉnh `backend/.env`: đặt `DB_HOST=127.0.0.1`, `DB_PORT=5432`, `DB_NAME=bivocal_dev`, `DB_USER` và `DB_PASSWORD` theo PostgreSQL trên máy bạn. Điền `JWT_SECRET` ngẫu nhiên ít nhất 32 ký tự; có thể tự tạo tại máy bằng `node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"` và lưu vào `.env`. Không chia sẻ hoặc commit `.env`.

```powershell
npm run dev
```

Giữ terminal mở. Truy cập `http://127.0.0.1:3000/api/v1/health`: kỳ vọng `status: ok` và `database: connected`. `http://127.0.0.1:3000/api/v1/topics` trả danh sách chủ đề. Google Sign-In và email khôi phục cần cấu hình dịch vụ riêng; đăng nhập mật khẩu thông thường không cần các cấu hình đó.

## 4. Android và tài khoản admin development

Mở thư mục `android-app` trong Android Studio, đồng bộ Gradle, tạo và chạy Emulator, sau đó chạy cấu hình `app` debug. Hoặc từ PowerShell tại `android-app`:

```powershell
.\gradlew.bat :app:assembleDebug
```

Emulator dùng `http://10.0.2.2:3000/api/v1/` để tới backend trên máy host. Backend phải đang chạy. Trên Android 17/API 37, debug manifest khai báo quyền mạng nội bộ; nếu app không kết nối, cấp quyền debug bằng `adb shell pm grant com.example.bivoca android.permission.ACCESS_LOCAL_NETWORK` với đúng Emulator đang chạy. Thiết bị thật dùng `adb reverse tcp:3000 tcp:3000`. Không dùng URL `localhost` của Emulator để chỉ máy host.

Đăng ký một tài khoản development mới trong app. Sau đó, tại thư mục `backend` chạy lệnh dưới với email vừa đăng ký, chỉ trên database development:

```powershell
npm run admin:set-role -- --email ban@example.test --role ADMIN
```

Thay email ví dụ bằng email tài khoản của bạn. Tài khoản được tạo bình thường qua app, mật khẩu không đưa vào câu lệnh. Không chạy lệnh cấp quyền này trên database khác ngoài ý muốn.

## 5. Chạy Admin Web

Mở terminal thứ hai ở gốc dự án:

```powershell
cd admin-web
npm ci
npm run dev
```

Mở `http://localhost:5173/login` và đăng nhập bằng tài khoản development đã cấp ADMIN. Vite chuyển `/api` tới backend `127.0.0.1:3000`.

## 6. Kiểm tra và xử lý lỗi

- PostgreSQL không kết nối: xác nhận dịch vụ đang chạy, thông tin `DB_*`, database đích và quyền tài khoản; không gửi `.env` cho người khác.
- Không có dữ liệu: kiểm tra đã import đúng thứ tự vào đúng database mới và lệnh `psql` không lỗi.
- Emulator không truy cập được backend: kiểm tra health trên host, `10.0.2.2`, firewall và quyền mạng nội bộ debug ở trên.
- Gradle/SDK lỗi: đồng bộ lại trong Android Studio, cài SDK yêu cầu và dùng JDK đi kèm; không chia sẻ `local.properties`.
- Bộ kiểm tra: `npm run typecheck` và `npm test` trong `backend`; `npm run build` và `npm test` trong `admin-web`; ` .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` trong `android-app`.

Snapshot là dữ liệu development, không phải bản sao lưu tài khoản hoặc database vận hành. Tài liệu này không chứng minh mọi chức năng đã được kiểm thử trên máy mới.
