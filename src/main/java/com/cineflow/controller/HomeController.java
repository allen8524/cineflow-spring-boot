package com.cineflow.controller;

import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.service.PublicMovieMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.function.Supplier;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PublicMovieMetadataService publicMovieMetadataService;

    @GetMapping({"/", "/index.html"})
    public String home(Model model) {
        List<PublicMovieMetadataDto> heroMovies = withLocalFallback(
                publicMovieMetadataService.getHeroMovies(3),
                () -> publicMovieMetadataService.getLocalHeroMovies(3)
        );
        List<PublicMovieMetadataDto> boxOfficeMovies = withLocalFallback(
                publicMovieMetadataService.getPopularMovies(8),
                () -> publicMovieMetadataService.getLocalActiveMovies(8)
        );
        List<PublicMovieMetadataDto> nowShowingMovies = withLocalFallback(
                publicMovieMetadataService.getNowShowingMovies(8),
                () -> publicMovieMetadataService.getLocalNowShowingMovies(8)
        );
        List<PublicMovieMetadataDto> comingSoonMovies = withLocalFallback(
                publicMovieMetadataService.getComingSoonMovies(8),
                () -> publicMovieMetadataService.getLocalComingSoonMovies(8)
        );

        model.addAttribute("heroMovies", heroMovies);
        model.addAttribute("featuredMovie", heroMovies.stream().findFirst().orElse(null));
        model.addAttribute("boxOfficeMovies", boxOfficeMovies);
        model.addAttribute("nowShowingMovies", nowShowingMovies);
        model.addAttribute("comingSoonMovies", comingSoonMovies);
        return "index";
    }

    private List<PublicMovieMetadataDto> withLocalFallback(
            List<PublicMovieMetadataDto> tmdbMovies,
            Supplier<List<PublicMovieMetadataDto>> localFallbackMovies
    ) {
        return tmdbMovies == null || tmdbMovies.isEmpty() ? localFallbackMovies.get() : tmdbMovies;
    }
}
