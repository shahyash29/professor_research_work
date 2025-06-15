package com.example.trafficcsv.controller;

import com.example.trafficcsv.service.CsvService;
import com.example.trafficcsv.service.TrafficDataService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class TrafficController {

    private final TrafficDataService dataService;
    private final CsvService         csvService;

    public TrafficController(
      TrafficDataService dataService,
      CsvService csvService
    ) {
        this.dataService = dataService;
        this.csvService  = csvService;
    }

    @GetMapping(value="/traffic", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTraffic() {
        return ResponseEntity.ok(dataService.fetchRawTrafficJson());
    }

    @GetMapping("/fetch-and-save")
    public ResponseEntity<String> fetchAndSave() {
        String path = dataService.fetchAndSaveTrafficData();
        return ResponseEntity.ok("Saved JSON to: " + path);
    }

    @GetMapping("/export-csv")
    public ResponseEntity<String> exportCsv() {
        try {
            String csvPath = csvService.createConsolidatedCsv();
            return ResponseEntity.ok("CSV written to: " + csvPath);
        } catch (IOException e) {
            return ResponseEntity
                     .status(500)
                     .body("CSV export failed: " + e.getMessage());
        }
    }
}
