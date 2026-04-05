package com.piloto.cdi.gateway.governance.evolution.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Type-safe configuration for the Cognitive Evolution Engine.
 * Bound to piloto.evolution.* in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "piloto.evolution")
public class EvolutionProperties {

    private int minSamplesToChallenge = 10;
    private double epsilonGreedy = 0.15;
    private Optimizer optimizer = new Optimizer();

    public int getMinSamplesToChallenge() { return minSamplesToChallenge; }
    public void setMinSamplesToChallenge(int v) { this.minSamplesToChallenge = v; }

    public double getEpsilonGreedy() { return epsilonGreedy; }
    public void setEpsilonGreedy(double v) { this.epsilonGreedy = v; }

    public Optimizer getOptimizer() { return optimizer; }
    public void setOptimizer(Optimizer optimizer) { this.optimizer = optimizer; }

    public static class Optimizer {
        private String cron = "0 0 3 */2 * *";
        private int minVariants = 3;
        private int minSamplesPerVariant = 10;

        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }

        public int getMinVariants() { return minVariants; }
        public void setMinVariants(int v) { this.minVariants = v; }

        public int getMinSamplesPerVariant() { return minSamplesPerVariant; }
        public void setMinSamplesPerVariant(int v) { this.minSamplesPerVariant = v; }
    }
}
