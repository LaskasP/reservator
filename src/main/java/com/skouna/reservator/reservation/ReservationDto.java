package com.skouna.reservator.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

final class ReservationDto {

    @Schema(name = "HoldRequest", description = "Request to place a temporary hold on one or more consecutive time slots")
    record HoldRequest(
            @Schema(description = "Username of the person making the reservation", example = "john.doe")
            String username,
            @Schema(description = "Date of the reservation", example = "2026-04-15", type = "string", format = "date")
            LocalDate date,
            @Schema(description = "Slot start time", example = "10:00", type = "string", format = "time")
            LocalTime startTime,
            @Schema(description = "Slot end time (must align with slot boundaries)", example = "11:00", type = "string", format = "time")
            LocalTime endTime
    ) {}

    @Schema(name = "ReservationResponse", description = "Current state of a reservation")
    record Response(
            @Schema(description = "Unique reservation identifier", example = "e5f6a7b8-c9d0-1234-ef01-23456789abcd")
            UUID id,
            @Schema(description = "Court the slot is reserved on", example = "d4e5f6a7-b8c9-0123-def0-123456789abc")
            UUID courtId,
            @Schema(description = "Username of the reservation owner", example = "john.doe")
            String username,
            @Schema(description = "Date of the reservation", example = "2026-04-15")
            LocalDate date,
            @Schema(description = "Slot start time", example = "10:00")
            LocalTime startTime,
            @Schema(description = "Slot end time", example = "11:00")
            LocalTime endTime,
            @Schema(description = "Current status in the lifecycle", example = "HELD")
            ReservationStatus status,
            @Schema(description = "ISO-8601 timestamp when the hold expires (null if not HELD)", example = "2026-04-15T09:15:00", nullable = true)
            String holdExpiresAt
    ) {
        static Response from(Reservation r) {
            return new Response(
                    r.getId(),
                    r.getCourt().getId(),
                    r.getUsername(),
                    r.getDate(),
                    r.getStartTime(),
                    r.getEndTime(),
                    r.getStatus(),
                    r.getHoldExpiresAt() != null ? r.getHoldExpiresAt().toString() : null
            );
        }
    }

    @Schema(name = "AvailableSlot", description = "A single time slot with its availability status")
    record AvailableSlot(
            @Schema(description = "Slot start time", example = "10:00")
            LocalTime startTime,
            @Schema(description = "Slot end time", example = "11:00")
            LocalTime endTime,
            @Schema(description = "AVAILABLE or TAKEN", example = "AVAILABLE")
            String status
    ) {}

    @Schema(name = "AvailabilityResponse", description = "Availability for a single court on a given date")
    record AvailabilityResponse(
            @Schema(description = "Queried date", example = "2026-04-15")
            LocalDate date,
            @Schema(description = "All time slots for the day with availability status")
            List<AvailableSlot> slots
    ) {}

    @Schema(name = "CourtAvailability", description = "Availability for one court inside a vendor-wide response")
    record CourtAvailability(
            @Schema(description = "Court identifier", example = "d4e5f6a7-b8c9-0123-def0-123456789abc")
            UUID courtId,
            @Schema(description = "Court name", example = "Court 1")
            String name,
            @Schema(description = "Slot duration in minutes", example = "60")
            int slotDurationMinutes,
            @Schema(description = "All time slots for the day with availability status")
            List<AvailableSlot> slots
    ) {}

    @Schema(name = "VendorAvailabilityResponse", description = "Aggregated availability across all courts for a vendor on a given date")
    record VendorAvailabilityResponse(
            @Schema(description = "Queried date", example = "2026-04-15")
            LocalDate date,
            @Schema(description = "Per-court availability")
            List<CourtAvailability> courts
    ) {}
}
