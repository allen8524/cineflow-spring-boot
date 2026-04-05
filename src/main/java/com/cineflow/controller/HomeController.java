package com.cineflow.controller;

import com.cineflow.domain.Movie;
import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.service.MovieService;
import com.cineflow.service.PublicMovieMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final MovieService movieService;
    private final PublicMovieMetadataService publicMovieMetadataService;

    @GetMapping({"/", "/index.html"})
    public String home(Model model) {
        List<Movie> heroSourceMovies = movieService.getFeaturedMovies(3);
        List<Movie> boxOfficeSourceMovies = movieService.getBoxOfficeMovies(8);
        List<Movie> nowShowingSourceMovies = movieService.getNowShowingMovies(8);
        List<Movie> comingSoonSourceMovies = movieService.getComingSoonMovies(8);

        Map<Long, PublicMovieMetadataDto> metadataByMovieId = resolveMetadataByMovieId(
                heroSourceMovies,
                boxOfficeSourceMovies,
                nowShowingSourceMovies,
                comingSoonSourceMovies
        );

        List<PublicMovieMetadataDto> heroMovies = mapMetadata(heroSourceMovies, metadataByMovieId);
        List<PublicMovieMetadataDto> boxOfficeMovies = mapMetadata(boxOfficeSourceMovies, metadataByMovieId);
        List<PublicMovieMetadataDto> nowShowingMovies = mapMetadata(nowShowingSourceMovies, metadataByMovieId);
        List<PublicMovieMetadataDto> comingSoonMovies = mapMetadata(comingSoonSourceMovies, metadataByMovieId);

        model.addAttribute("heroMovies", heroMovies);
        model.addAttribute("featuredMovie", heroMovies.stream().findFirst().orElse(null));
        model.addAttribute("boxOfficeMovies", boxOfficeMovies);
        model.addAttribute("nowShowingMovies", nowShowingMovies);
        model.addAttribute("comingSoonMovies", comingSoonMovies);
        return "index";
    }

    @SafeVarargs
    private Map<Long, PublicMovieMetadataDto> resolveMetadataByMovieId(List<Movie>... movieGroups) {
        Map<Long, Movie> uniqueMovies = new LinkedHashMap<>();

        for (List<Movie> movieGroup : movieGroups) {
            if (movieGroup == null) {
                continue;
            }

            for (Movie movie : movieGroup) {
                if (movie != null && movie.getId() != null) {
                    uniqueMovies.putIfAbsent(movie.getId(), movie);
                }
            }
        }

        return publicMovieMetadataService.resolveMetadata(List.copyOf(uniqueMovies.values())).stream()
                .filter(metadata -> metadata.getLocalMovieId() != null)
                .collect(Collectors.toMap(
                        PublicMovieMetadataDto::getLocalMovieId,
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }

    private List<PublicMovieMetadataDto> mapMetadata(
            List<Movie> sourceMovies,
            Map<Long, PublicMovieMetadataDto> metadataByMovieId
    ) {
        if (sourceMovies == null || sourceMovies.isEmpty()) {
            return List.of();
        }

        return sourceMovies.stream()
                .filter(Objects::nonNull)
                .map(movie -> {
                    if (movie.getId() != null && metadataByMovieId.containsKey(movie.getId())) {
                        return metadataByMovieId.get(movie.getId());
                    }
                    return publicMovieMetadataService.resolveMetadata(movie);
                })
                .toList();
    }
}
