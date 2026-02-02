package pl.edu.crypto_broker;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.VariablesAsType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Component
public class CryptoPriceWorker {

    private final WebClient webClient = WebClient.create("https://api.coingecko.com/api/v3");

    @JobWorker(type = "fetch-crypto-price")
    public Map<String, Object> handle(@VariablesAsType Map<String, Object> variables) {
        String cryptoId = (String) variables.get(ProcessConstants.VAR_CRYPTO_ID);

        System.out.println("Pobieram cenę dla: " + cryptoId);

        // Wywołanie zewnętrznego API
        Map response = webClient.get()
                .uri("/simple/price?ids=" + cryptoId + "&vs_currencies=usd")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // Wyciąganie ceny z JSONa (struktura: { "bitcoin": { "usd": 50000 } })
        Map cryptoData = (Map) response.get(cryptoId);
        Double price = Double.valueOf(cryptoData.get("usd").toString());

        System.out.println("Aktualna cena " + cryptoId + " to: " + price + " USD");

        // Ta linijka wysyła zmienną 'price' z powrotem do Camundy
        return Map.of("price", price);
    }
}