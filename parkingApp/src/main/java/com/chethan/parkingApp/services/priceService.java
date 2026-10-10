package com.chethan.parkingApp.services;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.chethan.parkingApp.model.PriceMaster;
import com.chethan.parkingApp.repository.priceRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class priceService {
    // Inject the priceRepo dependency
    private final priceRepo priceRepo;

    //  Get all price records
    public List<PriceMaster> getAllPrices() {
        return priceRepo.findAll();
    }

    // Save a new price record
    public void savePrice(PriceMaster price) {
        priceRepo.save(price);
    }

    // Update a specific price record by ID
    @Transactional
    public PriceMaster updatePrice(Long id, PriceMaster updatedData) {
        PriceMaster existing = priceRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "No price record found with id " + id));

        if (updatedData.getDuration() != null) {
            existing.setDuration(updatedData.getDuration());
        }
        if (updatedData.getDisplayDuration() != null) {
            existing.setDisplayDuration(updatedData.getDisplayDuration());
        }
        if (updatedData.getUnitPrice() != null) {
            existing.setUnitPrice(updatedData.getUnitPrice());
        }

        return priceRepo.save(existing);
    }

    
    /**
     * Bulk save for the "edit whole pricing table" form.
     * Rows with an id -> updated in place (full overwrite, not partial).
     * Rows without an id -> inserted as new tiers.
     */
    @Transactional
    public List<PriceMaster> saveAllPrices(
            List<PriceMaster> rows) {

        for (PriceMaster row : rows) {

            if (row.getId() == null) {
                throw new IllegalArgumentException(
                        "ID cannot be null when updating prices");
            }

            if (!priceRepo.existsById(row.getId())) {
                throw new NoSuchElementException(
                        "No price record found with id "
                                + row.getId()
                                + " — cannot update a row that doesn't exist");
            }
        }

        return priceRepo.saveAll(rows);
    }


    // Delete a specific price record by ID
    @Transactional
    public void deletePrice(Long id) {
        if (!priceRepo.existsById(id)) {
            throw new NoSuchElementException("No price record found with id " + id);
        }
        priceRepo.deleteById(id);
    }
}
