package com.example.bookingsystem.controller;

import com.example.bookingsystem.dto.response.BookingResponse;
import com.example.bookingsystem.service.BookingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> adminCancelBooking(@PathVariable Long id) {
        BookingResponse response = bookingService.cancelBookingAsAdmin(id);
        return ResponseEntity.ok(response);
    }
}
