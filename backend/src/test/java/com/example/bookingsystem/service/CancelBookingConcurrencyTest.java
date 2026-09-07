package com.example.bookingsystem.service;

import com.example.bookingsystem.dto.request.CreateBookingRequest;
import com.example.bookingsystem.entity.Booking;
import com.example.bookingsystem.entity.BookingStatus;
import com.example.bookingsystem.entity.Role;
import com.example.bookingsystem.entity.Slot;
import com.example.bookingsystem.entity.SlotStatus;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.repository.BookingRepository;
import com.example.bookingsystem.repository.SlotRepository;
import com.example.bookingsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class CancelBookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BookingRepository bookingRepository;

    private Long slotId;
    private Long bookingId;
    private User user1;
    private User user2;

    @BeforeEach
    void setup() {
        bookingRepository.deleteAll();
        slotRepository.deleteAll();
        userRepository.deleteAll();

        user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("password");
        user1.setRole(Role.USER);
        user1 = userRepository.save(user1);
        
        user2 = new User();
        user2.setUsername("user2");
        user2.setPassword("password");
        user2.setRole(Role.USER);
        user2 = userRepository.save(user2);

        Slot slot = new Slot();
        slot.setStartTime(LocalDateTime.now().plusDays(1));
        slot.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        slot.setStatus(SlotStatus.BOOKED);
        slot = slotRepository.save(slot);
        slotId = slot.getId();
        
        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setUser(user1);
        booking.setStatus(BookingStatus.ACTIVE);
        booking = bookingRepository.save(booking);
        bookingId = booking.getId();
    }

    @Test
    void concurrentCancelAndBook_ShouldMaintainConsistency() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successfulCancels = new AtomicInteger(0);
        AtomicInteger successfulBooks = new AtomicInteger(0);
        AtomicInteger failedBooks = new AtomicInteger(0);

        // Thread 1: User 1 cancels the booking
        executorService.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                
                bookingService.cancelBooking(bookingId, "user1");
                successfulCancels.incrementAndGet();
            } catch (Exception e) {
                // Ignore exception
            } finally {
                doneLatch.countDown();
            }
        });
        
        // Thread 2: User 2 tries to book the SAME slot
        executorService.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                
                CreateBookingRequest request = new CreateBookingRequest();
                request.setSlotId(slotId);
                
                bookingService.createBooking(request, "user2");
                successfulBooks.incrementAndGet();
            } catch (Exception e) {
                failedBooks.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        // Wait for all threads to be ready
        readyLatch.await();
        // Release the hounds
        startLatch.countDown();
        // Wait for all threads to finish
        doneLatch.await();
        executorService.shutdown();

        // Verification
        // Exactly one cancel should have succeeded
        assert successfulCancels.get() == 1;
        
        Slot finalSlot = slotRepository.findById(slotId).orElseThrow();
        long activeBookings = bookingRepository.findAll().stream()
            .filter(b -> b.getStatus() == BookingStatus.ACTIVE).count();
            
        // If booking thread ran AFTER cancel thread committed, the booking succeeds and slot is BOOKED again.
        // If booking thread ran BEFORE cancel thread committed, the booking fails (Conflict) and slot is AVAILABLE.
        
        if (successfulBooks.get() == 1) {
            // Book ran second
            assert finalSlot.getStatus() == SlotStatus.BOOKED;
            assert activeBookings == 1;
        } else {
            // Book ran first (and failed because slot was currently BOOKED by user1's active booking)
            assert finalSlot.getStatus() == SlotStatus.AVAILABLE;
            assert activeBookings == 0;
            assert failedBooks.get() == 1;
        }
    }
}
