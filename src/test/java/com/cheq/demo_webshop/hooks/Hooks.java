package com.cheq.demo_webshop.hooks;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;

import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import com.cheq.demo_webshop.factory.WebDriverFactory;
import com.cheq.demo_webshop.manager.DriverManager;
import com.cheq.demo_webshop.utils.AllureUtil;
import com.cheq.demo_webshop.utils.ConfigReader;
import com.cheq.demo_webshop.utils.LoggerUtil;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;

/**
* Cucumber hooks for managing WebDriver lifecycle and Allure reporting.
* Handles setup, teardown, screenshot capture, and driver access per scenario.
*/
public class Hooks {

    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private ThreadLocal<AllureUtil> allureUtil = new ThreadLocal<>();
    private static final Logger logger = LoggerUtil.getLogger(Hooks.class);

    /**
    * Initializes WebDriver, loads environment configuration,
    * sets up Allure reporting, and starts the test scenario.
    *
    * @param scenario the current Cucumber scenario
    * @throws IOException if configuration properties fail to load
    */
    @Before
    public void setUp(Scenario scenario) throws IOException {
        String env = System.getProperty("env", "dev");
        ConfigReader.loadProperties(env);

        String browser = System.getProperty("browser", ConfigReader.get("browser"));
        String url = ConfigReader.get("baseUrl");

        WebDriver drv = WebDriverFactory.loadDriver(browser);
        drv.manage().window().maximize();
        drv.get(url);

        driver.set(drv); // thread-local driver
        DriverManager.setDriver(drv);

        allureUtil.set(new AllureUtil(drv));
        allureUtil.get().writeAllureEnvironment(
            ImmutableMap.<String, String>builder()
                .put("OS", System.getProperty("os.name"))
                .put("Browser", browser)
                .put("Environment", env)
                .build()
        );

        logger.info("Starting scenario: " + scenario.getName());
    }

    /**
    * Quits the WebDriver instance and cleans up thread-local resources
    * after scenario execution.
    *
    * @param scenario the executed Cucumber scenario
    */
    @After(order = 0)
    public void tearDown(Scenario scenario) {
        WebDriver drv = driver.get();
        if (drv != null) {
            drv.quit();
            driver.remove();
            allureUtil.remove();
        }
    }

    /**
    * Captures and attaches a screenshot to the Allure report
    * if the scenario execution fails.
    *
    * @param scenario the executed Cucumber scenario
    */
    @After(order = 1)
    public void captureFailure(Scenario scenario) {
        WebDriver drv = driver.get();
        if (scenario.isFailed() && drv != null) {
            allureUtil.get().captureAndAttachScreenshot();
        }
    }

    /**
    * Captures and attaches a screenshot to the Allure report
    * after each executed step.
    *
    * @param scenario the current Cucumber scenario
    */
    @AfterStep
    public void afterEachStep(Scenario scenario) {
        WebDriver drv = driver.get();
        if (drv != null) {
            allureUtil.get().captureAndAttachScreenshot();
        }
    }

    /**
    * Returns the current thread-local WebDriver instance.
    *
    * @return the active WebDriver
    */
    public static WebDriver getDriver() {
        return driver.get();
    }
}
