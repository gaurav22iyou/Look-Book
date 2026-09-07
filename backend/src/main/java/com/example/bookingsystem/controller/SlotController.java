package com.example.bookingsystem.controller;

import com.example.bookingsystem.dto.request.CreateSlotRequest;
import com.example.bookingsystem.dto.response.SlotResponse;
import com.example.bookingsystem.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/slots")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @PostMapping
    public ResponseEntity<SlotResponse> createSlot(@Valid @RequestBody CreateSlotRequest request) {
        SlotResponse createdSlot = slotService.createSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSlot);
    }

    @GetMapping
    public ResponseEntity<List<SlotResponse>> getSlots() {
        return ResponseEntity.ok(slotService.getAllSlots());
    }
}
