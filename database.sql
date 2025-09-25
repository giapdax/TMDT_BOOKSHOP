CREATE DATABASE  IF NOT EXISTS `book_shop1` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `book_shop1`;
-- MySQL dump 10.13  Distrib 8.0.25, for Win64 (x86_64)
--
-- Host: localhost    Database: book_shop1
-- ------------------------------------------------------
-- Server version	8.0.25

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


create table categories
(
    category_id    bigint auto_increment
        primary key,
    category_image varchar(255) null,
    category_name  nvarchar(255) null,
    status         bit          null
);

create table login_ip_lock
(
    id             bigint auto_increment
        primary key,
    failed_attempt int         not null,
    ip             varchar(45) not null,
    last_failed_at datetime(6) null,
    locked_until   datetime(6) null
);

create index idx_ip
    on login_ip_lock (ip);

create table nxb
(
    id     bigint auto_increment
        primary key,
    name   nvarchar(255) null,
    status bit          null
);

create table products
(
    product_id    bigint auto_increment
        primary key,
    description   nvarchar(1000) null,
    discount      int           not null,
    entered_date  datetime      null,
    price         double        not null,
    product_image varchar(255)  null,
    product_name  nvarchar(255)  null,
    quantity      int           not null,
    status        bit           null,
    category_id   bigint        null,
    favorite      bit           not null,
    nxb_id        bigint        null,
    constraint FKog2rp4qthbtt2lfyhfo32lsw9
        foreign key (category_id) references categories (category_id),
    constraint FKp94s74eab94uf5uvkw9maod14
        foreign key (nxb_id) references nxb (id)
);

create table role
(
    id   bigint auto_increment
        primary key,
    name nvarchar(255) null
);

create table user
(
    user_id        bigint auto_increment
        primary key,
    avatar         varchar(255) null,
    email          varchar(255) null,
    name           nvarchar(255) null,
    password       varchar(255) null,
    register_date  date         null,
    status         bit          null,
    failed_attempt int          not null,
    last_login_at  datetime(6)  null,
    locked_until   datetime(6)  null,
    phone          varchar(20)  null,
    username       varchar(49)  null,
    constraint UKob8kqyqqgmefl0aco34akdtpe
        unique (email),
    constraint ux_user_email
        unique (email),
    constraint ux_user_username
        unique (username)
);

create table carts
(
    cart_id    bigint auto_increment
        primary key,
    created_at datetime(6)  not null,
    expires_at datetime(6)  null,
    status     varchar(255) not null,
    updated_at datetime(6)  not null,
    user_id    bigint       not null,
    constraint FKpay9408fi1tlnkqv3fhetr6hy
        foreign key (user_id) references user (user_id)
);

create table cart_items
(
    cart_item_id bigint auto_increment
        primary key,
    created_at   datetime(6) not null,
    last_touch   datetime(6) null,
    quantity     int         not null,
    unit_price   double      not null,
    updated_at   datetime(6) not null,
    cart_id      bigint      not null,
    product_id   bigint      not null,
    constraint ux_cart_product
        unique (cart_id, product_id),
    constraint FK1re40cjegsfvw58xrkdp6bac6
        foreign key (product_id) references products (product_id),
    constraint FKpcttvuq4mxppo8sxggjtn5i2c
        foreign key (cart_id) references carts (cart_id)
);

create table favorites
(
    favorite_id bigint auto_increment
        primary key,
    product_id  bigint null,
    user_id     bigint null,
    constraint FK1uphh0glinnprjknyl68k1hw1
        foreign key (user_id) references user (user_id),
    constraint FK6sgu5npe8ug4o42bf9j71x20c
        foreign key (product_id) references products (product_id)
);

create table orders
(
    order_id   bigint auto_increment
        primary key,
    address    nvarchar(255) null,
    amount     double       null,
    order_date datetime     null,
    phone      varchar(255) null,
    status     int          not null,
    user_id    bigint       null,
    constraint FKel9kyl84ego2otj2accfd8mr7
        foreign key (user_id) references user (user_id)
);

create table order_details
(
    order_detail_id bigint auto_increment
        primary key,
    price           double null,
    quantity        int    not null,
    order_id        bigint null,
    product_id      bigint null,
    constraint FK4q98utpd73imf4yhttm3w0eax
        foreign key (product_id) references products (product_id),
    constraint FKjyu2qbqt8gnvno9oe9j2s2ldk
        foreign key (order_id) references orders (order_id)
);

create table comments
(
    id              bigint auto_increment
        primary key,
    content         nvarchar(255) null,
    rate_date       datetime     null,
    rating          double       null,
    order_detail_id bigint       null,
    product_id      bigint       null,
    user_id         bigint       null,
    constraint FK6uv0qku8gsu6x1r2jkrtqwjtn
        foreign key (product_id) references products (product_id),
    constraint FKfwepd0s8syqc9s06xnqa7mdwf
        foreign key (order_detail_id) references order_details (order_detail_id),
    constraint FKqi14bvepnwtjbbaxm7m4v44yg
        foreign key (user_id) references user (user_id)
);

create table users_roles
(
    user_id bigint not null,
    role_id bigint not null,
    constraint FKgd3iendaoyh04b95ykqise6qh
        foreign key (user_id) references user (user_id),
    constraint FKt4v0rrweyk393bdgt107vdx0x
        foreign key (role_id) references role (id)
);

INSERT INTO nxb (name, status) VALUES
('Trẻ', 1),
('Thanh Niên', 1),
('Văn Học', 1),
('Hội Nhà Văn', 1),
('Hà Nội', 1),
('Thế Giới', 1);

INSERT INTO categories (category_name, category_image, status) VALUES
('Sách Trinh Thám', 'trinhtham.jpg', 1),
('Sách Tiểu Thuyết - Truyện', 'tieuthuyet.jpg', 1),
('Sách Văn Học', 'vanhoc.jpg', 1),
('Sách Xã Hội Lịch Sử', 'xahoi.jpg', 1),
('Sách Khoa Học - Công Nghệ', 'khoahoc.jpg', 1),
('Sách Chính Trị Pháp Luật', 'chinhtri.jpg', 1);

INSERT INTO products (product_name, description, product_image, price, quantity, discount, status, favorite, category_id, nxb_id, entered_date) VALUES
('Bồ Câu Chung Mái Vòm', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'bo-cau-chung-mai-vom.jpg', 85000, 100, 10, 1, 0, 1, 1, NOW()),
('Cánh Đồng Bất Tận', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'canh-dong-bat-tan.jpg', 99000, 100, 0, 1, 0, 2, 2, NOW()),
('Cây Cam Ngọt Của Tôi', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'cay-cam-ngot-cua-toi.jpg', 120000, 100, 20, 1, 0, 3, 3, NOW()),
('Chạy Trốn Mặt Trời', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'chay-tron-mat-troi.jpg', 75000, 100, 15, 1, 0, 4, 4, NOW()),
('Chiếc Lá Cuối Cùng', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'chiec-la-cuoi-cung.jpg', 67000, 100, 0, 1, 0, 5, 5, NOW()),
('Cho Tôi Một Vé Về Tuổi Thơ', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'cho-toi-mot-ve-ve-tuoi-tho.jpg', 88000, 100, 30, 1, 0, 6, 6, NOW()),
('Con Hoang', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'con-hoang.jpg', 110000, 100, 10, 1, 0, 1, 2, NOW()),
('Cung Đàn Báo Oán', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'cung-dan-bao-oan.jpg', 95000, 100, 0, 1, 0, 1, 3, NOW()),
('Cuộc Sống Bí Mặt Của Nhà Văn', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'cuoc-song-bi-mat-cua-nha-van.jpg', 78000, 100, 15, 1, 0, 4, 4, NOW()),
('Đất Rừng Phương Nam', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'dat-rung-phuong-nam.jpg', 125000, 100, 20, 1, 0, 1, 5, NOW()),
('Điều Kỳ Diệu Của Tiệm Tạp Hóa', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'dieu-ky-dieu-cua-tiem-tap-hoa.jpg', 99000, 100, 0, 1, 0, 5, 6, NOW()),
('Đôi Lứa Xứng Đôi', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'doi-lua-xung-doi.jpg', 65000, 100, 15, 1, 0, 6, 1, NOW()),
('Gió Đầu Mùa', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'gio-dau-mua.jpg', 78000, 100, 30, 1, 0, 1, 2, NOW()),
('Hãy Chăm Sóc Mẹ', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'hay-cham-soc-me.jpg', 132000, 100, 10, 1, 0, 3, 3, NOW()),
('Khi Loài Cá Biến Mất', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'khi-loai-ca-bien-mat.jpg', 105000, 100, 20, 1, 0, 2, 4, NOW()),
('Kiếp Nào Ta Cũng Tìm Thấy Nhau', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'kiep-nao-ta-cung-tim-thay-nhau.jpg', 98000, 100, 15, 1, 0, 4, 5, NOW()),
('Làm Đĩ', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'lam-di.jpg', 87000, 100, 0, 1, 0, 6, 6, NOW()),
('Lều Chõng', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'leu-chong.jpg', 91000, 100, 30, 1, 0, 2, 1, NOW()),
('Mình Chỉ Là Người Bình Thường', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'minh-chi-la-nguoi-binh-thuong.jpg', 113000, 100, 10, 1, 0, 5, 2, NOW()),
('Mình Nói Gì Khi Nói Về Hạnh Phúc', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'minh-noi-gi-khi-noi-ve-hanh-phuc.jpg', 99000, 100, 15, 1, 0, 4, 3, NOW()),
('Mộ Đóm Đóm', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'mo-dom-dom.jpg', 73000, 100, 20, 1, 0, 1, 4, NOW()),
('Một Thoáng Ta Rực Rỡ Ở Nhân Gian', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'mot-thoang-ta-ruc-ro-o-nhan-gian.jpg', 122000, 100, 0, 1, 0, 3, 5, NOW()),
('Mùa Hè Không Tên', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'mua-he-khong-ten.jpg', 88000, 100, 30, 1, 0, 1, 6, NOW()),
('Tàn Ngày Để Lại', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'tan-ngay-de-lai.jpg', 97000, 100, 15, 1, 0, 2, 1, NOW()),
('Ngôi Nhà Ngàn Tấm Gương', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'ngoi-nha-ngan-tam-guong.jpg', 109000, 100, 20, 1, 0, 4, 2, NOW()),
('Nhà Giả Kim', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'nha-gia-kim.jpg', 89000, 100, 10, 1, 0, 5, 3, NOW()),
('Nhật Ký trong Tù', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'nhat-ky-trong-tu.jpg', 132000, 100, 0, 1, 0, 6, 4, NOW()),
('Những Đêm Trăng', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'nhung-dem-trang.jpg', 75000, 100, 15, 1, 0, 5, 5, NOW()),
('Những Người Khốn Khổ', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'nhung-nguoi-khon-kho.jpg', 145000, 100, 30, 1, 0, 4, 6, NOW()),
('Số Đỏ', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'so-do.jpg', 96000, 100, 20, 1, 0, 3, 1, NOW()),
('Tắt Đèn', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'tat-den.jpg', 87000, 100, 10, 1, 0, 5, 2, NOW()),
('Tôi Đi Học', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'toi-di-hoc.jpg', 65000, 100, 0, 1, 0, 4, 3, NOW()),
('Về Nhà Với Mẹ', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 've-nha-voi-me.jpg', 99000, 100, 15, 1, 0, 5, 4, NOW()),
('Vì Thương', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'vi-thuong.jpg', 78000, 100, 20, 1, 0, 2, 5, NOW()),
('Vợ Nhặt', 'THÔNG TIN SÁCH - Nhà phát hành: LionBooks - Nhà xuất bản: NXB Hà Nội - Tác giả: Ngô Hương Thảo; minh họa: Thanh Phan - Kích thước: 23x23 cm - Số trang: 64 - Khối lượng: 300g - Hình thức bìa: Bìa cứng Giới Thiệu Nội Dung: KHIẾU HÀI HƯỚC GIÚP GÌ CHO BÉ? Những em bé có khiếu hài hước thường mang trong mình cả 2 chỉ số EQ và IQ cao. Không chỉ thông minh, lém lỉnh mà các em bé còn có sự nhạy cảm nhất định với sự sáng tạo, sự bất ngờ và cảm xúc của người đối diện. Lionbooks giới thiệu tới ba mẹ cuốn hay nhất, thú vị nhất dành cho các em bé nhằm phát triển khiếu hài hước ở trẻ: CUỘC PHIÊU LƯU ĐẦU TIÊN CỦA BẦY VỊT - Làm quen với các thành ngữ, tục ngữ để mở rộng vốn từ và cách diễn đạt, giúp con nói hay, nói giỏi. - Khám phá cách kể chuyện tự do, phá cách, sáng tạo nhưng vẫn gần gũi như trong giao tiếp hàng ngày. - Tranh minh họa siêu ngộ nghĩnh và đáng yêu, chinh phục các độc giả nhí ngay từ ấn tượng thị giác đầu tiên!', 'vo-nhat.jpg', 107000, 100, 30, 1, 0, 6, 6, NOW());

INSERT INTO role (name)
VALUES 
('ROLE_ADMIN'),
('ROLE_STAFF'),
('ROLE_USER');

INSERT INTO user (avatar, email, name, password, register_date, status, failed_attempt, last_login_at, locked_until, phone, username)
VALUES
-- ADMIN
('user.png', 'ttthinh2904@gmail.com', N'Admin', '$2a$10$jD5bEA9mwzzBOprN1eU3KOdTXHUeDch1vRVHcAJ0T2xQjv7Yc/qNa', CURDATE(), 1, 0, NOW(), NULL, '0338515037', 'admin'), -- Matkhau1234@

-- STAFF
('user.png', 'iva2jfxjtg@dcpa.net', N'Staff', '$2a$10$jD5bEA9mwzzBOprN1eU3KOdTXHUeDch1vRVHcAJ0T2xQjv7Yc/qNa', CURDATE(), 1, 0, NOW(), NULL, '0928823211', 'staff'),  -- Matkhau1234@

-- USER
('user.png', 'ttthinh290404@gmail.com', N'User', '$2a$10$jD5bEA9mwzzBOprN1eU3KOdTXHUeDch1vRVHcAJ0T2xQjv7Yc/qNa', CURDATE(), 1, 0, NOW(), NULL, '0987654321', 'user'); -- Matkhau1234@

-- Gán quyền cho 3 user
INSERT INTO users_roles (user_id, role_id)
VALUES
(1, 1), -- admin → ROLE_ADMIN
(2, 2), -- staff → ROLE_STAFF
(3, 3); -- user → ROLE_USER



