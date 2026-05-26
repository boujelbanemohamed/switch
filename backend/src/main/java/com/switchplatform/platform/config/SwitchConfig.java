package com.switchplatform.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "switch")
public class SwitchConfig {

    private Iso8583 iso8583 = new Iso8583();
    private Iso20022 iso20022 = new Iso20022();
    private Routing routing = new Routing();
    private Threads threads = new Threads();

    @Bean(name = "switchTaskExecutor")
    public Executor switchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads.corePoolSize);
        executor.setMaxPoolSize(threads.maxPoolSize);
        executor.setQueueCapacity(threads.queueCapacity);
        executor.setThreadNamePrefix("switch-");
        executor.initialize();
        return executor;
    }

    public static class Iso8583 {
        private String header = "ISO015000077";
        private int maxMessageLength = 4096;

        public String getHeader() { return header; }
        public void setHeader(String header) { this.header = header; }
        public int getMaxMessageLength() { return maxMessageLength; }
        public void setMaxMessageLength(int maxMessageLength) { this.maxMessageLength = maxMessageLength; }
    }

    public static class Iso20022 {
        private String namespace = "urn:iso:std:iso:20022:tech:xsd";

        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
    }

    public static class Routing {
        private String rulesFile = "classpath:routing-rules.yml";

        public String getRulesFile() { return rulesFile; }
        public void setRulesFile(String rulesFile) { this.rulesFile = rulesFile; }
    }

    public static class Threads {
        private int corePoolSize = 10;
        private int maxPoolSize = 50;
        private int queueCapacity = 100;

        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    }

    public Iso8583 getIso8583() { return iso8583; }
    public void setIso8583(Iso8583 iso8583) { this.iso8583 = iso8583; }
    public Iso20022 getIso20022() { return iso20022; }
    public void setIso20022(Iso20022 iso20022) { this.iso20022 = iso20022; }
    public Routing getRouting() { return routing; }
    public void setRouting(Routing routing) { this.routing = routing; }
    public Threads getThreads() { return threads; }
    public void setThreads(Threads threads) { this.threads = threads; }
}
