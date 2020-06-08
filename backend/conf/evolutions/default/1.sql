
# --- !Ups

CREATE TABLE "category" (
 "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 "name" VARCHAR NOT NULL
);

CREATE TABLE "subcategory" (
 "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 "name" VARCHAR NOT NULL,
 "category" INTEGER NOT NULL,
 FOREIGN KEY(category) references category(id)
);

CREATE TABLE "manufacturer" (
 "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 "name" VARCHAR NOT NULL
);

CREATE TABLE "product" (
 "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 "name" VARCHAR NOT NULL,
 "description" TEXT NOT NULL,
 "price" INTEGER NOT NULL,
 "amount" INTEGER NOT NULL,
 "manufacturer" INTEGER,
 "category" INTEGER,
 "subcategory" INTEGER,
 FOREIGN KEY(manufacturer) references manufacturer(id),
 FOREIGN KEY(category) references category(id),
 FOREIGN KEY(subcategory) references subcategory(id)
);

CREATE TABLE "user" (
 "id" VARCHAR NOT NULL PRIMARY KEY,
 "firstname" VARCHAR,
 "lastname" VARCHAR,
 "email" VARCHAR NOT NULL,
 "password" VARCHAR,
 "role" VARCHAR NOT NULL
);

CREATE TABLE "login_info" (
 "id" VARCHAR NOT NULL PRIMARY KEY,
 "provider_id" VARCHAR NOT NULL,
 "provider_key" VARCHAR NOT NULL
);

CREATE TABLE "user_login_info" (
 "user_id" VARCHAR NOT NULL,
 "login_info_id" VARCHAR NOT NULL,
 FOREIGN KEY(user_id) references user(id),
 FOREIGN KEY(login_info_id) references login_info(id)
);

CREATE TABLE "oauth2_info" (
  "id" VARCHAR NOT NULL PRIMARY KEY,
  "access_token" VARCHAR NOT NULL,
  "token_type" VARCHAR,
  "expires_in" INTEGER,
  "refresh_token" VARCHAR,
  "login_info_id" VARCHAR NOT NULL,
  FOREIGN KEY (login_info_id) REFERENCES login_info(id)
);

CREATE TABLE "review" (
 "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 "description" TEXT NOT NULL,
 "user" VARCHAR,
 "product" INTEGER NOT NULL,
 "date" TEXT NOT NULL,
 FOREIGN KEY(user) references user(id),
 FOREIGN KEY(product) references product(id)
);

CREATE TABLE "delivery" (
 "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 "name" VARCHAR NOT NULL,
 "price" INTEGER NOT NULL
);

CREATE TABLE "payment" (
 "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 "name" VARCHAR NOT NULL
);

CREATE TABLE "order_" (
 "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 "price" INTEGER NOT NULL,
 "date" TEXT NOT NULL,
 "address" TEXT NOT NULL,
 "sent" TINYINT NOT NULL,
 "user" VARCHAR NOT NULL,
 "payment" INTEGER,
 "delivery" INTEGER,
 "paid" INTEGER,
 "packageNr" TEXT,
 FOREIGN KEY(user) references user(id),
 FOREIGN KEY(payment) references payment(id),
 FOREIGN KEY(delivery) references delivery(id)
);

CREATE TABLE "orderdetail" (
 "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 "price" INTEGER NOT NULL,
 "order_" INTEGER NOT NULL,
 "product" INTEGER,
 "amount" INTEGER,
 FOREIGN KEY(order_) references order_(id),
 FOREIGN KEY(product) references product(id)
);

# --- !Downs

DROP TABLE "category"
DROP TABLE "subcategory"
DROP TABLE "manufacturer"
DROP TABLE "product"
DROP TABLE "user"
DROP TABLE "login_info"
DROP TABLE "user_login_info"
DROP TABLE "oauth2_info"
DROP TABLE "review"
DROP TABLE "delivery"
DROP TABLE "payment"
DROP TABLE "order_"
DROP TABLE "orderdetail"