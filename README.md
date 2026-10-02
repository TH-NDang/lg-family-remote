# Điều khiển TV · LG Family Remote

Ứng dụng Android tiếng Việt dành cho gia đình, điều khiển **LG webOS** qua mạng nội bộ. Không quảng cáo, không tài khoản, không máy chủ trung gian. Không phải ứng dụng chính thức của LG hoặc YouTube.

## Sử dụng

1. Bật tivi bằng remote thường. Điện thoại và tivi cần ở cùng mạng nhà (tivi có thể cắm LAN).
2. Mở app → **Tìm tivi** → chọn đúng tivi → **Cho phép** trên màn hình tivi. Chỉ ghép đôi trên mạng tin cậy.
3. Những lần sau mở app là tự kết nối: **YouTube**, **Trang chủ**, điều hướng / **OK**, **Quay lại**, âm lượng và tắt tiếng.

Cài đặt được thu gọn trong nút bánh răng. Nếu không tìm được tivi, mở **Nhập IP / tùy chọn**. Mặc định dùng WSS cổng 3001. Chỉ chọn chế độ TV cũ (WS cổng 3000, không mã hóa) khi cần; app không tự hạ cấp bảo mật.

## Tải APK

Vào **Actions → Build Android APK → lần chạy thành công → Artifacts**. Bản `preview-apk` cài được ngay để kiểm tra tivi; tên app có chữ **Thử**. Bản này dùng khóa debug tạm của runner, không dành cho cập nhật lâu dài.

Để tạo bản chính thức dùng lâu dài, thêm secret `ANDROID_SIGNING_BUNDLE` theo [hướng dẫn ký APK](docs/SIGNING.md), rồi chạy workflow với tùy chọn **publish_release**. Khi secret có mặt, mỗi lần build nhánh chính tạo cả APK ký cố định. Không bao giờ đưa khóa riêng vào Git.

## Phạm vi bản 0.1.0

- Một tivi đã lưu trên mỗi điện thoại; mỗi điện thoại ghép đôi riêng.
- Android 8.0 trở lên (minSdk 26), compile/target SDK 35.
- Tìm tivi bằng SSDP; ghi nhớ UUID và tìm lại IP khi UUID còn được công bố.
- Kết nối lại khi app ở phía trước; không chạy dịch vụ nền hoặc quét mạng khi app đóng.
- Khóa ghép đôi và dấu vân tay chứng chỉ được mã hóa bằng Android Keystore, nằm trong thư mục không sao lưu.
- Kiểm tra lỗi trả về từ TV; không hàng đợi lệnh cũ; tắt tiếng phản ánh trạng thái TV khi được hỗ trợ.
- Không điều khiển TV LG NetCast / TV không thông minh; không đánh thức TV đang tắt; không loại bỏ quảng cáo bên trong YouTube.

**Build / unit test thành công không chứng minh tương thích với mọi TV. Chưa kiểm thử trên tivi vật lý của gia đình.** Xem [bảng kiểm thử thực tế](docs/TESTING.md).

## Phát triển

Kotlin + Jetpack Compose, OkHttp WebSocket, coroutines. Không SDK quảng cáo, analytics, camera, micro, danh bạ hay định vị.

```sh
# JDK 17, Android SDK 35, Gradle 8.11.1
# Workflow dùng gradle/actions/setup-gradle; không cần self-hosted runner.
gradle testDebugUnitTest lintDebug assembleDebug
```

Gradle wrapper có thể tạo bằng `gradle wrapper --gradle-version 8.11.1` sau khi cài Gradle. Phiên bản dependency được ghim trong mã nguồn.

## Tham khảo giao thức

- https://connectsdk.com/en/latest/apis-and/and-webostvservice.html
- https://github.com/hobbyquaker/lgtv2
- https://developer.android.com/privacy-and-security/local-network-permission

SSAP / pointer input là API phụ thuộc firmware. App dùng manifest không mạo danh LG, chỉ xin nhóm quyền remote cần thiết; một số firmware có thể yêu cầu điều chỉnh sau khi thử thực tế. TLS dùng trust-on-first-use cho đúng IP được chọn, pin chứng chỉ ở lần ghép đôi thành công; đây không phải chứng thực độc lập danh tính tivi. Chi tiết tại [SECURITY.md](SECURITY.md).
