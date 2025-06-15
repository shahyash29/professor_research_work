package com.example.trafficcsv.controller;

import com.example.trafficcsv.service.CsvService;
import com.example.trafficcsv.service.TrafficDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/traffic")
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
        String csvPath = csvService.appendNewRows();
        if (csvPath == null) {
            return ResponseEntity.ok("No new data to append since last run.");
        }
        return ResponseEntity.ok("CSV written to: " + csvPath);
    }
}
