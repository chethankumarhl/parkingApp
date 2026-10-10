package com.chethan.parkingApp.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParkingPageTest extends BaseSeleniumTest {

        //The ParkingPageTest class verifies that the Parking page opens and that the main parking table is displayed correctly.
        //1. – Parking Page Opens
        //2. Parking Table Is Displayed
        //3. Parking Table Headings Are Displayed
        



         //1. – Parking Page Opens
    @Test
    void parkingPageShouldOpen() {

        driver.get("http://localhost:4200/parking");

        assertTrue(driver.getCurrentUrl().contains("/parking"));
    }


    //2. Parking Table Is Displayed
    @Test
    void parkingTableShouldBeDisplayed() {

        driver.get("http://localhost:4200/parking");

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.tagName("table")
                )
        );

        assertTrue(
                driver.findElement(By.tagName("table")).isDisplayed()
        );
    }

    //3. Parking Table Headings Are Displayed
    @Test
    void parkingTableShouldContainExpectedColumns() {

        driver.get("http://localhost:4200/parking");

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.tagName("table")
                )
        );

        String pageText = driver
                .findElement(By.tagName("body"))
                .getText();

        assertTrue(pageText.contains("自動車登録番号"));
        assertTrue(pageText.contains("入庫日時"));
        assertTrue(pageText.contains("出庫日時"));
        assertTrue(pageText.contains("駐車時間"));
        assertTrue(pageText.contains("最終料金"));
    }
}