CREATE TABLE goals (
    id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    target_amount NUMERIC(19,2) NOT NULL,
    target_date DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_goals PRIMARY KEY (id)
);

CREATE TABLE goal_users (
    goal_id UUID NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT pk_goal_users PRIMARY KEY (goal_id, user_id),
    CONSTRAINT fk_goal_users_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE,
    CONSTRAINT fk_goal_users_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE goal_transactions (
    goal_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    CONSTRAINT pk_goal_transactions PRIMARY KEY (goal_id, transaction_id),
    CONSTRAINT fk_goal_tx_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE,
    CONSTRAINT fk_goal_tx_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);
