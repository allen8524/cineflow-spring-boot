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

import java.util.List;

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
        model.addAttribute("movies", publicMovieMetadataService.getMovieList(24));
        return "movies/list";
    }

    @GetMapping("/movies/{id}")
    public String detail(@PathVariable Long id, Model model) {
        return renderDetail(publicMovieMetadataService.getMovieDetail(id), model);
    }

    @GetMapping("/moviesingle.html")
    public String detailByParam(@RequestParam(name = "id", required = false) Long id, Model model) {
        PublicMovieMetadataDto movie = id != null
                ? publicMovieMetadataService.getMovieDetail(id)
                : publicMovieMetadataService.getDefaultMovieDetail();
        return renderDetail(movie, model);
    }

    private String renderDetail(PublicMovieMetadataDto movie, Model model) {
        Movie linkedLocalMovie = resolveLinkedLocalMovie(movie);
        Long linkedMovieId = linkedLocalMovie != null ? linkedLocalMovie.getId() : null;
        List<ScheduleViewDto> schedules = linkedMovieId != null
                ? scheduleService.getSchedulesForMovie(linkedMovieId)
                : List.of();

        model.addAttribute("movie", movie);
        model.addAttribute("relatedMovies", publicMovieMetadataService.getRelatedMovies(movie.getTmdbId(), 4));
        model.addAttribute("theaters", linkedMovieId != null ? theaterService.getTheatersForMovie(linkedMovieId) : List.of());
        model.addAttribute("schedules", schedules);
        model.addAttribute("theaterScheduleGroups", linkedMovieId != null
                ? scheduleService.getTheaterScheduleGroupsByMovie(linkedMovieId)
                : List.of());
        model.addAttribute("nextSchedule", schedules.stream().findFirst().orElse(null));
        return "movies/detail";
    }

    private Movie resolveLinkedLocalMovie(PublicMovieMetadataDto movie) {
        if (movie == null || movie.getLocalMovieId() == null) {
            return null;
        }

        return movieService.findActiveMovie(movie.getLocalMovieId()).orElse(null);
    }
}
