--liquibase formatted sql

--changeset reservator:004-create-reservation-table
CREATE TABLE reservation (
    id              UUID         PRIMARY KEY,
    court_id        UUID         NOT NULL REFERENCES court(id),
    username        VARCHAR(255) NOT NULL,
    date            DATE         NOT NULL,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    hold_expires_at TIMESTAMP WITH TIME ZONE,
    idempotency_key UUID,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE  NOT NULL
);

CREATE INDEX idx_reservation_idempotency_key ON reservation(idempotency_key);
