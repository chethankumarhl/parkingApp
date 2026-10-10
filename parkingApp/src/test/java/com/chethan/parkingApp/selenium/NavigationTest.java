package com.chethan.parkingApp.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NavigationTest extends BaseSeleniumTest {

        //The NavigationTest class verifies navigation from the application interface to the main application pages.
        //1. – Navigate to Parking Page
        //2. – Navigate to Price Page
        //3. – Navigate to Calendar Page

    private WebDriverWait waitForPage() {
        return new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );
    }

    //1. – Navigate to Parking Page
    @Test
    void shouldNavigateToParkingPage() {

        driver.get("http://localhost:4200/parking");

        waitForPage().until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("a[routerLink='/parking']")
                )
        );

        driver.findElement(
                By.cssSelector("a[routerLink='/parking']")
        ).click();

        waitForPage().until(
                ExpectedConditions.urlContains("/parking")
        );

        assertTrue(driver.getCurrentUrl().contains("/parking"));
    }


    //2. – Navigate to Price Page
    @Test
    void shouldNavigateToPricePage() {

        driver.get("http://localhost:4200/parking");

        waitForPage().until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("a[routerLink='/price']")
                )
        );

        driver.findElement(
                By.cssSelector("a[routerLink='/price']")
        ).click();

        waitForPage().until(
                ExpectedConditions.urlContains("/price")
        );

        assertTrue(driver.getCurrentUrl().contains("/price"));
    }

    //3. – Navigate to Calendar Page
    @Test
    void shouldNavigateToCalendarPage() {

        driver.get("http://localhost:4200/parking");

        waitForPage().until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("a[routerLink='/calender']")
                )
        );

        driver.findElement(
                By.cssSelector("a[routerLink='/calender']")
        ).click();

        waitForPage().until(
                ExpectedConditions.urlContains("/calender")
        );

        assertTrue(driver.getCurrentUrl().contains("/calender"));
    }
}