package com.skouna.reservator.vendor;

import com.skouna.reservator.exception.ErrorCodeEnum;
import com.skouna.reservator.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
class VendorService {

    private final VendorRepository vendorRepository;

    Vendor create(VendorDto.CreateRequest request) {
        var vendor = new Vendor();
        applyFields(vendor, request);
        return vendorRepository.save(vendor);
    }

    @Transactional(readOnly = true)
    Vendor get(UUID id) {
        return vendorRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(ErrorCodeEnum.RESOURCE_NOT_FOUND,
                                "Vendor not found: " + id));
    }

    @Transactional(readOnly = true)
    List<Vendor> list() {
        return vendorRepository.findAll();
    }

    Vendor update(UUID id, VendorDto.CreateRequest request) {
        var vendor = get(id);
        applyFields(vendor, request);
        return vendorRepository.save(vendor);
    }

    void delete(UUID id) {
        var vendor = get(id);
        vendorRepository.delete(vendor);
    }

    private void applyFields(Vendor vendor, VendorDto.CreateRequest request) {
        vendor.setName(request.name());
        vendor.setTimezone(request.timezone());
        vendor.setHoldTtlMinutes(request.holdTtlMinutes());
        vendor.setRequiresConfirmation(request.requiresConfirmation());
        vendor.setMaxBookAheadDays(request.maxBookAheadDays());
        vendor.setMinBookBeforeMinutes(request.minBookBeforeMinutes());
        vendor.setMinCancelBeforeMinutes(request.minCancelBeforeMinutes());
    }
}
