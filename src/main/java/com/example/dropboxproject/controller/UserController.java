package com.example.dropboxproject.controller;

import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public ResponseEntity<UserModel> createUser(@RequestBody UserModel user) {
        UserModel savedUser = userService.saveUser(user);  // UserService'den çağırılıyor
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute UserModel user) {
        userService.saveUser(user);  // Kullanıcıyı kaydet
        return "redirect:/login";  // Kayıt işleminden sonra login sayfasına yönlendir
    }

}

