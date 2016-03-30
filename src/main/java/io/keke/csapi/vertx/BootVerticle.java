package io.keke.csapi.vertx;

import io.cloudslang.lang.api.Slang;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author keke
 */
public class BootVerticle extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(BootVerticle.class);
  private Slang slang;

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    Future<String> f1 = Future.future();
    Future<String> f2 = Future.future();
    getVertx().deployVerticle("io.keke.csapi.vertx.ApiVerticle", new DeploymentOptions().setConfig(config()), f1.completer());
    getVertx().deployVerticle("io.keke.csapi.vertx.SlangVerticle", new DeploymentOptions().setConfig(config()), f2.completer());
    CompositeFuture.all(f1, f2).setHandler(r -> {
      if (r.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(r.cause());
      }
    });
  }


  @Override
  public void stop() throws Exception {
    super.stop();
  }

  public static void main(String... args) throws IOException {
    JsonObject cfg = new JsonObject(FileUtils.readFileToString(new File(args[0])));
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new BootVerticle(), new DeploymentOptions(cfg), handle -> {
      LOG.info("Verticles are deployed");
    });
  }
}
