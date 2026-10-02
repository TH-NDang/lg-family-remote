# Khóa ký cho bản dùng lâu dài

Workflow luôn tạo **preview APK** có thể cài ngay. Bản thử có applicationId `vn.ndang.lgfamilyremote.preview`, tách khỏi bản gia đình. Khóa debug của runner có thể thay đổi giữa các lần build: có thể phải gỡ bản thử cũ và ghép đôi lại. Không dùng khóa debug công khai cho bản gia đình.

Bản gia đình có applicationId `vn.ndang.lgfamilyremote`, ký bằng khóa riêng cố định do chủ repo giữ. Không gửi khóa/mật khẩu vào cuộc trò chuyện hoặc commit vào Git.

## Một lần duy nhất trên máy bạn

Cần JDK 17 (`keytool`) và Python 3. Thực hiện ngoài thư mục repo hoặc giữ đúng `.gitignore`.

```sh
keytool -genkeypair -v -storetype JKS -keystore family.jks -alias family -keyalg RSA -keysize 3072 -validity 10000
```

Keytool hỏi mật khẩu; dùng mật khẩu mạnh và sao lưu an toàn file JKS, alias, mật khẩu kho và mật khẩu key. Đừng gõ mật khẩu vào tham số dòng lệnh hoặc chụp ảnh công khai.

Tạo JSON trên máy bằng Python (nhập mật khẩu ẩn):

```python
import base64, getpass, json
from pathlib import Path
bundle = {
    'keystore_base64': base64.b64encode(Path('family.jks').read_bytes()).decode(),
    'store_password': getpass.getpass('Store password: '),
    'key_alias': 'family',
    'key_password': getpass.getpass('Key password: '),
}
Path('signing-bundle.json').write_text(json.dumps(bundle), encoding='utf-8')
```

Vào repo → Settings → Secrets and variables → Actions → New repository secret.

Tên: **ANDROID_SIGNING_BUNDLE**. Nội dung: toàn bộ JSON trong `signing-bundle.json`.

Xóa bản JSON không cần thiết sau khi nhập, giữ bản sao khóa ở nơi an toàn. Dùng GitHub CLI đã đăng nhập cũng được: `gh secret set ANDROID_SIGNING_BUNDLE < signing-bundle.json`.

Chạy **Actions → Build Android APK → Run workflow**. Khi secret hợp lệ, workflow tạo cả `signed-family-apk` và Release gắn APK ký riêng. Workflow không tự thay đổi hoặc tự công khai khóa ký. Nếu secret thiếu, bản thử vẫn build; nếu secret sai, bước ký sẽ báo lỗi thay vì giả làm bản chính thức.

Mọi bản cập nhật gia đình phải giữ nguyên applicationId và khóa ký, đồng thời tăng versionCode. Workflow sử dụng github.run_number làm versionCode. Nếu đổi tên/tạo lại workflow hoặc nhập repo mới, kiểm tra versionCode không giảm trước khi phát hành.

Tài liệu Android: https://developer.android.com/studio/publish/app-signing
