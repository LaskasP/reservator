package com.skouna.reservator.vendor;

import com.jayway.jsonpath.JsonPath;
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
class VendorApiTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("GIVEN valid vendor details WHEN creating a vendor THEN it returns 201 with the vendor data")
    void createVendor() throws Exception {
        mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Padel Paradise",
                                  "timezone": "Europe/Athens",
                                  "holdTtlMinutes": 5,
                                  "requiresConfirmation": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Padel Paradise"))
                .andExpect(jsonPath("$.timezone").value("Europe/Athens"))
                .andExpect(jsonPath("$.holdTtlMinutes").value(5))
                .andExpect(jsonPath("$.requiresConfirmation").value(false));
    }

    @Test
    @DisplayName("GIVEN an existing vendor WHEN fetching by ID THEN it returns the vendor")
    void getVendorById() throws Exception {
        var result = mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Test Venue", "timezone": "UTC", "holdTtlMinutes": 3, "requiresConfirmation": true }
                                """))
                .andReturn();
        var id = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(get("/api/vendors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Test Venue"));
    }

    @Test
    @DisplayName("GIVEN a non-existent vendor ID WHEN fetching by ID THEN it returns 404")
    void getNonExistentVendorReturns404() throws Exception {
        mockMvc.perform(get("/api/vendors/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GIVEN two vendors exist WHEN listing vendors THEN it returns both")
    void listVendors() throws Exception {
        mockMvc.perform(post("/api/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Venue A", "timezone": "UTC", "holdTtlMinutes": 5, "requiresConfirmation": false }
                        """));
        mockMvc.perform(post("/api/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Venue B", "timezone": "UTC", "holdTtlMinutes": 5, "requiresConfirmation": false }
                        """));

        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GIVEN an existing vendor WHEN updating its fields THEN it returns the updated vendor")
    void updateVendor() throws Exception {
        var result = mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Old Name", "timezone": "UTC", "holdTtlMinutes": 5, "requiresConfirmation": false }
                                """))
                .andReturn();
        var id = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(put("/api/vendors/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "New Name", "timezone": "Europe/Athens", "holdTtlMinutes": 10, "requiresConfirmation": true }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.timezone").value("Europe/Athens"))
                .andExpect(jsonPath("$.holdTtlMinutes").value(10))
                .andExpect(jsonPath("$.requiresConfirmation").value(true));
    }

    @Test
    @DisplayName("GIVEN an existing vendor WHEN deleting it THEN it returns 204 and subsequent GET returns 404")
    void deleteVendor() throws Exception {
        var result = mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "To Delete", "timezone": "UTC", "holdTtlMinutes": 5, "requiresConfirmation": false }
                                """))
                .andReturn();
        var id = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(delete("/api/vendors/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vendors/{id}", id))
                .andExpect(status().isNotFound());
    }
}
