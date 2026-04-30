package com.cineflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class LegacyTemplateController {

    @GetMapping({
            "/moviegrid.html",
            "/moviegrid_light.html",
            "/moviegridfw.html",
            "/moviegridfw_light.html",
            "/movielist_light.html"
    })
    public String redirectLegacyMovieListPages() {
        return "redirect:/movies";
    }

    @GetMapping("/moviesingle_light.html")
    public String redirectLegacyMovieDetailPage(@RequestParam(required = false) Long id) {
        if (id != null) {
            return "redirect:/movies/" + id;
        }
        return "redirect:/movies";
    }

    @GetMapping({
            "/homev2.html",
            "/homev2_light.html",
            "/homev3.html",
            "/homev3_light.html",
            "/index-2.html",
            "/index_light.html",
            "/landing.html",
            "/comingsoon.html"
    })
    public String redirectLegacyHomePages() {
        return "redirect:/";
    }

    @GetMapping({
            "/blogdetail.html",
            "/blogdetail_light.html",
            "/bloggrid.html",
            "/bloggrid_light.html",
            "/bloglist.html",
            "/bloglist_light.html",
            "/celebritygrid01.html",
            "/celebritygrid01_light.html",
            "/celebritygrid02.html",
            "/celebritygrid02_light.html",
            "/celebritylist.html",
            "/celebritylist_light.html",
            "/celebritysingle.html",
            "/celebritysingle_light.html",
            "/seriessingle.html",
            "/seriessingle_light.html",
            "/userfavoritegrid.html",
            "/userfavoritegrid_light.html",
            "/userfavoritelist.html",
            "/userfavoritelist_light.html",
            "/userprofile.html",
            "/userprofile_light.html",
            "/userrate.html",
            "/userrate_light.html"
    })
    public String blockUnsupportedLegacyPages() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
