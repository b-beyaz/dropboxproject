package com.example.dropboxproject.repository;

import com.example.dropboxproject.model.FolderModel;
import com.example.dropboxproject.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<FolderModel, Long> {
    List<FolderModel> findByUserAndParentFolderIsNull(UserModel user);
    List<FolderModel> findByParentFolder(FolderModel parentFolder);
    boolean existsByNameAndUserAndParentFolder(String name, UserModel user, FolderModel parentFolder);
} 