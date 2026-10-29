# M15.5 — Vocabulary Expansion 5.000 → 20.000

**Ngày kiểm tra:** 2026-10-25  
**Trạng thái:** `QUALITY GATE FAILED / PRODUCTION NOT READY`  
**Production:** `flashcard_english` vẫn giữ 5.000 Flashcard và 19 chủ đề

## 1. Kết quả chính

Pipeline M15.5 đã tạo và xuất bản thử thành công đúng ba mốc trên PostgreSQL cô lập
`bivoca_m15_5_20261024`:

| Mốc | Flashcard | Headword khác nhau | Duplicate English + Vietnamese |
|---|---:|---:|---:|
| Baseline | 5.000 | 1.956 | 0 |
| Đợt 1 | 10.000 | 6.796 | 0 |
| Đợt 2 | 15.000 | 11.585 | 0 |
| Đợt 3 | 20.000 | 16.159 | 0 |

Database staging cuối có 20 chủ đề. Chủ đề mới `GENERAL_VOCABULARY` chứa từ vựng tổng quát
chưa có đủ bằng chứng để gán vào một trong 19 chủ đề chuyên biệt. Cách này tránh gán chủ đề sai
chỉ để đủ số lượng. Không nhân bản cùng nghĩa sang nhiều chủ đề.

Production được kiểm tra lại sau toàn bộ quy trình: vẫn 5.000 Flashcard, 19 chủ đề, 5 user,
14 progress, 12 Quiz result, 2 user score và 5 XP history. Không migration hoặc import nào chạy
trên production.

## 2. Nguồn và giấy phép

- CEFR: `backend/datasets/cefr/word_list_cefr.csv`, 7.988 dòng, giấy phép MIT.
- Nghĩa Việt, POS, IPA và ví dụ: Skypedia English-Vietnamese Dictionary, commit
  `388adc0826300912e5b0311c089e0e70494b065f`, CC BY-SA 4.0.
- SHA-256 SQLite: `2b65bea9577c2a4149caa90940c806f321d1c6cfac503ed8bbd9f93e43a0033a`.
- Nguồn hiện có đủ tạo 20.000 thẻ đạt cổng cấu trúc, nên không tải thêm WordNet, Wiktionary
  hoặc Tatoeba trong lượt này.
- Không tự dịch, không tạo IPA/câu ví dụ và không suy luận CEFR. Khi headword + POS không có
  trong nguồn CEFR đã ghim, `level` giữ `NULL`.

## 3. Audit nguồn và phân loại chất lượng

SQLite nguồn giữ kết quả audit: 104.829 words, 158.307 definitions, 175.735 quan hệ nghĩa,
108.776 pronunciation, 175.735 quan hệ có example, integrity `ok`, 0 FK violation.

Pipeline duyệt đủ 175.735 quan hệ nguồn:

| Phân loại | Số lượng |
|---|---:|
| `READY_TO_APPROVE` | 22.000 |
| `NEEDS_MANUAL_REVIEW` | 116.706 |
| `REJECTED` | 37.012 |
| `DUPLICATE_EXISTING` trong nguồn chuẩn hóa | 17 |
| `MISSING_SOURCE_LICENSE` | 0 |

Artifact lưu toàn bộ 22.000 ứng viên sẵn sàng và 116.706 ứng viên review. File rejected giữ
1.017 mẫu để kiểm tra; thống kê lý do vẫn tính đủ 37.029 dòng bị loại. Các lý do chính:

- Example không chứa headword: 10.728.
- Example chung/placeholder: 14.404.
- Nghĩa chỉ trỏ sang mục khác hoặc mô tả dạng chia: 14.156.
- POS không hỗ trợ: 2.656.
- Nhãn usage không cốt lõi: 1.630.
- Headword không phù hợp định dạng học: 549.
- Nội dung không phù hợp: 120.
- Markup không an toàn: 107.
- Nghĩa Việt thiếu/sai định dạng: 84.
- Trùng nghĩa chuẩn hóa: 17.
- CEFR xung đột: 6.

Pipeline ưu tiên đa dạng headword rồi giới hạn tối đa ba nghĩa có quan hệ nguồn rõ ràng cho mỗi
headword/POS. Preview PostgreSQL dùng đúng uniqueness thật `LOWER(english), LOWER(vietnamese)`.

## 4. CEFR, chủ đề và Quiz

Phân bố 20.000 Flashcard staging:

- A1: 1.682
- A2: 1.579
- B1: 1.902
- B2: 1.584
- Chưa xác định: 13.253

13.253 thẻ chưa xác định CEFR có 0 provenance giả từ `WORDS_CEFR_2024`.

Quiz Quality Guard trả 14.982/20.000 thẻ đủ điều kiện. 5.018 thẻ còn lại vẫn dùng để học/tra cứu
nhưng không mặc nhiên tham gia Quiz. Guard tiếp tục yêu cầu provenance approved và
`quiz_eligible=true`; 0 duplicate English + Vietnamese trong bảng Flashcard cuối.

## 5. Backup, restore và bảo toàn dữ liệu

Backup mới:

`backend/.m15-5-verification/flashcard_english_before_m15_5.dump`

- Kích thước: 5.824.883 byte.
- SHA-256: `e9aabaed6c71e82be426b727e3cb71a6ff450de91527c26b46d260ed52bca57c`.
- Restore-check vào `bivoca_m15_5_20261024`: PASS, đúng 5.000 cards/19 topics/5 users/
  14 progress/12 Quiz/2 scores/5 XP.
- Trước Android E2E, hash toàn bộ 5.000 Flashcard gốc và các bảng users, progress, Quiz, scores,
  XP giữa production và staging cuối: tất cả `true`. Lượt Google Login sau đó chỉ cập nhật
  `users.last_login` trên staging; không thay đổi progress, Quiz, scores hoặc XP.
- Idempotency dry-run sau cùng: current 20.000, needed 0, approved 0, duplicate 0, batch 0.

Không có migration M15.5 vì schema hiện tại đã hỗ trợ `level NULL`, provenance, review queue,
batch publish và các index cần thiết. Không thực hiện Up/Down/Up giả khi không có migration mới.

## 6. Lỗi tìm thấy và sửa tối thiểu

1. Module review M13 bắt buộc CEFR trước publish. Điều này mâu thuẫn quy tắc giữ CEFR chưa xác
   định. `ensureApprovable` nay yêu cầu meaning/topic/example nhưng cho phép CEFR `NULL`.
2. Publisher cũ luôn thêm provenance CEFR. Nay chỉ thêm `WORDS_CEFR_2024` khi candidate thật sự
   có CEFR.
3. Preview duplicate cũ so sánh English + Vietnamese + POS, trong khi unique index thật chỉ là
   English + Vietnamese. Guard repository, prepare và publish đã được đồng bộ với constraint thật;
   đồng thời loại trùng nội bộ trong cùng lô.
4. Đợt publish 15k từng dừng ở 11.800 do lỗi duplicate trên. Batch lỗi rollback; các batch trước
   đã commit độc lập và hợp lệ. Sau sửa, dry-run còn đúng 3.200, 0 duplicate, rồi hoàn tất 15k.

## 7. Hiệu năng trên 20.000 Flashcard

Đo bằng `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)` trên DB cô lập:

| Truy vấn | Execution |
|---|---:|
| Search English/Vietnamese, limit 20 | 2,015 ms |
| Topic page, limit 20 | 0,110 ms |
| CEFR page, limit 20 | 0,153 ms |
| Quiz eligible pool, limit 40 | 0,401 ms |
| Admin Pending page, offset 1.000/limit 100 | 0,197 ms |

Public Flashcard API và Admin review đều phân trang phía server. Android dùng Retrofit paging
cho Vocabulary Search và kiến trúc Online-Only từ M13.8. Chủ đề mới `GENERAL_VOCABULARY`
được chuyển riêng sang Vocabulary Search có `topicCode`, 20 thẻ/trang; vì vậy không tải 14.933
thẻ của chủ đề này vào RAM. 19 chủ đề cũ giữ luồng Flashcard Previous/Next. App hiện không còn
Room cache.

## 8. Kiểm thử

- Backend `npm run typecheck`: PASS.
- Backend `npm run build`: PASS.
- Backend `npm test`: **338/338 PASS**.
- M15.5 PostgreSQL integration: **5/5 PASS**.
- Pipeline Python M13 + M15.5 (lần cuối): **15/15 PASS**.
- Admin Web: **43/43 PASS**, production build PASS.
- Android `assembleE2e`: PASS; `testDebugUnitTest`: **75/75 PASS**; `lintDebug`: PASS.
- Android runtime trên `BiVoca_API35_Pixel6`: đã chạy với Backend cổng 3001 và DB staging;
  chi tiết tại mục 13.
- Android Room: không áp dụng; Room đã bị loại khỏi runtime ở M13.8 Online-Only.

## 9. Lệnh chính đã chạy

```powershell
npx tsx scripts/m15-5-audit.ts
python scripts/audit_en_vi_source.py
python scripts/m15_5_vocabulary_pipeline.py --approved-capacity 22000
python scripts/cross_validate_vnedict.py
npx tsx scripts/prepare-m15-5-isolated.ts
$env:DB_NAME='bivoca_m15_5_20261024'
npx tsx scripts/load-vocabulary-review-candidates.ts --input-dir processed/m15_5 --dry-run
npx tsx scripts/load-vocabulary-review-candidates.ts --input-dir processed/m15_5 --apply --database-confirm bivoca_m15_5_20261024
npx tsx scripts/prepare-vocabulary-target.ts --input-dir processed/m15_5 --target-total 10000|15000|20000
npx tsx scripts/publish-vocabulary-target.ts --target-total 10000|15000|20000 --batch-size 100
npx tsx scripts/verify-m15-5-isolated.ts
npx tsx scripts/compare-m15-5-protected.ts
npm run typecheck
npm run build
npm test
npm run test:m15-5:integration
python -m unittest tests.test_large_vocabulary_pipeline tests.test_m15_5_vocabulary_pipeline
npx tsx scripts/sample-m15-5-final-quality.ts
python scripts/review_m15_5_final_sample.py
```

## 10. File thêm và chỉnh sửa

- `backend/scripts/m15_5_vocabulary_pipeline.py`
- `backend/scripts/m15-5-audit.ts`
- `backend/scripts/prepare-m15-5-isolated.ts`
- `backend/scripts/verify-m15-5-isolated.ts`
- `backend/scripts/compare-m15-5-protected.ts`
- `backend/scripts/load-vocabulary-review-candidates.ts`
- `backend/scripts/prepare-vocabulary-target.ts`
- `backend/scripts/publish-vocabulary-target.ts`
- `backend/scripts/sample-m15-5-quality.ts`
- `backend/scripts/review_m15_5_quality_sample.py`
- `backend/scripts/cross_validate_vnedict.py`
- `backend/scripts/sample-m15-5-final-quality.ts`
- `backend/scripts/review_m15_5_final_sample.py`
- `backend/scripts/audit_m15_5_datafix_sources.py`
- `backend/scripts/review_m15_5_datafix_sample.py`
- `backend/src/modules/vocabulary-review/vocabulary-review.schema.ts`
- `backend/src/modules/vocabulary-review/vocabulary-review.service.ts`
- `backend/src/modules/vocabulary-review/vocabulary-review.repository.ts`
- `backend/tests/test_m15_5_vocabulary_pipeline.py`
- `backend/tests/m15-5.database.integration.ts`
- `backend/package.json`
- `backend/datasets/processed/m15_5/*`
- `backend/datasets/processed/m15_5/vnedict_cross_validation.jsonl`
- `backend/datasets/vnedict/source/*` (mirror nguồn và giấy phép VNEDICT)
- `backend/datasets/quality/m15_5/curated_semantic_overrides.csv`
- `backend/datasets/quality/m15_5/README_DataFix.md`
- `backend/.m15-5-datafix-pack/*` (gói gốc đã giải nén để đối chiếu)
- `backend/.m15-5-verification/quality-sample-500-final.{json,csv}`
- `backend/.m15-5-verification/quality-review-500-final.{json,csv}`
- `backend/.m15-5-verification/quality-sample-500-datafix.{json,csv}`
- `backend/.m15-5-verification/quality-review-500-datafix.{json,csv}`
- `android-app/app/src/main/java/com/example/bivoca/data/repository/VocabularyRepository.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/search/VocabularySearchActivity.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/search/VocabularySearchViewModel.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/topics/TopicsActivity.java`
- `android-app/app/src/main/res/layout/activity_vocabulary_search.xml`
- `android-app/app/src/main/res/layout/activity_settings_detail.xml`
- `M15_5_VOCABULARY_20000_REPORT.md`
- `WORK_STATUS.md`, `BACKEND_CONTEXT.md`, `ANDROID_CONTEXT.md`

## 11. Bước production còn chờ

Chưa ghi 15.000 thẻ mới vào `flashcard_english`. Không được chuyển sang production chỉ bằng một
lần phê duyệt triển khai mới: trước hết phải khắc phục quality gate 21/500 và có bằng chứng kiểm
duyệt độc lập đạt yêu cầu. Sau đó vẫn phải tạo backup mới ngay trước import, restore-check lại,
chạy ba lô có checkpoint 10k/15k/20k, kiểm tra mỗi mốc và cuối cùng nghiệm thu Android trên
Emulator/thiết bị thật. Không dùng lại backup này thay cho backup
ngay trước deployment nếu production đã thay đổi. Quality gate hiện chưa cho phép thực hiện bước
này; mẫu độc lập cuối đã lấy nhưng vẫn còn 21/500 hàng bị đánh dấu và phải được xử lý trước.

## 12. Quality gate thủ công ngày 2026-10-24

### 12.1 Cách lấy mẫu

Script `backend/scripts/sample-m15-5-quality.ts` lấy mẫu cố định 240 Flashcard chỉ thuộc lô M15.5
(`original_payload.schemaVersion=2`). Một nửa mẫu ưu tiên phủ topic/POS/CEFR, phần còn lại lấy theo
hash seed cố định `M15.5-QG-20261024`. Mẫu phủ 13 topic có thẻ mới, đủ 7 POS đang hỗ trợ và đủ
A1/A2/B1/B2/CEFR chưa xác định. Toàn bộ nghĩa/POS/IPA/example dùng nguồn Skypedia; 66 thẻ có
CEFR từ nguồn MIT và 174 thẻ giữ CEFR chưa xác định.

Mỗi hàng được đọc đồng thời English, Vietnamese, POS, IPA và example. Kết quả không dùng cờ
`APPROVED_AUTO` hoặc việc PASS schema để thay cho đánh giá nội dung:

| Kết quả thủ công | Số thẻ | Tỷ lệ |
|---|---:|---:|
| PASS trong phạm vi kiểm tra mẫu | 187 | 77,9% |
| Cần người duyệt lại | 53 | 22,1% |

Nhóm lỗi chính gồm 9 IPA đáng ngờ; 7 nghĩa và example không cùng nghĩa; 7 example thiếu tự nhiên;
5 example không truyền đạt nghĩa; 5 nghĩa Việt đáng ngờ; 4 nghĩa chỉ phiên âm/không giải thích;
cùng các trường hợp POS lệch, nghĩa bị cắt, lỗi chính tả, dạng chia động từ và thuật ngữ cũ/nhạy cảm.
Danh sách từng hàng và lý do nằm tại:

- `backend/.m15-5-verification/quality-review-240.json`
- `backend/.m15-5-verification/quality-review-240.csv`

### 12.2 Lỗi pipeline đã sửa và chạy lại

Mẫu ban đầu cho thấy lỗi hệ thống: nghĩa dạng `Xem ...`, mô tả số nhiều/ngôi chia, example mẫu chung
và gán topic chỉ từ một từ khóa dịch. Pipeline đã được sửa để:

- Đưa nghĩa tham chiếu/dạng chia và example mẫu chung sang loại không tự duyệt.
- Không dùng một từ khóa nghĩa Việt để tự gán topic chuyên biệt; chỉ category từ nguồn CEFR đã
  ghim mới được gán tự động, còn lại giữ `GENERAL_VOCABULARY`.
- Giữ nguyên nguồn, không tự viết lại nghĩa, IPA, CEFR hoặc example.

Sau sửa, pipeline tăng số bị loại từ 19.714 lên 37.029, sau đó database staging được dựng lại và
publish lại đúng ba checkpoint 10.000/15.000/20.000. Mẫu 240 nêu trên được lấy sau lần dựng lại.
Việc vẫn còn 53 thẻ cần review cho thấy nguồn còn lỗi ngữ nghĩa mà rule cấu trúc không thể xác nhận
an toàn. Vì vậy production chưa đạt quality gate.

## 13. Android E2E trên staging 20.000

APK `com.example.bivoca.e2e` dùng `http://10.0.2.2:3001/api/v1/`; Backend cổng 3001 kết nối đúng
`bivoca_m15_5_20261024`. Kết quả chạy thật:

- Google Credential Manager hiển thị account chooser; chọn tài khoản đã liên kết và vào app thành
  công. Staging vẫn 5 users; không sinh user trùng.
- Home tải Daily Goal `0/10`; Progress 7 ngày, SRS, Profile và Settings tải thành công.
- Danh mục trả đúng 20 topic. General Vocabulary hiển thị 14.933 thẻ.
- Search English `airline`: 2 kết quả. Search bằng nghĩa Việt không dấu `va li`: 4 kết quả đúng;
  API UTF-8 `Công ty hàng không`: 1 kết quả `airline`.
- General Vocabulary trang 1/747 và trang 2/747 tải 20 bản ghi/trang; tiếp tục đến trang 9 không
  crash. PSS tăng từ 77.470 KB lên 79.177 KB, RSS từ 189.268 KB lên 191.592 KB, phù hợp paging
  20 bản ghi và không có dấu hiệu nạp 20.000 thẻ vào RAM.
- Flashcard Animals lật mặt nghĩa/example, Previous/Next hoạt động. Google TTS nhận request
  `eng-GBR`; không ghi token hay dữ liệu nhạy cảm.
- Quiz Animals tải câu 1/10 và bốn lựa chọn. Không submit đáp án để tránh tạo lịch sử thử nghiệm.
- Không ghi nhận crash hoặc ANR trong luồng kiểm tra.

Lỗi runtime tìm thấy: màn General từng gửi `GET /flashcards?search=&topicCode=...`, Backend trả
HTTP 400 vì `search` phải có ít nhất một ký tự. `VocabularyRepository` nay gửi `null` để Retrofit
bỏ tham số rỗng. Build/test/lint PASS và General paging PASS sau khi cài lại APK.

Ảnh và UI dump nằm trong `android-app/runtime-evidence/`, gồm Login, Google chooser, Topics,
English/Vietnamese search, General trang 1/2/9, Flashcard, Quiz, Progress và About.

## 14. Attribution CC BY-SA 4.0

Project giữ commit, checksum, URL, `LICENSE` và `ATTRIBUTION.md` của nguồn. Khi phân phối database
đã lọc/phân loại, phải ghi công Skypedia và MinhQND, liên kết CC BY-SA 4.0, nói rõ BiVocal đã sửa
đổi/lọc/phân loại, và cung cấp dữ liệu phái sinh theo cùng giấy phép hoặc giấy phép tương thích,
không thêm hạn chế trái phép.

Trước quality gate này, attribution chỉ có trong repository. Màn Settings → Giới thiệu nay hiển thị
nguồn, thay đổi, CC BY-SA 4.0 và link nguồn; APK E2E đã xác minh text này xuất hiện. Khi đóng gói
đồ án, cần kèm nguyên `backend/datasets/en-vi/ATTRIBUTION.md` và `LICENSE` trong bộ bàn giao.

## 15. Kế hoạch production theo checkpoint

Preflight chỉ đọc xác nhận `flashcard_english`: 5.000 cards, 19 topics, 5 users, 14 progress,
12 Quiz results, 3 Google identities, 9.400 provenance rows và 0 nhóm duplicate. Chưa có thao tác
ghi production trong lượt này.

Khi một mẫu độc lập mới đạt ngưỡng được duyệt, quy trình dự kiến:

1. **Trước 5k → 10k:** tạo backup mới, SHA-256, restore vào DB tên mới; lưu count và hash các bảng
   users/progress/quiz/scores/xp/Google identities cùng 5.000 thẻ gốc. Chạy load candidates và
   dry-run; dừng nếu target không đúng 5.000 bản ghi mới, có duplicate hoặc provenance thiếu.
2. **Publish 10k:** batch transaction 100 thẻ; mỗi batch lỗi tự rollback. Xác minh đúng 10.000,
   duplicate 0, provenance/Quiz eligibility hợp lệ và hash dữ liệu người dùng không đổi. Tạo
   checkpoint backup trước đợt kế.
3. **10k → 15k:** lặp lại dry-run, kiểm tra đúng 5.000 ID chưa publish, rồi batch publish và kiểm
   tra tại mốc 15.000. Dừng an toàn tại checkpoint 10.000 nếu có sai lệch.
4. **15k → 20k:** lặp lại cùng guard, kiểm tra cuối đúng 20.000/20 topics, 0 duplicate, count/hash
   dữ liệu bảo vệ không đổi; sau đó mới chuyển Backend production và nghiệm thu Android.

Không dùng `DROP`, `TRUNCATE`, reset sequence hay chạy lại import 5.000 thẻ gốc. Không tự rollback
toàn database production nếu chưa có phê duyệt; khi lỗi chỉ dừng tại batch/checkpoint gần nhất.

## 16. Kết luận production readiness

Android E2E và hạ tầng staging đạt yêu cầu kỹ thuật sau hotfix paging. Tuy nhiên **chưa đủ điều kiện
import production** vì 53/240 thẻ trong mẫu hậu-fix vẫn cần kiểm tra nội dung. M15.5 giữ trạng thái
`QUALITY GATE FAILED / PRODUCTION NOT READY`; production tiếp tục ở 5.000 Flashcard.

## 17. Final quality gate 2026-10-25 (thay thế số liệu hiện hành ở các mục 1, 12 và 16)

### 17.1 Pipeline và nguồn đối chiếu độc lập

Sau quality gate 240 mẫu, pipeline được siết thêm để loại nghĩa tham chiếu, mô tả dạng chia,
định nghĩa hỏng, nghĩa trùng headword và các xung đột POS/nghĩa có thể nhận biết chắc chắn. IPA và
example của 15.000 ứng viên mới được đặt `NULL` thay vì xuất bản dữ liệu chưa đủ tin cậy; bản gốc
vẫn nằm trong audit payload để truy vết, không bị trình bày như dữ liệu đã xác minh.

VNEDICT được thêm làm nguồn kiểm tra chéo, không dùng để tự tạo nghĩa. Bản mirror ghi rõ từ điển
gốc của Paul Denisowski theo CC BY 3.0 và yêu cầu attribution. Trong 149.544 ứng viên đạt cổng cấu
trúc, 16.960 ứng viên có cụm nghĩa Việt khớp chéo; 132.584 ứng viên không được xác nhận bởi nguồn
thứ hai. Attribution và source metadata được giữ trong artifact.

Artifact cuối (`inputSha256 =
8793c474cb475701dd8040e0e88471b027918ab963de1425e61d01bfe8a492f4`) có:

| Trạng thái | Số lượng |
|---|---:|
| Materialized candidates | 149.544 |
| Approved cho staging | 22.000 |
| Needs review | 127.521 |
| Rejected | 26.214 |
| Approved có CEFR A1/A2/B1/B2 | 14.257 |
| Approved CEFR chưa xác định | 7.743 |

### 17.2 Staging cuối và checkpoint

Production được backup mới, restore-check vào database cô lập và chỉ được đọc. File backup:
`backend/.m15-5-verification/flashcard_english_before_m15_5.dump`, SHA-256
`5271b4d99df9afbdd48a7ee3dd895de22134c1df537b9dc9013316aaecf6f133`.

Database cô lập `bivoca_m15_5_20261024` được dựng lại từ backup. Ba checkpoint 5.000→10.000,
10.000→15.000 và 15.000→20.000 đều chạy 50 batch × 100 thẻ; dry-run trước mỗi mốc báo 0
duplicate. Snapshot cuối:

| Chỉ số | Kết quả |
|---|---:|
| Flashcard | 20.000 |
| English headword khác nhau | 9.869 |
| Chủ đề | 20 |
| Duplicate English + Vietnamese | 0 |
| Thiếu trường bắt buộc | 0 |
| CEFR có nguồn | 12.257 |
| CEFR chưa xác định | 7.743 |
| Quiz eligible | 6.680 |
| `GENERAL_VOCABULARY` | 14.120 |
| CEFR provenance giả | 0 |

Counts và checksum của 5.000 thẻ gốc, users, progress, Quiz results, scores và XP đều khớp
snapshot production. Truy vấn search/topic/CEFR/Quiz/admin pending lần lượt mất khoảng
2,823/0,160/0,194/0,355/0,662 ms trong lần đo staging này.

### 17.3 Mẫu độc lập mới 500 thẻ

Mẫu mới dùng seed `M15.5-FINAL-QG-20261025`, lấy từ đúng 15.000 thẻ được thêm vào staging và
không tái sử dụng mẫu 240. Mẫu phủ 18 topic, đủ 7 POS, các mức A1/A2/B1/B2 và CEFR chưa xác định;
431/500 hàng có cờ đối chiếu VNEDICT. Phương pháp review được ghi đúng là
`AI-assisted field-by-field semantic review`; số hàng được người duyệt thủ công là **0**.

| Kết quả | Số thẻ | Tỷ lệ |
|---|---:|---:|
| PASS trong mẫu | 479 | 95,8% |
| Cần xem xét lại | 21 | 4,2% |

21 trường hợp gồm 6 POS/meaning mismatch, 4 nghĩa Việt không khớp, 3 lỗi chính tả, 2 metadata
từ điển lọt vào nghĩa, 2 định nghĩa hỏng, 1 nghĩa chưa đầy đủ, 1 nghĩa không phải tiếng Việt,
1 nghĩa bị hỏng định dạng và 1 nghĩa đúng một phần. Ví dụ thực tế: `wrist → Cổ tay áo`,
`differently → In a different way.`, `ecocide → Thuốc hủy diệt sinh thái.`. Điều này chứng minh
đối chiếu từ khóa với nguồn thứ hai vẫn có false positive và chưa đủ thay cho kiểm duyệt nội dung.

Chi tiết từng hàng:

- `backend/.m15-5-verification/quality-sample-500-final.json`
- `backend/.m15-5-verification/quality-sample-500-final.csv`
- `backend/.m15-5-verification/quality-review-500-final.json`
- `backend/.m15-5-verification/quality-review-500-final.csv`

### 17.4 Kết quả test cuối

- Pipeline Python: **15/15 PASS**.
- Backend typecheck/build: **PASS**; unit/integration chuẩn: **338/338 PASS**.
- M15.5 PostgreSQL integration: lần đầu có 2 assertion ngưỡng cũ không còn phù hợp sau chính
  sách CEFR/Quiz mới; test được đổi thành đối chiếu động với SQL thực tế, chạy lại **5/5 PASS**.
- Admin Web: **43/43 PASS**, production build PASS.
- Android `assembleDebug`, `testDebugUnitTest`, `lintDebug`: **BUILD SUCCESSFUL**. Unit test dùng
  lại kết quả 75/75 đã có; Gradle báo task up-to-date trong lần cuối.
- Không chụp bộ ảnh runtime mới sau lần rebuild VNEDICT. Bộ ảnh staging 20.000 trước đó tại
  `android-app/runtime-evidence/` vẫn chứng minh paging/search/Quiz/navigation, nhưng không được
  dùng để tuyên bố nội dung của artifact cuối đã PASS ngôn ngữ.

### 17.5 Quyết định cuối

**Không triển khai production.** Quality gate cuối thất bại vì còn 21/500 bản ghi cần xem xét.
Không hạ tiêu chí, không xuất bản từng phần và không tự sửa nghĩa để đạt số lượng. Database
`flashcard_english` vẫn giữ **5.000 Flashcard, 19 chủ đề** cùng toàn bộ dữ liệu người dùng cũ.
M15.5 giữ trạng thái `QUALITY GATE FAILED / PRODUCTION NOT READY` cho đến khi có nguồn nghĩa
tin cậy hơn hoặc quy trình kiểm duyệt đủ để loại các false positive còn lại.

## 18. DataFix Pack và quality gate độc lập hậu-sửa — 2026-10-25

### 18.1 Tiếp nhận và chính sách áp dụng

Đã giải nén `BiVoca_M15_5_DataFix_Pack.zip` vào `backend/.m15-5-datafix-pack/`, đọc đầy đủ
`README_Codex.md` và chạy `test_bivoca_semantic_guard.py`: **2/2 PASS**. Pack có 137 override:
80 `BLOCK_REVIEW`, 57 `SUGGEST_REPLACE`; tất cả đều ghi `requires_source_check=true` và không có
hàng nào là `HUMAN_APPROVED`.

Script `audit_m15_5_datafix_sources.py` kiểm tra đề xuất với VNEDICT CC BY 3.0. Chỉ 8/57 đề xuất
có toàn bộ clause khớp chính xác nguồn. Để không biến đề xuất AI thành dữ liệu đã duyệt, pipeline
không ghi đè bất kỳ nghĩa nào. Cả 137 nghĩa gốc được đối chiếu bằng đúng bộ ba normalized
**English + POS + original Vietnamese**, gắn `DATAFIX_KNOWN_SEMANTIC_ISSUE`, loại khỏi pool và
thay bằng ứng viên nguồn khác. ID/sample_id không được dùng để match.

Guard của ZIP được chạy trên toàn bộ 148.707 candidates sau sửa và trên 22.000 approved. Tập
approved cuối có **0 BLOCK_REVIEW và 0 duplicate English+Vietnamese**; guard vẫn ghi rõ
`UNASSESSED_NOT_AUTO_APPROVED`, không được hiểu là thẩm định ngữ nghĩa của con người.

### 18.2 Artifact và staging dựng lại

Artifact cuối có SHA-256
`7aaa11a90eb1b75b07c3137d8d77e4ba0e2958cea5b073492649d14e56a5500c`:

- 175.735 source rows; 148.707 materialized candidates.
- 22.000 `APPROVED_AUTO`, 126.467 `NEEDS_REVIEW`, 27.268 rejected.
- 137 `DATAFIX_KNOWN_SEMANTIC_ISSUE`; 240 duplicate English+Vietnamese across POS bị loại.
- 6.698 approved Quiz eligible; 10.942 approved headword khác nhau.
- IPA/example chưa xác minh tiếp tục giữ `NULL`; CEFR không có nguồn giữ `NULL`.

Backup/restore-check production PASS. Backup mới tại
`backend/.m15-5-verification/flashcard_english_before_m15_5.dump`, SHA-256
`a768cdb60898fa3a744081d4ccb34526baf59307f52f4dce06de8b9d8629ce0a`; database cô lập khởi
đầu đúng 5.000 cards/19 topics/5 users/14 progress/12 Quiz/2 scores/5 XP.

Staging `bivoca_m15_5_20261024` được publish lại theo đúng checkpoint 10K, 15K, 20K; mỗi đợt 50
batch × 100 và dry-run duplicate = 0. Publisher được siết để chỉ nhận approval mang marker đúng
`targetTotal + inputSha256`, tránh lấy nhầm approval cũ trong bản restore. Snapshot cuối:

| Chỉ số | Kết quả |
|---|---:|
| Flashcard | 20.000 |
| Headword khác nhau | 9.928 |
| Topics | 20 |
| Duplicate groups | 0 |
| Missing required | 0 |
| CEFR có nguồn | 12.355 |
| CEFR chưa xác định | 7.645 |
| Quiz eligible | 6.660 |
| General Vocabulary | 14.275 |

Hash 5.000 thẻ gốc, users, progress, Quiz, scores và XP so với production đều `true`.

### 18.3 Quality gate độc lập mới

Mẫu mới dùng seed `M15.5-DATAFIX-QG-20261025`, gồm 500 thẻ trong đúng 15.000 additions, phủ 18
topics, 7 POS và đủ A1/A2/B1/B2/CEFR NULL. Đây không phải mẫu 240/500 trong ZIP. Review được ghi
đúng là `Independent AI-assisted field-by-field semantic review; human-reviewed count: 0`.

| Kết quả | Số thẻ | Tỷ lệ |
|---|---:|---:|
| PASS trong mẫu | 476 | 95,2% |
| Cần xem xét | 24 | 4,8% |

Lỗi mới gồm POS/meaning mismatch, metadata từ điển, typo, headword case/sense/number mismatch và
nghĩa chưa đầy đủ. Ví dụ: `deceive → Làm thất vọng`, `track → Đóng trăn, tra tấn`,
`pasteboard` mang POS noun nhưng nghĩa adjective, `regulation` mang POS noun nhưng nghĩa
adjective. Vì đây là lỗi ngoài danh sách ZIP, việc tiếp tục chặn từng mẫu rồi lấy mẫu mới sẽ chỉ
che lỗi nguồn chứ không chứng minh 15.000 thẻ đã đạt chất lượng.

Evidence:

- `backend/.m15-5-verification/datafix-source-audit.json`
- `backend/.m15-5-verification/datafix-full-candidates/qa_summary.json`
- `backend/.m15-5-verification/datafix-approved-final/qa_summary.json`
- `backend/.m15-5-verification/quality-sample-500-datafix.{json,csv}`
- `backend/.m15-5-verification/quality-review-500-datafix.{json,csv}`

### 18.4 Regression và quyết định production

- Pipeline M13 + M15.5: **17/17 PASS**; semantic guard pack: **2/2 PASS**.
- Backend typecheck/build PASS, **338/338 tests PASS**.
- M15.5 PostgreSQL integration trên staging: **5/5 PASS**.
- Admin Web: **43/43 PASS**, build PASS.
- Android `assembleDebug`, `testDebugUnitTest`, `lintDebug`: BUILD SUCCESSFUL; tasks unit/build
  phần lớn up-to-date. `adb devices -l` không có thiết bị, nên không có runtime screenshot mới.
- Không sửa Android hoặc Admin trong vòng DataFix này.

**Quyết định: không import production.** Quality gate độc lập còn 24/500 lỗi nên chưa có bằng
chứng rằng đủ 15.000 thẻ bổ sung đáng tin cậy. Production giữ nguyên 5.000 cards/19 topics và dữ
liệu người dùng. Backup được giữ lại; không chạy checkpoint production và không báo M15.5 DONE.

## 19. Repair v2 — hard blocks và semantic publication guard (2026-10-25)

### 19.1 Xác minh gói

Đã giải nén `BiVocal_M15_5_Repair_v2.zip`, đọc `README_Codex.md`, `data/summary.json` và
`PROMPT_GUI_CODEX.txt`. Checksum đầu vào khớp manifest:

- 22.000 candidates: `c0e257dc43541f57408169438ad8b7b5abf663bed754eb9e2a8bfd53afc8ed5e`.
- 15.000 staging additions: `a1ca8b52a677093de508331575939248142fea684d529b317c5f2ba4c89689ef`.

Ba script read-only trong README đã chạy lại. Kết quả **10/10 PASS** về count, identity, exact
triple, uniqueness, selection integrity và không giả human approval. Đây là integrity test, không
phải chứng nhận nghĩa đúng.

### 19.2 Thay đổi pipeline

- 57 `BLOCK` của Repair v2 được lưu trong `datasets/quality/m15_5/repair_v2/` và match bằng
  normalized English + POS + Vietnamese gốc. Không dùng staging ID; artifact mới còn **0/57**
  hard block.
- Hai file đề xuất 15.000 không được nạp vào pipeline hay database. Các nghĩa thay thế cùng
  headword/POS không được tự dùng vì chưa có xác minh sense độc lập.
- `cross_validate_vnedict.py` không còn gọi substring match là meaning confirmation. Output nay
  ghi `lexicalOverlap=true`, `semanticVerified=false` và
  `matchMethod=VNEDICT_VIETNAMESE_SUBSTRING_OVERLAP`.
- Candidate giữ trường tương thích `independentMeaningConfirmed=false`; thông tin VNEDICT nằm ở
  `independentLexicalOverlap*`. Lexical overlap không được ưu tiên trong candidate selection.
- `prepare-vocabulary-target.ts` chỉ chọn candidate có `source.semanticVerified=true`;
  `publish-vocabulary-target.ts` kiểm tra lại cùng điều kiện tại database. Dry-run yêu cầu thêm một
  thẻ đã dừng với `Only 0 unpublished validated candidates are available`, đúng fail-closed.

Pipeline build sau sửa tạo 22.000 candidate đạt preflight cấu trúc, SHA-256
`e6c775474998bffdf5b792f3d6b5528889dfa6d6485cdbd1d374da0ba25eb3ca`:

| Chỉ số | Kết quả |
|---|---:|
| Repair v2 hard block còn trong 22K | 0 |
| Lexical overlap VNEDICT | 7.613 |
| Semantic verified | **0** |
| REVIEW từ Repair v2 vẫn nằm trong 22K mới | 1.466 |
| Candidate mới ngoài tập triage Repair v2 | 7.903 |
| Có thể publish an toàn theo semantic guard | **0 thẻ bổ sung** |

Việc 7.903 candidate thay đổi ngoài tập Repair v2 là hệ quả bỏ ưu tiên lexical substring và cho
thấy không thể dùng kết quả triage cũ để chứng nhận artifact mới. Không tiếp tục vòng lấy mẫu 500;
toàn bộ candidate chưa có semantic evidence được giữ ngoài publication.

### 19.3 Bảng nhóm cần người duyệt

Số liệu dưới đây là triage của Repair v2; một hàng có thể mang nhiều cờ:

| Nhóm | Số lượng | Cách xử lý cụ thể |
|---|---:|---|
| Hard block đã biết | 57 | Đã loại exact triple; không sửa nghĩa tự động |
| Short ambiguous VNEDICT substring | 2.563 | Đối chiếu word + POS + sense bằng nguồn có định nghĩa đầy đủ |
| Metadata/corrupted text | 217 | Tách metadata; chỉ giữ gloss có nguồn sau review |
| POS-style mismatch | 78 | Kiểm tra lại POS và sense, không suy ra từ headword |
| Usage label/broken boundary | 64 | Chuẩn hóa ranh giới gloss có kiểm chứng |
| Proper name/acronym capitalization | 46 | Xác minh casing và loại từ |
| Vietnamese không dấu/nghi ngờ ngôn ngữ | 18 | Kiểm chứng hoặc loại |
| Long multi-sense gloss | 17 | Tách sense, không gộp tự động |
| AI spot-check uncertainty | 4 | Người duyệt kiểm tra trực tiếp |

`AUTO_PREFLIGHT_ONLY=19.048` trong Repair v2 chỉ có nghĩa không bị rule gắn cờ; không phải semantic
approval. Vì human-reviewed = 0 và semanticVerified = 0, **số thẻ bổ sung hiện đủ điều kiện triển
khai an toàn là 0**. 5.000 thẻ production hiện tại không bị ảnh hưởng.

### 19.4 Kiểm thử và quyết định

- Repair v2 data integrity: **10/10 PASS**.
- Pipeline M13 + M15.5: **19/19 PASS**.
- Backend TypeScript typecheck và build: PASS.
- Không chạy lại Android/Admin/OAuth/M14/M15 vì không có thay đổi ở các module đó.
- Không rebuild staging bằng hai shortlist chưa duyệt; staging 20K trước đó chỉ là môi trường cô
  lập và không phải bằng chứng quality cho artifact mới.
- Không tạo backup deployment mới và không chạy checkpoint production, vì semantic preflight đã
  fail trước bước đó.

File project được thêm/chỉnh trong vòng Repair v2:

- `backend/datasets/quality/m15_5/repair_v2/*`
- `backend/scripts/cross_validate_vnedict.py`
- `backend/scripts/m15_5_vocabulary_pipeline.py`
- `backend/scripts/prepare-vocabulary-target.ts`
- `backend/scripts/publish-vocabulary-target.ts`
- `backend/scripts/sample-m15-5-final-quality.ts`
- `backend/tests/test_m15_5_vocabulary_pipeline.py`
- `M15_5_VOCABULARY_20000_REPORT.md`, `WORK_STATUS.md`, `BACKEND_CONTEXT.md`

**Kết luận:** production `flashcard_english` tiếp tục giữ 5.000 cards/19 topics cùng toàn bộ dữ
liệu người dùng. M15.5 vẫn `QUALITY GATE FAILED / PRODUCTION NOT READY`; không chuyển M16.

## 20. Production import theo chính sách IMPORTED_UNVERIFIED (2026-10-25)

Mục này thay thế quyết định chặn production ở mục 19 theo phê duyệt mới của chủ dự án. Quality
Gate ngữ nghĩa không còn là điều kiện chặn import nội bộ; không có thẻ mới nào được ghi nhận là
đã kiểm duyệt ngữ nghĩa.

### 20.1 Kế hoạch dữ liệu cố định

- Nguồn ưu tiên: `repair_v2_proposed_additions_15000_unverified.jsonl`, sau đó lấy candidate dự
  phòng từ artifact 22.000 khi gặp exact duplicate với 5.000 thẻ gốc hoặc trùng trong selection.
- Kế hoạch cuối: đúng **15.000** candidate, trong đó 12.487 từ proposal Repair v2 và 2.513 bản
  dự phòng kỹ thuật; 0 missing required, 0 duplicate exact English+Vietnamese, 0 hard block.
- SHA-256 kế hoạch: `26169eff61a487d6ae96359ea086d7655b53ec02d928af15aa9af3fd8df67132`.
- Mọi thẻ mới: `contentStatus=IMPORTED_UNVERIFIED`, provenance `NEEDS_REVIEW`, reasons có
  `IMPORTED_UNVERIFIED` + `SEMANTIC_REVIEW_PENDING`, `quality_verified=false`,
  `quiz_eligible=false`. Không sửa bộ publication guard M13/M16.

### 20.2 Backup, staging và production

- Backup cuối trước import: `C:\Users\phuc\AppData\Local\BiVoca\backups\flashcard_english_before_m15_5_20261025.dump`
  (5.824.974 bytes), SHA-256
  `1794b0c0ed631d0153671d50240c2906da45087d98b18318e8de920cab8ecf3b`.
- Restore-check vào `bivoca_m15_5_20261024`: 5.000 cards, 19 topics, 5 users, 14 progress,
  12 Quiz results, 2 scores và 5 XP rows.
- Staging đã chạy dry-run + apply các checkpoint 10K → 15K → 20K; chạy lại target 20K chèn
  **0** hàng. Protected hashes cards/users/progress/Quiz/scores/XP đều khớp baseline.
- Production `flashcard_english` chạy lại đúng ba checkpoint với transaction batch 100. Kết quả
  lần lượt 10.000, 15.000 và **20.000** cards. Lần chạy idempotency cuối chèn **0** hàng.

### 20.3 Post-check production

| Kiểm tra | Kết quả |
|---|---:|
| Flashcards / Topics | **20.000 / 20** |
| Imported unverified | **15.000** |
| Bị đánh dấu semantic verified sai | **0** |
| Provenance sai trạng thái approved | **0** |
| Imported row Quiz-eligible / lọt Quiz pool | **0 / 0** |
| Missing required / duplicate groups | **0 / 0** |
| GENERAL_VOCABULARY primary cards | 14.329 |
| Users / Progress / Quiz / Scores / XP | 5 / 14 / 12 / 2 / 5, không đổi |

Backend typecheck/build PASS và **338/338 tests PASS**. Backend production cổng 3000 đã chạy lại
bản build mới; health `ok/connected`, search trả `total=20000`, page 501 trả đúng 20 hàng và DTO
có `contentStatus`. Admin Web **43/43 tests PASS**, build PASS và cổng 5173 trả HTTP 200; lượt này
không đăng nhập lại tài khoản ADMIN nên không tuyên bố một browser-authenticated Admin regression mới.

Android `testDebugUnitTest`, `assembleDebug`, `lintDebug` BUILD SUCCESSFUL. APK Debug cài bằng
`adb install -r`, giữ session; `adb reverse tcp:3000 tcp:3000`. Emulator API 35 đọc production:
Topics hiển thị dữ liệu mới, Vocabulary Search `society` trả 5 kết quả từ API và thẻ mới hiển thị
nhãn **“Chưa kiểm duyệt ngữ nghĩa”**. Ảnh runtime:
`android-app/app/build/reports/m15_5_android_20000.png`.

### 20.4 Trạng thái bàn giao

- **DATA_IMPORT_DONE**: PostgreSQL production có 20.000 Flashcard; API và Android đọc được.
- **SEMANTIC_REVIEW_PENDING**: 15.000 thẻ mới chưa được chứng nhận nghĩa chính xác; tiếp tục làm
  sạch nghĩa Việt/Quiz Quality Guard trong M16.
- Không chạy import 5.000 cũ, không xóa/ghi đè thẻ cũ, không sửa users, Google identities,
  Progress, Quiz history, XP, streak, Daily Learning hoặc Room.

File chính thêm/sửa: `backend/scripts/prepare-m15-5-unverified-plan.ts`,
`backend/scripts/import-m15-5-unverified.ts`, `backend/scripts/verify-m15-5-production.ts`,
`backend/src/modules/flashcards/flashcard.repository.ts`, `flashcard.service.ts`,
`backend/src/shared/types/api.ts`, `backend/tests/api.test.ts`, `backend/package.json`,
`android-app/.../data/model/Flashcard.java`, `FlashcardActivity.java`,
`VocabularySearchAdapter.java`, `FlashcardContractTest.java` và các tài liệu trạng thái.
