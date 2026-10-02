# Nút nguồn · phiên bản 0.2.0

Trên header, chạm biểu tượng nguồn màu đỏ → chọn **Bật tivi** hoặc **Tắt tivi**. Chọn rõ thao tác thay vì coi mất kết nối đồng nghĩa với tivi đã tắt. Không tự bật tivi khi mở app, nối lại Wi-Fi hay kết nối lại socket.

## Tắt tivi

App gửi `ssap://system/turnOff` qua phiên webOS đã ghép đôi. Có tivi đóng socket trước khi trả lời: app chỉ báo đã gửi yêu cầu, không báo chắc chắn tivi đã tắt. Lệnh không được tự gửi lại. Lỗi từ chối quyền vẫn được báo.

Nếu quyền cũ chưa cho phép điều khiển nguồn, vào bánh răng → Nhập IP / tùy chọn → Ghép đôi lại; chọn Cho phép trên tivi. App không tự bỏ qua xác nhận ghép đôi hay chứng chỉ.

## Bật tivi

App dùng Wake-on-LAN (UDP 9) trên Wi-Fi/Ethernet nội bộ của điện thoại. App thử đọc MAC Wi-Fi/LAN của chính tivi qua `com.webos.service.connectionmanager/getinfo` sau khi kết nối. MAC lưu cùng cấu hình mã hóa hiện có; API không có/không cho phép đọc MAC sẽ không làm hỏng các nút điều khiển khác.

Trên tivi bật **TV On With Mobile → Turn on via Wi-Fi** (đường dẫn khác tùy model). Tivi phải còn cắm điện, điện thoại và tivi cùng mạng. Một số model chỉ hỗ trợ tốt khi cắm LAN hoặc không hỗ trợ đánh thức qua mạng; không hứa tương thích mọi model.

Nếu app chưa tự lấy được MAC, mở bánh răng → Nhập IP / tùy chọn → Bật tivi qua mạng. Chọn Lấy địa chỉ từ tivi, hoặc nhập MAC của giao diện Wi-Fi/LAN đang dùng trên tivi, rồi Lưu địa chỉ bật tivi. Không nhập MAC điện thoại hay router.

Gói wake được gửi ba đợt ngắn, không lặp vô hạn. Địa chỉ broadcast tính theo subnet mask thực tế; không giả định mạng luôn là /24. Gửi UDP thành công không có nghĩa TV đã bật: chấm chỉ chuyển xanh khi phiên điều khiển đã đăng ký thành công.

## Header

Chấm xanh: đã kết nối; chấm vàng: đang thử kết nối/ghép đôi; chấm xám: chưa kết nối. Không còn khung Đã kết nối trên màn hình remote. Header cố định; bánh răng giữ phần kết nối chi tiết và nút thử lại. Trình đọc màn hình đọc được trạng thái của chấm.

## Kiểm thử thực tế

Người dùng đã xác nhận bản 0.1.0 hoạt động. Tính năng nguồn ở 0.2.0 chưa được xác nhận trên TV của gia đình. Kiểm tra: mở app → chấm xanh → nguồn/tắt → nguồn/bật → chấm xanh → YouTube/Home/âm lượng. Thử lại sau khi tivi chờ lâu, và khi điện thoại tắt Wi-Fi: không được báo đã bật thành công khi chỉ gửi UDP.

Giữ gói ghép đôi cũ khi cập nhật cùng chữ ký: model dữ liệu đọc được cấu hình 0.1.0 không có MAC. Tuy nhiên các APK preview được ký bằng khóa debug tạm của runner; đổi khóa có thể bắt buộc gỡ bản cũ, việc này xóa dữ liệu ứng dụng. Dùng khóa ký riêng trong docs/SIGNING.md để cập nhật lâu dài.

## Nguồn giao thức

- https://github.com/home-assistant-libs/aiowebostv/blob/main/aiowebostv/endpoints.py
- https://www.home-assistant.io/integrations/webostv/
- https://kr.eguide.lgappstv.com/manual/w23_mr2/global/Contents/settings/externaldevice/tvonmobile_e_a/eng/w23__settings__externaldevice__tvonmobile_e_a__eng.html
