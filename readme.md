# 🔨 Real-time Auction Platform Backend
![](assets/banner.png)
[![Java](https://img.shields.io/badge/Java-26-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![SQLite](https://img.shields.io/badge/SQLite-Database-blue.svg?style=flat-square&logo=sqlite)](https://www.sqlite.org/)
[![Reactive Streams](https://img.shields.io/badge/Reactive-Reactor-purple.svg?style=flat-square&logo=reactive-x)](https://projectreactor.io/)

Dự án này là hệ thống Backend cho sàn đấu giá trực tuyến thời gian thực (Real-time Auction Platform). Hệ thống cung cấp các API RESTful bảo mật và cơ chế phát dữ liệu thời gian thực (Server-Sent Events) giúp cập nhật biến động giá sản phẩm và số dư tài khoản người dùng ngay lập tức.

---

## 📌 Mục lục
1. [Tính năng nổi bật](#-tính-năng-nổi-bật)
2. [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
3. [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
4. [Cấu hình hệ thống](#-cấu-hình-hệ-thống)
5. [Hướng dẫn cài đặt & Chạy dự án](#-hướng-dẫn-cài-đặt--chạy-dự-án)
6. [Tài liệu API chi tiết](#-tài-liệu-api-chi-tiết)
7. [Ghi chú kỹ thuật về SQLite](#-ghi-chú-kỹ-thuật-về-sqlite)

---

## 🚀 Tính năng nổi bật

### 1. Quản lý người dùng & Bảo mật
*   **Đăng ký & Đăng nhập:** Bảo mật thông tin người dùng với cơ chế mã hóa mật khẩu.
*   **Xác thực JWT (JSON Web Tokens):** Cơ chế xác thực hai lớp với Access Token (ngắn hạn) và Refresh Token (dài hạn).
*   **Quản trị viên (Admin Panel):** Khóa (Ban) người dùng vi phạm và mở khóa (Unban) kèm đặt lại mật khẩu.

### 2. Quản lý sản phẩm đấu giá
*   **Đăng bán sản phẩm:** Người bán có thể đăng sản phẩm lên kèm giá khởi điểm, bước giá tối thiểu, giá mua đứt (Buy It Now), và thời gian kết thúc phiên.
*   **Hủy phiên đấu giá:** Người bán có thể tự hủy phiên đấu giá của mình, hoặc Admin có quyền force-cancel phiên đấu giá nếu phát hiện vi phạm.
*   **Phân trang danh sách:** Hỗ trợ tải phân trang cho danh sách sản phẩm đang hoạt động hoặc sản phẩm do người dùng cụ thể đăng bán.

### 3. Cơ chế Đấu giá linh hoạt
*   **Đặt cược thủ công (Manual Bid):** Người dùng đặt cược trực tiếp với số tiền hợp lệ (lớn hơn giá hiện tại + bước giá tối thiểu).
*   **Tự động đặt cược (Auto-Bid):** Người dùng cài đặt mức giới hạn tối đa, hệ thống sẽ tự động tăng giá cược khi có người khác trả giá cao hơn.
*   **Mua ngay (Buy It Now):** Mua đứt sản phẩm với mức giá định sẵn, phiên đấu giá sẽ kết thúc ngay lập tức.
*   **Thời gian bù giờ chống bắn tỉa (Anti-Sniping):** Khi có lượt đặt cược mới gần thời gian kết thúc, hệ thống sẽ tự động cộng thêm thời gian bù giờ (`extra_time` cấu hình trong file properties).

### 4. Phát trực tuyến dữ liệu thời gian thực (Real-time SSE)
*   **Biến động giá sản phẩm:** Sử dụng Server-Sent Events (SSE) qua Project Reactor (`Flux`) để cập nhật giá trực tiếp lên màn hình client mà không cần reload.
*   **Số dư tài khoản:** Đẩy thông tin biến động số dư ví cá nhân theo thời gian thực mỗi khi có giao dịch nạp tiền hoặc đặt cược.

---

## 🏗 Kiến trúc hệ thống

Dự án áp dụng mô hình kiến trúc phân lớp chuẩn (Layered Architecture) kết hợp với Reactive Streams (SSE) để xử lý các luồng dữ liệu thời gian thực.

<img width="1294" height="598" alt="image" src="https://github.com/user-attachments/assets/5d222ee5-5b5a-42ae-9ea2-4ad76938c09f" />


### Chi tiết các lớp:
*   **Controller Layer (`*Controller.java`):** Tiếp nhận HTTP Request từ client, thực hiện validate dữ liệu đầu vào thông qua các annotations `@Valid` và điều hướng tới Service tương ứng.
*   **Service Layer (`*Service.java`):** Lớp xử lý logic nghiệp vụ chính (tính toán bước giá, quản lý thời gian đấu giá, xử lý trừ tiền/hoàn tiền đặt cược cũ, tự động đặt giá).
*   **Repository Layer (`*Repository.java`):** Giao tiếp trực tiếp với cơ sở dữ liệu SQLite thông qua Spring Data JPA.

---

## 🛠 Công nghệ sử dụng

*   **Java 26:** Sử dụng các tính năng mới nhất của ngôn ngữ Java.
*   **Spring Boot 4.0.5:** Phiên bản Spring Boot hiện đại tối ưu hiệu năng.
*   **Spring Security & Spring Boot Starter Validation:** Đảm bảo an toàn thông tin và kiểm tra tính hợp lệ của dữ liệu đầu vào.
*   **Spring WebFlux & Project Reactor:** Hỗ trợ xử lý bất đồng bộ và cơ chế Reactive Streams để truyền tải luồng SSE thời gian thực.
*   **SQLite Database & Hibernate community dialect:** Lưu trữ dữ liệu dạng tệp nhẹ nhàng, dễ triển khai mà không cần cài đặt server DB cồng kềnh.
*   **JJWT (Java JWT):** Thư viện tạo và kiểm tra chữ ký token JWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`).
*   **Springdoc OpenAPI (Swagger UI):** Tự động phát sinh tài liệu API trực quan.

---

## ⚙ Cấu hình hệ thống

Các thiết lập quan trọng trong file [application.properties](file:///C:/Coding/GitHub/auctionbackendCS1/src/main/resources/application.properties):

```properties
# Cấu hình SQLite database (tạo file auctiondb.db ngay tại thư mục gốc dự án)
spring.datasource.url=jdbc:sqlite:auctiondb.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update

# Cấu hình Pool Connection cho SQLite (giới hạn tối đa 1 connection để tránh lỗi khóa ghi cơ sở dữ liệu)
spring.datasource.hikari.maximum-pool-size=1

# Cấu hình bảo mật JWT (thời gian tính bằng miliseconds)
jwt.secret=245cd2618f11d11d206e3131ee119341eb6f6926a35acefbcbfe1f05fb50fb20
jwt.expiration=360000          # 5 phút (Access Token)
jwt.refreshExpiration=604800000 # 7 ngày (Refresh Token)

# Cấu hình thời gian bù giờ cho đấu giá
max_extra_time=7200000 # Giới hạn thời gian bù giờ tối đa (2 giờ)
extra_time=360000      # Thời gian cộng thêm cho mỗi lượt cược cuối phiên (5 phút)
```

---

## 💻 Hướng dẫn cài đặt & Chạy dự án

### Yêu cầu hệ thống
*   Cài đặt **Java 26 hoặc cao hơn**.
*   Công cụ quản lý gói **Gradle**.

### Các bước khởi chạy
1.  **Clone mã nguồn dự án:**
    ```bash
    git clone https://github.com/your-username/auction-backend.git
    cd auction-backend
    ```
2.  **Xây dựng và chạy dự án thông qua Gradle:**
    ```bash
    ./gradlew bootRun
    ```
    *(Trên Windows, bạn có thể chạy bằng lệnh: `gradlew.bat bootRun`)*

3.  **Truy cập API Swagger UI:**
    Sau khi ứng dụng khởi chạy thành công trên cổng `8080`, hãy truy cập đường link sau để xem tài liệu chi tiết và chạy thử nghiệm các API:
    [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## 📑 Tài liệu API chi tiết

Tất cả các API ngoại trừ đăng nhập, đăng ký và làm mới token đều yêu cầu Header: `Authorization: Bearer <JWT_ACCESS_TOKEN>`.

### 1. Nhóm API Xác thực & Người dùng (`/`)
| Phương thức | API | Mô tả | Authentication |
|---|---|---|---|
| `POST` | `/register` | Đăng ký tài khoản người dùng mới | Không yêu cầu |
| `POST` | `/login` | Đăng nhập tài khoản, nhận Access Token & Refresh Token | Không yêu cầu |
| `POST` | `/refresh` | Làm mới Access Token đã hết hạn bằng Refresh Token | Không yêu cầu |
| `POST` | `/logout` | Đăng xuất hệ thống và thu hồi token hiện tại | Yêu cầu JWT |
| `GET` | `/users/me` | Lấy thông tin chi tiết hồ sơ cá nhân | Yêu cầu JWT |
| `GET` | `/users/me/balance` | Xem số dư ví tiền hiện tại | Yêu cầu JWT |
| `POST` | `/users/me/deposit` | Nạp tiền vào tài khoản ví cá nhân | Yêu cầu JWT |

### 2. Nhóm API Quản lý Sản phẩm (`/items`)
| Phương thức | API | Mô tả | Authentication |
|---|---|---|---|
| `POST` | `/items` | Đăng bán một mặt hàng đấu giá mới | Yêu cầu JWT |
| `GET` | `/items` | Lấy danh sách các sản phẩm đang đấu giá (hỗ trợ phân trang) | Yêu cầu JWT |
| `GET` | `/items/{itemId}` | Lấy chi tiết thông tin của sản phẩm qua ID | Yêu cầu JWT |
| `POST` | `/items/cancel/{itemId}` | Người bán tự hủy phiên đấu giá sản phẩm của mình | Yêu cầu JWT |
| `GET` | `/items/listings/{username}` | Lấy danh sách sản phẩm đăng bán của một user cụ thể | Yêu cầu JWT |
| `GET` | `/items/all` | Lấy toàn bộ sản phẩm trên hệ thống (Dành cho Dev) | Yêu cầu JWT |

### 3. Nhóm API Đấu giá & Theo dõi (Real-time SSE)
| Phương thức | API | Mô tả | Authentication |
|---|---|---|---|
| `POST` | `/bid` | Đặt giá cược thủ công cho một mặt hàng | Yêu cầu JWT |
| `POST` | `/auto-bid` | Thiết lập cấu hình tự động đấu giá (Auto-Bid) | Yêu cầu JWT |
| `POST` | `/buy-now/{itemId}` | Mua đứt sản phẩm ngay theo mức giá Buy It Now | Yêu cầu JWT |
| `GET` | `/me/bids` | Xem lịch sử các lượt đặt cược của bản thân (phân trang) | Yêu cầu JWT |
| `GET` | `/me/wins` | Lấy danh sách các sản phẩm đã thắng cuộc | Yêu cầu JWT |
| `GET` | `/bids/{itemId}/bids` | Xem danh sách các lượt đặt cược của một sản phẩm cụ thể | Yêu cầu JWT |
| `GET` | `/item/status/{itemId}` | Xem trạng thái đấu giá hiện tại của sản phẩm | Yêu cầu JWT |
| `GET` | `/items/stream/{itemId}` | **SSE Stream:** Lắng nghe biến động giá sản phẩm thời gian thực | Yêu cầu JWT |
| `GET` | `/{username}/balance/stream`| **SSE Stream:** Lắng nghe biến động số dư tài khoản thời gian thực | Yêu cầu JWT |

### 4. Nhóm API Quản trị viên (`/admin`)
| Phương thức | API | Mô tả | Vai trò |
|---|---|---|---|
| `POST` | `/admin/ban` | Khóa tài khoản người dùng vi phạm | Admin |
| `POST` | `/admin/unban` | Mở khóa tài khoản người dùng & đặt lại mật khẩu mới | Admin |
| `POST` | `/admin/cancel/{itemId}` | Hủy bỏ một phiên đấu giá sản phẩm bất kỳ trên hệ thống | Admin |

---

## 💾 Ghi chú kỹ thuật về SQLite

Mặc dù SQLite cực kỳ hữu dụng cho môi trường kiểm thử và phát triển nhanh do không yêu cầu cài đặt máy chủ, nó có một số hạn chế quan trọng về ghi đồng thời (write concurrency). 

Để tránh hiện tượng khóa cơ sở dữ liệu (`database is locked`) khi thực hiện ghi đồng thời cao (nhiều lượt đặt cược diễn ra trong cùng một thời điểm), dự án đã thực hiện cấu hình giới hạn kết nối:
```properties
spring.datasource.hikari.maximum-pool-size=1
```
> [!IMPORTANT]
> Việc đặt `maximum-pool-size=1` sẽ giúp tuần tự hóa tất cả các thao tác ghi vào SQLite, ngăn ngừa tranh chấp luồng ghi. Đối với môi trường Production chịu tải lớn, khuyến khích chuyển đổi cấu hình sang cơ sở dữ liệu có khả năng chịu tải cao hơn như **PostgreSQL** hoặc **MySQL**.
