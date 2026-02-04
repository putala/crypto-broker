package pl.edu.crypto_broker;

import io.camunda.zeebe.client.ZeebeClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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

        // 1. Standaryzacja ID kryptowaluty (np. "bitcoin")
        String standardizedCryptoId = (request.getCryptoId() != null ? request.getCryptoId() : "bitcoin").toLowerCase();

        // 2. Mapowanie zmiennych - UŻYWAMY BEZPOŚREDNICH NAZW Z BPMN
        // To rozwiąże błąd "Expected at least one condition to evaluate to true"
        variables.put("cryptoId", standardizedCryptoId);
        variables.put("transactionType", request.getType()); // Musi być "BUY" lub "SELL"
        variables.put("targetPrice", request.getTargetPrice());
        variables.put("orderStrategy", request.getOrderStrategy()); // LIMIT / PROGRES / PKC
        variables.put("clientTier", request.getClientTier() != null ? request.getClientTier() : "None");
        variables.put("transactionAmount", request.getAmount());
        variables.put("expiryDate", request.getExpiryDate());

        // Dodatkowo przesyłamy startDate dla logiki progresji
        variables.put("startDate", request.getStartDate());

        System.out.println("Uruchamiam proces dla danych: " + variables);

        // Upewnij się, że ProcessConstants.PROCESS_ID odpowiada ID w Modelerze (np. "crypto-process")
        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId(ProcessConstants.PROCESS_ID)
                .latestVersion()
                .variables(variables)
                .send()
                .join();

        return "Zlecenie przyjęte dla " + standardizedCryptoId + "!";
    }

    @PostMapping("/payment-received")
    public ResponseEntity<String> confirmPayment(@RequestParam String cryptoId) {
        String correlationKey = cryptoId.toLowerCase();

        System.out.println("Próba korelacji płatności dla klucza: " + correlationKey);

        zeebeClient.newPublishMessageCommand()
                .messageName("PaymentReceivedMessage")
                .correlationKey(correlationKey)
                .send()
                .join();

        return ResponseEntity.ok("Wysłano sygnał płatności dla: " + correlationKey);
    }
}