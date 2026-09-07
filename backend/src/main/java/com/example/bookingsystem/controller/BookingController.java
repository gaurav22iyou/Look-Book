package com.example.bookingsystem.controller;

import com.example.bookingsystem.dto.request.CreateBookingRequest;
import com.example.bookingsystem.dto.response.BookingResponse;
import com.example.bookingsystem.security.CustomUserDetails;
import com.example.bookingsystem.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            Principal principal,
            @Valid @RequestBody CreateBookingRequest request) {
        
        BookingResponse response = bookingService.createBooking(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getUserBookings(Principal principal) {
        List<BookingResponse> response = bookingService.getUserBookings(principal.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            Principal principal,
            @PathVariable Long id) {
        BookingResponse response = bookingService.cancelBooking(id, principal.getName());
        return ResponseEntity.ok(response);
    }
}
