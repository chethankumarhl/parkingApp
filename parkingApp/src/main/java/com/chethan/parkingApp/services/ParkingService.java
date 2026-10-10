package com.chethan.parkingApp.services;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.chethan.parkingApp.model.Parking;
import com.chethan.parkingApp.model.PriceMaster;
import com.chethan.parkingApp.repository.parkingRepo;
import com.chethan.parkingApp.repository.priceRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParkingService {

    private final parkingRepo parkingRepo; //repository for Parking entity
    private final priceRepo priceRepo; //repository for PriceMaster entity

    // get method to get all parking records
    // controller : get GetMapping("/all")
    public List<Parking> getAllParking() {
        return parkingRepo.findAll();
    }

    // get method to get parking record by id
    // controller : get GetMapping("/{id}")
    public Parking getParkingById(Long id) {
        return parkingRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "No parking record found with id " + id));
    }

    // get method to get available parking records
    // controller : get GetMapping("/available")
    public List<Parking> getAvailableParking() {
        return parkingRepo.findByExitDateIsNullAndFinalCostIsNull();
    }

    // get method to get recently completed parking records
    // controller : get GetMapping("/recently-completed")
    public List<Parking> getRecentlyCompleted() {
    return parkingRepo.findTop5ByExitDateIsNotNullAndFinalCostIsNotNullOrderByExitDateDesc();
}

    // save method to save parking record
    // controller : post PostMapping("/add")
    @Transactional
    public String saveParking(Parking parking) {
        if (parking.getRegistrationNumber() == null
                || parking.getRegistrationNumber().isBlank()) {
            throw new IllegalArgumentException("registrationNumber is required");
        }

        Optional<Parking> openRecord = parkingRepo
                .findByRegistrationNumberAndExitDateIsNullAndFinalCostIsNull(
                        parking.getRegistrationNumber());

        if (openRecord.isEmpty()) {
            // No open record -> this is an entry
            parking.setEntryDate(LocalDateTime.now());
            parking.setExitDate(null);
            parking.setFinalCost(null);
            parking.setParkedTime(null);
            parkingRepo.save(parking);
            return "Entry recorded for " + parking.getRegistrationNumber();
        }

        // Open record exists -> this is an exit
        Parking existing = openRecord.get();
        LocalDateTime now = LocalDateTime.now();
        Duration parkedDuration = Duration.between(existing.getEntryDate(), now);

        BigDecimal finalCost = calculateCost(parkedDuration);

        existing.setExitDate(now);
        existing.setParkedTime(formatDuration(parkedDuration));
        existing.setFinalCost(finalCost.setScale(0, RoundingMode.HALF_UP).intValue());

        parkingRepo.save(existing);
        return "Exit recorded for " + existing.getRegistrationNumber()
                + ", cost = " + existing.getFinalCost();
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        return String.format(
                "%d days %02d:%02d:%02d",
                days, hours, minutes, seconds);
    }

    private BigDecimal calculateCost(Duration parkedDuration) {
        List<PriceMaster> tiers = priceRepo.findAllByOrderByDurationAsc();
        if (tiers.isEmpty()) {
            throw new IllegalStateException("PRICE_MASTER has no configured tiers");
        }

        long parkedMinutes = parkedDuration.toMinutes();

        BigDecimal unitPrice = null;
        for (PriceMaster tier : tiers) {
            if (tier.getDuration() == null) {
                // catch-all tier (everything above the last threshold)
                unitPrice = tier.getUnitPrice();
                break;
            }
            if (parkedMinutes < tier.getDuration()) {
                unitPrice = tier.getUnitPrice();
                break;
            }
        }
        if (unitPrice == null) {
            // parked longer than every explicit tier and no catch-all row exists
            unitPrice = tiers.get(tiers.size() - 1).getUnitPrice();
        }

        BigDecimal parkedHours = BigDecimal.valueOf(parkedMinutes)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        return parkedHours.multiply(unitPrice);
    }


    // update method to update parking record
    // controller : put PutMapping("/update/{id}")
    @Transactional
    public Parking updateParking(Long id, Parking updatedData) {
        Parking existing = parkingRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "No parking record found with id " + id));

        // Only update fields that make sense to edit manually.
        // entryDate/exitDate/finalCost are normally system-calculated,
        // but PUT allows manual correction (e.g. admin fixing a data-entry mistake).
        if (updatedData.getRegistrationNumber() != null) {
            existing.setRegistrationNumber(updatedData.getRegistrationNumber());
        }
        if (updatedData.getEntryDate() != null) {
            existing.setEntryDate(updatedData.getEntryDate());
        }
        if (updatedData.getExitDate() != null) {
            existing.setExitDate(updatedData.getExitDate());
        }
        if (updatedData.getParkedTime() != null) {
            existing.setParkedTime(updatedData.getParkedTime());
        }
        if (updatedData.getFinalCost() != null) {
            existing.setFinalCost(updatedData.getFinalCost());
        }

        return parkingRepo.save(existing);
    }

    // delete method to delete parking record
    // controller : delete DeleteMapping("/delete/{id}")
    @Transactional
    public void deleteParking(Long id) {
        if (!parkingRepo.existsById(id)) {
            throw new NoSuchElementException("No parking record found with id " + id);
        }
        parkingRepo.deleteById(id);
    }
}