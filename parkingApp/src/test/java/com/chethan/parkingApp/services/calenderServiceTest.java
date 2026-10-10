package com.chethan.parkingApp.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.chethan.parkingApp.services.calenderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chethan.parkingApp.model.Parking;
import com.chethan.parkingApp.repository.parkingRepo;

@ExtendWith(MockitoExtension.class)
class calenderServiceTest {

    @Mock
    private parkingRepo parkingRepo;

    @InjectMocks
    private calenderService calenderService;

    //List of test cases for the calenderService class
    // 1. GET PARKING BY DATE
    // 2. NO PARKING RECORDS

    // 1. GET PARKING BY DATE
    @Test
    void getParkingByDate_shouldReturnParkingRecords() {

        LocalDate date = LocalDate.of(2026, 9, 23);

        Parking parking = new Parking();

        parking.setId(1);
        parking.setRegistrationNumber("KA42KA4428");
        parking.setEntryDate(
                LocalDateTime.of(2026, 9, 23, 10, 30)
        );

        when(parkingRepo.findByEntryDateBetween(
                date.atStartOfDay(),
                date.atTime(java.time.LocalTime.MAX)
        )).thenReturn(List.of(parking));

        List<Parking> result =
                calenderService.getParkingByDate(date);

        assertEquals(1, result.size());

        assertEquals(
                "KA42KA4428",
                result.get(0).getRegistrationNumber()
        );

        verify(parkingRepo).findByEntryDateBetween(
                date.atStartOfDay(),
                date.atTime(java.time.LocalTime.MAX)
        );
    }


    // 2. NO PARKING RECORDS
    @Test
    void getParkingByDate_shouldReturnEmptyList_whenNoRecordsExist() {

        LocalDate date = LocalDate.of(2026, 9, 23);

        when(parkingRepo.findByEntryDateBetween(
                date.atStartOfDay(),
                date.atTime(java.time.LocalTime.MAX)
        )).thenReturn(List.of());

        List<Parking> result =
                calenderService.getParkingByDate(date);

        assertTrue(result.isEmpty());

        verify(parkingRepo).findByEntryDateBetween(
                date.atStartOfDay(),
                date.atTime(java.time.LocalTime.MAX)
        );
    }
}