package com.example.bookingsystem.dto.response;

import com.example.bookingsystem.entity.SlotStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SlotResponse {
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private SlotStatus status;
    private Long activeBookingId;
}
