package com.example.dropboxproject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("name", "Ayşe");
        return "home"; // home.html dosyasına yönlendirir
    }
/*
    @GetMapping("/login")
    public String login(Model model) {
        return "login"; // home.html dosyasına yönlendirir
    }

    @GetMapping("/register")
    public String register(Model model) {
        return "register"; // home.html dosyasına yönlendirir
    }*/
}