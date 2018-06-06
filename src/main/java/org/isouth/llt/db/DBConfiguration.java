package org.isouth.llt.db;

import org.springframework.cloud.bootstrap.BootstrapConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * DB bootstrap configuration class
 *
 * @author qiyi
 * @since 1.0
 */
@BootstrapConfiguration
@Configuration
@Conditional(DBCondition.class)
public class DBConfiguration {

    @Bean
    public DBService dbService() {
        return new DBService();
    }
}
