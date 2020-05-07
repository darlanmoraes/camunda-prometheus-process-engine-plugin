package io.digitalstate.camunda.prometheus;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.spring.boot.starter.configuration.Ordering;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Order(Ordering.DEFAULT_ORDER + 1)
@ConditionalOnProperty(prefix = "spring", name = "prometheus.enabled", havingValue = "true")
public class PrometheusConfiguration extends AbstractCamundaConfiguration {

    @Value("${spring.prometheus.port:9999}")
    private Integer port;
    @Value("${spring.prometheus.grafana-server:http://localhost:3000}")
    private String grafanaServer;
    @Value("${spring.prometheus.reporting-interval:5}")
    private String reportingInterval;
    @Value("${spring.prometheus.collector-yml:classpath*:prometheus-metrics.yml}")
    private String collectorYml;

    @Bean
    public PrometheusProcessEnginePlugin prometheusPlugin() {
        final PrometheusProcessEnginePlugin plugin = new PrometheusProcessEnginePlugin();
        plugin.setPort(port);
        plugin.setCamundaReportingIntervalInSeconds(reportingInterval);
        plugin.setGrafanaServer(grafanaServer);
        plugin.setCollectorYmlFilePath(collectorYml);
        return plugin;
    }

    @Override
    public void postInit(final ProcessEngineConfigurationImpl engine) {
        final List<ProcessEnginePlugin> plugins = new ArrayList<>();
        plugins.add(this.prometheusPlugin());
        if (engine.getProcessEnginePlugins() != null) {
            plugins.addAll(engine.getProcessEnginePlugins());
        }
        engine.setProcessEnginePlugins(plugins);
    }

}
