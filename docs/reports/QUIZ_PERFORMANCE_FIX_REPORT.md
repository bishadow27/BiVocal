# Quiz — Khắc phục Backend phản hồi lâu

Trạng thái: **TECHNICAL_COMPLETE** cho tối ưu tạo câu hỏi Quiz. Quyền thực hiện: người dùng duyệt sau chẩn đoán nghẽn CPU. Không thay trạng thái nghiệm thu UI M16.6.

## Kết quả đo thật

Ba request mỗi phía, cùng protocol GET Quiz limit10; GET Health gửi sau10ms. Backend thật đang quản lý bởi launcher, PostgreSQL phục dựng, cùng thiết bị/máy. Không phải benchmark với dữ liệu production giả.

| Chỉ số | Trước | Sau |
| --- | ---: | ---: |
| Quiz toàn bộ — trung vị | 2858ms | 94ms |
| Quiz — khoảng đo | 1757–3879ms | 91–117ms |
| Health gửi cùng đợt Quiz — chậm nhất quan sát | 2876ms | 103ms |
| Health khi idle | 3ms | 4ms |
| Chuẩn hóa chuỗi, test120thẻ | 172320lần | 480lần |

Quiz nhanh hơn khoảng **30.4lần** theo trung vị. Health có thể trả trước hoặc trong đoạn CPU tùy thời điểm query hoàn tất; bảng ghi worst-observed thay vì coi mọi request Health đều bị chặn. N=3 là kiểm chứng hồi quy cục bộ, không phải load test hoặc cam kết P95.

Bằng chứng: [before](../exports/quiz-performance-fix/runtime-before.json), [after](../exports/quiz-performance-fix/runtime-after.json), [chẩn đoán ban đầu](../exports/quiz-performance-fix/diagnostic-before.json).

## Nguyên nhân và cách sửa

Trước đây cùng chuỗi English/nghĩa/thành phần nghĩa/POS/CEFR bị normalize lặp trong mọi phép so sánh cặp và comparator sort. Khâu tìm card theo ID còn liên tục flat/find toàn bộ nhóm. Công việc đồng bộ chiếm luồng xử lý Node.js; probe ban đầu đo khoảng1578ms CPU của Backend cho loạt GET này.

Chỉ sửa quiz.service.ts: chuẩn bị metadata **một lần cho mỗi card trong request**, dùng Set thành phần nghĩa và map theo ID; dữ liệu nguyên bản không bị sửa. Không cache xuyên request, không thêm dependency. Giữ nguyên group, ambiguity, filter, shuffle, comparator, loại xung đột giữa mọi option, global unique prompt, số câu an toàn khi thiếu và final slice. Không lấy mẫu sớm để đổi tiêu chí chọn câu.

Các hàm answer/history/summary đối chiếu nguyên văn sau chuẩn hóa line ending và không thay đổi. Repository, SQL/provenance eligibility, route, DTO, auth, SRS/XP, Android/Admin và .env nguyên hash đầu phiên. Chi phí pairwise/sort vẫn còn; tối ưu này bỏ phần lặp chuỗi/tra cứu, không tuyên bố loại hết mọi giới hạn khi dữ liệu hoặc tải tăng.

## Kiểm thử

| Kiểm tra | Kết quả |
| --- | --- |
| TDD RED trên code cũ | FAIL đúng regression172320calls/120cards;3test tương thích PASS |
| Focused Quiz service/HTTP/repository | PASS64/64 |
| npm run typecheck | PASS |
| npm test (unit/HTTP, repository doubles) | PASS342/342,0skip |
| npm run build | PASS |
| Review độc lập | Không có Critical/Important/Minor cần sửa |
| Runtime Quiz DTO/4option/unique/nghĩa | PASS |
|40optioncard trong mỗi mẫu kiểm chứng khớp GET detail | PASS, không lưu nội dung hoặc token trong report |
| Android điều hướng GET-only sau đăng nhập lại | PASS1/1 |

Tests mới ở quiz-performance.test.ts chặn work tăng theo số phép so sánh thay vì input; kiểm tra Unicode/NFKC/case, punctuation/ngoặc/thành phần nghĩa, ambiguity, global dedup/shortage count, POS/CEFR và dữ liệu thay đổi giữa các request. Fixture chỉ ở test cô lập; không dùng PostgreSQL, không đưa dữ liệu giả vào ứng dụng.

Log: [RED](../exports/quiz-performance-fix/red.log), [focused](../exports/quiz-performance-fix/green-focused.log), [full342](../exports/quiz-performance-fix/full-tests.log), [typecheck](../exports/quiz-performance-fix/typecheck.log), [build](../exports/quiz-performance-fix/build.log), [Android sau login](../exports/quiz-performance-fix/android-quiz-get-only-relogin.log).

## Android và giới hạn còn lại

Lượt Android đầu **FAIL/crash**: API trả401, session không còn token/user ID hợp lệ; NavigationDrawerController gọi LocalAvatarStore.load với ID không hợp lệ và ném “A valid user id is required”. Không do agent logout/clear data. Chưa kết luận riêng nguyên nhân401 ngoài dữ liệu quan sát; không thay Auth/Android để lách lỗi. Người dùng trực tiếp đăng nhập Google lại, auth/me200, chạy lại chính test GET-only và PASS. **Lỗi avatar khi mất phiên vẫn cần xử lý Android riêng nếu được duyệt.** Giữ [log crash](../exports/quiz-performance-fix/android-quiz-get-only.log).

Test Android đi Home → Topics/Detail → Browse/Review/New/Quiz, giữ VM/queue/index/face/question qua tab và xoay màn, không trả lời/chấm thẻ. Ảnh thật: [Quiz](../exports/quiz-performance-fix/android/quiz.png), [Home](../exports/quiz-performance-fix/android/home.png), [Browse](../exports/quiz-performance-fix/android/flashcard.png). Không dùng ảnh trước test thất bại làm kết quả sau sửa.

Không chạy database integration có ghi; không POST Review, QuizAnswer hoặc Favorite lên database phục dựng. Unit HTTP có kiểm tra POST qua repository doubles cục bộ, tách khỏi runtime. Chưa load-test nhiều người đồng thời hoặc mọi thiết bị.

## Vận hành, dữ liệu và Git

Chỉ restart child Backend đã đối chiếu parent launcher và listener3000. Launcher giữ nguyên, Admin HTTP200; backendMode managed trước/sau, childPID thay đổi, một listener3000, không tạo launcher/npm dev trùng. [Biên bản restart](../exports/quiz-performance-fix/restart.json).

GET trước/sau: PostgreSQL connected, **20000Flashcard/38Topics/catalog36(29semantic+7POS)**; JWT được dùng RAM-only và vẫn hợp lệ sau Backend restart. Phiên sau được người dùng đăng nhập lại khi Android nhận401 như ghi ở trên. Không migration/seed/import/reset/restore/M15.7Apply, đổi password/role/secret hoặc dữ liệu nội dung.

Snapshot712file Git-relevant và hash riêng .env. Không có thay đổi ngoài scope. Các thay đổi/untracked có trước được giữ nguyên; .env không sao chép vào evidence. Giữ HEAD a4a9801/feature/initial-import và staging rỗng; **CHƯA COMMIT/CHƯA PUSH**.

File source/test: backend/src/modules/quiz/quiz.service.ts; backend/tests/quiz-performance.test.ts. Tài liệu: docs/superpowers/plans/2026-11-02-quiz-performance.md; docs/reports/QUIZ_PERFORMANCE_FIX_REPORT.md; append WORK_STATUS.md. backend/dist được npm run build sinh lại và không chỉnh tay. Hash tại [integrity.json](../exports/quiz-performance-fix/integrity.json). Evidence ignored docs/exports/quiz-performance-fix/ không chứa credential.

Skills áp dụng: systematic-debugging đo HTTP/CPU trước sửa; writing-plans/executing-plans giới hạn thay đổi và hash guard; test-driven-development có RED→GREEN thật; requesting-code-review kiểm tra tương đương semantics; verification-before-completion và bivoca-quality-handoff phân biệt test cô lập/runtime/401/giới hạn và bảo vệ dữ liệu.
