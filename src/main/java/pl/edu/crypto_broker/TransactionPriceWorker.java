package pl.edu.crypto_broker;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;
import java.util.HashMap;

@Component
public class TransactionPriceWorker {

    private final WebClient webClient = WebClient.create("https://api.coingecko.com/api/v3");

    @JobWorker(type = "fetch-crypto-price")
    public void handle(final JobClient client, final ActivatedJob job,
                       @Variable(name = "cryptoId") String cryptoId,
                       @Variable(name = "orderStrategy") String strategy,
                       @Variable(name = "targetPrice") Double targetPrice) {

        String searchId = (cryptoId != null) ? cryptoId.toLowerCase() : "bitcoin";
        Map<String, Object> allVariables = job.getVariablesAsMap();

        System.out.println("\n----------------------------------------------------");
        System.out.println("MODUŁ TRANSAKCYJNY: Analiza kursu dla " + searchId.toUpperCase());

        try {
            Map response = webClient.get()
                    .uri("/simple/price?ids=" + searchId + "&vs_currencies=usd")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey(searchId)) {
                Map data = (Map) response.get(searchId);
                Double marketPrice = Double.valueOf(data.get("usd").toString());
                Double offeredPrice = (targetPrice != null) ? targetPrice : 0.0;

                // LOGIKA PROGRESYWNA - Z WARTOŚCIĄ STAŁĄ W KODZIE
                if ("PROGRES".equals(strategy)) {
                    Object stepObj = allVariables.get("priceStep");
                    Double priceStep;

                    // PRÓBA POBRANIA Z CAMUNDY, A JEŚLI BRAK -> PRZYPISANIE 1000.0
                    if (stepObj instanceof Number) {
                        priceStep = ((Number) stepObj).doubleValue();
                        System.out.println(">>> KROK CENOWY (pobrany z Camundy): +$" + priceStep);
                    } else {
                        priceStep = 1000.0; // Wartość przypisana na stałe
                        System.out.println(">>> KROK CENOWY (stały w kodzie): +$" + priceStep);
                    }

                    offeredPrice = offeredPrice + priceStep;
                    System.out.println(">>> STRATEGIA: PROGRESYWNA");
                }
                else if ("PKC".equals(strategy)) {
                    offeredPrice = marketPrice;
                    System.out.println(">>> STRATEGIA: PKC (Rynek)");
                }
                else {
                    System.out.println(">>> STRATEGIA: LIMIT");
                }

                System.out.println(">>> CENA RYNKOWA:  $" + marketPrice);
                System.out.println(">>> CENA OFEROWANA: $" + offeredPrice);

                Map<String, Object> outputVars = new HashMap<>();
                outputVars.put("price", marketPrice);
                outputVars.put("offeredPrice", offeredPrice);

                client.newCompleteCommand(job.getKey())
                        .variables(outputVars)
                        .send()
                        .join();
            }

        } catch (Exception e) {
            System.err.println("!!! PROBLEM Z API: " + e.getMessage());
            throw new RuntimeException("API tymczasowo niedostępne - czekam na retry...");
        }
    }
}