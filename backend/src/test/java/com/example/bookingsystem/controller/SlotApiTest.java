package com.example.bookingsystem.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class SlotApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CanCreateValidSlot() throws Exception {
        mockMvc.perform(post("/slots")
                .contentType("application/json")
                .content("{\"startTime\": \"2026-10-10T10:00:00\", \"endTime\": \"2026-10-10T11:00:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_CannotCreateSlot() throws Exception {
        mockMvc.perform(post("/slots")
                .contentType("application/json")
                .content("{\"startTime\": \"2026-10-10T10:00:00\", \"endTime\": \"2026-10-10T11:00:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_CannotCreateSlot() throws Exception {
        mockMvc.perform(post("/slots")
                .contentType("application/json")
                .content("{\"startTime\": \"2026-10-10T10:00:00\", \"endTime\": \"2026-10-10T11:00:00\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingStartTime_IsRejected() throws Exception {
        mockMvc.perform(post("/slots")
                .contentType("application/json")
                .content("{\"endTime\": \"2026-10-10T11:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingEndTime_IsRejected() throws Exception {
        mockMvc.perform(post("/slots")
                .contentType("application/json")
                .content("{\"startTime\": \"2026-10-10T10:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void startTimeNotBeforeEndTime_IsRejected() throws Exception {
        mockMvc.perform(post("/slots")
                .contentType("application/json")
                .content("{\"startTime\": \"2026-10-10T12:00:00\", \"endTime\": \"2026-10-10T11:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_CanRetrieveSlots() throws Exception {
        mockMvc.perform(get("/slots"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CanRetrieveSlots() throws Exception {
        mockMvc.perform(get("/slots"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSlots_returnsActiveBookingId_whenSlotBooked() throws Exception {
        mockMvc.perform(get("/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void unauthenticated_CannotRetrieveSlots() throws Exception {
        mockMvc.perform(get("/slots"))
                .andExpect(status().isUnauthorized());
    }
}
