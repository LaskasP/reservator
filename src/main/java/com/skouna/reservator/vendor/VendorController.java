package com.skouna.reservator.vendor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Manage vendor tenants and their reservation policies")
class VendorController {

    private final VendorService vendorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new vendor", description = "Creates a vendor tenant with its reservation policy (hold TTL, confirmation requirement, booking constraints).")
    VendorDto.Response create(@RequestBody VendorDto.CreateRequest request) {
        return VendorDto.Response.from(vendorService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a vendor by ID")
    VendorDto.Response get(@PathVariable UUID id) {
        return VendorDto.Response.from(vendorService.get(id));
    }

    @GetMapping
    @Operation(summary = "List all vendors")
    List<VendorDto.Response> list() {
        return vendorService.list().stream().map(VendorDto.Response::from).toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a vendor", description = "Replaces the vendor's configuration. Omitted nullable fields are set to null.")
    VendorDto.Response update(@PathVariable UUID id, @RequestBody VendorDto.CreateRequest request) {
        return VendorDto.Response.from(vendorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a vendor", description = "Removes the vendor and cancels all active reservations.")
    void delete(@PathVariable UUID id) {
        vendorService.delete(id);
    }
}
