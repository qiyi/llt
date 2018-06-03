package org.isouth.llt.etcd;

import org.springframework.cloud.bootstrap.BootstrapConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@BootstrapConfiguration
@Configuration
@Conditional(EtcdCondition.class)
public class EtcdConfiguration {

    @Bean
    public EtcdService etcdService() {
        return new EtcdService();
    }
}
