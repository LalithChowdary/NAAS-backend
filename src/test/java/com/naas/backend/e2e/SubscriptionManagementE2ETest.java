package com.naas.backend.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E Tests for Subscription Domain Actions — Suite 2
 *
 * SRS Coverage:
 * FR-SM4: Allow pausing subscriptions for a defined period.
 * FR-SM6: Enforce the 7-day advance notice rule for all changes.
 * FR-SM7: Allow cancellation of subscriptions with advance notice.
 *
 * Run command:
 * ./mvnw test -Dgroups=e2e
 *
 * Prerequisites: NAAS backend, Next.js frontend, and at least one ACTIVE
 * customer subscription must exist in the database before running.
 */
@Tag("e2e")
class SubscriptionManagementE2ETest extends BaseSeleniumTest {

        // ── Shared date helpers ───────────────────────────────────────────────────

        /** Returns a date string (yyyy-MM-dd) N days from today. */
        private String daysFromNow(int days) {
                return LocalDate.now().plusDays(days)
                                .format(DateTimeFormatter.ISO_DATE);
        }

        /**
         * Sets a date input's value using JavaScript (bypasses browser date-picker UI).
         */
        private void setDateInput(String testId, String value) {
                WebElement input = driver.findElement(By.cssSelector("[data-testid='" + testId + "']"));
                ((JavascriptExecutor) driver).executeScript(
                                "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, \"value\").set; nativeInputValueSetter.call(arguments[0], arguments[1]); arguments[0].dispatchEvent(new Event(\"input\", {bubbles:true}));",
                                input, value);
        }

        // ── Shared setup: Login before each test ─────────────────────────────────

        @BeforeEach
        void loginFirst() {
                driver.get(BASE_URL + "/login");
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector("[data-testid='login-email']")));
                driver.findElement(By.cssSelector("[data-testid='login-email']"))
                                .sendKeys(CUSTOMER_EMAIL);
                driver.findElement(By.cssSelector("[data-testid='login-password']"))
                                .sendKeys(CUSTOMER_PASSWORD);
                driver.findElement(By.cssSelector("[data-testid='login-submit']")).click();

                // Wait until dashboard is loaded before each test body runs
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector("[data-testid='customer-dashboard']")));
                try {
                        Thread.sleep(2000);
                } catch (Exception e) {
                }
        }

        // ── Test 6 omitted per user request ─────────────────────────

        // ── Test 7 omitted per user request ─────────────────────

        // ── Test 8: Multiple Subscriptions — Correct one is updated ──────────────

        @Test
        @DisplayName("E2E-SM-8: When multiple subscriptions exist, only the clicked one should change")
        void multipleSubscriptions_shouldOnlyUpdateTargetedCard() {
                // Arrange — find all active subscription cards
                List<WebElement> subCards = driver.findElements(
                                By.cssSelector("[data-testid^='sub-card-']"));

                // Only run this test if there are 2 or more subscriptions
                if (subCards.size() < 2) {
                        System.out.println("[SKIP] Test requires >= 2 active subscriptions. Found: " + subCards.size());
                        return;
                }

                // Get IDs of each card
                String card1Id = subCards.get(0).getAttribute("data-testid");
                String card2Id = subCards.get(1).getAttribute("data-testid");

                // Act — open the Pause modal on the SECOND card specifically
                WebElement pauseBtnOnCard2 = subCards.get(1).findElement(
                                By.cssSelector("[data-testid='pause-sub-btn']"));
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();",
                                pauseBtnOnCard2);

                wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector("[data-testid='pause-start-date']")));
                setDateInput("pause-start-date", daysFromNow(10));
                setDateInput("pause-end-date", daysFromNow(20));

                driver.findElement(By.cssSelector("[data-testid='confirm-action-btn']")).click();

                // Wait for modal to close and page to re-render
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                                By.cssSelector("[data-testid='confirm-action-btn']")));

                // Assert — SECOND card shows SUSPENDED
                WebElement updatedCard2 = wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector("[data-testid='" + card2Id + "']")));
                assertTrue(updatedCard2.getText().contains("SUSPENDED"),
                                "Expected the second subscription card to be SUSPENDED");

                // Assert — FIRST card still shows ACTIVE (was not affected)
                WebElement card1 = driver.findElement(By.cssSelector("[data-testid='" + card1Id + "']"));
                assertTrue(card1.getText().contains("ACTIVE"),
                                "Expected the first subscription card to remain ACTIVE");
        }

        // ── Test 9 omitted per user request ─────────────────────────────────

        // ── Test 10: Create New Subscription (browse catalog) ────────────────────

        @Test
        @DisplayName("E2E-SM-10: Navigating to catalog from dashboard and back should not crash the app")
        void navigateToCatalogAndBack_shouldMaintainAppState() {
                // Arrange — confirm we are on the dashboard
                assertTrue(driver.getCurrentUrl().contains("/customer"),
                                "Expected to start on /customer");

                // Act — Navigate to catalog (the Browse Publications link)
                driver.get(BASE_URL + "/");

                // Assert — catalog page loaded (root page)
                wait.until(ExpectedConditions.urlToBe(BASE_URL + "/"));
                assertEquals(BASE_URL + "/", driver.getCurrentUrl(),
                                "Expected to land on catalog root page");

                // Act — navigate back to customer dashboard
                driver.navigate().back();

                // Assert — dashboard is visible again
                wait.until(ExpectedConditions.urlContains("/customer"));
                assertTrue(driver.getCurrentUrl().contains("/customer"),
                                "Expected to return to /customer after navigating back");
        }
}
