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

import java.util.Random;


public class ReservoirSampler {
  private int valueCount = 0;
  private double[] values;
  private Random random = new Random();

  public ReservoirSampler(int capacity) {
    values = new double[capacity];
  }

  public void addSample(double value) {
    if (valueCount < values.length) {
      values[valueCount] = value;
      ++valueCount;
    } else {
      ++valueCount;
      int index = random.nextInt(valueCount);
      if (index < values.length) {
        values[index] = value;
      }
    }
  }

  public double[] getValues() {
    if (values.length <= valueCount) {
      return values;
    } else {
      double[] returnValue = new double[valueCount];
      System.arraycopy(values, 0, returnValue, 0, valueCount);
      return returnValue;
    }
  }
}
