# Điều khiển TV · LG Family Remote

Ứng dụng Android tiếng Việt dành cho gia đình, điều khiển **LG webOS** qua mạng nội bộ. **Không quảng cáo, không tài khoản, không máy chủ trung gian.** Không phải ứng dụng chính thức của LG hoặc YouTube.

## Tải và dùng thử

Mở mục **Releases** của repo → chọn bản thử mới nhất → **Assets → lg-family-remote-preview.apk**. APK chỉ được phát hành sau khi workflow kiểm thử, lint và build thành công. Các bản chạy thất bại không tạo APK mới.

Cũng có thể tải qua **Actions → Build Android APK → lần chạy thành công → Artifacts → preview-apk**, rồi giải nén lấy APK. Không tải nhầm gói Source code khi muốn cài app.

**Bản thử dùng khóa debug tạm:** bản ở lần build khác có thể cần gỡ bản thử cũ, cài lại và ghép đôi lại. Để cập nhật đè ổn định, dùng khóa riêng theo [hướng dẫn ký APK](docs/SIGNING.md). Bản ký riêng được cài tách biệt với bản thử.

## Mới trong 0.3.8

- Vùng điều khiển giữa cao hơn và cân hơn với màn hình: D-pad khoảng **254dp**, touchpad khoảng **220dp**.
- Các vùng bấm mũi tên được kéo giãn nhẹ để dễ thao tác hơn mà không làm các hàng khác to thêm.

## Mới trong 0.3.7

- Khu điều hướng là **một card gọn**; mũi tên có vùng bấm lớn nhưng không còn các khối nền rời.
- Hàng dưới chỉ còn 4 icon **Back / Vol− / Mute / Vol+**, bỏ badge gây rối.
- Khi bấm **Tắt TV**, remote bị khóa và chấm chuyển xám ngay; không còn giữ xanh trong lúc TV đang shutdown.
- App tạm ngăn reconnect tự động sau lệnh tắt để trạng thái cũ không bật xanh trở lại.

## Mới trong 0.3.6

- Hàng trên chỉ còn 3 icon: **YouTube / Mic / Home**, không màu thương hiệu và không chữ.
- D-pad giữa là **← | ↑ / OK / ↓ | →**, vùng trái/phải lớn để dễ bấm.
- Hàng cuối gom thành **Back / Vol− / Mute / Vol+**.
- Hai nút bàn phím/chuột vẫn nổi ở góc dưới-phải; touchpad chuột vẫn full ngang.

## Mới trong 0.3.5

- Header màn hình remote không còn tên app: **Cài đặt + trạng thái** ở trái, **Nguồn** ở phải.
- Touchpad chuột trải full ngang màn hình.
- D-pad có 4 vùng bấm lớn tỏa ra 4 phía, giúp bấm mũi tên dễ trúng hơn.
- Hai nút bàn phím/chuột nổi trong góc dưới-phải vùng điều khiển nên giao diện ngắn hơn.

## Mới trong 0.3.4

- Mở app nhanh hơn: TV đã lưu hiện trạng thái **đang kiểm tra** ngay, không đứng xám chờ lâu.
- App thử IP gần nhất và tìm đúng TV bằng UID/SSDP song song; IP cũ đúng thì kết nối ngay, IP đã đổi thì tự chuyển sang IP mới.
- Chỉ hiện xanh khi kết nối webOS thật sự thành công, nên nhanh nhưng không báo sai trạng thái.

## Mới trong 0.3.3

- App tự tìm lại **đúng TV đã ghép** khi router cấp IP mới: dùng UID/SSDP, cập nhật IP và kết nối lại mà không cần vào **Tìm tivi**.
- Client-key, chứng chỉ, MAC bật TV và cấu hình cũ vẫn được giữ nguyên; bình thường không phải xác nhận ghép đôi lại.
- Nhận dạng UID ổn định hơn giữa các firmware LG nhờ chuẩn hóa `uuid:` / `urn:uuid:` / USN.

## Mới trong 0.3.2

- Cụm điều hướng là **D-pad tròn** với OK ở giữa.
- Có 2 nút cạnh dưới-phải: **⌨ nhập chữ** và **🖱 rê chuột**.
- Khi TV đang focus một ô nhập, app tự hiện vùng nhập và bật bàn phím Android; gõ trên điện thoại sẽ thay nội dung ô nhập trên TV.
- Chế độ chuột dùng vùng tròn làm touchpad: rê để di chuyển, chạm để click.
- Nếu nhập chữ chưa hoạt động sau khi cập nhật, vào Cài đặt → **Ghép đôi lại với tivi** một lần để TV cấp `CONTROL_INPUT_TEXT`.

## Mới trong 0.3.1

- Hai nút âm lượng hiển thị icon loa lớn kèm dấu **− / +** để nhìn nhanh là biết giảm hay tăng.

## Mới trong 0.3.0

- **Tìm YouTube bằng giọng nói**: bấm mic, nói nội dung; app mở thẳng trang kết quả YouTube cho câu nói đó.
- Không cần mở sẵn ô tìm kiếm trên TV và không phụ thuộc webOS IME/text focus.
- Không cần quyền `CONTROL_INPUT_TEXT`; dùng quyền mở ứng dụng YouTube đã có.

## Mới trong 0.2.9

- Nút nguồn trên header lớn hơn: vùng bấm 64dp, icon 36dp và nền đỏ nhạt để bố mẹ dễ nhìn và dễ chạm.

## Mới trong 0.2.8

- Cải thiện bật TV chậm: sau Wake-on-LAN, app giữ chấm ở trạng thái đang kết nối và tự thử lại nhanh trong tối đa khoảng 90 giây.
- Khi TV hoàn tất khởi động, app tự chuyển chấm xanh; không cần bấm nút nguồn lần hai để “reload”.

## Mới trong 0.2.7

- Phiên bản cũ dùng webOS IME nên phụ thuộc ô nhập đang focus; cơ chế này đã được thay bằng YouTube deep-link ở 0.3.0.

## Mới trong 0.2.6

- Thêm icon ứng dụng hình remote dễ nhận biết trên màn hình chính Android, cùng phong cách xanh–tím đã duyệt.

## Mới trong 0.2.5

- Chống crash khi bấm nhanh bằng hàng đợi remote giới hạn thay cho việc tạo nhiều lệnh song song.
- Hàng đợi giữ tối đa 12 thao tác gần nhất và xử lý tuần tự; không làm giao diện chớp xám.

## Mới trong 0.2.4

- Bấm phím remote không còn làm toàn bộ giao diện chuyển sang trạng thái disabled rồi bật lại.
- Điều hướng, OK, Home, YouTube và âm lượng phản hồi mượt hơn; chỉ thao tác nguồn/thiết lập mới khóa giao diện khi cần.

## Mới trong 0.2.3

- Màn hình remote chỉ giữ các điều khiển cần dùng; bỏ footer và các thông báo thành công không cần thiết.
- Nút nguồn là **một nút một chạm**: chấm xanh thì bấm để tắt, chấm xám thì bấm để bật. Không có hộp xác nhận.
- Thông tin/lỗi kỹ thuật được dồn vào Cài đặt thay vì che màn hình remote của người dùng trong gia đình.

## Mới trong 0.2.2

- Sửa đọc MAC cho payload LG phổ biến wifiInfo / wiredInfo; trước đây app chỉ đọc wifi / wired nên có thể báo sai “chưa có MAC”.
- Wake-on-LAN dùng nhiều đường gửi trong cùng LAN: limited broadcast, broadcast theo subnet thật và unicast tới IP TV; gửi trên UDP 9 và 7.
- Nếu chưa lưu MAC, app sẽ cố lấy MAC trước khi tắt TV. Nếu không lấy được, app giữ TV đang bật và yêu cầu ghép đôi lại hoặc nhập MAC thủ công.

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
3. Những lần sau mở app sẽ tự kết nối đến tivi đã lưu: **YouTube**, **Trang chủ**, **Tìm kiếm bằng giọng nói**, điều hướng / **OK**, **Quay lại**, tăng/giảm âm lượng và tắt tiếng.

Cài đặt nằm trong nút bánh răng, không chiếm màn hình điều khiển. Mỗi điện thoại cần thiết lập riêng.

Không tìm được tivi? Mở **Nhập IP / tùy chọn** và nhập IP nội bộ của tivi. Mặc định dùng WSS cổng 3001. Chỉ bật **TV cũ: WS cổng 3000** khi cần; chế độ này không mã hóa và app không tự chuyển sang nó. Mạng khách có tính năng cách ly thiết bị có thể chặn kết nối.

Nút YouTube chỉ mở ứng dụng trên tivi, không loại bỏ quảng cáo bên trong YouTube.

## Giao diện

Header màn hình remote gồm **Cài đặt**, chấm kết nối và **Nguồn**, không hiển thị tên app. Bên dưới là một hàng icon **YouTube / Mic / Home**, D-pad gọn với **OK**, hai nút **bàn phím / chuột**, rồi một hàng **Back / Vol− / Mute / Vol+**. Chữ tiếng Việt, vùng bấm lớn. Màn hình nhỏ hoặc cỡ chữ lớn có thể cuộn để không mất nút. Thông báo lỗi vẫn có thể xuất hiện khi thao tác thất bại; trạng thái kết nối thường ngày chỉ là chấm nhỏ.

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
- Kotlin + Jetpack Compose, OkHttp WebSocket, coroutines. Không SDK quảng cáo, analytics, camera, danh bạ hoặc định vị. Tìm kiếm giọng nói dùng hoạt động nhận dạng có sẵn của Android và app không xin quyền RECORD_AUDIO trực tiếp.
- Tìm tivi bằng SSDP; ghi nhớ UUID. Khi IP cũ không còn dùng được, app tự quét nhanh theo UUID/UID, lưu IP mới và reconnect mà không cần người dùng chọn lại TV.
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
