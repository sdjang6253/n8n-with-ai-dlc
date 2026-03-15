UPDATE shop_user.users SET password='$2a$10$qW0JCZI4nRW6/z/K0mGTwuf6Qk4P9gsubTbrc2ncs6Lg2QMsk8DAe' WHERE email IN ('alice@example.com','bob@example.com','carol@example.com','dave@example.com','eve@example.com');
SELECT email, LEFT(password,20) as pw FROM shop_user.users;
