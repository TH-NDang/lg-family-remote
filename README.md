# Điều khiển TV · LG Family Remote

Ứng dụng Android tiếng Việt dành cho gia đình, điều khiển **LG webOS** qua mạng nội bộ. **Không quảng cáo, không tài khoản, không máy chủ trung gian.** Không phải ứng dụng chính thức của LG hoặc YouTube.

## Tải và dùng thử

Mở mục **Releases** của repo → chọn bản thử mới nhất → **Assets → lg-family-remote-preview.apk**. APK chỉ được phát hành sau khi workflow kiểm thử, lint và build thành công. Các bản chạy thất bại không tạo APK mới.

Cũng có thể tải qua **Actions → Build Android APK → lần chạy thành công → Artifacts → preview-apk**, rồi giải nén lấy APK. Không tải nhầm gói Source code khi muốn cài app.

**Bản thử dùng khóa debug tạm:** bản ở lần build khác có thể cần gỡ bản thử cũ, cài lại và ghép đôi lại. Để cập nhật đè ổn định, dùng khóa riêng theo [hướng dẫn ký APK](docs/SIGNING.md). Bản ký riêng được cài tách biệt với bản thử.

## Mới trong 0.2.1

- Hỗ trợ **PIN pairing** của LG webOS: khi TV yêu cầu PIN, app hiện hộp nhập mã. PIN chỉ tồn tại trong màn hình ghép đôi và **không được lưu**; sau khi TV trả client-key, app lưu khóa ghép đôi như trước để những lần sau tự kết nối.
- Lần ghép mới/ghép lại ưu tiên yêu cầu PIN. Nếu firmware trả kiểu PROMPT, app vẫn chuyển về hướng dẫn **Cho phép trên tivi** như trước.
- Sai PIN không được lưu và có thể nhập lại; app không tự phát lại PIN.

## Mới trong 0.2.0

- **Nguồn** nằm trên header: chạm rồi chọn rõ **Bật tivi** hoặc **Tắt tivi**. Đóng hộp thoại không gửi lệnh; không vô tình tắt chỉ vì chạm biểu tượng.
- **Tắt tivi** dùng kết nối webOS đã ghép đôi. **Bật tivi** dùng Wake-on-LAN, cần model hỗ trợ và bật **TV On With Mobile / Turn on via Wi-Fi** trên tivi. App thử đọc MAC tự động khi kết nối, có nhập thủ công trong cài đặt nâng cao.
- Màn hình remote chỉ có **một chấm kết nối trên header**, không còn khung trạng thái hoặc tên tivi phía dưới: xanh khi đã kết nối, vàng khi đang kết nối/ghép đôi, xám khi chưa kết nối. Mất kết nối không đồng nghĩa TV đã tắt.
- Chạm chấm hoặc bánh răng để xem chi tiết và thử kết nối lại. Header giữ nguyên ở trên khi cuộn các nút.
- Các nút YouTube, Trang chủ, điều hướng, Quay lại và âm lượng giữ nguyên. Xem [CHANGELOG.md](CHANGELOG.md) và [hướng dẫn nguồn](docs/POWER.md).

Sau khi yêu cầu tắt TV, app dừng thử kết nối lại để tránh quét mạng không cần thiết. Chọn **Bật tivi**, mở lại app hoặc chọn **Thử lại** trong Cài đặt để tiếp tục kết nối. App không tự gửi lệnh bật khi bạn chỉ mở app. Nếu TV từ chối quyền nguồn, mở **Cài đặt → Nhập IP / tùy chọn → Ghép đôi lại với tivi**, rồi xác nhận trên TV.

## Thiết lập lần đầu

1. Bật tivi bằng remote thường. Điện thoại và tivi cần ở cùng mạng nhà; tivi có thể cắm LAN vào cùng router.
2. Mở app → **Tìm tivi** → chọn đúng tivi → **Cho phép** trên màn hình tivi. Chỉ ghép đôi trên mạng tin cậy.
3. Những lần sau mở app sẽ tự kết nối đến tivi đã lưu: **YouTube**, **Trang chủ**, điều hướng / **OK**, **Quay lại**, tăng/giảm âm lượng và tắt tiếng.

Cài đặt nằm trong nút bánh răng, không chiếm màn hình điều khiển. Mỗi điện thoại cần thiết lập riêng.

Không tìm được tivi? Mở **Nhập IP / tùy chọn** và nhập IP nội bộ của tivi. Mặc định dùng WSS cổng 3001. Chỉ bật **TV cũ: WS cổng 3000** khi cần; chế độ này không mã hóa và app không tự chuyển sang nó. Mạng khách có tính năng cách ly thiết bị có thể chặn kết nối.

Nút YouTube chỉ mở ứng dụng trên tivi, không loại bỏ quảng cáo bên trong YouTube.

## Giao diện

Header gồm tiêu đề, chấm kết nối, **Nguồn** và bánh răng. Bên dưới là hai nút nổi bật **YouTube** và **Trang chủ**, cụm bốn hướng + **OK**, nút **Quay lại**, **Giảm tiếng / Tăng tiếng**, **Tắt tiếng / Bật lại tiếng**. Chữ tiếng Việt, vùng bấm lớn. Màn hình nhỏ hoặc cỡ chữ lớn có thể cuộn để không mất nút. Thông báo lỗi vẫn có thể xuất hiện khi thao tác thất bại; trạng thái kết nối thường ngày chỉ là chấm nhỏ.

## GitHub Actions

Workflow `.github/workflows/build-apk.yml` dùng **GitHub-hosted Ubuntu**, không cần self-hosted runner hay mở máy tính của bạn.

- Push mã vào `main`: kiểm thử JVM, Android Lint, biên dịch APK thử và phát hành một prerelease.
- Pull request: kiểm thử và build; không phát hành, không dùng khóa ký riêng.
- **Run workflow**: chạy thủ công, có tùy chọn `publish_release`.
- Có secret `ANDROID_SIGNING_BUNDLE`: build thêm APK ký riêng và đưa vào artifact `signed-family-apk`.
- Artifact APK lưu 30 ngày, báo cáo kiểm thử lưu 14 ngày theo cấu hình workflow. APK đính kèm Release không dùng thời hạn artifact này.

Không đưa file JKS, mật khẩu hoặc thông tin ghép đôi tivi vào repo công khai. Xem [SECURITY.md](SECURITY.md).

## Phạm vi kỹ thuật 0.2.0

- Android 8.0 trở lên (minSdk 26), compile/target SDK 35.
- Kotlin + Jetpack Compose, OkHttp WebSocket, coroutines. Không SDK quảng cáo, analytics, camera, micro, danh bạ hoặc định vị.
- Tìm tivi bằng SSDP; ghi nhớ UUID và tìm lại IP khi UUID còn được công bố.
- Kết nối lại khi app ở phía trước; không có dịch vụ chạy nền.
- Khóa ghép đôi, dấu vân tay chứng chỉ và MAC tivi được mã hóa bằng Android Keystore, lưu trong thư mục không sao lưu. Đọc được cấu hình cũ không có MAC.
- Không tự phát lại lệnh điều khiển khi nối lại, kể cả lệnh tắt nguồn. Lệnh đã tới tivi trước khi mất mạng không thể thu hồi.
- Xin thêm CONTROL_POWER và READ_NETWORK_STATE để điều khiển nguồn và đọc MAC của chính tivi. Tivi không cho đọc MAC vẫn dùng được các nút remote khác.
- Nếu TV đóng kết nối trước khi trả lời lệnh tắt, app không coi đó là bằng chứng chắc chắn TV đã tắt.
- Wake-on-LAN chỉ gửi khi bấm Bật tivi, ba đợt ngắn UDP 9 trên mạng nội bộ; gửi UDP thành công không phải xác nhận TV đã bật. Chấm xanh chỉ xuất hiện khi kết nối thật thành công.
- Tắt tiếng dựa trên trạng thái trả về từ tivi, không tự đoán khi tivi không cung cấp.
- Không hỗ trợ LG NetCast, TV không thông minh hoặc điều khiển qua Internet. Bật qua mạng tùy model và cài đặt, không bật được TV đã rút điện.

**Người dùng đã xác nhận bản 0.1.0 hoạt động trên tivi nhà mình. Chức năng nguồn mới của 0.2.0 chưa được xác nhận trên tivi vật lý. Build / unit test thành công không chứng minh tương thích với mọi TV.** Xem [bảng kiểm thử thực tế](docs/TESTING.md) và [kiểm tra nguồn](docs/POWER.md). SSAP / pointer input tùy thuộc firmware.

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
