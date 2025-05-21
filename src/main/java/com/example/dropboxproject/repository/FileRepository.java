package com.example.dropboxproject.repository;

import com.example.dropboxproject.model.FileModel;
import com.example.dropboxproject.model.FolderModel;
import com.example.dropboxproject.model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileModel, Long> {
    Page<FileModel> findByUserAndFolderIsNull(UserModel user, Pageable pageable);
    Optional<FileModel> findByFileNameAndUser(String fileName, UserModel user);
    Optional<FileModel> findByIdAndUser(Long id, UserModel user);
    List<FileModel> findByFolder(FolderModel folder);
    Page<FileModel> findByUser_UsernameAndFileNameContainingIgnoreCase(String username, String fileName, Pageable pageable);


}
