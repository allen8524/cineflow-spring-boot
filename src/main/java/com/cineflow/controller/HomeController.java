package com.cineflow.controller;

import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.service.PublicMovieMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PublicMovieMetadataService publicMovieMetadataService;

    @GetMapping({"/", "/index.html"})
    public String home(Model model) {
        List<PublicMovieMetadataDto> heroMovies = publicMovieMetadataService.getHeroMovies(3);
        List<PublicMovieMetadataDto> boxOfficeMovies = publicMovieMetadataService.getPopularMovies(8);
        List<PublicMovieMetadataDto> nowShowingMovies = publicMovieMetadataService.getNowShowingMovies(8);
        List<PublicMovieMetadataDto> comingSoonMovies = publicMovieMetadataService.getComingSoonMovies(8);

        model.addAttribute("heroMovies", heroMovies);
        model.addAttribute("featuredMovie", heroMovies.stream().findFirst().orElse(null));
        model.addAttribute("boxOfficeMovies", boxOfficeMovies);
        model.addAttribute("nowShowingMovies", nowShowingMovies);
        model.addAttribute("comingSoonMovies", comingSoonMovies);
        return "index";
    }
}
