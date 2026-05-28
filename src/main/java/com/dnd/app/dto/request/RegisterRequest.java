package com.dnd.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Имя пользователя обязательно")
    @Size(min = 3, max = 30, message = "Имя пользователя должно быть от 3 до 30 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Имя пользователя может содержать только латинские буквы, цифры и подчеркивания")
    private String username;

    @NotBlank(message = "Электронная почта обязательна")
    @Email(message = "Введите корректную электронную почту")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, message = "Пароль должен быть не короче 8 символов")
    private String password;

    @NotBlank(message = "Роль обязательна")
    @Pattern(regexp = "^(PLAYER|GAME_MASTER)$", message = "Роль должна быть PLAYER или GAME_MASTER")
    private String role;
}
