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
 * FIXME Document me!
 */
public class RequestAndResponseDetailsMessage {
  private final int statusCode;
  private final String requestString;
  private final String responseHeaders;
  private final String responseBodyPart;

  public RequestAndResponseDetailsMessage(int statusCode, String requestString, String responseHeaders,
      String responseBodyPart) {
    this.statusCode = statusCode;
    this.requestString = requestString;
    this.responseHeaders = responseHeaders;
    this.responseBodyPart = responseBodyPart;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getRequestString() {
    return requestString;
  }

  public String getResponseHeaders() {
    return responseHeaders;
  }

  public String getResponseBodyPart() {
    return responseBodyPart;
  }
}
