package com.example.bookingsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateBookingRequest {
    @NotNull(message = "slotId is required")
    private Long slotId;
}
