package com.skouna.reservator.schedule;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skouna.reservator.exception.ErrorCodeEnum;
import com.skouna.reservator.exception.ResourceNotFoundException;
import com.skouna.reservator.vendor.Vendor;
import com.skouna.reservator.vendor.VendorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final VendorRepository vendorRepository;

    Schedule create(UUID vendorId, ScheduleDto.CreateRequest request) {
        var vendor = findVendor(vendorId);
        var schedule = new Schedule();
        schedule.setVendor(vendor);
        schedule.setName(request.name());
        applySlots(schedule, request.slots());
        return scheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    Schedule get(UUID vendorId, UUID scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .filter(s -> s.getVendor().getId().equals(vendorId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCodeEnum.RESOURCE_NOT_FOUND,
                        "Schedule not found: " + scheduleId));
    }

    @Transactional(readOnly = true)
    List<Schedule> list(UUID vendorId) {
        return scheduleRepository.findByVendorId(vendorId);
    }

    Schedule update(UUID vendorId, UUID scheduleId, ScheduleDto.CreateRequest request) {
        var schedule = get(vendorId, scheduleId);
        schedule.setName(request.name());
        schedule.getSlots().clear();
        applySlots(schedule, request.slots());
        return scheduleRepository.save(schedule);
    }

    void delete(UUID vendorId, UUID scheduleId) {
        var schedule = get(vendorId, scheduleId);
        scheduleRepository.delete(schedule);
    }

    private void applySlots(Schedule schedule, List<ScheduleDto.SlotRequest> slots) {
        if (slots == null)
            return;
        for (var slotReq : slots) {
            var slot = new ScheduleSlot();
            slot.setSchedule(schedule);
            slot.setDayOfWeek(slotReq.dayOfWeek());
            slot.setOpenTime(slotReq.openTime());
            slot.setCloseTime(slotReq.closeTime());
            schedule.getSlots().add(slot);
        }
    }

    private Vendor findVendor(UUID vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCodeEnum.RESOURCE_NOT_FOUND,
                        "Vendor not found: " + vendorId));
    }
}
