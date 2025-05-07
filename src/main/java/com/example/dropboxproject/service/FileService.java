package com.example.dropboxproject.service;

import com.example.dropboxproject.model.FileModel;
import com.example.dropboxproject.repository.FileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final Path rootLocation = Paths.get("uploads");
    private final JavaMailSender mailSender;

    public FileService(FileRepository fileRepository, JavaMailSender mailSender) {
        this.fileRepository = fileRepository;
        this.mailSender = mailSender;
    }

    public FileModel saveFile(MultipartFile file) throws IOException {
        // Veritabanına kaydet
        FileModel fileModel = new FileModel();
        fileModel.setFileName(file.getOriginalFilename());
        fileModel.setFileType(file.getContentType());
        fileModel.setSize(file.getSize());
        fileModel.setData(file.getBytes());
        fileRepository.save(fileModel);

        // uploads klasörüne fiziksel dosyayı yaz
        if (!Files.exists(rootLocation)) {
            Files.createDirectories(rootLocation);
        }
        Path destinationFile = rootLocation.resolve(Paths.get(file.getOriginalFilename())).normalize().toAbsolutePath();
        Files.copy(file.getInputStream(), destinationFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        return fileModel;
    }


    public List<FileModel> listUploadedFiles() {
        return fileRepository.findAll();  // Veritabanındaki tüm dosyaları listele
    }

    public void deleteFile(Long id) {
        fileRepository.deleteById(id);
    }
    public void sendEmailWithLink(String to, String link, String filename) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Paylaşılan Dosya: " + filename);
        message.setText("Merhaba,\n\nAşağıdaki bağlantı üzerinden dosyayı indirebilirsiniz:\n" + link + "\n\nİyi günler!");
        mailSender.send(message);
    }
}


