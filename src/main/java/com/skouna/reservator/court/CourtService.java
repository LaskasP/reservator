package com.skouna.reservator.court;

import com.skouna.reservator.exception.ErrorCodeEnum;
import com.skouna.reservator.exception.ResourceNotFoundException;
import com.skouna.reservator.schedule.Schedule;
import com.skouna.reservator.schedule.ScheduleRepository;
import com.skouna.reservator.vendor.Vendor;
import com.skouna.reservator.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
class CourtService {

    private final CourtRepository courtRepository;
    private final VendorRepository vendorRepository;
    private final ScheduleRepository scheduleRepository;

    Court create(UUID vendorId, CourtDto.CreateRequest request) {
        var vendor = findVendor(vendorId);
        var schedule = findSchedule(request.scheduleId());
        var court = new Court();
        court.setVendor(vendor);
        court.setSchedule(schedule);
        applyFields(court, request);
        return courtRepository.save(court);
    }

    @Transactional(readOnly = true)
    Court get(UUID vendorId, UUID courtId) {
        return courtRepository.findById(courtId)
                .filter(c -> c.getVendor().getId().equals(vendorId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCodeEnum.RESOURCE_NOT_FOUND,
                        "Court not found: " + courtId));
    }

    @Transactional(readOnly = true)
    List<Court> list(UUID vendorId) {
        return courtRepository.findByVendorId(vendorId);
    }

    Court update(UUID vendorId, UUID courtId, CourtDto.CreateRequest request) {
        var court = get(vendorId, courtId);
        var schedule = findSchedule(request.scheduleId());
        court.setSchedule(schedule);
        applyFields(court, request);
        return courtRepository.save(court);
    }

    void delete(UUID vendorId, UUID courtId) {
        var court = get(vendorId, courtId);
        courtRepository.delete(court);
    }

    private void applyFields(Court court, CourtDto.CreateRequest request) {
        court.setName(request.name());
        court.setDescription(request.description());
        court.setSlotDurationMinutes(request.slotDurationMinutes());
    }

    private Vendor findVendor(UUID vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCodeEnum.RESOURCE_NOT_FOUND,
                        "Vendor not found: " + vendorId));
    }

    private Schedule findSchedule(UUID scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCodeEnum.RESOURCE_NOT_FOUND,
                        "Schedule not found: " + scheduleId));
    }
}
