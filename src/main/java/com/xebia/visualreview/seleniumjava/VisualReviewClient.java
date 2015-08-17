package com.xebia.visualreview.seleniumjava;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.IOException;
import java.io.StringWriter;

public class VisualReviewClient {

  private static final String HOSTNAME = "localhost";
  private static final String PORT = "7000";

  /**
   * Creates a new run on the VisualReview server with the given project name and suite name.
   *
   * @return the new run's RunID, which can be used to upload screenshots to
   * @throws IOException
   */
  public static int createRun(String projectName, String suiteName) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();
    HttpPost httpPost = new HttpPost("http://" + HOSTNAME + ":" + PORT + "/api/runs");
    StringEntity input = new StringEntity("{\"projectName\":\"" + projectName + "\",\"suiteName\":\"" + suiteName + "\"}");
    input.setContentType("application/json");

    httpPost.setEntity(input);
    CloseableHttpResponse response = httpclient.execute(httpPost);

    try {
      System.out.println("response from server when creating run: " + response.getStatusLine());
      HttpEntity responseEntity = response.getEntity();

      JsonFactory factory = new JsonFactory();
      JsonParser parser = factory.createParser(response.getEntity().getContent());
      if (parser.nextToken() != JsonToken.START_OBJECT) {
        throw new IOException("Expected data to start with an Object");
      }

      while (parser.nextToken() != JsonToken.END_OBJECT) {
        String fieldName = parser.getCurrentName();
        parser.nextToken(); // moves to value
        if (fieldName != null && fieldName.equals("id")) {
          return Integer.parseInt(parser.getValueAsString());
        }
      }
      EntityUtils.consume(responseEntity);
    } finally {
      response.close();
    }

    throw new RuntimeException("something went wrong while creating suite..");
  }

  public static void takeAndSendScreenshot(int runId, TakesScreenshot webDriver, String screenshotName, String platformName, String browserName, String browserVersion, String resolution) throws IOException {
    byte[] screenshotData = getScreenshotFromBrowser(webDriver);

    JsonFactory factory = new JsonFactory();
    StringWriter jsonString = new StringWriter();
    JsonGenerator generator = factory.createGenerator(jsonString);
    generator.writeStartObject();
    generator.writeStringField("browser", browserName);
    generator.writeStringField("platform", platformName);
    generator.writeStringField("resolution", resolution);
    generator.writeStringField("version", browserVersion);
    generator.writeEndObject();
    generator.flush();

    CloseableHttpClient httpclient = HttpClients.createDefault();
    HttpPost httpPost = new HttpPost("http://" + HOSTNAME + ":" + PORT + "/api/runs/" + runId + "/screenshots");

    HttpEntity input = MultipartEntityBuilder.create()
        .addBinaryBody("file", screenshotData, ContentType.parse("image/png"), "file.png")
        .addTextBody("screenshotName", screenshotName, ContentType.TEXT_PLAIN)
        .addTextBody("properties", jsonString.toString(), ContentType.APPLICATION_JSON)
        .addTextBody("meta", "{}", ContentType.APPLICATION_JSON)
        .build();

    httpPost.setEntity(input);
    CloseableHttpResponse response = httpclient.execute(httpPost);

    try {
      System.out.println("response from server when uploading screenshot: " + response.getStatusLine());
    } finally {
      response.close();
    }
  }

  private static byte[] getScreenshotFromBrowser(TakesScreenshot webDriver) throws IOException {
    return webDriver.getScreenshotAs(OutputType.BYTES);
  }
}
