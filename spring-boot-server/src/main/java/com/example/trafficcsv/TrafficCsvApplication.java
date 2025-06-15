package com.example.trafficcsv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;



@SpringBootApplication
@EnableScheduling
public class TrafficCsvApplication {
  public static void main(String[] args) {
    SpringApplication.run(TrafficCsvApplication.class, args);
  }
}
