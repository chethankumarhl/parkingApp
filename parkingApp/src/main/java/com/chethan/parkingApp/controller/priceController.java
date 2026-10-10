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

import com.chethan.parkingApp.model.PriceMaster;
import com.chethan.parkingApp.services.priceService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequiredArgsConstructor
@RequestMapping("/price")
public class priceController {
    private final priceService service;

    // Get all price records
    // http://localhost:8080/price/all
    @GetMapping("/all")
    public List<PriceMaster> priceRecords() {
        return service.getAllPrices();
    }

    // Get a specific price record by ID
    // http://localhost:8080/price/add
    /*{
        "id": 1,
        "duration": 60,
        "displayDuration": "0 - 1 hr",
        "unitPrice": 20.00
    } */
    @PostMapping("/add")
    public String postMethodName(@RequestBody PriceMaster price) {
        service.savePrice(price);
        return "Price record saved successfully";
    }


    // Update a specific price record by ID
    // http://localhost:8080/price/update/1
    /*{
    
        "id": 1,
        "duration": 60,
        "displayDuration": "0 - 1 hr",
        "unitPrice": 25.00
    } */
    @PutMapping("/update/{id}")
    public ResponseEntity<PriceMaster> updatePrice(
            @PathVariable Long id,
            @RequestBody PriceMaster price) {
        PriceMaster updated = service.updatePrice(id, price);
        return ResponseEntity.ok(updated);
    }


    // Bulk save for the "edit all prices" form screen
    // from frontend, send a list of PriceMaster objects in the request body
    /*[
    { "id": 1, "duration": 60,  "displayDuration": "0 - 1 hr",  "unitPrice": 20.00 },
    { "id": 2, "duration": 180, "displayDuration": "1 - 3 hr",  "unitPrice": 15.00 },
    { "id": 3, "duration": 360, "displayDuration": "3 - 6 hr",  "unitPrice": 12.00 },
    { "id": 4, "duration": 720, "displayDuration": "6 - 12 hr", "unitPrice": 10.00 }
    ] */

    @PutMapping("/update-all")
    public ResponseEntity<List<PriceMaster>> updateAllPrices(
            @RequestBody List<PriceMaster> prices) {
        List<PriceMaster> saved = service.saveAllPrices(prices);
        return ResponseEntity.ok(saved);
    }

    // Delete a specific price record by ID
    // http://localhost:8080/price/delete/1
    
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deletePrice(@PathVariable Long id) {
        service.deletePrice(id);
        return ResponseEntity.ok("Price record with id " + id + " deleted successfully");
    }
    

}
