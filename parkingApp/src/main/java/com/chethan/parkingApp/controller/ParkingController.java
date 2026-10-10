package com.chethan.parkingApp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chethan.parkingApp.model.Parking;
import com.chethan.parkingApp.services.ParkingService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:4200") // this is used to solve CORS issue when the frontend is running on a
                                                // different port than the backend
@RestController
@RequestMapping("/parking")
@RequiredArgsConstructor
public class ParkingController {

    // The parking service that will be used to handle the business logic for
    // parking operations
    private final ParkingService service;

    // GET http://localhost:8080/parking/all
    @GetMapping("/all")
    public List<Parking> parkingRecords() {
        return service.getAllParking();
    }

    // GET http://localhost:8080/parking/available
    @GetMapping("/available")
    public List<Parking> availableParkingRecords() {
        return service.getAvailableParking();
    }

    //GET http://localhost:8080/parking/recently-completed
    @GetMapping("/recently-completed")
    public List<Parking> getRecentlyCompleted() {
        return service.getRecentlyCompleted();
    }

    // GET http://localhost:8080/parking/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Parking> getParkingById(@PathVariable Long id) {
        Parking parking = service.getParkingById(id);
        return ResponseEntity.ok(parking);
    }

    // POST http://localhost:8080/parking/add
    @PostMapping("/add")
    public ResponseEntity<String> addParking(
            @RequestBody Parking parking) {

        String result = service.saveParking(parking);

        return ResponseEntity.ok(result);
    }

    // PUT http://localhost:8080/parking/update/{id}
    @PutMapping("/update/{id}")
    public ResponseEntity<Parking> updateParking(
            @PathVariable Long id,
            @RequestBody Parking parking) {

        Parking updated = service.updateParking(id, parking);

        return ResponseEntity.ok(updated);
    }

    // DELETE http://localhost:8080/parking/delete/{id}
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteParking(
            @PathVariable Long id) {

        service.deleteParking(id);

        return ResponseEntity.ok(
                "Parking record with id " + id +
                        " deleted successfully");
    }
}