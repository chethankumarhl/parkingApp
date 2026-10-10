package com.chethan.parkingApp.selenium;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

public class CalenderPageTest extends BaseSeleniumTest {

    //The CalenderPageTest class verifies that the Calendar page loads correctly and contains the expected page content.
    //1. – Calendar Page Opens
    @Test
    void calendarPageShouldOpen() {

        driver.get("http://localhost:4200/calender");

        assertTrue(
                driver.getCurrentUrl().contains("/calender")
        );
    }
    //2. – Calendar Page Displays Content
    @Test
    void calendarShouldDisplayContent() {

        driver.get("http://localhost:4200/calender");

        String pageText = driver.findElement(
                By.tagName("body")
        ).getText();

        assertTrue(pageText.contains("カレンダー"));
    }
}