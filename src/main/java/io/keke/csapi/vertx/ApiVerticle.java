package io.keke.csapi.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Created by keke on 3/29/16.
 */
public class ApiVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    Router apiRouter = Router.router(vertx);
    apiRouter.route().handler(BodyHandler.create());
    router.mountSubRouter("/api/v1", apiRouter);

    apiRouter.post("/run").produces("application/json").handler(this::run);

    server.requestHandler(router::accept).listen(config().getInteger("http.port", 8080));
  }

  private void run(RoutingContext routingContext) {
    String flow = routingContext.request().getParam("flow");
    JsonObject arg = new JsonObject().put("flow", flow);

    vertx.eventBus().send("run.flow", arg, ar -> {
      if (ar.succeeded()) {
        JsonObject result = (JsonObject) ar.result().body();
        JsonObject returnValue = new JsonObject().put("outputs", new JsonObject());
        if (result.getInteger("R") == 0) {
          String slangId = "SLANG-" + result.getLong("executionId");

          MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(slangId);

          consumer.handler(m -> {
            JsonObject r = (JsonObject) m.body();
            switch (r.getString("action")) {
              case "output":
                returnValue.getJsonObject("outputs").put(r.getLong("timestamp").toString(), r.getString("text"));
                break;
              case "end":
                returnValue.put("result", r.getString("result"));
                routingContext.response().end(returnValue.toString());
                consumer.unregister();
                break;
            }
          });
        } else {

        }
      } else {
        routingContext.response().end("ERROR");
      }
    });

  }

  @Override
  public void stop() throws Exception {
    super.stop();
  }
}
