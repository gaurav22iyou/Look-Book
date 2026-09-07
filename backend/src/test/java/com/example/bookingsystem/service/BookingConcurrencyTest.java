package com.example.bookingsystem.service;

import com.example.bookingsystem.dto.request.CreateBookingRequest;
import com.example.bookingsystem.entity.BookingStatus;
import com.example.bookingsystem.entity.Slot;
import com.example.bookingsystem.entity.SlotStatus;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.repository.BookingRepository;
import com.example.bookingsystem.repository.SlotRepository;
import com.example.bookingsystem.repository.UserRepository;
import com.example.bookingsystem.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void testConcurrentBookings_ExactlyOneSucceeds() throws InterruptedException {
        // Prepare slot
        Slot slot = new Slot();
        slot.setStartTime(LocalDateTime.now().plusDays(1));
        slot.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        slot.setStatus(SlotStatus.AVAILABLE);
        slot = slotRepository.save(slot);

        int numberOfUsers = 10;
        User[] users = new User[numberOfUsers];
        for (int i = 0; i < numberOfUsers; i++) {
            User user = new User();
            user.setUsername("concurrent_user_" + i);
            user.setPassword("password");
            user.setRole(com.example.bookingsystem.entity.Role.USER);
            users[i] = userRepository.save(user);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfUsers);
        AtomicInteger successCount = new AtomicInteger(0);

        Long finalSlotId = slot.getId();

        for (int i = 0; i < numberOfUsers; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait for all threads to be ready
                    
                    CreateBookingRequest request = new CreateBookingRequest();
                    request.setSlotId(finalSlotId);
                    
                    bookingService.createBooking(request, users[index].getUsername());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected to fail for 9 out of 10 threads due to ResponseStatusException 409 Conflict
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Start all threads at once
        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        // Assertions
        assertEquals(1, successCount.get(), "Exactly ONE booking should succeed");

        Slot updatedSlot = slotRepository.findById(finalSlotId).orElseThrow();
        assertEquals(SlotStatus.BOOKED, updatedSlot.getStatus(), "Slot status must be BOOKED");

        long activeBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getSlot().getId().equals(finalSlotId) && b.getStatus() == BookingStatus.ACTIVE)
                .count();
        assertEquals(1, activeBookings, "There should be exactly one ACTIVE booking for the slot");
    }
}
