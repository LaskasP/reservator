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
class CrossCourtAvailabilityTest {

    @Autowired
    MockMvc mockMvc;

    String vendorId;
    String court1Id;
    String court2Id;
    String tomorrow;

    @BeforeEach
    void setUp() throws Exception {
        tomorrow = LocalDate.now().plusDays(1).toString();

        var vendorResult = mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Multi Court Venue", "timezone": "UTC", "holdTtlMinutes": 5, "requiresConfirmation": false }
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
                                    { "dayOfWeek": "SATURDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "SUNDAY", "openTime": "08:00", "closeTime": "22:00" }
                                  ]
                                }
                                """))
                .andReturn();
        var scheduleId = JsonPath.read(scheduleResult.getResponse().getContentAsString(), "$.id").toString();

        var c1 = mockMvc.perform(post("/api/vendors/{vendorId}/courts", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Court 1", "slotDurationMinutes": 60, "scheduleId": "%s" }
                                """.formatted(scheduleId)))
                .andReturn();
        court1Id = JsonPath.read(c1.getResponse().getContentAsString(), "$.id").toString();

        var c2 = mockMvc.perform(post("/api/vendors/{vendorId}/courts", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Court 2", "slotDurationMinutes": 90, "scheduleId": "%s" }
                                """.formatted(scheduleId)))
                .andReturn();
        court2Id = JsonPath.read(c2.getResponse().getContentAsString(), "$.id").toString();
    }

    @Test
    @DisplayName("GIVEN two courts and one slot held on Court 1 WHEN checking vendor-wide availability THEN both courts are listed with correct slot statuses")
    void crossCourtAvailabilityShowsAllCourts() throws Exception {
        // Hold a slot on Court 1
        mockMvc.perform(post("/api/vendors/{vendorId}/courts/{courtId}/hold", vendorId, court1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "username": "alice", "date": "%s", "startTime": "09:00", "endTime": "10:00" }
                        """.formatted(tomorrow)));

        mockMvc.perform(get("/api/vendors/{vendorId}/availability", vendorId)
                        .param("date", tomorrow))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(tomorrow))
                .andExpect(jsonPath("$.courts").isArray())
                .andExpect(jsonPath("$.courts.length()").value(2))
                .andExpect(jsonPath("$.courts[?(@.name=='Court 1')].slots[?(@.startTime=='09:00:00')].status").value("BOOKED"))
                .andExpect(jsonPath("$.courts[?(@.name=='Court 1')].slots[?(@.startTime=='08:00:00')].status").value("FREE"))
                .andExpect(jsonPath("$.courts[?(@.name=='Court 2')].slots[?(@.startTime=='08:00:00')].status").value("FREE"));
    }
}
