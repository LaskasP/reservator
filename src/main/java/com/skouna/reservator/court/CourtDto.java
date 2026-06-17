package com.skouna.reservator.court;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

final class CourtDto {

    @Schema(name = "CourtCreateRequest", description = "Payload to create or update a court")
    record CreateRequest(
            @Schema(description = "Court display name", example = "Court 1")
            String name,
            @Schema(description = "Optional description of the resource", example = "Indoor padel court with artificial turf")
            String description,
            @Schema(description = "Fixed slot duration in minutes for this court", example = "60")
            int slotDurationMinutes,
            @Schema(description = "ID of the schedule defining operating hours", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
            UUID scheduleId
    ) {}

    @Schema(name = "CourtResponse", description = "Court details")
    record Response(
            @Schema(description = "Unique court identifier", example = "d4e5f6a7-b8c9-0123-def0-123456789abc")
            UUID id,
            @Schema(description = "Owning vendor ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            UUID vendorId,
            @Schema(description = "Assigned schedule ID", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
            UUID scheduleId,
            @Schema(description = "Court name", example = "Court 1")
            String name,
            @Schema(description = "Court description", example = "Indoor padel court with artificial turf")
            String description,
            @Schema(description = "Slot duration in minutes", example = "60")
            int slotDurationMinutes
    ) {
        static Response from(Court court) {
            return new Response(
                    court.getId(),
                    court.getVendor().getId(),
                    court.getSchedule().getId(),
                    court.getName(),
                    court.getDescription(),
                    court.getSlotDurationMinutes()
            );
        }
    }
}
