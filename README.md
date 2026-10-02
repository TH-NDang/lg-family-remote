# Điều khiển TV · LG Family Remote

Ứng dụng Android tiếng Việt dành cho gia đình, điều khiển **LG webOS** qua mạng nội bộ. **Không quảng cáo, không tài khoản, không máy chủ trung gian.** Không phải ứng dụng chính thức của LG hoặc YouTube.

## Tải và dùng thử

Mở mục **Releases** của repo → chọn bản thử mới nhất → **Assets → lg-family-remote-preview.apk**. APK chỉ được phát hành sau khi workflow kiểm thử, lint và build thành công. Các bản chạy thất bại không tạo APK mới.

Cũng có thể tải qua **Actions → Build Android APK → lần chạy thành công → Artifacts → preview-apk**, rồi giải nén lấy APK. Không tải nhầm gói Source code khi muốn cài app.

**Bản thử dùng khóa debug tạm:** bản ở lần build khác có thể cần gỡ bản thử cũ, cài lại và ghép đôi lại. Để cập nhật đè ổn định, dùng khóa riêng theo [hướng dẫn ký APK](docs/SIGNING.md). Bản ký riêng được cài tách biệt với bản thử.

## Thiết lập lần đầu

1. Bật tivi bằng remote thường. Điện thoại và tivi cần ở cùng mạng nhà; tivi có thể cắm LAN vào cùng router.
2. Mở app → **Tìm tivi** → chọn đúng tivi → **Cho phép** trên màn hình tivi. Chỉ ghép đôi trên mạng tin cậy.
3. Những lần sau mở app sẽ tự kết nối đến tivi đã lưu: **YouTube**, **Trang chủ**, điều hướng / **OK**, **Quay lại**, tăng/giảm âm lượng và tắt tiếng.

Cài đặt nằm trong nút bánh răng, không chiếm màn hình điều khiển. Mỗi điện thoại cần thiết lập riêng.

Không tìm được tivi? Mở **Nhập IP / tùy chọn** và nhập IP nội bộ của tivi. Mặc định dùng WSS cổng 3001. Chỉ bật **TV cũ: WS cổng 3000** khi cần; chế độ này không mã hóa và app không tự chuyển sang nó. Mạng khách có tính năng cách ly thiết bị có thể chặn kết nối.

Chưa có chức năng bật tivi từ trạng thái tắt. Nút YouTube chỉ mở ứng dụng trên tivi, không loại bỏ quảng cáo bên trong YouTube.

## Giao diện

Hai nút nổi bật **YouTube** và **Trang chủ**, cụm bốn hướng + **OK**, nút **Quay lại**, **Giảm tiếng / Tăng tiếng**, **Tắt tiếng / Bật lại tiếng**. Chữ tiếng Việt, vùng bấm lớn. Màn hình nhỏ hoặc cỡ chữ lớn có thể cuộn để không mất nút.

## GitHub Actions

Workflow `.github/workflows/build-apk.yml` dùng **GitHub-hosted Ubuntu**, không cần self-hosted runner hay mở máy tính của bạn.

- Push mã vào `main`: kiểm thử JVM, Android Lint, biên dịch APK thử và phát hành một prerelease.
- Pull request: kiểm thử và build; không phát hành, không dùng khóa ký riêng.
- **Run workflow**: chạy thủ công, có tùy chọn `publish_release`.
- Có secret `ANDROID_SIGNING_BUNDLE`: build thêm APK ký riêng và đưa vào artifact `signed-family-apk`.
- Artifact APK lưu 30 ngày, báo cáo kiểm thử lưu 14 ngày theo cấu hình workflow. APK đính kèm Release không dùng thời hạn artifact này.

Không đưa file JKS, mật khẩu hoặc thông tin ghép đôi tivi vào repo công khai. Xem [SECURITY.md](SECURITY.md).

## Phạm vi kỹ thuật 0.1.0

- Android 8.0 trở lên (minSdk 26), compile/target SDK 35.
- Kotlin + Jetpack Compose, OkHttp WebSocket, coroutines. Không SDK quảng cáo, analytics, camera, micro, danh bạ hoặc định vị.
- Tìm tivi bằng SSDP; ghi nhớ UUID và tìm lại IP khi UUID còn được công bố.
- Kết nối lại khi app ở phía trước; không có dịch vụ chạy nền.
- Khóa ghép đôi và dấu vân tay chứng chỉ được mã hóa bằng Android Keystore, lưu trong thư mục không sao lưu.
- Không tự phát lại lệnh khi nối lại. Lệnh đã tới tivi trước khi mất mạng không thể thu hồi.
- Tắt tiếng dựa trên trạng thái trả về từ tivi, không tự đoán khi tivi không cung cấp.
- Không hỗ trợ LG NetCast, TV không thông minh hoặc điều khiển qua Internet.

**Build / unit test thành công không chứng minh tương thích với mọi TV. Chưa kiểm thử trên tivi vật lý của gia đình.** Xem [bảng kiểm thử thực tế](docs/TESTING.md). SSAP / pointer input tùy thuộc firmware.

## Phát triển

```sh
# JDK 17, Android SDK 35, Gradle 8.11.1
gradle testDebugUnitTest lintDebug assembleDebug
```

Workflow tự cài Gradle và Android SDK. Khi build trên máy, có thể tạo wrapper bằng `gradle wrapper --gradle-version 8.11.1`. Phiên bản dependency được ghim trong mã nguồn. Khi nâng target lên Android 17/API 37, phải bổ sung xử lý quyền mạng nội bộ theo tài liệu Android, không chỉ đổi số SDK.

## Tham khảo giao thức

- https://connectsdk.com/en/latest/apis-and/and-webostvservice.html
- https://github.com/ConnectSDK/Connect-SDK-Android-Core
- https://github.com/hobbyquaker/lgtv2
- https://developer.android.com/privacy-and-security/local-network-permission

App dùng manifest không mạo danh LG và chỉ xin nhóm quyền remote cần thiết. TLS dùng trust-on-first-use cho IP được chọn, pin chứng chỉ sau khi ghép đôi thành công; không phải xác minh độc lập danh tính tivi. Một số firmware có thể cần điều chỉnh sau khi thử thực tế.
