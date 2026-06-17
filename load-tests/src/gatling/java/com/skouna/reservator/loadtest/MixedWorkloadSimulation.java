package com.skouna.reservator.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Mixed Workload Simulation.
 *
 * 200 req/s mixed traffic:
 *   - 40% GET availability
 *   - 25% hold
 *   - 20% book
 *   - 10% cancel
 *   -  5% admin CRUD (vendor creation)
 *
 * Assertions:
 *   - p95 response time < 200ms
 *   - p99 response time < 500ms
 */
public class MixedWorkloadSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // Helper to generate a date within the next 7 days
    private static String randomFutureDate() {
        int daysAhead = ThreadLocalRandom.current().nextInt(1, 8);
        return LocalDate.now().plusDays(daysAhead).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    // Helper to generate a random hour-based time slot
    private static String[] randomTimeSlot() {
        int startHour = ThreadLocalRandom.current().nextInt(8, 20);
        return new String[]{
                String.format("%02d:00", startHour),
                String.format("%02d:00", startHour + 1)
        };
    }

    // --- Setup: create a vendor and court for load test traffic ---

    ChainBuilder setupVendorAndCourt = exec(
            http("Setup - Create Vendor")
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
    ).exec(
            http("Setup - Create Court")
                    .post("/api/vendors/#{vendorId}/courts")
                    .body(StringBody("""
                            {
                              "name": "Court 1"
                            }
                            """))
                    .check(status().is(201))
                    .check(jsonPath("$.id").saveAs("courtId"))
    ).exec(session -> {
        System.setProperty("test.vendorId", session.getString("vendorId"));
        System.setProperty("test.courtId", session.getString("courtId"));
        return session;
    });

    // --- Workload chains ---

    ChainBuilder injectTestContext = exec(session -> session
            .set("vendorId", System.getProperty("test.vendorId"))
            .set("courtId", System.getProperty("test.courtId"))
    );

    // 40% — GET availability
    ChainBuilder getAvailability = exec(session -> session.set("queryDate", randomFutureDate()))
            .exec(
                    http("GET Availability")
                            .get("/api/vendors/#{vendorId}/courts/#{courtId}/availability")
                            .queryParam("date", "#{queryDate}")
                            .check(status().in(200, 404))
            );

    // 25% — Hold a slot
    ChainBuilder holdSlot = exec(session -> {
        String[] slot = randomTimeSlot();
        return session
                .set("username", "user-" + UUID.randomUUID())
                .set("holdDate", randomFutureDate())
                .set("startTime", slot[0])
                .set("endTime", slot[1]);
    }).exec(
            http("Hold Slot")
                    .post("/api/vendors/#{vendorId}/courts/#{courtId}/hold")
                    .body(StringBody(session -> """
                            {
                              "username": "%s",
                              "date": "%s",
                              "startTime": "%s",
                              "endTime": "%s"
                            }
                            """.formatted(
                            session.getString("username"),
                            session.getString("holdDate"),
                            session.getString("startTime"),
                            session.getString("endTime")
                    )))
                    .check(status().in(201, 409))
                    .check(jsonPath("$.id").optional().saveAs("reservationId"))
    );

    // 20% — Book a reservation (hold first, then book)
    ChainBuilder bookReservation = exec(session -> {
        String[] slot = randomTimeSlot();
        return session
                .set("username", "booker-" + UUID.randomUUID())
                .set("bookDate", randomFutureDate())
                .set("startTime", slot[0])
                .set("endTime", slot[1]);
    }).exec(
            http("Book - Hold First")
                    .post("/api/vendors/#{vendorId}/courts/#{courtId}/hold")
                    .body(StringBody(session -> """
                            {
                              "username": "%s",
                              "date": "%s",
                              "startTime": "%s",
                              "endTime": "%s"
                            }
                            """.formatted(
                            session.getString("username"),
                            session.getString("bookDate"),
                            session.getString("startTime"),
                            session.getString("endTime")
                    )))
                    .check(status().in(201, 409))
                    .check(jsonPath("$.id").optional().saveAs("reservationId"))
    ).doIf(session -> session.contains("reservationId")).then(
            exec(
                    http("Book Reservation")
                            .post("/api/vendors/#{vendorId}/courts/#{courtId}/reservations/#{reservationId}/book")
                            .check(status().in(200, 201, 409))
            )
    );

    // 10% — Cancel a reservation (hold, book, then cancel)
    ChainBuilder cancelReservation = exec(session -> {
        String[] slot = randomTimeSlot();
        return session
                .set("username", "canceller-" + UUID.randomUUID())
                .set("cancelDate", randomFutureDate())
                .set("startTime", slot[0])
                .set("endTime", slot[1]);
    }).exec(
            http("Cancel - Hold First")
                    .post("/api/vendors/#{vendorId}/courts/#{courtId}/hold")
                    .body(StringBody(session -> """
                            {
                              "username": "%s",
                              "date": "%s",
                              "startTime": "%s",
                              "endTime": "%s"
                            }
                            """.formatted(
                            session.getString("username"),
                            session.getString("cancelDate"),
                            session.getString("startTime"),
                            session.getString("endTime")
                    )))
                    .check(status().in(201, 409))
                    .check(jsonPath("$.id").optional().saveAs("reservationId"))
    ).doIf(session -> session.contains("reservationId")).then(
            exec(
                    http("Cancel - Book First")
                            .post("/api/vendors/#{vendorId}/courts/#{courtId}/reservations/#{reservationId}/book")
                            .check(status().in(200, 201, 409))
            ).exec(
                    http("Cancel Reservation")
                            .patch("/api/vendors/#{vendorId}/courts/#{courtId}/reservations/#{reservationId}/cancel")
                            .check(status().in(200, 204, 409))
            )
    );

    // 5% — Admin CRUD (create a new vendor)
    ChainBuilder adminCrud = exec(
            http("Admin - Create Vendor")
                    .post("/api/vendors")
                    .body(StringBody(session -> """
                            {
                              "name": "Vendor-%s",
                              "timezone": "Europe/Athens",
                              "holdTtlMinutes": 5,
                              "requiresConfirmation": false
                            }
                            """.formatted(UUID.randomUUID().toString().substring(0, 8))))
                    .check(status().in(200, 201))
    );

    // --- Scenarios ---

    ScenarioBuilder setupScenario = scenario("Setup")
            .exec(setupVendorAndCourt);

    ScenarioBuilder mixedWorkload = scenario("Mixed Workload")
            .exec(injectTestContext)
            .randomSwitch().on(
                    percent(40.0).then(exec(getAvailability)),
                    percent(25.0).then(exec(holdSlot)),
                    percent(20.0).then(exec(bookReservation)),
                    percent(10.0).then(exec(cancelReservation)),
                    percent(5.0).then(exec(adminCrud))
            );

    {
        setUp(
                setupScenario.injectOpen(atOnceUsers(1))
                        .andThen(
                                mixedWorkload.injectOpen(
                                        constantUsersPerSec(200).during(60)
                                )
                        )
        ).protocols(httpProtocol)
                .assertions(
                        global().responseTime().percentile(95.0).lt(200),
                        global().responseTime().percentile(99.0).lt(500)
                );
    }
}
