package pl.edu.crypto_broker;

import lombok.Data;

@Data
public class OrderRequest {
    private String cryptoId;
    private String type;         // BUY / SELL
    private String orderStrategy; // DODANO
    private String clientTier;    // DODANO
    private Double targetPrice;
    private Double amount;
    private String startDate; // DODAJ TO POLE
    private String expiryDate;   // Format ISO 8601
}