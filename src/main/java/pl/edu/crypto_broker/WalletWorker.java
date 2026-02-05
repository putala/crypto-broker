package pl.edu.crypto_broker;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;

@Component
public class WalletWorker {

    @JobWorker(type = "execute-exchange-order")
    public Map<String, Object> executeOrder(@Variable String cryptoId, @Variable Double targetPrice) {
        System.out.println(">>>> [GIEŁDA] Egzekucja: " + cryptoId + " @ $" + targetPrice);
        return Map.of("exchangeConfirmed", true);
    }

    @JobWorker(type = "check-wallet-balance")
    public void updateWallet(
            @Variable String transactionId,
            @Variable String cryptoId,
            @Variable String type,
            @Variable Double amount,
            @Variable Double targetPrice,
            @Variable String clientTier,
            @Variable Double commissionRate) {

        System.out.println("\n========================================");
        System.out.println(">>>> [DEBUG] FINALIZACJA TRANSAKCJI");
        System.out.println("     - ID: " + transactionId);
        System.out.println("     - Klient (Tier): " + clientTier);
        System.out.println("     - Stawka DMN: " + (commissionRate != null ? (commissionRate * 100) + "%" : "BŁĄD (null -> 5%)"));

        double baseValue = amount * targetPrice;
        double finalRate = (commissionRate != null) ? commissionRate : 0.05;
        double commissionValue = baseValue * finalRate;

        // Obliczenie finalnego kosztu (BUY dodaje prowizję, SELL odejmuje)
        double totalCost = ("BUY".equals(type)) ? (baseValue + commissionValue) : (baseValue - commissionValue);

        System.out.println(String.format("     - Wartość bazowa: $%.2f", baseValue));
        System.out.println(String.format("     - Kwota prowizji: $%.2f", commissionValue));
        System.out.println(String.format("     - Suma końcowa:   $%.2f", totalCost));
        System.out.println("========================================\n");

        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("cryptoId", cryptoId);
        result.put("type", type);
        result.put("amount", amount);
        result.put("clientTier", clientTier);
        result.put("commission", commissionValue);
        result.put("commissionRate", finalRate);
        result.put("totalCost", totalCost);

        OrderController.completedTransactions.put(transactionId, result);
    }

    @JobWorker(type = "generate-transaction-pdf")
    public void generateReport() {
        System.out.println(">>>> [RAPORT] PDF wygenerowany.");
    }
}