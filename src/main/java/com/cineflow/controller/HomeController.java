package com.cineflow.controller;

import com.cineflow.dto.MovieViewDto;
import com.cineflow.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final MovieService movieService;

    @GetMapping({"/", "/index.html"})
    public String home(Model model) {
        List<MovieViewDto> heroMovies = movieService.getFeaturedMovieViews(3);

        model.addAttribute("heroMovies", heroMovies);
        model.addAttribute("featuredMovie", heroMovies.stream().findFirst().orElse(null));
        model.addAttribute("boxOfficeMovies", movieService.getBoxOfficeMovieViews(8));
        model.addAttribute("nowShowingMovies", movieService.getNowShowingMovieViews(8));
        model.addAttribute("comingSoonMovies", movieService.getComingSoonMovieViews(8));
        return "index";
    }
}
