/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.benchmark.common.actors;

import akka.actor.UntypedActor;
import com.google.common.math.Quantiles;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import j2html.tags.ContainerTag;
import org.apache.pinot.benchmark.common.messages.DisplayStatisticsMessage;
import org.apache.pinot.benchmark.common.messages.EndStatisticsMessage;
import org.apache.pinot.benchmark.common.messages.InitializeStatisticsMessage;
import org.apache.pinot.benchmark.common.messages.RequestAndResponseDetailsMessage;
import org.apache.pinot.benchmark.common.messages.RequestStatistics;
import org.apache.pinot.benchmark.common.messages.UpdateStatisticsMessage;
import org.apache.pinot.benchmark.common.utils.ReservoirSampler;
import org.joda.time.LocalDateTime;

import static j2html.TagCreator.*;


public class StatisticsActor extends UntypedActor {
  private AtomicInteger errorCount;
  private AtomicInteger successCount;
  private AtomicInteger requestsSentCount;
  private AtomicInteger totalRequestsEnqueued;
  private AtomicInteger requestsInFlight;
  private Map<String, Integer> exceptionCount = new HashMap<>();
  private Map<Integer, Integer> statusCounts = new ConcurrentHashMap<>();
  private Map<Integer, ReservoirSampler> updateStatusLatencySampler = new HashMap<>();
  private Map<Integer, ReservoirSampler> overallStatusLatencySampler = new HashMap<>();
  private Map<Integer, String> requestContents = new HashMap<>();
  private Map<Integer, String> responseHeaders = new HashMap<>();
  private Map<Integer, String> responseContents = new HashMap<>();
  private final String reportName; // Name for this run so that report file is not overwritten.

  private static StatisticsActor instance = null;
  public static final List<Integer> PERCENTILES = Arrays.asList(50, 90, 95, 99);

  public StatisticsActor(String reportName) {
    if (instance != null) {
      throw new RuntimeException("More than one statistics actor initialized!");
    }

    instance = this;
    this.reportName = reportName;
  }

  @Override
  public void onReceive(Object message)
      throws Throwable {
    if (message instanceof DisplayStatisticsMessage) {
      DisplayStatisticsMessage displayStatisticsMessage = (DisplayStatisticsMessage) message;
      float secondsSinceTestStart =
          (System.currentTimeMillis() - displayStatisticsMessage.getTestStartTime()) / 1000.0f;

      int currentSuccessCount = successCount.get();
      System.out.println("successCount = " + currentSuccessCount);
      System.out.println("successes/s = " + currentSuccessCount / secondsSinceTestStart);

      int currentErrorCount = errorCount.get();
      System.out.println("errorCount = " + currentErrorCount);
      System.out.println("errors/s = " + currentErrorCount / secondsSinceTestStart);

      int currentRequestsSentCount = requestsSentCount.get();
      System.out.println("requestsSentCount = " + currentRequestsSentCount);
      System.out.println("requests sent/s = " + currentRequestsSentCount / secondsSinceTestStart);

      int currentTotalRequestsEnqueued = totalRequestsEnqueued.get();
      System.out.println("totalRequestsEnqueued = " + currentTotalRequestsEnqueued);
      System.out.println("requests enqueues/s = " + currentTotalRequestsEnqueued / secondsSinceTestStart);

      int currentRequestsInFlight = requestsInFlight.get();
      System.out.println("requestsInFlight = " + currentRequestsInFlight);
      System.out.println();
      System.out.println("Exception summary");
      for (Map.Entry<String, Integer> stringIntegerEntry : exceptionCount.entrySet()) {
        String exceptionText = stringIntegerEntry.getKey();
        Integer count = stringIntegerEntry.getValue();

        System.out.println(exceptionText);
        System.out.println(count + " (" + count / secondsSinceTestStart + "/s)");
      }
      System.out.println();
      System.out.println("Status code summary");
      for (Map.Entry<Integer, Integer> stringIntegerEntry : statusCounts.entrySet()) {
        Integer statusCode = stringIntegerEntry.getKey();
        Integer count = stringIntegerEntry.getValue();
        System.out.println(statusCode + ":" + count + " (" + count / secondsSinceTestStart + "/s)");
        Quantiles.ScaleAndIndexes percentiles = Quantiles.percentiles().indexes(50, 90, 95, 99);
        if (updateStatusLatencySampler.containsKey(statusCode)) {
          System.out.println("Batch %iles   : " + displayPercentiles(percentiles.compute(updateStatusLatencySampler.get(statusCode).getValues())));
        }
        System.out.println("Overall %iles : " + displayPercentiles(percentiles.compute(overallStatusLatencySampler.get(statusCode).getValues())));
      }
      updateStatusLatencySampler.clear();
      System.out.println();
    } else if (message instanceof RequestAndResponseDetailsMessage) {
      RequestAndResponseDetailsMessage requestAndResponseDetailsMessage = (RequestAndResponseDetailsMessage) message;
      requestContents.put(requestAndResponseDetailsMessage.getStatusCode(), requestAndResponseDetailsMessage.getRequestString());
      responseHeaders.put(requestAndResponseDetailsMessage.getStatusCode(), requestAndResponseDetailsMessage.getResponseHeaders());
      responseContents.put(requestAndResponseDetailsMessage.getStatusCode(), requestAndResponseDetailsMessage.getResponseBodyPart());
    } else if (message instanceof InitializeStatisticsMessage) {
      InitializeStatisticsMessage initializeStatisticsMessage = (InitializeStatisticsMessage) message;
      errorCount = initializeStatisticsMessage.getErrorCount();
      successCount = initializeStatisticsMessage.getSuccessCount();
      requestsSentCount = initializeStatisticsMessage.getRequestsSentCount();
      totalRequestsEnqueued = initializeStatisticsMessage.getTotalRequestsEnqueued();
      requestsInFlight = initializeStatisticsMessage.getRequestsInFlight();
    } else if (message instanceof UpdateStatisticsMessage) {
      UpdateStatisticsMessage updateStatisticsMessage = (UpdateStatisticsMessage) message;
      requestsInFlight.decrementAndGet();
      RequestStatistics requestStatistics = updateStatisticsMessage.getRequestStatistics();
      if (requestStatistics.isExceptionCaught() || 400 <= requestStatistics.getStatusCode()) {
        if (requestStatistics.isExceptionCaught()) {
          String exceptionText = requestStatistics.getExceptionText();
          Integer thisExceptionCount = exceptionCount.getOrDefault(exceptionText, 0);
          exceptionCount.put(exceptionText, thisExceptionCount + 1);
        } else {
          logNonExceptionalRequest(requestStatistics);
        }

        errorCount.incrementAndGet();
      } else {
        logNonExceptionalRequest(requestStatistics);

        successCount.incrementAndGet();
      }
    }

    // Create fancy HTML report
    if (message instanceof EndStatisticsMessage) {
      EndStatisticsMessage endStatisticsMessage = (EndStatisticsMessage) message;

      createReport(endStatisticsMessage.getTestStartTime(), reportName);
      System.out.println("Wrote report to " + reportName);
      instance = null; // Clear this out since it should go out of scope for the next run.
    }
  }

  private static String displayPercentiles(Map<Integer, Double> percentileMap) {
    return PERCENTILES.stream()
        .map(percentile -> String.format("%d%%ile %.1f ms", percentile, percentileMap.get(percentile)))
        .collect(Collectors.joining("     "));
  }

  private void createReport(long startTime, String reportFileName) {
    float secondsSinceTestStart = (System.currentTimeMillis() - startTime) / 1000.0f;

    int currentSuccessCount = successCount.get();

    int currentErrorCount = errorCount.get();

    int currentRequestsSentCount = requestsSentCount.get();

    int currentTotalRequestsEnqueued = totalRequestsEnqueued.get();


    String reportContents = html(
        head(),
        body(
            h1("Pinot benchmark results"),
            p("Ran at " + new LocalDateTime(startTime).toString()),
            hr(),
            h2("Summary of events"),
            displayCountRateAndTotal("Enqueued", currentTotalRequestsEnqueued, secondsSinceTestStart, currentTotalRequestsEnqueued),
            displayCountRateAndTotal("Sent", currentRequestsSentCount, secondsSinceTestStart, currentTotalRequestsEnqueued),
            displayCountRateAndTotal("Error", currentErrorCount, secondsSinceTestStart, currentTotalRequestsEnqueued),
            displayCountRateAndTotal("Successful", currentSuccessCount, secondsSinceTestStart, currentTotalRequestsEnqueued),
            hr(),
            h2("Exception summary"),
            each(exceptionCount.entrySet(), exceptionAndCount ->
                displayCountRateAndTotal(exceptionAndCount.getKey(), exceptionAndCount.getValue(), secondsSinceTestStart, currentTotalRequestsEnqueued)),
            hr(),
            h2("HTTP status code summary"),
            each(statusCounts.entrySet(), statusAndCount ->
                div(displayCountRateAndTotal(Integer.toString(statusAndCount.getKey()), statusAndCount.getValue(),
                    secondsSinceTestStart, currentTotalRequestsEnqueued),
                    displayQuantiles(overallStatusLatencySampler.get(statusAndCount.getKey()).getValues()),
                    b("Sample request"),
                    pre(requestContents.get(statusAndCount.getKey())),
                    b("Sample response headers"),
                    pre(responseHeaders.get(statusAndCount.getKey())),
                    b("Sample response body"),
                    pre(responseContents.get(statusAndCount.getKey()))
                ))
        )
    ).render();

    try {
      PrintWriter writer = new PrintWriter(new FileOutputStream(reportFileName));
      writer.write(reportContents);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private ContainerTag displayCountRateAndTotal(String eventType, int eventCount, float timeInSeconds, int totalEventCount) {
    return div(
        h3(eventType),
        p(eventCount + " " + String.format("%02.2f/s", eventCount / timeInSeconds) + " " + String.format("%02.2f%%", eventCount * 100.0 / totalEventCount))
    );
  }

  private ContainerTag displayQuantiles(double[] values) {
    Map<Integer, Double> quantiles = Quantiles.percentiles().indexes(PERCENTILES).compute(values);
    return div(
        each(PERCENTILES, percentile ->
            p(b(percentile + "%ile "), span(String.format("%02.2fms", quantiles.get(percentile)))))
    );
  }

  private void logNonExceptionalRequest(RequestStatistics requestStatistics) {
    Integer statusCode = requestStatistics.getStatusCode();
    Integer statusCodeCount = statusCounts.getOrDefault(statusCode, 0);
    statusCounts.put(statusCode, statusCodeCount + 1);

    ReservoirSampler batchReservoirSampler =
        updateStatusLatencySampler.computeIfAbsent(statusCode, k -> new ReservoirSampler(10000));
    ReservoirSampler overallReservoirSampler =
        overallStatusLatencySampler.computeIfAbsent(statusCode, k -> new ReservoirSampler(100000));

    long requestNanos = requestStatistics.getHeadersReceivedNanos() - requestStatistics.getSendNanos();
    double millis = requestNanos / 1000000.0;

    batchReservoirSampler.addSample(millis);
    overallReservoirSampler.addSample(millis);
  }

  public static StatisticsActor getInstance() {
    return instance;
  }

  public boolean hasSeenStatusCode(int statusCode) {
    return statusCounts.containsKey(statusCode);
  }
}
