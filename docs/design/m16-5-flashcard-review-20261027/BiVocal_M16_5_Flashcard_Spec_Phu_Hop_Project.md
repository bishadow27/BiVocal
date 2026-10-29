# BiVocal M16.5 — Đặc tả Flashcard UI phù hợp project

**Trạng thái: DESIGN REVIEW ONLY.** Chưa triển khai Android, chưa xác nhận source FlashcardActivity/API SRS. Bản này dựa trên `flashcard_demo.html` Antigravity và những quyết định M16.3/M16.4 đã chốt.

## 1. Mục tiêu và nguyên tắc

- Giữ diện mạo: nền `#090D16`, card `#111726`, viền `#334155`, cyan `#38BDF8`, xanh `#2563EB`, chữ `#F8FAFC`, chữ phụ `#94A3B8`; không hình ảnh cho từng từ, không hiệu ứng vũ trụ/neon/ánh sáng giả.
- Android native **Java/XML, MVVM, Retrofit, Online-Only**. Không WebView.
- Tận dụng `FlashcardActivity`, ViewModel/Repository, TTS/audio, API và thuật toán SRS **đang có**. Không xây lại Backend hoặc sửa database.
- Flashcard là **một thẻ có hai mặt**, không phải hai Activity. Trong ôn tập, đánh giá xuất hiện sau khi lật thẻ, **không phải màn thứ ba riêng**.
- Màn hoàn thành là trạng thái sau **toàn bộ phiên**, không phải mỗi từ.
- Khi dữ liệu thiếu, ẩn thành phần không hỗ trợ hoặc hiển thị Empty State thích hợp. Không tạo dữ liệu giả trong production.
- Header có Back vì đây là màn nằm trong luồng học, khác màn Home/Topics cấp cao.
- **Không Bottom Navigation 4 tab trong phiên học**, nhằm tập trung và tránh chạm nhầm. Trở về Topics qua Back/Exit đúng luồng.

## 2. Hai chế độ nghiệp vụ

| Chế độ | Trình tự giao diện | Điểm cần xác thực trong source |
|---|---|---|
| `NEW_TODAY` | Mặt trước → mặt sau → hành động xác nhận/tiếp tục theo flow thật → từ tiếp theo → kết thúc phiên | Tên nút thực tế, thời điểm ghi học/XP/goal, cách xử lý skip và end session |
| `REVIEW_TODAY` | Mặt trước → mặt sau → thao tác đánh giá đã có (ví dụ `Đã nhớ`/`Chưa nhớ`) → đợi API xác nhận → từ tiếp theo → kết thúc phiên | Chính xác loại action và request/response SRS; không tự biến thành 4 mức Anki |

**Không triển khai bốn action `EASY` / `GOOD` / `HARD` / `AGAIN` từ HTML chỉ vì demo có.** Không hardcode `+4 ngày`, `+2 ngày`, `+12 giờ` hoặc “ôn lại ngay”. Những quyết định này thuộc Backend/SRS hiện có.

## 3. Token và chuyển đổi

Hệ số 420 dpi: `px = dp × 2.625`. Màn hình **1080×2400 px** tương đương ~**411.4×914.3 dp**; `412×915 dp` là frame thiết kế danh nghĩa, sai khác nhỏ do làm tròn.

| Token | dp / sp | px xấp xỉ @420dpi |
|---|---:|---:|
| Lề ngang | 24dp | 63 |
| Header app | 56dp | 147 |
| Khoảng cách khối | 16–24dp | 42–63 |
| Flashcard radius | 20dp | 52.5 |
| Flashcard padding | 24dp | 63 |
| Button chính | 52dp | 136.5 |
| Nút trước/sau | 52dp | 136.5 |
| Thanh progress | 8dp | 21 |
| Chip radius | 12dp | 31.5 |
| Vùng chạm tối thiểu | 48dp | 126 |
| Từ mặt trước | 34sp | ~89.3 px ở font scale 1 |
| Từ mặt sau | 26sp | ~68.3 px ở font scale 1 |
| Nghĩa | 20sp | ~52.5 px ở font scale 1 |
| Phiên âm | 16–17sp | ~42–44.6 px ở font scale 1 |

`1dp stroke` tương ứng 2.625px vật lý trước làm tròn; `1px` vật lý **không đồng nghĩa** `1dp`. Android dùng drawable stroke `1dp` cho đồng nhất UI.

## 4. Auto Layout tree (Figma)

```text
Flashcard Session / Vertical
  ├─ Status Bar (SYSTEM; Android sở hữu)
  ├─ Session Header / H≈56dp / paddingH=24dp
  │   ├─ Back action / touch ≥48dp
  │   ├─ Topic name + current/total
  │   └─ Overflow (chỉ nếu source đã có hành động thực)
  ├─ Progress Row / bar=8dp / real session state
  ├─ Session Content / weight=1 / paddingH=24dp / minHeight=0
  │   ├─ Flashcard container / fill / radius20dp / padding24dp
  │   │   ├─ FRONT: Word + IPA + audio (conditional) + POS (conditional) + flip hint
  │   │   └─ BACK: Word + IPA + audio + meaning + POS + examples (conditional)
  │   └─ Session Actions / below card
  │       ├─ NEW_TODAY: confirmation/next per existing behavior
  │       └─ REVIEW_TODAY: existing SRS controls, ONLY after reveal
  ├─ Completion (replaces Session Content once all processed)
  │   ├─ Checkmark + text
  │   ├─ Real available session metrics ONLY
  │   └─ Existing supported next/exit actions
  └─ System gesture/navigation inset (SYSTEM)
```

**Các tab chuyển giai đoạn và nút “📐 Figma Spec” chỉ xuất hiện bên ngoài khung điện thoại trong HTML demo. Không đưa chúng vào APK.**

## 5. Dữ liệu UI / điều kiện hiển thị

| Trường UI | Nguồn cần dùng |
|---|---|
| Từ Anh/IPA/POS/nghĩa/ví dụ | Flashcard model/API đã có |
| Tên chủ đề | Topic hiện tại |
| Số thẻ & tiến độ | Phiên học đang chạy, không hardcode `5/30` |
| Audio phát âm | TTS/audio implementation hiện tại; nếu không hỗ trợ thì disable/hide |
| Đánh giá nhớ | Action + request của SRS hiện tại, không thêm 4 enums mới |
| Hoàn thành phiên | Sự kiện hoàn thành tất cả mục trong queue |
| Thống kê hoàn thành | Chỉ các chỉ số session thực sự được theo dõi; không hardcode `30/28/2/15 phút` |
| XP, Daily Goal | Chỉ refresh/hiển thị sau phản hồi hợp lệ của cơ chế hiện tại |

## 6. Các ràng buộc UX và kỹ thuật

- Từ dài, nghĩa và ví dụ dài: card cuộn bên trong khi cần, nút hành động không bị che.
- Hỗ trợ màn nhỏ `320×640dp`, frame `412×915dp`, Emulator hiện tại, font scale `1.3`; WindowInsets đúng status/gesture bar.
- Back xử lý đúng khi card đang lật và khi có session đã thay đổi; không tự giả định discard/save.
- Tránh skip đánh giá nếu nghiệp vụ thật không cho phép. Loading/disable sau submit để tránh request trùng.
- Không tự tạo lượt học, cập nhật DB hoặc reset account chỉ để dựng ảnh.
- Không sửa Home M16.3, Topics M16.4, 20.000 Flashcard, mapping M15.7, JWT/Auth, Quiz, Gamification, Admin.

## 7. Cổng duyệt trước khi giao Codex

1. Codex/Antigravity chỉ **audit** `FlashcardActivity`, ViewModel/Repository/API của NEW_TODAY và REVIEW_TODAY, layout và logic SRS hiện có trước khi chốt nút.
2. Xác nhận thao tác ghi nhận đúng của từng mode bằng source (bao gồm “Đã nhớ / Chưa nhớ” nếu đang dùng).
3. Đảm bảo ví dụ, audio, loại từ, session summary chỉ xuất hiện khi có dữ liệu/chức năng thật.
4. Duyệt demo HTML mới, sau đó mới cho phép code Java/XML theo quy trình approval của project.
5. Milestone lớn M16.5 cần **người dùng nghiệm thu**; không tự ghi PASS chỉ nhờ build/unit test.

**Lưu ý:** Demo HTML có thể phát âm mẫu bằng Web Speech API của trình duyệt để xem trước. Đây KHÔNG phải đề xuất đổi engine phát âm của Android.
