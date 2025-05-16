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
            // Şifre uzunluğu kontrolü
            String password = userModel.getPassword();
            if (password.length() < 6 || password.length() > 12) {
                model.addAttribute("errorMessage", "Şifre 6 ile 12 karakter arasında olmalıdır!");
                return "register";
            }

            // Kullanıcı adı kontrolü
            if (userRepository.findByUsername(userModel.getUsername()).isPresent()) {
                model.addAttribute("errorMessage", "Bu kullanıcı adı zaten kullanılıyor!");
                return "register";
            }

            // Şifreyi hashle ve kullanıcıyı kaydet
            userModel.setPassword(passwordEncoder.encode(userModel.getPassword()));
            userRepository.save(userModel);
            
            logger.info("Yeni kullanıcı kaydedildi: {}", userModel.getUsername());
            model.addAttribute("successMessage", "Kayıt başarılı! Şimdi giriş yapabilirsiniz.");
            return "redirect:/login";
        } catch (Exception e) {
            logger.error("Kayıt işlemi sırasında hata: {}", e.getMessage());
            model.addAttribute("errorMessage", "Kayıt sırasında bir hata oluştu: " + e.getMessage());
            return "register";
        }
    }

    // Login sayfasını gösterir
    @GetMapping("/login")
    public String showLoginForm(Model model, @RequestParam(required = false) String error) {
        if (error != null) {
            model.addAttribute("errorMessage", "Geçersiz kullanıcı adı veya şifre!");
        }
        return "login";
    }

    // Genel hata yakalama
    @ExceptionHandler(Exception.class)
    public String handleError(Exception e, Model model) {
        logger.error("Beklenmeyen bir hata oluştu: {}", e.getMessage());
        model.addAttribute("errorMessage", "Bir hata oluştu: " + e.getMessage());
        return "error";
    }
}
