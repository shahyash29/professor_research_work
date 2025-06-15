// src/main/java/com/example/trafficcsv/service/TrafficDataService.java
package com.example.trafficcsv.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TrafficDataService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${traffic.api.url}")
    private String apiUrl;

    @Value("${traffic.data.dir}")
    private String dataDirPath;

    private File saveDirectory;

    /**  
     * Runs once when the application is ready.  
     * Creates the data directory and does a single fetch.  
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        saveDirectory = new File(dataDirPath);
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        try {
            String path = fetchAndSaveTrafficData();
            System.out.println("Initial JSON saved to: " + path);
        } catch (Exception e) {
            System.err.println("Initial fetch failed: " + e.getMessage());
        }
    }

    /**
     * Fetches the JSON payload but does *not* write to disk.
     */
    public String fetchRawTrafficJson() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

            HttpResponse<String> resp = httpClient.send(
                req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() != 200) {
                throw new RuntimeException("HTTP error: " + resp.statusCode());
            }
            return resp.body();

        } catch (Exception e) {
            throw new RuntimeException("Fetch failed: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches the JSON *and* writes it to a timestamped file in traffic_data/.
     */
    public String fetchAndSaveTrafficData() {
        String json = fetchRawTrafficJson();
        String ts   = LocalDateTime.now()
                          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"));
        File out    = new File(saveDirectory, "traffic_data_" + ts + ".json");
        try (FileWriter fw = new FileWriter(out, StandardCharsets.UTF_8)) {
            fw.write(json);
        } catch (Exception e) {
            throw new RuntimeException("Write failed: " + e.getMessage(), e);
        }
        return out.getAbsolutePath();
    }

    /**
     * Runs every 60 s **after** an initial 60 s delay.
     * Only saves JSON; CSV is generated on-demand.
     */
    @Scheduled(initialDelay = 60000, fixedRate = 60000)
    public void scheduledFetch() {
        try {
            String path = fetchAndSaveTrafficData();
            System.out.println("Scheduled JSON saved to: " + path);
        } catch (Exception e) {
            System.err.println("Scheduled fetch failed: " + e.getMessage());
        }
    }
}
