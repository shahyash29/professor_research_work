package com.example.trafficcsv.service;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosBulkOperations;
import com.azure.cosmos.models.CosmosItemOperation;
import com.azure.cosmos.models.PartitionKey;
import com.example.trafficcsv.model.TrafficSegment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class AzureTrafficDataService {

    private static final Logger log = LoggerFactory.getLogger(AzureTrafficDataService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final CosmosAsyncContainer asyncContainer;

    @Value("${traffic.data.dir}")
    private String dataDir;

    public AzureTrafficDataService(CosmosAsyncContainer asyncContainer) {
        this.asyncContainer = asyncContainer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        pushPendingFiles();
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledFolderScan() {
        pushPendingFiles();
    }

    private void pushPendingFiles() {
        if (dataDir == null || dataDir.isBlank()) {
            log.warn("traffic.data.dir is not set; nothing to ingest");
            return;
        }

        Path dir = Paths.get(dataDir);
        if (!Files.exists(dir)) {
            log.warn("Directory {} does not exist", dataDir);
            return;
        }

        try (Stream<Path> files = Files.list(dir).filter(p -> p.toString().toLowerCase().endsWith(".json"))) {
            List<Path> jsonFiles = files.collect(Collectors.toList());
            if (jsonFiles.isEmpty()) {
                log.debug("No JSON files found in {}", dataDir);
                return;
            }

            for (Path file : jsonFiles) {
                try {
                    String json = Files.readString(file, StandardCharsets.UTF_8);
                    uploadJson(json);
                    Files.deleteIfExists(file);
                    log.info("Uploaded and removed {}", file.getFileName());
                } catch (IOException ex) {
                    log.error("Failed to process {}", file, ex);
                }
            }
        } catch (IOException ex) {
            log.error("Failed scanning directory {}", dataDir, ex);
        }
    }

    private void uploadJson(String jsonData) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        JsonNode root;
        try {
            root = mapper.readTree(jsonData);
        } catch (Exception e) {
            log.error("Malformed JSON skipped", e);
            return;
        }

        JsonNode flowArray = extractFlowNode(root);
        if (!flowArray.isArray() || flowArray.isEmpty()) {
            log.warn("No traffic segments found in file at {}", ts);
            return;
        }

        List<CosmosItemOperation> ops = StreamSupport.stream(flowArray.spliterator(), false)
            .map(node -> createOp(node, ts))
            .collect(Collectors.toList());

        Flux.fromIterable(ops)
            .buffer(10)
            .concatMap(batch -> asyncContainer.executeBulkOperations(Flux.fromIterable(batch))
                                           .publishOn(Schedulers.boundedElastic()))
            .doOnNext(res -> {
                if (res.getException() != null) {
                    log.error("Failed item={}, err={}", res.getOperation().getItem(), res.getException().getMessage());
                }
            })
            .doOnError(err -> log.error("Bulk upload failed", err))
            .subscribe();
    }

    private CosmosItemOperation createOp(JsonNode seg, String ts) {
        JsonNode loc = seg.path("location");
        JsonNode cf  = seg.path("currentFlow");

        String rawDescription = loc.path("description").asText("Unknown");
        String safeDescription = rawDescription.replaceAll("[/\\\\#]", "-");
        String id = ts + "-" + safeDescription;

        TrafficSegment item = new TrafficSegment(
            id,
            ts,
            rawDescription,
            loc.path("length").asDouble(0.0),
            cf.path("speed").asDouble(0.0),
            cf.path("speedUncapped").asDouble(0.0),
            cf.path("freeFlow").asDouble(0.0),
            cf.path("jamFactor").asDouble(0.0),
            cf.path("confidence").asDouble(0.0),
            cf.path("traversability").asText("N/A")
        );

        return CosmosBulkOperations.getUpsertItemOperation(item, new PartitionKey(item.getTime()));
    }

    private JsonNode extractFlowNode(JsonNode root) {
        JsonNode flow = root.path("flow");
        if (flow.isArray() && !flow.isEmpty()) return flow;

        JsonNode results = root.path("results");
        if (results.isArray() && results.size() > 0) return results;

        JsonNode rws = root.path("RWS");
        if (rws.isArray() && rws.size() > 0) {
            JsonNode rw = rws.get(0).path("RW");
            if (rw.isArray() && rw.size() > 0) {
                JsonNode fis = rw.get(0).path("FIS");
                if (fis.isArray() && fis.size() > 0) {
                    return fis.get(0).path("FI");
                }
            }
        }
        return mapper.createArrayNode();
    }
}
