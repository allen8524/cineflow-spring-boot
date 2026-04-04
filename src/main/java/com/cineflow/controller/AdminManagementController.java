package com.cineflow.controller;

import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.domain.Screen;
import com.cineflow.dto.AdminMovieForm;
import com.cineflow.dto.AdminScheduleForm;
import com.cineflow.dto.AdminScreenForm;
import com.cineflow.dto.AdminTheaterForm;
import com.cineflow.service.MovieService;
import com.cineflow.service.ScheduleService;
import com.cineflow.service.ScreenService;
import com.cineflow.service.TheaterService;
import com.cineflow.service.TmdbClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminManagementController {

    private static final String TMDB_ENABLED_MESSAGE = "Search TMDB to auto-fill poster, overview, release date, and runtime.";
    private static final String TMDB_DISABLED_MESSAGE = "TMDB search is optional. Set TMDB_BEARER_TOKEN in local env to enable it. The rest of the app still works without it.";

    private final MovieService movieService;
    private final TheaterService theaterService;
    private final ScreenService screenService;
    private final ScheduleService scheduleService;
    private final TmdbClient tmdbClient;

    @GetMapping("/movies")
    public String movies(Model model) {
        model.addAttribute("movies", movieService.getAllMoviesForAdmin());
        return "admin/movies";
    }

    @GetMapping("/movies/new")
    public String newMovie(Model model) {
        if (!model.containsAttribute("movieForm")) {
            model.addAttribute("movieForm", new AdminMovieForm());
        }
        populateMovieFormModel(model, false, null);
        return "admin/movie-form";
    }

    @PostMapping("/movies")
    public String createMovie(
            @Valid @ModelAttribute("movieForm") AdminMovieForm movieForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateMovieFormModel(model, false, null);
            return "admin/movie-form";
        }
        try {
            movieService.createMovie(movieForm);
            redirectAttributes.addFlashAttribute("successMessage", "Movie created successfully.");
            return "redirect:/admin/movies";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("formErrorMessage", ex.getMessage());
            populateMovieFormModel(model, false, null);
            return "admin/movie-form";
        }
    }

    @GetMapping("/movies/{id}/edit")
    public String editMovie(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (!model.containsAttribute("movieForm")) {
                model.addAttribute("movieForm", AdminMovieForm.from(movieService.getMovieForAdmin(id)));
            }
            populateMovieFormModel(model, true, id);
            return "admin/movie-form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/movies";
        }
    }

    @PostMapping("/movies/{id}")
    public String updateMovie(
            @PathVariable Long id,
            @Valid @ModelAttribute("movieForm") AdminMovieForm movieForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateMovieFormModel(model, true, id);
            return "admin/movie-form";
        }
        try {
            movieService.updateMovie(id, movieForm);
            redirectAttributes.addFlashAttribute("successMessage", "Movie updated successfully.");
            return "redirect:/admin/movies";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("formErrorMessage", ex.getMessage());
            populateMovieFormModel(model, true, id);
            return "admin/movie-form";
        }
    }

    @PostMapping("/movies/{id}/delete")
    public String deleteMovie(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            movieService.deactivateMovie(id);
            redirectAttributes.addFlashAttribute("successMessage", "Movie deactivated.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/movies";
    }

    @GetMapping("/theaters")
    public String theaters(Model model) {
        model.addAttribute("theaters", theaterService.getAllTheatersForAdmin());
        return "admin/theaters";
    }

    @GetMapping("/theaters/new")
    public String newTheater(Model model) {
        if (!model.containsAttribute("theaterForm")) {
            model.addAttribute("theaterForm", new AdminTheaterForm());
        }
        populateTheaterFormModel(model, false, null);
        return "admin/theater-form";
    }

    @PostMapping("/theaters")
    public String createTheater(
            @Valid @ModelAttribute("theaterForm") AdminTheaterForm theaterForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateTheaterFormModel(model, false, null);
            return "admin/theater-form";
        }
        try {
            theaterService.createTheater(theaterForm);
            redirectAttributes.addFlashAttribute("successMessage", "Theater created successfully.");
            return "redirect:/admin/theaters";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("formErrorMessage", ex.getMessage());
            populateTheaterFormModel(model, false, null);
            return "admin/theater-form";
        }
    }

    @GetMapping("/theaters/{id}/edit")
    public String editTheater(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (!model.containsAttribute("theaterForm")) {
                model.addAttribute("theaterForm", AdminTheaterForm.from(theaterService.getTheaterForAdmin(id)));
            }
            populateTheaterFormModel(model, true, id);
            return "admin/theater-form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/theaters";
        }
    }

    @PostMapping("/theaters/{id}")
    public String updateTheater(
            @PathVariable Long id,
            @Valid @ModelAttribute("theaterForm") AdminTheaterForm theaterForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateTheaterFormModel(model, true, id);
            return "admin/theater-form";
        }
        try {
            theaterService.updateTheater(id, theaterForm);
            redirectAttributes.addFlashAttribute("successMessage", "Theater updated successfully.");
            return "redirect:/admin/theaters";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("formErrorMessage", ex.getMessage());
            populateTheaterFormModel(model, true, id);
            return "admin/theater-form";
        }
    }

    @PostMapping("/theaters/{id}/delete")
    public String deleteTheater(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            theaterService.deactivateTheater(id);
            redirectAttributes.addFlashAttribute("successMessage", "Theater deactivated.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/theaters";
    }

    @GetMapping("/screens")
    public String screens(Model model) {
        model.addAttribute("screens", screenService.getAllScreensForAdmin());
        return "admin/screens";
    }

    @GetMapping("/screens/new")
    public String newScreen(Model model) {
        if (!model.containsAttribute("screenForm")) {
            model.addAttribute("screenForm", new AdminScreenForm());
        }
        populateScreenFormModel(model, false, null);
        return "admin/screen-form";
    }

    @PostMapping("/screens")
    public String createScreen(
            @Valid @ModelAttribute("screenForm") AdminScreenForm screenForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateScreenFormModel(model, false, null);
            return "admin/screen-form";
        }
        try {
            screenService.createScreen(screenForm);
            redirectAttributes.addFlashAttribute("successMessage", "Screen created successfully.");
            return "redirect:/admin/screens";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("formErrorMessage", ex.getMessage());
            populateScreenFormModel(model, false, null);
            return "admin/screen-form";
        }
    }

    @GetMapping("/screens/{id}/edit")
    public String editScreen(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (!model.containsAttribute("screenForm")) {
                model.addAttribute("screenForm", AdminScreenForm.from(screenService.getScreenForAdmin(id)));
            }
            populateScreenFormModel(model, true, id);
            return "admin/screen-form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/screens";
        }
    }

    @PostMapping("/screens/{id}")
    public String updateScreen(
            @PathVariable Long id,
            @Valid @ModelAttribute("screenForm") AdminScreenForm screenForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateScreenFormModel(model, true, id);
            return "admin/screen-form";
        }
        try {
            screenService.updateScreen(id, screenForm);
            redirectAttributes.addFlashAttribute("successMessage", "Screen updated successfully.");
            return "redirect:/admin/screens";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("formErrorMessage", ex.getMessage());
            populateScreenFormModel(model, true, id);
            return "admin/screen-form";
        }
    }

    @PostMapping("/screens/{id}/delete")
    public String deleteScreen(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            screenService.deactivateScreen(id);
            redirectAttributes.addFlashAttribute("successMessage", "Screen deactivated.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/screens";
    }

    @GetMapping("/schedules/new")
    public String newSchedule(Model model) {
        if (!model.containsAttribute("scheduleForm")) {
            model.addAttribute("scheduleForm", new AdminScheduleForm());
        }
        populateScheduleFormModel(model, false, null);
        return "admin/schedule-form";
    }

    @PostMapping("/schedules")
    public String createSchedule(
            @Valid @ModelAttribute("scheduleForm") AdminScheduleForm scheduleForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateScheduleFormModel(model, false, null);
            return "admin/schedule-form";
        }
        try {
            scheduleService.createSchedule(scheduleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Schedule created successfully.");
            return "redirect:/admin/schedules";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("formErrorMessage", ex.getMessage());
            populateScheduleFormModel(model, false, null);
            return "admin/schedule-form";
        }
    }

    @GetMapping("/schedules/{id}/edit")
    public String editSchedule(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (!model.containsAttribute("scheduleForm")) {
                model.addAttribute("scheduleForm", AdminScheduleForm.from(scheduleService.getScheduleForAdmin(id)));
            }
            populateScheduleFormModel(model, true, id);
            return "admin/schedule-form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/schedules";
        }
    }

    @PostMapping("/schedules/{id}")
    public String updateSchedule(
            @PathVariable Long id,
            @Valid @ModelAttribute("scheduleForm") AdminScheduleForm scheduleForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateScheduleFormModel(model, true, id);
            return "admin/schedule-form";
        }
        try {
            scheduleService.updateSchedule(id, scheduleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Schedule updated successfully.");
            return "redirect:/admin/schedules";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("formErrorMessage", ex.getMessage());
            populateScheduleFormModel(model, true, id);
            return "admin/schedule-form";
        }
    }

    @PostMapping("/schedules/{id}/delete")
    public String deleteSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            scheduleService.deactivateSchedule(id);
            redirectAttributes.addFlashAttribute("successMessage", "Schedule deactivated.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/schedules";
    }

    private void populateMovieFormModel(Model model, boolean editMode, Long movieId) {
        boolean tmdbFeatureEnabled = tmdbClient.isConfigured();

        model.addAttribute("movieStatuses", MovieStatus.values());
        model.addAttribute("editMode", editMode);
        model.addAttribute("formAction", editMode ? "/admin/movies/" + movieId : "/admin/movies");
        model.addAttribute("pageTitle", editMode ? "Edit Movie" : "Create Movie");
        model.addAttribute("tmdbFeatureEnabled", tmdbFeatureEnabled);
        model.addAttribute("tmdbStatusMessage", tmdbFeatureEnabled ? TMDB_ENABLED_MESSAGE : TMDB_DISABLED_MESSAGE);
    }

    private void populateTheaterFormModel(Model model, boolean editMode, Long theaterId) {
        model.addAttribute("editMode", editMode);
        model.addAttribute("formAction", editMode ? "/admin/theaters/" + theaterId : "/admin/theaters");
        model.addAttribute("pageTitle", editMode ? "Edit Theater" : "Create Theater");
    }

    private void populateScreenFormModel(Model model, boolean editMode, Long screenId) {
        model.addAttribute("theaters", theaterService.getAllTheatersForAdmin());
        model.addAttribute("editMode", editMode);
        model.addAttribute("formAction", editMode ? "/admin/screens/" + screenId : "/admin/screens");
        model.addAttribute("pageTitle", editMode ? "Edit Screen" : "Create Screen");
    }

    private void populateScheduleFormModel(Model model, boolean editMode, Long scheduleId) {
        List<Movie> activeMovies = movieService.getAllMoviesForAdmin().stream()
                .filter(Movie::isActive)
                .toList();
        List<Screen> activeScreens = screenService.getActiveScreensForSchedule();

        model.addAttribute("movies", activeMovies);
        model.addAttribute("screens", activeScreens);
        model.addAttribute("editMode", editMode);
        model.addAttribute("formAction", editMode ? "/admin/schedules/" + scheduleId : "/admin/schedules");
        model.addAttribute("pageTitle", editMode ? "Edit Schedule" : "Create Schedule");
    }
}
