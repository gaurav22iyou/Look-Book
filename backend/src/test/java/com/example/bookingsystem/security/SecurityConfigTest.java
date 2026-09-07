package com.example.bookingsystem.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticated_CannotCreateBooking() throws Exception {
        mockMvc.perform(post("/bookings")
                .contentType("application/json")
                .content("{\"slotId\": 1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequest_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/slots"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_CanGetSlots() throws Exception {
        mockMvc.perform(get("/slots"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CanGetSlots() throws Exception {
        mockMvc.perform(get("/slots"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void user_CanCreateBooking() throws Exception {
        mockMvc.perform(post("/bookings")
                .contentType("application/json")
                .content("{\"slotId\": 1}"))
                // Expect 500 or 404 because authorization succeeded and reached service layer
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 404 || status == 500);
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CannotCreateBooking() throws Exception {
        mockMvc.perform(post("/bookings")
                .contentType("application/json")
                .content("{\"slotId\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_CannotCreateSlot() throws Exception {
        mockMvc.perform(post("/slots"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CanCreateSlot() throws Exception {
        mockMvc.perform(post("/slots")
                .contentType("application/json")
                .content("{\"startTime\": \"2026-10-10T10:00:00\", \"endTime\": \"2026-10-10T11:00:00\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_CannotAccessAdminCancel() throws Exception {
        mockMvc.perform(post("/admin/bookings/1/cancel"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CanAccessAdminCancel() throws Exception {
        mockMvc.perform(post("/admin/bookings/1/cancel"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 404 || status == 500);
                });
    }
    
    @Test
    @WithMockUser(roles = "USER")
    void user_CanAccessOwnCancelEndpoint() throws Exception {
        mockMvc.perform(post("/bookings/1/cancel"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 404 || status == 500 || status == 400); // 400 could happen if slot exists but booking not found, wait, 404 is booking not found. Let's just allow 404/500
                });
    }
}
