ALTER TABLE transactions ADD COLUMN description VARCHAR(500);
ALTER TABLE transactions ADD COLUMN category VARCHAR(100);
ALTER TABLE transactions ADD COLUMN transaction_date DATE NOT NULL DEFAULT CURRENT_DATE;
CREATE INDEX idx_transactions_category ON transactions(category);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
