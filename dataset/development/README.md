# Snapshot PostgreSQL cho development

`schema.sql` và `vocabulary.sql` được xuất từ cùng một snapshot chỉ đọc PostgreSQL 18. `schema.sql` chỉ chứa cấu trúc; `vocabulary.sql` chỉ chứa `topics`, `flashcards`, `flashcard_topics`, `vocabulary_sources`, `flashcard_provenance`, `learning_system_config`, `pgmigrations` và sequence của các bảng này. Không xuất bảng tài khoản, xác thực, tiến độ, Quiz, điểm hay lịch sử XP.

Số liệu khi xuất: 38 topics, 20.000 flashcards, 23.043 liên kết và 34.133 bản ghi provenance. Snapshot được thử import vào database QA mới, rỗng; xem [hướng dẫn](../../CLONE_AND_RUN.md). Không dùng snapshot để ghi đè database đang vận hành.

Dữ liệu từ vựng bao gồm nguồn Skypedia/MinhQND, CC BY-SA 4.0, và Words CEFR Dataset, MIT. Giữ [attribution và giấy phép Skypedia](../../backend/datasets/en-vi/ATTRIBUTION.md), [license](../../backend/datasets/en-vi/LICENSE), [attribution CEFR](../../backend/datasets/cefr/ATTRIBUTION.md) và [license CEFR](../../backend/datasets/cefr/LICENSE) khi phân phối. Thông tin provenance và điều khoản nguồn trong từng bộ dữ liệu cần được giữ lại.
