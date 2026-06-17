package com.skouna.reservator.schedule;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ScheduleApiTest {

    @Autowired
    MockMvc mockMvc;

    String vendorId;

    @BeforeEach
    void setUp() throws Exception {
        var result = mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Test Vendor", "timezone": "Europe/Athens", "holdTtlMinutes": 5, "requiresConfirmation": false }
                                """))
                .andReturn();
        vendorId = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();
    }

    @Test
    @DisplayName("GIVEN a vendor WHEN creating a schedule with day-of-week slots THEN it returns 201 with the slots")
    void createScheduleWithSlots() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/schedules", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Weekday Hours",
                                  "slots": [
                                    { "dayOfWeek": "MONDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "TUESDAY", "openTime": "08:00", "closeTime": "22:00" },
                                    { "dayOfWeek": "SATURDAY", "openTime": "10:00", "closeTime": "18:00" }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Weekday Hours"))
                .andExpect(jsonPath("$.slots.length()").value(3))
                .andExpect(jsonPath("$.slots[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.slots[0].openTime").value("08:00:00"))
                .andExpect(jsonPath("$.slots[0].closeTime").value("22:00:00"));
    }

    @Test
    @DisplayName("GIVEN an existing schedule WHEN fetching by ID THEN it returns the schedule with its slots")
    void getScheduleById() throws Exception {
        var result = mockMvc.perform(post("/api/vendors/{vendorId}/schedules", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Morning Schedule",
                                  "slots": [
                                    { "dayOfWeek": "MONDAY", "openTime": "06:00", "closeTime": "12:00" }
                                  ]
                                }
                                """))
                .andReturn();
        var scheduleId = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(get("/api/vendors/{vendorId}/schedules/{scheduleId}", vendorId, scheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Morning Schedule"))
                .andExpect(jsonPath("$.slots.length()").value(1));
    }

    @Test
    @DisplayName("GIVEN two schedules exist WHEN listing schedules for the vendor THEN it returns both")
    void listSchedulesForVendor() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/schedules", vendorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Schedule A", "slots": [] }
                        """));
        mockMvc.perform(post("/api/vendors/{vendorId}/schedules", vendorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Schedule B", "slots": [] }
                        """));

        mockMvc.perform(get("/api/vendors/{vendorId}/schedules", vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GIVEN an existing schedule WHEN updating it THEN the old slots are replaced with the new ones")
    void updateScheduleReplacesSlots() throws Exception {
        var result = mockMvc.perform(post("/api/vendors/{vendorId}/schedules", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Original",
                                  "slots": [
                                    { "dayOfWeek": "MONDAY", "openTime": "08:00", "closeTime": "22:00" }
                                  ]
                                }
                                """))
                .andReturn();
        var scheduleId = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(put("/api/vendors/{vendorId}/schedules/{scheduleId}", vendorId, scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated",
                                  "slots": [
                                    { "dayOfWeek": "WEDNESDAY", "openTime": "10:00", "closeTime": "20:00" },
                                    { "dayOfWeek": "FRIDAY", "openTime": "09:00", "closeTime": "17:00" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.slots.length()").value(2))
                .andExpect(jsonPath("$.slots[0].dayOfWeek").value("WEDNESDAY"));
    }

    @Test
    @DisplayName("GIVEN an existing schedule WHEN deleting it THEN it returns 204 and subsequent GET returns 404")
    void deleteSchedule() throws Exception {
        var result = mockMvc.perform(post("/api/vendors/{vendorId}/schedules", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "To Delete", "slots": [] }
                                """))
                .andReturn();
        var scheduleId = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(delete("/api/vendors/{vendorId}/schedules/{scheduleId}", vendorId, scheduleId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vendors/{vendorId}/schedules/{scheduleId}", vendorId, scheduleId))
                .andExpect(status().isNotFound());
    }
}
