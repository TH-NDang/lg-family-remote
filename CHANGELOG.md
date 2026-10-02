# Thay đổi

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
