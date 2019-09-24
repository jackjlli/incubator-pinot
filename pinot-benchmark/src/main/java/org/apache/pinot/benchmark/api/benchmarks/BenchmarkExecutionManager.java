package org.apache.pinot.benchmark.api.benchmarks;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.pinot.benchmark.PinotBenchConf;
import org.apache.pinot.benchmark.api.resources.PinotBenchException;
import org.apache.pinot.benchmark.api.resources.SuccessResponse;
import org.apache.pinot.benchmark.common.utils.PinotClusterClient;
import org.apache.pinot.benchmark.common.utils.PinotClusterLocator;
import org.apache.pinot.benchmark.common.providers.Provider;
import org.apache.pinot.benchmark.common.providers.RandomProvider;
import org.apache.pinot.benchmark.common.providers.RoundRobinProvider;
import org.apache.pinot.common.exception.HttpErrorStatusException;
import org.apache.pinot.common.utils.JsonUtils;
import org.apache.pinot.common.utils.SimpleHttpResponse;
import org.apache.pinot.pql.parsers.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BenchmarkExecutionManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkExecutionManager.class);

  private final PinotBenchConf _conf;
  private final PinotClusterClient _pinotClusterClient;
  private final PinotClusterLocator _pinotClusterLocator;
  private final BenchmarkExecutor _benchmarkExecutor;
  private final String _baseQueryDir;
  private final Map<String, Long> _benchmarkRunMap;

  public BenchmarkExecutionManager(PinotBenchConf pinotBenchConf, PinotClusterClient pinotClusterClient,
      PinotClusterLocator pinotClusterLocator) {
    _conf = pinotBenchConf;
    _baseQueryDir = _conf.getPinotBenchBaseQueryDir();
    _pinotClusterClient = pinotClusterClient;
    _pinotClusterLocator = pinotClusterLocator;
    _benchmarkRunMap = new ConcurrentHashMap<>();
    _benchmarkExecutor = new BenchmarkExecutor(_benchmarkRunMap);
  }

  public SuccessResponse runBenchmark(String targetTableName, String qps, String durationStr) {

    // TODO: Call GET /tables/{tableName}/instances,
    //  Then check queries and data are prepare or not.
    //  Finally send queries.

    String response = listInstancesForTable(targetTableName);
    JsonNode responseJsonNode;
    try {
      responseJsonNode = JsonUtils.stringToJsonNode(response);
    } catch (IOException e) {
      throw new PinotBenchException(LOGGER, "IOException when parsing the response of listing instance for Table: " + targetTableName, Response.Status.INTERNAL_SERVER_ERROR);
    }

    JsonNode brokerJsonNode = responseJsonNode.get("brokers");
    JsonNode serverJsonNode = responseJsonNode.get("server");

    List<String> brokers = getListOfInstances(brokerJsonNode);
    List<String> servers = getListOfInstances(serverJsonNode);

    Provider<String> brokerProvider = new RoundRobinProvider<>(brokers);


    // Checks queries are ready or not.
    File targetTableDir = new File(_baseQueryDir, targetTableName);
    if (!targetTableDir.exists()) {
      throw new PinotBenchException(LOGGER, "Queries for Table: " + targetTableName + " have not been uploaded yet.", Response.Status.BAD_REQUEST);
    }
    File[] files = targetTableDir.listFiles();
    if (files == null || files.length == 0) {
      throw new PinotBenchException(LOGGER, "Queries for Table: " + targetTableName + " have not been uploaded yet.", Response.Status.BAD_REQUEST);
    }

    Provider<String> queryProvider;
    try {
      queryProvider = initializeQueryProvider(true);
    } catch (IOException e) {
      throw new PinotBenchException(LOGGER, "IOException when getting the queries for Table: " + targetTableName, Response.Status.INTERNAL_SERVER_ERROR, e);
    }

    long duration = parseDurationToSeconc(durationStr);
    _benchmarkExecutor.run(targetTableName, brokerProvider, queryProvider, Double.parseDouble(qps), duration);
    return new SuccessResponse("Submitted the benchmark run for Table: " + targetTableName + " with " + qps + " qps and " + durationStr + " duration.");
  }

  private String listInstancesForTable(String tableName) {
    Pair<String, Integer> pair = _pinotClusterLocator.getPerfClusterPair();

    try {
      SimpleHttpResponse response = _pinotClusterClient.sendGetRequest(PinotClusterClient.getListInstancesHttpURI(pair.getFirst(), pair.getSecond(), tableName));
      return response.getResponse();
    } catch (HttpErrorStatusException e) {
      throw new PinotBenchException(LOGGER, e.getMessage(), e.getStatusCode(), e);
    } catch (Exception e) {
      throw new PinotBenchException(LOGGER, "Exception when listing instances for Table: " + tableName, Response.Status.INTERNAL_SERVER_ERROR, e);
    }
  }

  private List<String> getListOfInstances(JsonNode jsonNode) {
    List<String> instances = new ArrayList<>();
    for (final JsonNode node: jsonNode) {
      instances.add(node.asText());
    }
    return instances;
  }

  private Provider<String> initializeQueryProvider(boolean random) throws IOException {
    List<String> queries = FileUtils.readLines(new File("queries.txt"));
    System.out.println("Read " + queries.size() + " queries from queries.txt file");
    return (random) ? new RandomProvider<>(queries) : new RoundRobinProvider<>(queries);
  }

  private long parseDurationToSeconc(String durationString) {
    if (durationString.endsWith("s")) {
      String numberStr = durationString.substring(0, durationString.lastIndexOf('s'));
      return TimeUnit.SECONDS.toSeconds(Integer.parseInt(numberStr));
    } else if (durationString.endsWith("hr")) {
      String numberStr = durationString.substring(0, durationString.lastIndexOf("hr"));
      return TimeUnit.HOURS.toSeconds(Integer.parseInt(numberStr));
    } else {
      return TimeUnit.MINUTES.toSeconds(Integer.parseInt(durationString));
    }
  }
}
