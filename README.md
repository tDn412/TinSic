# Báo cáo Bài tập lớn - Môn INT3120 1
## Đề tài: Xây dựng ứng dụng nghe nhạc & Karaoke thông minh TinSic

### Thông tin nhóm
- **Nhóm:** 6
- **Môn học:** Phát triển ứng dụng di động (INT3120 1)
- **Giảng viên hướng dẫn:** Lê Khánh Trình

| Họ và tên | Mã sinh viên | Email | Github |
| :--- | :--- | :--- | :--- |
| **Nhữ Đình Tú** | 23021703 | nhudinhtu1@gmail.com | [@tDn412](https://github.com/tDn412) |
| **Nguyễn Anh Tuấn** | 23021707 | nguyenanhtuan070305@gmail.com | [@natuan05](https://github.com/natuan05) |
| **Lê Duy Vũ** | 23021751 | djanh123456@gmail.com | [@notvux00](https://github.com/notvux00) |
| **Lê Ngọc Quyết** | 23021679 | lengocquyet120305@gmail.com | [@LeeNgocQuyet](https://github.com/LeeNgocQuyet) |
| **Lê Nhữ Quang** | 23021671 | lvs.hunghoa@gmail.com | [@qu4ll12](https://github.com/qu4ll12) |

---

### 1. Giới thiệu tổng quan
TinSic là ứng dụng giải trí đa phương tiện trên nền tảng Android, kết hợp giữa trình phát nhạc trực tuyến, tính năng Karaoke chấm điểm thời gian thực và các Minigame âm nhạc tương tác. Dự án hướng tới trải nghiệm người dùng hiện đại, trẻ trung với giao diện Dark Mode (Gravity Theme).

### 2. Các chức năng đã xây dựng hoàn thiện
Nhóm đã hoàn thành các module sau:

#### a. Module Phát nhạc (Music Player)
- **Core Player:** Sử dụng **ExoPlayer (Media3)** để xử lý luồng âm thanh chuẩn.
- **Tính năng:** Phát/Tạm dừng, Chuyển bài, Thanh tiến độ (Seek bar).
- **Background Service:** Hỗ trợ phát nhạc nền khi thoát ứng dụng.

#### b. Module Khám phá & Cá nhân hóa (Personalization)
- **Discover Mode (Tinder-style):** Giao diện vuốt (Swipe) trái/phải để thích hoặc bỏ qua bài hát, giúp gợi ý nhạc mới dựa trên sở thích (tương tự cơ chế của Tinder).
- **Thư viện cá nhân (Library):**
    - **History:** Lưu lại lịch sử nghe nhạc.
    - **Liked Songs:** Danh sách bài hát yêu thích.
    - **Playlist:** Tạo và quản lý danh sách phát cá nhân.
- **Search:** Tìm kiếm bài hát, nghệ sĩ, album.

#### c. Module Karaoke thông minh (Tính năng nổi bật)
- **Giao diện:** Hiển thị lời bài hát chạy chữ (Lyrics Scrolling) đồng bộ theo thời gian thực.
- **Xử lý âm thanh:**
    - **Pitch Visualizer:** Biểu đồ trực quan hóa cao độ giọng hát người dùng so với cao độ chuẩn của bài hát.
    - **Scoring System:** Hệ thống chấm điểm tự động (Thang điểm C -> SSS) dựa trên độ chính xác của cao độ và nhịp điệu.
    - **Recording:** Hỗ trợ thu âm giọng hát người dùng.

#### d. Module Minigame (Music Quiz)
Tích hợp 3 chế độ chơi tương tác để tăng tính giải trí:
1.  **Guess The Song:** Nghe đoạn nhạc dạo (Music Preview) và đoán tên bài hát trong thời gian giới hạn.
2.  **Finish The Lyrics:** Điền từ còn thiếu vào đoạn lời bài hát đang hiển thị.
3.  **Emoji Challenge:** Đoán tên bài hát dựa trên các biểu tượng Emoji gợi ý.
- **Cơ chế Game:** Tính điểm (Score), Chuỗi thắng (Streak), Đếm ngược thời gian (Timer).

#### e. Giao diện & Trải nghiệm (UI/UX)
- **Jetpack Compose:** 100% giao diện được viết bằng Toolkit khai báo mới nhất của Google.
- **Hiệu ứng:** Sử dụng Brush Gradient, Animation cho các màn hình Play, Karaoke để tạo cảm giác "không gian" (Deep Space theme).

### 3. Công nghệ lập trình
- **Ngôn ngữ:** Kotlin.
- **Kiến trúc:** MVVM (Model-View-ViewModel).
- **UI Framework:** Jetpack Compose (Material Design 3).
- **Media Engine:** Android Media3 (ExoPlayer).
- **Dependency Injection:** Dagger Hilt.
- **Xử lý bất đồng bộ:** Coroutines & Flow.

### 4. Hướng dẫn cài đặt
1. **Yêu cầu:** Android Studio bản mới nhất (Ladybug/Hedgehog), JDK 17.
2. **Cài đặt:**
   - Clone repo: `git clone https://github.com/tDn412/INT_3120_1_ProjectG6.git`
   - Mở project và đợi Gradle Sync.
   - Cấp quyền Micro (Record Audio) khi vào tính năng Karaoke.
