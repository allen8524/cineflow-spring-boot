package com.cineflow.controller;

import com.cineflow.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class MovieController {

    private final MovieService movieService;

    @GetMapping({"/movies", "/movielist.html"})
    public String list(Model model) {
        model.addAttribute("movies", movieService.getAllMovies());
        return "movielist";
    }

    @GetMapping("/movies/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("movie", movieService.getMovie(id));
        return "moviesingle";
    }

    @GetMapping("/moviesingle.html")
    public String detailByParam(@RequestParam(name = "id", required = false) Long id, Model model) {
        model.addAttribute("movie", movieService.getMovieOrDefault(id));
        return "moviesingle";
    }
}
