-- Add timeframe column, default to CUSTOM (existing budgets become custom)
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS timeframe VARCHAR(16);
UPDATE budgets SET timeframe = 'CUSTOM' WHERE timeframe IS NULL;
ALTER TABLE budgets ALTER COLUMN timeframe SET NOT NULL;
