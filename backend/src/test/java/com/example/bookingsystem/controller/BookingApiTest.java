package com.example.bookingsystem.controller;

import com.example.bookingsystem.entity.Booking;
import com.example.bookingsystem.entity.BookingStatus;
import com.example.bookingsystem.entity.Slot;
import com.example.bookingsystem.entity.SlotStatus;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.entity.Role;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void setup() {
        bookingRepository.deleteAll();
        slotRepository.deleteAll();
        userRepository.deleteAll();
        
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setRole(Role.USER);
        userRepository.save(user);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void user_CanBookAvailableSlot() throws Exception {
        Slot slot = new Slot();
        slot.setStartTime(LocalDateTime.now().plusDays(1));
        slot.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        slot.setStatus(SlotStatus.AVAILABLE);
        slot = slotRepository.save(slot);

        mockMvc.perform(post("/bookings")
                .contentType("application/json")
                .content("{\"slotId\": " + slot.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.userId").isNumber());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void user_CannotBookAlreadyBookedSlot() throws Exception {
        Slot slot = new Slot();
        slot.setStartTime(LocalDateTime.now().plusDays(1));
        slot.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        slot.setStatus(SlotStatus.BOOKED);
        slot = slotRepository.save(slot);

        mockMvc.perform(post("/bookings")
                .contentType("application/json")
                .content("{\"slotId\": " + slot.getId() + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void user_CannotBookNonexistentSlot() throws Exception {
        mockMvc.perform(post("/bookings")
                .contentType("application/json")
                .content("{\"slotId\": 999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_CannotBook() throws Exception {
        mockMvc.perform(post("/bookings")
                .contentType("application/json")
                .content("{\"slotId\": 1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CannotBook() throws Exception {
        mockMvc.perform(post("/bookings")
                .contentType("application/json")
                .content("{\"slotId\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void user_CanGetOwnActiveBookings() throws Exception {
        Slot slot = new Slot();
        slot.setStartTime(LocalDateTime.now().plusDays(1));
        slot.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        slot.setStatus(SlotStatus.BOOKED);
        slot = slotRepository.save(slot);

        User user = userRepository.findByUsername("testuser").orElseThrow();

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setUser(user);
        booking.setStatus(BookingStatus.ACTIVE);
        bookingRepository.save(booking);

        mockMvc.perform(get("/bookings")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].slotId").value(slot.getId()));
    }

    @Test
    @WithMockUser(username = "otheruser", roles = "USER")
    void user_CannotGetAnotherUsersBookings() throws Exception {
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setPassword("password");
        otherUser.setRole(Role.USER);
        userRepository.save(otherUser);

        Slot slot = new Slot();
        slot.setStartTime(LocalDateTime.now().plusDays(1));
        slot.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        slot.setStatus(SlotStatus.BOOKED);
        slot = slotRepository.save(slot);

        User testuser = userRepository.findByUsername("testuser").orElseThrow();

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setUser(testuser); // Booking belongs to testuser
        booking.setStatus(BookingStatus.ACTIVE);
        bookingRepository.save(booking);

        // otheruser requests their bookings, should be empty
        mockMvc.perform(get("/bookings")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unauthenticated_CannotGetBookings() throws Exception {
        mockMvc.perform(get("/bookings")
                .contentType("application/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void cancelledBookingsAreNotReturned() throws Exception {
        Slot slot = new Slot();
        slot.setStartTime(LocalDateTime.now().plusDays(1));
        slot.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        slot.setStatus(SlotStatus.AVAILABLE);
        slot = slotRepository.save(slot);

        User user = userRepository.findByUsername("testuser").orElseThrow();

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setUser(user);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        mockMvc.perform(get("/bookings")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
