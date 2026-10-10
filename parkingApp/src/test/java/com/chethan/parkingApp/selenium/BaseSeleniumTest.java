package com.chethan.parkingApp.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class BaseSeleniumTest {

    //The Selenium tests use ChromeDriver to start and control a Google Chrome browser.
    //The BaseSeleniumTest class provides setup and teardown methods for Selenium tests.
    protected WebDriver driver;

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}