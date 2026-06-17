package com.skouna.reservator.court;

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
class CourtApiTest {

    @Autowired
    MockMvc mockMvc;

    String vendorId;
    String scheduleId;

    @BeforeEach
    void setUp() throws Exception {
        var vendorResult = mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Test Vendor", "timezone": "Europe/Athens", "holdTtlMinutes": 5, "requiresConfirmation": false }
                                """))
                .andReturn();
        vendorId = JsonPath.read(vendorResult.getResponse().getContentAsString(), "$.id").toString();

        var scheduleResult = mockMvc.perform(post("/api/vendors/{vendorId}/schedules", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Default Schedule",
                                  "slots": [
                                    { "dayOfWeek": "MONDAY", "openTime": "08:00", "closeTime": "22:00" }
                                  ]
                                }
                                """))
                .andReturn();
        scheduleId = JsonPath.read(scheduleResult.getResponse().getContentAsString(), "$.id").toString();
    }

    @Test
    @DisplayName("GIVEN a vendor and schedule WHEN creating a court THEN it returns 201 with the court data")
    void createCourt() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Court 1",
                                  "description": "Indoor padel court",
                                  "slotDurationMinutes": 60,
                                  "scheduleId": "%s"
                                }
                                """.formatted(scheduleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.vendorId").value(vendorId))
                .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.name").value("Court 1"))
                .andExpect(jsonPath("$.description").value("Indoor padel court"))
                .andExpect(jsonPath("$.slotDurationMinutes").value(60));
    }

    @Test
    @DisplayName("GIVEN an existing court WHEN fetching by ID THEN it returns the court")
    void getCourtById() throws Exception {
        var result = mockMvc.perform(post("/api/vendors/{vendorId}/courts", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Court A",
                                  "description": null,
                                  "slotDurationMinutes": 90,
                                  "scheduleId": "%s"
                                }
                                """.formatted(scheduleId)))
                .andReturn();
        var courtId = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(get("/api/vendors/{vendorId}/courts/{courtId}", vendorId, courtId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courtId))
                .andExpect(jsonPath("$.name").value("Court A"))
                .andExpect(jsonPath("$.slotDurationMinutes").value(90));
    }

    @Test
    @DisplayName("GIVEN a non-existent court ID WHEN fetching by ID THEN it returns 404")
    void getNonExistentCourtReturns404() throws Exception {
        mockMvc.perform(get("/api/vendors/{vendorId}/courts/{courtId}", vendorId, "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GIVEN two courts exist WHEN listing courts for the vendor THEN it returns both")
    void listCourtsForVendor() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/courts", vendorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Court 1", "slotDurationMinutes": 60, "scheduleId": "%s" }
                        """.formatted(scheduleId)));
        mockMvc.perform(post("/api/vendors/{vendorId}/courts", vendorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Court 2", "slotDurationMinutes": 90, "scheduleId": "%s" }
                        """.formatted(scheduleId)));

        mockMvc.perform(get("/api/vendors/{vendorId}/courts", vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GIVEN an existing court WHEN updating its fields THEN it returns the updated court")
    void updateCourt() throws Exception {
        var result = mockMvc.perform(post("/api/vendors/{vendorId}/courts", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Old Name", "description": "Old desc", "slotDurationMinutes": 60, "scheduleId": "%s" }
                                """.formatted(scheduleId)))
                .andReturn();
        var courtId = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(put("/api/vendors/{vendorId}/courts/{courtId}", vendorId, courtId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "New Name", "description": "New desc", "slotDurationMinutes": 90, "scheduleId": "%s" }
                                """.formatted(scheduleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.description").value("New desc"))
                .andExpect(jsonPath("$.slotDurationMinutes").value(90));
    }

    @Test
    @DisplayName("GIVEN an existing court WHEN deleting it THEN it returns 204 and subsequent GET returns 404")
    void deleteCourt() throws Exception {
        var result = mockMvc.perform(post("/api/vendors/{vendorId}/courts", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "To Delete", "slotDurationMinutes": 60, "scheduleId": "%s" }
                                """.formatted(scheduleId)))
                .andReturn();
        var courtId = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(delete("/api/vendors/{vendorId}/courts/{courtId}", vendorId, courtId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vendors/{vendorId}/courts/{courtId}", vendorId, courtId))
                .andExpect(status().isNotFound());
    }
}
