# M15 — Daily Learning Goals & Progress

**Ngày nghiệm thu:** 2026-10-24  
**Trạng thái:** `DONE / PRODUCTION_DEPLOYED`  
**Production:** migration M15 đã áp dụng trên `flashcard_english`; Backend cổng 3000 và APK Debug đã nghiệm thu

## 1. Kết quả triển khai

M15 đã hoàn thiện trên source, được kiểm thử trên PostgreSQL cô lập
`bivoca_m15_20261024`, sau đó backup/restore-check và triển khai lên production theo phê duyệt.
Home hiển thị mục tiêu hằng ngày, tiến độ từ mới, nút Học từ mới và số từ SRS đến hạn. Progress
có biểu đồ 7 ngày cùng lịch sử trạng thái mục tiêu. Settings đọc/ghi mục tiêu qua Backend và chỉ
cập nhật giao diện sau khi server xác nhận thành công.

API production:

- `GET /api/v1/learning/goal`
- `PUT /api/v1/learning/goal`
- `GET /api/v1/learning/today`
- `GET /api/v1/learning/history?days=7`
- `GET /api/v1/learning/new-cards?limit=10&topicCode=...&level=...`

Tất cả API lấy user ID từ JWT. Danh sách từ mới trả tối đa 20 Flashcard active, loại trừ từ đã
học và không tải toàn bộ 5.000 từ vào Android.

## 2. Migration và quy tắc dữ liệu

Migration duy nhất được áp dụng trong lượt production là
`1792789200000_add_daily_learning_goals.sql`. Trạng thái `pgmigrations` chuyển từ 9 migration,
M14 là migration cuối, sang 10 migration với M15 là migration cuối.

Migration thêm:

- `user_learning_settings`: mục tiêu 5/10/15/20, mặc định 10, khóa theo `user_id`.
- `user_learned_flashcards`: unique `(user_id, flashcard_id)`, trọng tài cho lần học đầu.
- `user_learning_daily`: snapshot mục tiêu, số từ mới và số lượt ôn theo ngày.
- `learning_system_config`: ngày kích hoạt M15 theo `Asia/Ho_Chi_Minh`.

Review và Quiz gọi bộ đếm M15 trong cùng transaction với Progress/Quiz/XP. `INSERT ... ON
CONFLICT DO NOTHING RETURNING` bảo đảm request đồng thời chỉ tính một từ mới. Mở thẻ và
Favorite không tính. Lỗi ở bất kỳ bước nào rollback toàn transaction.

Backfill production tạo đúng 12 dòng `LEGACY` từ bằng chứng Progress/Quiz cũ. Tất cả có
`first_learned_at=NULL`, nên không tạo ngày học giả và không bị tính lại thành từ mới. Sau
migration: 0 settings, 12 learned/12 legacy, 0 daily.

## 3. Backup, restore-check và toàn vẹn production

Backup mới ngay trước migration:

`backend/.m15-verification/flashcard_english_before_m15.dump` — 5.816.580 byte.

Dump đã restore thành công vào `bivoca_m15_20261024`. Snapshot restore khớp production trước
migration: 5.000 Flashcard, 19 topic, 5 user, 14 progress, 12 Quiz, 2 score, 5 XP; chưa có schema
M15. Migration và integration được chạy lại trên DB cô lập, **1/1 PASS**, rồi tài khoản thử được
cleanup về baseline.

Snapshot production trước/sau migration:

| Dữ liệu | Trước | Sau |
|---|---:|---:|
| Flashcard | 5.000 | 5.000 |
| Topic | 19 | 19 |
| User | 5 | 5 |
| Google identity | 3 | 3 |
| Progress | 14 | 14 |
| Quiz result | 12 | 12 |
| User score | 2 | 2 |
| XP history | 5 | 5 |

Checksum toàn bộ hàng của 8 bảng bảo vệ giống tuyệt đối ngay trước và ngay sau migration:

| Bảng | Checksum |
|---|---|
| `topics` | `94732ca4a7d5e5db3c4ad78101ab845c` |
| `flashcards` | `d191e53b07ce91dbe6b09ee7100f9a9b` |
| `users` | `7d28c6659930a5095d959dbba9597315` |
| `user_auth_identities` | `63a4420d3c0efb46d5a937e18f5d6d14` |
| `user_progress` | `d7702e65b95ec7763550c3591a30c5f8` |
| `quiz_results` | `47d5c7cd082927dbb63a6ae102a1fde8` |
| `user_scores` | `5cb375550f00bcce53e9ae5eb5928587` |
| `xp_history` | `6b2a63ed92c40e20c476b21b9d8dc262` |

Sau nghiệm thu Google Login, checksum `users` và `user_auth_identities` thay đổi do các trường
`last_login`/`last_login_at` được cập nhật đúng chức năng. Counts vẫn là 5 user/3 identity;
checksum Flashcard, topic, Progress, Quiz, score và XP vẫn giữ nguyên. Không tạo identity trùng.

## 4. Backend production và regression

- `npm run typecheck`: PASS.
- `npm run build`: PASS.
- `npm test`: **338/338 PASS**.
- `npm run test:m15:integration`: **1/1 PASS**, chỉ chạy trên DB cô lập với khóa ghi rõ ràng.
- `GET /api/v1/learning/today` không JWT trả 401 `AUTH_TOKEN_MISSING`, không phải 404.
- Windows launcher tự khởi động lại Backend build mới; trạng thái cuối chỉ một Node process lắng
  nghe `127.0.0.1:3000`; health trả `status=ok`, `database=connected`.

Không chạy integration ghi dữ liệu trên production.

## 5. Android Debug production

- `:app:assembleDebug`: PASS.
- `:app:testDebugUnitTest`: **75/75 PASS**.
- `:app:lintDebug`: PASS.
- APK Debug cài bằng `adb install -r`; không clear data.
- Google Account Chooser thật → `POST /auth/google` **200** → Home PASS.
- JWT gọi `auth/me`, `learning/today`, `learning/history`, `learning/goal`, Progress và
  Gamification đều **200** trên cổng production 3000.
- Home hiển thị `0 / 10`; Progress tải lịch sử 7 ngày; Settings/Học tập hiển thị mục tiêu đã
  đồng bộ 10; Profile giữ 30 XP và dữ liệu cũ.
- Không thực hiện Review, Quiz hoặc thay đổi mục tiêu trên tài khoản production.

Instrumentation **3/3 PASS** thuộc lượt nghiệm thu DB cô lập trước deployment; không chạy lại
instrumentation có khả năng ghi trên production. Kiểm tra production dùng các luồng read-only và
Google Login đã nêu trên.

Ảnh production:

- `android-app/M15_production_google_home.png`
- `android-app/M15_production_progress.png`
- `android-app/M15_production_settings_goal.png`
- `android-app/M15_production_profile.png`

## 6. File thêm và chỉnh sửa

Backend: migration M15; module `src/modules/learning/*`; tích hợp Progress/Quiz; test HTTP và DB;
`scripts/prepare-m15-isolated.ts`, `scripts/m15-snapshot.ts`, `scripts/m15-integrity.ts`,
`scripts/m15-migration-state.ts`; `package.json`.

Android: Learning API/repository/model; Home; New Learning trong Flashcard; Progress chart 7 ngày;
Settings goal; các layout/ViewModel liên quan và ảnh nghiệm thu.

Tài liệu: báo cáo này, `WORK_STATUS.md`, `BACKEND_CONTEXT.md`, `ANDROID_CONTEXT.md`.

## 7. Ghi nhận cho M17 và giới hạn

Không thay đổi UI trong lượt production. Các việc sau được ghi nhận cho M17:

- Giảm khoảng trống Home.
- Làm chú giải màu biểu đồ rõ hơn.
- Hiển thị lịch sử 7 ngày gọn hơn.
- Đổi nhãn `Đã ôn hôm nay (UTC)` thành ngôn ngữ thân thiện mà không đổi logic thời gian.
- Giải thích rõ cách tính tỷ lệ ghi nhớ.

M15 không kiểm thử ghi Review/Quiz trên production theo điều kiện bảo vệ dữ liệu. Hành vi ghi,
concurrency, rollback và cleanup đã PASS trên DB cô lập. Không tự động triển khai M16.
