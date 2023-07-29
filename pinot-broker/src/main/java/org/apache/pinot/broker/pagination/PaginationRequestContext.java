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
package org.apache.pinot.broker.pagination;

/**
 * This is the interface for storing the context of pagination request.
 */
public interface PaginationRequestContext {

  int getErrorCode();

  void setErrorCode(int errorCode);

  String getExceptionMessage();

  void setExceptionMessage(String exceptionMessage);

  int getPaginationOffset();

  void setPaginationOffset(int paginationOffset);

  int getPaginationFetch();

  void setPaginationFetch(int paginationFetch);

  String getQuery();

  void setQuery(String query);

  String getPointer();

  void setPointer(String pointer);

  long getPaginationTtlMillis();

  void setPaginationTtlMillis(long paginationTtlMillis);

  long getPaginationUploadTimeoutMillis();

  void setPaginationUploadTimeoutMillis(long paginationUploadTimeoutMillis);

  int getNumPaginatedResultRows();

  void setNumPaginatedResultRows(int numPaginatedResultRows);

  long getPaginationInitTimeNanos();

  void setPaginationInitTimeNanos(long paginationInitTimeNanos);

  long getPaginationUploadTimeNanos();

  void setPaginationUploadTimeNanos(long paginationUploadTimeNanos);

  long getPaginationDownloadTimeNanos();

  void setPaginationDownloadTimeNanos(long paginationDownloadTimeNanos);

  void setTableName(String var1);

  String getTableName();

  long getPaginationQueryExecutionTimeNanos();

  void setPaginationQueryExecutionTimeNanos(long paginationQueryExecutionTimeMs);

  long getPaginationQueryTotalTimeNanos();

  void setPaginationQueryTotalTimeNanos(long paginationQueryTotalTime);
}
