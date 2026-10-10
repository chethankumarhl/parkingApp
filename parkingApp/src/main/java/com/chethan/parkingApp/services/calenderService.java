package com.chethan.parkingApp.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.chethan.parkingApp.model.Parking;
import com.chethan.parkingApp.repository.parkingRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class calenderService {

    private final parkingRepo parkingRepo;

    // Get parking records for a specific date
    public List<Parking> getParkingByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return parkingRepo.findByEntryDateBetween(startOfDay, endOfDay);
    }
}