package pl.edu.crypto_broker;

import io.camunda.zeebe.client.ZeebeClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final ZeebeClient zeebeClient;

    public OrderController(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    @PostMapping("/start")
    public String startOrder(@RequestBody OrderRequest request) {
        Map<String, Object> variables = new HashMap<>();

        // 1. Unikalny klucz korelacji dla instancji procesu
        String tId = UUID.randomUUID().toString();
        variables.put("transactionId", tId);

        // 2. Podstawowe dane zlecenia
        variables.put("cryptoId", request.getCryptoId().toLowerCase());
        variables.put("type", request.getType()); // BUY / SELL
        variables.put("targetPrice", request.getTargetPrice());
        variables.put("orderStrategy", request.getOrderStrategy());
        variables.put("amount", request.getAmount()); // Zachowujemy też samą ilość jednostek

        // 3. KLUCZOWA POPRAWKA DLA DMN: Wyliczamy wartość zlecenia w USD
        // transactionAmount = ilość monet * cena za monetę
        double totalValueUsd = request.getAmount() * request.getTargetPrice();
        variables.put("transactionAmount", totalValueUsd);

        // 4. Obsługa poziomu klienta (Tier)
        String tier = (request.getClientTier() != null && !request.getClientTier().equalsIgnoreCase("None"))
                ? request.getClientTier().toUpperCase()
                : "BRONZE";
        variables.put("clientTier", tier);

        variables.put("expiryDate", request.getExpiryDate());

        // Logowanie dla ułatwienia debugowania w konsoli IntelliJ
        System.out.println(">>> START PROCESU [" + tId + "]");
        System.out.println("    Strategia: " + request.getOrderStrategy());
        System.out.println("    Wartość transakcji ($): " + totalValueUsd);
        System.out.println("    Tier klienta: " + tier);

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("crypto-broker-process-v2")
                .latestVersion()
                .variables(variables)
                .send()
                .join();

        return "Zlecenie przyjęte! Wartość: " + String.format("%.2f", totalValueUsd) + " USD. ID: " + tId;
    }

    @PostMapping("/payment-received")
    public ResponseEntity<String> confirmPayment(@RequestParam String transactionId) {
        System.out.println("Korelacja płatności dla TX: " + transactionId);

        zeebeClient.newPublishMessageCommand()
                .messageName("PaymentReceivedMessage")
                .correlationKey(transactionId)
                .send()
                .join();

        return ResponseEntity.ok("Wysłano sygnał płatności dla: " + transactionId);
    }
}