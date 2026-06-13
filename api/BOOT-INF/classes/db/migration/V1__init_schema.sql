CREATE TABLE users (
    id UUID NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    refresh_token VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE accounts (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    initial_balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

CREATE TABLE account_users (
    account_id UUID NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT pk_account_users PRIMARY KEY (account_id, user_id),
    CONSTRAINT fk_account_users_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_account_users_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE budgets (
    id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_budgets PRIMARY KEY (id),
    CONSTRAINT fk_budgets_user FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE transactions (
    id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    account_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE budget_transactions (
    budget_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    CONSTRAINT pk_budget_transactions PRIMARY KEY (budget_id, transaction_id),
    CONSTRAINT fk_bt_budget FOREIGN KEY (budget_id) REFERENCES budgets(id),
    CONSTRAINT fk_bt_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

CREATE INDEX idx_account_users_user ON account_users(user_id);
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_budgets_user ON budgets(created_by);
