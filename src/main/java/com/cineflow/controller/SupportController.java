package com.cineflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SupportController {

    @GetMapping("/support")
    public String support() {
        return "support/index";
    }

    @GetMapping("/support.html")
    public String legacySupport() {
        return "redirect:/support";
    }
}
