package pase.test.com.order.management.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartUp implements CommandLineRunner {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${pom.version}")
    private String pomVersion;

    @Value("${app.library.database.version}")
    private String databaseLibraryVersion;

    @Value("${server.port}")
    private String port;

    @Override
    public void run(String... args) {
        log.info("----------------------------------------------------------------");
        log.info("Microservice: [{}]", applicationName);
        log.info("Version: [{}]", pomVersion);
        log.info("DatabaseLibrary: [{}]", databaseLibraryVersion);
        log.info("Port: [{}]", port);
        log.info("StartUp: [✓]");
        log.info("----------------------------------------------------------------");
    }
}
