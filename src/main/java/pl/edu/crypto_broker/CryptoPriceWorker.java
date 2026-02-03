package pl.edu.crypto_broker;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Component
public class CryptoPriceWorker {

    private final WebClient webClient = WebClient.create("https://api.coingecko.com/api/v3");

    @JobWorker(type = "fetch-crypto-price")
    // Używamy adnotacji @Variable, aby bezpośrednio wyciągnąć cryptoId z procesu
    public Map<String, Object> handle(@Variable(name = "cryptoId") String cryptoId) {

        // 1. Zabezpieczenie przed null
        if (cryptoId == null || cryptoId.isEmpty()) {
            System.err.println("BŁĄD: Zmienna cryptoId jest pusta!");
            return Map.of("price", 0.0);
        }

        String searchId = cryptoId.toLowerCase();
        System.out.println("Pobieram cenę dla: " + searchId);

        try {
            // 2. Wywołanie zewnętrznego API
            Map response = webClient.get()
                    .uri("/simple/price?ids=" + searchId + "&vs_currencies=usd")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // 3. Bezpieczne wyciąganie danych z JSONa
            if (response != null && response.containsKey(searchId)) {
                Map cryptoData = (Map) response.get(searchId);
                if (cryptoData != null && cryptoData.containsKey("usd")) {
                    Double price = Double.valueOf(cryptoData.get("usd").toString());
                    System.out.println("Aktualna cena " + searchId + " to: " + price + " USD");

                    // Zwracamy cenę do procesu
                    return Map.of("price", price);
                }
            }

            System.err.println("API nie zwróciło ceny dla: " + searchId);
            return Map.of("price", 0.0);

        } catch (Exception e) {
            System.err.println("Błąd podczas komunikacji z API: " + e.getMessage());
            // W przypadku błędu zwracamy 0, co może wywołać ścieżkę błędu w BPMN
            return Map.of("price", 0.0);
        }
    }
}