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
    String tomorrow;

    @BeforeEach
    void setUp() throws Exception {
        tomorrow = LocalDate.now().plusDays(1).toString();

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
                                { "username": "alice", "date": "%s", "startTime": "09:00", "endTime": "10:00" }
                                """.formatted(tomorrow)))
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
                                { "username": "alice", "date": "%s", "startTime": "09:00", "endTime": "10:00" }
                                """.formatted(tomorrow)))
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
                                { "username": "bob", "date": "%s", "startTime": "09:00", "endTime": "10:00" }
                                """.formatted(tomorrow)))
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
                                { "username": "charlie", "date": "%s", "startTime": "09:00", "endTime": "10:00" }
                                """.formatted(tomorrow)))
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
                                { "username": "alice", "date": "%s", "startTime": "09:00", "endTime": "10:00" }
                                """.formatted(tomorrow)))
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
                        { "username": "alice", "date": "%s", "startTime": "09:00", "endTime": "10:00" }
                        """.formatted(tomorrow)));

        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "bob", "date": "%s", "startTime": "09:00", "endTime": "10:00" }
                                """.formatted(tomorrow)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Slot is already booked"));
    }

    @Test
    @DisplayName("GIVEN a court with 60-min slots WHEN holding a misaligned time (09:30) THEN it returns 400")
    void misalignedSlotReturns400() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "alice", "date": "%s", "startTime": "09:30", "endTime": "10:30" }
                                """.formatted(tomorrow)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Start time must align with the slot grid"));
    }

    @Test
    @DisplayName("GIVEN one slot is held WHEN checking availability THEN the held slot is BOOKED and others are FREE")
    void availabilityShowsFreeSlots() throws Exception {
        // Hold one slot
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "username": "alice", "date": "%s", "startTime": "09:00", "endTime": "10:00" }
                        """.formatted(tomorrow)));

        mockMvc.perform(get("/api/vendors/{vendorId}/courts/{courtId}/availability", vendorId, courtId)
                        .param("date", tomorrow))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(tomorrow))
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots[?(@.startTime=='09:00:00')].status").value("BOOKED"))
                .andExpect(jsonPath("$.slots[?(@.startTime=='08:00:00')].status").value("FREE"));
    }

    @Test
    @DisplayName("GIVEN a court with 60-min slots WHEN holding from 09:00 to 11:00 THEN a two-slot reservation is created")
    void multiSlotBooking() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "alice", "date": "%s", "startTime": "09:00", "endTime": "11:00" }
                                """.formatted(tomorrow)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.endTime").value("11:00:00"));
    }

    @Test
    @DisplayName("GIVEN a user already has an active hold WHEN they try to hold another slot THEN it returns 409 Conflict")
    void oneHoldPerUserPerVendor() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "username": "alice", "date": "%s", "startTime": "09:00", "endTime": "10:00" }
                        """.formatted(tomorrow)));

        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "alice", "date": "%s", "startTime": "11:00", "endTime": "12:00" }
                                """.formatted(tomorrow)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("User already has an active hold for this vendor"));
    }
}
