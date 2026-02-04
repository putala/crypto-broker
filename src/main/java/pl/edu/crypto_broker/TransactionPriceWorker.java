package pl.edu.crypto_broker;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Component
public class TransactionPriceWorker {

    private final WebClient webClient = WebClient.create("https://api.coingecko.com/api/v3");

    @JobWorker(type = "fetch-crypto-price")
    public void handle(final JobClient client, final ActivatedJob job, @Variable(name = "cryptoId") String cryptoId) {
        String searchId = (cryptoId != null) ? cryptoId.toLowerCase() : "bitcoin";

        System.out.println("\n----------------------------------------------------");
        System.out.println("MODUŁ TRANSAKCYJNY: Pobieranie kursu...");

        try {
            // 1. OBOWIĄZKOWE OPOŹNIENIE DLA PĘTLI BPMN
            // Bez tego pętla "Ponowić zlecenie?" zabije każde API
            Thread.sleep(10000);

            Map response = webClient.get()
                    .uri("/simple/price?ids=" + searchId + "&vs_currencies=usd")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey(searchId)) {
                Map data = (Map) response.get(searchId);
                Double currentPrice = Double.valueOf(data.get("usd").toString());

                System.out.println(">>> Cena pobrana: $" + currentPrice);

                // KOŃCZYMY SUKCESEM - tylko gdy mamy realną cenę
                client.newCompleteCommand(job.getKey())
                        .variables(Map.of("price", currentPrice))
                        .send()
                        .join();
            } else {
                throw new RuntimeException("Pusta odpowiedź z API");
            }

        } catch (Exception e) {
            System.err.println("!!! WYKRYTO PROBLEM Z API: " + e.getMessage());

            // 2. REAKCJA NA BŁĄD API (ZGODNIE Z DIAGRAMEM image_fdfdc1.png)
            // Wysyłamy BPMN Error "Błąd API", który aktywuje przerywane zdarzenie brzegowe (Boundary Event)
            // Kod błędu musi być taki sam jak w Modelerze (np. "API_ERROR")
            client.newThrowErrorCommand(job.getKey())
                    .errorCode("API_ERROR")
                    .errorMessage("Przekroczono limity CoinGecko lub brak połączenia")
                    .send()
                    .join();
        }
    }
}