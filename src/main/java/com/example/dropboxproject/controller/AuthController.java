package com.example.dropboxproject.controller;

import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Kayıt sayfasını gösterir
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("userModel", new UserModel());
        return "register"; // resources/templates/register.html dosyasını gösterir
    }

    @PostMapping("/perform_register")
    public String processRegister(@ModelAttribute UserModel userModel, Model model) {
        try {
            // Kullanıcı adı kontrolü
            if (userRepository.findByUsername(userModel.getUsername()).isPresent()) {
                model.addAttribute("error", "Bu kullanıcı adı zaten kullanılıyor!");
                return "register";
            }

            // Şifreyi hashle ve kullanıcıyı kaydet
            userModel.setPassword(passwordEncoder.encode(userModel.getPassword()));
            userRepository.save(userModel);
            
            logger.info("Yeni kullanıcı kaydedildi: {}", userModel.getUsername());
            model.addAttribute("success", "Kayıt başarılı! Şimdi giriş yapabilirsiniz.");
            return "redirect:/login";
        } catch (Exception e) {
            logger.error("Kayıt işlemi sırasında hata: {}", e.getMessage());
            model.addAttribute("error", "Kayıt sırasında bir hata oluştu!");
            return "register";
        }
    }

    // Login sayfasını gösterir
    @GetMapping("/login")
    public String showLoginForm() {
        return "login"; // resources/templates/login.html
    }

    // (İsteğe bağlı) Ana sayfa
    @GetMapping("/")
    public String home() {
        return "index"; // resources/templates/home.html
    }
}
