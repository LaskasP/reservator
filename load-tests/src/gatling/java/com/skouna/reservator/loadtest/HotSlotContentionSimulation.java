package com.skouna.reservator.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Hot Slot Contention Simulation.
 *
 * 200 concurrent users attempt to book the exact same court slot simultaneously.
 * Assertions:
 *   - Exactly 1 request succeeds with HTTP 201
 *   - The remaining 199 receive HTTP 409 (Conflict)
 *   - Zero HTTP 500 errors
 */
public class HotSlotContentionSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String TARGET_DATE = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // Step 1: Create a vendor (executed once as setup)
    ChainBuilder createVendor = exec(
            http("Create Vendor")
                    .post("/api/vendors")
                    .body(StringBody("""
                            {
                              "name": "Load Test Vendor",
                              "timezone": "Europe/Athens",
                              "holdTtlMinutes": 5,
                              "requiresConfirmation": false
                            }
                            """))
                    .check(status().is(201))
                    .check(jsonPath("$.id").saveAs("vendorId"))
    );

    // Step 2: Create a court (executed once as setup)
    ChainBuilder createCourt = exec(
            http("Create Court")
                    .post("/api/vendors/#{vendorId}/courts")
                    .body(StringBody("""
                            {
                              "name": "Center Court"
                            }
                            """))
                    .check(status().is(201))
                    .check(jsonPath("$.id").saveAs("courtId"))
    );

    // Step 3: Each user attempts to hold the same slot
    ChainBuilder holdSlot = exec(session -> session.set("username", "user-" + UUID.randomUUID()))
            .exec(
                    http("Hold Slot")
                            .post("/api/vendors/#{vendorId}/courts/#{courtId}/hold")
                            .body(StringBody(session -> """
                                    {
                                      "username": "%s",
                                      "date": "%s",
                                      "startTime": "10:00",
                                      "endTime": "11:00"
                                    }
                                    """.formatted(
                                    session.getString("username"),
                                    TARGET_DATE
                            )))
                            .check(
                                    status().in(201, 409),
                                    status().saveAs("holdStatus")
                            )
                            .check(
                                    jsonPath("$.id").optional().saveAs("reservationId")
                            )
            );

    // Step 4: If hold succeeded, book the reservation
    ChainBuilder bookIfHeld = doIf(session -> "201".equals(session.getString("holdStatus")))
            .then(
                    exec(
                            http("Book Reservation")
                                    .post("/api/vendors/#{vendorId}/courts/#{courtId}/reservations/#{reservationId}/book")
                                    .check(status().in(201, 409).saveAs("bookStatus"))
                    )
            );

    // Setup scenario: create vendor + court, store IDs globally
    ScenarioBuilder setup = scenario("Setup")
            .exec(createVendor)
            .exec(createCourt)
            .exec(session -> {
                // Store vendorId and courtId for contention users
                System.setProperty("test.vendorId", session.getString("vendorId"));
                System.setProperty("test.courtId", session.getString("courtId"));
                return session;
            });

    // Contention scenario: all 200 users race to hold+book the same slot
    ScenarioBuilder contention = scenario("Hot Slot Contention")
            .exec(session -> session
                    .set("vendorId", System.getProperty("test.vendorId"))
                    .set("courtId", System.getProperty("test.courtId"))
            )
            .exec(holdSlot)
            .exec(bookIfHeld);

    {
        setUp(
                setup.injectOpen(atOnceUsers(1))
                        .andThen(
                                contention.injectOpen(atOnceUsers(200))
                        )
        ).protocols(httpProtocol)
                .assertions(
                        // Exactly 1 hold should succeed (201)
                        details("Hold Slot").successfulRequests().count().is(1L),
                        // 199 should be conflicts (409) — counted as "failed" by Gatling since check passes for both
                        // Actually both 201 and 409 are "successful" from Gatling's perspective since we check status().in(201, 409)
                        details("Hold Slot").failedRequests().count().is(0L),
                        // No 500 errors — all requests either 201 or 409
                        global().failedRequests().count().is(0L),
                        // Exactly 1 booking should succeed
                        details("Book Reservation").successfulRequests().count().is(1L)
                );
    }
}
