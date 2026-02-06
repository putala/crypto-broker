package pl.edu.crypto_broker;

import lombok.Data;

@Data
public class OrderRequest {
    private String cryptoId;
    private String type;         // BUY / SELL
    private String orderStrategy;
    private String clientTier;
    private Double targetPrice;
    private Double amount;
    private String startDate;
    private String expiryDate;
}