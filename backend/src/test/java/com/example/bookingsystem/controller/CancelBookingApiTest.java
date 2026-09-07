package com.example.bookingsystem.controller;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CancelBookingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private User user1;
    private User user2;
    private Booking user1Booking;

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

        user1Booking = new Booking();
        user1Booking.setSlot(slot);
        user1Booking.setUser(user1);
        user1Booking.setStatus(BookingStatus.ACTIVE);
        user1Booking = bookingRepository.save(user1Booking);
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void user_CanCancelOwnBooking() throws Exception {
        mockMvc.perform(post("/bookings/" + user1Booking.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
                
        Slot slot = slotRepository.findById(user1Booking.getSlot().getId()).orElseThrow();
        assert slot.getStatus() == SlotStatus.AVAILABLE;
    }

    @Test
    @WithMockUser(username = "user2", roles = "USER")
    void user_CannotCancelAnotherUsersBooking() throws Exception {
        mockMvc.perform(post("/bookings/" + user1Booking.getId() + "/cancel"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void user_CannotCancelAlreadyCancelledBooking() throws Exception {
        user1Booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(user1Booking);
        
        mockMvc.perform(post("/bookings/" + user1Booking.getId() + "/cancel"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void user_CannotCancelNonexistentBooking() throws Exception {
        mockMvc.perform(post("/bookings/999999/cancel"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void unauthenticated_CannotCancelBooking() throws Exception {
        mockMvc.perform(post("/bookings/" + user1Booking.getId() + "/cancel"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CannotUseUserCancelEndpoint() throws Exception {
        mockMvc.perform(post("/bookings/" + user1Booking.getId() + "/cancel"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CanCancelAnyBooking() throws Exception {
        mockMvc.perform(post("/admin/bookings/" + user1Booking.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
                
        Slot slot = slotRepository.findById(user1Booking.getSlot().getId()).orElseThrow();
        assert slot.getStatus() == SlotStatus.AVAILABLE;
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CannotCancelAlreadyCancelledBooking() throws Exception {
        user1Booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(user1Booking);
        
        mockMvc.perform(post("/admin/bookings/" + user1Booking.getId() + "/cancel"))
                .andExpect(status().isConflict());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CannotCancelNonexistentBooking() throws Exception {
        mockMvc.perform(post("/admin/bookings/999999/cancel"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void user_CannotUseAdminCancelEndpoint() throws Exception {
        mockMvc.perform(post("/admin/bookings/" + user1Booking.getId() + "/cancel"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void unauthenticated_CannotUseAdminCancelEndpoint() throws Exception {
        mockMvc.perform(post("/admin/bookings/" + user1Booking.getId() + "/cancel"))
                .andExpect(status().isUnauthorized());
    }
}
