package com.example.trafficcsv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CsvService {

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${traffic.data.dir}")
    private String inputDirPath;

    @Value("${traffic.csv.dir}")
    private String outputDirPath;

    @PostConstruct
    public void ensureOutputDir() throws IOException {
        Files.createDirectories(Paths.get(outputDirPath));
    }

    public String createConsolidatedCsv() throws IOException {
        Path inputDir   = Paths.get(inputDirPath);
        Path outputFile = Paths.get(outputDirPath, "traffic_data.csv");

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
            "Time","Location Description","Length",
            "Speed","Uncapped Speed","Free Flow",
            "Jam Factor","Confidence","Traversability"
        });

        // 1) Gather and sort JSON files by filename (timestamps in name sort lexicographically)
        List<Path> jsonFiles;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "*.json")) {
            jsonFiles = new ArrayList<>();
            stream.forEach(jsonFiles::add);
        }
        // sort by filename ascending
        jsonFiles.sort(Comparator.comparing(p -> p.getFileName().toString()));

        int files = 0, records = 0;
        for (Path jsonPath : jsonFiles) {
            files++;
            JsonNode root;
            try {
                root = mapper.readTree(Files.readString(jsonPath));
            } catch (Exception ex) {
                System.err.println("Skipping invalid JSON: " + jsonPath.getFileName());
                continue;
            }

            // drill down to your FI array
            JsonNode flow = root.path("flow");
            if (!flow.isArray()) flow = root.path("results");
            if (!flow.isArray()) {
                JsonNode rws = root.path("RWS");
                if (rws.isArray() && rws.size()>0) {
                    JsonNode rw  = rws.get(0).path("RW");
                    JsonNode fis = (rw.isArray()&&rw.size()>0)
                                  ? rw.get(0).path("FIS")
                                  : null;
                    if (fis!=null && fis.isArray() && fis.size()>0) {
                        flow = fis.get(0).path("FI");
                    }
                }
            }
            if (!flow.isArray()) continue;

            // derive human‐readable time from filename
            String fileTime = jsonPath.getFileName().toString()
                .replace("traffic_data_","")
                .replace(".json","")
                .replace('-',':')
                .replace('T',' ');

            for (JsonNode seg : flow) {
                JsonNode loc = seg.path("location");
                JsonNode cf  = seg.path("currentFlow");
                rows.add(new String[]{
                    fileTime,
                    loc.path("description").asText("Unknown"),
                    loc.has("length") ? loc.get("length").asText() : "N/A",
                    cf.path("speed").asText("0.0"),
                    cf.path("speedUncapped").asText("0.0"),
                    cf.path("freeFlow").asText("0.0"),
                    cf.path("jamFactor").asText("0.0"),
                    cf.path("confidence").asText("0.0"),
                    cf.path("traversability").asText("N/A")
                });
                records++;
            }
        }

        // write CSV
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile.toFile()))) {
            writer.writeAll(rows);
        }

        System.out.printf("Processed %d files, wrote %d records to %s%n",
                          files, records, outputFile.toAbsolutePath());
        return outputFile.toAbsolutePath().toString();
    }
}
