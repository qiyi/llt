package org.isouth.llt.bootstrap;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author qiyi
 * @since 1.0
 */
public class Bootstrap {
    /**
     * Get the bootstrap environment including system environemtn and system properties
     *
     * @param resourceLoader resource loader for loading properties files
     * @param profiles       active profiles for bootstrap environment
     * @return the bootstrap environment
     */
    public static ConfigurableEnvironment getBootstrapEnvironment(ResourceLoader resourceLoader, String[] profiles) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles(profiles);
        List<String> locations = Arrays.stream(profiles)
                .map(profile -> ResourceUtils.CLASSPATH_URL_PREFIX + "bootstrap-" + profile + ".properties")
                .collect(Collectors.toList());
        locations.add(ResourceUtils.CLASSPATH_URL_PREFIX + "bootstrap.properties");
        ResourceLoader loader = resourceLoader == null ? new DefaultResourceLoader() : resourceLoader;
        locations.forEach(location -> addPropertiesFileToEnvironment(environment, loader, location));
        return environment;
    }

    /**
     * Get a properties file and add to the environment
     *
     * @param environment    the target environment
     * @param resourceLoader resource loader for loading the resource
     * @param location       resource location
     */
    public static void addPropertiesFileToEnvironment(ConfigurableEnvironment environment, ResourceLoader resourceLoader, String location) {
        String resolvedLocation = environment.resolveRequiredPlaceholders(location);
        Resource resource = resourceLoader.getResource(resolvedLocation);
        if (resource.exists()) {
            try {
                environment.getPropertySources().addLast(new ResourcePropertySource(resource));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Create bootstrap application context with the supplied active profiles
     *
     * @param profiles active profiles
     * @return refreshed bootstrap application context
     */
    public static ConfigurableApplicationContext create(String[] profiles) {
        AnnotationConfigApplicationContext bootstrapContext = new AnnotationConfigApplicationContext();

        // Get bootstrap environment and add to a composite property source
        ConfigurableEnvironment bootstrapEnvironment = getBootstrapEnvironment(bootstrapContext, profiles);
        MutablePropertySources bootstrapPropertySources = bootstrapEnvironment.getPropertySources();
        bootstrapPropertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        bootstrapPropertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        CompositePropertySource bootstrapPropertySource = new CompositePropertySource("bootstrap");
        bootstrapPropertySources.forEach(bootstrapPropertySource::addPropertySource);

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
}
