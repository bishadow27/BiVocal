---
name: bivoca-quality-handoff
description: Kiểm thử theo phạm vi, phân biệt nghiệm thu người dùng và bàn giao BiVoca giữa Codex và Antigravity. Dùng khi kiểm tra hoặc kết thúc công việc BiVoca, review kết quả, hay cập nhật WORK_STATUS.md.
---

# BiVoca — Kiểm thử và bàn giao

Đọc `AGENTS.md` tại gốc project trước khi áp dụng. Skill không cấp thêm quyền sửa, Git hoặc nghiệm thu; phê duyệt hiện có chỉ áp dụng đúng phạm vi đã giao.

## Xác định đầu vào

- Đọc `context.md`, `memory.md`, đầu `WORK_STATUS.md`, tài liệu module và source liên quan. Đối chiếu cập nhật mới với ghi chú lịch sử, không lấy trạng thái từ bộ nhập dự thảo.
- Nếu có Git, kiểm tra nhánh/status, diff unstaged/staged và 5 commit gần nhất. Giữ thay đổi của người dùng/AI khác; chỉ dừng khu vực xung đột chưa rõ chủ sở hữu.
- Nếu không có repository, báo chưa xác định nhánh/remote/lịch sử; đối chiếu nội dung/mã băm cho phần đã được phép sửa. Không tự `git init` hoặc đoán quyền commit/push.
- Xác định việc nhỏ/lớn theo `AGENTS.md`. Nếu phê duyệt cần thiết đã có, tiếp tục trong phạm vi đó; yêu cầu chỉ phân tích thì không ghi file.

## Chọn kiểm tra có ý nghĩa

- **Tài liệu/skill:** kiểm tra cấu trúc, YAML/frontmatter, liên kết, nguồn dẫn, nguy cơ secret/thông tin cá nhân; xác minh lịch sử nghiệm thu được giữ nguyên. Không cần build ứng dụng nếu không đổi code/cấu hình ứng dụng.
- **Android:** đọc wrapper, `settings.gradle.kts`, app build và thiết bị trước khi chọn task. Module hiện có là `:app`; lệnh Windows có thể gồm `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. Chọn kiểm tra theo thay đổi, không bắt buộc chạy mọi task cho mọi việc.
- **Instrumentation/runtime:** đọc guard giữ APK ở `AGENTS.md` và `ANDROID_CONTEXT.md`; `adb devices -l` xác minh thiết bị, không mặc định AVD đang chạy. Không uninstall/clear data/reset DB để sửa test. Source cấu hình mạng không chứng minh API đang hoạt động.
- **Backend/API:** đọc `backend/package.json`, test config và guard database. `npm test` chạy unit/HTTP tests; PostgreSQL integration là script riêng và cần đúng DB/phạm vi được phép. Không chỉ build Android rồi kết luận Backend đạt.
- **Hồi quy:** tập trung luồng bị ảnh hưởng và hành vi đã nghiệm thu. Không đưa Room trở lại runtime Online-Only từ ghi chú M8 lịch sử.

## Ghi đúng kết quả và quyền nghiệm thu

- Mỗi kiểm tra ghi lệnh/thao tác, `PASS`/`FAIL`/`NOT_RUN`/`BLOCKED`, bằng chứng, giới hạn. Ghi rõ test mới chạy, up-to-date/cache hay chỉ đọc kết quả cũ; không khẳng định đã thấy UI nếu chưa quan sát.
- Việc nhỏ có thể `TECHNICAL_COMPLETE` sau kiểm tra phù hợp. Tính năng lớn/milestone tối đa `READY_FOR_ACCEPTANCE` đến khi người dùng xác nhận `PASS`. Test PASS không thay thế nghiệm thu milestone.
- Giữ nguyên lịch sử DONE/PASS và các trạng thái đang chờ duyệt; tích hợp tài liệu không tự nghiệm thu M16 hoặc triển khai production.
- Thiếu một kiểm tra thì đánh dấu kiểm tra đó `NOT_RUN`/`BLOCKED`; tiếp tục phần độc lập còn được phép.

## Git và bàn giao

- Chỉ commit/push việc nhỏ nếu đã có quyền hành động và nhánh feature được chỉ định cho phạm vi; kiểm tra diff, test và remote đích. Không coi bản nhập, tên nhánh hoặc duyệt sửa tài liệu là quyền push.
- Khi không thực hiện, ghi CHƯA COMMIT/CHƯA PUSH. Không gom file ngoài phạm vi, force-push hoặc sửa lịch sử.
- Bổ sung bản ghi mới vào `WORK_STATUS.md` khi được phép; giữ nguyên nội dung cũ. Nêu file, kiểm tra, giới hạn, nghiệm thu còn chờ và việc AI tiếp theo cần tiếp tục. `memory.md` chỉ nhận quyết định/bài học lâu dài có nguồn.
- Báo cáo tiếng Việt ngắn gọn: kết quả, lý do, file liên quan, kiểm thử, cách nghiệm thu phù hợp và Git/bàn giao. Chỉ hướng dẫn Emulator khi công việc liên quan Android runtime; không cần cho thay đổi chỉ gồm tài liệu.

Các đường dẫn trong skill tính từ thư mục chứa `AGENTS.md` gốc, không từ thư mục skill. Mở phiên Codex mới tại gốc để kiểm tra khả năng nạp hướng dẫn và phát hiện skill; không tuyên bố đã thử cơ chế này nếu mới kiểm tra file.
