package com.skouna.reservator.vendor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

final class VendorDto {

    @Schema(name = "VendorCreateRequest", description = "Payload to create or update a vendor")
    record CreateRequest(
            @Schema(description = "Display name of the vendor", example = "Padel Club Barcelona")
            String name,
            @Schema(description = "IANA timezone for the vendor's operating hours", example = "Europe/Madrid")
            String timezone,
            @Schema(description = "Minutes before an unheld reservation expires", example = "15")
            int holdTtlMinutes,
            @Schema(description = "Whether bookings require admin approval", example = "true")
            boolean requiresConfirmation,
            @Schema(description = "Maximum days in advance a reservation can be made", example = "30", nullable = true)
            Integer maxBookAheadDays,
            @Schema(description = "Minimum minutes before slot start to allow booking", example = "60", nullable = true)
            Integer minBookBeforeMinutes,
            @Schema(description = "Minimum minutes before slot start to allow cancellation", example = "120", nullable = true)
            Integer minCancelBeforeMinutes
    ) {}

    @Schema(name = "VendorResponse", description = "Vendor details")
    record Response(
            @Schema(description = "Unique vendor identifier", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            UUID id,
            @Schema(description = "Display name", example = "Padel Club Barcelona")
            String name,
            @Schema(description = "IANA timezone", example = "Europe/Madrid")
            String timezone,
            @Schema(description = "Hold TTL in minutes", example = "15")
            int holdTtlMinutes,
            @Schema(description = "Whether admin confirmation is required", example = "true")
            boolean requiresConfirmation,
            @Schema(description = "Max booking-ahead window in days", example = "30", nullable = true)
            Integer maxBookAheadDays,
            @Schema(description = "Min lead-time in minutes for booking", example = "60", nullable = true)
            Integer minBookBeforeMinutes,
            @Schema(description = "Min lead-time in minutes for cancellation", example = "120", nullable = true)
            Integer minCancelBeforeMinutes
    ) {
        static Response from(Vendor vendor) {
            return new Response(
                    vendor.getId(),
                    vendor.getName(),
                    vendor.getTimezone(),
                    vendor.getHoldTtlMinutes(),
                    vendor.isRequiresConfirmation(),
                    vendor.getMaxBookAheadDays(),
                    vendor.getMinBookBeforeMinutes(),
                    vendor.getMinCancelBeforeMinutes()
            );
        }
    }
}
