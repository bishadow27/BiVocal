-- Generated from topics.csv; internal IDs are never supplied.
BEGIN;
SET LOCAL standard_conforming_strings = on;
INSERT INTO topics (code, name, description, image_url)
VALUES
    ('FOOD', 'Food', 'Từ vựng về thức ăn và đồ uống', NULL),
    ('ANIMALS', 'Animals', 'Từ vựng về động vật và môi trường sống', NULL),
    ('TRAVEL', 'Travel', 'Từ vựng dùng khi đi du lịch', NULL),
    ('SCHOOL', 'School', 'Từ vựng về trường học và học tập', NULL),
    ('FAMILY', 'Family', 'Từ vựng về gia đình và các mối quan hệ', NULL),
    ('TECHNOLOGY', 'Technology', 'Từ vựng về công nghệ và thiết bị số', NULL),
    ('WORK', 'Work', 'Từ vựng về công việc và môi trường làm việc', NULL),
    ('HEALTH', 'Health', 'Từ vựng về sức khỏe và chăm sóc cơ thể', NULL),
    ('SHOPPING', 'Shopping', 'Từ vựng về mua sắm và thanh toán', NULL),
    ('WEATHER', 'Weather', 'Từ vựng về thời tiết và khí hậu', NULL)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    image_url = EXCLUDED.image_url,
    updated_at = CURRENT_TIMESTAMP;
COMMIT;
