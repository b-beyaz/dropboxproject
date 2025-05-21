package com.example.dropboxproject;


import com.example.dropboxproject.config.SecurityConfig;
import com.example.dropboxproject.controller.AuthController;
import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.ui.Model;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@Import(SecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void testShowRegisterForm() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("userModel"));
    }

    @Test
    void testShowLoginForm() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void testHomePage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSuccessfulRegistration() throws Exception {
        // Kullanıcı adı mevcut değil
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("1234")).thenReturn("hashedPassword");
        when(userRepository.save(any(UserModel.class))).thenReturn(new UserModel());

        mockMvc.perform(MockMvcRequestBuilders.post("/perform_register")
                        .param("username", "newuser")
                        .param("password", "1234")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testRegistrationWithExistingUsername() throws Exception {
        // Kullanıcı adı zaten mevcut
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(new UserModel()));

        mockMvc.perform(MockMvcRequestBuilders.post("/perform_register")
                        .param("username", "existinguser")
                        .param("password", "1234")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testRegistrationFailureException() throws Exception {
        // Exception fırlasın
        when(userRepository.findByUsername("failuser")).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(MockMvcRequestBuilders.post("/perform_register")
                        .param("username", "failuser")
                        .param("password", "1234")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }
}
