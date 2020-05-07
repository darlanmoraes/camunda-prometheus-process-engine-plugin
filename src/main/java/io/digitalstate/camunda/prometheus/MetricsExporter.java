package io.digitalstate.camunda.prometheus;

import io.prometheus.client.exporter.HTTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MetricsExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsExporter.class);
    private final HTTPServer server;

    /**
     * Starts HTTPServer for Prometheus on the defined port.
     * @param port The Port to expose the HTTP Server that Prometheus will connect to.
     */
    public MetricsExporter(int port) throws IOException {
        LOGGER.info("Starting server for Prometheus metrics on port: " + port);
        this.server = new HTTPServer(port);
    }

    public void stop() {
        this.server.stop();
    }

}
