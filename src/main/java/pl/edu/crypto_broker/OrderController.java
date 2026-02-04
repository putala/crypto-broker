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

        // 1. Standaryzacja ID kryptowaluty
        String standardizedCryptoId = (request.getCryptoId() != null ? request.getCryptoId() : "bitcoin").toLowerCase();

        // 2. Mapowanie zmiennych zgodnie z ProcessConstants i tabelami DMN
        variables.put(ProcessConstants.VAR_CRYPTO_ID, standardizedCryptoId);
        variables.put(ProcessConstants.VAR_TRANSACTION_TYPE, request.getType()); // BUY / SELL
        variables.put(ProcessConstants.VAR_TARGET_PRICE, request.getTargetPrice());

        // KLUCZOWA POPRAWKA: Upewnij się, że ProcessConstants.VAR_AMOUNT to "transactionAmount"
        // Twoja tabela DMN (image_e06e5b.png) szuka właśnie tej nazwy.
        variables.put(ProcessConstants.VAR_AMOUNT, request.getAmount());

        // 3. DODANO: Poziom klienta - Tabela ryzyka wymaga tej zmiennej!
        // Jeśli nie masz jej w OrderRequest, ustawiamy domyślnie "Bronze"
        variables.put("clientTier", "Bronze");

        variables.put(ProcessConstants.VAR_EXPIRY_DATE, request.getExpiryDate());

        System.out.println("Uruchamiam proces dla danych: " + variables);

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