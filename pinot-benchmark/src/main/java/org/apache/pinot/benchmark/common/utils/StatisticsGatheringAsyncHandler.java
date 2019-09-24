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
import com.google.common.base.Charsets;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pinot.benchmark.common.actors.StatisticsActor;
import org.apache.pinot.benchmark.common.messages.RequestAndResponseDetailsMessage;
import org.apache.pinot.benchmark.common.messages.RequestStatistics;
import org.apache.pinot.benchmark.common.messages.UpdateStatisticsMessage;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Response;
import org.asynchttpclient.handler.TransferCompletionHandler;
import org.asynchttpclient.netty.request.NettyRequest;


/**
 * FIXME Document me!
 */
public class StatisticsGatheringAsyncHandler extends TransferCompletionHandler {
  private ActorRef statisticsActor;
  private RequestStatistics requestStatistics;
  private AtomicInteger requestsSentCounter;
  private NettyRequest nettyRequest;
  private String responseBody;
  private String responseHeaders;

  public StatisticsGatheringAsyncHandler(ActorRef statisticsActor, AtomicInteger requestSentCounter) {
    this.statisticsActor = statisticsActor;
    this.requestsSentCounter = requestSentCounter;
    requestStatistics = new RequestStatistics();
    requestStatistics.setEnqueueNanos(System.nanoTime());
  }

  @Override
  public void onThrowable(Throwable t) {
    requestStatistics.setExceptionReceivedNanos(System.nanoTime());
    requestStatistics.setExceptionCaught(true);
    requestStatistics.setExceptionText(t.toString());
    t.printStackTrace();
    statisticsActor.tell(new UpdateStatisticsMessage(requestStatistics), ActorRef.noSender());
  }

  @Override
  public void onRequestSend(NettyRequest request) {
    nettyRequest = request;
    requestStatistics.setSendNanos(System.nanoTime());
    requestsSentCounter.incrementAndGet();
  }

  @Override
  public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
    StringBuffer buffer = new StringBuffer();
    byte[] bodyPartBytes = bodyPart.getBodyPartBytes();
    if (bodyPartBytes != null) {
      buffer.append(new String(bodyPartBytes, Charsets.ISO_8859_1));
    }
    String bodyString = buffer.toString();
    if (shouldKeepHeadersAndBody()) {
      responseBody += bodyString;
    }

    if (bodyString.contains("hasPartialResults")) {
      requestStatistics.setExceptionReceivedNanos(System.nanoTime());
      requestStatistics.setExceptionCaught(true);
      requestStatistics.setExceptionText("Partial result");
    }

    return State.CONTINUE;
  }

  @Override
  public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
    requestStatistics.setStatusCode(responseStatus.getStatusCode());
    requestStatistics.setHeadersReceivedNanos(System.nanoTime());
    statisticsActor.tell(new UpdateStatisticsMessage(requestStatistics), ActorRef.noSender());

    // Normally, we'd abort here to avoid the overhead of processing the rest of the response but unfortunately it
    // causes problems with HTTP pipelining
    return State.CONTINUE;
  }

  @Override
  public State onHeadersReceived(HttpHeaders headers) throws Exception {
    if (shouldKeepHeadersAndBody()) {
      StringBuilder builder = new StringBuilder();
      for (Map.Entry<String, String> keyValueEntry : headers.entries()) {
        builder.append(keyValueEntry.getKey()).append(": ").append(keyValueEntry.getValue()).append("\n");
      }
      responseHeaders = builder.toString();
    }

    return State.CONTINUE;
  }

  @Override
  public Response onCompleted(Response response) throws Exception {
    if (shouldKeepHeadersAndBody()) {
      StringBuilder builder = new StringBuilder();
      HttpRequest request = nettyRequest.getHttpRequest();
      builder.append(request.method()).append(" ").append(request.uri()).append("\n\n");

      HttpHeaders headers = request.headers();
      for (Map.Entry<String, String> headerEntry : headers) {
        builder.append(headerEntry.getKey()).append(": ").append(headerEntry.getValue()).append("\n");
      }

      String requestString = builder.toString();

      statisticsActor.tell(new RequestAndResponseDetailsMessage(requestStatistics.getStatusCode(), requestString, responseHeaders,
          responseBody), ActorRef.noSender());
    }

    return response;
  }

  private boolean shouldKeepHeadersAndBody() {
    return StatisticsActor.getInstance().hasSeenStatusCode(requestStatistics.getStatusCode());
  }
}
