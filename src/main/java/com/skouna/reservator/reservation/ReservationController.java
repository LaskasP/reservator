package com.skouna.reservator.reservation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Reservation lifecycle: hold a slot, book it, then optionally confirm or reject")
class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/api/vendors/{vendorId}/courts/{courtId}/hold")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Hold a time slot", description = "Places a temporary hold on one or more consecutive slots. The hold expires after the vendor's holdTtlMinutes. One active hold per user per vendor.")
    ReservationDto.Response hold(@PathVariable UUID vendorId, @PathVariable UUID courtId,
                                 @RequestBody ReservationDto.HoldRequest request) {
        return ReservationDto.Response.from(reservationService.hold(vendorId, courtId, request));
    }

    @PostMapping("/api/vendors/{vendorId}/courts/{courtId}/reservations/{reservationId}/book")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a held slot", description = "Transitions a HELD reservation to BOOKED (or directly to CONFIRMED when the vendor does not require admin approval).")
    ReservationDto.Response book(@PathVariable UUID vendorId, @PathVariable UUID courtId,
                                 @PathVariable UUID reservationId) {
        return ReservationDto.Response.from(reservationService.book(reservationId));
    }

    @PatchMapping("/api/vendors/{vendorId}/courts/{courtId}/reservations/{reservationId}/confirm")
    @Operation(summary = "Confirm a booking (admin)", description = "Admin action: transitions a BOOKED reservation to CONFIRMED.")
    ReservationDto.Response confirm(@PathVariable UUID vendorId, @PathVariable UUID courtId,
                                    @PathVariable UUID reservationId) {
        return ReservationDto.Response.from(reservationService.confirm(reservationId));
    }

    @PatchMapping("/api/vendors/{vendorId}/courts/{courtId}/reservations/{reservationId}/reject")
    @Operation(summary = "Reject a booking (admin)", description = "Admin action: transitions a BOOKED reservation to REJECTED.")
    ReservationDto.Response reject(@PathVariable UUID vendorId, @PathVariable UUID courtId,
                                   @PathVariable UUID reservationId) {
        return ReservationDto.Response.from(reservationService.reject(reservationId));
    }

    @PatchMapping("/api/vendors/{vendorId}/courts/{courtId}/reservations/{reservationId}/cancel")
    @Operation(summary = "Cancel a reservation", description = "Cancels a reservation in any active state (HELD, BOOKED, or CONFIRMED). Respects the vendor's minCancelBeforeMinutes policy.")
    ReservationDto.Response cancel(@PathVariable UUID vendorId, @PathVariable UUID courtId,
                                   @PathVariable UUID reservationId) {
        return ReservationDto.Response.from(reservationService.cancel(reservationId));
    }

    @GetMapping("/api/vendors/{vendorId}/courts/{courtId}/reservations")
    @Operation(summary = "List reservations by date", description = "Returns all reservations for a court on a given date, regardless of status.")
    List<ReservationDto.Response> list(@PathVariable UUID vendorId, @PathVariable UUID courtId,
                                       @RequestParam LocalDate date) {
        return reservationService.listByDate(courtId, date).stream()
                .map(ReservationDto.Response::from).toList();
    }

    @GetMapping("/api/vendors/{vendorId}/courts/{courtId}/availability")
    @Operation(summary = "Check court availability", description = "Returns every possible time slot for a court on a given date, each marked AVAILABLE or TAKEN.")
    ReservationDto.AvailabilityResponse availability(@PathVariable UUID vendorId, @PathVariable UUID courtId,
                                                      @RequestParam LocalDate date) {
        return reservationService.availability(vendorId, courtId, date);
    }

    @GetMapping("/api/vendors/{vendorId}/availability")
    @Operation(summary = "Check vendor-wide availability", description = "Aggregates availability across all of the vendor's courts for a given date.")
    ReservationDto.VendorAvailabilityResponse vendorAvailability(@PathVariable UUID vendorId,
                                                                  @RequestParam LocalDate date) {
        return reservationService.vendorAvailability(vendorId, date);
    }
}
