# M13.6 — Vocabulary Review & Direct Database Publishing

**Cập nhật:** 2026-10-23  
**Trạng thái:** DONE — staging, backup/restore, production publish, Admin Web, API và Android Room đều PASS.

## 1. Kết quả đã triển khai

### Backend và PostgreSQL

- Thêm migration `1792616400000_add_vocabulary_review_workflow.sql` cho hàng đợi kiểm duyệt, chủ đề nguồn, publish batch, batch item và audit log.
- Thêm module `/api/v1/admin/vocabulary-review` theo kiến trúc Route → Controller → Service → Repository.
- Chỉ JWT có role `ADMIN` được đọc, sửa, duyệt, từ chối, xem trước và xuất bản.
- Hỗ trợ tìm kiếm, bộ lọc và pagination phía server cho 24.388 ứng viên.
- Bulk action chỉ nhận danh sách ID được chọn, tối đa 100 ID; “chọn tất cả” chỉ áp dụng cho trang hiện tại.
- Sửa một ứng viên sẽ tăng `rowVersion`, đưa về `PENDING` và lưu giá trị trước/sau vào audit.
- Publish gồm hai bước: tạo preview có fingerprint và xác nhận đúng batch. Transaction khóa phiên bản ứng viên, chống bấm lặp và không tạo Flashcard trùng.
- Exact duplicate theo English + Vietnamese + POS được bỏ qua; từ giống nhau nhưng khác nghĩa được giữ tách biệt và hiển thị cảnh báo.
- Script `load-vocabulary-review-candidates.ts` mặc định dry-run; chế độ ghi yêu cầu database local và `--database-confirm` khớp chính xác.
- Quiz chỉ lấy provenance đã được phê duyệt (`APPROVED_AUTO` hoặc `APPROVED_MANUAL`).

### Admin Web

- Thêm menu **Kiểm duyệt Dataset** và route `/admin/vocabulary-review`.
- Hiển thị tổng số, Pending, Approved, Rejected và Published.
- Có tìm English/Vietnamese; lọc CEFR, topic, POS, trạng thái, lý do và nguồn.
- Có checkbox, chọn trang hiện tại, duyệt/từ chối/chuyển chờ hàng loạt.
- Drawer chỉnh sửa nghĩa, IPA, CEFR, topic và ví dụ; hiển thị nguồn, cảnh báo chất lượng/giấy phép/trùng và đa nghĩa.
- Preview hiển thị số insert/skip/block; chỉ cho xác nhận khi không còn mục bị chặn.

### Android

- Bộ lọc CEFR dùng chữ trắng trên nền tối; nút **Tìm** dùng màu xanh chủ đạo.
- Quiz hiển thị `Câu x / y`; màu đáp án và phản hồi đúng/sai sáng, dễ đọc hơn.
- Flashcard không lặp lại tiêu đề chủ đề bên dưới shared top bar.
- Banner Room offline gọn hơn và giới hạn hai dòng.
- Android tiếp tục lấy dữ liệu qua API và Room, không hardcode 58 Flashcard mới.

## 2. Thử nghiệm database cô lập

Database chính dùng để thử nghiệm:

`bivoca_m13_6_20261021_30f444edd8b4`

Database phục hồi kiểm chứng:

`bivoca_m13_6_20261021_30f444edd8b4_restore`

Kết quả:

| Hạng mục | Kết quả |
| --- | ---: |
| Ứng viên nạp lần đầu | 24.388 inserted |
| Nạp lại cùng dữ liệu | 0 inserted, 24.388 skipped |
| Pending ban đầu | 23.990 |
| Approved ban đầu | 58 |
| Rejected ban đầu | 340 |
| Preview lô approved | 58 insert, 0 skip, 0 block |
| Publish staging | 58 inserted |
| Xác nhận lại cùng batch | 0 bản ghi mới; trả cùng kết quả batch |
| Trước publish | 10 topics, 300 Flashcards |
| Sau publish | 16 topics, 358 Flashcards |
| Hash 300 Flashcard gốc | Không đổi |
| Provenance sau publish | 116 rows |
| Backup staging | 4.375.849 bytes |
| Restore staging | PASS, snapshot trùng khớp |

Audit thực tế có `EDIT`, `APPROVE`, `REJECT`, `RESET_PENDING`, `BULK_APPROVE`, `BULK_RESET_PENDING`, `PREVIEW` và `PUBLISH`, kèm actor, timestamp, candidate ID, before/after và batch ID.

Phân quyền integration: không token trả 401, role USER trả 403, role ADMIN trả 200. Bộ integration M13.6 PASS 4/4.

## 3. Nghiệm thu Android trên Emulator

AVD `BiVoca_API35_Pixel6`, APK E2E kết nối backend staging tại `10.0.2.2:3001`.

- Login và Home bằng tài khoản E2E: PASS.
- Danh sách 16 chủ đề sau publish: PASS.
- Chủ đề mới `Emotions`, Flashcard `fun`, lật thẻ và xoay màn hình: PASS.
- Food hiển thị 43 Flashcard, chỉ một tiêu đề `Food`: PASS.
- Tìm `breakfast` trả hai nghĩa riêng biệt: PASS.
- Quiz hiển thị `Câu 1 / 10`, chấm đáp án `mưa` đúng và phản hồi rõ: PASS.
- Tắt backend, force-stop/mở lại app: Topics và Food 43 thẻ vẫn đọc từ Room; banner offline đúng trạng thái: PASS.
- Bật lại backend và bấm **Thử kết nối lại**: banner biến mất, dữ liệu tải lại: PASS.
- Tài khoản E2E đã xóa theo đúng username + email; file credential tạm đã xóa; emulator đã tắt.

Ảnh và UI dump nằm tại `android-app/app/build/reports/m13-6-review-publish/`.

## 4. Lệnh kiểm thử và kết quả

```powershell
cd backend
npm run typecheck
npm run build
npm test
npm run test:m13-6:integration

cd ..\admin-web
npm run build
npm test -- --run

cd ..\android-app
.\gradlew.bat :app:assembleDebug :app:assembleE2e
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

- Backend typecheck/build: PASS.
- Backend unit/integration mặc định: 321/321 PASS.
- M13.6 database integration: 4/4 PASS.
- Admin Web build: PASS; test 43/43 PASS.
- Android debug/E2E build: PASS; unit test 96/96 PASS; lint 0 lỗi.

## 5. Database vận hành — hoàn tất 2026-10-23

Backup custom format đã được lưu cục bộ tại:

`C:\Users\phuc\AppData\Local\BiVoca\backups\flashcard_english_before_m13_6_20261023032439.dump`

- Kích thước: 51.562 bytes.
- SHA-256: `9a4b0150b540d426308f34b1dc940d76fcf5d1e2b11d8d47092a16742c35f6bc`.
- ACL chỉ còn `NT AUTHORITY\SYSTEM` và tài khoản Windows `PC-OF-BI\phuc`, đều FullControl.
- Restore-check trên database cô lập `bivoca_m13_6_restorecheck_e0451c33d8`: PASS; snapshot topics/cards/users/progress/Quiz/scores/XP và hash Flashcard trùng hoàn toàn. Database restore-check được xóa sau kiểm chứng; file backup được giữ nguyên.

Hai migration M13/provenance và review workflow đã chạy trên `flashcard_english`. Loader production:

- Dry-run: 24.388 candidates, 19 topic metadata, SHA-256 nguồn `4a82d593f3fd21425c1781241232398fb9d9d7cd96305328da11db168b709834`.
- Lần apply đầu: 24.388 inserted, 0 skipped.
- Chạy lại cùng lệnh: 0 inserted, 24.388 skipped.
- Phân bố trước publish: 58 Approved, 23.990 Pending, 340 Rejected.

Admin Web tại `/admin/vocabulary-review` tải đủ số liệu. Preview thực tế hiển thị **58 sẽ thêm, 0 bỏ qua trùng, 0 bị chặn**. Sau xác nhận:

| Chỉ số | Trước | Sau |
| --- | ---: | ---: |
| Topics | 10 | 16 |
| Flashcards | 300 | 358 |
| Approved candidates | 58 | 0 |
| Published candidates | 0 | 58 |
| Pending candidates | 23.990 | 23.990 |
| Rejected candidates | 340 | 340 |
| Provenance rows | 0 | 116 |
| Audit rows | 58 | 116 |

Các bất biến production đều giữ nguyên: 3 users, 10 progress rows, 8 Quiz rows, 2 score rows và 4 XP rows. Checksum 300 Flashcard gốc trước/sau cùng là `2a2cfb0e62dc9916021ad3d592506d20`.

## 6. Giấy phép

Pipeline giữ nguồn và trạng thái attribution cho từng ứng viên. Việc phát hành công khai toàn bộ dataset vẫn bị chặn cho đến khi điều khoản phân phối và nội dung attribution của từng nguồn được xác minh đầy đủ. Điều này không ngăn thử nghiệm nội bộ hoặc hoàn thiện chức năng kiểm duyệt.

## 7. File M13.6 chính

- `backend/migrations/1792616400000_add_vocabulary_review_workflow.sql`
- `backend/src/modules/vocabulary-review/*`
- `backend/scripts/load-vocabulary-review-candidates.ts`
- `backend/tests/m13-6.database.integration.ts`
- `backend/.m13-6-verification/isolated.mjs`
- `backend/.m13-6-verification/backup-production.mjs`
- `backend/src/routes.ts`, `backend/src/server.ts`, `backend/src/modules/quiz/quiz.repository.ts`
- `admin-web/src/pages/VocabularyReviewPage.tsx`
- `admin-web/src/api/admin.api.ts`, `admin-web/src/types/api.ts`
- `admin-web/src/routes/AppRoutes.tsx`, `admin-web/src/components/layout/AdminLayout.tsx`, `admin-web/src/styles.css`
- `android-app/app/src/main/java/com/example/bivoca/ui/search/VocabularySearchActivity.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/quiz/QuizActivity.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/flashcard/FlashcardActivity.java`
- `android-app/app/src/main/res/layout/item_spinner_dark.xml`
- `android-app/app/src/main/res/layout/activity_vocabulary_search.xml`
- `android-app/app/src/main/res/layout/activity_topics.xml`
- `android-app/app/src/main/res/layout/activity_flashcard.xml`
- `android-app/app/src/main/res/values/offline_strings.xml`

## 8. Hotfix tải dữ liệu Admin — 2026-10-23

### Chẩn đoán thực tế

- Admin Vite đang nghe `127.0.0.1:5173`; proxy `/api` trỏ đúng `http://127.0.0.1:3000`.
- Backend đang nghe `127.0.0.1:3000`; `GET /api/v1/health` trả HTTP 200 và `database: connected`.
- Route `/api/v1/admin/vocabulary-review/stats` tồn tại; request không JWT trả đúng HTTP 401 `AUTH_TOKEN_MISSING`, vì vậy lỗi không phải network, CORS, base URL hoặc route 404.
- Backend kết nối đúng database `flashcard_english`, dung lượng đo lúc kiểm tra là 9.118 kB.
- Migration history production dừng ở `1791964800000_schedule_progress_reviews` (M10).
- Hai migration chưa chạy: `1792533600000_add_vocabulary_provenance` và `1792616400000_add_vocabulary_review_workflow`.
- Các bảng `vocabulary_review_topics`, `vocabulary_review_candidates`, `vocabulary_publish_batches`, `vocabulary_publish_batch_items`, `vocabulary_review_audit` chưa tồn tại. Các bảng provenance/topic relation/source mà migration M13.6 phụ thuộc cũng chưa tồn tại.
- Vì repository truy vấn `vocabulary_review_candidates`, request ADMIN trước hotfix đi vào PostgreSQL undefined-table (`42P01`) rồi bị middleware chung đổi thành HTTP 500 `INTERNAL_SERVER_ERROR`. Đây là nguyên nhân của “Không thể tải dữ liệu”.

### Sửa source

- Router kiểm duyệt bắt riêng PostgreSQL `42P01` trong phạm vi module và trả HTTP 503 `REVIEW_SCHEMA_UNAVAILABLE`, không lộ tên bảng, SQL hoặc connection details.
- Admin Web hiển thị thông báo cụ thể: database chưa có schema M13.6 và cần backup → migration → nạp queue; bổ sung thông báo riêng cho 404, 500 và 503.
- Backend/Admin launcher đã được build và khởi động lại; Backend PID được launcher quản lý, health HTTP 200.

### Dry-run và kiểm thử hotfix

- `DB_NAME=flashcard_english` + `load-vocabulary-review-candidates.ts --dry-run`: 24.388 candidates, 19 topic metadata, SHA-256 `4a82d593f3fd21425c1781241232398fb9d9d7cd96305328da11db168b709834`; không ghi database.
- Production vẫn giữ 10 topics, 300 Flashcards, 3 users, 10 progress rows, 8 quiz rows, 2 score rows và 4 XP history rows.
- Backend typecheck/build PASS; **321/321 tests PASS** (thêm test 503 schema missing và không rò chi tiết PostgreSQL).
- Admin build PASS; **43/43 tests PASS** (thêm test thông báo schema cụ thể).
- M13.6 integration trên database riêng: **4/4 PASS**.

### Kết quả sau hotfix

Sau khi người dùng xác nhận phạm vi production, backup/restore-check, hai migration, queue import và publish đã hoàn tất theo mục 5. Trang Admin tải 24.388 ứng viên và thống kê đúng; lỗi generic ban đầu không còn tái hiện.

## 9. Nghiệm thu production qua API và Android Room — 2026-10-23

- `GET /api/v1/health`: HTTP 200, database connected.
- `GET /api/v1/topics/catalog`: 16 chủ đề.
- `GET /api/v1/flashcards?search=breakfast&limit=100`: trả hai nghĩa riêng, gồm Flashcard mới ID 307.
- `gradlew.bat assembleDebug testDebugUnitTest`: BUILD SUCCESSFUL.
- Cài APK bằng `adb install -r`: Success, giữ phiên đăng nhập `duyphuc`.
- Android Topics tải 16 chủ đề từ production. Chủ đề Health hiển thị 39 thẻ và mở `doctor` 1/39.
- Sau khi đóng app và sao chép đồng thời file Room + WAL + SHM, Room có 16 topic metadata và 69 thẻ đã tải. Trong cache Health có 9 Flashcard mới ID > 300: `diarrhoea` 310, `diphtheria` 312, `malaria` 319, `stomachache` 320, `toothache` 324, `ache` 326, `outpatient` 343, `tuberculosis` 344 và `therapy` 347.
- Kết luận: Android nhận dữ liệu production qua REST API và ghi các Flashcard mới vào Room; không hardcode và không kết nối PostgreSQL trực tiếp.

Ảnh xác nhận Admin sau publish: `M13_6_ADMIN_PUBLISHED.png`. Snapshot Room dùng kiểm chứng nằm trong `backend/.m13-6-verification/` và không chứa token/mật khẩu.

### Lệnh production chính đã chạy

```powershell
node .m13-6-verification/backup-production.mjs
$env:DB_NAME='flashcard_english'; npm run migrate:up
$env:DB_NAME='flashcard_english'; npx tsx scripts/load-vocabulary-review-candidates.ts --dry-run
$env:DB_NAME='flashcard_english'; npx tsx scripts/load-vocabulary-review-candidates.ts --apply --database-confirm flashcard_english
node .m13-6-verification/verify-production.mjs

cd ..\admin-web
npm run build
npm test -- --run

cd ..\android-app
.\gradlew.bat assembleDebug testDebugUnitTest
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 10. File bổ sung trong lượt production

- `admin-web/src/components/ui/index.tsx`: thêm lựa chọn 100 dòng/trang để chọn trọn 58 Approved trong một preview.
- `backend/.m13-6-verification/verify-production.mjs`: snapshot chỉ đọc, đếm dữ liệu và checksum 300 thẻ gốc.
- `backend/.m13-6-verification/inspect_room.py`: đọc bản sao Room để xác minh topics và Flashcard mới.
- `backend/.m13-6-verification/android-room-production.db*`: bản sao kiểm chứng Room/WAL/SHM từ emulator.
- `M13_6_ADMIN_PUBLISHED.png`: ảnh runtime Admin sau publish.

