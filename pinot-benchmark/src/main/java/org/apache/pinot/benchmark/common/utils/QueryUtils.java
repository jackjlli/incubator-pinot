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
package org.apache.pinot.benchmark.common.utils;

import akka.actor.ActorRef;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pinot.benchmark.common.providers.Provider;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.apache.pinot.benchmark.common.messages.DisplayStatisticsMessage;


public class QueryUtils {

  protected QueryUtils() {

  }

  public static void runQueries(Provider<String> brokerUrlProvider, Provider<String> queryProvider,
      Double desiredQueryRatePerSecond, AtomicInteger requestsSentCount, AtomicInteger totalRequestsEnqueued,
      AtomicInteger requestsInFlight, ActorRef statisticsActor, AsyncHttpClient asyncHttpClient, long secondsForTest,
      long startTime)
      throws InterruptedException, UnsupportedEncodingException {
    // Start sending queries
    final int totalQueryCount = (int) (secondsForTest * desiredQueryRatePerSecond);
    int sentQueryCount = 0;
    int queriesSentSinceLastUpdate = 0;
    long lastUpdateTime = 0L;

    System.out.println("Targeting " + desiredQueryRatePerSecond + " queries per second");

    while (sentQueryCount < totalQueryCount) {
      long millisSinceTestStart = System.currentTimeMillis() - startTime;
      double secondsSinceTestStart = millisSinceTestStart / 1000.0;
      int expectedQueryCount = (int) (desiredQueryRatePerSecond * secondsSinceTestStart);

      int inFlightRequestCount = requestsInFlight.get();

      int queriesToSend = Math.min(
          expectedQueryCount - sentQueryCount,
          desiredQueryRatePerSecond.intValue() - inFlightRequestCount);

      if (0 < queriesToSend) {
        for (int i = 0; i < queriesToSend; i++) {
          String body = "q=statistics&bqlRequest=" + URLEncoder.encode(queryProvider.provide(), "utf-8");
          Request request = new RequestBuilder()
              .setUrl(brokerUrlProvider.provide())
              .addHeader("Content-Type", "application/x-www-form-urlencoded")
              .addHeader("X-HTTP-Method-Override", "GET")
              .setBody(body.getBytes())
              .setMethod("POST")
              .build();

          AsyncHandler statisticsHandler = new StatisticsGatheringAsyncHandler(statisticsActor, requestsSentCount);

          asyncHttpClient.executeRequest(request, statisticsHandler);

          sentQueryCount++;
          totalRequestsEnqueued.incrementAndGet();
          requestsInFlight.incrementAndGet();
          queriesSentSinceLastUpdate++;
        }
      }

      // Display stats every once in a while
      if (100000 < queriesSentSinceLastUpdate || 30000L < System.currentTimeMillis() - lastUpdateTime) {
        statisticsActor.tell(new DisplayStatisticsMessage(startTime), ActorRef.noSender());
        queriesSentSinceLastUpdate = 0;
        lastUpdateTime = System.currentTimeMillis();
      }

      // Wait for a bit to re-enqueue requests
      Thread.sleep(1);
    }
  }
}
