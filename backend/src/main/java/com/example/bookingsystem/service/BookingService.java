package com.example.bookingsystem.service;

import com.example.bookingsystem.dto.request.CreateBookingRequest;
import com.example.bookingsystem.dto.response.BookingResponse;
import com.example.bookingsystem.entity.Booking;
import com.example.bookingsystem.entity.BookingStatus;
import com.example.bookingsystem.entity.Slot;
import com.example.bookingsystem.entity.SlotStatus;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.repository.BookingRepository;
import com.example.bookingsystem.repository.SlotRepository;
import com.example.bookingsystem.repository.UserRepository;
import com.example.bookingsystem.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bookingsystem.exception.*;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, SlotRepository slotRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        // Acquire pessimistic write lock on the slot
        Slot slot = slotRepository.findByIdWithPessimisticWriteLock(request.getSlotId())
                .orElseThrow(() -> new SlotNotFoundException("Slot not found"));

        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new SlotAlreadyBookedException("Slot is already booked");
        }

        // Change slot status
        slot.setStatus(SlotStatus.BOOKED);
        
        // Create booking
        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setUser(user);
        booking.setStatus(BookingStatus.ACTIVE);
        
        Booking savedBooking = bookingRepository.save(booking);

        return mapToResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String username) {
        return bookingRepository.findByUserUsernameAndStatus(username, BookingStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String username) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
        
        if (!booking.getUser().getUsername().equals(username)) {
            throw new UnauthorizedBookingException("You cannot cancel another user's booking");
        }
        
        return processCancellation(booking);
    }

    @Transactional
    public BookingResponse cancelBookingAsAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
        
        return processCancellation(booking);
    }

    private BookingResponse processCancellation(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Booking is already cancelled");
        }

        // Lock the associated slot using pessimistic write locking
        Slot slot = slotRepository.findByIdWithPessimisticWriteLock(booking.getSlot().getId())
                .orElseThrow(() -> new SlotNotFoundException("Associated slot not found"));
        
        // Final sanity check after acquiring the lock to handle any weird racing updates
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        slot.setStatus(SlotStatus.AVAILABLE);

        return mapToResponse(booking);
    }

    private BookingResponse mapToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setSlotId(booking.getSlot().getId());
        response.setUserId(booking.getUser().getId());
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }
}
