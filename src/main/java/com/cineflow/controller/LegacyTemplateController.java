package com.cineflow.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class LegacyTemplateController {

    private final ResourceLoader resourceLoader;

    public LegacyTemplateController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/{page:[a-zA-Z0-9_-]+}.html")
    public String legacyHtmlPage(@PathVariable String page) {
        Resource template = resourceLoader.getResource("classpath:/templates/" + page + ".html");
        if (template.exists()) {
            return page;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
