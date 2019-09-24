package org.apache.pinot.benchmark.api.benchmarks;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pinot.benchmark.common.actors.StatisticsActor;
import org.apache.pinot.benchmark.common.messages.EndStatisticsMessage;
import org.apache.pinot.benchmark.common.messages.InitializeStatisticsMessage;
import org.apache.pinot.benchmark.common.providers.Provider;
import org.apache.pinot.benchmark.common.utils.HttpClient;
import org.apache.pinot.benchmark.common.utils.QueryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;


public class BenchmarkExecutor {
  public static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkExecutor.class);

  private final ExecutorService _executorService;
  private final Map<String, Long> _benchmarkRunMap;

  public BenchmarkExecutor(Map<String, Long> benchmarkRunMap) {
    _executorService = Executors.newSingleThreadExecutor();
    _benchmarkRunMap = benchmarkRunMap;
  }

  public void run(String targetTableName, Provider<String> brokerProvider, Provider<String> queryProvider, double qps,
      long duration) {
    _benchmarkRunMap.put(targetTableName, System.currentTimeMillis() + duration);
    _executorService.submit(new BenchmarkRun(targetTableName, qps, duration, brokerProvider, queryProvider));
  }

  public class BenchmarkRun implements Callable<Void> {

    private String _tableName;
    private double _qps;
    private long _duration;
    private Provider<String> _brokerProvider;
    private Provider<String> _queryProvider;

    public BenchmarkRun(String tableName, double qps, long duration, Provider<String> brokerProvider,
        Provider<String> queryProvider) {
      _tableName = tableName;
      _qps = qps;
      _duration = duration;
      _brokerProvider = brokerProvider;
      _queryProvider = queryProvider;
    }

    @Override
    public Void call()
        throws Exception {

      // Initialize shared counters
      AtomicInteger errorCount = new AtomicInteger(0);
      AtomicInteger successCount = new AtomicInteger(0);
      AtomicInteger requestsSentCount = new AtomicInteger(0);
      AtomicInteger totalRequestsEnqueued = new AtomicInteger(0);
      AtomicInteger requestsInFlight = new AtomicInteger(0);

      // Prepare statistics actor
      ActorSystem actorSystem = ActorSystem.create();
      ActorRef statisticsActor = actorSystem.actorOf(Props.create(StatisticsActor.class, "report.html"));
      statisticsActor.tell(
          new InitializeStatisticsMessage(errorCount, successCount, requestsSentCount, totalRequestsEnqueued,
              requestsInFlight), ActorRef.noSender());

      long startTime = System.currentTimeMillis();
      QueryUtils
          .runQueries(_brokerProvider, _queryProvider, _qps, requestsSentCount, totalRequestsEnqueued, requestsInFlight,
              statisticsActor, HttpClient.get(), _duration, startTime);

      // Wait for requests in flight to complete
      System.out.print("Enqueued all requests, waiting for requests in flight to terminate...");
      while (0 < requestsInFlight.intValue()) {
        Thread.sleep(100);
        System.out.print(" " + requestsInFlight.intValue());
      }
      System.out.println();

      statisticsActor.tell(new EndStatisticsMessage(startTime), ActorRef.noSender());

      System.out.print("Awaiting termination... ");
      Future<Terminated> future = actorSystem.terminate();
      Await.result(future, Duration.Inf());
      System.out.println("done");

      LOGGER.info("Finished running benchmark test for Table: " + _tableName);
      _benchmarkRunMap.remove(_tableName);
      return null;
    }
  }
}
