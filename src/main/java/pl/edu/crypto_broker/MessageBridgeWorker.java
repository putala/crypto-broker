package pl.edu.crypto_broker;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Map;

@Component
public class MessageBridgeWorker {

    private final ZeebeClient zeebeClient;

    public MessageBridgeWorker(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    // KROK 1: Broker wysyła sygnał startu do Giełdy
    @JobWorker(type = "send-quote-request")
    public void bridgeToExchange(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        System.out.println("BROKER -> GIEŁDA: Wysyłam zapytanie dla TX: " + vars.get("transactionId"));

        zeebeClient.newPublishMessageCommand()
                .messageName("QuoteRequestMessage")
                .correlationKey("") // Puste dla startu nowego procesu
                .variables(vars)
                .timeToLive(Duration.ofMinutes(5)) // Wiadomość poczeka, jeśli giełda "zaspi"
                .send()
                .join();
    }

    // KROK 2: Logika Giełdy (symulacja)
    @JobWorker(type = "generate-exchange-data")
    public void processOnExchange(final JobClient client, final ActivatedJob job) {
        System.out.println("GIEŁDA: Generuję dane dla TX: " + job.getVariablesAsMap().get("transactionId"));

        // Dodajemy spread do procesu
        client.newCompleteCommand(job.getKey())
                .variable("currentSpread", 0.02)
                .send()
                .join();
    }

    // KROK 3: Giełda odsyła sygnał powrotny do Brokera
    @JobWorker(type = "send-quote-response")
    public void bridgeToBroker(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String tId = String.valueOf(vars.get("transactionId"));

        System.out.println("GIEŁDA -> BROKER: Odsyłam odpowiedź dla TX: " + tId);

        zeebeClient.newPublishMessageCommand()
                .messageName("QuoteResponseMessage")
                .correlationKey(tId) // Tu musi być tId, bo Broker już na to czeka
                .variables(vars)
                .send()
                .join();
    }
}