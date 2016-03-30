package io.keke.csapi;

import io.cloudslang.lang.api.Slang;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author keke
 */
public class Boot {
  public static void main(String... args) {
    ApplicationContext applicationContext =
        new ClassPathXmlApplicationContext("classpath*:/META-INF/spring/cloudSlangContext.xml");

    Slang slang = applicationContext.getBean(Slang.class);

    slang.subscribeOnAllEvents(event -> System.out.println(event.getEventType() + " : " + event.getData()));
    Vertx vertx = Vertx.vertx();
    HttpServer server = vertx.createHttpServer();
    server.requestHandler(request -> {
      HttpServerResponse response = request.response();
      response.putHeader("content-type", "text/plain");
      response.end("Hello");
    });
    server.listen(8080);


//
//    File flowFile = new File("./src/test/cs/test/flow.sl");
//    Set<SlangSource> dependencies = new HashSet<>();
//    dependencies.add(SlangSource.fromFile(new File("./src/test/cs/test/print.sl")));
//    System.out.println("To run file");
//    slang.compileAndRun(SlangSource.fromFile(flowFile), dependencies, new HashMap<>(),
//        new HashSet<>());
//    System.out.println("Flow executed");
  }
}
