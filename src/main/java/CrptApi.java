import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final String uri = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduled;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.scheduled = Executors.newSingleThreadScheduledExecutor();
    }


    public void createDocument(Document document, String signature) {
        scheduled.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                while (semaphore.tryAcquire()) {
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
                    Gson gson = gsonBuilder.create();
                    String body = gson.toJson(document);

                    sendPost(uri, body, signature);
                }
                semaphore.release(requestLimit);
            }
        }, 0, 1, timeUnit);
    }

    private void sendPost(String uri, String body, String signature) {
        final HttpClient client = HttpClient.newBuilder().build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static class Document {
        private String description;
        private String doc_id;
        private String doc_status;

        private DocType doc_type;

        private Boolean importRequest;

        private String owner_inn;
        private String participant_inn;
        private String producer_inn;

        private LocalDate production_date;

        private String production_type;

        private Products products;

        public Document(String description, String doc_id, String doc_status, DocType doc_type,
                        Boolean importRequest, String owner_inn, String participant_inn,
                        String producer_inn, LocalDate production_date, String production_type, Products products) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
        }
    }

    static class Products {

        private String certificate_document;

        private LocalDate certificate_document_date;

        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;

        private LocalDate production_date;

        private String tnved_code;
        private String uit_code;
        private String uitu_code;

        public Products(String certificate_document, LocalDate certificate_document_date,
                        String certificate_document_number, String owner_inn, String producer_inn,
                        LocalDate production_date, String tnved_code, String uit_code, String uitu_code) {
            this.certificate_document = certificate_document;
            this.certificate_document_date = certificate_document_date;
            this.certificate_document_number = certificate_document_number;
            this.owner_inn = owner_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.tnved_code = tnved_code;
            this.uit_code = uit_code;
            this.uitu_code = uitu_code;
        }
    }

    enum DocType {
        LP_INTRODUCE_GOODS
    }

    static class LocalDateAdapter extends TypeAdapter<LocalDate> {

        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public void write(JsonWriter jsonWriter, LocalDate localDate) throws IOException {
            jsonWriter.value(localDate.format(formatter));
        }

        @Override
        public LocalDate read(JsonReader jsonReader) throws IOException {
            return LocalDate.parse(jsonReader.nextString(), formatter);
        }
    }
}
