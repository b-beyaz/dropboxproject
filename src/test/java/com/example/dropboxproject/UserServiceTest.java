package com.example.dropboxproject;


import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.repository.UserRepository;
import com.example.dropboxproject.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveUser() {
        // Arrange
        UserModel user = new UserModel();
        user.setUsername("testuser");
        user.setPassword("123456");

        when(userRepository.save(user)).thenReturn(user);

        // Act
        UserModel savedUser = userService.saveUser(user);

        // Assert
        assertNotNull(savedUser);
        assertEquals("testuser", savedUser.getUsername());
        verify(userRepository, times(1)).save(user);
    }
}
