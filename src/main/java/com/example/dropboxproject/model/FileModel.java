package com.example.dropboxproject.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fileName;
    private String originalFilename;
    private String fileType;
    private Long size;
    private LocalDateTime uploadDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserModel user;
    //birden fazla dosyanın tek kullanıcısı olabilir

    @ManyToOne
    @JoinColumn(name = "folder_id")
    private FolderModel folder;

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
    }

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public FolderModel getFolder() {
        return folder;
    }

    public void setFolder(FolderModel folder) {
        this.folder = folder;
    }
}

