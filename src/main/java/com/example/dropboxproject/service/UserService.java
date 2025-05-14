package com.example.dropboxproject.service;

import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserModel saveUser(UserModel user) {
        logger.info("Kullanıcı kaydediliyor: " + user.getUsername());
        return userRepository.save(user);
    }
}
