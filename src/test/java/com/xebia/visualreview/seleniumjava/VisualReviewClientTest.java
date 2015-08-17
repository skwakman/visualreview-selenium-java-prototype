package com.xebia.visualreview.seleniumjava;

import org.junit.Test;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.net.URL;

public class VisualReviewClientTest {

  @Test
  public void quickTest() throws IOException {
    // this test requires a running selenium server instance on port 4444
    RemoteWebDriver driver = new RemoteWebDriver(new URL("http", "localhost", 4444, "/wd/hub"), DesiredCapabilities.firefox());
    driver.get("http://www.google.nl");
    int runId = VisualReviewClient.createRun("my project", "my suite");

    Dimension resolution = driver.manage().window().getSize();
    try {
      VisualReviewClient.takeAndSendScreenshot(runId, driver, "google search",
          driver.getCapabilities().getPlatform().toString(),
          driver.getCapabilities().getBrowserName(),
          driver.getCapabilities().getVersion(),
          resolution.getWidth() + "x" + resolution.getHeight());

    } finally {
      driver.close();
    }
  }

}


