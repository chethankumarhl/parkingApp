package com.chethan.parkingApp.repository;
import com.chethan.parkingApp.model.PriceMaster;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface priceRepo extends JpaRepository<PriceMaster, Long> {

    List<PriceMaster> findAllByOrderByDurationAsc();
}
