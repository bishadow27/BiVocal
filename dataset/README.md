# Dataset Flashcard tiếng Anh

Seed gồm **10 chủ đề và 300 flashcard**, đúng **30 flashcard/chủ đề**.
Đây là 300 từ đầu tiên để bootstrap ứng dụng Android học tiếng Anh.
Sau import, **PostgreSQL là nguồn dữ liệu chính của hệ thống**. CSV là nguồn seed
và các lô bổ sung; CSV không phải database production và không lưu tiến độ người dùng.

## Các tệp

```text
dataset/
├── schema.sql                 # Schema PostgreSQL và unique index
├── topics.csv                 # 10 chủ đề seed
├── flashcards.csv             # 300 flashcard seed
├── seed_topics.sql            # Upsert chủ đề theo code
├── seed_flashcards.sql        # Ánh xạ topic_code rồi upsert flashcard
├── generate_seed_sql.py       # Tạo lại SQL từ CSV để hai nguồn khớp nhau
├── import_flashcards.py       # Import trực tiếp hoặc xuất SQL cho pgAdmin
├── validate_dataset.py        # Validator seed và mở rộng
├── test_dataset.py            # Kiểm thử validator và PostgreSQL
├── validation_report.txt      # Kết quả chạy kiểm tra thực tế
└── additions/
    ├── README.md
    └── example_addition.csv   # Ví dụ riêng; không cộng vào 300 từ seed
```

Python **3.10+**, chỉ dùng thư viện chuẩn. Không cần cài psycopg hay dependency Python.
Import trực tiếp cần PostgreSQL client `psql`. SQL dùng PostgreSQL **14+ với ICU**;
đã kiểm thử trên PostgreSQL **18.6**. Bản Windows tiêu chuẩn trên máy này có ICU.
Nếu PostgreSQL được tự biên dịch không có ICU, cần dùng bản hỗ trợ ICU trước khi chạy schema.

## Chạy seed qua pgAdmin 4

1. Tạo/chọn database có encoding **UTF8**.
2. Mở Query Tool của đúng database/schema.
3. Mở và chạy toàn bộ `schema.sql`.
4. Mở và chạy toàn bộ `seed_topics.sql`.
5. Mở và chạy toàn bộ `seed_flashcards.sql`.
6. Đọc JSON thống kê trong Data Output và chạy các truy vấn kiểm tra bên dưới.

Trên database mới: `inserted=300, updated=0, skipped=0, failed=0`.
Chạy lại seed flashcard không đổi: `inserted=0, updated=0, skipped=300, failed=0`.
Chạy lại seed topic cập nhật metadata theo `code`, không tạo thêm chủ đề.
Không chạy gộp các tệp bên trong một transaction đang mở: mỗi tệp tự có `BEGIN/COMMIT`.

`schema.sql` dành cho khởi tạo schema mới. `IF NOT EXISTS` cho phép chạy lại,
nhưng **không tự sửa cấu trúc bảng đã có từ phiên bản khác**. Khi dùng bảng cũ,
cần migration riêng để đồng bộ kiểu dữ liệu, collation, constraints và index;
không xóa bảng chứa dữ liệu runtime để chạy lại seed.

## Thiết kế database

CSV không chứa `id`, `topic_id`, `created_at` hoặc `updated_at`.
Database tự cấp `SERIAL`, timestamps và `is_active=TRUE` khi thêm mới.

```text
CSV.topic_code → topics.code → topics.id → flashcards.topic_id
```

Không sửa ID bằng tay và không giả định Travel có ID 3. Kiểm thử dùng ID chủ đề
bắt đầu từ 101 để xác nhận việc ánh xạ độc lập với ID.

Unique index đúng theo cặp từ + nghĩa:

```sql
CREATE UNIQUE INDEX IF NOT EXISTS idx_flashcards_unique_word_meaning
ON flashcards (LOWER(english), LOWER(vietnamese));
```

`book → quyển sách` và `book → đặt chỗ` được phép cùng tồn tại.
`apple → quả táo` không được thêm lần hai kể cả thay đổi chữ hoa/thường.
Schema đặt collation ICU `dataset_unicode` trên hai cột khóa để `LOWER()` xử lý
đúng cả tiếng Việt ngay cả khi locale mặc định của database là `C`.
Đã kiểm thử `BOOK / QUYỂN SÁCH` khớp bản ghi `book / quyển sách`.
Collation ảnh hưởng đến `LOWER()` theo [tài liệu PostgreSQL](https://www.postgresql.org/docs/18/collation.html).

Khóa ngoại có `ON DELETE CASCADE`: xóa chủ đề sẽ xóa các flashcard của chủ đề đó.
Có index trên `flashcards.topic_id` để hỗ trợ truy vấn theo chủ đề.
Importer giữ `id`, `created_at` và `is_active` của bản ghi cũ, chỉ đổi metadata
và `updated_at` khi dữ liệu thay đổi. Admin Web sau này có thể dùng cùng schema,
nhưng phải cập nhật `updated_at` khi sửa dữ liệu qua API; schema không có trigger thời gian.

## Validate

Từ thư mục project:

```powershell
python dataset/validate_dataset.py --seed
python dataset/validate_dataset.py
python dataset/validate_dataset.py --flashcards dataset/additions/example_addition.csv
```

Chế độ `--seed` kiểm tra 10 mã chủ đề yêu cầu, 300 flashcard và 30 từ/chủ đề.
Chế độ mặc định không giới hạn 300 từ; dataset 500/1000/5000 từ dùng cùng validator.
Phải có ít nhất một flashcard. Chỉ định nguồn topic khác khi cần:

```powershell
python dataset/validate_dataset.py --topics dataset/topics.csv --flashcards dataset/additions/travel_001.csv
```

Validator kiểm tra UTF-8 (chấp nhận BOM), cấu trúc/header CSV chính xác, số cột,
trường bắt buộc, giới hạn độ dài của schema, mã topic, CEFR, loại từ,
format IPA, placeholder và duplicate theo `(english.strip().lower(), vietnamese.strip().lower())`.
Chuỗi được chuẩn hóa Unicode NFC trước khi so sánh/import.
Khoảng trắng thừa hoặc Unicode chưa NFC có warning và được chuẩn hóa khi import.
Mỗi lỗi có `file`, `row`, `english`, `error`; row là dòng vật lý trong CSV.
CSV chứa dấu phẩy, dấu nháy hoặc xuống dòng phải được quote theo chuẩn CSV.

Câu ví dụ thiếu headword nguyên dạng hoặc ngoài khoảng 5–15 từ tạo warning;
không tự fail khi có biến đổi ngữ pháp. Bốn warning seed đã được đọc lại:

| Headword | Dạng trong ví dụ | Kết quả rà soát |
|---|---|---|
| generation | generations | Số nhiều đúng ngữ cảnh |
| ancestor | ancestors | Số nhiều đúng ngữ cảnh |
| earn | earns | Chia động từ đúng chủ ngữ |
| employ | employs | Chia động từ đúng chủ ngữ |

PASS của validator là đạt quy tắc cấu trúc và tính toàn vẹn tự động.
Validator không chứng minh ngữ pháp, bản dịch, phiên âm từ điển hay CEFR chính thức.
IPA sử dụng dạng British English với ký hiệu trọng âm và trường âm; dấu tách âm
tiết được lược bỏ. Có đối chiếu những biến thể dễ nhầm, ví dụ
[medicine](https://dictionary.cambridge.org/pronunciation/english/medicine) và
[upbringing](https://dictionary.cambridge.org/us/dictionary/chinese-traditional-english/%E6%95%99%E9%A4%8A)
trên Cambridge. Không khẳng định đã tra trực tuyến từng IPA của cả 300 từ.
Các câu ví dụ và bản dịch được biên soạn cho dataset này.
CEFR là nhãn biên soạn theo nghĩa và độ hữu ích cho người học, không phải bộ
dữ liệu CEFR được cấp chứng nhận; có thể tinh chỉnh khi giảng viên duyệt nội dung.

Phân bố seed:

| CEFR | Số flashcard | Tỷ lệ |
|---|---:|---:|
| A1 | 102 | 34% |
| A2 | 84 | 28% |
| B1 | 81 | 27% |
| B2 | 33 | 11% |

## Thêm flashcard: CSV → Validate → Import / Upsert → PostgreSQL

Xem `additions/README.md` để lấy header và quy trình chi tiết.
Ví dụ `example_addition.csv` gồm hai từ/nghĩa mới và một bản cập nhật:
`watermelon`, `book → đặt chỗ`, và cập nhật ví dụ `book → quyển sách`.
Chạy sau seed lần đầu sẽ có **inserted=2, updated=1, skipped=0, failed=0**.
Chạy lại sẽ có **skipped=3**, database có **302 flashcard**.

### Xuất SQL để chạy trong pgAdmin (không cần kết nối Python)

```powershell
python dataset/import_flashcards.py dataset/additions/example_addition.csv --topics dataset/topics.csv --sql-output dataset/additions/example_addition.sql
```

Sau đó mở tệp SQL vừa tạo trong Query Tool và Execute. Chế độ này chỉ báo
`SQL GENERATED`; số inserted/updated/skipped/failed **NOT VERIFIED** cho đến khi
SQL được thực thi. Thống kê thực nằm trong JSON ở Data Output.

### Import trực tiếp

Đặt các biến môi trường kết nối trong terminal hiện tại. Thay các giá trị bằng
thông tin database của bạn; đoạn dưới là mẫu cấu hình, source code không lưu chúng:

```powershell
$env:DB_HOST = 'localhost'
$env:DB_PORT = '5432'
$env:DB_NAME = 'english_flashcards'
$env:DB_USER = 'postgres'
$credential = Get-Credential -UserName $env:DB_USER -Message 'Nhập mật khẩu PostgreSQL'
$env:DB_PASSWORD = $credential.GetNetworkCredential().Password
python dataset/import_flashcards.py dataset/additions/example_addition.csv --apply
Remove-Item Env:DB_PASSWORD
```

`psql` được tìm theo `--psql`, `PSQL_PATH`, PATH hoặc thư mục cài PostgreSQL Windows.
Ví dụ khi muốn chỉ định rõ:

```powershell
$env:PSQL_PATH = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
```

Importer truyền mật khẩu qua môi trường của tiến trình con, không đặt vào câu lệnh.
Có thể dùng các thiết lập `PGSSLMODE`, `PGSSLROOTCERT` theo chính sách server.
Kết nối dùng UTF-8 và tắt prompt mật khẩu để lỗi cấu hình được báo ngay.

`--topics` là kiểm tra local tùy chọn. Khi không dùng tùy chọn đó, topic được
xác minh trực tiếp trong PostgreSQL, nên topic mới ở database không cần tồn tại
trong file seed cũ. Khi validate offline, dùng `topics.csv` hoặc bản CSV catalog
mới có chứa tất cả topic của lô cần import.

### Upsert và thống kê

- Cặp từ + nghĩa chưa có: insert.
- Cặp đã có và metadata khác: update topic, IPA, loại từ, hai câu ví dụ và CEFR.
- Cặp đã có và metadata giống nhau: skipped; không đổi `updated_at`.
- Topic không tồn tại hoặc một lỗi database theo dòng: failed kèm dòng và lý do.

Duplicate trong cùng một CSV hoặc lỗi cấu trúc/validation sẽ từ chối cả lô
**trước khi chạy SQL**, không đổi database. Các trường hợp lỗi file/header không
gắn được với một dòng dữ liệu có `row=0/1`; tổng `failed` khi đó không phải số dòng
CSV không đọc được. Hãy đọc `status=VALIDATION_FAILED` và thông báo lỗi.

Khi CSV hợp lệ nhưng một dòng lỗi database, các dòng hợp lệ khác vẫn được commit.
Importer trả exit code **1 nếu có failed**; xem summary, sửa dòng lỗi và import lại.
Đây là import từng dòng với rollback cục bộ, không phải all-or-nothing cho lỗi theo dòng.
Lỗi SQL trước COMMIT sẽ rollback toàn transaction. Lỗi cấu hình trước khi chạy
SQL báo `NO_SQL_EXECUTED`. Lỗi tiến trình database/kết nối báo
`DATABASE_RESULT_NOT_VERIFIED` với số đếm `null`; nếu kết nối mất đúng lúc COMMIT,
cần truy vấn PostgreSQL rồi chạy lại cùng lô để xác nhận.
SQL upsert dùng [ON CONFLICT của PostgreSQL](https://www.postgresql.org/docs/18/sql-insert.html).
Importer khóa ghi bảng trong transaction để việc phân loại kết quả không bị race;
nên chạy các lô lớn vào lúc ít ghi dữ liệu. Luôn inspect SQL output trước khi chạy.

Chính tả `english` và `vietnamese` của bản ghi cũ được giữ nguyên khi khác hoa/thường.
Đổi nghĩa là **khóa mới**, nên tạo thêm flashcard. Muốn sửa lỗi chính tả của khóa
cũ phải chỉnh record theo `id` qua công cụ quản trị; importer không suy đoán khóa cũ.
Seed/import không tự xóa từ cũ và không bật lại flashcard đã bị vô hiệu hóa.

## Thêm chủ đề mới

Ví dụ thêm trực tiếp qua Query Tool:

```sql
INSERT INTO topics (code, name, description, image_url)
VALUES ('SPORT', 'Sport', 'Từ vựng về thể thao', NULL)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    image_url = EXCLUDED.image_url,
    updated_at = CURRENT_TIMESTAMP;
```

CSV bổ sung dùng `SPORT`; importer tra ID thực tế. Các chủ đề cũ không cần chỉnh sửa.
Để dùng validator offline, thêm dòng SPORT vào catalog topics hoặc tạo một catalog
topics CSV riêng theo đúng header rồi truyền `--topics`.
Nếu muốn cập nhật seed catalog và seed SQL, thêm dòng vào `topics.csv`, sau đó chạy:

```powershell
python dataset/validate_dataset.py
python dataset/generate_seed_sql.py
```

`generate_seed_sql.py` chạy validation **chế độ mở rộng**, nên cũng dùng được khi
seed phát triển lên nhiều topic/từ hơn. SQL được tạo từ CSV hiện tại và không chứa ID.
Chạy `seed_topics.sql` trước khi import từ của topic mới. `--seed` dành riêng cho
snapshot ban đầu 10/300/30 và sẽ fail nếu bạn đã mở rộng snapshot đó.

## SQL kiểm tra sau import

```sql
SELECT COUNT(*) FROM topics;
SELECT COUNT(*) FROM flashcards;

SELECT t.code, t.name, COUNT(f.id) AS total
FROM topics t
LEFT JOIN flashcards f ON f.topic_id = t.id
GROUP BY t.id, t.code, t.name
ORDER BY t.name;

SELECT level, COUNT(*)
FROM flashcards
GROUP BY level
ORDER BY level;

SELECT LOWER(english), LOWER(vietnamese), COUNT(*)
FROM flashcards
GROUP BY LOWER(english), LOWER(vietnamese)
HAVING COUNT(*) > 1;

-- Không được có flashcard mất chủ đề.
SELECT f.id FROM flashcards f
LEFT JOIN topics t ON t.id = f.topic_id
WHERE t.id IS NULL;
```

Trên database mới chỉ chạy seed: 10 topics, 300 flashcards, 30/topic,
duplicate query và orphan query trả 0 dòng.
Database runtime đã thêm dữ liệu không còn bị ràng buộc về số lượng 300.

## Kiểm thử và bằng chứng

```powershell
python dataset/test_dataset.py
# Đặt DB_* trỏ đến một database kiểm thử riêng trước lệnh sau:
python dataset/test_dataset.py --integration
```

Integration tạo schema UUID riêng, đặt search_path chỉ vào schema đó, kiểm thử rồi
xóa đúng schema do nó tạo. Không ghi vào bảng ở `public` hoặc schema khác.
Đã chạy **11 test PASS**: seed, mở rộng 301 từ, UTF-8/CSV lỗi, thiếu field/enums,
duplicate chuẩn hóa, từ đa nghĩa, thêm topic và vòng đời PostgreSQL thực tế.
Vòng đời gồm seed chạy lại, ID chủ đề không cố định, cập nhật giữ ID/timestamps,
đổi topic, giữ trạng thái inactive, case tiếng Việt, topic không tồn tại,
escaping SQL, duplicate và CLI thực tế. Xem `validation_report.txt` để đọc output.

Không có backend, frontend, Android, authentication, quiz, bảng xếp hạng hay tiến độ
người dùng trong giai đoạn dataset này.
