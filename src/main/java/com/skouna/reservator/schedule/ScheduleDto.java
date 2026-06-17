package com.skouna.reservator.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

final class ScheduleDto {

    @Schema(name = "ScheduleCreateRequest", description = "Payload to create or update a schedule with day-of-week time slots")
    record CreateRequest(
            @Schema(description = "Name of the schedule", example = "Weekday Hours")
            String name,
            @Schema(description = "Operating-hour slots, one per day of the week")
            List<SlotRequest> slots
    ) {}

    @Schema(name = "ScheduleSlotRequest", description = "Operating hours for a single day of the week")
    record SlotRequest(
            @Schema(description = "Day of the week", example = "MONDAY")
            DayOfWeek dayOfWeek,
            @Schema(description = "Opening time", example = "08:00", type = "string", format = "time")
            LocalTime openTime,
            @Schema(description = "Closing time", example = "22:00", type = "string", format = "time")
            LocalTime closeTime
    ) {}

    @Schema(name = "ScheduleResponse", description = "Schedule with its time slots")
    record Response(
            @Schema(description = "Unique schedule identifier", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
            UUID id,
            @Schema(description = "Schedule name", example = "Weekday Hours")
            String name,
            @Schema(description = "Day-of-week operating-hour slots")
            List<SlotResponse> slots
    ) {
        static Response from(Schedule schedule) {
            return new Response(
                    schedule.getId(),
                    schedule.getName(),
                    schedule.getSlots().stream().map(SlotResponse::from).toList()
            );
        }
    }

    @Schema(name = "ScheduleSlotResponse", description = "Operating hours for a single day")
    record SlotResponse(
            @Schema(description = "Slot identifier", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
            UUID id,
            @Schema(description = "Day of the week", example = "MONDAY")
            DayOfWeek dayOfWeek,
            @Schema(description = "Opening time", example = "08:00", type = "string", format = "time")
            LocalTime openTime,
            @Schema(description = "Closing time", example = "22:00", type = "string", format = "time")
            LocalTime closeTime
    ) {
        static SlotResponse from(ScheduleSlot slot) {
            return new SlotResponse(slot.getId(), slot.getDayOfWeek(), slot.getOpenTime(), slot.getCloseTime());
        }
    }
}
