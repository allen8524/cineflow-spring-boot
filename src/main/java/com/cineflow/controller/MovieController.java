package com.cineflow.controller;

import com.cineflow.domain.Movie;
import com.cineflow.dto.PublicMovieMetadataDto;
import com.cineflow.dto.ScheduleViewDto;
import com.cineflow.service.MovieService;
import com.cineflow.service.PublicMovieMetadataService;
import com.cineflow.service.ScheduleService;
import com.cineflow.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class MovieController {

    private final MovieService movieService;
    private final PublicMovieMetadataService publicMovieMetadataService;
    private final TheaterService theaterService;
    private final ScheduleService scheduleService;

    @GetMapping({"/movies", "/movielist.html"})
    public String list(Model model) {
        model.addAttribute("movies", publicMovieMetadataService.resolveMetadata(movieService.getAllMovies()));
        return "movies/list";
    }

    @GetMapping("/movies/{id}")
    public String detail(@PathVariable Long id, Model model) {
        return renderDetail(movieService.getMovie(id), model);
    }

    @GetMapping("/moviesingle.html")
    public String detailByParam(@RequestParam(name = "id", required = false) Long id, Model model) {
        return renderDetail(movieService.getMovieOrDefault(id), model);
    }

    private String renderDetail(Movie movie, Model model) {
        List<ScheduleViewDto> schedules = scheduleService.getSchedulesForMovie(movie.getId());
        List<Movie> relatedSourceMovies = movieService.getRelatedMovies(movie.getId(), 4);
        Map<Long, PublicMovieMetadataDto> metadataByMovieId = resolveMetadataByMovieId(movie, relatedSourceMovies);

        model.addAttribute("movie", resolveDetailMovie(movie, metadataByMovieId));
        model.addAttribute("relatedMovies", mapRelatedMovies(relatedSourceMovies, metadataByMovieId));
        model.addAttribute("theaters", theaterService.getTheatersForMovie(movie.getId()));
        model.addAttribute("schedules", schedules);
        model.addAttribute("theaterScheduleGroups", scheduleService.getTheaterScheduleGroupsByMovie(movie.getId()));
        model.addAttribute("nextSchedule", schedules.stream().findFirst().orElse(null));
        return "movies/detail";
    }

    private Map<Long, PublicMovieMetadataDto> resolveMetadataByMovieId(Movie movie, List<Movie> relatedMovies) {
        List<Movie> sourceMovies = Stream.concat(Stream.of(movie), relatedMovies.stream()).toList();

        return publicMovieMetadataService.resolveMetadata(sourceMovies).stream()
                .filter(metadata -> metadata.getLocalMovieId() != null)
                .collect(Collectors.toMap(
                        PublicMovieMetadataDto::getLocalMovieId,
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }

    private PublicMovieMetadataDto resolveDetailMovie(Movie movie, Map<Long, PublicMovieMetadataDto> metadataByMovieId) {
        if (movie.getId() != null && metadataByMovieId.containsKey(movie.getId())) {
            return metadataByMovieId.get(movie.getId());
        }
        return publicMovieMetadataService.resolveMetadata(movie);
    }

    private List<PublicMovieMetadataDto> mapRelatedMovies(
            List<Movie> relatedSourceMovies,
            Map<Long, PublicMovieMetadataDto> metadataByMovieId
    ) {
        return relatedSourceMovies.stream()
                .map(relatedMovie -> {
                    if (relatedMovie.getId() != null && metadataByMovieId.containsKey(relatedMovie.getId())) {
                        return metadataByMovieId.get(relatedMovie.getId());
                    }
                    return publicMovieMetadataService.resolveMetadata(relatedMovie);
                })
                .toList();
    }
}
