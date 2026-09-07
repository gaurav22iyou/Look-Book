package com.example.bookingsystem.dto.response;

import com.example.bookingsystem.entity.BookingStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingResponse {
    private Long id;
    private Long slotId;
    private Long userId;
    private BookingStatus status;
    private LocalDateTime createdAt;
}
