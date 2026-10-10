package com.chethan.parkingApp.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chethan.parkingApp.model.Parking;
import com.chethan.parkingApp.model.PriceMaster;
import com.chethan.parkingApp.repository.parkingRepo;
import com.chethan.parkingApp.repository.priceRepo;
@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {
	@Mock
	//@Mock	Creates a mock of parkingRepo and priceRepo
	private parkingRepo parkingRepo;
	@Mock
	private priceRepo priceRepo;
	@InjectMocks
    private ParkingService parkingService;
	
	private Parking parking;
	private PriceMaster price1;
	private PriceMaster price2;
	private PriceMaster price3;
	
	//List of test cases for the ParkingService class
	// 1. GET ALL PARKING RECORDS
	// 2. GET PARKING BY ID
	// 3. SAVE PARKING - ENTRY
	// 4. SAVE PARKING - EXIT
	// 5. UPDATE PARKING
	// 6. DELETE PARKING

	 @BeforeEach
	 //@BeforeEach	Creates fresh fixtures (Parking and PriceMaster objects) before each test in ParkingServiceTest
	    void setUp() {

	        parking = new Parking();

	        parking.setId(1);
	        parking.setRegistrationNumber("KA42KA4428");

	        price1 = new PriceMaster();
	        price1.setId(1L);
	        price1.setDuration(60);
	        price1.setUnitPrice(new BigDecimal("20"));

	        price2 = new PriceMaster();
	        price2.setId(2L);
	        price2.setDuration(120);
	        price2.setUnitPrice(new BigDecimal("30"));

	        price3 = new PriceMaster();
	        price3.setId(3L);
	        price3.setDuration(null);
	        price3.setUnitPrice(new BigDecimal("50"));
	    }

	//to get all the data from db  getAllParking()
	@Test
	//@Test Marks a method as a test case
	void getAllParking_shouldReturnAllParkingRecords() {
		  Parking parking2 = new Parking();
	        parking2.setId(2);
	        parking2.setRegistrationNumber("KA01AB1234");

	        List<Parking> parkingList = List.of(parking, parking2);

	        when(parkingRepo.findAll()).thenReturn(parkingList);

	        List<Parking> result = parkingService.getAllParking();

	        assertNotNull(result);
	        assertEquals(2, result.size());
			//Assertequals Compare actual values with expected values
	        assertEquals("KA42KA4428",
	                result.get(0).getRegistrationNumber());
	        assertEquals("KA01AB1234",
	                result.get(1).getRegistrationNumber());

	        verify(parkingRepo, times(1)).findAll();

			//assertThrows	Confirms that the correct exception type is thrown, and its message is then checked
		
	}
	
//	to get the data by id  getParkingById()
	 @Test
	    void getParkingById_shouldReturnParking_whenIdExists() {

	        when(parkingRepo.findById(1L))
	                .thenReturn(Optional.of(parking));

	        Parking result = parkingService.getParkingById(1L);

	        assertNotNull(result);
	        assertEquals(1, result.getId());
	        assertEquals("KA42KA4428",
	                result.getRegistrationNumber());

	        verify(parkingRepo, times(1)).findById(1L);
	    }
	 @Test
	    void getParkingById_shouldThrowException_whenIdDoesNotExist() {

	        when(parkingRepo.findById(99L))
	                .thenReturn(Optional.empty());

	        NoSuchElementException exception =
	                assertThrows(
	                        NoSuchElementException.class,
	                        () -> parkingService.getParkingById(99L)
	                );

	        assertEquals(
	                "No parking record found with id 99",
	                exception.getMessage()
	        );

	        verify(parkingRepo, times(1)).findById(99L);
	    }
	
//	 to save saveParking() - ENTRY
	 @Test
	    void saveParking_shouldCreateEntry_whenNoOpenRecordExists() {

	        Parking newParking = new Parking();
	        newParking.setRegistrationNumber("KA42KA4428");

	        when(parkingRepo
	                .findByRegistrationNumberAndExitDateIsNullAndFinalCostIsNull(
	                        "KA42KA4428"))
	                .thenReturn(Optional.empty());

	        when(parkingRepo.save(any(Parking.class)))
	                .thenAnswer(invocation -> invocation.getArgument(0));

	        String result = parkingService.saveParking(newParking);

	        assertEquals(
	                "Entry recorded for KA42KA4428",
	                result
	        );

	        assertNotNull(newParking.getEntryDate());

	        assertNull(newParking.getExitDate());
	        assertNull(newParking.getFinalCost());
	        assertNull(newParking.getParkedTime());

	        verify(parkingRepo, times(1))
	                .findByRegistrationNumberAndExitDateIsNullAndFinalCostIsNull(
	                        "KA42KA4428");

	        verify(parkingRepo, times(1))
	                .save(newParking);
	    }
	 
	 // saveParking() - INVALID REGISTRATION


	    @Test
	    void saveParking_shouldThrowException_whenRegistrationNumberIsNull() {

	        Parking invalidParking = new Parking();

	        invalidParking.setRegistrationNumber(null);

	        IllegalArgumentException exception =
	                assertThrows(
	                        IllegalArgumentException.class,
	                        () -> parkingService.saveParking(invalidParking)
	                );

	        assertEquals(
	                "registrationNumber is required",
	                exception.getMessage()
	        );

	        verifyNoInteractions(parkingRepo);
	        verifyNoInteractions(priceRepo);
	    }

	    @Test
	    void saveParking_shouldThrowException_whenRegistrationNumberIsBlank() {

	        Parking invalidParking = new Parking();

	        invalidParking.setRegistrationNumber("   ");

	        IllegalArgumentException exception =
	                assertThrows(
	                        IllegalArgumentException.class,
	                        () -> parkingService.saveParking(invalidParking)
	                );

	        assertEquals(
	                "registrationNumber is required",
	                exception.getMessage()
	        );

	        verifyNoInteractions(parkingRepo);
	        verifyNoInteractions(priceRepo);
	    }
	    //saving to parking db while entering 
	    @Test
	    void saveParking_shouldCreateEntry() {

	        Parking parking = new Parking();
	        parking.setRegistrationNumber("KA42KA4428");

	        when(parkingRepo
	                .findByRegistrationNumberAndExitDateIsNullAndFinalCostIsNull(
	                        "KA42KA4428"))
	                .thenReturn(Optional.empty());

	        String result = parkingService.saveParking(parking);

	        assertEquals(
	                "Entry recorded for KA42KA4428",
	                result
	        );

	        assertNotNull(parking.getEntryDate());
	        assertNull(parking.getExitDate());
	        assertNull(parking.getFinalCost());

	        verify(parkingRepo).save(parking);
	    }
	    //invalid reg no 
	    @Test
	    void saveParking_shouldRejectEmptyRegistrationNumber() {

	        Parking parking = new Parking();
	        parking.setRegistrationNumber("");

	        IllegalArgumentException exception =
	                assertThrows(
	                        IllegalArgumentException.class,
	                        () -> parkingService.saveParking(parking)
	                );

	        assertEquals(
	                "registrationNumber is required",
	                exception.getMessage()
	        );

	        verifyNoInteractions(parkingRepo);
	    }

	    
	    //save while exit 
	    @Test
	    void saveParking_shouldProcessExit() {

	        Parking existingParking = new Parking();

	        existingParking.setId(1);
	        existingParking.setRegistrationNumber("KA42KA4428");

	        existingParking.setEntryDate(
	                LocalDateTime.now().minusMinutes(30)
	        );

	        Parking parking = new Parking();
	        parking.setRegistrationNumber("KA42KA4428");

	        PriceMaster price = new PriceMaster();
	        price.setDuration(60);
	        price.setUnitPrice(new BigDecimal("20"));

	        when(parkingRepo
	                .findByRegistrationNumberAndExitDateIsNullAndFinalCostIsNull(
	                        "KA42KA4428"))
	                .thenReturn(Optional.of(existingParking));

	        when(priceRepo.findAllByOrderByDurationAsc())
	                .thenReturn(List.of(price));

	        String result = parkingService.saveParking(parking);

	        assertTrue(result.contains("Exit recorded"));

	        assertNotNull(existingParking.getExitDate());
	        assertNotNull(existingParking.getParkedTime());
	        assertNotNull(existingParking.getFinalCost());

	        verify(parkingRepo).save(existingParking);
	    }
//error while NO PRICE CONFIGURATION
	    @Test
	    void saveParking_shouldFailWhenNoPriceTiersExist() {

	        Parking existingParking = new Parking();

	        existingParking.setRegistrationNumber("KA42KA4428");

	        existingParking.setEntryDate(
	                LocalDateTime.now().minusMinutes(30)
	        );

	        Parking parking = new Parking();
	        parking.setRegistrationNumber("KA42KA4428");

	        when(parkingRepo
	                .findByRegistrationNumberAndExitDateIsNullAndFinalCostIsNull(
	                        "KA42KA4428"))
	                .thenReturn(Optional.of(existingParking));

	        when(priceRepo.findAllByOrderByDurationAsc())
	                .thenReturn(List.of());

	        IllegalStateException exception =
	                assertThrows(
	                        IllegalStateException.class,
	                        () -> parkingService.saveParking(parking)
	                );

	        assertEquals(
	                "PRICE_MASTER has no configured tiers",
	                exception.getMessage()
	        );
	    }

	    //update parking record 
	    @Test
	    void updateParking_shouldUpdateParking() {

	        Parking existing = new Parking();

	        existing.setId(1);
	        existing.setRegistrationNumber("KA42KA4428");

	        Parking updated = new Parking();

	        updated.setRegistrationNumber("KA01AB1234");
	        updated.setFinalCost(100);

	        when(parkingRepo.findById(1L))
	                .thenReturn(Optional.of(existing));

	        when(parkingRepo.save(any(Parking.class)))
	                .thenReturn(existing);

	        Parking result =
	                parkingService.updateParking(1L, updated);

	        assertEquals(
	                "KA01AB1234",
	                result.getRegistrationNumber()
	        );

	        assertEquals(
	                100,
	                result.getFinalCost()
	        );

	        verify(parkingRepo).save(existing);
	    }


	    //delete the parking record

	    @Test
	    void deleteParking_shouldDeleteParking() {

	        when(parkingRepo.existsById(1L))
	                .thenReturn(true);

	        parkingService.deleteParking(1L);

	        verify(parkingRepo).deleteById(1L);
	    }
	    
	}
	    
