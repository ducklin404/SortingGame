-- bcrypt("123456") = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa.3j5xYh8BuBW1N9AEBmK3yYq"

INSERT INTO players (username, display_name, hashed_password)
VALUES
 ('u1', 'User One',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa.3j5xYh8BuBW1N9AEBmK3yYq'),
 ('u2', 'User Two',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa.3j5xYh8BuBW1N9AEBmK3yYq'),
 ('u3', 'User Three','$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa.3j5xYh8BuBW1N9AEBmK3yYq'),
 ('u4', 'User Four', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa.3j5xYh8BuBW1N9AEBmK3yYq'),
 ('u5', 'User Five', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa.3j5xYh8BuBW1N9AEBmK3yYq'),
 ('u6', 'User Six',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa.3j5xYh8BuBW1N9AEBmK3yYq'),
 ('u7', 'User Seven','$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa.3j5xYh8BuBW1N9AEBmK3yYq'),
 ('u8', 'User Eight','$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa.3j5xYh8BuBW1N9AEBmK3yYq'),
 ('u9', 'User Nine', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa.3j5xYh8BuBW1N9AEBmK3yYq'),
 ('u10','User Ten',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa.3j5xYh8BuBW1N9AEBmK3yYq');
