package com.example.dropboxproject.service;

import com.example.dropboxproject.model.FolderModel;
import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.repository.FolderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FolderService {

    @Autowired
    private FolderRepository folderRepository;

    public List<FolderModel> getRootFolders(UserModel user) {
        return folderRepository.findByUserAndParentFolderIsNull(user);
    }

    public List<FolderModel> getSubFolders(FolderModel parentFolder) {
        return folderRepository.findByParentFolder(parentFolder);
    }

    public FolderModel getFolder(Long id) {
        return folderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Folder not found with id: " + id));
    }

    @Transactional
    public FolderModel createFolder(String name, UserModel user, FolderModel parentFolder) {
        if (folderRepository.existsByNameAndUserAndParentFolder(name, user, parentFolder)) {
            throw new IllegalArgumentException("Bu isimde bir klasör zaten mevcut: " + name);
        }

        FolderModel folder = new FolderModel();
        folder.setName(name);
        folder.setUser(user);
        folder.setParentFolder(parentFolder);
        return folderRepository.save(folder);
    }

    @Transactional
    public void deleteFolder(Long id) {
        FolderModel folder = getFolder(id);
        folderRepository.delete(folder);
    }
} 