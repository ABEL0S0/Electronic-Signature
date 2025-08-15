package com.ES.Backend.controller;

import com.ES.Backend.data.UserDTO;
import com.ES.Backend.entity.User;
import com.ES.Backend.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Obtener todos los usuarios (sin contraseñas)
    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(UserDTO::fromUser)
                .collect(Collectors.toList());
    }

    // Buscar usuario por email (sin contraseña)
    @GetMapping("/search")
    public UserDTO getUserByEmail(@RequestParam String email) {
        User user = userService.findByEmail(email);
        return UserDTO.fromUser(user);
    }
    
}
