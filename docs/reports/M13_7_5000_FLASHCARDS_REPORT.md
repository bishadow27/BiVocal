# M13.7 — Báo cáo hoàn thiện kho 5.000 Flashcard

**Ngày nghiệm thu:** 2026-10-23  
**Database vận hành:** `flashcard_english`  
**Kết luận:** **DONE — 5.000 Flashcard thực tế, đã kiểm tra PostgreSQL, API, Admin API, Android và Room offline.**

## 1. Phạm vi và nguồn dữ liệu

- Giữ nguyên 358 Flashcard đã có trước M13.7, toàn bộ user, progress, Quiz, score và XP.
- CEFR lấy từ `backend/datasets/cefr/word_list_cefr.csv` (7.988 dòng; 7.035 headword; 7.981 cặp headword/POS).
- Nghĩa, IPA và ví dụ lấy từ bản SQLite của **Skypedia English-Vietnamese Dictionary**, commit `388adc0826300912e5b0311c089e0e70494b065f`, giấy phép **CC BY-SA 4.0**.
- SHA-256 file từ điển: `2b65bea9577c2a4149caa90940c806f321d1c6cfac503ed8bbd9f93e43a0033a`.
- Pipeline không tự tạo nghĩa, IPA hoặc ví dụ. CEFR được gắn ở phạm vi headword + POS, không khẳng định từng nghĩa là nghĩa CEFR cốt lõi. Các nghĩa đa nghĩa không chắc chắn bị loại khỏi Quiz tự động.

## 2. Backup và thử khôi phục

- Backup custom format được giữ tại:
  `C:\Users\phuc\AppData\Local\BiVoca\backups\flashcard_english_before_5000_20261023041616.dump`
- Kích thước: 4.376.527 byte.
- SHA-256: `ffffc6cc3dd96c2dcf861fcd0edbc55ba721d3d37496e04d7467a094b2990408`.
- ACL chỉ giữ quyền Full Control cho `PC-OF-BI\phuc` và `NT AUTHORITY\SYSTEM`.
- Restore-check trên database cô lập PASS, snapshot và hash dữ liệu trùng backup; database restore-check đã được xóa sau kiểm tra. File backup không bị xóa.

## 3. Cơ chế kiểm tra tự động

Pipeline `backend/scripts/large_vocabulary_pipeline.py` kiểm tra:

- English/POS/CEFR hợp lệ và khớp chính xác theo headword + POS.
- Nghĩa tiếng Việt có nội dung, giới hạn độ dài, không bằng chính headword, không chứa markup hoặc nội dung bị chặn.
- IPA có định dạng hợp lệ.
- Ví dụ có độ dài hợp lệ, chứa headword và không phải mẫu placeholder chung.
- Quan hệ word-definition phải tồn tại trong SQLite nguồn.
- Trùng khóa chuẩn hóa English + POS + Vietnamese.
- Chủ đề dựa trên category CEFR đã ánh xạ hoặc bằng chứng nghĩa; không tự đoán chủ đề khi thiếu bằng chứng.
- Nghĩa gần trùng trong cùng English/POS dùng token tiếng Việt và ngưỡng Jaccard bảo thủ `>= 0,85`; trường hợp không chắc chắn chuyển `NEEDS_REVIEW`.
- Script prepare/publish có guard database local, yêu cầu xác nhận đúng tên DB, dry-run, preview toàn bộ lô, kiểm tra trùng và chạy lại an toàn.

Trong lấy mẫu API sau lần xuất bản đầu, phát hiện `singular` có hai nghĩa gần trùng. Bộ kiểm tra được bổ sung, tìm thấy 6 ứng viên cần xem lại; 5 ứng viên đã xuất bản chưa có liên kết progress/Quiz/XP được đưa lại hàng chờ bằng giao dịch, rồi thay bằng 5 ứng viên hợp lệ khác. Quy trình sửa được chạy PASS trên staging trước production.

## 4. Kết quả pipeline

| Chỉ số | Số lượng |
| --- | ---: |
| Tổng ứng viên | 24.388 |
| `APPROVED_AUTO` trong artifact | 4.700 |
| `NEEDS_REVIEW` | 930 |
| `REJECTED` | 18.758 |
| Nghĩa gần trùng chuyển review | 6 |
| Trùng chuẩn hóa bị loại | 12 |
| Ví dụ thiếu headword | 427 |
| Ví dụ placeholder/chung | 300 |
| Nghĩa sai định dạng | 255 |
| Nhãn usage không cốt lõi | 264 |
| CEFR xung đột | 6 |
| Thiếu chủ đề có căn cứ | 18.474 |

930 ứng viên chờ gồm 924 bản ghi hoãn sau khi đủ chỉ tiêu và 6 trường hợp nghĩa gần trùng. Không có bản ghi chờ nào được tự xuất bản.

## 5. Staging và xuất bản production

Database staging: `bivoca_5000_staging_9a5230c777`.

- Dry-run: cần 4.642, có 4.642 ứng viên được chọn, 0 exact duplicate, 47 lô.
- Publish staging: 46 lô × 100 và lô cuối 42; đạt đúng 5.000.
- Sửa near-duplicate staging: đưa 5 thẻ về review, xóa đúng 5 thẻ mới không có quan hệ người dùng, thêm 5 thẻ thay thế; trở lại đúng 5.000.
- Hash 358 thẻ gốc: `49bb170c588b5de050ea847bb54b2f93`, khác biệt hàng dữ liệu cũ: 0.
- Database staging đã được xóa sau khi hoàn tất đối chiếu; backup production vẫn được giữ nguyên.

Production được chạy đúng quy trình đã kiểm chứng:

- Trước import: 358 Flashcard, 16 chủ đề.
- Thêm: 4.642 Flashcard qua 47 lô.
- Sau sửa chất lượng: vẫn đúng 5.000 Flashcard; 19 chủ đề.
- Idempotency dry-run sau cùng: cần 0, approved 0, exact duplicate 0, batches 0.

## 6. Snapshot production sau cùng

| Chỉ số | Trước | Sau | Kết quả |
| --- | ---: | ---: | --- |
| Flashcard | 358 | 5.000 | PASS |
| Chủ đề | 16 | 19 | PASS |
| User | 3 | 3 | Không đổi |
| Progress | 10 | 10 | Không đổi |
| Quiz results | 8 | 8 | Không đổi |
| User score | 2 | 2 | Không đổi |
| XP history | 4 | 4 | Không đổi |
| Exact duplicate mới | — | 0 | PASS |
| Bản ghi mới sai trường bắt buộc | — | 0 | PASS |

Phân bố CEFR của toàn bộ 5.000 thẻ:

- A1: 1.438
- A2: 1.241
- B1: 1.350
- B2: 971

Có 626 thẻ import được đánh dấu đủ chắc chắn cho Quiz; cùng 300 thẻ gốc tạo tổng 926 thẻ Quiz eligible. Các nghĩa đa nghĩa không chắc chắn vẫn hiển thị để học nhưng không được đưa vào Quiz tự động.

## 7. API, Admin Web và Android

### API và Admin

- Health: HTTP 200, database connected.
- `/api/v1/topics/catalog`: 19 chủ đề và số lượng thẻ thực tế.
- Search `singular`: đúng 1 kết quả sau sửa near-duplicate.
- Admin review API dùng JWT thử nghiệm chỉ tồn tại trong bộ nhớ: stats HTTP 200, list HTTP 200; total 24.388, pending 930, approved 0, rejected 18.758, published 4.700.
- Admin Web: 43/43 test PASS, production build PASS.
- Tab Admin đang mở bị chuyển về Login vì JWT cũ hết hạn sau reload. Không tự điền hoặc tạo mật khẩu; cần quản trị viên đăng nhập lại để nhìn số liệu mới trên giao diện. Backend API phục vụ đúng số liệu.

### Android Emulator `BiVoca_API35_Pixel6`

- APK cài bằng `adb install -r`, giữ dữ liệu và phiên đăng nhập.
- Online: Topics nhận 19 chủ đề; Daily Life hiển thị 595 thẻ; tìm `singular` trả 1 kết quả đầy đủ English/Vietnamese/IPA/POS/CEFR/example.
- Quiz Daily Life tải 10 câu; câu lấy mẫu `guideline` và bốn đáp án được hiển thị từ API thật. Không gửi đáp án nên không làm thay đổi lịch sử Quiz.
- Room snapshot: 19 topic metadata, 664 Flashcard đã cache, trong đó 594 thẻ ID > 364; `maxCardId=5000`.
- Offline: tháo `adb reverse tcp:3000`, force-stop/relaunch, Topics vẫn hiển thị từ Room; Daily Life vẫn mở `1/595`, thẻ `check-in`; chuyển Next sang `2/595`. Sau đó ADB reverse được khôi phục.
- 8/8 instrumentation tests PASS trên Emulator.

Ảnh bằng chứng offline: `M13_7_ANDROID_OFFLINE.png`.

## 8. Kết quả test

- Backend `npm run typecheck`: PASS.
- Backend `npm test`: 321/321 PASS.
- Pipeline Python: 9/9 PASS.
- Backend `npm run build`: PASS.
- Admin Web `npm test`: 43/43 PASS.
- Admin Web `npm run build`: PASS.
- Android `assembleDebug testDebugUnitTest`: BUILD SUCCESSFUL.
- Android `connectedDebugAndroidTest`: 8/8 PASS.

## 9. File source chính đã thêm/sửa

- `backend/scripts/large_vocabulary_pipeline.py`
- `backend/scripts/prepare-vocabulary-target.ts`
- `backend/scripts/publish-vocabulary-target.ts`
- `backend/scripts/requeue-near-duplicate-meanings.ts`
- `backend/src/modules/vocabulary-review/vocabulary-review.repository.ts`
- `backend/tests/test_large_vocabulary_pipeline.py`
- `backend/datasets/processed/bivoca_vocab_*.jsonl`
- `backend/datasets/processed/bivoca_vocab_statistics.json`
- `backend/datasets/processed/bivoca_vocab_validation_report.md`
- `backend/datasets/processed/bivoca_topics.json`
- `backend/.m13-7-verification/*`
- `M13_7_5000_FLASHCARDS_REPORT.md`
- `WORK_STATUS.md`
- `ANDROID_CONTEXT.md`

## 10. Giới hạn còn lại

- 930 ứng viên vẫn cần xem lại nếu muốn mở rộng sau 5.000.
- Dataset có nhiều nghĩa từ điển hợp lệ cho cùng headword/POS; CEFR chỉ xác nhận headword/POS. Vì vậy chỉ 626 thẻ import có độ chắc chắn đủ cao cho Quiz, còn các nghĩa khác phục vụ tra cứu/học và giữ đầy đủ nguồn.
- Room tải theo chủ đề khi người dùng mở, không tự tải toàn bộ 5.000 thẻ một lần. Đây là hành vi M8 hiện có nhằm tránh tải nặng; nghiệm thu đã xác nhận topic metadata và chủ đề Daily Life được cache/offline.

