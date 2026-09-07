package com.example.bookingsystem.repository;

import com.example.bookingsystem.entity.Booking;
import com.example.bookingsystem.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserUsernameAndStatus(String username, BookingStatus status);
    List<Booking> findByStatus(BookingStatus status);
}
