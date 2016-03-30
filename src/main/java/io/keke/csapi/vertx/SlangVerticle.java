package io.keke.csapi.vertx;

import io.cloudslang.lang.api.Slang;
import io.cloudslang.lang.compiler.SlangSource;
import io.cloudslang.lang.runtime.env.ReturnValues;
import io.cloudslang.lang.runtime.events.LanguageEventData;
import io.cloudslang.score.events.ScoreEvent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.Nullable;
import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author keke
 */
public class SlangVerticle extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(SlangVerticle.class);
  private Slang slang;
  private Set<SlangSource> deps;
  private Set<String> depPaths;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    final long start = System.currentTimeMillis();

    vertx.executeBlocking(code -> {
      ApplicationContext applicationContext =
          new ClassPathXmlApplicationContext("classpath*:/META-INF/spring/cloudSlangContext.xml");
      slang = applicationContext.getBean(Slang.class);
      slang.subscribeOnAllEvents(this::handleEvent);
      loadDeps();
      code.complete(slang);
    }, result -> {
      if (result.succeeded()) {
        init();
        LOG.info("Slang was created, took {}", System.currentTimeMillis() - start);
        startFuture.complete();
      } else {
        startFuture.fail(result.cause());
      }
    });
  }

  private void handleEvent(ScoreEvent scoreEvent) {
    LOG.debug("Received Slang event type={}, data={}", scoreEvent.getEventType(), scoreEvent.getData());
    switch (scoreEvent.getEventType()) {
      case "EVENT_OUTPUT_START":
        LanguageEventData data = (LanguageEventData) scoreEvent.getData();
        ReturnValues returnValues = (ReturnValues) data.get("actionReturnValues");
        if (returnValues != null) {
          Map<String, Serializable> outputs = returnValues.getOutputs();
          if (outputs != null) {
            String text = (String) returnValues.getOutputs().get("text");
            if (text != null) {
              JsonObject obj = new JsonObject().put("action", "output").put("text", text).put("timestamp", data.getTimeStamp().getTime());
              getVertx().eventBus().publish("SLANG-" + data.getExecutionId(), obj);
            }
          }
        }
        break;
      case "EVENT_EXECUTION_FINISHED":
        data = (LanguageEventData) scoreEvent.getData();
        JsonObject result = new JsonObject().put("result", data.getResult()).put("timestamp", data.getTimeStamp().getTime()).put("action", "end");
        getVertx().eventBus().publish("SLANG-" + data.getExecutionId(), result);
        break;
    }
  }

  private void loadDeps() {
    deps = new HashSet<>();
    depPaths = new HashSet<>();
    config().getJsonArray("deps").forEach(dep -> {
      try {
        URL url = new URL((String) dep);
        if (url.getProtocol().equals("file")) {
          deps.addAll(FileUtils.listFiles(new File(url.getPath()), null, true).stream().map(SlangSource::fromFile).collect(Collectors.toSet()));
          depPaths.add((String) dep);
        } else {
          LOG.warn("Unknown dependency path {}", dep);
        }
      } catch (MalformedURLException e) {
        LOG.error("Unable to load dependencies from {}", dep, e);
      }
    });
  }

  private void init() {
    vertx.eventBus().consumer("run.flow", this::runFlow);
  }

  private <T> void runFlow(Message<T> message) {
    JsonObject arg = (JsonObject) message.body();

    File flow = resolveFlow(arg.getString("flow"));
    LOG.debug("To run flow {}", flow);
    JsonObject result = new JsonObject().put("R", 0);
    if (flow == null) {
      result.put("R", 1);
      message.reply(result);
    } else {
      SlangSource flowSource = SlangSource.fromFile(flow);
      long runId = slang.compileAndRun(flowSource, deps, new HashMap<>(), new HashSet<>());
      LOG.info("Flow {} is run, ID={}", flow, runId);
      result.put("executionId", runId);
      message.reply(result);
    }


  }

  @Nullable
  private File resolveFlow(String flow) {
    for (String path : depPaths) {
      try {
        URL url = new URL(path);
        if (url.getProtocol().equals("file")) {
          File f = new File(url.getPath(), flow);
          if (f.exists() && f.isFile()) {
            return f;
          }
        }
      } catch (MalformedURLException e) {
        LOG.warn("Invalid Path {}", path, e);
        return null;
      }
    }
    return null;
  }

  @Override
  public void stop() throws Exception {
    super.stop();
  }
}
