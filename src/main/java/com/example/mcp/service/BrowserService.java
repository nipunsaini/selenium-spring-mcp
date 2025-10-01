package com.example.mcp.service;

import com.example.mcp.data.Browser;
import com.example.mcp.data.BrowserOptions;
import com.example.mcp.data.LocatorStrategy;
import com.example.mcp.data.TabAction;
import jakarta.annotation.PreDestroy;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BrowserService {

    private static final Logger logger = LoggerFactory.getLogger(BrowserService.class);

    private Map<String, WebDriver> browserSessions = new ConcurrentHashMap<>();
    private String currentSession;

    @Tool(name = "browser_open", description = "Open the browser by its name")
    public String openBrowser(
            @ToolParam(description = "Name of the browser e.g chrome, firefox") Browser browserName,
            @ToolParam(description = "Browser options e.g headless, arguments etc.") BrowserOptions options
            ) {
        try {
            WebDriver driver;
            switch (browserName) {
                case chrome -> {
                    ChromeOptions chromeOptions = new ChromeOptions();
                    if (options.headless())
                        chromeOptions.addArguments("--headless=new");
                    options.arguments().forEach(chromeOptions::addArguments);
                    driver = new ChromeDriver(chromeOptions);
                }
                case firefox -> {
                    FirefoxOptions firefoxOptions = new FirefoxOptions();
                    if (options.headless())
                        firefoxOptions.addArguments("--headless");
                    options.arguments().forEach(firefoxOptions::addArguments);
                    driver = new FirefoxDriver(firefoxOptions);
                }
                case safari -> {
                    SafariOptions safariOptions = new SafariOptions();
                    driver = new SafariDriver(safariOptions);
                }
                default -> throw new IllegalArgumentException("Browser not supported: " + browserName);
            }

            String session =  String.format("%s-%s", browserName, UUID.randomUUID().toString());
            browserSessions.put(session, driver);
            currentSession = session;
            return "Browser started successfully with session_id: " + session;
        } catch (Exception e) {
            String msg = "Error in starting browser: " + e.getMessage();
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_close", description = "Close the browser")
    public String closeBrowser() {
        try {
            getDriver().quit();
            return "Browser session closed successfully with id: " + currentSession;
        } catch (Exception e) {
            String msg = "Error in closing browser session: " + e.getMessage();
            logger.info(msg);
            return msg;
        } finally {
            if (currentSession != null)
                browserSessions.remove(currentSession);
            currentSession = null;
        }
    }

    @Tool(name = "browser_navigate", description = "Navigate to the url")
    public String browserNavigate(@ToolParam(description = "Url of web page to navigate to") String url) {
        try {
            if (!url.contains("://")) {
                url = "https://" + url;
            }
            getDriver().get(url);
            return "Successfully navigated to " + url;
        } catch (Exception e) {
            String msg = "Error while navigating to url " + url + " : " + e.getMessage();
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_click", description = "Click on a web element")
    public String browserClick(
            @ToolParam(description = "Method name to locate web element e.g cssSelector, xpath, id, className, name, tag, linkText, partialLinkText etc.") LocatorStrategy findBy,
            @ToolParam(description = "Locator value of web element") String value,
            @ToolParam(description = "Timeout value in seconds to wait for web element", required = false) Long timeout
    ) {
        try {
            timeout = setTimeout(timeout);
            WebElement element = findElement(findBy, value, timeout);
            WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeout));
            wait.until(ExpectedConditions.elementToBeClickable(element));
            element.click();
            return "Element clicked successfully.";
        } catch (Exception e) {
            String msg = String.format("Error in clicking Web Element [%s, %s]: %s", findBy, value, e.getMessage());
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_send_keys", description = "Send or Type keys to a web element")
    public String browserSendKeys(
            @ToolParam(description = "Method name to locate web element e.g cssSelector, xpath, id, className, name, tag, linkText, partialLinkText etc.") LocatorStrategy findBy,
            @ToolParam(description = "Locator value of web element") String locatorValue,
            @ToolParam(description = "Text to type to a web element") String text,
            @ToolParam(description = "Timeout value in seconds to wait for web element", required = false) Long timeout
    ) {
        try {
            timeout = setTimeout(timeout);
            WebElement element = findElement(findBy, locatorValue, timeout);
            element.clear();
            element.sendKeys(text);
            return "Entered text into web element: " + text;
        } catch (Exception e) {
            String msg = String.format("Error in entering text to Web Element [%s, %s]: %s", findBy, locatorValue, e.getMessage());
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_take_screenshot", description = "Take screenshot of a browser tab")
    public String takeScreenshot(
            @ToolParam(description = "Output file path for the screenshot [optional]", required = false) String outputPath
    ) {
        try {
            TakesScreenshot driver = (TakesScreenshot) getDriver();
            File screenshot = driver.getScreenshotAs(OutputType.FILE);
            if (outputPath == null || outputPath.trim().isEmpty()) {
                outputPath = Paths.get(
                        System.getProperty("user.home"),
                        ".mcp",
                        "screenshots",
                        System.currentTimeMillis() + ".png"
                ).toString();
            }
            File destination = new File(outputPath);
            File parentDir = destination.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            FileCopyUtils.copy(screenshot, destination);
            return "Screenshot captured successfully and saved to : " + outputPath;
        } catch (Exception e) {
            String msg = String.format("Error in capturing screenshot: %s", e.getMessage());
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_get_text", description = "Get text of a web element")
    public String getText(
            @ToolParam(description = "Method name to locate web element e.g cssSelector, xpath, id, className, name, tag, linkText, partialLinkText etc.") LocatorStrategy findBy,
            @ToolParam(description = "Locator value of web element") String locatorValue,
            @ToolParam(description = "Timeout value in seconds to wait for web element", required = false) Long timeout
    ) {
        try {
            timeout = setTimeout(timeout);
            WebElement element = findElement(findBy, locatorValue, timeout);
            return element.getText();
        } catch (Exception e) {
            String msg = String.format("Error in getting text of Web Element [%s, %s]: %s", findBy, locatorValue, e.getMessage());
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_hover", description = "Move the mouse to hover on a web element")
    public String hoverOnElement(
            @ToolParam(description = "Method name to locate web element e.g cssSelector, xpath, id, className, name, tag, linkText, partialLinkText etc.") LocatorStrategy findBy,
            @ToolParam(description = "Locator value of web element") String locatorValue,
            @ToolParam(description = "Timeout value in seconds to wait for web element", required = false) Long timeout
    ) {
        try {
            timeout = setTimeout(timeout);
            WebElement element = findElement(findBy, locatorValue, timeout);
            Actions actions = new Actions(getDriver());
            actions.moveToElement(element).perform();
            return "Moved mouse on the web element";
        } catch (Exception e) {
            String msg = String.format("Error in hovering on a Web Element [%s, %s]: %s", findBy, locatorValue, e.getMessage());
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_drag_and_drop", description = "Drag an element and drop it to another web element")
    public String dragAndDrop(
            @ToolParam(description = "Method name to locate source web element to drag e.g cssSelector, xpath, id, className, name, tag, linkText, partialLinkText etc.") LocatorStrategy sourceFindBy,
            @ToolParam(description = "Locator value of web element") String sourceLocatorValue,
            @ToolParam(description = "Method name to locate target web element e.g cssSelector, xpath, id, className, name, tag, linkText, partialLinkText etc.") LocatorStrategy targetFindBy,
            @ToolParam(description = "Locator value of web element") String targetLocatorValue,
            @ToolParam(description = "Timeout value in seconds to wait for web element", required = false) Long timeout
    ) {
        try {
            timeout = setTimeout(timeout);
            WebElement source = findElement(sourceFindBy, sourceLocatorValue, timeout);
            WebElement target = findElement(targetFindBy, targetLocatorValue, timeout);
            Actions actions = new Actions(getDriver());
            actions.dragAndDrop(source, target).perform();
            return "Source element dragged to target element successfully";
        } catch (Exception e) {
            String msg = "Error in performing drag and drop: " + e.getMessage();
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_double_click", description = "Perform double click on a web element")
    public String doubleClick(
            @ToolParam(description = "Method name to locate web element e.g cssSelector, xpath, id, className, name, tag, linkText, partialLinkText etc.") LocatorStrategy findBy,
            @ToolParam(description = "Locator value of web element") String locatorValue,
            @ToolParam(description = "Timeout value in seconds to wait for web element", required = false) Long timeout
    ) {
        try {
            timeout = setTimeout(timeout);
            WebElement element = findElement(findBy, locatorValue, timeout);
            Actions actions = new Actions(getDriver());
            actions.doubleClick(element).perform();
            return "Double click performed successfully on the web element";
        } catch (Exception e) {
            String msg = String.format("Error in performing double click on the Web Element [%s, %s]: %s", findBy, locatorValue, e.getMessage());
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_right_click", description = "Perform right/context click on a web element")
    public String rightClick(
            @ToolParam(description = "Method name to locate web element e.g cssSelector, xpath, id, className, name, tag, linkText, partialLinkText etc.") LocatorStrategy findBy,
            @ToolParam(description = "Locator value of web element") String locatorValue,
            @ToolParam(description = "Timeout value in seconds to wait for web element", required = false) Long timeout
    ) {
        try {
            timeout = setTimeout(timeout);
            WebElement element = findElement(findBy, locatorValue, timeout);
            Actions actions = new Actions(getDriver());
            actions.contextClick(element).perform();
            return "Right click performed successfully on the web element";
        } catch (Exception e) {
            String msg = String.format("Error in performing right click on the Web Element [%s, %s]: %s", findBy, locatorValue, e.getMessage());
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_press_key", description = "Press a keyboard key to a web element")
    public String pressKey(
            @ToolParam(description = "Method name to locate web element e.g cssSelector, xpath, id, className, name, tag, linkText, partialLinkText etc.") LocatorStrategy findBy,
            @ToolParam(description = "Locator value of web element") String locatorValue,
            @ToolParam(description = "Keyboard key value e.g ENTER, CANCEL, CONTROL, ALT, etc. ") Keys key,
            @ToolParam(description = "Timeout value in seconds to wait for web element", required = false) Long timeout
    ) {
        try {
            timeout = setTimeout(timeout);
            WebElement element = findElement(findBy, locatorValue, timeout);
            element.sendKeys(key);
            return String.format("Keyboard key '%s' pressed successfully to the web element", key);
        } catch (Exception e) {
            String msg = String.format("Error in pressing key to the Web Element [%s, %s]: %s", findBy, locatorValue, e.getMessage());
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_upload_file", description = "Upload a file to a file input web element")
    public String uploadFile(
            @ToolParam(description = "Method name to locate web element e.g cssSelector, xpath, id, className, name, tag, linkText, partialLinkText etc.") LocatorStrategy findBy,
            @ToolParam(description = "Locator value of web element") String locatorValue,
            @ToolParam(description = "Path of the file to upload") String filePath,
            @ToolParam(description = "Timeout value in seconds to wait for web element", required = false) Long timeout
    ) {
        try {
            timeout = setTimeout(timeout);
            WebElement element = findElement(findBy, locatorValue, timeout);
            element.sendKeys(filePath);
            return "File has been uploaded to the input web element";
        } catch (Exception e) {
            String msg = String.format("Error in uploading file to the Web Element [%s, %s]: %s", findBy, locatorValue, e.getMessage());
            logger.info(msg);
            return msg;
        }
    }


    @Tool(name = "browser_page_source", description = "Fetch the web page source code from current browser tab")
    public String pageSource() {
        try {
            return getDriver().getPageSource();
        } catch (Exception e) {
            String msg = String.format("Error in fetching page source code: %s", e.getMessage());
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_execute_javascript", description = "Execute the javascript command in browser context")
    public String executeScript(
            @ToolParam(description = "Javascript command to execute") String script,
            @ToolParam(description = "Method name to locate web element e.g cssSelector, xpath, id, className, name, tag, linkText, partialLinkText etc.", required = false) LocatorStrategy findBy,
            @ToolParam(description = "Locator value of web element", required = false) String locatorValue
    ) {
        try {

            JavascriptExecutor executor = (JavascriptExecutor) getDriver();
            if (script.contains("arguments[")) {
                if (findBy == null || locatorValue == null || locatorValue.isEmpty()) {
                    return "locator strategy and locator value must be provided for parameterised javascript execution";
                }
                WebElement element = findElement(findBy, locatorValue, 5);
                executor.executeScript(script, element);
            } else {
                executor.executeScript(script);
            }
            return "Javascript executed successfully";
        } catch (Exception e) {
            String msg = String.format("Error in executing javascript %s", e.getMessage());
            logger.info(msg);
            return msg;
        }
    }

    @Tool(name = "browser_tabs", description = "Manage browser tabs")
    public String executeScript(
            @ToolParam(description = "Tab action to perform e.g CLOSE, SELECT, NEW") TabAction action,
            @ToolParam(description = "Index of the tab, used for close/select. If not provided for close, current tab will be closed", required = false) Integer index
    ) {
        try {

            switch (action) {
                case NEW -> {
                    getDriver().switchTo().newWindow(WindowType.TAB);
                    return "Control switched to the new window";
                }
                case CLOSE -> {
                    if (index == null)
                        getDriver().close();
                    else {
                        Set<String> tabs = getDriver().getWindowHandles();
                        String currentTab = getDriver().getWindowHandle();
                        if (index >= tabs.size())
                            throw new IllegalArgumentException("Tab index should be less than total tabs opened");
                        int counter = 0;
                        for (String tab : tabs) {
                            if (counter == index) {
                                String tabToClose = tab;
                                if (!tabToClose.equals(currentTab)) {
                                    getDriver().switchTo().window(tabToClose);
                                    getDriver().close();
                                    getDriver().switchTo().window(currentTab);
                                } else {
                                    getDriver().close();
                                    Set<String> newTabs = getDriver().getWindowHandles();
                                    for (String t : newTabs) {
                                        getDriver().switchTo().window(t);
                                        break;
                                    }
                                }
                                break;
                            }
                            counter++;
                        }
                    }
                }
                case SELECT -> {
                    if (index == null)
                        throw new IllegalArgumentException("Tab index required to select a tab");
                    Set<String> tabs = getDriver().getWindowHandles();
                    if (index >= tabs.size())
                        throw new IllegalArgumentException("Tab index should be less than total tabs opened");
                    int counter = 0;
                    for (String tab : tabs) {
                        if (counter == index) {
                            getDriver().switchTo().window(tab);
                            return "Tab selected successfully.";
                        }
                        counter++;
                    }
                }
            }
            return "Tab action performed successfully.";
        } catch (Exception e) {
            String msg = String.format("Error in performing tab action %s", e.getMessage());
            logger.info(msg);
            return msg;
        }
    }

    private WebDriver getDriver() {
        if (currentSession == null || !browserSessions.containsKey(currentSession)) {
            throw new IllegalStateException("No active browser sessions exist");
        }
        return browserSessions.get(currentSession);
    }

    private By getLocator(LocatorStrategy by, String value) {
        switch (by) {
            case id: return By.id(value);
            case className: return By.className(value);
            case cssSelector: return By.cssSelector(value);
            case name: return By.name(value);
            case tag: return By.tagName(value);
            case linkText: return By.linkText(value);
            case partialLinkText: return By.partialLinkText(value);
            case xpath: return By.xpath(value);
            default: throw new IllegalArgumentException("No such locator strategy exist for locating web element: " + by);
        }
    }

    private WebElement findElement(LocatorStrategy by, String value, long timeoutInSeconds) {
        By locator = getLocator(by, value);
        WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeoutInSeconds));
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        return getDriver().findElement(locator);
    }

    private Long setTimeout(Long timeout) {
        return timeout == null || timeout <= 0 ? 20L : timeout;
    }

    @PreDestroy
    public void onShutdown() {
        logger.info("🔴 shutting down browser service...");
        browserSessions.keySet().forEach(session -> {
            try {
                browserSessions.get(session).quit();
            } catch (Exception e) {
                logger.info(e.getMessage());
            }
        });
    }
}
