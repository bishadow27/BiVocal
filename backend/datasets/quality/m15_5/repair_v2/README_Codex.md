# BiVocal M15.5 — Gói sửa lỗi dữ liệu v2 (2026-10-25)

## Kết luận, đọc trước khi chạy

Gói này được tạo bằng cách **quét thật 22.000 ứng viên + đối chiếu toàn bộ 15.000 ID staging + 500 dòng QA hậu DataFix**. Nó **không tự động sửa/viết nghĩa tiếng Việt** và **không có bất kỳ thẻ nào được người kiểm duyệt chuyên môn duyệt**. `APPROVED_AUTO`, `qualityVerified`, hoặc `independentMeaningConfirmed` trong dữ liệu nguồn không phải là bằng chứng nghĩa đúng. Không dùng file đề xuất để import production ngay.

Các số liệu ở `data/summary.json` là kết quả của bộ quy tắc **xác định lỗi và triage**, không phải chứng nhận 19.000 nghĩa tiếng Việt chính xác.

## Kết quả đã chạy trên dữ liệu được gửi

- Đọc: 22.000 ứng viên, 15.000 Flashcard staging (tất cả ID khớp candidate và nội dung so khớp English+POS+Vietnamese), 500 dòng QA, 137 đề xuất DataFix cũ.
- **57 BLOCK**: 24/24 lỗi QA gần nhất, 3 lỗi ngữ nghĩa phát hiện thêm khi rà mẫu, cùng các lỗi định dạng rõ ràng. (Một số nguyên nhân trùng trên cùng một hàng.)
- **2.895 REVIEW**: metadata từ điển, POS nghi ngờ, viết hoa danh từ riêng, đối chiếu VNEDICT quá ngắn, v.v. Một số cảnh báo có thể là false positive, cần xem từng nghĩa.
- **19.048 AUTO_PREFLIGHT_ONLY**: không bị các quy tắc hiện tại gắn cờ; **không phải người kiểm duyệt xác nhận**.
- Hai đề xuất 15.000 ứng viên, **chưa được phép publish**:
  - `data/minimal_change_15000_UNVERIFIED.jsonl`: thay 46 dòng hard-block trên staging; 2.467 dòng còn gắn REVIEW (cần kiểm duyệt nếu đưa ra học thật).
  - `data/proposed_additions_quality_triaged.jsonl`: tạm lọc hết BLOCK + REVIEW, gợi ý thay 2.513 ứng viên bằng dự phòng; **tuy vậy các từ dự phòng chưa được xác minh nghĩa và có thể sai**, không mặc định cách này tốt hơn.
- Có 7.000 ứng viên dự phòng chưa vào staging, nhưng số lượng dự phòng không phải bằng chứng chất lượng.
- Đối chiếu sẵn 24 trường hợp lỗi với phương án **thẻ cùng headword/POS khác nghĩa** (không tự đồng nhất nghĩa) trong `data/same_headword_alternatives_24.csv`.
- Tất cả quy trình đều **read-only với PostgreSQL**. Không có truy cập `flashcard_english`, người dùng, JWT, email hoặc dữ liệu học tập.

## Phát hiện cần sửa trong pipeline hiện tại

File `scripts/cross_validate_vnedict.py` trong gói nguồn có nhánh:

```python
matches = [...] if len(term) >= 2 and term in meaning
```

`term in meaning` là **so khớp chuỗi con**. Nó không đảm bảo: (1) hai nghĩa cùng sense, (2) POS khớp, (3) bản dịch không chứa metadata, hoặc (4) bản dịch không sai. Nên đổi tên trường báo cáo thành `independent_lexical_overlap` và đặt mặc định `semantic_verified=false` thay vì coi `independentMeaningConfirmed=true` là thẩm định ngữ nghĩa. Việc sửa tên phải duy trì tương thích với mã đang tiêu thụ field này hoặc migration thích hợp.

**Quan trọng:** Những bản sửa v2 được xác định theo `candidateId` từ nguồn, nhưng đối chiếu lỗi ngữ nghĩa theo **English + POS + nghĩa gốc**; **không bao giờ theo ID staging**. Dùng `inputSha256` trong `deploymentSelection` và checksum đầu vào để tránh lấy nhầm phiên bản.

## Cấu trúc dữ liệu

- `data/summary.json`: số liệu đối chiếu, SHA-256, phân loại và giới hạn.
- `data/triage_all_22000.csv`: toàn bộ ứng viên và lý do đánh dấu.
- `data/block_and_review.csv`: 2.952 ứng viên phải chặn hoặc rà soát.
- `data/hard_block_only.csv`: 57 ứng viên phải chặn vì cờ hiện có.
- `data/staging_candidate_removals.csv`: 2.513 ứng viên trong staging được đề xuất gỡ ở kịch bản thận trọng cao.
- `data/replacement_candidate_suggestions.csv`: 2.513 ứng viên dự phòng **chưa thẩm định**.
- `data/minimal_change_15000_UNVERIFIED.jsonl`: tập dự kiến thay tối thiểu 46 hard block.
- `data/proposed_additions_quality_triaged.jsonl`: tập dự kiến 15.000 không bị cờ tự động.
- `data/same_headword_alternatives_24.csv`: các nghĩa khác có sẵn trong bộ 22k cho 24 trường hợp QA.
- `data/additional_spotcheck_flags.csv`: 7 phát hiện bổ sung sau khi đọc dữ liệu nguồn, trong đó 3 BLOCK và 4 REVIEW; có ghi rõ AI-assisted, không giả human approval.
- `scripts/triage_vocabulary.py`: chạy lại công cụ QA độc lập, **không cần cài package**.
- `scripts/augment_evidence.py`: tạo kịch bản thay tối thiểu và các nghĩa thay thế.
- `scripts/test_repair_v2.py`: xác minh đối chiếu toàn bộ, tính duy nhất và 24/24 lỗi đã chặn.
- `input_original/`: bản gốc các file từ ZIP người dùng, chỉ để tái lập kết quả. Không có dữ liệu riêng tư.

## Chạy lại tại máy Codex (PowerShell)

Trong thư mục ZIP đã giải nén:

```powershell
python .\scripts\triage_vocabulary.py --input-dir .\input_original --output-dir .\data
python .\scripts\augment_evidence.py --input-dir .\input_original --output-dir .\data
python .\scripts\test_repair_v2.py --input-dir .\input_original --output-dir .\data
```

Các script chỉ đọc `input_original` và ghi `data`; không đụng database.

## Áp dụng vào project (việc Codex cần thực hiện)

1. Đọc `summary.json`, `triage_all_22000.csv`, `hard_block_only.csv` và `same_headword_alternatives_24.csv`.
2. Thêm **hard exclusions** vào `m15_5_vocabulary_pipeline.py` theo English + POS + Vietnamese gốc. Đảm bảo cả 24 lỗi hậu DataFix và 3 lỗi spot-check mới không được auto-approved. **Không sửa nghĩa trong source khi không có nguồn xác minh.**
3. `REVIEW` là **ưu tiên kiểm chứng**, không phải mọi dòng đều sai. Có thể dùng dự phòng nhưng phải rà nghĩa và POS trước khi bật `APPROVED_AUTO`.
4. Không coi `independentMeaningConfirmed` là bảo đảm chất lượng. Tách chỉ số lexical overlap và semantic verification.
5. Xác thực thẻ dự kiến bổ sung không trùng **5.000 thẻ production** theo unique constraint `LOWER(english), LOWER(vietnamese)`. Đầu vào ZIP **không có danh sách 5.000 thẻ production**.
6. Đánh giá nghĩa độc lập theo cặp **word + POS + Vietnamese gloss** và các sense khác nhau; kiểm tra đặc biệt nhóm từ đa nghĩa. Không gắn nhãn `HUMAN_APPROVED` nếu không có người duyệt.
7. Chỉ sau khi có bằng chứng QA đủ mạnh, làm backup ngay trước triển khai và test restore, checkpoint 10k/15k/20k trên staging; cần phê duyệt mới nếu phát sinh rủi ro ngoài phạm vi.
8. Phần Android/Backend/Admin đã có build E2E, chỉ chạy regression phần thay đổi. **Không chạy lại Google OAuth hay M14/M15 không liên quan.**

## Giới hạn không thể vượt qua chỉ bằng script

Không có cơ sở để xác nhận tất cả 15.000 nghĩa mới đúng với dữ liệu chỉ gồm hai từ điển có cùng lỗi hoặc lexical overlap. Gói này giúp **phát hiện, cách ly, đề xuất và giảm việc Codex phải phân tích lại**, không thay thế quy trình kiểm duyệt ngôn ngữ. Nếu mục tiêu 20.000 thẻ vượt nguồn đáng tin cậy, giữ các thẻ còn thiếu ở trạng thái **chờ duyệt**, không hiển thị nghĩa sai cho người học.

Quyền dữ liệu: Skypedia CC BY-SA 4.0 và VNEDICT CC BY 3.0; giữ attribution/license trong dự án khi phân phối dữ liệu phái sinh.
