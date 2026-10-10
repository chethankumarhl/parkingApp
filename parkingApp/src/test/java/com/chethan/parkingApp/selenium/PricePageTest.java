package com.chethan.parkingApp.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PricePageTest extends BaseSeleniumTest {

    //The PricePageTest class verifies that the Price page loads correctly and contains the expected page content.
    //Price Page Opens

    //1. – Price Page Opens
    @Test
    void pricePageShouldOpen() {

        driver.get("http://localhost:4200/price");

        assertTrue(
                driver.getCurrentUrl().contains("/price")
        );
    }

    //2. – Price Page Displays Content  
    @Test
    void pricePageShouldDisplayContent() {

        driver.get("http://localhost:4200/price");

        String pageText = driver.findElement(
                By.tagName("body")
        ).getText();

        assertTrue(pageText.contains("Price"));
    }
}