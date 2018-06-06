package org.isouth.llt.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.isouth.llt.bootstrap.Bootstrap;
import org.isouth.llt.bootstrap.BootstrapConfiguration;
import org.isouth.llt.spring.LLTInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link TomcatLoader} used with {@link org.springframework.test.context.ContextConfiguration} and without
 * spring boot, The loader will create embedded tomcat.
 *
 * @author qiyi
 * @since 1.0
 */
public class TomcatLoader extends AbstractContextLoader {
    @Override
    public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
        super.processContextConfiguration(configAttributes);
        if (configAttributes.getLocations().length == 0
                && configAttributes.getClasses().length == 0
                && configAttributes.getInitializers().length == 0) {
            // Set a class
            configAttributes.setClasses(BootstrapConfiguration.class);
        }
    }

    @Override
    protected String getResourceSuffix() {
        return ".xml";
    }

    @Override
    public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
        // prepare bootstrap environment
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        ConfigurableEnvironment bootstrapEnvironment = Bootstrap.getBootstrapEnvironment(resourceLoader,
                mergedConfig.getActiveProfiles());
        TestPropertySourceUtils.addPropertiesFilesToEnvironment(bootstrapEnvironment, resourceLoader,
                mergedConfig.getPropertySourceLocations());
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(bootstrapEnvironment,
                mergedConfig.getPropertySourceProperties());

        // prepare webapp configuration
        int port = bootstrapEnvironment.getProperty("llt.tomcat.port", int.class, 8080);
        String contextPath = bootstrapEnvironment.getProperty("llt.tomcat.contextPath", "");
        String docBase = bootstrapEnvironment.getProperty("llt.tomcat.docBase", "src/main/webapp");
        File docBaseDir = new File(docBase);
        if (!docBaseDir.exists() || !docBaseDir.isDirectory()) {
            docBaseDir = createTempDocBase(port);
        }

        // start webapp
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        Context context = tomcat.addWebapp(contextPath, docBaseDir.getAbsolutePath());
        context.setResources(new StandardRoot(context));
        context.setTldValidation(bootstrapEnvironment.getProperty("llt.tomcat.tldValidation", boolean.class, false));
        StandardJarScanner jarScanner = new StandardJarScanner();
        jarScanner.setScanClassPath(bootstrapEnvironment.getProperty("llt.tomcat.scanClassPath", boolean.class, true));
        jarScanner.setScanManifest(bootstrapEnvironment.getProperty("llt.tomcat.scanManifest", boolean.class, false));
        StandardJarScanFilter scanFilter = new StandardJarScanFilter();
        scanFilter.setDefaultTldScan(bootstrapEnvironment.getProperty("llt.tomcat.tldScan", boolean.class, false));
        jarScanner.setJarScanFilter(scanFilter);
        context.setJarScanner(jarScanner);

        // configure profiles and initializer classes
        context.addParameter(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME,
                String.join(",", mergedConfig.getActiveProfiles()));
        Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses = new HashSet<>(
                mergedConfig.getContextInitializerClasses());
        contextInitializerClasses.add(LLTInitializer.class);
        List<Class<? extends ApplicationContextInitializer<?>>> initializerClasses = new ArrayList<>(
                contextInitializerClasses);
        AnnotationAwareOrderComparator.sort(initializerClasses);
        List<String> initializerNames = initializerClasses.stream().map(Class::getName).collect(Collectors.toList());
        context.addParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM, String.join(",", initializerNames));

        // TODO support custom connector for some ssl sense
        // TODO support servlet annotations while no tomcat.xml supplied
        // TODO add shutdown hook and listener for close event from the application context
        tomcat.start();

        // check if the web app start successfully
        if (context.getState() != LifecycleState.STARTED) {
            throw new IllegalStateException("Webapp started failed");
        }

        // return the tomcat application context
        ServletContext servletContext = context.getServletContext();
        return WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
    }

    private File createTempDocBase(int port) {
        try {
            File tempFile = File.createTempFile("llt.tomcat", "." + port);
            tempFile.delete();
            tempFile.mkdir();
            tempFile.deleteOnExit();
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ApplicationContext loadContext(String... locations) throws Exception {
        throw new UnsupportedOperationException("loadContext");
    }
}
