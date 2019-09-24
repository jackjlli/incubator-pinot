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

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javax.net.ssl.SSLException;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;


/**
 * Returns an appropriately configured async http client
 */
public class HttpClient {

  protected HttpClient() {

  }

  public static AsyncHttpClient get() throws SSLException {
    SslContext
        sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    // Prepare HTTP client
    AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
        .setConnectTimeout(500)
        .setMaxConnections(5000)
        .setReadTimeout(15000)
        .setRequestTimeout(20000)
        .setMaxRequestRetry(0)
        .setCompressionEnforced(true)
        .setSslContext(sslContext)
        .build();
    return new DefaultAsyncHttpClient(config);
  }
}
