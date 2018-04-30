package org.isouth.llt.spring;

import org.isouth.llt.bootstrap.Bootstrap;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.Map;

/**
 * {@link LLTInitializer} used with {@link org.springframework.test.context.junit4} or
 * {@link org.springframework.test.context.junit.jupiter.SpringExtension} and without
 * spring boot
 *
 * @author qiyi
 * @since 1.0
 */
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class LLTInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        ConfigurableEnvironment environment = configurableApplicationContext.getEnvironment();
        ConfigurableApplicationContext bootstrapContext = Bootstrap.create(environment.getActiveProfiles());
        configurableApplicationContext.setParent(bootstrapContext);
        Map<String, ApplicationContextInitializer> initializerMap = bootstrapContext.getBeansOfType(ApplicationContextInitializer.class);
        ArrayList<ApplicationContextInitializer> initializerList = new ArrayList<>(initializerMap.values());
        AnnotationAwareOrderComparator.sort(initializerList);
        for (ApplicationContextInitializer initializer : initializerList) {
            //noinspection unchecked
            initializer.initialize(configurableApplicationContext);
        }
    }
}
