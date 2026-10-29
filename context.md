# context.md — Bối cảnh làm việc BiVoca

> Đối chiếu source/cấu hình và tài liệu ngày 26/10/2026. Đây là bối cảnh bằng file, được đọc qua `AGENTS.md`; không xác nhận hệ thống đang chạy hoặc thay thế nghiệm thu.

## Cách cộng tác

- Trả lời tiếng Việt, ngắn gọn, dễ hiểu; nêu kết quả, kiểm thử thật và giới hạn.
- Quyền tự chủ, thay đổi lớn, Git và nghiệm thu theo `AGENTS.md`; không suy ra quyền từ hồ sơ cá nhân hoặc bản nhập.
- Bàn giao qua `WORK_STATUS.md` để Codex/Antigravity tiếp tục cùng phạm vi. Không lưu thông tin học tập cá nhân hoặc tài khoản vào cấu hình chia sẻ.

## Đã kiểm chứng từ file

| Nội dung | Hiện trạng | Nguồn |
|---|---|---|
| Các module | Backend, Android, Admin Web, dataset, maintenance | Thư mục tại gốc project |
| Backend | Express 5, TypeScript, pg; scripts typecheck/test và PostgreSQL integration riêng | [package.json](backend/package.json), [bối cảnh Backend](BACKEND_CONTEXT.md) |
| Android | Java/XML, MVVM, Retrofit; module `:app`, Gradle 9.5.0, AGP 9.3.3, compile/target SDK 37, min SDK 24 | [settings](android-app/settings.gradle.kts), [wrapper](android-app/gradle/wrapper/gradle-wrapper.properties), [versions](android-app/gradle/libs.versions.toml), [app build](android-app/app/build.gradle.kts) |
| Runtime Android | Online-Only; app build không còn dependency Room, mã lịch sử ở ngoài app source set | [app build](android-app/app/build.gradle.kts), [Android](ANDROID_CONTEXT.md), `android-app/historical-room/` |
| API local | Emulator cổng 3000 qua `10.0.2.2`, thiết bị thật qua `127.0.0.1` + ADB reverse; E2E cổng 3001 | [ApiBaseUrl](android-app/app/src/main/java/com/example/bivoca/data/api/ApiBaseUrl.java), app build |
| Admin | React 19, Vite, TailwindCSS | [package.json](admin-web/package.json) |

## Hiện trạng theo hồ sơ, chưa tái nghiệm thu runtime trong phiên tích hợp

- Bộ dữ liệu gốc 10 chủ đề/300 từ khác kho vận hành. Hồ sơ M15.5–M15.7 và đầu `WORK_STATUS.md` ghi 20.000 flashcards/38 topics; 15.000 thẻ nhập vẫn chưa kiểm duyệt ngữ nghĩa và không được đưa vào Quiz. Không coi số liệu này là truy vấn DB mới.
- M14: `GOOGLE_PRODUCTION_DONE / PASSWORD_RECOVERY_LOCAL_DONE / PASSWORD_RECOVERY_PUBLIC_HTTPS_PENDING`. Không kết luận mọi phần khôi phục mật khẩu đã sẵn sàng công khai.
- M16.1–M16.3: còn chờ người dùng duyệt giao diện. Phản hồi mới nhất ghi bỏ Back trên cùng ở Register/Forgot và bỏ gạch chân slogan; không dùng mô tả cũ để đảo ngược.
- Số test là bằng chứng theo từng lần chạy; không cố định 311/338 Backend hoặc 81 Android làm số lượng bắt buộc của các phiên sau.

Nguồn: [tiến độ](WORK_STATUS.md), [M14](docs/reports/M14_GOOGLE_SIGNIN_PASSWORD_RECOVERY_REPORT.md), [M15.5](docs/reports/M15_5_VOCABULARY_20000_REPORT.md), [M15.7](docs/reports/M15_7_SEMANTIC_TOPIC_ENRICHMENT_REPORT.md), [M16.3](docs/reports/M16_3_HOME_FRONTEND_REPORT.md).

## Chưa xác minh / phải kiểm tra lại đầu phiên

- Ngày 26/10/2026, `git status` tại gốc và các module báo không có repository; không thấy `.git` tại gốc hoặc các thư mục cha. Chưa biết nhánh, remote, lịch sử hoặc file đã được theo dõi. Không tự khởi tạo Git.
- Trạng thái server/DB, thiết bị/Emulator đang kết nối, SMTP và HTTPS thực tế chưa được kiểm tra lại trong phiên tích hợp tài liệu.
- Mô tả cũ trong `PROJECT_CONTEXT.md` và các mục lịch sử có thể còn Room hoặc dữ liệu 300/5.000 từ; đọc bản cập nhật mới và source trước khi hành động.
- Chưa kiểm tra tự động nạp hướng dẫn/skill trong một phiên Codex mới; kiểm tra từ thư mục chứa `AGENTS.md` này, nhất là khi chưa xác định được Git root.

## Bản đồ tài liệu

- [AGENTS.md](AGENTS.md): quy tắc bắt buộc và quyền thao tác.
- [memory.md](memory.md): quyết định/bài học lâu dài có nguồn.
- [WORK_STATUS.md](WORK_STATUS.md): tiến độ và nghiệm thu; giữ nguyên lịch sử.
- [ANDROID_CONTEXT.md](ANDROID_CONTEXT.md), [BACKEND_CONTEXT.md](BACKEND_CONTEXT.md), [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md): chi tiết theo module, cần đối chiếu tuổi của từng ghi chú.
- [README.md](README.md): mục lục báo cáo trong `docs/reports/`.
- [bivoca-quality-handoff](.agents/skills/bivoca-quality-handoff/SKILL.md): quy trình kiểm thử và bàn giao.
