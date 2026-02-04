package pl.edu.crypto_broker;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
public class WalletWorker {

    // 1. Obsługa kafelka: "Wysyłka zlecenia do giełdy"
    @JobWorker(type = "execute-exchange-order")
    public Map<String, Object> executeOrder(
            @Variable String cryptoId,
            @Variable Double price) {

        System.out.println("\n>>>> [GIEŁDA] Wysyłanie zlecenia egzekucyjnego...");
        System.out.println(">>>> [OPERACJA] " + cryptoId.toUpperCase() + " po cenie rynkowej: $" + price);

        // Generujemy unikalny ID transakcji, który przyda się w raporcie
        String transactionId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println(">>>> [POTWIERDZENIE] Giełda przyjęła zlecenie. ID: " + transactionId);

        return Map.of("transactionId", transactionId);
    }

    // 2. Obsługa kafelka: "Aktualizacja portfela klienta"
    @JobWorker(type = "check-wallet-balance")
    public Map<String, Object> updateWallet(@Variable Double transactionAmount) {
        System.out.println("\n>>>> [PORTFEL] Rozpoczynam księgowanie transakcji...");
        System.out.println(">>>> [SALDO] Pobrano kwotę: $" + transactionAmount);

        return Map.of("walletUpdated", true);
    }

    // 3. Obsługa kafelka: "Generuj raport PDF"
    @JobWorker(type = "generate-transaction-pdf")
    public void generateReport(@Variable String transactionId, @Variable String cryptoId) {
        System.out.println("\n>>>> [RAPORT] System generuje plik potwierdzenia PDF...");
        System.out.println(">>>> [INFO] Dokument gotowy dla transakcji: " + transactionId);
        System.out.println(">>>> [INFO] Aktywo: " + cryptoId.toUpperCase());
        System.out.println("\n****************************************************");
        System.out.println("   SUKCES: PROCES TRANSAKCYJNY DOBIEGŁ KOŃCA!");
        System.out.println("****************************************************\n");
    }
}