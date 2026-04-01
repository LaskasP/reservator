--liquibase formatted sql

--changeset reservator:001-create-vendor-table
CREATE TABLE vendor (
    id                       UUID         PRIMARY KEY,
    name                     VARCHAR(255) NOT NULL,
    timezone                 VARCHAR(64)  NOT NULL,
    hold_ttl_minutes         INT          NOT NULL,
    requires_confirmation    BOOLEAN      NOT NULL,
    max_book_ahead_days      INT,
    min_book_before_minutes  INT,
    min_cancel_before_minutes INT,
    created_at               TIMESTAMP WITH TIME ZONE  NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE  NOT NULL
);
