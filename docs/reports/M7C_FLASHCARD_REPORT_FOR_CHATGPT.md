# Báo cáo M7C — Học flashcard theo chủ đề

**Dự án:** BiVoca Android  
**Package:** `com.example.bivoca`  
**Ngày:** 13/10/2026

## Mục tiêu

Từ màn hình Chủ đề, chọn một chủ đề để mở màn hình flashcard, tải từ vựng thật từ backend, xem từng thẻ, chuyển trước/sau và yêu cầu Android đọc từ tiếng Anh.

## API đã xác minh

- **Method/path:** `GET /api/v1/topics/{code}/flashcards` (ví dụ: `/api/v1/topics/FOOD/flashcards`).
- **Query parameters:** không có.
- **Xác thực:** endpoint public, không cần Bearer token.
- **Response:** JSON array; mỗi object có đúng 8 trường:

```json
{
  "id": 1,
  "english": "apple",
  "vietnamese": "quả táo",
  "pronunciation": "/ˈæpəl/",
  "partOfSpeech": "noun",
  "example": "I eat an apple every morning.",
  "exampleVi": "Tôi ăn một quả táo mỗi sáng.",
  "level": "A1"
}
```

`pronunciation`, `partOfSpeech`, `example`, `exampleVi`, và `level` có thể `null`. Backend đã được kiểm tra trong `backend/src/modules/topics/topic.route.ts`, `backend/src/modules/flashcards/flashcard.route.ts`, controller, service, repository, và `backend/src/shared/types/api.ts`.

## Android đã triển khai

- `TopicsActivity` mở `FlashcardActivity` và truyền đúng `topicCode` cùng `topicName`; không hard-code Food.
- Retrofit gọi `topics/{code}/flashcards` qua `RetrofitClient` public client.
- Model map các trường response; metadata không bắt buộc được phép null.
- Repository chuyển lỗi mạng, timeout, HTTP 400/401/404, và JSON không hợp lệ thành thông báo tiếng Việt. Có thể hủy request cũ.
- ViewModel cung cấp state danh sách, loading, error, và vị trí hiện tại; kiểm tra mã chủ đề, giới hạn Next/Previous, bỏ qua phản hồi cũ khi đổi chủ đề, giữ và khôi phục vị trí khi Activity được tạo lại.
- Activity có trạng thái loading, lỗi và nút Thử lại, danh sách rỗng, thẻ từ cùng phiên âm/nghĩa/loại từ/cấp độ/ví dụ, và các nút Trước/Phát âm/Tiếp. Nút điều hướng bị khóa tại hai đầu.
- Android TextToSpeech ưu tiên `Locale.UK`, thử `Locale.US` khi cần, bật nút khi khởi tạo thành công và giải phóng engine khi Activity đóng. Manifest khai báo TTS service query và Activity không export.
- Không thay đổi schema, migration, dataset, hay backend API.

## File liên quan

- `android-app/app/src/main/java/com/example/bivoca/data/model/Flashcard.java`
- `android-app/app/src/main/java/com/example/bivoca/data/api/FlashcardApi.java`
- `android-app/app/src/main/java/com/example/bivoca/data/repository/FlashcardRepository.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/flashcard/FlashcardViewModel.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/flashcard/FlashcardActivity.java`
- `android-app/app/src/main/res/layout/activity_flashcard.xml`
- `android-app/app/src/main/res/values/flashcard_strings.xml`
- `android-app/app/src/main/res/drawable/ic_flashcard_speaker.xml`
- `android-app/app/src/main/java/com/example/bivoca/ui/topics/TopicsActivity.java`
- `android-app/app/src/main/AndroidManifest.xml`
- Unit tests: `android-app/app/src/test/java/com/example/bivoca/ui/flashcard/FlashcardViewModelTest.java`, `android-app/app/src/test/java/com/example/bivoca/data/api/FlashcardContractTest.java`

## Kết quả kiểm tra

- `:app:assembleDebug` — PASS.
- `:app:testDebugUnitTest` — PASS, 14/14 tests, không lỗi hoặc bỏ qua test.
- `:app:lintDebug` — hoàn tất, 0 lỗi và 194 cảnh báo trên toàn project.
- `:app:installDebug` — PASS trên OPPO CPH2557, Android 15.
- Xác minh endpoint bằng dữ liệu database thật: Food 30 thẻ, Animals 30 thẻ, Travel 30 thẻ.
- Xác minh trên điện thoại: Home → Chủ đề → Food → thẻ đầu `apple`; Next chuyển sang `bread`; Previous quay lại `apple`.
- Unit tests kiểm tra request path, không có query/Bearer, mapping 8 trường, metadata nullable, điều hướng ở hai đầu, danh sách rỗng, topic không hợp lệ, hủy request cũ, lỗi và retry, cùng vị trí được khôi phục/giới hạn.

## Trạng thái Text-to-Speech

Google Text-to-Speech có trên OPPO. Ứng dụng khởi tạo TTS và nút Phát âm ở trạng thái sẵn sàng, nhưng lần xác minh âm thanh trên điện thoại bị gián đoạn trước khi xác nhận engine hoàn tất phát. Chưa có xác nhận bằng tai người nghe; không kết luận TTS đã PASS hoàn toàn.

## Giới hạn phạm vi

Không triển khai Quiz, Room/offline, spaced repetition, migration, hay thay đổi backend trong phần M7C này. Các tính năng khác đã có trong project hiện tại không được xem là nội dung mới của milestone này.

## Yêu cầu review dành cho ChatGPT

Hãy review báo cáo và các file source Android/backend được đính kèm. Xác minh contract API, luồng Activity → ViewModel → Repository → Retrofit, việc hủy request, state empty/error/loading, ranh giới Next/Previous, cấu hình và vòng đời TextToSpeech, cùng kết quả build/test. Chỉ nêu lỗi kỹ thuật có bằng chứng và đề xuất sửa đổi nhỏ nhất. Không bắt đầu Quiz/Room hoặc thay đổi backend. Không coi TTS đã PASS nếu chưa xác nhận âm thanh thực tế.
