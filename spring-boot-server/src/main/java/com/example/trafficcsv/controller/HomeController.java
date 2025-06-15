package com.example.trafficcsv.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    /** Serve demo.html at “/” */
    @GetMapping("/")
    public String index() {
        return "forward:/demo.html";
    }
}
