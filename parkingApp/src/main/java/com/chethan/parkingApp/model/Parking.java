package com.chethan.parkingApp.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parking_info", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
//the no-args constructor is required by JPA to create instances of the entity class.
//  It allows JPA to instantiate the entity using reflection, 
// which is necessary for various operations such as retrieving data from the database and populating entity objects.
public class Parking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "entry_date")
    private LocalDateTime entryDate;

    @Column(name = "parked_time")
    private String parkedTime;

    @Column(name = "exit_date")
    private LocalDateTime exitDate;

    @Column(name = "final_cost")
    private Integer finalCost;
}