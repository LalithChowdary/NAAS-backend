package com.naas.backend.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Base class for all Selenium E2E tests.
 *
 * - Sets up and tears down a Chrome browser for every test.
 * - All E2E tests must extend this class.
 * - Chrome is launched in headless mode by default (so it runs without a
 * visible
 * window). To watch tests run visually, comment out the "--headless=new" line.
 *
 * Prerequisites before running these tests:
 * 1. Start the Spring Boot backend: ./mvnw spring-boot:run
 * 2. Start the Next.js frontend: npm run dev (in /frontend dir)
 * 3. Run ONLY E2E tests: ./mvnw test -Dgroups=e2e
 */
public abstract class BaseSeleniumTest {

    protected static final String BASE_URL = "http://localhost:3000";

    // Credentials for a real customer account seeded in your database.
    // Replace these with credentials that exist in your local/test DB.
    protected static final String CUSTOMER_EMAIL = "customer1@naas.com";
    protected static final String CUSTOMER_PASSWORD = "password";

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeEach
    void setUpBrowser() {
        // WebDriverManager automatically downloads the correct chromedriver binary.
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // Headless mode is DISABLED — Next.js Server Action redirects are
        // unreliable in headless Chrome (POST → redirect not followed correctly).
        // options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        // Page load timeout — Next.js SSR pages can take a moment to hydrate.
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        // Explicit wait — 30s to account for Next.js SSR + server action round‑trips.
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    @AfterEach
    void tearDownBrowser() {
        if (driver != null) {
            driver.quit(); // Always release the browser, even if test fails.
        }
    }
}
