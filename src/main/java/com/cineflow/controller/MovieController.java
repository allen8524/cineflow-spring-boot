package com.cineflow.controller;

import com.cineflow.domain.Movie;
import com.cineflow.dto.ScheduleViewDto;
import com.cineflow.service.MovieService;
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
    private final TheaterService theaterService;
    private final ScheduleService scheduleService;

    @GetMapping({"/movies", "/movielist.html"})
    public String list(Model model) {
        model.addAttribute("movies", movieService.getAllMovieViews());
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

        model.addAttribute("movie", movieService.toView(movie));
        model.addAttribute("relatedMovies", movieService.getRelatedMovieViews(movie.getId(), 4));
        model.addAttribute("theaters", theaterService.getTheatersForMovie(movie.getId()));
        model.addAttribute("schedules", schedules);
        model.addAttribute("theaterScheduleGroups", scheduleService.getTheaterScheduleGroupsByMovie(movie.getId()));
        model.addAttribute("nextSchedule", schedules.stream().findFirst().orElse(null));
        return "movies/detail";
    }
}
