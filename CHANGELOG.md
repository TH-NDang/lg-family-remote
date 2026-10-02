# Thay đổi

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
