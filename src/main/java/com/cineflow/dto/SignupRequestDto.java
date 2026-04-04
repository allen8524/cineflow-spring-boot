package com.cineflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequestDto {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해 주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "아이디는 영문, 숫자, ., _, - 만 사용할 수 있습니다.")
    private String loginId;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일 형식으로 입력해 주세요.")
    @Size(max = 120, message = "이메일은 120자 이하로 입력해 주세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상으로 입력해 주세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
    private String passwordConfirm;

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 50, message = "이름은 50자 이하로 입력해 주세요.")
    private String name;

    @NotBlank(message = "휴대폰 번호를 입력해 주세요.")
    @Size(max = 30, message = "휴대폰 번호는 30자 이하로 입력해 주세요.")
    private String phone;
}
