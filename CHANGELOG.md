# Thay đổi

## 0.3.8 — 2026-10-06

- Tăng chiều cao vùng điều khiển giữa để tận dụng tốt phần trống màn hình.
- Card D-pad tăng từ khoảng **204dp lên 254dp**; vùng chứa tổng khoảng **260dp**.
- Touchpad khi bật tăng lên khoảng **220dp**.
- Vùng bấm mũi tên trên/dưới cao hơn và hai vùng trái/phải cũng được kéo dài nhẹ để cân với card mới.
- Giữ nguyên hàng icon trên và hàng Back / Vol− / Mute / Vol+ phía dưới.


## 0.3.7 — 2026-10-06

- Làm lại khu điều hướng thành **một card thống nhất** thay vì nhiều khối rời.
- Bốn mũi tên chỉ hiển thị icon nhưng vẫn giữ vùng bấm lớn; **OK** vẫn là nút chính ở giữa.
- Touchpad khi kích hoạt dùng cùng phong cách card, full ngang và gần như không có chữ.
- Hàng cuối chỉ còn **Back / Vol− / Mute / Vol+** dạng icon; bỏ badge `+`, `−` và số âm lượng gây rối.
- Nút Mute đổi trạng thái bằng màu active thay vì badge.
- Sửa lỗi bấm tắt TV nhưng remote vẫn sáng và chấm vẫn xanh: app chuyển ngay sang `SHUTTING_DOWN`, khóa remote và đổi chấm sang xám ngay khi bấm tắt.
- Sau khi gửi lệnh tắt thành công, app chuyển Offline, reset volume/text-focus và tạm chặn reconnect tự động khoảng 20 giây để socket cũ không kéo trạng thái xanh trở lại.
- Nếu lệnh tắt thất bại nhưng WebSocket vẫn còn, app khôi phục Connected để tiếp tục điều khiển.
- Bấm Bật, Thử lại hoặc Ghép đôi lại sẽ xóa khoảng chặn reconnect ngay.


## 0.3.6 — 2026-10-04

- Gom **YouTube / Tìm / Home** thành một hàng duy nhất.
- Ba thao tác nhanh chỉ hiển thị **icon**, không dùng màu đỏ/xanh riêng cho YouTube hoặc Home; cùng một phong cách tonal trung tính.
- Icon YouTube đổi sang dạng màn hình video có nút play để vẫn dễ nhận biết dù không có chữ.
- D-pad giữa đổi sang cấu trúc gọn: trụ dọc **↑ / OK / ↓**, hai vùng **← / →** lớn ở hai bên.
- Không còn vòng tròn trắng/cánh hoa lớn quanh D-pad.
- Hai nút **⌨ / 🖱** tiếp tục nằm ở góc dưới-phải vùng điều hướng.
- Gom hàng cuối thành **← / 🔉− / 🔇 / 🔊+**, bỏ các hàng Back và âm lượng dài trước đó.
- Mức âm lượng hiện tại được đưa vào badge nhỏ trên nút mute khi TV cung cấp giá trị.
- Bố cục chính ngắn hơn đáng kể, gần cấu trúc remote cố định và giảm nhu cầu cuộn màn hình.


## 0.3.5 — 2026-10-04

- Bỏ tên ứng dụng khỏi header màn hình remote để tiết kiệm chiều cao và giảm nhiễu.
- Đưa nút **Cài đặt** sang góc trái; giữ chấm trạng thái cạnh đó và nút nguồn lớn ở bên phải.
- Chế độ chuột dùng **touchpad full chiều ngang**, dạng chữ nhật bo góc thay vì vòng tròn nhỏ.
- Hai icon **bàn phím / chuột** được đặt nổi ở góc dưới-phải vùng điều khiển, không còn chiếm một hàng riêng.
- D-pad vẫn xoay tròn quanh OK nhưng bốn hướng dùng vùng bấm lớn kéo tỏa ra ngoài: trên/dưới 112×78dp, trái/phải 78×112dp.
- Icon mũi tên lớn hơn, hit target lớn hơn phần icon để dễ bấm trúng.
- Thu gọn nhẹ các nút phụ nhưng vẫn giữ vùng chạm từ khoảng 52dp trở lên để giảm nhu cầu cuộn.


## 0.3.4 — 2026-10-04

- Tăng tốc trạng thái khi mở lại app: dữ liệu TV đã lưu được dùng ngay và trạng thái chuyển **đang kiểm tra** thay vì đứng xám.
- Với TV đã ghép, kết nối tới IP gần nhất và tìm TV theo UID/SSDP được chạy **song song**.
- Nếu WebSocket IP cũ kết nối trước, app hủy quét SSDP và chuyển xanh ngay.
- Nếu SSDP tìm thấy cùng TV ở IP mới trước, app hủy kết nối IP cũ, lưu IP mới và kết nối lại ngay.
- Ghi lại thời điểm kết nối thành công gần nhất; nếu TV vừa dùng gần đây, direct-IP được ưu tiên 350 ms trước khi bắt đầu SSDP để giảm multicast không cần thiết.
- Chấm xanh vẫn chỉ xuất hiện sau khi webOS đăng ký WebSocket thành công; app không giả trạng thái kết nối từ cache.
- Dữ liệu cấu hình cũ không có thời điểm kết nối vẫn đọc bình thường.


## 0.3.3 — 2026-10-04

- Sửa trường hợp TV đã ghép từ trước nhưng sau một thời gian app không tự nhận ra lại cho đến khi người dùng vào **Tìm tivi**.
- Khi kết nối tới IP đã lưu thất bại, app tự quét SSDP theo **UID của TV đã ghép**, lấy IP mới rồi kết nối lại mà không cần người dùng chọn TV thủ công.
- Nếu IP thay đổi do DHCP, app cập nhật và lưu IP mới ngay nhưng **giữ nguyên client-key, chứng chỉ TLS, MAC Wake-on-LAN và các tùy chọn**.
- Tìm lại TV ngay sau lần kết nối mạng thất bại đầu tiên, sau đó định kỳ thử lại trong reconnect loop thay vì chờ người dùng thao tác.
- Cơ chế tìm lại UID cũng được dùng trong giai đoạn TV đang khởi động sau Wake-on-LAN.
- Chuẩn hóa các dạng UID phổ biến của LG/SSDP: `uuid:...`, `urn:uuid:...`, USN có `::urn:lge-com:...` và khác biệt chữ hoa/thường.
- Khi chọn lại TV từ màn hình tìm kiếm, UID tương đương vẫn được nhận là TV cũ nên giữ khóa ghép đôi.
- Thêm kiểm thử nhận dạng cùng thiết bị qua nhiều định dạng UID.


## 0.3.2 — 2026-10-03

- Đổi cụm 4 hướng + OK sang **D-pad tròn** giống remote vật lý.
- Thêm hai icon ở góc dưới-phải khu điều hướng: **bàn phím** và **chuột**.
- Chế độ chuột biến vùng D-pad thành touchpad: rê ngón tay để di chuyển con trỏ, chạm để click.
- Dùng hàng đợi pointer giới hạn để thao tác rê nhanh không tạo vô hạn coroutine/request.
- Subscribe `com.webos.service.ime/registerRemoteKeyboard` để biết khi TV đang focus ô nhập.
- Khi TV focus ô nhập, app tự mở vùng nhập chữ và gọi bàn phím Android; nội dung được gửi bằng `insertText` với `replace=true` để phù hợp cả gõ tiếng Việt/chỉnh sửa nội dung.
- Thêm nút Enter trên vùng nhập và hỗ trợ phím Done của bàn phím Android.
- Bổ sung quyền webOS `CONTROL_INPUT_TEXT`; TV đã ghép từ bản cũ có thể cần **Ghép đôi lại** một lần để cấp quyền mới.
- Thêm kiểm thử cho trạng thái keyboard focus và frame pointer move/click.


## 0.3.1 — 2026-10-03

- Làm rõ hai nút âm lượng bằng icon **loa + dấu “−”** và **loa + dấu “+”**.
- Tăng kích thước icon loa từ 29dp lên **34dp**.
- Dấu cộng/trừ được đặt trong badge tròn nhỏ ngay trên icon để dễ nhận biết bằng mắt.
- Giữ nguyên chữ “Giảm tiếng” / “Tăng tiếng” để người lớn tuổi vẫn dễ hiểu.


## 0.3.0 — 2026-10-02

- Làm lại tìm kiếm bằng giọng nói theo hướng **YouTube-first**.
- Sau khi Android nhận câu nói, app mở thẳng YouTube với deep-link `https://www.youtube.com/tv?q=...` qua `system.launcher/launch`.
- Payload gửi đồng thời `contentId` và `params.contentTarget` để tương thích nhiều thế hệ firmware/webOS YouTube.
- Không còn phụ thuộc TV đang focus đúng ô nhập, nên có thể tìm từ màn hình Home hoặc khi YouTube chưa mở.
- Bỏ `CONTROL_INPUT_TEXT` khỏi manifest ghép đôi cho chức năng này; chỉ dùng quyền `LAUNCH` đã có từ đầu.
- Bỏ yêu cầu ưu tiên nhận dạng offline để tránh trường hợp máy không có gói tiếng Việt offline và trả về không có kết quả.
- Đổi nhãn nút thành **Tìm YouTube**.
- Thêm kiểm thử mã hóa tiếng Việt và payload deep-link YouTube.


## 0.2.9 — 2026-10-02

- Tăng nút nguồn trên header từ 48dp lên **64dp** để dễ bấm hơn cho người lớn tuổi.
- Tăng icon nguồn từ 29dp lên **36dp**.
- Thêm nền tròn đỏ nhạt giúp nút nguồn nổi bật hơn mà không làm thay đổi cơ chế một chạm.
- Khi đang kết nối/ghép đôi và nút tạm khóa, icon chuyển xám để dễ nhận biết.


## 0.2.8 — 2026-10-02

- Sửa trường hợp Wake-on-LAN đã đánh thức TV nhưng app chuyển chấm xám trước khi webOS sẵn sàng nhận kết nối.
- Sau khi bấm bật, app giữ trạng thái **đang chờ tivi bật** và tự reconnect trong tối đa khoảng 90 giây.
- Trong giai đoạn boot, timeout mỗi lần thử ngắn hơn và khoảng nghỉ chỉ khoảng 1.5–3 giây; không dùng backoff 4/8/15/30 giây như reconnect thông thường.
- Chấm không chuyển xám giữa các lần thử. Khi webOS mở WebSocket thành công, app tự chuyển xanh và các nút điều khiển hoạt động mà không cần bấm nguồn lần hai.
- Chỉ chuyển xám sau khi hết cửa sổ chờ hoặc gặp lỗi không phải lỗi mạng (ví dụ cần ghép đôi/chứng chỉ).
- Thêm kiểm thử cho timeout reconnect nhanh khi TV đang boot.


## 0.2.7 — 2026-10-02

- Thêm nút **Tìm kiếm** với mic trên màn hình remote.
- Gọi trình nhận dạng giọng nói Android với ngôn ngữ `vi-VN`, ưu tiên offline khi dịch vụ trên máy hỗ trợ.
- Kết quả được gửi qua `com.webos.service.ime/insertText`, sau đó `sendEnterKey`.
- Giữ tạm câu nói qua vòng onStop/onStart của trình nhận dạng và gửi sau khi TV kết nối lại.
- Thêm quyền webOS `CONTROL_INPUT_TEXT`; không lưu bản ghi âm hay câu tìm kiếm vào cấu hình.
- Thêm kiểm thử cho quyền text input và chuỗi lệnh insertText → Enter.


## 0.2.6 — 2026-10-02

- Thêm launcher icon riêng dựa trên mẫu đã duyệt: nền gradient xanh–tím, remote màu tối, nút nguồn đỏ, D-pad/OK lớn.
- Android 8+ dùng vector icon; Android 8/API 26 trở lên có adaptive icon để launcher tự áp dụng hình tròn/squircle mà remote vẫn nằm trong vùng an toàn.
- Không thêm chữ hoặc logo hãng vào icon.


## 0.2.5 — 2026-10-02

- Sửa tình trạng app có thể tự thoát/crash khi thao tác liên tục ở 0.2.4.
- Không còn tạo một coroutine/request song song mới cho mỗi lần chạm.
- Các lệnh remote thường đi qua **một worker duy nhất** với hàng đợi giới hạn 12 thao tác; khi người dùng bấm quá nhanh, thao tác cũ nhất được bỏ thay vì tích tụ vô hạn.
- Giao diện vẫn không dùng `busy` cho các phím thường nên không quay lại hiện tượng chớp xám.
- Khi đổi kết nối, app ra nền hoặc bấm nguồn, hàng đợi thao tác cũ được xóa để không phát lại lên phiên mới.


## 0.2.4 — 2026-10-02

- Sửa hiện tượng màn hình “giật/chớp xám” mỗi lần bấm nút.
- Các lệnh thường ngày (điều hướng, OK, Home, YouTube, tăng/giảm âm lượng, tắt tiếng) không còn thay đổi trạng thái `busy` của toàn màn hình.
- Cho phép các lệnh ngắn chạy nối tiếp/đồng thời qua request ID riêng của phiên webOS; không khóa tất cả nút trong lúc chờ TV phản hồi.
- Vẫn giữ khóa giao diện cho thao tác nguồn/thiết lập cần tính tuần tự.
- Khi app rời foreground hoặc đổi kết nối, toàn bộ lệnh remote đang chờ được hủy để không tác động lên phiên mới.


## 0.2.3 — 2026-10-02

- Tối giản màn hình remote cho người không rành công nghệ: bỏ footer “Không quảng cáo · Chỉ dùng mạng nhà”, bỏ card lỗi kỹ thuật khỏi màn hình chính và bỏ snackbar thành công.
- Lỗi kỹ thuật chỉ hiện trong phần Cài đặt/thiết lập, không che các nút điều khiển hằng ngày.
- Nút nguồn trên header trở thành một nút một chạm, không mở hộp xác nhận:
  - đang kết nối (chấm xanh) → tắt TV;
  - chưa kết nối/chấm xám → gửi lệnh bật TV;
  - đang kết nối/ghép đôi → tạm khóa nút nguồn.
- Giữ cơ chế an toàn của 0.2.2: nếu chưa có MAC, app cố lấy/lưu MAC trước khi tắt; nếu không lấy được thì không tắt.
- Thêm kiểm thử quyết định bật/tắt theo trạng thái kết nối.


## 0.2.2 — 2026-10-02

- Sửa lỗi chính khiến app báo không có MAC trên nhiều LG webOS: hỗ trợ đúng wifiInfo / wiredInfo, đồng thời giữ wifi / wired cho firmware cũ.
- Không đọc MAC của gateway/router khi phân tích dữ liệu mạng TV.
- Wake-on-LAN gửi tới limited broadcast, broadcast đúng subnet và IP TV (unicast), trên cả UDP 9 và UDP 7, lặp 5 đợt ngắn.
- Nếu chưa có MAC, khi bấm Tắt TV app thử lấy và lưu MAC trước. Nếu vẫn không lấy được thì không tắt TV, tránh tình trạng tắt được nhưng không thể bật lại.
- Bổ sung test cho cấu trúc dữ liệu MAC thực tế của LG và kế hoạch gửi Wake-on-LAN.


## 0.2.1 — 2026-10-02

- Thêm ghép đôi PIN: TV hiện mã, app mở ô nhập số và gửi qua ssap://pairing/setPin.
- PIN không được ghi vào cấu hình; chỉ client-key do TV cấp sau khi ghép thành công được lưu như trước.
- Lần sau tự kết nối bằng client-key, không hiện lại ô PIN trừ khi TV thu hồi quyền, app bị xóa dữ liệu hoặc người dùng chọn ghép đôi lại.
- Vẫn hỗ trợ kiểu PROMPT: nếu TV yêu cầu xác nhận trực tiếp, app hiển thị hướng dẫn chọn **Cho phép** trên TV.
- Thêm kiểm thử PIN đúng, PIN sai/thử lại, định dạng PIN và không lưu PIN.

## 0.2.0 — 2026-10-02

- Giữ nút nguồn, xác nhận thao tác tắt, header cố định và chấm trạng thái của 0.1.1.
- Nút nguồn mở hai lựa chọn rõ ràng **Bật tivi** / **Tắt tivi**. Không suy ra tivi đã tắt chỉ vì socket mất kết nối.
- Thêm Wake-on-LAN cho model hỗ trợ TV On With Mobile; tự đọc MAC Wi-Fi/LAN khi tivi cho phép, có nhập thủ công trong cài đặt nâng cao. Không tự bật tivi khi mở app.
- Giữ cơ chế dừng thử nối lại sau yêu cầu tắt chủ động. Chọn Bật tivi, Thử lại hoặc mở lại app để nối lại.
- Giữ các kiểm thử nguồn 0.1.1; bổ sung kiểm thử gói wake, MAC, subnet, đọc cấu hình cũ và các kết quả lệnh nguồn.
- Từ chối quyền nguồn vẫn là lỗi; khi socket đóng trước hồi đáp chỉ ghi nhận yêu cầu đã gửi, không khẳng định tivi đã tắt.
- Hướng dẫn và giới hạn model: [docs/POWER.md](docs/POWER.md).

## 0.1.1 — 2026-10-02

- Nút **Nguồn** trên header, có xác nhận **Tắt TV** để tránh chạm nhầm. Hiện chỉ tắt TV, chưa bật từ trạng thái chờ.
- Bỏ khung trạng thái kết nối và tên tivi khỏi màn hình remote. Chỉ giữ chấm nhỏ trên header: xanh khi đã kết nối, vàng khi đang kết nối/ghép đôi, xám khi chưa kết nối.
- Chạm chấm hoặc bánh răng để mở thông tin kết nối, thử lại hoặc ghép đôi. Header luôn ở trên cùng khi cuộn.
- Các nút YouTube, Trang chủ, điều hướng, OK, Quay lại và âm lượng giữ nguyên.
- Xin thêm quyền CONTROL_POWER; không xóa khóa ghép đôi hay thay đổi cơ chế lưu thông tin tivi.
- Không gửi lại lệnh nguồn khi mất kết nối. Nếu TV đóng socket trước khi trả lời, không khẳng định chắc chắn TV đã tắt.
- Tạm dừng kết nối lại sau yêu cầu tắt chủ động. Mở lại app hoặc bấm Thử lại trong Cài đặt để kết nối khi TV đã bật.
- Thêm 5 kiểm thử cho lệnh tắt nguồn, quyền nguồn, lỗi quyền, kết nối bị đóng và không phát lại lệnh.

## Kiểm tra trên tivi thật

Bản 0.1.0 đã được người dùng xác nhận hoạt động. Các mục dưới đây của 0.1.1 vẫn cần thử trên TV thật, không được coi là đã đạt chỉ vì build/test thành công:

1. Sau khi kết nối, chỉ có chấm xanh trên header, không còn khung trạng thái phía dưới.
2. Chạm Nguồn → Hủy: TV không tắt. Chạm Nguồn → Tắt TV: TV tắt nếu đã cấp quyền.
3. Mất Wi-Fi không hiện chấm xanh; nút nguồn bị vô hiệu hóa khi chưa kết nối.
4. Sau khi bật TV bằng remote thường, mở lại app, thử YouTube/Home/điều hướng/âm lượng.
5. Với chữ lớn và màn hình nhỏ, header vẫn nhìn thấy; phần nút bên dưới cuộn được.

APK preview dùng khóa debug tạm theo runner: bản mới có thể không cài đè lên bản cũ. Khi đó gỡ bản thử cũ rồi cài và ghép đôi lại. Đây là giới hạn chữ ký của bản thử, không phải chủ động xóa cấu hình trong app.
