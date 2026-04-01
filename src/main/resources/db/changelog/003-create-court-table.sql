--liquibase formatted sql

--changeset reservator:003-create-court-table
CREATE TABLE court (
    id                    UUID          PRIMARY KEY,
    vendor_id             UUID          NOT NULL REFERENCES vendor(id),
    schedule_id           UUID          NOT NULL REFERENCES schedule(id),
    name                  VARCHAR(255)  NOT NULL,
    description           VARCHAR(1000),
    slot_duration_minutes INT           NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE   NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE   NOT NULL
);
