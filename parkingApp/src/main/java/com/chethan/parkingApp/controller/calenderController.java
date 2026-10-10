package com.chethan.parkingApp.controller;
import com.chethan.parkingApp.model.Parking;
import com.chethan.parkingApp.services.calenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "https://parkingapp-frontend.onrender.com")
@RestController
@RequestMapping("/calender")
@RequiredArgsConstructor
public class calenderController {

    private final calenderService service;


    // GET /calender/all?date=2026-09-01
    @GetMapping("/all")
    public List<Parking> parkingRecords(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return service.getParkingByDate(date);
    }
}