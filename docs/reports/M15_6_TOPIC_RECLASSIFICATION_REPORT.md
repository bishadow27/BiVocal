# M15.6 — Phân loại lại General Vocabulary

Ngày nghiệm thu: 2026-10-25  
Trạng thái: **DONE**

## 1. Kết quả chính

- PostgreSQL production `flashcard_english` vẫn có đúng **20.000 Flashcard**.
- `GENERAL_VOCABULARY`: **14.329 → 0** thẻ chính; không còn liên kết phụ tới General.
- Tạo thêm **18 topic**: 10 topic ngữ nghĩa và 8 nhóm dự phòng theo từ loại. Database tăng từ 20 lên 38 topic; API/Android hiển thị 36 topic có dữ liệu và ẩn `GENERAL_VOCABULARY` cùng `POS_PHRASE` đang rỗng.
- Trong 14.329 thẻ General: **1.471** thẻ được chuyển sang topic ngữ nghĩa và **12.858** thẻ vào nhóm từ loại. Không còn thẻ chưa phân nhóm.
- Không INSERT Flashcard, không đổi ID/nội dung/CEFR/IPA/content status. 15.000 thẻ M15.5 vẫn là `IMPORTED_UNVERIFIED`; không thẻ nào trong nhóm này được tự bật Quiz.

## 2. Cơ chế phân loại

Script `backend/scripts/classify-m15-6-topics.ts` đọc English, Vietnamese, POS và metadata `sourceTopics`. Phân loại ngữ nghĩa chỉ dùng rule exact token/phrase; từ đa nghĩa được xét trên nghĩa Việt cụ thể. Các từ không đủ bằng chứng được đưa về nhóm POS thay vì ép vào topic ngữ nghĩa.

Một lỗi dry-run đã được phát hiện trước production: bỏ dấu tiếng Việt và so khớp chuỗi con làm các từ như `cá`, `bán`, `phòng`, `câu` khớp sai. Pipeline được sửa để giữ dấu, dùng ranh giới token/phrase và bỏ các rule một từ mơ hồ. Không có dữ liệu production nào bị áp dụng bằng bản rule lỗi.

Mỗi mapping ghi `flashcardId`, English, Vietnamese, POS, topic cũ/mới, lý do và confidence tại:

- `backend/.m15-6-verification/topic-mapping.jsonl`
- `backend/.m15-6-verification/topic-mapping.csv`
- `backend/.m15-6-verification/topic-mapping-summary.json`

SHA-256 mapping đã đóng băng: `6e7105c84a0a04aabc85d07c9f126638326a055e332db7b2ce0f15b28367840f`.

## 3. Phân bố production sau triển khai

Số dưới đây là topic chính của mỗi Flashcard, vì vậy tổng bằng đúng 20.000.

| Nhóm | Topic | Số thẻ |
|---|---|---:|
| Ngữ nghĩa | Animals | 121 |
| Ngữ nghĩa | Arts & Literature | 14 |
| Ngữ nghĩa | Buildings & Places | 11 |
| Ngữ nghĩa | Clothing & Fashion | 60 |
| Ngữ nghĩa | Communication & Media | 16 |
| Ngữ nghĩa | Culture | 195 |
| Ngữ nghĩa | Daily Life | 428 |
| Ngữ nghĩa | Emotions | 130 |
| Ngữ nghĩa | Family | 183 |
| Ngữ nghĩa | Finance & Economy | 124 |
| Ngữ nghĩa | Food | 475 |
| Ngữ nghĩa | Health | 389 |
| Ngữ nghĩa | House & Home | 676 |
| Ngữ nghĩa | Language & Grammar | 71 |
| Ngữ nghĩa | Law & Government | 54 |
| Ngữ nghĩa | Leisure | 455 |
| Ngữ nghĩa | Nature & Environment | 295 |
| Ngữ nghĩa | Personal Life | 323 |
| Ngữ nghĩa | Relationships | 85 |
| Ngữ nghĩa | School | 701 |
| Ngữ nghĩa | Science | 16 |
| Ngữ nghĩa | Shopping | 804 |
| Ngữ nghĩa | Sports & Fitness | 19 |
| Ngữ nghĩa | Technology | 131 |
| Ngữ nghĩa | Time & Calendar | 152 |
| Ngữ nghĩa | Transportation | 128 |
| Ngữ nghĩa | Travel | 398 |
| Ngữ nghĩa | Weather | 136 |
| Ngữ nghĩa | Work | 552 |
| Từ loại | Noun | 6.000 |
| Từ loại | Verb | 3.381 |
| Từ loại | Adjective | 2.889 |
| Từ loại | Adverb | 468 |
| Từ loại | Preposition | 59 |
| Từ loại | Conjunction | 31 |
| Từ loại | Pronoun | 30 |
| Từ loại | Phrase | 0 |
| Ẩn | General Vocabulary | 0 |
| **Tổng** |  | **20.000** |

## 4. Staging, backup và triển khai production

1. `prepare-m15-6-isolated.ts` tạo backup custom-format và restore vào DB cô lập `bivoca_m15_6_20261025`.
2. Dry-run, apply, verify và apply lần hai trên DB cô lập đều PASS; lần hai chuyển 0 thẻ, xác nhận idempotent.
3. So sánh protected checksums giữa staging và baseline PASS cho nội dung Flashcard ngoài topic, users, progress, Quiz, scores, XP và provenance.
4. Backup cuối ngay trước production được restore-check thành công. Bản sao giữ tại `C:\Users\phuc\AppData\Local\BiVoca\backups\flashcard_english_before_m15_6_20261025.dump`, 8.319.228 byte, SHA-256 `cbce90d6c3647a230fa2a514110aa6247f6375a05381dbb0d56c40ead8de3411`.
5. Apply production theo batch transaction 500 thẻ, verify PASS; chạy apply lần hai chuyển 0 thẻ.

Snapshot bảo vệ trước/sau: users 5, progress 14, Quiz results 12, scores 2, XP history 5. Checksum tất cả nhóm dữ liệu này, nội dung 20.000 Flashcard ngoài topic và provenance đều khớp baseline.

## 5. Backend và Admin

- Catalog trả `topicGroup`, ẩn General và topic rỗng, sắp “Học theo chủ đề” trước “Học theo loại từ”.
- API phân trang production: Noun có 6.000 thẻ, 60 trang x 100; trang 60 trả đủ 100 thẻ.
- Search tiếng Anh `abacus` và tiếng Việt `Bàn tính` đều trả đúng kết quả.
- Backend `typecheck`, build và **338/338 test PASS**.
- Backend production cổng 3000 đã chạy bản build mới; health `ok/database connected`.
- Admin Web **43/43 test PASS**, build PASS; `http://localhost:5173/admin/topics` trả HTTP 200. Không dùng tài khoản người dùng để tạo mutation khi nghiệm thu.

## 6. Android

- Topics hiển thị hai nhóm rõ ràng, số thẻ thật, tìm topic và mở Flashcard theo topic.
- Luồng Flashcard topic lớn dùng endpoint phân trang server, 100 thẻ/trang; không tải 6.000 hoặc 20.000 thẻ vào RAM. Previous/Next qua biên trang được unit test; Review/Favorite vẫn thao tác trên thẻ hiện tại.
- `testDebugUnitTest`: **76/76 PASS**; `assembleDebug` PASS; `lintDebug`: 0 lỗi, 289 cảnh báo.
- APK Debug được cài bằng `adb install -r`, giữ dữ liệu. Emulator `emulator-5554` với `adb reverse tcp:3000 tcp:3000` xác nhận:
  - nhóm “HỌC THEO CHỦ ĐỀ” đứng trước “HỌC THEO LOẠI TỪ”;
  - General không xuất hiện;
  - Noun hiển thị 6.000 thẻ và màn học ghi `1 / 6000`;
  - lật `abacus` thấy `Bàn tính.`, Next sang `2 / 6000`, Previous về `1 / 6000`;
  - tìm `abacus` trả 1 kết quả với nhãn “Chưa kiểm duyệt ngữ nghĩa”.

Ảnh nghiệm thu:

- `android-app/app/build/reports/m15_6_topics_final.png`
- `android-app/app/build/reports/m15_6_noun_page.png`
- `android-app/app/build/reports/m15_6_search_abacus.png`

## 7. File thêm/chỉnh sửa

Backend: bốn script M15.6 trong `backend/scripts/`, `backend/package.json`, topic repository/service, shared API type và test API. Android: `Topic`, `TopicAdapter`, `TopicsActivity`, `item_topic.xml`, `FlashcardPage`, `FlashcardRepository`, `FlashcardViewModel`, `FlashcardActivity` và `FlashcardViewModelTest`.

## 8. Giới hạn đúng phạm vi

- Đây là phân loại chủ đề, không phải chứng nhận nghĩa. 15.000 thẻ mới vẫn cần semantic review ở milestone sau.
- `POS_PHRASE` được tạo làm fallback an toàn nhưng hiện không có thẻ nên bị catalog ẩn.
- Không chạy lại import M15.5, không đưa Room trở lại, không thay đổi XP/SRS/Daily Learning/Quiz history/Google identities và không triển khai M16.
