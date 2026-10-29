# M13 — CEFR Dataset Integration (Phase 1)

**Ngày nghiệm thu:** 2026-10-21  
**Trạng thái:** **DONE — Phase 1 trên database thử nghiệm riêng**  
**Database vận hành:** `flashcard_english` chỉ đọc, snapshot trước/sau không đổi  
**Database thử nghiệm giữ làm bằng chứng:** `bivoca_m13_20261021_d4e222639863`

## 1. Phạm vi đã thực hiện

Phase 1 kiểm tra `backend/datasets/cefr/word_list_cefr.csv`, xây dựng bộ phân tích/mapping,
đối chiếu 300 Flashcard thật trong PostgreSQL, ghi kết quả vào bảng audit trên database M13 riêng
và chạy lại import để xác minh idempotence. Không thêm hàng nghìn từ mới, không sửa Android,
Quiz, XP, streak, lịch ôn hoặc Room. Không chạy migration hay ghi dữ liệu trên database vận hành.

## 2. Nguồn và giấy phép

- Nguồn: [Maximax67/Words-CEFR-Dataset](https://github.com/Maximax67/Words-CEFR-Dataset).
- File: `datasets/word_list_cefr.csv`, SHA-256
  `c5e381905a87b47bd1321ac1cd34f870b107c0baa7b9f35f66e5b3a9b8ad8123`.
- Repository nguồn công bố giấy phép MIT, Copyright (c) 2024 Belikov Maxim. Bản quyền và toàn
  văn giấy phép được giữ tại `backend/datasets/cefr/LICENSE`; attribution nằm trong
  `backend/datasets/cefr/ATTRIBUTION.md`.
- README nguồn mô tả nhãn được tính từ CEFR-J, lemma, stem, từ loại và tần suất. BiVoca coi đây
  là **nhãn suy luận của dataset**, không trình bày như chứng nhận CEFR chính thức.
- Nguồn còn ghi nhận CEFR-J, Google Books Ngram, spaCy, LemmInflect, Penn Treebank và các word
  list. Trước khi phân phối dataset dẫn xuất ra ngoài bài tập, cần rà điều khoản từng nguồn
  thượng nguồn; MIT của repository không tự giải quyết mọi ràng buộc dữ liệu nguồn.

## 3. Cấu trúc và kết quả làm sạch CSV

CSV UTF-8 dùng dấu chấm phẩy, có đúng sáu cột:

| Cột | Ý nghĩa sử dụng trong M13 |
|---|---|
| `headword` | Từ tiếng Anh dùng để đối chiếu |
| `pos` | Từ loại dùng trong khóa ghép |
| `CEFR` | Cấp độ nguồn |
| `CoreInventory 1` | Nhãn/chủ điểm nguồn, chỉ lưu metadata audit |
| `CoreInventory 2` | Nhãn/chủ điểm nguồn, chỉ lưu metadata audit |
| `Threshold` | Nhãn/chủ điểm nguồn, chỉ lưu metadata audit |

Ba cột cuối không phải mã chủ đề BiVoca và không thay thế mười `topics.code` hiện có.

| Kiểm tra | Kết quả |
|---|---:|
| Dòng vật lý, gồm header | 7.989 |
| Bản ghi | 7.988 |
| Bản ghi thiếu headword/POS hoặc CEFR sai | 0 |
| Headword có nhiều dòng | 865 |
| Dòng dư do headword lặp | 953 |
| Khóa `(headword, pos)` bị lặp | 7 |
| Dòng dư theo khóa `(headword, pos)` | 7 |
| Khóa lặp có CEFR xung đột | 1 (`march`, noun: A1/B1) |

Sáu khóa lặp còn lại có cùng CEFR: `a.m.`/adverb, `am`/adverb, `e-mail`/noun,
`internet`/noun, `p.m.`/adverb và `pm`/adverb. Pipeline gộp các dòng cùng khóa/cùng cấp độ;
khóa có nhiều cấp độ luôn thành `uncertain`.

| A1 | A2 | B1 | B2 | C1 | C2 |
|---:|---:|---:|---:|---:|---:|
| 1.200 | 1.443 | 2.486 | 2.859 | 0 | 0 |

README nguồn nói hệ thống có thể biểu diễn A1–C2, nhưng file được cung cấp chỉ có A1–B2.
Schema BiVoca hiện cũng giới hạn `level` ở A1–B2 nên Phase 1 không cần nới constraint.

Phân bố 15 từ loại nguồn: noun 4.193; adjective 1.515; verb 1.396; adverb 567;
pronoun 83; preposition 78; determiner 46; conjunction 37; number 30;
modal auxiliary 13; be-verb 11; interjection 9; do-verb 5; have-verb 4; infinitive-to 1.

Các cờ chủ điểm có dữ liệu ở 1.751 dòng `CoreInventory 1`, 109 dòng `CoreInventory 2`
và 1.790 dòng `Threshold`.

## 4. Schema BiVoca và chiến lược mapping

`flashcards` có `id`, `topic_id`, `english`, `vietnamese`, `pronunciation`,
`part_of_speech`, `example`, `example_vi`, `level`, `is_active` và timestamps. Khóa duy nhất
hiện tại là `LOWER(english), LOWER(vietnamese)`, cho phép một từ có nhiều nghĩa.

Pipeline chuẩn hóa headword/POS bằng Unicode NFKC, trim, gom khoảng trắng và lowercase:

1. `exact`: đúng cả từ tiếng Anh và từ loại, nguồn chỉ cho một CEFR, và BiVoca chỉ có một
   Flashcard cho cặp từ + từ loại đó.
2. `uncertain`: chỉ trùng từ nhưng lệch từ loại; nguồn có nhiều CEFR cho cùng khóa; hoặc BiVoca
   có nhiều nghĩa cùng từ + từ loại mà nguồn không phân biệt sense.
3. `unmatched`: không có headword trong nguồn. Không gán A1 và không thay đổi `level`.

Không ép `be-verb`, `do-verb`, `have-verb` thành `verb`, hoặc `number` thành `noun`; ánh xạ mất
thông tin phải được duyệt thủ công và nếu dùng sẽ thuộc nhóm `uncertain`.

Khi chạy `--apply`, script chỉ điền CEFR cho hàng `exact` có `flashcards.level IS NULL`.
Nhãn đang có không bị ghi đè. Nghĩa tiếng Việt, IPA, ví dụ, topic, trạng thái, ID và các trường
khác không nằm trong câu lệnh cập nhật.

## 5. Kết quả đối chiếu 300 Flashcard

| Trạng thái | Số lượng | Tỷ lệ |
|---|---:|---:|
| Khớp chính xác | 293 | 97,67% |
| Khớp chưa chắc chắn | 0 | 0% |
| Không khớp | 7 | 2,33% |

Bảy từ không khớp: `nest`, `sibling`, `app`, `wireless`, `allergy`, `try on`, `hurricane`.
Chúng giữ nguyên nhãn hiện tại; không có fallback A1.

| CEFR nguồn của 293 exact | A1 | A2 | B1 | B2 |
|---|---:|---:|---:|---:|
| Số thẻ | 123 | 66 | 76 | 28 |

Phân bố nhãn BiVoca hiện tại là A1 102, A2 84, B1 81, B2 33. Có 99/293 kết quả exact
có nhãn nguồn khác nhãn hiện tại. Đây là xung đột **nhãn**, không phải ánh xạ từ/POS mơ hồ.
Phase 1 ghi nhận chúng trong audit và không tự ghi đè.

## 6. Import thử nghiệm và tính an toàn

Harness tạo mới database `bivoca_m13_20261021_d4e222639863`, xác nhận chưa tồn tại,
nạp đúng schema + 10 topics + 300 cards, rồi áp dụng 6 migration backend. Import tạo bảng
`cefr_flashcard_matches` chỉ trong DB này để lưu nguồn, SHA-256, giấy phép, kết quả, lý do,
CEFR/POS ứng viên, topic nguồn và nhãn trước import.

| Kiểm tra | Lần 1 | Lần 2 |
|---|---:|---:|
| Audit rows | 300 | 300 |
| Level updates | 0 | 0 |
| Exact | 293 | 293 |
| Unmatched | 7 | 7 |

`imported_at` không đổi ở lần hai vì upsert chỉ cập nhật khi nội dung thay đổi. Hash toàn bộ 300
Flashcard trước/sau import giống nhau. Không tạo Flashcard, xóa dữ liệu, đổi ID hay tác động
quan hệ Quiz/progress/review.

Script `--apply` chỉ chấp nhận host local và tên DB khớp `bivoca_m13_*`. Chạy thử với cấu hình
`flashcard_english` bị từ chối trước khi kết nối ghi với exit code 1. Snapshot vận hành trước/sau
giữ nguyên 10 topics, 300 flashcards, 3 users và cùng dataset hash.

## 7. Lệnh kiểm thử và kết quả

| Lệnh | Kết quả |
|---|---|
| `npm run cefr:audit` | PASS, đọc `flashcard_english`, 300 thẻ |
| `npm run typecheck` | PASS |
| `npm run build` | PASS |
| `npm test` | PASS 317/317 |
| `node --import tsx --test tests/cefr-mapping.test.ts` | PASS 4/4 |
| `python test_dataset.py` trong `dataset/` | PASS 10/10 |
| `node .m13-verification/isolated.mjs` | PASS, import 2 lần trên DB riêng |
| `npm run cefr:import:test` với DB vận hành | Từ chối đúng, exit 1, không ghi DB |

Một lần gọi `python -m unittest dataset.test_dataset` từ thư mục gốc thất bại do bộ test cũ
dùng import theo thư mục làm việc. Chạy đúng entry point trong `dataset/` đã PASS 10/10;
không sửa bộ test cũ vì không có lỗi chức năng.

## 8. File thêm/chỉnh sửa

- `backend/scripts/cefr/cefr-mapping.ts`: parser, chuẩn hóa, phân loại và thống kê.
- `backend/scripts/import-cefr.ts`: dry-run và import có guard DB M13, audit idempotent.
- `backend/tests/cefr-mapping.test.ts`: unit tests cho CSV, normalize, POS, đa nghĩa/xung đột.
- `backend/.m13-verification/isolated.mjs`: tạo DB riêng, seed, import hai lần, kiểm hash nguồn.
- `backend/.m13-verification/state.json`: bằng chứng máy đọc được, không chứa secret.
- `backend/datasets/cefr/ATTRIBUTION.md` và `LICENSE`: nguồn và giấy phép.
- `backend/package.json`: lệnh `cefr:audit` và `cefr:import:test`.
- `M13_CEFR_DATASET_INTEGRATION_REPORT.md`, `WORK_STATUS.md`, `BACKEND_CONTEXT.md`.

## 9. Rủi ro và kế hoạch giai đoạn sau

- 99 xung đột CEFR cần duyệt theo nghĩa/ngữ cảnh trước khi thay nhãn BiVoca.
- Bảy từ chưa khớp cần nguồn thứ hai hoặc duyệt thủ công; không suy ra A1.
- Nếu dataset tương lai có C1/C2, cần migration mở rộng constraint và Admin validation; chưa tạo
  migration vì file hiện tại không cần và database vận hành chưa được phép thay đổi.
- Khi được phê duyệt production, nên tạo migration riêng cho bảng provenance/audit, sao lưu DB,
  chạy dry-run, duyệt xung đột, rồi chỉ điền các `level IS NULL` exact.
- Thêm hàng nghìn Flashcard mới phải là phase riêng: cần nghĩa tiếng Việt, IPA, ví dụ, phân loại
  topic, kiểm tra bản quyền và review nội dung. Phase 1 không thực hiện phần này.
