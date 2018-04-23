package org.isouth.llt.bootstrap;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class Bootstrap {
    public static ConfigurableApplicationContext create(ConfigurableEnvironment environment) {
        AnnotationConfigApplicationContext bootstrapContext = new AnnotationConfigApplicationContext();
        ConfigurableEnvironment bootstrapEnvironment = bootstrapContext.getEnvironment();
        MutablePropertySources bootstrapProperties = bootstrapEnvironment
                .getPropertySources();
        for (PropertySource<?> source : bootstrapProperties) {
            bootstrapProperties.remove(source.getName());
        }
        for (PropertySource<?> source : environment.getPropertySources()) {
            bootstrapProperties.addLast(source);
        }
        CompositePropertySource bootstrapPropertySource = new CompositePropertySource("bootstrap");
        for (String profile : environment.getActiveProfiles()) {
            String filename = "bootstrap-" + profile + ".properties";
            loadPropertySource(bootstrapPropertySource, bootstrapContext, filename);
        }
        loadPropertySource(bootstrapPropertySource, bootstrapContext, "bootstrap.properties");
        bootstrapProperties.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, bootstrapPropertySource);

        ClassLoader classLoader = bootstrapContext.getClassLoader();
        List<String> names = SpringFactoriesLoader
                .loadFactoryNames(BootstrapConfiguration.class, classLoader);
        List<Class<?>> sources = new ArrayList<>();
        for (String name : names) {
            sources.add(ClassUtils.resolveClassName(name, classLoader));
        }
        AnnotationAwareOrderComparator.sort(sources);

        bootstrapContext.setEnvironment(bootstrapEnvironment);
        sources.forEach(bootstrapContext::register);
        bootstrapContext.refresh();
        return bootstrapContext;
    }

    private static void loadPropertySource(CompositePropertySource compositePropertySource, ResourceLoader resourceLoader, String filename) {
        PropertiesPropertySource propertySource = loadPropertySource(resourceLoader, filename);
        if (propertySource != null) {
            compositePropertySource.addPropertySource(propertySource);
        }

    }

    private static PropertiesPropertySource loadPropertySource(ResourceLoader resourceLoader, String filename) {
        Resource resource = resourceLoader.getResource("classpath:" + filename);
        if (resource.exists()) {
            try {
                new PropertiesPropertySource(filename, PropertiesLoaderUtils.loadProperties(resource));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return null;
    }
}
