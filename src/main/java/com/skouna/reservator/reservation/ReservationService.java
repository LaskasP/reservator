package com.skouna.reservator.reservation;

import com.skouna.reservator.court.Court;
import com.skouna.reservator.court.CourtRepository;
import com.skouna.reservator.exception.*;
import static com.skouna.reservator.reservation.ReservationStatus.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final ApplicationEventPublisher eventPublisher;

    Reservation hold(UUID vendorId, UUID courtId, ReservationDto.HoldRequest request) {
        var court = findCourt(vendorId, courtId);
        var vendor = court.getVendor();

        validateNotInPast(request.date(), request.startTime());
        validateSlotAlignment(court, request.date(), request.startTime(), request.endTime());
        validateWithinOperatingHours(court, request.date(), request.startTime(), request.endTime());
        validateNoOverlap(courtId, request.date(), request.startTime(), request.endTime());
        validateOneHoldPerUserPerVendor(request.username(), vendorId);

        var reservation = new Reservation();
        reservation.setCourt(court);
        reservation.setUsername(request.username());
        reservation.setDate(request.date());
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setStatus(ReservationStatus.HELD);
        reservation.setHoldExpiresAt(Instant.now().plus(vendor.getHoldTtlMinutes(), ChronoUnit.MINUTES));

        return reservationRepository.save(reservation);
    }

    Reservation book(UUID reservationId) {
        var reservation = findReservation(reservationId);
        if (reservation.getStatus() != ReservationStatus.HELD) {
            throw new ConflictException(ErrorCodeEnum.INVALID_STATE, "Reservation is not in HELD state");
        }
        if (reservation.getHoldExpiresAt() != null && reservation.getHoldExpiresAt().isBefore(Instant.now())) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            throw new ConflictException(ErrorCodeEnum.HOLD_EXPIRED, "Hold has expired");
        }

        var vendor = reservation.getCourt().getVendor();
        if (vendor.isRequiresConfirmation()) {
            reservation.setStatus(ReservationStatus.BOOKED);
        } else {
            reservation.setStatus(ReservationStatus.CONFIRMED);
        }
        reservation.setHoldExpiresAt(null);
        return reservationRepository.save(reservation);
    }

    Reservation confirm(UUID reservationId) {
        var reservation = findReservation(reservationId);
        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new ConflictException(ErrorCodeEnum.INVALID_STATE, "Reservation is not in BOOKED state");
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    Reservation reject(UUID reservationId) {
        var reservation = findReservation(reservationId);
        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new ConflictException(ErrorCodeEnum.INVALID_STATE, "Reservation is not in BOOKED state");
        }
        reservation.setStatus(ReservationStatus.REJECTED);
        return reservationRepository.save(reservation);
    }

    Reservation cancel(UUID reservationId) {
        var reservation = findReservation(reservationId);
        var allowed = List.of(ReservationStatus.HELD, ReservationStatus.BOOKED, ReservationStatus.CONFIRMED);
        if (!allowed.contains(reservation.getStatus())) {
            throw new ConflictException(ErrorCodeEnum.INVALID_STATE,
                    "Reservation cannot be cancelled from state: " + reservation.getStatus());
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    List<Reservation> listByDate(UUID courtId, LocalDate date) {
        return reservationRepository.findByCourtIdAndDate(courtId, date);
    }

    @Transactional(readOnly = true)
    ReservationDto.AvailabilityResponse availability(UUID vendorId, UUID courtId, LocalDate date) {
        var court = findCourt(vendorId, courtId);
        var dayOfWeek = date.getDayOfWeek();

        var scheduleSlot = court.getSchedule().getSlots().stream()
                .filter(s -> s.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElse(null);

        if (scheduleSlot == null) {
            return new ReservationDto.AvailabilityResponse(date, List.of());
        }

        var slotDuration = court.getSlotDurationMinutes();
        var activeReservations = reservationRepository.findOverlapping(
                courtId, date, scheduleSlot.getOpenTime(), scheduleSlot.getCloseTime(), BLOCKING_STATUSES);

        // Expire stale holds lazily
        expireStaleHolds(activeReservations);
        activeReservations = activeReservations.stream()
                .filter(r -> r.getStatus() != ReservationStatus.EXPIRED)
                .toList();

        var slots = new ArrayList<ReservationDto.AvailableSlot>();
        var current = scheduleSlot.getOpenTime();
        while (current.plusMinutes(slotDuration).compareTo(scheduleSlot.getCloseTime()) <= 0) {
            var slotEnd = current.plusMinutes(slotDuration);
            var start = current;
            var isBooked = activeReservations.stream().anyMatch(
                    r -> r.getStartTime().isBefore(slotEnd) && r.getEndTime().isAfter(start));
            slots.add(new ReservationDto.AvailableSlot(current, slotEnd, isBooked ? "BOOKED" : "FREE"));
            current = slotEnd;
        }

        return new ReservationDto.AvailabilityResponse(date, slots);
    }

    @Transactional(readOnly = true)
    ReservationDto.VendorAvailabilityResponse vendorAvailability(UUID vendorId, LocalDate date) {
        var courts = courtRepository.findByVendorId(vendorId);
        var courtAvailabilities = courts.stream().map(court -> {
            var avail = availability(vendorId, court.getId(), date);
            return new ReservationDto.CourtAvailability(
                    court.getId(), court.getName(), court.getSlotDurationMinutes(), avail.slots());
        }).toList();
        return new ReservationDto.VendorAvailabilityResponse(date, courtAvailabilities);
    }

    private void validateNotInPast(LocalDate date, LocalTime startTime) {
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException(ErrorCodeEnum.BOOKING_IN_PAST, "Cannot book in the past");
        }
    }

    private void validateSlotAlignment(Court court, LocalDate date, LocalTime startTime, LocalTime endTime) {
        var duration = java.time.Duration.between(startTime, endTime).toMinutes();
        if (duration <= 0 || duration % court.getSlotDurationMinutes() != 0) {
            throw new BadRequestException(ErrorCodeEnum.SLOT_NOT_ALIGNED,
                    "Duration must be a positive multiple of " + court.getSlotDurationMinutes() + " minutes");
        }

        var dayOfWeek = date.getDayOfWeek();
        var scheduleSlot = court.getSchedule().getSlots().stream()
                .filter(s -> s.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(ErrorCodeEnum.OUTSIDE_OPERATING_HOURS,
                        "Court is closed on " + dayOfWeek));

        var minutesFromOpen = java.time.Duration.between(scheduleSlot.getOpenTime(), startTime).toMinutes();
        if (minutesFromOpen < 0 || minutesFromOpen % court.getSlotDurationMinutes() != 0) {
            throw new BadRequestException(ErrorCodeEnum.SLOT_NOT_ALIGNED, "Start time must align with the slot grid");
        }
    }

    private void validateWithinOperatingHours(Court court, LocalDate date, LocalTime startTime, LocalTime endTime) {
        var dayOfWeek = date.getDayOfWeek();
        var scheduleSlot = court.getSchedule().getSlots().stream()
                .filter(s -> s.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(ErrorCodeEnum.OUTSIDE_OPERATING_HOURS,
                        "Court is closed on " + dayOfWeek));

        if (startTime.isBefore(scheduleSlot.getOpenTime()) || endTime.isAfter(scheduleSlot.getCloseTime())) {
            throw new BadRequestException(ErrorCodeEnum.OUTSIDE_OPERATING_HOURS,
                    "Reservation is outside operating hours");
        }
    }

    private static final List<ReservationStatus> BLOCKING_STATUSES = List.of(HELD, BOOKED, CONFIRMED);

    private void validateNoOverlap(UUID courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        var overlapping = reservationRepository.findOverlapping(courtId, date, startTime, endTime, BLOCKING_STATUSES);
        // Expire stale holds lazily
        expireStaleHolds(overlapping);
        var active = overlapping.stream()
                .filter(r -> r.getStatus() != ReservationStatus.EXPIRED)
                .toList();
        if (!active.isEmpty()) {
            throw new ConflictException(ErrorCodeEnum.SLOT_ALREADY_BOOKED, "Slot is already booked");
        }
    }

    private void validateOneHoldPerUserPerVendor(String username, UUID vendorId) {
        if (reservationRepository.countActiveHoldsByUserAndVendor(username, vendorId, HELD) > 0) {
            throw new ConflictException(ErrorCodeEnum.HOLD_LIMIT_EXCEEDED,
                    "User already has an active hold for this vendor");
        }
    }

    private void expireStaleHolds(List<Reservation> reservations) {
        for (var r : reservations) {
            if (r.getStatus() == ReservationStatus.HELD
                    && r.getHoldExpiresAt() != null
                    && r.getHoldExpiresAt().isBefore(Instant.now())) {
                r.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(r);
            }
        }
    }

    private Court findCourt(UUID vendorId, UUID courtId) {
        return courtRepository.findById(courtId)
                .filter(c -> c.getVendor().getId().equals(vendorId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCodeEnum.RESOURCE_NOT_FOUND,
                        "Court not found: " + courtId));
    }

    private Reservation findReservation(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCodeEnum.RESOURCE_NOT_FOUND,
                        "Reservation not found: " + id));
    }

}
