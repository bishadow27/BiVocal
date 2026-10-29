# Thêm dữ liệu bổ sung

Seed ban đầu ở `../flashcards.csv` giữ 300 từ. Các CSV trong thư mục này là
những lô thêm/cập nhật độc lập, không được ghép vào seed chỉ để import.

1. Tạo file UTF-8 như `travel_001.csv` hoặc `technology_001.csv` với header:

   ```csv
   topic_code,english,vietnamese,pronunciation,part_of_speech,example,example_vi,level
   ```

2. Điền từ thật, đủ 8 trường. Dùng mã như `TRAVEL`, không nhập `topic_id`.
   IPA ưu tiên British English; CEFR chỉ A1/A2/B1/B2.
   Loại từ: noun/verb/adjective/adverb/preposition/conjunction/pronoun;
   dùng phrase khi flashcard là cụm từ. Quote trường có dấu phẩy hoặc dấu nháy.
3. Validate từ thư mục project:

   ```powershell
   python dataset/validate_dataset.py --flashcards dataset/additions/travel_001.csv
   ```

   Đọc cả warning câu ví dụ. Không dùng `--seed` cho lô bổ sung.
   Topic mới cần có trong catalog CSV truyền qua `--topics` khi validate offline.
4. Import sau khi cấu hình `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`:

   ```powershell
   python dataset/import_flashcards.py dataset/additions/travel_001.csv --apply
   ```

   Hoặc xuất SQL để chạy trong pgAdmin:

   ```powershell
   python dataset/import_flashcards.py dataset/additions/travel_001.csv --sql-output dataset/additions/travel_001.sql
   ```

5. Xem inserted/updated/skipped/failed rồi kiểm tra PostgreSQL theo README chính.
   Nếu có failed, các dòng hợp lệ khác có thể đã được commit; sửa lỗi và chạy lại
   cùng lô. Khóa từ + nghĩa không đổi sẽ được update hoặc skipped, không tạo trùng.

`example_addition.csv` là ví dụ chạy được sau seed:

- Thêm `watermelon → dưa hấu` vào FOOD.
- Cập nhật câu ví dụ của `book → quyển sách` ở SCHOOL.
- Thêm nghĩa động từ `book → đặt chỗ` vào TRAVEL.

Kỳ vọng lần đầu: inserted=2, updated=1, skipped=0, failed=0.
Chạy lại không sửa CSV: inserted=0, updated=0, skipped=3, failed=0.
Database có 302 flashcard sau ví dụ, nhưng `flashcards.csv` vẫn có 300.

Để thêm topic mới, upsert vào `topics` theo code trước; xem ví dụ SPORT trong
`../README.md`. Không cần chỉnh ID hay các chủ đề cũ. Không truyền `--topics` vào
importer khi muốn dùng catalog hiện hành trong PostgreSQL làm nguồn kiểm tra topic.
