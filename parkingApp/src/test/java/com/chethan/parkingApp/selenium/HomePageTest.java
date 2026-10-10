package com.chethan.parkingApp.selenium;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HomePageTest extends BaseSeleniumTest {

//The HomePageTest class verifies that the Home page opens correctly.
//1. Tests Home Page Opens Successfully
    @Test
    void homePageShouldOpen() {

        driver.get("http://localhost:4200/home");

        System.out.println("Current URL: " + driver.getCurrentUrl());

        assertTrue(
                driver.getCurrentUrl().contains("/home")
        );
    }
}