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
            @Variable Double targetPrice) {

        System.out.println(">>>> [CAMUNDA] Finał procesu dla: " + transactionId);

        // Wysyłamy sygnał do kontrolera, który odbierze Frontend
        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("cryptoId", cryptoId);
        result.put("type", type);
        result.put("amount", amount);
        result.put("totalCost", amount * targetPrice);

        OrderController.completedTransactions.put(transactionId, result);
    }

    @JobWorker(type = "generate-transaction-pdf")
    public void generateReport() {
        System.out.println(">>>> [RAPORT] PDF wygenerowany.");
    }
}