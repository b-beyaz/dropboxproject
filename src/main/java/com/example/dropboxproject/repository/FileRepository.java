package com.example.dropboxproject.repository;

import com.example.dropboxproject.model.FileModel;
import com.example.dropboxproject.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileModel, Long> {
    List<FileModel> findByUser(UserModel user);
    Optional<FileModel> findByFileNameAndUser(String fileName, UserModel user);
    Optional<FileModel> findByIdAndUser(Long id, UserModel user);
}
