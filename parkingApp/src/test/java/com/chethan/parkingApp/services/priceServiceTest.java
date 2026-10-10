package com.chethan.parkingApp.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chethan.parkingApp.services.priceService;
import com.chethan.parkingApp.model.PriceMaster;
import com.chethan.parkingApp.repository.priceRepo;

@ExtendWith(MockitoExtension.class)
//Integrates Mockito with JUnit so mocks are created and verified automatically
class priceServiceTest {

    @Mock
    //Creates a mock of the priceRepo
    private priceRepo priceRepo;

    @InjectMocks
    //Creates the service under test and injects the mocks into it
    // The @InjectMocks annotation is used to create an instance of the class under test and inject the mock dependencies into it. 
    // In this case, it creates an instance of priceService and injects the mocked priceRepo into it.
    private priceService priceService;

        //List of test cases for the priceService class
        // 1. GET ALL PRICES
        // 2. SAVE PRICE
        // 3. UPDATE PRICE
        // 4. UPDATE PRICE - ID NOT FOUND
        // 5. SAVE ALL PRICES
        // 6. DELETE PRICE
        // 7. DELETE PRICE - ID NOT FOUND

    // 1. GET ALL PRICES
    @Test
    void getAllPrices_shouldReturnAllPrices() {

        PriceMaster price1 = new PriceMaster();
        price1.setId(1L);
        price1.setDuration(60);
        price1.setUnitPrice(new BigDecimal("20"));

        PriceMaster price2 = new PriceMaster();
        price2.setId(2L);
        price2.setDuration(120);
        price2.setUnitPrice(new BigDecimal("30"));

        when(priceRepo.findAll())
                .thenReturn(List.of(price1, price2));

        List<PriceMaster> result =
                priceService.getAllPrices();

        assertEquals(2, result.size());

        verify(priceRepo).findAll();
    }


    // 2. SAVE PRICE
    @Test
    void savePrice_shouldSavePrice() {

        PriceMaster price = new PriceMaster();

        price.setDuration(60);
        price.setDisplayDuration("1 hr");
        price.setUnitPrice(new BigDecimal("20"));

        priceService.savePrice(price);

        verify(priceRepo).save(price);
    }


    // 3. UPDATE PRICE
    @Test
    void updatePrice_shouldUpdatePrice() {

        PriceMaster existing = new PriceMaster();

        existing.setId(1L);
        existing.setDuration(60);
        existing.setDisplayDuration("1 hr");
        existing.setUnitPrice(new BigDecimal("20"));

        PriceMaster updated = new PriceMaster();

        updated.setDuration(120);
        updated.setDisplayDuration("2 hr");
        updated.setUnitPrice(new BigDecimal("30"));

        when(priceRepo.findById(1L))
                .thenReturn(Optional.of(existing));

        when(priceRepo.save(any(PriceMaster.class)))
                .thenReturn(existing);

        PriceMaster result =
                priceService.updatePrice(1L, updated);

        assertEquals(
                120,
                result.getDuration()
        );

        assertEquals(
                "2 hr",
                result.getDisplayDuration()
        );

        assertEquals(
                new BigDecimal("30"),
                result.getUnitPrice()
        );

        verify(priceRepo).save(existing);
    }


    // 4. UPDATE PRICE - ID NOT FOUND
    @Test
    void updatePrice_shouldThrowException_whenIdDoesNotExist() {

        PriceMaster updated = new PriceMaster();

        updated.setUnitPrice(new BigDecimal("30"));

        when(priceRepo.findById(99L))
                .thenReturn(Optional.empty());

        NoSuchElementException exception =
                assertThrows(
                        NoSuchElementException.class,
                        () -> priceService.updatePrice(99L, updated)
                );

        assertEquals(
                "No price record found with id 99",
                exception.getMessage()
        );

        verify(priceRepo, never()).save(any());
    }


    // 5. SAVE ALL PRICES
    @Test
    void saveAllPrices_shouldSaveAllPrices() {

        PriceMaster price1 = new PriceMaster();

        price1.setId(1L);
        price1.setDuration(60);
        price1.setUnitPrice(new BigDecimal("20"));

        PriceMaster price2 = new PriceMaster();

        price2.setId(2L);
        price2.setDuration(120);
        price2.setUnitPrice(new BigDecimal("30"));

        List<PriceMaster> prices =
                List.of(price1, price2);

        when(priceRepo.existsById(1L))
                .thenReturn(true);

        when(priceRepo.existsById(2L))
                .thenReturn(true);

        when(priceRepo.saveAll(prices))
                .thenReturn(prices);

        List<PriceMaster> result =
                priceService.saveAllPrices(prices);

        assertEquals(2, result.size());

        verify(priceRepo).saveAll(prices);
    }

    // 6. DELETE PRICE
    @Test
    void deletePrice_shouldDeletePrice() {

        when(priceRepo.existsById(1L))
                .thenReturn(true);

        priceService.deletePrice(1L);

        verify(priceRepo).deleteById(1L);
    }



    // 7. DELETE PRICE - ID NOT FOUND
    @Test
    void deletePrice_shouldThrowException_whenIdDoesNotExist() {

        when(priceRepo.existsById(99L))
                .thenReturn(false);

        NoSuchElementException exception =
                assertThrows(
                        NoSuchElementException.class,
                        () -> priceService.deletePrice(99L)
                );

        assertEquals(
                "No price record found with id 99",
                exception.getMessage()
        );

        verify(priceRepo, never()).deleteById(anyLong());
    }
}