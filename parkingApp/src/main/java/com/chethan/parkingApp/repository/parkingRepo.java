package com.chethan.parkingApp.repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chethan.parkingApp.model.Parking;

@Repository
public interface parkingRepo extends JpaRepository<Parking, Long> {

    Optional<Parking> findByRegistrationNumberAndExitDateIsNullAndFinalCostIsNull(String registrationNumber);

     List<Parking> findByEntryDateBetween(LocalDateTime start, LocalDateTime end);

    public List<Parking> findByExitDateIsNullAndFinalCostIsNull();

    public List<Parking> findTop5ByExitDateIsNotNullAndFinalCostIsNotNullOrderByExitDateDesc();
}
