CREATE TABLE reminders (
    id UUID NOT NULL,
    message VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    user_id UUID NOT NULL,
    transaction_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_reminders PRIMARY KEY (id),
    CONSTRAINT fk_reminders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reminders_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE SET NULL
);
