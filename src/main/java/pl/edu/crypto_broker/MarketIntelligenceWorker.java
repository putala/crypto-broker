package pl.edu.crypto_broker;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;
import java.util.Random;

@Component
public class MarketIntelligenceWorker {

    private final WebClient webClient = WebClient.create("https://api.coingecko.com/api/v3");
    private final Random random = new Random();

    @JobWorker(type = "fetch-order-book")
    public Map<String, Object> handle(@Variable(name = "cryptoId") String cryptoId) {
        String searchId = (cryptoId == null || cryptoId.trim().isEmpty()) ? "bitcoin" : cryptoId.toLowerCase();

        System.out.println("\n====================================================");
        System.out.println("STACJA ANALITYCZNA: Rozpoczynam badanie arkusza...");
        System.out.println(">>> Instrument: " + searchId.toUpperCase());

        try {
            // Pobranie danych z API
            Map response = webClient.get()
                    .uri("/simple/price?ids=" + searchId + "&vs_currencies=usd")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            Double price = 0.0;
            if (response != null && response.containsKey(searchId)) {
                Map data = (Map) response.get(searchId);
                if (data != null && data.containsKey("usd")) {
                    price = Double.valueOf(data.get("usd").toString());
                }
            }

            Double marketSpread = 0.5 + (random.nextDouble() * 2.0);

            System.out.println(">>> Odczyt ceny: $" + price);
            System.out.println(">>> Wyliczony spread: " + String.format("%.2f", marketSpread) + "%");

            // --- USUNIĘTO MECHANIZM OCHRONY API (Thread.sleep) ---

            System.out.println(">>> STATUS: Przekazuję dane do tabeli DMN (Analiza Spreadu)");
            System.out.println("====================================================\n");

            return Map.of(
                    "price", price,
                    "marketSpread", marketSpread
            );

        } catch (Exception e) {
            System.err.println("!!! BŁĄD ANALIZY: " + e.getMessage());
            System.err.println("!!! STATUS: Wymuszam tryb bezpieczeństwa (Spread 5.0%)");
            return Map.of("price", 0.0, "marketSpread", 5.0);
        }
    }
}