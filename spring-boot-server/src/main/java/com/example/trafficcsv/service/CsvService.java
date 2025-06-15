package com.example.trafficcsv.service;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.models.SqlParameter;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class CsvService {

    private final CosmosAsyncContainer container;
    private final Path csvPath;
    private OffsetDateTime lastRun;

    public CsvService(
        CosmosAsyncContainer container,
        @Value("${traffic.csv.dir}") String outputDir
    ) throws IOException {
        this.container = container;
        Files.createDirectories(Paths.get(outputDir));
        this.csvPath = Paths.get(outputDir, "traffic_live.csv");

        if (!Files.exists(csvPath)) {
            try (CSVWriter w = new CSVWriter(new FileWriter(csvPath.toFile()))) {
                w.writeNext(new String[]{
                  "Time","LocationDescription","Length",
                  "Speed","UncappedSpeed","FreeFlow",
                  "JamFactor","Confidence","Traversability"
                });
            }
        }

        this.lastRun = OffsetDateTime.now().minusMinutes(1);
    }

    @Scheduled(fixedRate = 60_000, initialDelay = 0)
    public void scheduledAppend() {
        appendNewRows();
    }

    /**
     * Queries for all new items since lastRun, appends them if any,
     * and returns the CSV path if rows were added, or null otherwise.
     */
    public String appendNewRows() {
        OffsetDateTime now = OffsetDateTime.now();
        SqlQuerySpec spec = new SqlQuerySpec(
            "SELECT * FROM c WHERE c.time > @from AND c.time <= @to",
            List.of(
              new SqlParameter("@from", lastRun.toString()),
              new SqlParameter("@to",   now.toString())
            )
        );
        CosmosQueryRequestOptions opts = new CosmosQueryRequestOptions();

        List<JsonNode> docs = container
          .queryItems(spec, opts, JsonNode.class)
          .byPage()
          .flatMap(p -> Flux.fromIterable(p.getResults()))
          .collectList()
          .block();

        lastRun = now;

        if (docs == null || docs.isEmpty()) {
            return null;
        }

        try (CSVWriter w = new CSVWriter(new FileWriter(csvPath.toFile(), true))) {
            for (JsonNode d : docs) {
                w.writeNext(new String[]{
                  d.path("time").asText(""),
                  d.path("locationDescription").asText(""),
                  d.path("length").asText("0"),
                  d.path("speed").asText("0"),
                  d.path("speedUncapped").asText("0"),
                  d.path("freeFlow").asText("0"),
                  d.path("jamFactor").asText("0"),
                  d.path("confidence").asText("0"),
                  d.path("traversability").asText("")
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return csvPath.toAbsolutePath().toString();
    }
}
