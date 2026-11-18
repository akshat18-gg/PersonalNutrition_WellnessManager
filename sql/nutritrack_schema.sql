-- ==========================================================
-- NUTRITRACK - Personal Nutrition & Wellness Manager
-- Database Schema & Sample Records
--
-- WARNING: this script DROPS and re-creates all six tables,
-- so any data already in the nutritrack database is replaced.
--
-- Demo login:  student@nutritrack.com  /  password123
-- ==========================================================

-- 1. CREATE DATABASE
CREATE DATABASE IF NOT EXISTS nutritrack;
USE nutritrack;

-- 2. DROP TABLES IN REVERSE ORDER (To allow clean re-execution)
DROP TABLE IF EXISTS water_intake;
DROP TABLE IF EXISTS meal_items;
DROP TABLE IF EXISTS meals;
DROP TABLE IF EXISTS food;
DROP TABLE IF EXISTS nutrition_goals;
DROP TABLE IF EXISTS users;

-- ==========================================================
-- 3. CREATE TABLES WITH PRIMARY & FOREIGN KEYS
-- ==========================================================

-- Table 1: USERS
-- password holds a SHA-256 hash (64 hex characters), never the real password.
-- gender and activity_level are needed by the Smart Goal Calculator.
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    age INT,
    gender VARCHAR(10),
    height DECIMAL(5,2),
    weight DECIMAL(5,2),
    activity_level VARCHAR(20)
);

-- Table 2: NUTRITION GOALS (users 1:N nutrition_goals)
CREATE TABLE nutrition_goals (
    goal_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    target_calories INT,
    target_protein DECIMAL(7,2),
    target_carbs DECIMAL(7,2),
    target_fat DECIMAL(7,2),
    target_water INT,
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- Table 3: FOOD (Master library of food items, values per 100 g)
CREATE TABLE food (
    food_id INT PRIMARY KEY AUTO_INCREMENT,
    food_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    calories DECIMAL(7,2),
    protein DECIMAL(7,2),
    carbs DECIMAL(7,2),
    fat DECIMAL(7,2)
);

-- Table 4: MEALS (users 1:N meals)
CREATE TABLE meals (
    meal_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    meal_type VARCHAR(30) NOT NULL,
    meal_date DATE NOT NULL,
    CONSTRAINT fk_meals_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- Table 5: MEAL ITEMS (meals 1:N meal_items, food 1:N meal_items)
CREATE TABLE meal_items (
    meal_item_id INT PRIMARY KEY AUTO_INCREMENT,
    meal_id INT NOT NULL,
    food_id INT NOT NULL,
    quantity DECIMAL(7,2),
    CONSTRAINT fk_meal_items_meal FOREIGN KEY (meal_id)
        REFERENCES meals(meal_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_meal_items_food FOREIGN KEY (food_id)
        REFERENCES food(food_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- Table 6: WATER INTAKE (users 1:N water_intake)
CREATE TABLE water_intake (
    water_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    amount_ml INT NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_water_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- ==========================================================
-- 4. INSERT SAMPLE DEMONSTRATION RECORDS
-- ==========================================================

-- Demo user. SHA2('password123', 256) gives the same 64-character hash
-- that util.PasswordUtil produces in Java, so the login check matches.
INSERT INTO users (user_id, name, email, password, age, gender, height, weight, activity_level)
VALUES (1, 'Aritra', 'student@nutritrack.com', SHA2('password123', 256), 20, 'Male', 175.0, 68.5, 'Moderate');

-- Daily goals for the demo user
INSERT INTO nutrition_goals (user_id, target_calories, target_protein, target_carbs, target_fat, target_water)
VALUES (1, 2200, 120.00, 250.00, 70.00, 2500);

-- Food library (all values per 100 g). IDs are written out because the
-- sample meal items below refer to them.
INSERT INTO food (food_id, food_name, category, calories, protein, carbs, fat) VALUES
( 1, 'Oatmeal',        'Grains',           389.00, 16.90, 66.30,  6.90),
( 2, 'Eggs',           'Dairy & Poultry',  155.00, 13.00,  1.10, 11.00),
( 3, 'Chicken Breast', 'Meat',             165.00, 31.00,  0.00,  3.60),
( 4, 'Rice',           'Grains',           130.00,  2.70, 28.20,  0.30),
( 5, 'Banana',         'Fruits',            89.00,  1.10, 22.80,  0.30),
( 6, 'Apple',          'Fruits',            52.00,  0.30, 13.80,  0.20),
( 7, 'Paneer',         'Dairy',            265.00, 18.30,  1.20, 20.80),
( 8, 'Dal',            'Pulses',           116.00,  9.00, 20.00,  0.40),
( 9, 'Roti',           'Breads',           120.00,  3.50, 22.00,  2.00),
(10, 'Salad',          'Vegetables',        35.00,  1.50,  6.50,  0.30),
-- More common Indian foods
(11, 'Idli',           'South Indian',     146.00,  4.50, 30.00,  0.70),
(12, 'Dosa',           'South Indian',     168.00,  3.90, 29.00,  3.70),
(13, 'Sambar',         'South Indian',      60.00,  3.00,  9.00,  1.50),
(14, 'Poha',           'Breakfast',        130.00,  2.60, 23.00,  3.00),
(15, 'Upma',           'Breakfast',        155.00,  3.50, 22.00,  5.50),
(16, 'Aloo Paratha',   'Breads',           210.00,  5.00, 30.00,  8.00),
(17, 'Rajma',          'Pulses',           127.00,  8.70, 22.80,  0.50),
(18, 'Chole',          'Pulses',           164.00,  8.90, 27.40,  2.60),
(19, 'Curd',           'Dairy',             61.00,  3.50,  4.70,  3.30),
(20, 'Milk',           'Dairy',             62.00,  3.20,  4.80,  3.30),
(21, 'Soya Chunks',    'Pulses',           345.00, 52.00, 33.00,  0.50),
(22, 'Peanuts',        'Nuts',             567.00, 25.80, 16.10, 49.20),
(23, 'Almonds',        'Nuts',             579.00, 21.20, 21.60, 49.90),
(24, 'Moong Sprouts',  'Pulses',            30.00,  3.00,  5.90,  0.20),
(25, 'Khichdi',        'Grains',           120.00,  4.00, 20.00,  2.50);

-- ----------------------------------------------------------
-- Meals for the last 7 days (6 days ago ... today).
-- CURDATE() - INTERVAL n DAY keeps the data "recent" whenever
-- the script is run, so the weekly chart and streak always look full.
-- Today has only breakfast and lunch, so the dashboard still shows
-- remaining goals and food suggestions.
-- ----------------------------------------------------------
INSERT INTO meals (meal_id, user_id, meal_type, meal_date) VALUES
-- 6 days ago
( 1, 1, 'Breakfast', CURDATE() - INTERVAL 6 DAY),
( 2, 1, 'Lunch',     CURDATE() - INTERVAL 6 DAY),
( 3, 1, 'Snack',     CURDATE() - INTERVAL 6 DAY),
( 4, 1, 'Dinner',    CURDATE() - INTERVAL 6 DAY),
-- 5 days ago
( 5, 1, 'Breakfast', CURDATE() - INTERVAL 5 DAY),
( 6, 1, 'Lunch',     CURDATE() - INTERVAL 5 DAY),
( 7, 1, 'Snack',     CURDATE() - INTERVAL 5 DAY),
( 8, 1, 'Dinner',    CURDATE() - INTERVAL 5 DAY),
-- 4 days ago (a heavy day, goes over the calorie goal)
( 9, 1, 'Breakfast', CURDATE() - INTERVAL 4 DAY),
(10, 1, 'Lunch',     CURDATE() - INTERVAL 4 DAY),
(11, 1, 'Snack',     CURDATE() - INTERVAL 4 DAY),
(12, 1, 'Dinner',    CURDATE() - INTERVAL 4 DAY),
-- 3 days ago (a light day, no snack)
(13, 1, 'Breakfast', CURDATE() - INTERVAL 3 DAY),
(14, 1, 'Lunch',     CURDATE() - INTERVAL 3 DAY),
(15, 1, 'Dinner',    CURDATE() - INTERVAL 3 DAY),
-- 2 days ago
(16, 1, 'Breakfast', CURDATE() - INTERVAL 2 DAY),
(17, 1, 'Lunch',     CURDATE() - INTERVAL 2 DAY),
(18, 1, 'Snack',     CURDATE() - INTERVAL 2 DAY),
(19, 1, 'Dinner',    CURDATE() - INTERVAL 2 DAY),
-- yesterday
(20, 1, 'Breakfast', CURDATE() - INTERVAL 1 DAY),
(21, 1, 'Lunch',     CURDATE() - INTERVAL 1 DAY),
(22, 1, 'Snack',     CURDATE() - INTERVAL 1 DAY),
(23, 1, 'Dinner',    CURDATE() - INTERVAL 1 DAY),
-- today
(24, 1, 'Breakfast', CURDATE()),
(25, 1, 'Lunch',     CURDATE());

-- Meal items: (meal_id, food_id, quantity in grams)
INSERT INTO meal_items (meal_id, food_id, quantity) VALUES
-- 6 days ago
( 1,  1,  80.00), ( 1, 20, 200.00), ( 1,  5, 100.00),                  -- Oatmeal, Milk, Banana
( 2,  4, 250.00), ( 2,  8, 200.00), ( 2, 10, 100.00), ( 2, 19, 100.00), -- Rice, Dal, Salad, Curd
( 3, 22,  30.00), ( 3,  6, 150.00),                                    -- Peanuts, Apple
( 4,  9, 150.00), ( 4,  7, 120.00), ( 4, 10, 100.00),                  -- Roti, Paneer, Salad
-- 5 days ago
( 5, 11, 200.00), ( 5, 13, 150.00),                                    -- Idli, Sambar
( 6,  4, 250.00), ( 6, 17, 250.00), ( 6, 19, 150.00),                  -- Rice, Rajma, Curd
( 7, 23,  30.00), ( 7,  5, 120.00),                                    -- Almonds, Banana
( 8,  9, 180.00), ( 8,  3, 200.00), ( 8, 10, 100.00),                  -- Roti, Chicken Breast, Salad
-- 4 days ago
( 9, 16, 250.00), ( 9, 19, 150.00),                                    -- Aloo Paratha, Curd
(10,  4, 300.00), (10, 18, 250.00), (10, 10, 100.00),                  -- Rice, Chole, Salad
(11, 22,  50.00), (11, 20, 250.00),                                    -- Peanuts, Milk
(12,  9, 180.00), (12,  7, 150.00), (12,  8, 150.00),                  -- Roti, Paneer, Dal
-- 3 days ago
(13, 14, 250.00), (13, 20, 200.00),                                    -- Poha, Milk
(14, 25, 350.00), (14, 19, 150.00),                                    -- Khichdi, Curd
(15,  9, 150.00), (15,  8, 200.00), (15, 10, 150.00),                  -- Roti, Dal, Salad
-- 2 days ago
(16, 12, 200.00), (16, 13, 150.00), (16,  2, 100.00),                  -- Dosa, Sambar, Eggs
(17,  4, 250.00), (17,  3, 180.00), (17, 10, 100.00),                  -- Rice, Chicken Breast, Salad
(18, 24, 150.00), (18,  6, 150.00), (18, 23,  25.00),                  -- Moong Sprouts, Apple, Almonds
(19,  9, 180.00), (19, 17, 200.00), (19, 19, 100.00),                  -- Roti, Rajma, Curd
-- yesterday
(20, 15, 250.00), (20,  5, 120.00), (20, 20, 200.00),                  -- Upma, Banana, Milk
(21,  4, 250.00), (21,  8, 200.00), (21,  7, 100.00),                  -- Rice, Dal, Paneer
(22, 21,  40.00), (22,  6, 150.00),                                    -- Soya Chunks, Apple
(23,  9, 180.00), (23, 18, 200.00), (23, 10, 100.00),                  -- Roti, Chole, Salad
-- today
(24, 16, 200.00), (24, 19, 150.00),                                    -- Aloo Paratha, Curd
(25,  4, 250.00), (25,  8, 150.00), (25,  7,  60.00);                  -- Rice, Dal, Paneer

-- Water for the last 7 days. TIMESTAMP(date, time) joins a date and a time.
INSERT INTO water_intake (user_id, amount_ml, recorded_at) VALUES
-- 6 days ago (2250 ml)
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '08:00:00')),
(1, 750, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '13:00:00')),
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '17:30:00')),
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '21:00:00')),
-- 5 days ago (2500 ml)
(1, 750, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '07:45:00')),
(1, 750, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '12:30:00')),
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '16:00:00')),
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '20:30:00')),
-- 4 days ago (1750 ml)
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '09:00:00')),
(1, 750, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '14:00:00')),
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '19:00:00')),
-- 3 days ago (2000 ml)
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '08:30:00')),
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '11:30:00')),
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '15:00:00')),
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '20:00:00')),
-- 2 days ago (2750 ml)
(1, 750, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '07:30:00')),
(1, 750, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '12:00:00')),
(1, 750, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '16:30:00')),
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '21:00:00')),
-- yesterday (2250 ml)
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '08:00:00')),
(1, 750, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '13:00:00')),
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '17:00:00')),
(1, 500, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '21:30:00')),
-- today (1250 ml so far)
(1, 500, TIMESTAMP(CURDATE(), '07:30:00')),
(1, 250, TIMESTAMP(CURDATE(), '10:00:00')),
(1, 500, TIMESTAMP(CURDATE(), '12:30:00'));
