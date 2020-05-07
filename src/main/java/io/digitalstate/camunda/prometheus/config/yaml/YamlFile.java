package io.digitalstate.camunda.prometheus.config.yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlFile {

    private List<SystemMetricsConfig> system = new ArrayList<>();
    private List<CustomMetricsConfig> custom = new ArrayList<>();
    private Map<String, DurationTrackingConfig> durationTracking = new HashMap<>();

    public List<SystemMetricsConfig> getSystem() {
        return system;
    }

    public void setSystem(List<SystemMetricsConfig> system) {
        this.system = system;
    }

    public List<CustomMetricsConfig> getCustom() {
        return custom;
    }

    public void setCustom(List<CustomMetricsConfig> custom) {
        this.custom = custom;
    }

    public Map<String, DurationTrackingConfig> getDurationTracking() {
        return durationTracking;
    }

    public void setDurationTracking(Map<String, DurationTrackingConfig> durationTracking) {
        this.durationTracking = durationTracking;
    }

    @Override
    public String toString() {
        return "YamlFile{" +
            "system=" + system +
            ", custom=" + custom +
            ", durationTracking=" + durationTracking +
            '}';
    }
}
