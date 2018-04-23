package org.isouth.llt.spring;

import org.isouth.llt.bootstrap.Bootstrap;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.Map;

public class LLTInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        ConfigurableEnvironment environment = configurableApplicationContext.getEnvironment();
        ConfigurableApplicationContext bootstrapContext = Bootstrap.create(environment);
        configurableApplicationContext.setParent(bootstrapContext);
        Map<String, ApplicationContextInitializer> initializerMap = bootstrapContext.getBeansOfType(ApplicationContextInitializer.class);
        ArrayList<ApplicationContextInitializer> initializers = new ArrayList<>(initializerMap.values());
        AnnotationAwareOrderComparator.sort(initializers);
        for (ApplicationContextInitializer initializer : initializers) {
            //noinspection unchecked
            initializer.initialize(configurableApplicationContext);
        }
    }
}
