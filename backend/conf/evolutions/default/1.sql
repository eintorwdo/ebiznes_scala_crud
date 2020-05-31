
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
 "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 "name" VARCHAR NOT NULL,
 "email" VARCHAR NOT NULL,
 "password" VARCHAR NOT NULL
);

CREATE TABLE "review" (
 "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 "description" TEXT NOT NULL,
 "user" INTEGER,
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
 "user" INTEGER NOT NULL,
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
DROP TABLE "review"
DROP TABLE "delivery"
DROP TABLE "payment"
DROP TABLE "order_"
DROP TABLE "orderdetail"