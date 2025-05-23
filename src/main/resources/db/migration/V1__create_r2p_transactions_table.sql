-- V1__create_r2p_transactions_table.sql
CREATE TABLE IF NOT EXISTS r2p_transactions
(
    id                          BIGSERIAL PRIMARY KEY,
    payment_request_id          VARCHAR(35) NOT NULL UNIQUE,
    end_to_end_id               VARCHAR(35) NOT NULL,
    request_message_id          VARCHAR(35),
    response_message_id         VARCHAR(35),
    transaction_status          VARCHAR(10) NOT NULL,
    use_case                    VARCHAR(10) NOT NULL,
    product                     VARCHAR(10) NOT NULL,
    requested_amount            DECIMAL(19, 2),
    requested_amount_currency   VARCHAR(3),
    accepted_amount             DECIMAL(19, 2),
    accepted_amount_currency    VARCHAR(3),
    creditor_agent_id           VARCHAR(35),
    debtor_agent_id             VARCHAR(35),
    creditor_alias              VARCHAR(35),
    debtor_alias                VARCHAR(35),
    creditor_alias_type         VARCHAR(10),
    debtor_alias_type           VARCHAR(10),
    due_date                    TIMESTAMP,
    request_reason_json         TEXT,
    message                     TEXT,
    settlement_details_json     TEXT,
    original_payment_request_id VARCHAR(35),
    payment_request_type        VARCHAR(10),
    is_refund                   BOOLEAN              DEFAULT FALSE,
    cancellation_reason         VARCHAR(10),
    creditor_ack_message        VARCHAR(250),
    creditor_ack_emoji          VARCHAR(250),
    created_at                  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP   NOT NULL DEFAULT NOW(),
    version                     BIGINT               DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_payment_request_id ON r2p_transactions (payment_request_id);
CREATE INDEX idx_end_to_end_id ON r2p_transactions (end_to_end_id);
CREATE INDEX idx_transaction_status ON r2p_transactions (transaction_status);
CREATE INDEX idx_creditor_agent_id ON r2p_transactions (creditor_agent_id);
CREATE INDEX idx_debtor_agent_id ON r2p_transactions (debtor_agent_id);
CREATE INDEX idx_created_at ON r2p_transactions (created_at);
CREATE INDEX idx_due_date ON r2p_transactions (due_date);
CREATE INDEX idx_original_payment_request_id ON r2p_transactions (original_payment_request_id);