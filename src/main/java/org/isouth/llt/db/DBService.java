package org.isouth.llt.db;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * maria db service
 *
 * @author qiyi
 * @since 1.0
 */
public class DBService {

    private final Logger logger = LoggerFactory.getLogger(DBService.class);

    @Autowired
    private Environment environment;

    @Value("${llt.db.databases:test}")
    private String[] databases;

    @Value("${llt.db.port:3306}")
    private int port;

    private DB db;

    @PostConstruct
    public void init() throws ManagedProcessException, InterruptedException, ExecutionException {
        // create and start db
        this.db = DB.newEmbeddedDB(DBConfigurationBuilder.newBuilder().setPort(port).build());
        db.start();

        // create databases and execute sql files
        StopWatch sw = new StopWatch();
        sw.start();
        ExecutorService executorService = Executors.newFixedThreadPool(databases.length);
        List<Future<Void>> futures = new ArrayList<>(databases.length);
        for (String database : databases)
            futures.add(executorService.submit(() -> {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                logger.info("Start create database:{}", databases);
                db.createDB(database);
                if (environment.getProperty("llt.db.flyway.enable", boolean.class, false)) {
                    String url = "jdbc:mysql://localhost:" + port + "/" + database +
                            "?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8&useSSL=false";
                    logger.info("Start migrate on:{}", url);
                    Flyway flyway = new Flyway();
                    flyway.setDataSource(url, null, null, null);
                    // use database locations priority
                    String locations = environment.getProperty("llt.db.flyway." + database + ".locations");
                    if (locations == null) {
                        locations = environment.getProperty("llt.db.flyway.locations", "db/migration");
                    }
                    flyway.setLocations(locations);
                    flyway.migrate();
                }
                stopWatch.stop();
                logger.info("Create database {} successfully, cost:{}ms", databases, stopWatch.getTotalTimeMillis());
                return null;
            }));
        executorService.shutdown();
        for (Future<Void> future : futures) {
            future.get();
        }
        sw.stop();
        logger.info("DBService init cost:{}ms", sw.getTotalTimeMillis());
    }

    @PreDestroy
    public void destroy() throws ManagedProcessException {
        if (db != null) {
            db.stop();
        }
    }
}
