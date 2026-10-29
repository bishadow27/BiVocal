# BiVoca M15.5 — Bản sửa dữ liệu trước khi gửi Codex

## Phạm vi thực tế

Bộ này được tạo từ **500 thẻ mẫu cuối** (`quality-sample-500-final.csv`) và **240 thẻ mẫu trước** (`quality-review-240.csv`) bạn đã tải lên. Không có file 15.000 bản ghi staging đầy đủ và không có source M15.5 hiện tại, do đó **chưa thể thay thế toàn bộ 15.000 thẻ** hay trực tiếp sửa PostgreSQL `flashcard_english` từ môi trường này.

Cảnh báo đã kiểm tra: **5 giá trị ID bị trùng giữa hai file CSV nhưng tương ứng với từ khác nhau** (`12254`, `15241`, `15176`, `5565`, `18240`). Vì vậy bản vá đối chiếu theo *English + POS + nghĩa gốc*, không áp dụng bằng ID.

Đây là **đề xuất sửa ngữ nghĩa độc lập**; không được ghi `HUMAN_APPROVED`, không được xem các thẻ không gắn cờ là đã thẩm định, không được tự động import bản sửa vào production nếu chưa kiểm tra nguồn và uniqueness.

## Những file đã làm sẵn

- `curated_semantic_overrides.csv`: đối chiếu theo English + POS + nghĩa Việt gốc (không theo DB ID vì ID staging có thể đổi), chứa nghĩa đề xuất và lý do. `BLOCK_REVIEW` là cách ly; `SUGGEST_REPLACE` chỉ là sửa đề xuất, **không tự duyệt**.
- `quality_sample_500_annotated.csv`: đủ 500 hàng và nhãn/đề xuất cụ thể.
- `quality_sample_240_annotated.csv`: đủ 240 hàng, giữ các nhãn NEEDS_REVIEW từ file nguồn.
- `quality_sample_500_draft_fixed.csv`, `quality_sample_240_draft_fixed.csv`: bản **đã điền nghĩa đề xuất ở cột `draft_vietnamese`** để tiết kiệm thao tác thủ công; không được import trực tiếp vì chưa kiểm chứng nguồn và trùng với production.
- `BiVoca_Quality_Audit.xlsx`: bản Excel dễ xem, phân màu.
- `bivoca_semantic_guard.py`: script stdlib chỉ đọc CSV/JSONL, xuất `qa_actions.csv` và `qa_summary.json`. Phát hiện metadata, POS/meaning bất thường, exact duplicate và các lỗi đã biết. **Không ghi/sửa source hay DB.**
- `test_bivoca_semantic_guard.py`: bài kiểm tra smoke test.

## Cách chạy tại repo trong Codex

Sau khi giải nén pack vào ví dụ `backend/.m15-5-fixpack/`:

```powershell
python backend/.m15-5-fixpack/bivoca_semantic_guard.py --input backend/.m15-5-fixpack/quality-sample-500-final.csv --overrides backend/.m15-5-fixpack/curated_semantic_overrides.csv --out-dir backend/.m15-5-verification/preaudit
```

Lệnh trên chỉ minh họa đường dẫn; **để quét 15.000 ứng viên thật**, thay `--input` bằng file CSV/JSONL ứng viên M15.5 trong project (cần có cột English/Vietnamese/POS; nếu schema khác, Codex chỉ viết adapter tối thiểu).

Đúng nguồn staging nên là các file trong `backend/datasets/processed/m15_5/` đã được Codex tạo ở máy người dùng.

> Đầu vào mẫu trong ví dụ là 500 dòng **để thử script**, không phải quét đủ 15.000 thẻ. Để kiểm tra toàn bộ 15.000 thẻ mới, chỉ đổi input sang artifact ứng viên staging thật.

## Quy trình tối ưu usage cho Codex

1. Giải nén, đọc README, chạy unit tests của pack.
2. Xác định file candidate 22.000 hoặc 15.000 mới thực tế, chạy `bivoca_semantic_guard.py` để tạo danh sách hành động.
3. Dùng các nghĩa đề xuất **sau khi kiểm chứng nguồn**; thẻ có lỗi ngữ nghĩa không thể khắc phục thì loại/thay bằng ứng viên khác đã có nguồn.
4. Rebuild staging 20K. Lấy mẫu độc lập mới; không tái dùng nhãn PASS cũ.
5. Chỉ sau QA đạt, backup/restore-check production và import checkpoint 10K/15K/20K như kế hoạch. Không tự gọi script này là một Quality Gate ngữ nghĩa đủ mạnh.

## Chú ý đánh giá

Giữ nguyên 5.000 thẻ production và dữ liệu tiến độ. Mục tiêu 20K chỉ là mục tiêu; không tô hồng bằng cách dùng từ sai hoặc ghi nhãn người duyệt giả. Tránh đổi `english`/`vietnamese` ở thẻ **đang có tiến độ**; bản sửa này hướng đến 15K thẻ chưa publish.
