package io.digitalstate.camunda.prometheus.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.digitalstate.camunda.prometheus.config.yaml.DurationTrackingConfig;
import io.digitalstate.camunda.prometheus.config.yaml.CustomMetricsConfig;
import io.digitalstate.camunda.prometheus.config.yaml.SystemMetricsConfig;
import io.digitalstate.camunda.prometheus.config.yaml.YamlFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlConfig.class);

    private final YamlFile yaml;

    public YamlConfig(final String path) throws IOException {
        if ((this.yaml = this.getYaml(path)) == null) {
            throw new IOException("Yaml couldn't be loaded. path=" + path);
        }
        LOGGER.info("Loaded yaml = {}", this.yaml);
    }

    private YamlFile getYaml(final String path) throws IOException {
        final File stream = ResourceUtils.getFile(path);
        return MAPPER.readValue(stream, YamlFile.class);
    }

    public List<CustomMetricsConfig> getCustom() {
        return this.yaml.getCustom();
    }

    public List<SystemMetricsConfig> getSystem() {
        return this.yaml.getSystem();
    }

    public Map<String, DurationTrackingConfig> getDurationTracking() {
        return this.yaml.getDurationTracking();
    }

}
