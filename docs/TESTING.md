# Kiểm thử

## CI tự động

`gradle testDebugUnitTest lintDebug assembleDebug`: kiểm tra biên dịch, Android Lint, unit test định dạng lệnh, mạng nội bộ, chứng chỉ và WebSocket bằng MockWebServer. Đây không phải mock giao diện được trình bày như app đã điều khiển tivi thật.

## Kiểm thử trên tivi thật (chưa thực hiện)

Ghi model TV, phiên bản webOS, model điện thoại và Android; không chia sẻ số sê-ri hoặc khóa ghép đôi.

- [ ] Android cài APK, mở app không crash; kích thước chữ lớn vẫn cuộn được, không mất nút.
- [ ] Cùng router: tìm đúng TV, tên/IP hợp lý. Guest Wi-Fi cách ly thiết bị phải hiển thị lỗi phù hợp.
- [ ] Ghép đôi lần đầu hiện Cho phép trên TV. Từ chối không được coi là thành công.
- [ ] Chấp nhận → đóng app → mở lại, không phải ghép đôi lại.
- [ ] YouTube mở trên TV, không mở trên điện thoại. Ứng dụng YouTube chưa cài phải báo lỗi.
- [ ] Home, Back, bốn hướng và OK hoạt động trong Home / YouTube.
- [ ] Tăng/giảm tiếng, tắt tiếng và bật lại tiếng; kiểm tra cả âm thanh ngoài nếu dùng soundbar.
- [ ] Dùng remote vật lý đổi âm lượng: app cập nhật nếu TV hỗ trợ subscription.
- [ ] Mất Wi-Fi rồi bật lại: tự nối lại khi app phía trước; không phát lại lệnh cũ.
- [ ] Tắt TV: app không khẳng định chắc chắn TV đang tắt, không gửi WOL tự động.
- [ ] TV đổi IP, UUID còn giữ: tìm lại địa chỉ. Nếu không công bố UUID, nhập IP thủ công.
- [ ] Certificate thay đổi: không tự bỏ qua pin; chủ nhà phải xác nhận ghép đôi lại.
- [ ] TV cũ không mở WSS: chỉ bật chế độ WS thủ công trên mạng tin cậy.
- [ ] Điện thoại thứ hai cần ghép đôi riêng.
- [ ] Bản APK ký riêng cập nhật đè giữ kết nối. Bản debug tạm không có bảo đảm này.

## Giao thức / giới hạn

Bản đầu dùng manifest chưa ký với các quyền remote tối thiểu, không sao chép chữ ký danh tính LG. Firmware có thể khác; nếu TV từ chối manifest hoặc pointer endpoint, cần xem lỗi tương thích trên model cụ thể rồi chỉnh. Không thể kết luận hỗ trợ mọi LG chỉ vì có chữ LG.

Android compile/target 35, min 26. Khi nâng target lên 37, phải thêm xử lý ACCESS_LOCAL_NETWORK theo https://developer.android.com/privacy-and-security/local-network-permission và thử lại SSDP/WebSocket. Không tăng target SDK mà bỏ bước này.
