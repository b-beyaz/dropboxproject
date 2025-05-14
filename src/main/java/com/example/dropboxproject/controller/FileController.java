package com.example.dropboxproject.controller;

import com.example.dropboxproject.model.FileModel;
import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.repository.UserRepository;
import com.example.dropboxproject.service.FileService;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class FileController {
    private static final Logger logger = LogManager.getLogger(FileController.class);

    private final FileService fileService;
    
    @Autowired
    private UserRepository userRepository;

    private final Path rootLocation = Paths.get("uploads");

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /*@GetMapping("/")
    public String home() {
        logger.info("Ana sayfadasınız");
        return "index";
    }*/

    @GetMapping("/upload")
    public String uploadForm() {
        logger.info("Yükleme sayfadasınız");
        return "upload"; // upload.html sayfasını döndürür
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            // Mevcut oturum açmış kullanıcıyı al
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            fileService.saveFile(file, username);
            model.addAttribute("message", "Dosya başarıyla yüklendi!");
            logger.info("Dosya başarıyla yüklendi: {}", file.getOriginalFilename());
        } catch (IOException e) {
            model.addAttribute("message", "Dosya yükleme sırasında hata oluştu!");
            logger.error("Dosya yükleme hatası: {}", e.getMessage());
        }
        return "upload";
    }

    @GetMapping("/list")
    public String listUploadedFiles(Model model) {
        try {
            // Mevcut oturum açmış kullanıcıyı al
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            List<FileModel> files = fileService.listUploadedFiles(username);
            model.addAttribute("files", files);
            logger.info("Kullanıcı {} için {} dosya listeleniyor", username, files.size());
            
            if (files.isEmpty()) {
                model.addAttribute("message", "Henüz hiç dosyanız bulunmuyor.");
            }
            
            return "list";
        } catch (Exception e) {
            logger.error("Dosya listeleme hatası: {}", e.getMessage());
            model.addAttribute("error", "Dosyalar listelenirken bir hata oluştu: " + e.getMessage());
            model.addAttribute("files", List.of()); // Boş liste gönder
            return "list";
        }
    }

    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            // Mevcut oturum açmış kullanıcıyı al
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Resource resource = fileService.loadFileAsResource(filename, username);
            
            if (resource.exists() || resource.isReadable()) {
                logger.info("Dosya indiriliyor: {}", filename);
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                logger.warn("Dosya bulunamadı: {}", filename);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Dosya erişim hatası: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteFile(@PathVariable Long id, Model model) {
        try {
            // Mevcut oturum açmış kullanıcıyı al
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            if (fileService.deleteFile(id, username)) {
                model.addAttribute("message", "Dosya başarıyla silindi!");
                logger.info("Dosya silindi. ID: {}", id);
            } else {
                model.addAttribute("message", "Dosya silinirken hata oluştu veya dosya bulunamadı!");
                logger.warn("Dosya silme başarısız. ID: {}", id);
            }
        } catch (Exception e) {
            model.addAttribute("message", "Dosya silinirken bir hata oluştu!");
            logger.error("Dosya silme hatası. ID: {}", e.getMessage());
        }
        return "redirect:/list"; // Dosya listesine geri yönlendir
    }

    @PostMapping("/share")
    public String shareFileByEmail(@RequestParam String email, @RequestParam String filename, Model model) {
        // Mevcut oturum açmış kullanıcıyı al
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        String downloadLink = "http://localhost:8080/uploads/" + filename;

        try {
            if (fileService.isFileOwnedByUser(filename, username)) {
                fileService.sendEmailWithLink(email, downloadLink, filename);
                model.addAttribute("message", "Dosya başarıyla paylaşıldı!");
                logger.info("Dosya {} e-posta ile paylaşıldı: {}", filename, email);
            } else {
                model.addAttribute("message", "Bu dosyayı paylaşma yetkiniz yok!");
                logger.warn("Yetkisiz dosya paylaşım denemesi: {}", filename);
            }
        } catch (Exception e) {
            model.addAttribute("message", "Dosya paylaşılırken hata oluştu: " + e.getMessage());
            logger.error("Dosya paylaşım hatası ({}): {}", filename, e.getMessage());
        }

        return "redirect:/list"; // Liste sayfasına geri dön
    }
}
