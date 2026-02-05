package pl.edu.crypto_broker;

import io.camunda.zeebe.client.ZeebeClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final ZeebeClient zeebeClient;
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

    @GetMapping("/status/{tId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String tId) {
        if (completedTransactions.containsKey(tId)) {
            return ResponseEntity.ok(completedTransactions.get(tId));
        }
        return ResponseEntity.ok(Map.of("status", "PENDING"));
    }

    // POPRAWKA: Dynamiczne pobieranie raportu na podstawie ID transakcji
    @GetMapping("/download-pdf/{tId}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String tId) {
        String fileName = "raport_" + tId + ".pdf";
        File file = new File(fileName);

        if (!file.exists()) {
            System.err.println(">>>> [BŁĄD] Brak pliku raportu dla ID: " + tId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(new FileSystemResource(file));
    }
}