# M16_RECOVERY_AUDIT

**Ngày kiểm kê:** 01/11/2026, múi giờ Asia/Saigon.
**Phạm vi:** BƯỚC 0 — kiểm kê source và lập kế hoạch, chưa triển khai Phase C/D.
**Trạng thái:** AUDIT_COMPLETE / AWAITING_PHASE_C_APPROVAL. Đây không phải nghiệm thu bản phục dựng.
**Source được đối chiếu:** `5638fdf6b2ef9290d30dde068a3cf68b3b19e807`, nhánh `feature/initial-import`.

## 1. Kết luận để quyết định

1. **Home và Login/Register/Forgot còn nguyên source tracked tại HEAD đã nhập lại. Giữ nguyên.** Home Phase A và Login Phase B có báo cáo nghiệm thu 29/10; Recovery Brief xác định phạm vi bảo vệ gồm Register/Forgot. Không dùng các báo cáo cũ còn ghi chờ duyệt để đảo ngược nghiệm thu mới hơn.
2. **Topic List chưa mất toàn bộ:** còn bản M16.4 ngày 27/10, nhưng thiếu các chỉnh sửa màu và filtered-layout cuối Phase C. List hiện mở thẳng Flashcard, chưa qua Detail.
3. **Topic Detail mất phần triển khai:** không có Activity, ViewModel, adapter, hai layout, resource Phase C hoặc hai test Detail được brief nêu. Repository/API/DTO cần để tái dựng đã có, không cần sửa Backend hoặc tạo lớp truy cập dữ liệu trùng.
4. **Flashcard chưa mất nền tảng học:** còn ba mode, phân trang, TTS, Settings, các trạng thái và logic ACK của hàng đợi. Tuy nhiên typography, progress, nút flip, panel đánh giá, phản hồi sau ACK và navbar chưa khớp Phase D cuối.
5. **Thanh bốn tab mới chỉ có ở Home/Topics.** Home tự áp dụng thông số mới; component dùng chung còn thông số cũ. Flashcard/Quiz chưa include/bind navbar, chưa có cơ chế chuyển tab qua guard của phiên học/Quiz.
6. Trong phạm vi tìm kiếm bên dưới **chưa tìm được source Android cuối 30/10**. Có thể tái dựng theo brief và đối chiếu người dùng; không đủ chứng cứ để tuyên bố phục hồi từng byte source cũ.
7. Có **327 file source/resource/test Android hiện diện** trong inventory: 326 tracked và 1 untracked debug manifest có sẵn. **13 file được brief nêu tên chưa có**. Không phải toàn bộ 327 file đều thuộc M16 hoặc đều cần sửa.

**Khuyến nghị:** duyệt phạm vi Phase C trước, nghiệm thu lại bằng Android thật; tiếp tục Phase D rồi đồng bộ navbar một lần. Không dùng demo gpt-taste mới làm đặc tả phục dựng.

## 2. Nguồn và thứ tự đối chiếu

| Nguồn | Vai trò và giới hạn |
|---|---|
| `BiVocal_M16_Recovery_Brief.md`, người dùng cung cấp ngày 01/11 | Đặc tả phục dựng mới nhất cho A/B/C/D; Phase C USER_ACCEPTED_PASS 30/10; Phase D vẫn READY_FOR_ACCEPTANCE. Là bằng chứng lịch sử, không thay source/runtime |
| `AGENTS.md`, `context.md`, `memory.md`, `WORK_STATUS.md`, `ANDROID_CONTEXT.md` | Quy tắc bảo vệ, kiến trúc, bối cảnh và quyết định đã duyệt; ghi chú lịch sử không ghi đè brief mới |
| `.agents/skills/bivoca-quality-handoff/SKILL.md` | Tách kiểm tra static, kết quả lịch sử, chạy mới và nghiệm thu người dùng |
| `README.md`, `PROJECT_CONTEXT.md`, `BACKEND_CONTEXT.md` | Bản đồ tài liệu và giới hạn module; mô tả Room hoặc database cũ chỉ là lịch sử |
| `docs/reports/HOME_APPROVED_PHASE_A_REPORT.md` | Nghiệm thu Home 29/10 và danh sách file bảo vệ |
| `docs/reports/LOGIN_APPROVED_PHASE_B_REPORT.md` | Nghiệm thu Login 29/10; Register/Forgot giữ giao diện đã có |
| Các report `M16_1_*`, `M16_2_*`, `M16_3_*`, `M16_4_TOPICS_FRONTEND_REPORT.md`, `M16_5_FLASHCARD_FRONTEND_REPORT.md` | Báo cáo các bản 25–28/10; dùng làm baseline, không coi là bản C/D cuối 30/10 |
| `docs/design/m16-5-flashcard-review-20261027/` | Còn 5 PNG, HTML và spec cũ; không phải ảnh runtime Phase D 30/10 |
| Source Java/XML/test tại working tree và Git HEAD | Xác định cái thực sự còn; sự hiện diện không đồng nghĩa đáp ứng đặc tả hoặc runtime PASS |

Không tìm thấy các báo cáo gốc riêng ngày 30/10 cho Phase C/D trong project/Downloads đã kiểm tra. Brief là nguồn tổng hợp các hồ sơ đó. Không suy đoán nội dung ngoài brief; cần người dùng bổ sung ảnh/source gốc nếu còn.

SHA-256 của Recovery Brief đã đọc: `587E709EA4DE53B084BFA3AF5B27869B57737C3FEBF87C98FE71D7988C70C1CB`.

### Quy ước

- **ĐÃ CÓ:** tìm thấy triển khai tương ứng trong source; chưa mặc nhiên chứng nhận runtime.
- **THIẾU:** chưa tìm thấy file hoặc hành vi được brief yêu cầu trong phạm vi kiểm kê.
- **KHÁC:** có triển khai cũ nhưng khác đặc tả mới.
- **CẦN XÁC MINH:** cần Emulator, test hoặc bằng chứng gốc; không kết luận theo suy đoán.
- **GIỮ NGUYÊN:** bảo vệ khỏi thay đổi trong phục dựng, trừ phạm vi riêng được duyệt.

Đường dẫn source trong các bảng tính từ gốc project. Số sau dấu `:` là dòng kiểm tra tại HEAD nêu trên.

## 3. Git và bảo toàn công việc hiện có

| Mục | Kết quả kiểm kê |
|---|---|
| Nhánh | `feature/initial-import`; không ở main, không đổi nhánh |
| HEAD / remote-tracking local | `5638fdf6b2ef9290d30dde068a3cf68b3b19e807`; trùng mốc trong brief. Không fetch hoặc chứng nhận lại GitHub trực tuyến |
| Lịch sử local | `5638fdf` Home/Login, `7dabeae` reports/design, `7d7f254` initial import |
| Worktrees | Một worktree duy nhất: project hiện tại |
| Reflog | Chỉ thấy dấu vết clone/remote set-head ngày 01/11; không thấy commit C/D ngày 30/10 |
| Android tracked diff | `git diff HEAD --name-only -- android-app/app/src` rỗng: 326 file tracked Android trong inventory không có tracked change so HEAD |
| Android untracked | `android-app/app/src/debug/AndroidManifest.xml` có sẵn từ sửa quyền LAN local; giữ nguyên, không stage trong commit audit |
| Staging đầu audit | Rỗng |
| Hash bảo toàn | Lưu baseline SHA-256 của 431 file Android/Backend/Admin source và một số cấu hình build ngoài project; đối chiếu lại trước commit |

Các thay đổi đã tồn tại trước Bước 0, **không thuộc commit audit**:

| Trạng thái | Đường dẫn |
|---|---|
| Modified | `WORK_STATUS.md` |
| Modified | `backend/package.json`, `backend/package-lock.json` |
| Modified | `maintenance/local-admin.mjs`, `maintenance/local-admin.vbs` |
| Untracked | `android-app/app/src/debug/` |
| Untracked | `backend/M15_6_APPROVED_README.txt`, `backend/audit/` |
| Untracked | `backend/scripts/BiVocal_M15_6_approved_overrides.json`, `BiVocal_M15_6_prepare_approved.mjs`, `classify-m15-6-recovery.ts`, `classify-m15-7-recovery.ts` |
| Untracked | `docs/reports/BIVOCAL_RECOVERED_STARTUP_2026-11-01.md` |
| Untracked | `maintenance/BACKEND_AUTOSTART.md`, `maintenance/install-backend-autostart.ps1` |

Người dùng bổ sung quyền **commit khi audit xong** trong chat hiện tại. Quyền này được áp dụng riêng cho báo cáo audit và phụ lục inventory; không bao gồm gom source/cấu hình trước đó, triển khai Phase C hoặc push. Không sửa `WORK_STATUS.md` đang modified để tránh trộn bàn giao trước với commit mới.

## 4. Tìm source và bằng chứng cũ

| Nơi đã kiểm tra | Kết quả | Kết luận |
|---|---|---|
| Project, HEAD tree, local branches/worktree/reflog | Không có TopicDetail, TopicPresentation, phase_c/phase_d mới; một worktree | Chưa có nguồn C/D cuối để checkout lại |
| `C:\Projects` | Chỉ thấy `BiVocal-Recovered` | Không thấy bản sao project khác tại đây |
| `C:\Windows.old` | Thư mục tồn tại nhưng có 0 entry, xác minh ngoài sandbox | Không có source để tìm trong thư mục này hiện tại |
| Downloads, workspace chat, project | Tìm tên TopicDetail/TopicPresentation/phase_c/phase_d/LearningBottomNavigationUiTest không thấy source cũ | Không tìm thấy trong phạm vi đã quét; không khẳng định mọi ổ đĩa/cloud đều không còn |
| `BiVocal_M15_6_Approved_Recovery_Pack.zip`, `BiVocal_Recovery_v2.zip` trong Downloads | Mỗi ZIP 4 entry; không có Android M16 liên quan; chỉ đọc directory ZIP, không giải nén/restore | Không phải backup source M16 |
| `docs/design/m16-5-flashcard-review-20261027/` | Còn 5 PNG, một demo HTML và spec | Dùng đối chiếu baseline 27/10; không áp các thông số cũ trái brief |
| `android-app/runtime-evidence/`, `docs/exports/` được các report cũ dẫn | Không tìm thấy bộ evidence runtime tương ứng trong project hiện tại | Không thể chứng nhận pixel-match hoặc tái dùng ảnh nghiệm thu bị mất |

Chưa kiểm tra bản sao trên thiết bị/ổ đĩa không được nêu, cloud version history hoặc file nén khác ngoài các ứng viên tìm thấy. Không có quyền phục hồi/xóa dữ liệu. Nếu nguồn cũ xuất hiện sau audit, đối chiếu hash và diff trước khi quyết định tái dựng; không ghi đè working tree.

## 5. Phase A/B — còn nguyên và phải bảo vệ

| Màn / yêu cầu | Hiện trạng | Bằng chứng / hành động |
|---|---|---|
| Home được nghiệm thu | GIỮ NGUYÊN | `HomeActivity.java`, `HomePresentation.java`, `activity_home.xml`, header/SRS/leaderboard, `home_approved.xml`, drawable `home_approved_*` còn; tracked không khác HEAD. Report Phase A ghi nghiệm thu 29/10 |
| Home brand, slogan, streak/avatar Drawer, Daily Goal, SRS và leaderboard | ĐÃ CÓ, không thiết kế lại | Main manifest:30 dùng `Theme.BiVoca.Home.Approved`; Home bind dữ liệu/CTA và navbar tại `HomeActivity.java:64–120`. Giữ API/count/rank/mapping SRS hiện có |
| Home bốn tab | ĐÃ CÓ, thông số cuối đã áp riêng | `HomeActivity.java:86–109` override nền #0D1320, active/inactive theo approved tokens, icon22dp/chữ12sp. Bước đồng bộ chỉ được chạm phần navbar này và phải chứng minh Home còn nguyên về hình thức/hành vi |
| Login nghiệm thu | GIỮ NGUYÊN | `activity_login.xml`, `login_approved.xml`, field/primary drawables và `LoginApprovedUiTest.java` còn; main manifest:66 dùng `Theme.BiVoca.Login.Approved`. Không sửa Login Java/validation/Google/SessionManager |
| Register/Forgot trong phạm vi đã duyệt | GIỮ NGUYÊN | Activity/layout, `view_auth_brand_header.xml`, `auth_ui.xml` còn; main manifest:54,60 vẫn `Theme.BiVoca.Login.Reference`. Reference là tên theme có chủ ý, không phải chứng cứ mất UI |
| Các phản hồi sau báo cáo cũ | GIỮ quyết định mới | Tiêu đề Register/Forgot có trong `auth_ui.xml:4–6`. Không khôi phục Back trên header hoặc gạch chân slogan từ mô tả M16.1/M16.2 cũ trái memory/brief |
| Test A/B | ĐÃ CÓ; chạy mới NOT_RUN | `HomePresentationTest`, `HomeResponsiveUiTest`, `HomeRuntimeUiTest`, `LoginApprovedUiTest`, `AuthScreensUiTest` hiện diện. Dùng làm regression sau duyệt, không tự logout hoặc gửi register/reset/challenge |
| Nghiệm thu hình ảnh sau cài Windows | CẦN XÁC MINH theo phạm vi thay đổi | Giữ nguyên nghiệm thu lịch sử; audit static không tạo nghiệm thu runtime mới. Header font/inset/IME sẽ hồi quy sau khi thay component chung |

**Ngoại lệ duy nhất dự kiến liên quan Home:** wiring/navbar trong bước đồng bộ đã được brief giới hạn. Chưa có quyền sửa ở Bước 0. Không dùng phục dựng C/D để chỉnh lại Home/Login/Register/Forgot.

## 6. Phase C — bảng đối chiếu đầy đủ

### 6.1 Topic List

| Yêu cầu cuối theo brief | Hiện trạng | Source / việc cần sau duyệt |
|---|---|---|
| Nền #070B12, card #121A26, border #243246, CTA #2457E6, cyan #22C7F8 | KHÁC | `activity_topics.xml:3` nền #090D16; `item_topic.xml:7` card #111726/border#334155; `topics_frontend.xml:28` border cũ; heading `activity_topics.xml:26,35` #38BDF8. CTA Detail chưa có |
| Ba title Kho từ vựng / CHỦ ĐỀ / LOẠI TỪ màu cyan cuối | KHÁC MỘT PHẦN | Text có ở `topics_frontend.xml:3,8,9`. Title `activity_topics.xml:8` kế thừa chữ trắng `Home16Title`; hai heading cyan cũ |
| Search theo tên/code/description; API thật | ĐÃ CÓ | `TopicFilter.java:17–29,41–44`, `TopicRepository.java:23–24,37–57`, `TopicApi.java:12–13` GET catalog |
| Chip Tất cả đứng yên trái; chip khác cuộn ngang | ĐÃ CÓ; CẦN RUNTIME | `activity_topics.xml:14–18`, `TopicsActivity.java:75–94`, assertion `TopicsUiTest.java:99–102` |
| All: hai khung riêng, tối đa4 hàng/frame, inner scroll và outer scroll | ĐÃ CÓ; CẦN RUNTIME | `activity_topics.xml:21–42`, `TopicAdapter.java:65–76`, `TopicsActivity.java:97–111`; không đổi thành một list hoặc bỏ yêu cầu4 hàng |
| Semantic/POS/Life/Work/Nature dùng hết phần còn lại, cuộn nội bộ; riêng All không kéo giãn | THIẾU NHÁNH GIÃN | Mọi filter vẫn dùng cùng `viewportHeight`, cap4 tại `TopicAdapter.java:76` và `TopicsActivity.java:114–119,148–149` |
| POS không giữ vùng Semantic trống | CÓ MỘT PHẦN / KHÁC SPACING | `TopicsActivity.java:144–145` frame rỗng GONE, nhưng POS vẫn có marginTop16dp tại `activity_topics.xml:32`; cần xử lý theo số frame hiện hữu |
| Giữ filter/query qua recreation | ĐÃ CÓ NỀN TẢNG | `TopicsActivity.java:47–50,168–171`; ViewModel giữ request/data. Vị trí cuộn và chiều cao ở font/rotation cần runtime |
| Loading/empty/error/retry | ĐÃ CÓ | `TopicsActivity.java:121–159`, retry:71. Không thay lỗi thật thành mock success |
| Không hardcode số lượng/topic mẫu | ĐÃ CÓ | Adapter bind API tại `TopicAdapter.java:48–52`; mapping category-code trong `TopicFilter` là phân nhóm UI hiện có, không phải seed dữ liệu |
| Chọn card phải vào Topic Detail | KHÁC | `TopicsActivity.java:162–166` mở thẳng Flashcard. Đây là dấu hiệu thiếu bản Phase C cuối |

### 6.2 Topic Detail

| Yêu cầu cuối theo brief | Hiện trạng | Source / việc cần sau duyệt |
|---|---|---|
| Có Activity/VM/PreviewAdapter/layout/resource, không tạo trùng | THIẾU | 4 Java,2 layout,1 resource Phase C và2 Detail tests trong inventory chưa có; `AndroidManifest.xml` chưa đăng ký Detail |
| Header Back/tên/avatar mở Drawer phải | THIẾU MÀN; CÓ COMPONENT | `NavigationDrawerController.java:80–100,135–140,371–374` có Back/title/avatar/END Drawer; tái sử dụng, không tạo controller song song |
| Metadata tên/count/icon/description từ catalog thật | THIẾU PRESENTATION DETAIL | DTO `Topic.java:5–11,30–55` và mapping icon/glyph/color trong `TopicAdapter.java:53–62,78–106` còn; có thể gom mapping vào TopicPresentation mới |
| Học Flashcard mở BROWSE đúng topic | THIẾU CTA; CÓ DESTINATION | Intent code/name và Browse loader còn tại `TopicsActivity.java:163–165`, `FlashcardActivity.java:101,201,232`. Không bật extras NEW/REVIEW cho CTA Browse |
| Quiz nhận topicCode thật; Backend quyết định chất lượng/số câu | THIẾU CTA; CÓ CONTRACT | `QuizActivity.java:35,64,124` có EXTRA_TOPIC_CODE. Không hardcode eligibility hoặc tự bật Quiz cho imported cards |
| Preview GET flashcards?topicCode&page=1&limit=20 | THIẾU CALLER; API/REPO CÓ | `VocabularyApi.java:10–13`, `FlashcardRepository.java:46–74`; dùng getFlashcardPage(code,1,20,...), không endpoint legacy unpaged |
| Tối đa3 thẻ hợp lệ đang hiển thị, không load20.000 vào RAM | THIẾU | Cần bounded request20 và cap displayed3 trong Detail; không sửa paging Browse100 |
| Warning unverified chỉ dựa các thẻ đang hiển thị | THIẾU | DTO `Flashcard.java:17,44–45` có contentStatus/isImportedUnverified. Chỉ xem displayed3, không suy ra trạng thái cả topic hoặc các item20 không hiển thị |
| Null status/empty/error không tự suy đoán warning | THIẾU | Cần test null/empty/error và item unverified nằm ngoài3 displayed; không tạo ví dụ/metadata giả |
| Metadata và preview loading/error/retry độc lập | THIẾU | Detail VM chưa có; lỗi preview phải giữ metadata và CTA |
| Stale callbacks/cancellation/recreation | THIẾU Ở DETAIL | Có pattern từ `TopicsViewModel.java:27–29,52–75,81–84`; áp riêng cho metadata/preview, giữ state qua xoay |
| Tab/Drawer Chủ đề từ Detail về List | THIẾU CASE DETAIL | `NavigationRoute.java` chưa có Detail; policy:9–15 chỉ alias FLASHCARDS→TOPICS. `LearningBottomNavigation.java:42–48` no-op nếu current==destination: không được bind Detail như chính List rồi khiến tab không quay về |

**Lưu ý contract khi thiết kế:** `FlashcardRepository.java:104–111` hiện kiểm tra validity toàn trang; một item malformed có thể làm cả request lỗi. Không âm thầm sửa repository để đáp ứng preview. Xác định xử lý hợp lệ bằng test/contract và trình duyệt riêng nếu cần thay tầng dữ liệu.

## 7. Phase D — bảng đối chiếu đầy đủ

| Yêu cầu cuối theo brief | Hiện trạng | Source / việc cần sau duyệt |
|---|---|---|
| Java/XML, MVVM, Online-Only | ĐÃ CÓ, GIỮ | Activity/VM/repository Retrofit; không đưa Room lại |
| BROWSE / NEW_TODAY / REVIEW_TODAY | ĐÃ CÓ | `FlashcardActivity.java:40–44,91–95,162–163,230–232`, VM:78–123 |
| Settings/Vietnamese-first | ĐÃ CÓ | Activity:235–247,477–494,637–669; giữ text scaling, hướng mặt trước và auto speech |
| TTS từ và câu ví dụ | ĐÃ CÓ NỀN TẢNG; CẦN XÁC MINH ÂM THANH | Activity:166–169,502–505,564–635,673–693. Giọng UK fallback US, có listener/stop/shutdown. Phản hồi người dùng về âm khựng ở lượt trước không phải bằng chứng mất Phase D; xử lý TTS riêng nếu cần |
| Favorite/Quiz menu | ĐÃ CÓ; KHÔNG BẤM GHI | Activity:179–194,304–335; Favorite là API ghi thật, không dùng tạo ảnh |
| Browse paging tối đa100 | ĐÃ CÓ | VM:44,190–199,214–231; repository:46–73 |
| Hai mặt cùng viewport/vị trí flip, nội dung dài cuộn | ĐÃ CÓ NỀN TẢNG; CẦN RUNTIME | Activity:250–301; XML:21–25,64,75–79. Browse back giữ chỗ prev/next bằng INVISIBLE |
| Title và progress lấy số thực | ĐÃ CÓ | Activity:101–105,412–424,474–476; không hardcode sample |
| Progress4dp | KHÁC:10dp | `activity_flashcard.xml:18`, dùng Home drawable; cần phase_d_progress riêng |
| Front word≈36sp / IPA16sp | KHÁC:34sp /17sp | XML:26–27 và Java override `applyLearningTextSize`:637–640. Chỉ sửa XML sẽ không đủ |
| POS/CEFR/contentStatus API | ĐÃ CÓ | Activity:481–491; thiếu thì ẩn; Vietnamese-first chủ ý ẩn front IPA/metadata theo setting |
| Audio vùng chạm tối thiểu48dp | ĐÃ CÓ | `flashcard_frontend.xml:22`; cần font/inset runtime |
| Back word30sp / meaning24sp / examples16/14sp | KHÁC:26/20/14/13 | XML:35,44,55,58 và Java:641–646 |
| Metadata bên phải nghĩa | ĐÃ CÓ | XML:41–49 hai cột3:2; Activity:489–491 optional metadata |
| Ví dụ không nền/viền; không bịa EN/VI | ĐÃ CÓ | XML:51–59 trong suốt, padding14; Activity:495–497 bind dữ liệu thật |
| Flip xanh#2457E6 | KHÁC | Fc16Primary dùng gradient Home cũ; `flashcard_frontend.xml:21`, `home16_primary.xml:1` |
| Quay mặt trước dùng viền nhẹ | THIẾU | Activity:265 chỉ đổi text cùng primary style; chưa chuyển visual state |
| Chưa nhớ/Đã nhớ bên dưới Flip | ĐÃ CÓ | XML:75–87 đúng thứ tự |
| Bỏ outer rating panel | KHÁC | XML:80–86 vẫn `fc16_review`, padding14 và câu hỏi trong panel |
| Pending khóa các nút cần thiết | CÓ MỘT PHẦN | Activity:235–236,431–447,498–505,530–543 khóa rating/flip/prev-next/Favorite/Quiz; audio phụ thuộc ttsReady; cần xét navbar mới và không suy rằng mọi đường rời đã được guard |
| Pending làm mờ + trạng thái rõ | KHÁC MỘT PHẦN | Có text pending, alpha explicit mới ở một số nút; cần đồng bộ visual alpha/disabled theo saving, không tạo pending giả trong runtime vận hành |
| Chỉ “Đã lưu kết quả” khi Backend ACK | THIẾU UI SUCCESS ACK | Không có chuỗi/binding này. `fc16_saved` là lời giải thích ACK, chưa dùng; Activity:379–385 và VM:138–149 là điểm ACK cần ánh xạ presentation |
| Lỗi giữ card/processed và cho retry | ĐÃ CÓ CƠ SỞ | VM:126–156 không dequeue/count trước ACK; Browse lỗi Toast giữ card tại Activity:388–393. Phân biệt queue inline error với Browse error |
| Loading, lỗi tải, Retry/reconnect, empty | ĐÃ CÓ | Activity:198–202,398–411; XML:66–70; không fake offline data |
| Completion dùng processed/total thật | ĐÃ CÓ | Activity:412–429; VM:143–149; `view_flashcard_complete.xml:8–16` |
| Initial queue empty không giả completion | ĐÃ CÓ | Điều kiện total>0, reviewCompleted, list rỗng; test queue empty hiện hữu |
| Không tính XP/tiến độ giả | ĐÃ CÓ CƠ SỞ | Completion dùng processed/fraction, không số giả; chưa chạy E2E ghi trong audit |
| BROWSE có thể POST SRS | PHẢI BẢO VỆ | Activity:361–395, `ProgressApi.java:34–37`. Chỉ xem/lật/paging mới thuộc phạm vi GET-only; không coi mọi Browse đều read-only |
| Không sửa VM/repo/API/Auth/SRS chỉ để khớp UI | GIỮ NGUYÊN | Chỉ đề xuất UI/presentation wiring; nghi vấn logic bên dưới tách khỏi phạm vi phục dựng tự động |

### 7.1 Nghi vấn cũ cần test/duyệt riêng, chưa kết luận là lỗi bị mất

| Nghi vấn từ source | Giới hạn kết luận |
|---|---|
| Browse ACK ở cuối page100 chỉ auto-next khi index<page-size-1 (`FlashcardActivity.java:382–384`) | Chưa rõ có chủ ý hay yêu cầu thay hành vi; không tự sửa paging/VM |
| Queue response.flashcardId không khớp thì return trước clear pending (`FlashcardViewModel.java:138–146`) | Cần test response sai/stale; không kết luận Backend đang trả sai, không gửi POST để thử trên recovery |
| Browse pending là field Activity/callback giữ instance cũ (`:67,373`) | Cần kiểm tra recreation lúc in-flight trong test cô lập; không đồng nhất với queue pending được VM giữ |
| Drawer chuyển route trực tiếp, không qua custom Back guard | Có rủi ro rời phiên; brief bắt buộc guard tab mới, còn mở rộng Drawer phải trình phạm vi rõ ràng |

Các mục này không phải giấy phép sửa ViewModel hoặc nghiệp vụ. Nếu thật sự cần sửa, trình file, ca tái hiện và phê duyệt bổ sung trước.

## 8. Đồng bộ bốn tab — đối chiếu riêng

| Yêu cầu cuối | Hiện trạng | Phạm vi đồng bộ sau duyệt |
|---|---|---|
| Trang chủ / Chủ đề / Ôn tập / Trắc nghiệm, dùng component chung | CÓ ở Home và Topics | `LearningBottomNavigation.java:25–27`, layout chung; không tạo mỗi màn một thanh mới |
| Nền#0D1320, active#22C7F8, inactive#B7C3D4 | KHÁC ở component chung; Home đúng nhờ override | Shared layout nền#090D16, controller:22 dùng#38BDF8/#94A3B8. Gom tokens/presentation đúng một lần |
| Icon22dp/chữ12sp/minHeight64dp/4 vùng bằng nhau | CÓ min64 và weight1; KHÁC icon20/chữ10 ở shared | `view_learning_bottom_navigation.xml:2–3`, `home_frontend.xml:84–94`. Home hiện override22/12 |
| Nav fixed bottom, content cuộn phía trên | CÓ Home/Topics; THIẾU Flashcard/Quiz | Flashcard root toàn NestedScrollView, Quiz chưa có include. Thêm container đúng inset sau duyệt |
| Browse chọn Chủ đề; NEW/REVIEW chọn Ôn tập | THIẾU NAV FLASHCARD | Phân biệt mode với selected-tab và current-screen, không dùng selected-tab để no-op sai đích |
| Detail chọn Chủ đề nhưng bấm tab/Drawer quay List | THIẾU ROUTE/POLICY | Thêm case Detail vào NavigationRoute/Policy/controller nếu dùng route riêng; test quay list không tạo detail mới |
| Flashcard không rời qua tab lúc saving | THIẾU HOOK TAB | Shared controller hiện startActivity trực tiếp:42–49. Tái sử dụng pending guard Activity:154–161 |
| Quiz submitting→block, đã trả lời/chưa xong→confirm | CÓ QuizBackPolicy và handler Back; THIẾU WIRING TAB | `QuizBackPolicy.java:9–17`, `QuizActivity.java:274–294`. Tab phải dùng chính policy, giữ destination qua confirm, không đổi chấm điểm |
| Một component dùng chung, Home chỉ navbar | CÓ NỀN TẢNG | Loại bỏ override Home trùng lặp chỉ nếu kết quả bảo toàn pixel/hành vi; không chỉnh các section Home |
| Test navbar | THIẾU TEST MỚI | `LearningBottomNavigationUiTest.java` chưa có; NavigationRoutePolicyTest cũ chỉ FLASHCARDS/REVIEW/top-level |

## 9. Inventory theo file và danh sách thay đổi dự kiến

Phụ lục [M16_RECOVERY_FILE_INVENTORY.csv](M16_RECOVERY_FILE_INVENTORY.csv) có **340 dòng**: 327 file hiện hữu với SHA-256 và 13 file thiếu theo tên brief. `PRESENT_MATCHES_HEAD_TRACKED` chỉ chứng minh tracked baseline, không có nghĩa khớp Phase C/D cuối. Đường dẫn cha của file thiếu là **đề xuất**, không phải source gốc đã thu hồi.

### 9.1 Giữ nguyên

- Home ngoài navbar: `HomeActivity.java`, `HomePresentation.java`, `activity_home.xml`, header variants, SRS/leaderboard và approved resources.
- Toàn bộ Login/Register/Forgot Java/XML/theme/drawables/auth tests hiện tại; không đổi Auth/Google/session.
- `FlashcardViewModel.java`, `TopicsViewModel.java`, `TopicFilter.java`, repository/API/DTO hiện có, trừ phê duyệt riêng sau khi có lỗi thật.
- Backend, Admin, PostgreSQL, backup, các script M15.x và file chưa commit/untracked trước audit.
- Debug manifest quyền mạng nội bộ có sẵn; không gỡ để làm source giống HEAD.

### 9.2 Phase C — phạm vi đề nghị duyệt trước

| Hành động dự kiến | File / nhóm file | Mục đích |
|---|---|---|
| Thêm | `ui/topics/TopicPresentation.java` | Tái dùng icon/glyph/color và metadata presentation; không đổi dữ liệu topic |
| Thêm | `ui/topics/TopicDetailActivity.java` | Detail header/Drawer/CTA, bind hai state độc lập |
| Thêm | `ui/topics/TopicDetailViewModel.java` | Catalog metadata + bounded preview, cancellation/versioning/recreation |
| Thêm | `ui/topics/TopicPreviewAdapter.java` | Tối đa3 displayed cards; bind trạng thái đúng metadata |
| Thêm | `res/layout/activity_topic_detail.xml`, `item_topic_preview.xml` | UI Detail/preview theo đặc tả cũ được phục dựng |
| Thêm | `res/values/topics_phase_c.xml` và drawable thực sự cần | Tokens/styles/strings riêng tránh thay A/B |
| Chỉnh tối thiểu | `ui/topics/TopicsActivity.java`, `TopicAdapter.java` | List→Detail; All4 rows, filtered viewport-expand; shared mapping |
| Chỉnh | `res/layout/activity_topics.xml`, `item_topic.xml`, `res/values/topics_frontend.xml`, `res/color/topics16_chip_background.xml`, `topics16_chip_text.xml` | Palette/type/spacing/state; khungPOS không để khoảng trốngSemantic |
| Chỉnh entry | `app/src/main/AndroidManifest.xml` | Đăng ký Detail, không đổi entries/theme A/B |
| Chỉnh có phối hợp nav | `NavigationRoute.java`, `NavigationRoutePolicy.java`, `NavigationDrawerController.java`, `LearningBottomNavigation.java` | Selected TOPICS khác current Detail; bấm đúng đíchList |
| Thêm test | `TopicDetailViewModelTest.java`, `TopicDetailUiTest.java` | Request bounded, independent state, stale/retry, warning displayed-only, CTA/nav |
| Mở rộng test | `TopicsUiTest.java`, `NavigationRoutePolicyTest.java` | Filter expansion, recreation và List→Detail→Browse; thay assertion cũ mở thẳngFlashcard |

Không bắt buộc thêm mọi drawable nếu component hiện có đã đáp ứng. Trước sửa phải rà soát trùng tên và chốt hunk/file list; bảng là phạm vi đề xuất, chưa được triển khai. Namespace Java đầy đủ nằm dưới `android-app/app/src/main/java/com/example/bivoca/`; test unit/androidTest dùng source set tương ứng.

### 9.3 Phase D — chỉ làm sau Phase C được duyệt

| Hành động dự kiến | File | Mục đích |
|---|---|---|
| Chỉnh presentation | `ui/flashcard/FlashcardActivity.java` | Text scaling36/16/30/24/16/14, face-style, pending/ACK/error đúng state; không đổi semantics học |
| Chỉnh layout | `res/layout/activity_flashcard.xml` | Progress4dp, rating không outer panel, cấu trúc content-scroll/nav-fixed, touch/inset |
| Chỉnh nếu đối chiếu cần | `res/layout/view_flashcard_complete.xml`, `res/values/flashcard_frontend.xml` | Completion và strings/status khớp spec; dùng processed/total thật |
| Thêm | `res/drawable/phase_d_progress.xml`, `phase_d_status_check.xml`, `phase_d_status_error.xml` | Resource Phase D được brief nêu; không tái dùng gradientHome trái đặc tả |
| Chỉnh/thêm drawable có giới hạn | Flip solid blue và return outline riêng Phase D | Không sửa `home16_primary` dùng chung với màn đã nghiệm thu |
| Mở rộng test | `FlashcardFrontendUiTest.java` | Type/progress/panel/face bounds/pending presentation; không click rating/Favorite/Quiz submit trên recovery |
| Giữ baseline | FlashcardViewModel/repository/API/Auth/SRS/XP | Các nghi vấn ngoàiUI phải trình duyệt riêng |

### 9.4 Đồng bộ điều hướng sau Phase D

| File | Phạm vi duy nhất |
|---|---|
| `LearningBottomNavigation.java` | Tokens/style chung, current-screen khác selected-tab, guard/veto điều hướng và destination sau confirm |
| `view_learning_bottom_navigation.xml` | Nền/icon/label/equal hit areas/fixed layout |
| `home_frontend.xml` hoặc resource navbar riêng | Styles22dp/12sp/min64; tránh sửa các style Home khác |
| `QuizActivity.java`, `activity_quiz.xml` | Include/bind navbar và tái sử dụng QuizBackPolicy; không đổi submit/grade |
| `FlashcardActivity.java`, `activity_flashcard.xml` | Bind theo3mode, chặn tab khi saving |
| `HomeActivity.java` | Chỉ wiring/navbar, giữ toàn bộ nội dung Home và callbacks khác |
| `TopicPresentation.java`, `TopicsActivity.java`, `TopicDetailActivity.java` nếu cần | Chỉ đồng nhất navbar và selected-route; không áp palette navbar cũ PhaseC |
| `LearningBottomNavigationUiTest.java` mới; `NavigationRoutePolicyTest.java` | Nav/guard/state/font/landscape và hồi quy A/B |

## 10. Kế hoạch và cổng phê duyệt

### Giai đoạn C — Topic List rồi Detail

1. Người dùng duyệt bảng9.2 và ảnh/spec Phase C; giữ A/B và lớp dữ liệu làm baseline bất biến.
2. Chốt routing/presentation/topic metadata và state contract Detail trước khi viết; dùng API/repository hiện có, không thêm dependency.
3. Phục dựng palette/title/filter-height và routingList→Detail; All vẫn2frame×4rows; POS bỏ gap.
4. Detail phân tách metadata/preview state; request page1/limit20; cap3 valid displayed; warning dựa displayed-only; stale callbacks/retry/recreation.
5. Unit/build/lint + runtime GET-only có guard, ảnh portrait/landscape/font1.0/1.3; so spec gốc, không dùng ảnh demo mới.
6. Người dùng nghiệm thu mới. Không tự gọi source mới là USER_ACCEPTED_PASS chỉ vì lịch sửPhaseC cũ đã PASS.

### Giai đoạn D — Flashcard UI và trạng thái

1. Sau duyệt riêng: sửa đúng thông số ở bảng7, không chạm nghiệp vụ. Kiểm tra cả XML và Java text scaling.
2. Giữ ba mode, Settings/Vietnamese-first/TTS/menu/paging; face flip bounds đồng nhất, example trong suốt, rating dướiflip.
3. Ánh xạ pending/ACK/error/completion từ state có thật; không tự tăng processed/XP hoặc dequeue khi lỗi.
4. Bằng chứng trạng thái ghi: unit/layout fixtures tách biệt; E2E ghi thật chỉ khi người dùng cho phép database test riêng. Recovery DB tuyệt đối không dùng tạo ảnh pending/completion.
5. PhaseD tối đa READY_FOR_ACCEPTANCE trước khi người dùng xem và chấp nhận.

### Giai đoạn navbar — một lần đồng bộ chung

1. Chốt final tokens theo brief mới, không dùng các thông số27/10.
2. Reuse layout/controller; đúng selected mode, Detail→List, guard Flashcard/Quiz.
3. Hồi quy Home có navbar giống bản đã nghiệm thu, Login/Register/Forgot không đổi hash; không thiết kế lại.
4. Kiểm tra back stack, duplicate Activity, Drawer, rotation/font/IME/insets và4equal hit areas. Gửi báo cáo mới để người dùng nghiệm thu.

## 11. Phương án kiểm thử Emulator sau phê duyệt

| Nhóm kiểm tra | Thao tác dự kiến | Bảo vệ dữ liệu / bằng chứng |
|---|---|---|
| Preflight | Đọc wrapper/module/variant; adb devices; kiểm session, permission LAN và health | Không logout/clear/reinstall để né lỗi. Emulator hiện thấy emulator-5554/API37/font1.0; không coi cấu hình này chứng minh UI PASS |
| Build/unit/lint | `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` | Chỉ sau duyệt triển khai; ghi số test/executed/up-to-date thực tế, không ép84/91. Không chạy clean |
| Instrumentation guard | Xác minh leaveApksInstalledAfterRun=true, keepInstalledApks=true, uninstallIncompatibleApks=false | Gradle property leaveApksInstalledAfterRun hiện có. File init guard runtime-evidence cũ không còn: phải xác minh lại task config, không tự giả là đã an toàn |
| Cài APK | `adb -s emulator-5554 install -r ...` | Giữ data/session; không uninstall, pm clear hoặc wipe AVD |
| PhaseC unit | Bounded20/cap3, null/empty/error, unverified ngoài/giữa displayed3, stale/cancel/retry, retained state | Test doubles/fixtures chỉ trong test, không thay runtimeAPI hoặc chụp giả trạng thái production |
| PhaseC runtime | Catalog thật, search/filter/All-fixed, selected-height, POS-gap, Detail metadata/preview/retry, Back/avatar/Drawer, Detail→Browse | Chỉ GET; không bấm Quiz submit/Favorite/rating. Không suy eligibility từ sample preview |
| PhaseD Browse runtime | Hai mặt, long text/optional metadata/examples, progress4dp, 100/page, prev-next, TTS, Vietnamese-first | Audio chất lượng cần nghe thật; click/nút enabled không đủ. Browse có POST rating nên chỉ xem/lật/paging/audio |
| NEW/REVIEW runtime | Chỉ tải/xem/lật/quay lại và initial-empty thật nếu có | Không tạo review để làm queue rỗng hoặc completion |
| Pending/ACK/error/completion | Unit/state/layout trước; delayed/error/ACK integration trên DB test được duyệt sau | Không fake ACK vào app vận hành; không POST trên bivoca_m15_7_recovered. Nếu chưa có DB test, ghi NOT_RUN/BLOCKED cho E2E ghi |
| Navbar/QuizBackPolicy | Mode→selected; Detail→List; pending blocks; Quiz submitting/answered guards | Cases cần answered/submitting dùng test cô lập, không trả lời Quiz recovery để tạo state |
| RegressionA/B | Source hashes, Home sections, Login header/form, Register/Forgot local validation và Back/IME | Không register/reset/Google challenge, không tự logout phiên hiện có. Cần người dùng chủ động mở Login nếu đang đăng nhập |
| Display matrix | Ít nhất320dp/screen hiện có/landscape; font1.0 và1.3, longtext/insets/nav | Phân biệt inflate/measure với AVD nhỏ thật. Không đổi AVD size/density để làm đẹp ảnh; font tạm đổi phải ghi/khôi phục |

Các bộ test còn trên source: TopicFilterTest, TopicsViewModelTest, NavigationRoutePolicyTest, QuizBackPolicyTest, FlashcardViewModelTest, DueReviewViewModelTest, TopicsUiTest, FlashcardFrontendUiTest, Home/Login/Auth tests. Test Topics hiện yêu cầu card mở thẳngFlashcard: cần cập nhật cùng hợp đồngDetail, không dùng test cũ PASS làm bằng chứngPhaseC mới.

## 12. Kết quả kiểm tra trong chính Bước 0

| Kiểm tra | Trạng thái | Giới hạn |
|---|---|---|
| Đọc brief / đối chiếu source A–D / inventory và tìm recovery | PASS cho việc kiểm kê | Không phải PASS runtime/feature |
| Git HEAD/status/worktree/reflog và Android tracked diff | PASS cho việc xác minh baseline | Chưa fetch GitHub; nguồn local khớp mốc brief |
| Bảo toàn source sau audit | PASS | So lại SHA-256 của 431 file baseline: 0 file đổi hoặc mất. Inventory340 đường dẫn duy nhất,327 hiện diện/13 thiếu |
| A/B source hiện diện/không tracked diff | PASS static | Nghiệm thu29/10 giữ nguyên; không tái nghiệm thu hình ảnh |
| Emulator inventory | PASS quan sát | emulator-5554/API37/font1.0; đang ở FlashcardActivity. Không thao tác UI học |
| Build/unit/lint/instrumentation trong Bước0 | NOT_RUN | Audit chỉ đọc; không lấy84/91 hoặc logbuild trước làm kết quả mới |
| Chạy API/SQL hoặc kiểm lại20.000/38 trong Bước0 | NOT_RUN | Số lượng là invariant từ brief/lượt phục dựng trước, không có truy vấn mới trong audit |
| Review/Quiz/Favorite POST | NOT_RUN / KHÔNG THỰC HIỆN | Không dùng recovery DB tạo trạng thái kiểm thử |
| Migration/seed/import/restore/reset/M15.7 Apply | KHÔNG THỰC HIỆN | Không có quyền trong audit |
| Phục dựng source PhaseC/D | NOT_RUN | Chờ người dùng duyệt danh sách thay đổi |

## 13. Rủi ro và điểm cần duyệt

- **Mất source/ảnh cuối:** không thể cam kết pixel-match hoặc byte-for-byte từ báo cáo chữ. Nếu còn ảnh30/10, dùng làm chuẩn trước khi triển khai; không tự chọn phong cách mới.
- **Tài liệu khác tuổi:** spec27/10 nói khôngnavbar/progress8dp; report28/10progress10dp; brief cuối4dp+cónavbar. Ưu tiên brief mới; không phục dựng ngược bản cũ.
- **Home dùng component chung:** chỉnh navbar/style có thể làm Home khác dù không đổi layoutHome. Bắt buộc phạm vi navbar và regressionA/B; không sửa global theme.
- **Preview và eligibility:** không suy trạng thái toàntopic từ preview; không sửa15.000IMPORTED_UNVERIFIED hoặc cho Quiz chỉ để nút trông hoạt động.
- **Ghi dữ liệu do thao tác UI:** Browse rating, Favorite và Quiz answer là writes. Chỉ mở menu/điều hướng an toàn chưa đủ để gọi toàntest read-only; phải review từng test trước chạy.
- **Async/pending/recreation:** cần test riêng stale response và Browse pending; không sửa VM/Auth/API dưới danh nghĩa UI nếu chưa có phê duyệt bổ sung.
- **Emulator sau reinstall:** quyền LAN debug hiện có phải giữ; TTS/âm thanh/giọngoffline là hạng mục runtime riêng, không thay engine/cấu hình trong audit.
- **Git:** local có nhiều thay đổi phục dựng trước; commit audit chỉ đúng2 file tài liệu. Không reset/clean, không add-A, không merge/rebase, không push.

## 14. Phạm vi bàn giao và điểm dừng

File do Bước0 tạo để bàn giao/commit:

1. `docs/reports/M16_RECOVERY_AUDIT.md` — báo cáo này.
2. `docs/reports/M16_RECOVERY_FILE_INVENTORY.csv` — inventory340dòng, SHA-256 các file hiện hữu và13tên thiếu.

Baseline/integrity evidence được lưu ngoài project trong workspace chat, không stage. Source/Backend/Admin/database/các file chưa commit được giữ nguyên. Không cài dependency, không chạy bài test có ghi và không thiết kế lại A/B.

**DỪNG SAU AUDIT VÀ COMMIT TÀI LIỆU ĐƯỢC NGƯỜI DÙNG CHO PHÉP. CHỜ DUYỆT BẢNG9.2 TRƯỚC KHI TRIỂN KHAI PHASE C. KHÔNG PUSH KHI CHƯA CÓ YÊU CẦU.**
