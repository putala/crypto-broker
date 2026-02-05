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

        // 1. Unikalny klucz korelacji - niezbędny do poprawnego działania wiadomości!
        String tId = UUID.randomUUID().toString();
        variables.put("transactionId", tId);

        // 2. Mapowanie danych z formularza HTML
        variables.put("cryptoId", request.getCryptoId().toLowerCase());
        variables.put("type", request.getType()); // BUY / SELL
        variables.put("targetPrice", request.getTargetPrice());

        // 3. TO ROZWIĄZUJE PROBLEM BRAMKI: Przekazujemy strategię (PKC/LIMIT/PROGRES)
        // Camunda teraz zobaczy wartość na bramce XOR
        variables.put("orderStrategy", request.getOrderStrategy());

        // 4. MAPOWANIE DLA DMN: Tabela (image_c24b80.png) wymaga "transactionAmount" i "clientTier"
        variables.put("transactionAmount", request.getAmount());

        // Ważne: Zmieniamy na DUŻE LITERKI, aby pasowało do Twojego formularza HTML
        String tier = (request.getClientTier() != null) ? request.getClientTier().toUpperCase() : "BRONZE";
        variables.put("clientTier", tier);

        variables.put("expiryDate", request.getExpiryDate());

        System.out.println(">>> START PROCESU [" + tId + "] Strategia: " + request.getOrderStrategy());

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("crypto-broker-process-v2") // Nazwa z Twojego schematu
                .latestVersion()
                .variables(variables)
                .send()
                .join();

        return "Zlecenie " + request.getOrderStrategy() + " przyjęte! ID: " + tId;
    }

    @PostMapping("/payment-received")
    public ResponseEntity<String> confirmPayment(@RequestParam String transactionId) {
        // Używamy transactionId jako klucza korelacji zamiast nazwy krypto
        System.out.println("Korelacja płatności dla TX: " + transactionId);

        zeebeClient.newPublishMessageCommand()
                .messageName("PaymentReceivedMessage")
                .correlationKey(transactionId)
                .send()
                .join();

        return ResponseEntity.ok("Wysłano sygnał płatności dla: " + transactionId);
    }
}