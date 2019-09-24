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

/**
 * Wrapper for statistics about a request.
 */
public class RequestStatistics {
  private long enqueueNanos = 0L;
  private long sendNanos = 0L;
  private long headersReceivedNanos = 0L;
  private long exceptionReceivedNanos = 0L;
  private int statusCode = 0;
  private boolean exceptionCaught = false;
  private String exceptionText = "";
  public RequestStatistics() {

  }

  public long getEnqueueNanos() {
    return enqueueNanos;
  }

  public void setEnqueueNanos(long enqueueNanos) {
    this.enqueueNanos = enqueueNanos;
  }

  public long getSendNanos() {
    return sendNanos;
  }

  public void setSendNanos(long sendNanos) {
    this.sendNanos = sendNanos;
  }

  public long getHeadersReceivedNanos() {
    return headersReceivedNanos;
  }

  public void setHeadersReceivedNanos(long headersReceivedNanos) {
    this.headersReceivedNanos = headersReceivedNanos;
  }

  public long getExceptionReceivedNanos() {
    return exceptionReceivedNanos;
  }

  public void setExceptionReceivedNanos(long exceptionReceivedNanos) {
    this.exceptionReceivedNanos = exceptionReceivedNanos;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public boolean isExceptionCaught() {
    return exceptionCaught;
  }

  public void setExceptionCaught(boolean exceptionCaught) {
    this.exceptionCaught = exceptionCaught;
  }

  public String getExceptionText() {
    return exceptionText;
  }

  public void setExceptionText(String exceptionText) {
    this.exceptionText = exceptionText;
  }
}
