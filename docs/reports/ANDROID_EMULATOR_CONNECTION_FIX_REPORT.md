# Android Emulator Connection Fix — 2026-10-16

## Nguyên nhân

Hai Retrofit client dùng cố định `http://127.0.0.1:3000/api/v1/`. Trên Android Emulator, `127.0.0.1` là loopback của máy ảo nên ứng dụng chỉ kết nối được khi có `adb reverse tcp:3000 tcp:3000`. Reverse không tồn tại sau khi Emulator hoặc máy tính khởi động lại.

## Thay đổi

- Thêm `ApiBaseUrl` để nhận diện Android Emulator từ các thuộc tính `Build`.
- Android Emulator dùng `http://10.0.2.2:3000/api/v1/`, là địa chỉ host loopback do Emulator cung cấp.
- Điện thoại thật tiếp tục dùng `http://127.0.0.1:3000/api/v1/` và cần `adb reverse` khi kết nối bằng USB.
- Cả `RetrofitClient` và `AuthenticatedRetrofitClient` dùng chung resolver; không đổi API contract, Repository, ViewModel, Backend hoặc PostgreSQL.
- Thêm unit tests cho Google API Emulator và OPPO CPH2557 để ngăn hồi quy.

## Backend tự khởi động

Shortcut `BIPASSION Local Admin.lnk` đã tồn tại trong Startup của Windows. `maintenance/local-admin.mjs` tự chạy Backend tại `127.0.0.1:3000` và Admin Web khi người dùng đăng nhập Windows. Endpoint Topics trên Backend trả HTTP 200 trong lúc nghiệm thu.

## Kiểm thử

Lệnh build:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Kết quả: BUILD SUCCESSFUL; 82/82 unit tests PASS; lint 0 issue trong báo cáo XML.

Nghiệm thu AVD `BiVoca_API35_Pixel6`:

1. Khởi động lạnh AVD và chờ `sys.boot_completed=1`.
2. Chạy `adb reverse --remove-all`; `adb reverse --list` trả danh sách trống.
3. Cài APK bằng `adb install -r`; kết quả `Success`, giữ dữ liệu ứng dụng.
4. Từ chính `emulator-5554`, mở TCP tới `10.0.2.2:3000` và gửi `GET /api/v1/topics`.
5. Backend trả `HTTP/1.1 200 OK` cùng JSON chủ đề.

Kết luận: Emulator không còn phụ thuộc `adb reverse`. Sau khi mở lại Windows, cần chờ shortcut Startup chạy Backend và PostgreSQL; sau đó mở AVD và ứng dụng bình thường.

## File thay đổi

- `android-app/app/src/main/java/com/example/bivoca/data/api/ApiBaseUrl.java`
- `android-app/app/src/main/java/com/example/bivoca/data/api/RetrofitClient.java`
- `android-app/app/src/main/java/com/example/bivoca/data/api/AuthenticatedRetrofitClient.java`
- `android-app/app/src/test/java/com/example/bivoca/data/api/ApiBaseUrlTest.java`
- `WORK_STATUS.md`
- `ANDROID_CONTEXT.md`
- `ANDROID_EMULATOR_CONNECTION_FIX_REPORT.md`
