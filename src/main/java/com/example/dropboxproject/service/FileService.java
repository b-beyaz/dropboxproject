package com.example.dropboxproject.service;

import com.example.dropboxproject.model.FileModel;
import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.repository.FileRepository;
import com.example.dropboxproject.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    
    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;
    
    private Path rootLocation;

    public FileService(FileRepository fileRepository, UserRepository userRepository, JavaMailSender mailSender) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(uploadDir);
            Files.createDirectories(rootLocation);
            logger.info("Dosya yükleme dizini oluşturuldu: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Dosya yükleme dizini oluşturulamadı: {}", e.getMessage());
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Transactional
    public FileModel saveFile(MultipartFile file, String username) throws IOException {
        try {
            UserModel user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Dosya adını benzersiz yap
            String originalFilename = file.getOriginalFilename();
            String uniqueFilename = System.currentTimeMillis() + "_" + originalFilename;

            // Veritabanına kaydet
            FileModel fileModel = new FileModel();
            fileModel.setFileName(uniqueFilename);
            fileModel.setFileType(file.getContentType());
            fileModel.setSize(file.getSize());
            fileModel.setData(file.getBytes());
            fileModel.setUser(user);
            
            FileModel savedFile = fileRepository.save(fileModel);
            logger.info("Dosya veritabanına kaydedildi: {}", uniqueFilename);

            // Fiziksel dosyayı kaydet
            Path destinationFile = rootLocation.resolve(Paths.get(uniqueFilename)).normalize().toAbsolutePath();
            
            if (!destinationFile.getParent().equals(rootLocation.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }
            
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Dosya fiziksel olarak kaydedildi: {}", destinationFile);
            }

            return savedFile;
        } catch (IOException e) {
            logger.error("Dosya kaydedilirken hata oluştu: {}", e.getMessage());
            throw new IOException("Failed to store file", e);
        }
    }

    public List<FileModel> listUploadedFiles(String username) {
        UserModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return fileRepository.findByUser(user);
    }

    public Resource loadFileAsResource(String filename, String username) throws Exception {
        UserModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FileModel fileModel = fileRepository.findByFileNameAndUser(filename, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        Path file = rootLocation.resolve(filename);
        Resource resource = new UrlResource(file.toUri());

        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Could not read file: " + filename);
        }
    }

    public boolean deleteFile(Long id, String username) {
        try {
            UserModel user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            FileModel fileModel = fileRepository.findByIdAndUser(id, user)
                    .orElseThrow(() -> new RuntimeException("File not found or access denied"));

            // Fiziksel dosyayı sil
            Path file = rootLocation.resolve(fileModel.getFileName());
            Files.deleteIfExists(file);

            // Veritabanından sil
            fileRepository.delete(fileModel);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting file: {}", e.getMessage());
            return false;
        }
    }

    public void sendEmailWithLink(String email, String downloadLink, String filename) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Dosya Paylaşımı");
        message.setText("Merhaba,\n\nSize bir dosya paylaşıldı: " + filename + 
                       "\n\nDosyayı indirmek için aşağıdaki linki kullanabilirsiniz:\n" + downloadLink);
        mailSender.send(message);
    }

    public boolean isFileOwnedByUser(String filename, String username) {
        UserModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return fileRepository.findByFileNameAndUser(filename, user).isPresent();
    }

    public void shareFileWithUser(Long fileId, String ownerUsername, UserModel targetUser) {
        UserModel owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new RuntimeException("Dosya sahibi bulunamadı"));

        FileModel file = fileRepository.findByIdAndUser(fileId, owner)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı veya erişim izniniz yok"));

        // Dosyanın bir kopyasını hedef kullanıcıya oluştur
        FileModel sharedFile = new FileModel();
        sharedFile.setFileName(file.getFileName());
        sharedFile.setFileType(file.getFileType());
        sharedFile.setSize(file.getSize());
        sharedFile.setData(file.getData());
        sharedFile.setUser(targetUser);
        
        fileRepository.save(sharedFile);
        logger.info("Dosya {} kullanıcı {} ile paylaşıldı", file.getFileName(), targetUser.getUsername());
    }
}


