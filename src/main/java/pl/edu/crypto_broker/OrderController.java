package pl.edu.crypto_broker;

import io.camunda.zeebe.client.ZeebeClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final ZeebeClient zeebeClient;
    // Mapa przechowująca statusy zakończonych transakcji dla Frontendu
    public static final Map<String, Map<String, Object>> completedTransactions = new ConcurrentHashMap<>();

    public OrderController(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startOrder(@RequestBody OrderRequest request) {
        String tId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();

        variables.put("transactionId", tId);
        variables.put("cryptoId", request.getCryptoId().toLowerCase());
        variables.put("type", request.getType());
        variables.put("targetPrice", request.getTargetPrice());
        variables.put("orderStrategy", request.getOrderStrategy());
        variables.put("amount", request.getAmount());

        // KLUCZOWA POPRAWKA:
        variables.put("clientTier", request.getClientTier());

        variables.put("transactionAmount", request.getAmount() * request.getTargetPrice());

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("crypto-broker-process-v2")
                .latestVersion()
                .variables(variables)
                .send()
                .join();

        return ResponseEntity.ok(Map.of("transactionId", tId));
    }

    // Endpoint, który Frontend odpytuje (Polling)
    @GetMapping("/status/{tId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String tId) {
        if (completedTransactions.containsKey(tId)) {
            return ResponseEntity.ok(completedTransactions.remove(tId));
        }
        return ResponseEntity.ok(Map.of("status", "PENDING"));
    }
}