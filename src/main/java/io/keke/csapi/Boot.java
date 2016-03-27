package io.keke.csapi;

import io.cloudslang.lang.api.Slang;
import io.cloudslang.lang.compiler.SlangSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author keke
 */
public class Boot {
  public static void main(String... args) {
    ApplicationContext applicationContext =
        new ClassPathXmlApplicationContext("classpath*:/META-INF/spring/cloudSlangContext.xml");

    Slang slang = applicationContext.getBean(Slang.class);

    slang.subscribeOnAllEvents(event -> System.out.println(event.getEventType() + " : " + event.getData()));

    File flowFile = new File("./src/test/cs/test/flow.sl");
    Set<SlangSource> dependencies = new HashSet<>();
    dependencies.add(SlangSource.fromFile(new File("./src/test/cs/test/print.sl")));
    System.out.println("To run file");
    slang.compileAndRun(SlangSource.fromFile(flowFile), dependencies, new HashMap<>(),
        new HashSet<>());
    System.out.println("Flow executed");
  }
}
