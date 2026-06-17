package com.skouna.reservator.exception;

public enum ErrorCodeEnum {
    RESOURCE_NOT_FOUND,
    SLOT_NOT_ALIGNED,
    BOOKING_IN_PAST,
    OUTSIDE_OPERATING_HOURS,
    SLOT_ALREADY_BOOKED,
    HOLD_LIMIT_EXCEEDED,
    HOLD_EXPIRED,
    INVALID_STATE
}
