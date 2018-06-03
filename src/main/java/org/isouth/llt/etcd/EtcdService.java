package org.isouth.llt.etcd;

import com.opentable.etcd.EtcdConfiguration;
import com.opentable.etcd.EtcdInstance;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * @author qiyi
 * @since 1.0
 */
public class EtcdService {
    private final Logger logger = LoggerFactory.getLogger(EtcdService.class);

    @Value("${llt.etcd.port:4001}")
    private int port;

    @Value("${llt.etcd.rootDir:/}")
    private String rootDir;

    @Value("${llt.etcd.configDir:etcd/}")
    private String configDir;

    private EtcdInstance instance;

    @PostConstruct
    public void start() throws IOException, URISyntaxException, EtcdAuthenticationException, TimeoutException, EtcdException {
        EtcdConfiguration configuration = new EtcdConfiguration();
        Path dir = Files.createTempDirectory("etcd");
        dir.toFile().deleteOnExit();
        configuration.setDataDirectory(dir);
        configuration.setDestroyNodeOnExit(true);
        configuration.setClientPort(port);
        configuration.setHostname("127.0.0.1");
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("windows")) {
            System.setProperty("os.name", "Windows");
        }
        instance = new EtcdInstance(configuration);
        System.setProperty("os.name", osName);
        instance.start();
        waitForServerInit();
        migrate();
    }

    public void migrate() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource configDirRes = resourceLoader.getResource(ResourceUtils.CLASSPATH_URL_PREFIX + configDir);
        if (configDirRes.exists()) {
            Path configDirPath = configDirRes.getFile().toPath();
            List<Path> configs = Files.walk(configDirPath, FileVisitOption.FOLLOW_LINKS)
                    .filter(p -> !Files.isDirectory(p))
                    .collect(Collectors.toList());
            try (EtcdClient client = new EtcdClient(URI.create("http://127.0.0.1:" + port))) {
                for (Path config : configs) {
                    doMigrate(client, configDirPath, config);
                }
            }
        }
    }

    private void doMigrate(EtcdClient client, Path configDirPath, Path file) throws IOException, TimeoutException, EtcdException, EtcdAuthenticationException {
        Path configDir = file.getParent();
        Path dir = configDir.subpath(configDirPath.getNameCount(), configDir.getNameCount());
        String etcdConfigDir = rootDir + dir.toString();
        etcdConfigDir = StringUtils.replace(etcdConfigDir, "\\", "/");

        try {
            client.getDir(etcdConfigDir)
                    .send().get();
        } catch (EtcdException e) {
            client.putDir(etcdConfigDir)
                    .send().get();
        }

        Properties properties = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file.toFile()));
        Enumeration<Object> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = properties.getProperty(key);
            client.put(String.join("/", etcdConfigDir, key), value)
                    .send().get();
        }
    }


    public synchronized String getConnectString() {
        if (!instance.isRunning()) {
            throw new IllegalStateException("etcd server was not started");
        }
        return "http://127.0.0.1:" + instance.getClientPort();
    }

    private void waitForServerInit() throws IOException {
        final URL versionUrl = new URL(getConnectString() + "/version");
        IOException exc = null;
        for (int i = 0; i < 100; i++) {
            try {
                IOUtils.toString(versionUrl.openStream(), StandardCharsets.UTF_8);
                exc = null;
                break;
            } catch (IOException e) {
                exc = e;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        if (exc != null) {
            throw exc;
        }
    }

    @PreDestroy
    public void stop() {
        logger.info("Start stop etcd server.");
        if (this.instance != null) {
            this.instance.stop();
        }
        logger.info("Stop etcd server successfully.");
    }
}
