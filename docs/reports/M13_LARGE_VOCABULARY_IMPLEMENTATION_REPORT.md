# M13 LARGE VOCABULARY DATASET, CEFR, BACKEND VÀ ANDROID

Ngày nghiệm thu: 2026-10-21  
Trạng thái: **STAGING ACCEPTED; PRODUCTION CHƯA TRIỂN KHAI**

## 1. Kết luận

M13 đã hoàn tất pipeline kỹ thuật, migration, import staging, REST API, Android/Room/Quiz và nghiệm thu E2E trên Android Emulator. Database vận hành `flashcard_english` chỉ được đọc và giữ nguyên **10 topics, 300 flashcards**, checksum `2a2cfb0e62dc9916021ad3d592506d20` trước và sau.

Mục tiêu 5.000 thẻ **không đạt**. Sau khi siết quality gate để loại nghĩa sai, ví dụ chung chung, từ đa nghĩa chưa được duyệt và topic suy đoán, chỉ **58 thẻ mới** trong 24.388 ứng viên được tự động phê duyệt. Staging có tổng cộng **358 thẻ**. Không hạ chuẩn, không tạo nghĩa/ví dụ giả và không đưa review queue vào Quiz để chạy theo số lượng.

Các phần M13.1–M13.5 hoàn thành trong staging. Việc đưa migration/dataset lên production cần một lượt phê duyệt riêng, backup và kế hoạch triển khai đồng bộ Backend + Android.

## 2. Nguồn dữ liệu và giấy phép

### CEFR

- File: `backend/datasets/cefr/word_list_cefr.csv`, phân cách bằng dấu chấm phẩy.
- 7.988 dòng, 7.035 headword chuẩn hóa, 7.981 cặp headword/POS.
- Mapping Phase 1 đối chiếu 300 thẻ gốc: 293 exact, 7 unmatched, 99 xung đột level được giữ để review; không ghi đè dữ liệu gốc.
- Nguồn: `Maximax67/Words-CEFR-Dataset`; giấy phép MIT được lưu tại `backend/datasets/cefr/LICENSE` và attribution tại `backend/datasets/cefr/ATTRIBUTION.md`.

### Từ điển Anh–Việt

- File local: `backend/datasets/en-vi/dictionary_en_vi.db`.
- SQLite 44.212.224 byte; SHA-256 `2b65bea9577c2a4149caa90940c806f321d1c6cfac503ed8bbd9f93e43a0033a`; `PRAGMA integrity_check=ok`; không có lỗi foreign key.
- Nguồn: `skypediacode/english-vietnamese-dictionary`, commit `388adc0826300912e5b0311c089e0e70494b065f`, giấy phép CC BY-SA 4.0.
- Attribution, license và schema nguồn được lưu trong `backend/datasets/en-vi/`. File `.db` được gitignore để không đưa binary 44 MB vào source control.
- Audit: 104.829 words, 158.307 definitions, 175.735 liên kết word-definition, 108.776 pronunciation rows; 31.897 từ có nhiều nghĩa và 9.795 từ có nhiều POS.

## 3. Pipeline chất lượng

`backend/scripts/large_vocabulary_pipeline.py` chuẩn hóa Unicode NFKC, trim/casefold để đối chiếu, map POS, giữ sense identity từ SQLite, chọn IPA theo nguồn và tạo ID xác định để chạy lại an toàn. Pipeline không tự dịch, không tự viết ví dụ và không tự gán A1.

Mỗi ứng viên thuộc một trong ba trạng thái:

- `APPROVED_AUTO`: đủ CEFR, nghĩa tiếng Việt, ví dụ, POS và topic được xác nhận bằng nghĩa/nguồn.
- `NEEDS_REVIEW`: dữ liệu có thể dùng nhưng không đủ chắc chắn để tự động phát hành.
- `REJECTED`: malformed, unsafe, trùng sense chuẩn hóa hoặc không đạt các điều kiện tối thiểu.

Kết quả cuối:

| Trạng thái | Số lượng |
|---|---:|
| APPROVED_AUTO | 58 |
| NEEDS_REVIEW | 23.990 |
| REJECTED | 340 |
| Tổng ứng viên | 24.388 |

Mọi headword/POS đa nghĩa được đưa vào review vì CEFR nguồn chỉ định danh ở mức headword/POS, không đủ để xác nhận từng sense tiếng Việt. Topic chỉ được cấp khi nghĩa và nguồn cùng xác nhận; ví dụ mẫu chung chung bị loại.

Phân bố 58 thẻ mới: A1 15, A2 13, B1 14, B2 16. Topic chính: FOOD 13, HEALTH 9, TRAVEL 7, SHOPPING 7, LEISURE 6, SCHOOL 4, FAMILY/HOME/PERSONAL/WEATHER/WORK mỗi nhóm 2, EMOTIONS/TRANSPORTATION mỗi nhóm 1. Cả 58 thẻ đều `quizEligible=true`.

Các lý do kiểm tra chi tiết và số lượng được lưu trong `backend/datasets/processed/bivoca_vocab_statistics.json` và `bivoca_vocab_validation_report.md`. Review queue được giữ riêng, không import ngầm.

## 4. PostgreSQL staging và migration

Migration `1792533600000_add_vocabulary_provenance.sql` tạo:

- `vocabulary_sources` để ghi nguồn, phiên bản và license;
- `flashcard_provenance` để gắn record nguồn, trạng thái review và quyền dùng trong Quiz;
- `flashcard_topics` cho quan hệ nhiều-nhiều;
- index phục vụ topic, provenance và truy vấn Quiz.

Migration backfill đúng 300 quan hệ topic của các thẻ cũ; down migration chỉ bỏ object M13. Không đổi ID, nghĩa Việt, IPA, ví dụ hoặc quan hệ học tập của 300 thẻ ban đầu.

Importer `backend/scripts/import-large-vocabulary.ts` mặc định dry-run. Chế độ `--apply` chỉ chấp nhận PostgreSQL local có tên khớp `bivoca_m13_large_*`, chạy trong transaction và dùng source record ID để chống trùng.

Database nghiệm thu: `bivoca_m13_large_20261021_b06075c1d427`.

- Migration up/down/up: PASS, tổng 7 migrations.
- Failure injection sau 10 bản ghi: rollback PASS, còn 300 cards, 0 source, 0 provenance.
- Import lần 1: inserted 58, 211 ms.
- Import lần 2: inserted 0, skipped 58, 241 ms.
- Kết quả: 16 topics, 358 cards, 375 card-topic relations, 116 provenance rows, 2 sources.
- 0 foreign key violation, 0 duplicate source record; 300 thẻ gốc được bảo toàn.
- Database size: 9.508.543 byte.

Benchmark local staging: topic catalog 1,301 ms; topic page 1,114 ms; English search 0,756 ms; Vietnamese search 0,742 ms; CEFR filter 0,527 ms; Quiz eligible 0,631 ms.

## 5. Backend

API mới/mở rộng:

- `GET /api/v1/topics/catalog`: danh mục topic kèm `flashcardCount`.
- `GET /api/v1/flashcards?search=&topicCode=&level=&page=&limit=`: tìm tiếng Anh/Việt, lọc topic/CEFR và phân trang; `limit` tối đa 100.
- `GET /api/v1/topics/:code/flashcards`: giữ contract cũ, đọc quan hệ nhiều-nhiều.
- Quiz chỉ lấy thẻ import có provenance `APPROVED` và `quiz_eligible=true`; 300 thẻ cũ không có provenance vẫn hợp lệ như trước.

Kiến trúc Route → Controller → Service → Repository được giữ nguyên. Không thay Auth, Progress, XP, streak hoặc lịch M10.

Integration PostgreSQL staging 4/4 PASS: catalog 16 topic, endpoint topic legacy, paging/search/filter, và Quiz nhìn thấy đúng 358 thẻ gồm 58 thẻ import đủ điều kiện.

## 6. Android và Room

- Topics gọi `/topics/catalog`, hiển thị số lượng thật từ Backend.
- Room nâng schema 1 → 2 bằng migration `ALTER TABLE cached_topics ADD COLUMN flashcardCount INTEGER NOT NULL DEFAULT 0`; không dùng destructive migration.
- Thêm màn tìm từ vựng có English/Vietnamese search, CEFR filter, page 20 và Previous/Next. Android không tải toàn bộ dataset vào RAM.
- `VocabularyRepository` tiếp tục dùng Retrofit/Backend; không kết nối PostgreSQL trực tiếp.
- Home đổi mô tả thành “Học theo các chủ đề”; không ghi cứng 10 topic.
- Quiz, Flashcard, Settings cache, navigation và offline Room tiếp tục dùng luồng hiện có.

Nghiệm thu Emulator API 35 với package E2E riêng và Backend staging cổng 3001:

- Phiên đăng nhập giữ nguyên khi cài `adb install -r`.
- 16 topics và FOOD 43 thẻ hiển thị đúng.
- Thẻ mới theo level: `breakfast` A1, `steak` A2, `bacon` B1, `pancake` B2.
- Lật thẻ hiện nghĩa/ví dụ; Previous/Next hoạt động.
- Tìm `breakfast` trả kết quả cũ và mới; bộ lọc/paging hoạt động.
- Quiz dùng thẻ mới `pizza`; submit và màn kết quả PASS, không gửi trùng.
- Tắt Backend, force-stop và mở lại: 16 topics và FOOD 43 vẫn đọc từ Room cùng trạng thái Offline.
- Khởi động lại Backend và Retry: dữ liệu online trở lại, banner offline mất, phiên vẫn còn.
- Drawer phải, Settings và navigation regression PASS; không crash.
- Data Settings tại thời điểm chụp: 16 topics, 55 cached cards, 325,5 KB.

16 ảnh chạy thật nằm tại `android-app/app/build/reports/m13-large-vocabulary/`.

## 7. Kiểm thử đã chạy

| Lệnh | Kết quả |
|---|---|
| `npm run vocab:audit-source` | PASS |
| `npm run vocab:build` | PASS; 24.388 → 58 approved |
| `python -m unittest tests.test_large_vocabulary_pipeline` | PASS 8/8 |
| `npm run vocab:import:dry-run` | PASS; không ghi production |
| `node .m13-large-verification/isolated.mjs` | PASS migration/rollback/idempotency/isolation |
| `npm run test:m13:integration` trên staging | PASS 4/4 |
| `npm run typecheck` | PASS |
| `npm run build` | PASS |
| `npm test` | PASS 320/320 |
| `admin-web: npm test -- --run` | PASS 42/42 |
| `admin-web: npm run build` | PASS |
| `:app:assembleDebug` | PASS |
| `:app:testDebugUnitTest` | PASS 96/96 |
| `:app:lintDebug` | PASS, 0 lỗi |
| `:app:assembleE2e` | PASS |
| `adb install -r <E2E APK>` | PASS, giữ dữ liệu |
| `:app:connectedAndroidTest` | PASS 8/8 trên `BiVoca_API35_Pixel6` |

Một lệnh `:app:connectedE2eAndroidTest` được thử nhưng task này không tồn tại; task đúng của dự án là `:app:connectedAndroidTest` và đã PASS 8/8. Đây là lỗi tên task, không phải test failure của ứng dụng.

## 8. File thêm/chỉnh sửa

### Backend và dữ liệu

- `backend/scripts/audit_en_vi_source.py`
- `backend/scripts/large_vocabulary_pipeline.py`
- `backend/scripts/import-large-vocabulary.ts`
- `backend/tests/test_large_vocabulary_pipeline.py`
- `backend/tests/m13-large.database.integration.ts`
- `backend/migrations/1792533600000_add_vocabulary_provenance.sql`
- `backend/src/modules/topics/{topic.route,topic.controller,topic.service,topic.repository}.ts`
- `backend/src/modules/flashcards/{flashcard.route,flashcard.controller,flashcard.service,flashcard.repository}.ts`
- `backend/src/modules/quiz/quiz.repository.ts`
- các repository double trong backend tests để khớp interface đã mở rộng
- `backend/package.json`, `backend/package-lock.json`, `backend/.gitignore`
- `backend/datasets/en-vi/{ATTRIBUTION.md,LICENSE,SOURCE_SCHEMA.sql}`
- `backend/datasets/processed/en_vi_source_audit.json`
- `backend/datasets/processed/bivoca_vocab_{candidates,approved,review_queue,rejected}.jsonl`
- `backend/datasets/processed/bivoca_topics.json`
- `backend/datasets/processed/bivoca_vocab_statistics.json`
- `backend/datasets/processed/bivoca_vocab_validation_report.md`
- `backend/.m13-large-verification/{isolated.mjs,state.json}`

### Android

- `data/api/TopicApi.java`, `data/api/VocabularyApi.java`
- `data/model/Topic.java`, `data/model/FlashcardPage.java`
- `data/local/LearningDatabase.java`, `data/local/TopicEntity.java`
- `data/repository/VocabularyRepository.java`
- `ui/search/VocabularySearchActivity.java`, `VocabularySearchAdapter.java`, `VocabularySearchViewModel.java`
- `ui/topics/TopicAdapter.java`, `TopicsActivity.java`
- `res/layout/activity_topics.xml`, `activity_vocabulary_search.xml`, `item_vocabulary_search.xml`
- `res/values/strings.xml`, `AndroidManifest.xml`
- `androidTest/.../RoomLearningCacheTest.java`
- `app/schemas/com.example.bivoca.data.local.LearningDatabase/2.json`

### Tài liệu

- `M13_LARGE_VOCABULARY_IMPLEMENTATION_REPORT.md`
- `WORK_STATUS.md`, `BACKEND_CONTEXT.md`, `ANDROID_CONTEXT.md`

## 9. Giới hạn và kế hoạch tiếp theo

- 23.990 ứng viên cần review, phần lớn do từ đa nghĩa hoặc topic chưa đủ bằng chứng. Muốn tiến đến 5.000 cần quy trình duyệt sense/POS và topic có người kiểm tra, hoặc nguồn song ngữ có example/sense alignment tốt hơn.
- CC BY-SA 4.0 yêu cầu giữ attribution và xem xét nghĩa vụ share-alike khi phân phối database phái sinh. Trước production cần rà soát license theo cách phát hành thực tế.
- Production hiện chưa có migration M13/API catalog/dataset mới. Android build có contract catalog mới, vì vậy rollout phải đồng bộ Backend migration + Backend release trước hoặc cùng Android release.
- Chưa có lệnh nào migration/import vào `flashcard_english`; chưa triển khai hàng nghìn thẻ; chưa thay dữ liệu người dùng vận hành.
- Tài khoản E2E staging đã được xóa theo email chính xác; credential tạm đã xóa. Database staging và bằng chứng không chứa mật khẩu/token.

