# M15.7 — Smart Semantic Topic Classification

Ngày nghiệm thu: 2026-10-25  
Trạng thái: **DONE**

## 1. Kết quả

- Chỉ đọc và xử lý **12.858 Flashcard đang có primary topic `POS_*`** sau M15.6; không phân loại lại 20.000 thẻ từ đầu.
- Chuyển thêm **426 thẻ** sang 26 topic ngữ nghĩa, còn **12.432 thẻ** ở nhóm POS.
- Độ phủ bổ sung M15.7: **3,31%** của tập POS. Tỷ lệ thẻ có primary topic ngữ nghĩa toàn kho tăng từ **35,71% lên 37,84%**.
- Không hạ tiêu chí để tăng số lượng: 12.432 thẻ đa nghĩa/không đủ bằng chứng vẫn giữ POS.
- Tổng Flashcard vẫn **20.000**, topic vẫn 38 trong DB/36 nhóm có dữ liệu hiển thị, General vẫn 0.
- Không thay ID, English, Vietnamese, POS, CEFR, IPA, example, `contentStatus`, Quiz eligibility hoặc dữ liệu người dùng.

## 2. Phương pháp local

`backend/scripts/classify-m15-7-semantic-topics.ts` dùng pipeline deterministic:

1. Lấy đúng các thẻ có primary topic `POS_NOUN`, `POS_VERB`, `POS_ADJECTIVE`, `POS_ADVERB`, `POS_PREPOSITION`, `POS_CONJUNCTION`, `POS_PRONOUN` hoặc `POS_PHRASE`.
2. Đọc English, nghĩa Vietnamese và POS; metadata `sourceTopics` của nhóm này đã được kiểm tra nhưng không cung cấp tín hiệu hữu ích nên không được dùng để ép topic.
3. Học 950 lexical features từ 5.000 thẻ đã semantic-verified, nhưng mô hình học thống kê không có mapping nào vượt quality gate cuối. Không dùng tín hiệu yếu để tự gán.
4. Mapping thực tế dùng headword domain-specific, cụm nghĩa exact, morphology có giới hạn, false-positive exclusions và 22 audit override sau khi kiểm tra mẫu.
5. Không dùng substring thô, không bỏ dấu tiếng Việt, không gọi AI cho từng Flashcard.

Các lỗi được phát hiện và sửa trước production gồm:

- `butterfly` nghĩa “kiểu bơi bướm” phải vào Sports, không phải Animals.
- `marry` có chữ “nhà thờ” nhưng nghĩa là kết hôn, chuyển Family thay vì Buildings.
- `cable` có “đại dương” nhưng là truyền tin, chuyển Communication thay vì Nature.
- `sympathy` không được nhận nhầm hậu tố y khoa `-pathy`.
- `operation` không được nhận nhầm tiền tố `opera`.
- `investigate`/`insurrection` không được nhận nhầm `invest`/`insur` của Finance.
- “ngây thơ”, “lơ thơ”, “kịch liệt”, “mặc dù”, “mặc cả”, “rửa tội” có exclusion riêng để tránh Arts/Clothing/Law sai.

## 3. Mapping và audit artifact

- `backend/.m15-7-verification/semantic-mapping.jsonl`
- `backend/.m15-7-verification/semantic-mapping.csv`
- `backend/.m15-7-verification/semantic-sample.csv`
- `backend/.m15-7-verification/semantic-summary.json`

Mỗi mapping có ID, English, Vietnamese, POS, topic cũ/mới, reason, confidence và evidence. SHA-256 frozen plan:

`ff14a95e67b485828473c1e4c01c66acf27292b23c1d2c5e9cd86c983f57d08b`

Phân bố 426 thẻ chuyển thêm: Animals 5, Arts 70, Buildings 3, Clothing 36, Communication 2, Culture 13, Emotions 39, Family 4, Finance 30, Food 10, Health 49, Home 5, Language 9, Law & Government 45, Nature 7, Relationships 1, School 6, Science 6, Shopping 3, Sports 6, Technology 6, Time 5, Transportation 33, Travel 1, Weather 8, Work 24. Daily Life, Leisure và Personal không có mapping mới đủ chắc chắn.

## 4. Phân bố primary topic trước/sau

Số liệu dưới đây đếm primary topic; tổng mỗi cột bằng 20.000. API catalog có thể lớn hơn primary count ở một số topic vì giữ các quan hệ phụ hợp lệ đã tồn tại trước M15.7. M15.7 không tự tạo topic phụ mới.

| Nhóm | Topic | Trước | Sau | Chênh lệch |
|---|---|---:|---:|---:|
| Semantic | Animals | 121 | 126 | +5 |
| Semantic | Arts & Literature | 14 | 84 | +70 |
| Semantic | Buildings & Places | 11 | 14 | +3 |
| Semantic | Clothing & Fashion | 60 | 96 | +36 |
| Semantic | Communication & Media | 16 | 18 | +2 |
| Semantic | Culture | 195 | 208 | +13 |
| Semantic | Daily Life | 428 | 428 | 0 |
| Semantic | Emotions | 130 | 169 | +39 |
| Semantic | Family | 183 | 187 | +4 |
| Semantic | Finance & Economy | 124 | 154 | +30 |
| Semantic | Food | 475 | 485 | +10 |
| Semantic | Health | 389 | 438 | +49 |
| Semantic | House & Home | 676 | 681 | +5 |
| Semantic | Language & Grammar | 71 | 80 | +9 |
| Semantic | Law & Government | 54 | 99 | +45 |
| Semantic | Leisure | 455 | 455 | 0 |
| Semantic | Nature & Environment | 295 | 302 | +7 |
| Semantic | Personal Life | 323 | 323 | 0 |
| Semantic | Relationships | 85 | 86 | +1 |
| Semantic | School | 701 | 707 | +6 |
| Semantic | Science | 16 | 22 | +6 |
| Semantic | Shopping | 804 | 807 | +3 |
| Semantic | Sports & Fitness | 19 | 25 | +6 |
| Semantic | Technology | 131 | 137 | +6 |
| Semantic | Time & Calendar | 152 | 157 | +5 |
| Semantic | Transportation | 128 | 161 | +33 |
| Semantic | Travel | 398 | 399 | +1 |
| Semantic | Weather | 136 | 144 | +8 |
| Semantic | Work | 552 | 576 | +24 |
| Semantic/ẩn | General Vocabulary | 0 | 0 | 0 |
| POS | Noun | 6.000 | 5.729 | -271 |
| POS | Verb | 3.381 | 3.284 | -97 |
| POS | Adjective | 2.889 | 2.834 | -55 |
| POS | Adverb | 468 | 465 | -3 |
| POS | Preposition | 59 | 59 | 0 |
| POS | Conjunction | 31 | 31 | 0 |
| POS | Pronoun | 30 | 30 | 0 |
| POS/ẩn | Phrase | 0 | 0 | 0 |
| **Tổng** |  | **20.000** | **20.000** | **0** |

Không có topic tăng bất thường theo rule: mức tăng lớn nhất Arts +70, Health +49, Law +45, Emotions +39, Clothing +36 và Transportation +33; tất cả đã được kiểm tra mapping chi tiết.

## 5. Staging, transaction và idempotency

- Dump/restore production vào DB cô lập `bivoca_m15_7_20261025` PASS: 20.000 cards, 38 topics, 12.858 POS, users 5, progress 14, Quiz 12, scores 2, XP 5.
- Lần apply staging đầu tiên phát hiện unique constraint primary topic do thiếu dependency giữa hai CTE. Transaction rollback toàn bộ; production chưa bị chạm. SQL được sửa để insert primary mới phụ thuộc vào bước clear primary cũ.
- Apply lại staging chuyển 426 thẻ; verify PASS; apply lần hai chuyển 0, xác nhận idempotent.
- Staging API: 36 nhóm hiển thị, General ẩn, semantic đứng trước POS; Noun 5.729 thẻ/58 trang, trang cuối 29 thẻ. `projection` có trong Arts và `kneecap` có trong Health.
- Protected comparison staging/production baseline PASS cho nội dung thẻ ngoài topic, users, progress, Quiz, scores, XP và provenance.

## 6. Backup và production

Backup custom-format cuối ngay trước production được restore-check trên `bivoca_m15_7_restorecheck_20261025`:

- File: `C:\Users\phuc\AppData\Local\BiVoca\backups\flashcard_english_before_m15_7_20261025.dump`
- Kích thước: 8.331.292 byte
- SHA-256: `589262097a02a5e64ef17169e7a17cc63b4d8778129d151880c0d365bf427dcd`

Production apply theo batch transaction 500 thẻ PASS. Verify sau apply:

- Flashcards 20.000, topics 38, POS 12.432.
- Invalid/missing/mismatched/duplicate primary: 0.
- Imported unverified: 15.000; unverified Quiz-eligible: 0.
- Users 5, progress 14, Quiz 12, scores 2, XP history 5.
- Apply lần hai chuyển 0.
- Protected checksum so với restore-check trước triển khai khớp cho nội dung Flashcard, users, progress, Quiz, scores, XP và provenance.

## 7. Backend, API, Admin và Android

- Backend typecheck/build PASS; **338/338 tests PASS**.
- Production cổng 3000: health `ok/database connected`; catalog 36 nhóm, General ẩn, semantic trước POS.
- API Noun: total 5.729, 58 trang x tối đa 100, trang cuối 29; không tải toàn danh sách vào RAM.
- Admin Web route `/admin/topics` trả HTTP 200; không có mutation Admin trong lượt này.
- Không cần sửa Android source: UI M15.6 đã nhóm semantic trước POS và dùng paging server.
- Android `testDebugUnitTest`, `assembleDebug`, `lintDebug` PASS: **76/76 test**, lint 0 lỗi/289 cảnh báo.
- APK Debug cài bằng `adb install -r`, giữ app data; `adb reverse tcp:3000 tcp:3000` PASS. Sau cài, app đang ở Login nên chưa có bằng chứng UI authenticated mới cho count 5.729; không giả báo runtime Topics PASS. Dữ liệu production và contract Android được xác nhận qua API thật và regression test.

## 8. File thêm/chỉnh sửa

- `backend/scripts/classify-m15-7-semantic-topics.ts`
- `backend/scripts/prepare-m15-7-isolated.ts`
- `backend/scripts/backup-m15-7-production.ts`
- `backend/scripts/verify-m15-7-topics.ts`
- `backend/scripts/compare-m15-7-protected.ts`
- `backend/package.json`
- `M15_7_SEMANTIC_TOPIC_ENRICHMENT_REPORT.md`
- `WORK_STATUS.md`, `BACKEND_CONTEXT.md`, `ANDROID_CONTEXT.md`

Không sửa Android source, Backend API source, migration, schema hay Admin source.

## 9. Giới hạn

- 426 là số mapping đạt tiêu chí hiện tại, không phải mục tiêu số lượng cưỡng ép. Dữ liệu từ điển chứa nhiều headword đa nghĩa như `interest`, `patient`, `uniform`, `operation`; nếu chỉ dùng keyword sẽ tạo sai hàng loạt.
- 12.432 thẻ còn ở POS là trạng thái an toàn và có chủ ý. Muốn tăng mạnh hơn cần một nguồn lexical taxonomy/sense-domain đã được cấp phép hoặc duyệt theo lô; không nên hạ threshold của rule hiện tại.
- Không nhập dataset, không chạy lại M15.5, không thay semantic verification của 15.000 nghĩa và không triển khai M16.
