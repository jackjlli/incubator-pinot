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
package org.apache.pinot.benchmark.common.messages;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * FIXME Document me!
 */
public class InitializeStatisticsMessage implements ActorMessage {
  private AtomicInteger errorCount;
  private AtomicInteger successCount;
  private AtomicInteger requestsSentCount;
  private AtomicInteger totalRequestsEnqueued;
  private AtomicInteger requestsInFlight;

  public InitializeStatisticsMessage(AtomicInteger errorCount, AtomicInteger successCount,
      AtomicInteger requestsSentCount, AtomicInteger totalRequestsEnqueued, AtomicInteger requestsInFlight) {
    this.errorCount = errorCount;
    this.successCount = successCount;
    this.requestsSentCount = requestsSentCount;
    this.totalRequestsEnqueued = totalRequestsEnqueued;
    this.requestsInFlight = requestsInFlight;
  }

  public AtomicInteger getErrorCount() {
    return errorCount;
  }

  public AtomicInteger getSuccessCount() {
    return successCount;
  }

  public AtomicInteger getRequestsSentCount() {
    return requestsSentCount;
  }

  public AtomicInteger getTotalRequestsEnqueued() {
    return totalRequestsEnqueued;
  }

  public AtomicInteger getRequestsInFlight() {
    return requestsInFlight;
  }
}
