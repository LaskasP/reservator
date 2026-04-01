--liquibase formatted sql

--changeset reservator:002-create-schedule-table
CREATE TABLE schedule (
    id         UUID         PRIMARY KEY,
    vendor_id  UUID         NOT NULL REFERENCES vendor(id),
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE  NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE  NOT NULL
);

--changeset reservator:002-create-schedule-slot-table
CREATE TABLE schedule_slot (
    id          UUID        PRIMARY KEY,
    schedule_id UUID        NOT NULL REFERENCES schedule(id),
    day_of_week VARCHAR(16) NOT NULL,
    open_time   TIME        NOT NULL,
    close_time  TIME        NOT NULL,
    CONSTRAINT uq_schedule_slot_day UNIQUE (schedule_id, day_of_week)
);
