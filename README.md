# FlipaClip 2D Animation App - Android Studio Project

Ứng dụng vẽ hoạt hình 2D từng khung hình (Frame-by-frame 2D Animation) phong cách **FlipaClip** chuyên nghiệp, được xây dựng hoàn toàn bằng **Kotlin**, **Jetpack Compose**, và **Android Native Canvas Engine**.

---

## 📱 1. Các tính năng nổi bật của ứng dụng

### 🎨 Canvas Vẽ & Animation Studio
- **Đa thao tác cảm ứng (Multi-touch)**: Hỗ trợ phóng to/thu nhỏ (pinch-to-zoom), xoay, di chuyển vùng nhìn (pan) và nút Reset góc nhìn canvas nhanh chóng.
- **Bộ công cụ vẽ toàn diện**:
  - **Pen / Pencil / Marker / Airbrush**: Chỉnh kích thước nét vẽ (1px - 60px), độ đậm nhạt (opacity), đường cong Bezier làm mịn nét tự nhiên.
  - **Eraser (Tẩy)**: Điều chỉnh độ lớn và độ tẩy sạch.
  - **Paint Bucket (Đổ màu thông minh)**: Thuật toán Flood Fill tô màu nhanh cho các vùng khép kín.
  - **Shape & Ruler (Thước kẻ hình học)**: Đoạn thẳng (Line), Hình chữ nhật (Rectangle), Hình tròn/Oval (Circle), Hình ngôi sao (Star), hỗ trợ bật/tắt tô đặc (Fill).
  - **Text Tool (Chữ viết)**: Thêm chữ lên khung hình với kích thước tùy chỉnh.
  - **Color Picker & Palette**: Bảng màu trực quan, bộ chỉnh màu RGB, mã màu HEX.
  - **Undo / Redo**: Hoàn tác và làm lại không giới hạn trên từng khung hình.

### 🧅 Chế độ Da Hành (Onion Skinning)
- Hiển thị bóng ma mờ của các khung hình trước (**Màu đỏ**) và khung hình sau (**Màu xanh lá**).
- Tùy chỉnh số lượng frame bóng ma (1 - 5 frames) và độ trong suốt (opacity) giúp căn chỉnh chuyển động mượt mà đúng chuẩn nguyên lý hoạt họa 12 Disney principles.

### 📑 Hệ thống Nhiều Lớp Vẽ (Multi-Layer)
- Quản lý các Layer độc lập: Phác thảo (Sketch), Viền nét (Lineart), Tô màu (Color), Phông nền (Background).
- Thêm layer mới, Xóa layer, Khóa/Mở khóa layer (Lock), Ẩn/Hiện layer (Visibility), Điều chỉnh độ trong suốt của từng layer.

### ⏱️ Timeline & Điều khiển Hoạt họa
- Dải khung hình (Frame strip) cuộn ngang với hình ảnh thu nhỏ trực quan.
- Thao tác frame: Thêm frame trống mới `(+)`, Nhân bản frame hiện tại (Duplicate), Copy & Paste frame, Xóa frame.
- **Playback Bar**: Nút Play/Pause chạy hoạt hình thời gian thực với tốc độ FPS chuẩn xác, Bật/Tắt chế độ Lặp lại (Loop), Chuyển tới/lùi từng frame (`|<`, `|>`).

### 🎙️ Ghi âm & Âm thanh Đồng bộ (Audio Tracks)
- Ghi âm trực tiếp giọng nói / hiệu ứng âm thanh qua Microphone tích hợp biểu đồ sóng âm (Waveform visualizer).
- Quản lý nhiều track âm thanh: Nghe thử, Tắt tiếng (Mute), Tăng giảm âm lượng (Volume slider).

### 🎬 Xuất Video & Hoạt ảnh (Export / Make Movie)
- Xuất video chuẩn **MP4 Video (H.264)** độ phân giải cao (720p / 1080p).
- Xuất ảnh động **Animated GIF** tự động lặp vô hạn.
- Xuất chuỗi ảnh **PNG Sequence (ZIP)**.
- Trình xem trước video và nút Chia sẻ (**Android Share Sheet**) gửi qua Zalo, Messenger, Drive, TikTok, YouTube...

---

## 🛠️ 2. Hướng dẫn mở và chạy dự án trong Android Studio

### Bước 1: Mở dự án trong Android Studio
1. Khởi động **Android Studio**.
2. Chọn **Open** (hoặc vào menu `File` -> `Open...`).
3. Điều hướng và chọn thư mục: `D:\TTS`.
4. Nhấn **OK**. Android Studio sẽ tự động nhận diện cấu hình Gradle và bắt đầu quá trình **Gradle Sync** (tải các thư viện AndroidX, Compose nếu cần).

### Bước 2: Chạy thử ứng dụng (Run App)
1. **Sử dụng Máy ảo (Emulator)**:
   - Trong Android Studio, mở menu **Device Manager** (hoặc biểu tượng điện thoại ở góc phải trên).
   - Chọn một máy ảo (Ví dụ: *Pixel 7 Pro - Android 14/15* hoặc tạo máy ảo mới) và bấm **Start**.
2. **Sử dụng Điện thoại thật (Physical Device)**:
   - Trên điện thoại Android, vào *Cài đặt* -> *Tùy chọn nhà phát triển* -> Bật **Gỡ lỗi qua USB (USB Debugging)**.
   - Cắm cáp kết nối điện thoại với máy tính.
3. Nhấn nút **Run 'app'** (Biểu tượng tam giác xanh ▶ hoặc phím tắt `Shift + F10`).
4. Ứng dụng sẽ được biên dịch và cài đặt trực tiếp lên thiết bị!

---

## 📦 3. Hướng dẫn Build file APK / Phát hành lên Google Play

### Cách 1: Build file APK để cài đặt trực tiếp (Debug APK)
1. Trong Android Studio, vào menu `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`.
2. Khi hoàn thành, thông báo hiện ra ở góc phải dưới, nhấn **locate** để mở thư mục chứa file `app-debug.apk` (nằm tại `D:\TTS\app\build\outputs\apk\debug\app-debug.apk`).
3. Bạn có thể chép file `.apk` này vào điện thoại để cài đặt ngay.

### Cách 2: Build Signed APK / Android App Bundle (.aab) để đẩy lên Google Play
1. Trong Android Studio, vào menu `Build` -> `Generate Signed Bundle / APK...`.
2. Chọn:
   - **Android App Bundle (.aab)**: Chuẩn tải lên Google Play Console.
   - Hoặc **APK**: Để chia sẻ phát hành trực tiếp.
3. Chọn hoặc tạo mới Keystore (Key alias, Password) -> Nhấn **Next**.
4. Chọn build variant `release` -> Nhấn **Finish**.
5. File `.aab` / `.apk` phát hành sẽ được tạo tại thư mục `app/release/`.

---

## 📂 4. Cấu trúc thư mục mã nguồn

```
d:\TTS\
├── app\
│   ├── build.gradle.kts
│   └── src\main\
│       ├── AndroidManifest.xml
│       ├── java\com\flipaclip\animation\
│       │   ├── MainActivity.kt                  # Entry point & Xin quyền Runtime
│       │   ├── Navigation.kt                    # Điều hướng Compose NavHost
│       │   ├── data\
│       │   │   ├── model\                       # Project, Frame, Layer, DrawingStroke, ToolType...
│       │   │   └── repository\                  # ProjectRepository & SampleProjectGenerator
│       │   ├── engine\
│       │   │   ├── CanvasDrawingEngine.kt       # Engine vẽ cọ, Bezier, Shape, Text, Layer Bitmap
│       │   │   ├── FloodFill.kt                 # Thuật toán đổ màu thùng sơn thông minh
│       │   │   ├── OnionSkinEngine.kt           # Engine da hành vẽ bóng ma đỏ/xanh lá
│       │   │   ├── VideoExporter.kt             # Render MP4 Video qua MediaCodec & MediaMuxer
│       │   │   ├── GifExporter.kt               # Xuất ảnh động Animated GIF chất lượng cao
│       │   │   ├── PngSequenceExporter.kt       # Xuất chuỗi ảnh PNG nén ZIP
│       │   │   └── AudioEngine.kt               # Ghi âm Microphone & Waveform trực quan
│       │   └── ui\
│       │       ├── theme\                       # Theme FlipaClip Dark Mode, Color, Typography
│       │       ├── viewmodel\                   # HomeViewModel, StudioViewModel, ExportViewModel
│       │       ├── home\                        # HomeScreen, ProjectCard, CreateProjectDialog
│       │       ├── studio\                      # StudioScreen, CanvasView, TopToolBar, BottomTimeline...
│       │       ├── audio\                       # VoiceRecorderDialog, AudioTrackSheet
│       │       └── export\                      # ExportDialog, ExportProgressScreen, VideoPreviewPlayer
│       └── res\                                 # Icons, themes, colors, strings, file_paths
├── gradle\wrapper\                              # Gradle 8.5 distribution wrapper
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```
