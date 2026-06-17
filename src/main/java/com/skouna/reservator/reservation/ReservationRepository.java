package com.skouna.reservator.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByCourtIdAndDate(UUID courtId, LocalDate date);

    @Query("""
            SELECT r FROM Reservation r
            WHERE r.court.id = :courtId
              AND r.date = :date
              AND r.status IN :statuses
              AND r.startTime < :endTime
              AND r.endTime > :startTime
            """)
    List<Reservation> findOverlapping(UUID courtId, LocalDate date, LocalTime startTime, LocalTime endTime,
                                       List<ReservationStatus> statuses);

    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.username = :username
              AND r.court.vendor.id = :vendorId
              AND r.status = :status
            """)
    long countActiveHoldsByUserAndVendor(String username, UUID vendorId, ReservationStatus status);

    Optional<Reservation> findByIdempotencyKey(UUID idempotencyKey);
}
