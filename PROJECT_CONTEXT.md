# PROJECT_CONTEXT.md — Tổng quan và Kiến trúc Hệ thống BiVoca

> Các báo cáo milestone đã được gom vào `docs/reports/`; xem [mục lục tài liệu](README.md).

**Ngày cập nhật:** 2026-10-17  
**Hiện trạng:** Đã xác minh qua mã nguồn và lệnh kiểm thử thực tế.

---

## 1. TỔNG QUAN DỰ ÁN
- **Tên dự án:** BiVoca
- **Mục tiêu:** Ứng dụng học từ vựng tiếng Anh bằng Flashcard, có trắc nghiệm Quiz, theo dõi tiến độ học tập, tích lũy XP, duy trì chuỗi ngày streak và bảng xếp hạng Leaderboard. Có trang web Admin quản trị dữ liệu.
- **Ngôn ngữ hiển thị:** Tiếng Việt.
- **Phong cách giao diện:** Dark Night Sky, nền navy đậm, card navy, xanh dương/cyan thương hiệu và chữ trắng/xám xanh.

---

## 2. KIẾN TRÚC TOÀN HỆ THỐNG

```
[ PostgreSQL 18 ] (Database: flashcard_english)
       │
       ▼
[ Backend REST API ] (Express 5 + TypeScript, http://127.0.0.1:3000/api/v1/)
       ├──► [ Android App ] (Java + MVVM + Retrofit + Room, kết nối qua ADB reverse)
       └──► [ Admin Web ] (React 19 + Vite + Tailwind, http://127.0.0.1:5173/)
```

- **Quy ước dữ liệu:**
  - Database PostgreSQL: `snake_case`.
  - Backend TypeScript & API JSON: `camelCase`.
  - Mọi route API nghiệp vụ đều nằm dưới tiền tố `/api/v1/`.

---

## 3. CÔNG NGHỆ THỰC TẾ SỬ DỤNG
- **Database:** PostgreSQL (hỗ trợ migration qua `node-pg-migrate`).
- **Backend:** Node.js (>=22.0.0), Express 5.2.1, TypeScript 7.0.2 strict, `pg` 8.23.1, Zod 4.6.5, jsonwebtoken 9.0.3, bcrypt 6.0.0, Node.js built-in test runner + tsx.
- **Android App:** Java 11, compileSdk 37, minSdk 24, targetSdk 37, AGP 9.3.3, Retrofit 2.11.0, OkHttp 4.12.0, Gson 2.11.0, AndroidX Lifecycle (ViewModel & LiveData 2.8.7), Material Components 1.14.0.
- **Offline cache Android M8:** Room 2.8.5, Java annotationProcessor, cache nội dung public theo chủ đề. M8 DONE trên OPPO CPH2557 Android 15; không đổi Android trong M9.
- **Admin Web:** React 19.3.0, Vite 8.3.2, TailwindCSS 4.3.3, TanStack Query 5.104.1, Lucide React, Recharts 3.10.1.

---

## 4. BỘ DỮ LIỆU TỪ VỰNG (DATASET)
- Vị trí: `dataset/`
- Gồm 10 chủ đề:
  1. `ANIMALS` (Động vật)
  2. `FAMILY` (Gia đình)
  3. `FOOD` (Thức ăn)
  4. `HEALTH` (Sức khỏe)
  5. `SCHOOL` (Trường học)
  6. `SHOPPING` (Mua sắm)
  7. `TECHNOLOGY` (Công nghệ)
  8. `TRAVEL` (Du lịch)
  9. `WEATHER` (Thời tiết)
  10. `WORK` (Công việc)
- Mỗi chủ đề có chính xác 30 flashcard → Tổng cộng 300 từ vựng.
- Phân bố trình độ CEFR: A1 (102 từ), A2 (84 từ), B1 (81 từ), B2 (33 từ).
- Đầy đủ phiên âm IPA Anh-Anh, từ loại, câu ví dụ tiếng Anh và bản dịch tiếng Việt.

---

## 5. MÔI TRƯỜNG VẬN HÀNH LOCAL
- Backend server: `http://127.0.0.1:3000`
- Admin Web: `http://127.0.0.1:5173` (được cấu hình proxy `/api` tới cổng 3000)
- Cơ chế nền: Thư mục `maintenance/` chứa launcher tự khởi động ngầm cho Windows (`local-admin.vbs`, `local-admin.mjs`), tự duy trì cả Backend và Admin Web khi người dùng đăng nhập Windows.

## 6. NGHIỆM THU M9 — 2026-10-14

- **M9 DONE:** Backend 311/311 tests, typecheck/build PASS; Admin Web 42/42 tests, build/format PASS. Không đổi Backend source, migration, dataset hoặc Android.
- PostgreSQL thực tế 18.6, DB `flashcard_english` có đủ 5 migration, gồm M6A role authorization đã apply. DB gốc chỉ được đọc trong các probe/test tự động.
- 68 HTTP checks trên server thật PASS; integration chỉ đọc DB gốc 5/5 PASS. DB riêng `bivoca_m9_20261014_5fc9558e413d` chạy integration có ghi và cleanup 10/10 PASS, được giữ lại theo yêu cầu người dùng.
- Browser Admin thật: Dashboard, topics/search, flashcards/filter/pagination, users/detail/progress/history, Quiz statistics, leaderboard, session restore và logout/guard PASS. Các mutation được kiểm tra qua API trên DB riêng và UI tests với fetch fixtures; không lưu thay đổi nội dung/quyền trên DB gốc để nghiệm thu.
- Sửa lỗi Admin restore xóa session khi lỗi mạng/5xx; giữ token, hiển thị lỗi và cho thử kết nối lại. 401 hoặc ADMIN_FORBIDDEN vẫn kết thúc phiên; request cũ không được hủy phiên mới.
- Báo cáo và giới hạn kiểm thử: `M9_INTEGRATION_SECURITY_REPORT.md`. M8 giữ DONE; `ANDROID_CONTEXT.md`, cấu hình giữ APK và cache không thay đổi.

## 7. M12 SETTINGS — 2026-10-17

- Android có hệ thống Settings hoàn chỉnh trong Drawer: study, audio, reminder, display, data và about. Cấu hình học/audio/reminder tách theo user; display theo thiết bị.
- Flashcard dùng trực tiếp lựa chọn mặt trước, TTS rate/auto pronunciation, text size và reduce motion. Reminder dùng AlarmManager inexact weekly + BroadcastReceiver; Room public cache có repository thống kê/xóa phạm vi an toàn.
- Không thay đổi Backend, PostgreSQL, Admin Web, dataset hay API. Build/test/lint và Emulator acceptance được ghi tại `M12_SETTINGS_REPORT.md`.
