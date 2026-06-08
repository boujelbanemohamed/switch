package com.switchplatform.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "transfer")
public class TransferConfig {

    private FeeConfig a2a = new FeeConfig();
    private FeeConfig p2p = new FeeConfig();

    public FeeConfig getA2a() { return a2a; }
    public void setA2a(FeeConfig a2a) { this.a2a = a2a; }
    public FeeConfig getP2p() { return p2p; }
    public void setP2p(FeeConfig p2p) { this.p2p = p2p; }

    public static class FeeConfig {
        private BigDecimal fixed = BigDecimal.ZERO;
        private BigDecimal percent = BigDecimal.ZERO;
        private String currency = "TND";
        private String description = "";

        public BigDecimal getFixed() { return fixed; }
        public void setFixed(BigDecimal fixed) { this.fixed = fixed; }
        public BigDecimal getPercent() { return percent; }
        public void setPercent(BigDecimal percent) { this.percent = percent; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
