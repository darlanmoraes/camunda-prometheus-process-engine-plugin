package io.digitalstate.camunda.prometheus;

import io.digitalstate.camunda.grafana.annotations.reporters.DeploymentReporterParseListener;
import io.digitalstate.camunda.prometheus.collectors.camunda.CamundaMetrics;
import io.digitalstate.camunda.prometheus.collectors.custom.CamundaCustomMetrics;
import io.digitalstate.camunda.prometheus.config.YamlConfig;
import io.digitalstate.camunda.prometheus.parselisteners.BpmnDurationTrackingParseListener;
import io.prometheus.client.CollectorRegistry;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.metrics.reporter.DbMetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class PrometheusProcessEnginePlugin extends AbstractProcessEnginePlugin {

    private static YamlConfig yamlConfig;

    /**
     * The Port that the Prometheus Client HTTP Server will be exposed on.
     * Promethus.yml configuration on the Prometheus server can then be set up with a job such as
     * ```yml
     *   - job_name: 'camunda'
     *     scrape_interval: 10s
     *     honor_labels: true
     *     static_configs:
     *     - targets: ['camundaUrl:9999']
     * ```
     * Where the above configuration is scraping the data from port 9999 every 10 seconds.
     * Take note to have a sensible scape interval in your prometheus configuration that aligns with your
     * Polling Frequency and the Camunda Reporting reporting interval
     * Default Configuration is port 9999.
     */
    private Integer port = 9999;

    /**
     * The Interval for Camunda Reporting Metrics to execute on the DB.
     * This is overriding the default 15min interval in the Camunda Engine.
     * This plugin defaults to a 900 second interval, which is 15 minutes.
     */
    private String camundaReportingIntervalInSeconds = "900";

    /**
     * YAML file path for Collector description/configuration.
     */
    private String collectorYmlFilePath;

    /**
     * Boolean as string (true/false) to indicate whether to activate the BPMN Duration Parse Listener.
     */
    private String bpmnDurationParseListener;

    /**
     * Boolean as string (true/false) to indicate whether to activate the Grafana Annotation Reporting.
     */
    private String grafanaAnnotationReporting = "false";

    /**
     * String of Grafana server URL.  Defaults to http://localhost:3000
     */
    private String grafanaServer = "http://localhost:3000";

    /**
     * Grafana Bearer Token file Path.  Points to a File System Path which containts the token.
     * "Bearer " will be pre-pended to the Token for you.
     * Note that Auth Tokens are enabled by Default on Grafana.  So usually you will need to generate a Token with Editor Rights.
     */
    private String grafanaAuthTokenPath = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusProcessEnginePlugin.class);
    final private CollectorRegistry registry = CollectorRegistry.defaultRegistry;

    @Override
    public void preInit(final ProcessEngineConfigurationImpl engine) {

        try {
            // Get YAML Config File
            yamlConfig = new YamlConfig(getCollectorYmlFilePath());

            // Starts up the Prometheus Client HTTP Server
            new MetricsExporter(this.getPort());

            // Initialize Custom Parse Listeners
            List<BpmnParseListener> parseListeners = engine.getCustomPreBPMNParseListeners();
            if (parseListeners == null) {
                parseListeners = new ArrayList<>();
                engine.setCustomPreBPMNParseListeners(parseListeners);
            }

            // Add BPMN Parse Listener for Duration Tracking if it is set to True
            if (Boolean.parseBoolean(bpmnDurationParseListener)) {
                // Add BPMN  Duration Tracking Parse Listener
                parseListeners.add(new BpmnDurationTrackingParseListener());
                LOGGER.info("Prometheus Bpmn Duration Parse Listener is Active");

            } else {
                LOGGER.info("Prometheus Bpmn Duration Parse Listener is Disabled");
            }

            // Add Grafana Annotation Reporting Parse Listener if set to True
            if (Boolean.parseBoolean(getGrafanaAnnotationReporting())) {
                try {
                    URI grafanaServer = new URI(getGrafanaServer());

                    InputStream grafanaToken = new FileSystemResource(getGrafanaAuthTokenPath()).getInputStream();
                    String grafanaAuthToken = FileCopyUtils.copyToString(new InputStreamReader(grafanaToken));

                    parseListeners.add(new DeploymentReporterParseListener(grafanaServer, grafanaAuthToken));
                    LOGGER.info("Grafana Deployment Annotation Reporter Parse Listener is Active");
                    LOGGER.info("Grafana Annotation Server URL is set to: {}", grafanaServer.toURL().toString());

                } catch (URISyntaxException e) {
                    LOGGER.error("Could not start Grafana Annotation Reporting due to URI error", e);
                } catch (IOException e) {
                    LOGGER.error("Could not start Grafana Annotation Reporting due to IO error", e);
                }
            } else {
                LOGGER.info("Grafana Deployment Annotation Reporter Parse Listener is Disabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl engine) {
        // Overrides the default Metrics reporter with a new instance that has a customized Reporting interval.
        // This allows the override of the built in 15min interval
        LOGGER.info("DbMetricsReporter is being started with Interval of: " + getCamundaReportingIntervalInSeconds() + " seconds.");

        DbMetricsReporter metricsReporter = new DbMetricsReporter(engine.getMetricsRegistry(),
                engine.getCommandExecutorTxRequired());

        metricsReporter.setReportingIntervalInSeconds(Long.parseLong(getCamundaReportingIntervalInSeconds()));
        engine.setDbMetricsReporter(metricsReporter);
    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {
        // Starts Prometheus reporting for the built in Camunda Metrics system.
        new CamundaMetrics(yamlConfig.getSystem(), processEngine);

        // Starts Prometheus reporting for Custom defined Metrics.
        new CamundaCustomMetrics(yamlConfig.getCustom(), processEngine);
    }

    public static YamlConfig getYamlConfig() {
        return yamlConfig;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getCamundaReportingIntervalInSeconds() {
        return camundaReportingIntervalInSeconds;
    }

    public void setCamundaReportingIntervalInSeconds(String camundaReportingIntervalInSeconds) {
        this.camundaReportingIntervalInSeconds = camundaReportingIntervalInSeconds;
    }

    public String getCollectorYmlFilePath() {
        return collectorYmlFilePath;
    }

    public void setCollectorYmlFilePath(String collectorYmlFilePath) {
        this.collectorYmlFilePath = collectorYmlFilePath;
    }

    public String getBpmnDurationParseListener() {
        return bpmnDurationParseListener;
    }

    public void setBpmnDurationParseListener(String bpmnDurationParseListener) {
        this.bpmnDurationParseListener = bpmnDurationParseListener;
    }

    public String getGrafanaAnnotationReporting() {
        return grafanaAnnotationReporting;
    }

    public void setGrafanaAnnotationReporting(String grafanaAnnotationReporting) {
        this.grafanaAnnotationReporting = grafanaAnnotationReporting;
    }

    public String getGrafanaServer() {
        return grafanaServer;
    }

    public void setGrafanaServer(String grafanaServer) {
        this.grafanaServer = grafanaServer;
    }

    public String getGrafanaAuthTokenPath() {
        return grafanaAuthTokenPath;
    }

    public void setGrafanaAuthTokenPath(String grafanaAuthTokenPath) {
        this.grafanaAuthTokenPath = grafanaAuthTokenPath;
    }

    public CollectorRegistry getRegistry() {
        return registry;
    }
}