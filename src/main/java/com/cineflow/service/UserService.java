package com.cineflow.service;

import com.cineflow.domain.User;
import com.cineflow.domain.UserRole;
import com.cineflow.dto.SignupRequestDto;
import com.cineflow.repository.UserRepository;
import com.cineflow.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(SignupRequestDto request) {
        String normalizedLoginId = normalizeLoginId(request.getLoginId());
        String normalizedEmail = normalizeEmail(request.getEmail());

        validateSignupRequest(request, normalizedLoginId, normalizedEmail);

        User user = User.builder()
                .loginId(normalizedLoginId)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName().trim())
                .phone(request.getPhone().trim())
                .role(UserRole.USER)
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public User createAdminIfAbsent(String loginId, String email, String rawPassword, String name, String phone) {
        String normalizedLoginId = normalizeLoginId(loginId);
        return userRepository.findByLoginId(normalizedLoginId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .loginId(normalizedLoginId)
                        .email(normalizeEmail(email))
                        .password(passwordEncoder.encode(rawPassword))
                        .name(name)
                        .phone(phone)
                        .role(UserRole.ADMIN)
                        .enabled(true)
                        .build()));
    }

    public Optional<User> findByLoginId(String loginId) {
        if (!StringUtils.hasText(loginId)) {
            return Optional.empty();
        }
        return userRepository.findByLoginId(normalizeLoginId(loginId));
    }

    public Optional<User> findCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return Optional.of(authenticatedUser.getUser());
        }
        if (principal instanceof UserDetails userDetails) {
            return findByLoginId(userDetails.getUsername());
        }
        if (principal instanceof String username) {
            return findByLoginId(username);
        }
        return Optional.empty();
    }

    public boolean existsByLoginId(String loginId) {
        return StringUtils.hasText(loginId) && userRepository.existsByLoginIdIgnoreCase(loginId.trim());
    }

    public boolean existsByEmail(String email) {
        return StringUtils.hasText(email) && userRepository.existsByEmailIgnoreCase(email.trim());
    }

    public boolean isAdmin(User user) {
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByLoginIdIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 계정입니다."));
        return new AuthenticatedUser(user);
    }

    private void validateSignupRequest(SignupRequestDto request, String normalizedLoginId, String normalizedEmail) {
        if (!StringUtils.hasText(request.getPassword()) || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상으로 입력해 주세요.");
        }
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
        }
        if (userRepository.existsByLoginIdIgnoreCase(normalizedLoginId)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
    }

    private String normalizeLoginId(String loginId) {
        return loginId == null ? null : loginId.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
