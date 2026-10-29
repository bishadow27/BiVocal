# Admin tự chạy trên Windows

Mở **http://localhost:5173** và đăng nhập bằng tài khoản ADMIN hiện có.
Sau khi cài, Windows tự chạy Admin ở chế độ nền khi người dùng này đăng nhập máy.
Không cần mở PowerShell hay chạy `npm run dev` mỗi lần dùng.

Trong thư mục `maintenance`, các shortcut được tạo khi cài đặt:

- `Open Admin.url`: mở trang Admin trong trình duyệt.
- `Start Admin.lnk`: bật lại nếu đã dừng thủ công; mở nhiều lần không tạo nhiều launcher.
- `Stop Admin.lnk`: dừng Admin và backend do launcher này tạo. Shortcut tự khởi động vẫn còn cho lần đăng nhập Windows sau.

## Cách hoạt động

`BIPASSION Local Admin.lnk` trong thư mục Startup của người dùng chạy
`local-admin.vbs`, rồi chạy Node.js ẩn cửa sổ. `local-admin.mjs` phục vụ bản build
Admin bằng Vite preview ở `127.0.0.1:5173`, chuyển `/api` tới `127.0.0.1:3000`.
Đây là cách chạy tại máy cá nhân, không phải triển khai public.

Backend dùng cấu hình `.env` hiện có trong thư mục `backend`. Launcher chỉ ghi đè
HOST/PORT thành `127.0.0.1:3000`. PostgreSQL phải chạy; trên máy này dịch vụ
`postgresql-x64-18` đã được cấu hình Automatic. Nếu backend thoát khi DB chưa sẵn
sàng, launcher thử lại sau tối đa 10 giây. Tiến trình đang giữ cổng 3000 được giữ
nguyên; khi cổng trống, launcher mới chạy backend của project.

Launcher không chạy migration, seed hay reset tài khoản. Nhật ký sự kiện khởi
động nằm ở `maintenance/.runtime/local-admin.log`, không ghi mật khẩu hoặc token.
Một named pipe riêng theo đường dẫn project ngăn chạy trùng và nhận lệnh dừng.
Chỉ các tiến trình do launcher tạo mới bị dừng bởi `Stop Admin.lnk`.

## Cài lại hoặc cập nhật source

Chạy từ thư mục gốc project sau khi đã cài dependencies và cấu hình backend:

```powershell
npm --prefix backend run build
npm --prefix admin-web run build
& .\maintenance\install-local-admin.ps1
```

Sau khi sửa source, dừng bằng `Stop Admin.lnk`, build lại cả hai phần rồi mở
`Start Admin.lnk`. Bản chạy nền dùng `dist`, không tự cập nhật khi sửa source.
Nếu di chuyển project hoặc đổi đường dẫn Node.js, cần cài lại shortcut.

Kiểm tra trạng thái khi cần chẩn đoán:

```powershell
node .\maintenance\local-admin.mjs --status
```

Để tắt tự chạy, xóa shortcut **BIPASSION Local Admin** trong thư mục mở bằng
`Win + R` → `shell:startup`, rồi mở `Stop Admin.lnk`. Hoặc chạy:

```powershell
& .\maintenance\install-local-admin.ps1 -Uninstall
```

Nếu cổng 5173 bị phần mềm khác chiếm, launcher ghi lỗi vào log và dừng; không
tự chuyển sang cổng khác hoặc dừng phần mềm đó. Sau khi giải phóng cổng, mở
`Start Admin.lnk` để chạy lại.
