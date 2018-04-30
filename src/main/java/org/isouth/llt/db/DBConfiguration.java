package org.isouth.llt.db;

import org.springframework.cloud.bootstrap.BootstrapConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@BootstrapConfiguration
@Configuration
@Conditional(DBCondition.class)
public class DBConfiguration {

    @Bean(initMethod = "init", destroyMethod = "destroy")
    public DBService dbService() {
        return new DBService();
    }
}
