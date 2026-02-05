package pl.edu.crypto_broker;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.File;
import java.util.Map;

@Component
public class NowyPdfWorker {

    @JobWorker(type = "generate-pdf-new")
    public void handle(final JobClient client, final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String tId = variables.getOrDefault("transactionId", "unknown").toString();

        String fileName = "raport_" + tId + ".pdf";

        try {
            double amount = Double.parseDouble(variables.getOrDefault("amount", "0").toString());
            double targetPrice = Double.parseDouble(variables.getOrDefault("targetPrice", "0").toString());
            double transactionAmount = Double.parseDouble(variables.getOrDefault("transactionAmount", "0").toString());
            double commissionRate = Double.parseDouble(variables.getOrDefault("commissionRate", "0").toString());

            double commissionValue = transactionAmount * commissionRate;
            double totalCost = transactionAmount + commissionValue;

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            // Nagłówek
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("POTWIERDZENIE TRANSAKCJI", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Tabela
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            // Wiersze
            addRow(table, "ID Transakcji:", tId);
            addRow(table, "Kryptowaluta:", variables.getOrDefault("cryptoId", "N/A").toString().toUpperCase());

            // POPRAWKA: Pobieranie transactionType zamiast type
            addRow(table, "Typ operacji:", variables.getOrDefault("transactionType", "N/A").toString());

            // Opcjonalnie: Dodanie informacji o strategii (PKC/LIMIT)
            addRow(table, "Rodzaj zlecenia:", variables.getOrDefault("orderStrategy", "N/A").toString());

            addRow(table, "Ilosc:", String.format("%.4f", amount));
            addRow(table, "Cena egzekucji:", String.format("$%.2f", targetPrice));
            addRow(table, "Wartosc bazowa:", String.format("$%.2f", transactionAmount));

            // POPRAWKA: Wyświetlanie precyzyjnego procentu prowizji (np. 0.5% zamiast 0%)
            String commissionLabel = String.format("Prowizja (%.1f%%):", commissionRate * 100);
            addRow(table, commissionLabel, String.format("$%.2f", commissionValue));

            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            table.addCell(new Phrase("SUMA KONCOWA (USD):", boldFont));
            table.addCell(new Phrase(String.format("$%.2f", totalCost), boldFont));

            document.add(table);

            Paragraph footer = new Paragraph("\nData wygenerowania: " + new java.util.Date());
            footer.setFont(FontFactory.getFont(FontFactory.HELVETICA, 10));
            document.add(footer);

            document.close();

            // Przekazanie danych do kontrolera (musimy dodać status SUCCESS dla frontendu)
            variables.put("commission", commissionValue);
            variables.put("totalCost", totalCost);
            variables.put("status", "SUCCESS"); // To odblokuje frontend
            OrderController.completedTransactions.put(tId, variables);

            client.newCompleteCommand(job.getKey()).send().join();
            System.out.println(">>>> [SUCCESS] Wygenerowano unikalny raport: " + fileName);

        } catch (Exception e) {
            System.err.println(">>>> [ERROR] Błąd generowania PDF: " + e.getMessage());
            client.newFailCommand(job.getKey()).retries(0).send();
        }
    }

    private void addRow(PdfPTable table, String label, String value) {
        table.addCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        table.addCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 12)));
    }
}