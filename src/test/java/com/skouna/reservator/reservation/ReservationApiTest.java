package com.skouna.reservator.reservation;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationApiTest {

    @Autowired
    MockMvc mockMvc;

    String vendorId;
    String courtId;
    LocalDate reservationDate;
    String reservationDateText;
    LocalTime firstSlotStart;
    LocalTime firstSlotEnd;
    LocalTime secondSlotStart;
    LocalTime secondSlotEnd;

    @BeforeEach
    void setUp() throws Exception {
        reservationDate = LocalDate.now().plusDays(1);
        reservationDateText = reservationDate.toString();
        firstSlotStart = openingTimeFor(reservationDate);
        firstSlotEnd = firstSlotStart.plusHours(1);
        secondSlotStart = firstSlotEnd;
        secondSlotEnd = secondSlotStart.plusHours(1);

        var vendorResult = mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Test Venue", "timezone": "Europe/Athens", "holdTtlMinutes": 5, "requiresConfirmation": false }
                                """))
                .andReturn();
        vendorId = JsonPath.read(vendorResult.getResponse().getContentAsString(), "$.id").toString();

        var scheduleResult = mockMvc.perform(post("/api/vendors/{vendorId}/schedules", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Full Week",
                                  "slots": [
                                    { "dayOfWeek": "MONDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "TUESDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "WEDNESDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "THURSDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "FRIDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "SATURDAY", "openTime": "10:00", "closeTime": "18:00" },
                                    { "dayOfWeek": "SUNDAY", "openTime": "10:00", "closeTime": "18:00" }
                                  ]
                                }
                                """))
                .andReturn();
        var scheduleId = JsonPath.read(scheduleResult.getResponse().getContentAsString(), "$.id").toString();

        var courtResult = mockMvc.perform(post("/api/vendors/{vendorId}/courts", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Court 1", "slotDurationMinutes": 60, "scheduleId": "%s" }
                                """.formatted(scheduleId)))
                .andReturn();
        courtId = JsonPath.read(courtResult.getResponse().getContentAsString(), "$.id").toString();
    }

    @Test
    @DisplayName("GIVEN an available slot WHEN a user holds it THEN status is HELD with an expiry time")
    void holdSlot() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "alice", "date": "%s", "startTime": "%s", "endTime": "%s" }
                                """.formatted(reservationDateText, firstSlotStart, firstSlotEnd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.holdExpiresAt").exists());
    }

    @Test
    @DisplayName("GIVEN a held slot and a vendor without confirmation WHEN booking THEN status goes directly to CONFIRMED")
    void holdAndBookWithoutConfirmation() throws Exception {
        var holdResult = mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "alice", "date": "%s", "startTime": "%s", "endTime": "%s" }
                                """.formatted(reservationDateText, firstSlotStart, firstSlotEnd)))
                .andReturn();
        var reservationId = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/reservations/{reservationId}/book",
                        vendorId, courtId, reservationId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GIVEN a held slot and a vendor requiring confirmation WHEN booking and admin confirms THEN status transitions HELD -> BOOKED -> CONFIRMED")
    void holdAndBookWithConfirmation() throws Exception {
        // Create vendor that requires confirmation
        var vResult = mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Strict Venue", "timezone": "UTC", "holdTtlMinutes": 5, "requiresConfirmation": true }
                                """))
                .andReturn();
        var strictVendorId = JsonPath.read(vResult.getResponse().getContentAsString(), "$.id").toString();

        var sResult = mockMvc.perform(post("/api/vendors/{vendorId}/schedules", strictVendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Schedule", "slots": [
                                    { "dayOfWeek": "MONDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "TUESDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "WEDNESDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "THURSDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "FRIDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "SATURDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "SUNDAY", "openTime": "08:00", "closeTime": "22:00" }
                                ] }
                                """))
                .andReturn();
        var sId = JsonPath.read(sResult.getResponse().getContentAsString(), "$.id").toString();

        var cResult = mockMvc.perform(post("/api/vendors/{vendorId}/courts", strictVendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Court X", "slotDurationMinutes": 60, "scheduleId": "%s" }
                                """.formatted(sId)))
                .andReturn();
        var strictCourtId = JsonPath.read(cResult.getResponse().getContentAsString(), "$.id").toString();

        var holdResult = mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold",
                        strictVendorId, strictCourtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "bob", "date": "%s", "startTime": "%s", "endTime": "%s" }
                                """.formatted(reservationDateText, firstSlotStart, firstSlotEnd)))
                .andReturn();
        var resId = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.id").toString();

        // Book -> should go to BOOKED (not CONFIRMED)
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/reservations/{reservationId}/book",
                        strictVendorId, strictCourtId, resId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"));

        // Admin confirms
        mockMvc.perform(patch("/api/vendors/{vendorId}/courts/{courtId}/reservations/{reservationId}/confirm",
                        strictVendorId, strictCourtId, resId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GIVEN a booked reservation WHEN admin rejects it THEN status is REJECTED")
    void adminRejectsBooking() throws Exception {
        var vResult = mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Strict Venue", "timezone": "UTC", "holdTtlMinutes": 5, "requiresConfirmation": true }
                                """))
                .andReturn();
        var sv = JsonPath.read(vResult.getResponse().getContentAsString(), "$.id").toString();

        var sResult = mockMvc.perform(post("/api/vendors/{vendorId}/schedules", sv)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Schedule", "slots": [
                                    { "dayOfWeek": "MONDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "TUESDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "WEDNESDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "THURSDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "FRIDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "SATURDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "SUNDAY", "openTime": "08:00", "closeTime": "22:00" }
                                ] }
                                """))
                .andReturn();
        var sid = JsonPath.read(sResult.getResponse().getContentAsString(), "$.id").toString();

        var cResult = mockMvc.perform(post("/api/vendors/{vendorId}/courts", sv)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Court Y", "slotDurationMinutes": 60, "scheduleId": "%s" }
                                """.formatted(sid)))
                .andReturn();
        var sc = JsonPath.read(cResult.getResponse().getContentAsString(), "$.id").toString();

        var holdResult = mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", sv, sc)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "charlie", "date": "%s", "startTime": "%s", "endTime": "%s" }
                                """.formatted(reservationDateText, firstSlotStart, firstSlotEnd)))
                .andReturn();
        var rid = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/reservations/{reservationId}/book", sv, sc, rid));

        mockMvc.perform(patch("/api/vendors/{vendorId}/courts/{courtId}/reservations/{reservationId}/reject", sv, sc, rid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("GIVEN a held reservation WHEN the user cancels it THEN status is CANCELLED")
    void cancelHeldReservation() throws Exception {
        var holdResult = mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "alice", "date": "%s", "startTime": "%s", "endTime": "%s" }
                                """.formatted(reservationDateText, firstSlotStart, firstSlotEnd)))
                .andReturn();
        var resId = JsonPath.read(holdResult.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(patch("/api/vendors/{vendorId}/courts/{courtId}/reservations/{reservationId}/cancel",
                        vendorId, courtId, resId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GIVEN a slot already held WHEN another user tries to hold the same slot THEN it returns 409 Conflict")
    void doubleBookingSameSlotReturns409() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "username": "alice", "date": "%s", "startTime": "%s", "endTime": "%s" }
                        """.formatted(reservationDateText, firstSlotStart, firstSlotEnd)));

        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "bob", "date": "%s", "startTime": "%s", "endTime": "%s" }
                                """.formatted(reservationDateText, firstSlotStart, firstSlotEnd)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Slot is already booked"));
    }

    @Test
    @DisplayName("GIVEN a court with 60-min slots WHEN holding a misaligned time (09:30) THEN it returns 400")
    void misalignedSlotReturns400() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "alice", "date": "%s", "startTime": "%s", "endTime": "%s" }
                                """.formatted(reservationDateText, firstSlotStart.plusMinutes(30), secondSlotStart.plusMinutes(30))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Start time must align with the slot grid"));
    }

    @Test
    @DisplayName("GIVEN a requested slot before opening hours WHEN a user holds it THEN it returns 400 outside operating hours")
    void holdBeforeOpeningReturnsOutsideOperatingHours() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "alice", "date": "%s", "startTime": "%s", "endTime": "%s" }
                                """.formatted(reservationDateText, firstSlotStart.minusHours(1), firstSlotEnd.minusHours(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Reservation is outside operating hours"))
                .andExpect(jsonPath("$.errorCode").value("OUTSIDE_OPERATING_HOURS"));
    }

    @Test
    @DisplayName("GIVEN one slot is held WHEN checking availability THEN the held slot is BOOKED and others are FREE")
    void availabilityShowsFreeSlots() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "username": "alice", "date": "%s", "startTime": "%s", "endTime": "%s" }
                        """.formatted(reservationDateText, firstSlotStart, firstSlotEnd)));

        mockMvc.perform(get("/api/vendors/{vendorId}/courts/{courtId}/availability", vendorId, courtId)
                        .param("date", reservationDateText))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(reservationDateText))
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots[?(@.startTime=='%s')].status"
                        .formatted(apiTime(firstSlotStart))).value("BOOKED"))
                .andExpect(jsonPath("$.slots[?(@.startTime=='%s')].status"
                        .formatted(apiTime(secondSlotStart))).value("FREE"));
    }

    @Test
    @DisplayName("GIVEN a court with 60-min slots WHEN holding from 09:00 to 11:00 THEN a two-slot reservation is created")
    void multiSlotBooking() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "alice", "date": "%s", "startTime": "%s", "endTime": "%s" }
                                """.formatted(reservationDateText, firstSlotStart, secondSlotEnd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").value(apiTime(firstSlotStart)))
                .andExpect(jsonPath("$.endTime").value(apiTime(secondSlotEnd)));
    }

    @Test
    @DisplayName("GIVEN a user already has an active hold WHEN they try to hold another slot THEN it returns 409 Conflict")
    void oneHoldPerUserPerVendor() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "username": "alice", "date": "%s", "startTime": "%s", "endTime": "%s" }
                        """.formatted(reservationDateText, firstSlotStart, firstSlotEnd)));

        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "alice", "date": "%s", "startTime": "%s", "endTime": "%s" }
                                """.formatted(reservationDateText, secondSlotStart, secondSlotEnd)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("User already has an active hold for this vendor"));
    }

    private static LocalTime openingTimeFor(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> LocalTime.of(10, 0);
            default -> LocalTime.of(8, 0);
        };
    }

    private static String apiTime(LocalTime time) {
        return time + ":00";
    }
}
