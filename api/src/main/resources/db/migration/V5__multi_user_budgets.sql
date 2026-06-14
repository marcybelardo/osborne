CREATE TABLE budget_users (
    budget_id UUID NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT pk_budget_users PRIMARY KEY (budget_id, user_id),
    CONSTRAINT fk_budget_users_budget FOREIGN KEY (budget_id) REFERENCES budgets(id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_users_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO budget_users (budget_id, user_id)
SELECT id, created_by FROM budgets WHERE created_by IS NOT NULL;

ALTER TABLE budgets DROP COLUMN created_by;
