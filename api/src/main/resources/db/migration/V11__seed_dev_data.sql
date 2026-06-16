-- V11: Seed development data with test users, accounts, budgets, goals, transactions, and reminders
-- Password for all test users is "password1" (BCrypt hash: $2a$10$...)
-- Well-known UUIDs for test users allow tests to reference them

-- ============================================================
-- USERS
-- ============================================================
INSERT INTO users (id, display_name, email, password_hash, role, created_at, updated_at)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Admin',    'admin@test.dev',  '$2a$10$Z3vTuj.oWRm6UjH6qFij9.wCtLLEzJPlzaCM4rj9xk4uGMFKfD7hO', 'ADMIN', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000002', 'Alice',    'alice@test.dev',  '$2a$10$Z3vTuj.oWRm6UjH6qFij9.wCtLLEzJPlzaCM4rj9xk4uGMFKfD7hO', 'USER',  NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000003', 'Bob',      'bob@test.dev',    '$2a$10$Z3vTuj.oWRm6UjH6qFij9.wCtLLEzJPlzaCM4rj9xk4uGMFKfD7hO', 'USER',  NOW(), NOW());

-- ============================================================
-- ACCOUNTS
-- ============================================================
INSERT INTO accounts (id, name, type, currency, initial_balance, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'Joint Checking',    'ASSET',        'USD', 5000.00,   NOW(), NOW()),
    (gen_random_uuid(), 'Alice''s Savings',  'ASSET',        'USD', 12000.00,  NOW(), NOW()),
    (gen_random_uuid(), 'Bob''s Credit Card', 'CREDIT_CARD',  'USD', 0,         NOW(), NOW()),
    (gen_random_uuid(), 'Groceries',          'EXPENSE',      'USD', 0,         NOW(), NOW());

-- ============================================================
-- ACCOUNT-USERS (shared accounts)
-- ============================================================
-- Joint Checking: Admin, Alice, Bob
INSERT INTO account_users (account_id, user_id)
SELECT a.id, u.id
FROM (SELECT id FROM accounts WHERE name = 'Joint Checking') a
CROSS JOIN (SELECT id FROM users) u;

-- Alice's Savings: Alice only
INSERT INTO account_users (account_id, user_id)
SELECT a.id, u.id
FROM (SELECT id FROM accounts WHERE name = 'Alice''s Savings') a
CROSS JOIN (SELECT id FROM users WHERE display_name = 'Alice') u;

-- Bob's Credit Card: Bob only
INSERT INTO account_users (account_id, user_id)
SELECT a.id, u.id
FROM (SELECT id FROM accounts WHERE name = 'Bob''s Credit Card') a
CROSS JOIN (SELECT id FROM users WHERE display_name = 'Bob') u;

-- Groceries: Admin, Alice
INSERT INTO account_users (account_id, user_id)
SELECT a.id, u.id
FROM (SELECT id FROM accounts WHERE name = 'Groceries') a
CROSS JOIN (SELECT id FROM users WHERE display_name IN ('Admin', 'Alice')) u;

-- ============================================================
-- TRANSACTIONS (~20 total, spread across last 30 days)
-- ============================================================
-- Joint Checking transactions
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  5000.00, 'Initial deposit',           'Income',       NOW() - interval '28 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Joint Checking';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(), -2500.00, 'Rent payment',              'Housing',      NOW() - interval '25 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Joint Checking';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  -450.00, 'Electric bill',             'Utilities',    NOW() - interval '20 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Joint Checking';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  -120.00, 'Internet service',          'Utilities',    NOW() - interval '18 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Joint Checking';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  3500.00, 'Paycheck - Admin',          'Income',       NOW() - interval '14 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Joint Checking';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  -200.00, 'Gas station',               'Transport',    NOW() - interval '10 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Joint Checking';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  -85.00,  'Phone bill',                'Utilities',    NOW() - interval '7 days',  a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Joint Checking';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  -320.00, 'Grocery store',             'Food',         NOW() - interval '5 days',  a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Joint Checking';

-- Alice's Savings transactions
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(), 12000.00, 'Initial deposit',           'Transfer',     NOW() - interval '28 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Alice''s Savings';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),   500.00, 'Monthly savings transfer',  'Savings',      NOW() - interval '14 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Alice''s Savings';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),   500.00, 'Monthly savings transfer',  'Savings',      NOW(), a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Alice''s Savings';

-- Bob's Credit Card transactions
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),   -45.00, 'Restaurant lunch',          'Dining',       NOW() - interval '22 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Bob''s Credit Card';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  -120.00, 'New shoes',                 'Shopping',     NOW() - interval '15 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Bob''s Credit Card';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),   -60.00, 'Grocery delivery',          'Food',         NOW() - interval '8 days',  a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Bob''s Credit Card';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),   -35.00, 'Coffee shop',               'Dining',       NOW() - interval '3 days',  a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Bob''s Credit Card';

-- Groceries transactions
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  -150.00, 'Weekly groceries',          'Food',         NOW() - interval '26 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Groceries';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  -200.00, 'Weekly groceries',          'Food',         NOW() - interval '19 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Groceries';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  -175.00, 'Weekly groceries',          'Food',         NOW() - interval '12 days', a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Groceries';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),  -220.00, 'Weekly groceries',          'Food',         NOW() - interval '5 days',  a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Groceries';
INSERT INTO transactions (id, amount, description, category, transaction_date, account_id, created_at, updated_at)
SELECT gen_random_uuid(),   -90.00, 'Farmers market',            'Food',         NOW() - interval '2 days',  a.id, NOW(), NOW() FROM accounts a WHERE a.name = 'Groceries';

-- ============================================================
-- BUDGETS
-- ============================================================
INSERT INTO budgets (id, name, description, amount, timeframe, start_date, end_date, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'Monthly Groceries', 'Shared grocery budget', 600.00,  'MONTHLY', DATE_TRUNC('month', NOW()), NULL, NOW(), NOW()),
    (gen_random_uuid(), 'Weekly Dining Out', 'Dining out allowance',  150.00,  'WEEKLY',  CURRENT_DATE, NULL, NOW(), NOW()),
    (gen_random_uuid(), 'Rent',              'Monthly rent payment',  2000.00, 'CUSTOM',  DATE_TRUNC('month', NOW()), (DATE_TRUNC('month', NOW()) + interval '1 month' - interval '1 day')::date, NOW(), NOW());

-- Budget users
INSERT INTO budget_users (budget_id, user_id)
SELECT b.id, u.id
FROM (SELECT id FROM budgets WHERE name = 'Monthly Groceries') b
CROSS JOIN (SELECT id FROM users WHERE display_name IN ('Admin', 'Alice')) u;

INSERT INTO budget_users (budget_id, user_id)
SELECT b.id, u.id
FROM (SELECT id FROM budgets WHERE name = 'Weekly Dining Out') b
CROSS JOIN (SELECT id FROM users) u;

INSERT INTO budget_users (budget_id, user_id)
SELECT b.id, u.id
FROM (SELECT id FROM budgets WHERE name = 'Rent') b
CROSS JOIN (SELECT id FROM users WHERE display_name = 'Admin') u;

-- Budget-transactions: link grocery transactions to Monthly Groceries budget
INSERT INTO budget_transactions (budget_id, transaction_id)
SELECT b.id, t.id
FROM budgets b
CROSS JOIN transactions t
WHERE b.name = 'Monthly Groceries'
  AND t.description IN ('Weekly groceries', 'Farmers market', 'Grocery store')
  AND t.account_id = (SELECT id FROM accounts WHERE name = 'Groceries');

-- Budget-transactions: link dining transactions to Weekly Dining Out budget
INSERT INTO budget_transactions (budget_id, transaction_id)
SELECT b.id, t.id
FROM budgets b
CROSS JOIN transactions t
WHERE b.name = 'Weekly Dining Out'
  AND t.description IN ('Restaurant lunch', 'Coffee shop');

-- Budget-transactions: link rent to Rent budget
INSERT INTO budget_transactions (budget_id, transaction_id)
SELECT b.id, t.id
FROM budgets b
CROSS JOIN transactions t
WHERE b.name = 'Rent'
  AND t.description = 'Rent payment';

-- ============================================================
-- GOALS
-- ============================================================
INSERT INTO goals (id, name, target_amount, target_date, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'Vacation Fund',   3000.00,  (DATE_TRUNC('year', NOW()) + interval '11 months' + interval '30 days')::date, NOW(), NOW()),
    (gen_random_uuid(), 'Emergency Fund', 10000.00,  NULL, NOW(), NOW());

-- Goal users
INSERT INTO goal_users (goal_id, user_id)
SELECT g.id, u.id
FROM (SELECT id FROM goals WHERE name = 'Vacation Fund') g
CROSS JOIN (SELECT id FROM users WHERE display_name IN ('Admin', 'Alice')) u;

INSERT INTO goal_users (goal_id, user_id)
SELECT g.id, u.id
FROM (SELECT id FROM goals WHERE name = 'Emergency Fund') g
CROSS JOIN (SELECT id FROM users) u;

-- Goal-transactions: link savings transfers to Vacation Fund
INSERT INTO goal_transactions (goal_id, transaction_id)
SELECT g.id, t.id
FROM goals g
CROSS JOIN transactions t
WHERE g.name = 'Vacation Fund'
  AND t.description = 'Monthly savings transfer';

-- ============================================================
-- REMINDERS
-- ============================================================
-- 1. Bill mismatch for Bob (references Bob's Credit Card restaurant transaction)
INSERT INTO reminders (id, message, status, type, user_id, transaction_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Restaurant spending on Bob''s Credit Card seems higher than expected', 'PENDING', 'BILL_MISMATCH',
       u.id, t.id, NOW(), NOW()
FROM users u
CROSS JOIN transactions t
WHERE u.display_name = 'Bob'
  AND t.description = 'Restaurant lunch'
  AND t.account_id = (SELECT id FROM accounts WHERE name = 'Bob''s Credit Card');

-- 2. Goal milestone for Alice (Vacation Fund 50% reached)
INSERT INTO reminders (id, message, status, type, user_id, transaction_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Vacation Fund goal is 50% funded! Keep saving toward your $3,000 target.', 'PENDING', 'GOAL_MILESTONE',
       u.id, NULL, NOW(), NOW()
FROM users u
WHERE u.display_name = 'Alice';

-- 3. Bill mismatch for Admin (references rent payment)
INSERT INTO reminders (id, message, status, type, user_id, transaction_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Rent payment of $2,500 was higher than the $2,000 budgeted amount.', 'PENDING', 'BILL_MISMATCH',
       u.id, t.id, NOW(), NOW()
FROM users u
CROSS JOIN transactions t
WHERE u.display_name = 'Admin'
  AND t.description = 'Rent payment';

-- 4. Goal milestone for Bob (Emergency Fund)
INSERT INTO reminders (id, message, status, type, user_id, transaction_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Emergency Fund has reached 50% of its $10,000 target. Great progress!', 'PENDING', 'GOAL_MILESTONE',
       u.id, NULL, NOW(), NOW()
FROM users u
WHERE u.display_name = 'Bob';
