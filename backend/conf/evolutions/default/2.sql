# --- !Ups

INSERT INTO "category"("name") VALUES("komputery");
INSERT INTO "subcategory"("name", category) VALUES("komputery gamingowe", 1);
INSERT INTO "manufacturer"("name") VALUES("asus");
INSERT INTO "product"("name", "description", "price", "amount", "manufacturer", "category", "subcategory") VALUES("strx ga15", "superfast", 9999, 2, 1, 1, 1);
INSERT INTO "user"("id", "firstname", "lastname", "email", "password", "role") VALUES("abc123", "janusz", "kowalski", "janusz@janusz.hu", "1337", "ADMIN");
INSERT INTO "delivery"("name", "price") VALUES("inpost", 15);
INSERT INTO "payment"("name") VALUES("blik");
INSERT INTO "order_"("price", "date", "address", "sent", "user", "payment", "delivery", "paid", "packageNr") VALUES(9999, "2020-04-17", "ul. slonecznikowa 2, 38-929 bombolewo", 1, "abc123", 1, 1, 0, "");
INSERT INTO "orderdetail"("price", "order_", "product", "amount") VALUES (9999, 1, 1, 1);

# --- !Downs

DELETE FROM "category"("name") WHERE name="komputery";
DELETE FROM "subcategory" WHERE name="komputery gamingowe";
DELETE FROM "manufacturer" WHERE name="asus";
DELETE FROM "product" WHERE name="strix ga15";
DELETE FROM "user" WHERE id="abc123";
DELETE FROM "delivery" WHERE name="inpost";
DELETE FROM "payment" WHERE name="blik";
DELETE FROM "order_" WHERE address="ul. slonecznikowa 2, 38-929 bombolewo" AND date="2020-04-17";
DELETE FROM "orderdetail" WHERE order_=1