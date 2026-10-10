package com.chethan.parkingApp.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_master",schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//the no-args constructor is required by JPA to create instances of the entity class.
//  It allows JPA to instantiate the entity using reflection,
// which is necessary for various operations such as retrieving data from the database and populating entity objects.

public class PriceMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "display_duration", nullable = false)
    private String displayDuration;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;
}