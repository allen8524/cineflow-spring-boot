package com.cineflow.controller;

import com.cineflow.dto.SignupRequestDto;
import com.cineflow.security.AuthenticatedUser;
import com.cineflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        if (authenticatedUser != null) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupForm(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            Model model
    ) {
        if (authenticatedUser != null) {
            return "redirect:/";
        }
        if (!model.containsAttribute("signupRequest")) {
            model.addAttribute("signupRequest", new SignupRequestDto());
        }
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(
            @Valid @ModelAttribute("signupRequest") SignupRequestDto signupRequest,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (authenticatedUser != null) {
            return "redirect:/";
        }

        if (!bindingResult.hasFieldErrors("password")
                && !bindingResult.hasFieldErrors("passwordConfirm")
                && signupRequest.getPassword() != null
                && signupRequest.getPasswordConfirm() != null
                && !signupRequest.getPassword().equals(signupRequest.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "signup.passwordConfirm", "비밀번호 확인이 일치하지 않습니다.");
        }

        if (!bindingResult.hasFieldErrors("loginId") && userService.existsByLoginId(signupRequest.getLoginId())) {
            bindingResult.rejectValue("loginId", "signup.loginId.duplicate", "이미 사용 중인 아이디입니다.");
        }

        if (!bindingResult.hasFieldErrors("email") && userService.existsByEmail(signupRequest.getEmail())) {
            bindingResult.rejectValue("email", "signup.email.duplicate", "이미 사용 중인 이메일입니다.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        try {
            userService.register(signupRequest);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("signupError", ex.getMessage());
            return "auth/signup";
        }

        redirectAttributes.addFlashAttribute("successMessage", "회원가입이 완료되었습니다. 로그인 후 예매를 이어가실 수 있습니다.");
        return "redirect:/login?registered";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }
}
