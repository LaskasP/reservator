package com.skouna.reservator.court;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendors/{vendorId}/courts")
@RequiredArgsConstructor
@Tag(name = "Courts", description = "Manage bookable resources (courts, rooms, etc.) within a vendor")
class CourtController {

    private final CourtService courtService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a court", description = "Registers a bookable resource under a vendor with a fixed slot duration and an assigned schedule.")
    CourtDto.Response create(@PathVariable UUID vendorId, @RequestBody CourtDto.CreateRequest request) {
        return CourtDto.Response.from(courtService.create(vendorId, request));
    }

    @GetMapping("/{courtId}")
    @Operation(summary = "Get a court by ID")
    CourtDto.Response get(@PathVariable UUID vendorId, @PathVariable UUID courtId) {
        return CourtDto.Response.from(courtService.get(vendorId, courtId));
    }

    @GetMapping
    @Operation(summary = "List courts for a vendor")
    List<CourtDto.Response> list(@PathVariable UUID vendorId) {
        return courtService.list(vendorId).stream().map(CourtDto.Response::from).toList();
    }

    @PutMapping("/{courtId}")
    @Operation(summary = "Update a court", description = "Replaces the court's name, description, slot duration, and schedule assignment.")
    CourtDto.Response update(@PathVariable UUID vendorId, @PathVariable UUID courtId,
                             @RequestBody CourtDto.CreateRequest request) {
        return CourtDto.Response.from(courtService.update(vendorId, courtId, request));
    }

    @DeleteMapping("/{courtId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a court", description = "Removes the court and cancels all of its active reservations.")
    void delete(@PathVariable UUID vendorId, @PathVariable UUID courtId) {
        courtService.delete(vendorId, courtId);
    }
}
