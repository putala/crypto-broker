package pl.edu.crypto_broker;

public class ProcessConstants {
    // Identyfikator procesu z Twojego diagramu BPMN
    public static final String PROCESS_ID = "crypto-broker-process";

    // Nazwy zmiennych procesowych
    public static final String VAR_CRYPTO_ID = "cryptoId";
    public static final String VAR_PRICE = "price";
    public static final String VAR_TARGET_PRICE = "targetPrice";      // Cena docelowa klienta
    public static final String VAR_TRANSACTION_TYPE = "transactionType"; // BUY lub SELL
    public static final String VAR_EXPIRY_DATE = "expiryDate";        // Data ważności zlecenia
    public static final String VAR_AMOUNT = "amount";                // Wartość/Ilość transakcji
}