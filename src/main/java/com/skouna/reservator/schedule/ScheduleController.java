package com.skouna.reservator.schedule;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendors/{vendorId}/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Manage reusable operating-hour schedules assigned to courts")
class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a schedule", description = "Creates a named schedule with per-day-of-week operating hours. Assign it to courts via the Courts API.")
    ScheduleDto.Response create(@PathVariable UUID vendorId, @RequestBody ScheduleDto.CreateRequest request) {
        return ScheduleDto.Response.from(scheduleService.create(vendorId, request));
    }

    @GetMapping("/{scheduleId}")
    @Operation(summary = "Get a schedule by ID")
    ScheduleDto.Response get(@PathVariable UUID vendorId, @PathVariable UUID scheduleId) {
        return ScheduleDto.Response.from(scheduleService.get(vendorId, scheduleId));
    }

    @GetMapping
    @Operation(summary = "List schedules for a vendor")
    List<ScheduleDto.Response> list(@PathVariable UUID vendorId) {
        return scheduleService.list(vendorId).stream().map(ScheduleDto.Response::from).toList();
    }

    @PutMapping("/{scheduleId}")
    @Operation(summary = "Update a schedule", description = "Replaces the schedule's name and all time slots.")
    ScheduleDto.Response update(@PathVariable UUID vendorId, @PathVariable UUID scheduleId,
                                @RequestBody ScheduleDto.CreateRequest request) {
        return ScheduleDto.Response.from(scheduleService.update(vendorId, scheduleId, request));
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a schedule")
    void delete(@PathVariable UUID vendorId, @PathVariable UUID scheduleId) {
        scheduleService.delete(vendorId, scheduleId);
    }
}
