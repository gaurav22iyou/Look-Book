package com.example.bookingsystem.service;

import com.example.bookingsystem.dto.request.CreateSlotRequest;
import com.example.bookingsystem.dto.response.SlotResponse;
import com.example.bookingsystem.entity.Booking;
import com.example.bookingsystem.entity.BookingStatus;
import com.example.bookingsystem.entity.Slot;
import com.example.bookingsystem.entity.SlotStatus;
import com.example.bookingsystem.repository.BookingRepository;
import com.example.bookingsystem.repository.SlotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SlotService {

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;

    public SlotService(SlotRepository slotRepository, BookingRepository bookingRepository) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
    }

    public SlotResponse createSlot(CreateSlotRequest request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }

        Slot slot = new Slot();
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setStatus(SlotStatus.AVAILABLE);

        Slot savedSlot = slotRepository.save(slot);
        return mapToResponse(savedSlot, null);
    }

    public List<SlotResponse> getAllSlots() {
        List<Slot> slots = slotRepository.findAll();
        
        // Map slotId -> active bookingId for fast lookup
        Map<Long, Long> activeBookingMap = bookingRepository.findByStatus(BookingStatus.ACTIVE).stream()
                .filter(b -> b.getSlot() != null && b.getSlot().getId() != null)
                .collect(Collectors.toMap(
                        b -> b.getSlot().getId(),
                        Booking::getId,
                        (existing, replacement) -> existing
                ));

        return slots.stream()
                .map(slot -> {
                    Long activeBookingId = (slot.getStatus() == SlotStatus.BOOKED)
                            ? activeBookingMap.get(slot.getId())
                            : null;
                    return mapToResponse(slot, activeBookingId);
                })
                .collect(Collectors.toList());
    }

    private SlotResponse mapToResponse(Slot slot, Long activeBookingId) {
        SlotResponse response = new SlotResponse();
        response.setId(slot.getId());
        response.setStartTime(slot.getStartTime());
        response.setEndTime(slot.getEndTime());
        response.setStatus(slot.getStatus());
        response.setActiveBookingId(activeBookingId);
        return response;
    }
}
