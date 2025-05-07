package com.example.dropboxproject.controller;

import com.example.dropboxproject.model.FileModel;
import com.example.dropboxproject.service.FileService;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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


@Controller
public class FileController {
    private static final Logger logger = LogManager.getLogger(FileController.class);


    private final FileService fileService;
    private final Path rootLocation = Paths.get("uploads");

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/")
    public String home() {
        logger.info("Ana sayfadasınız");
        return "index";
    }


    @GetMapping("/upload")
    public String uploadForm() {
        logger.info("Yükleme sayfadasınız");
        return "upload"; // upload.html sayfasını döndürür
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            fileService.saveFile(file);
            model.addAttribute("message", "Dosya başarıyla yüklendi!");
            logger.info("Dosya başarıyla yüklendi: {}", file.getOriginalFilename());
        } catch (IOException e) {
            model.addAttribute("message", "Dosya yükleme sırasında hata oluştu!");
            logger.error("Dosya başarıyla yüklendi: {}", file.getOriginalFilename());
        }
        return "upload";
    }

    @GetMapping("/list")
    public String listUploadedFiles(Model model) {
        List<FileModel> files = fileService.listUploadedFiles();  // Veritabanındaki dosyaları listele
        model.addAttribute("files", files);  // Dosyaları model'e ekle
        logger.info("Dosyalar listeleniyor.");
        logger.info("Toplam {} dosya listelendi.", files.size());
        return "list";  // list.html sayfasına yönlendir
    }

    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                logger.info("Dosya indiriliyor: {}", filename);
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                logger.warn("Dosya okunamıyor ya da bulunamadı: {}", filename);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (MalformedURLException e) {
            logger.error("Dosya yolu hatası oluştu: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteFile(@PathVariable Long id, Model model) {
        fileService.deleteFile(id);
        model.addAttribute("message", "Dosya başarıyla silindi!");
        logger.info("Dosya silindi. ID: {}", id);
        return "redirect:/list"; // Dosya listesine geri yönlendir
    }

    @PostMapping("/share")
    public String shareFileByEmail(@RequestParam String email, @RequestParam String filename, Model model) {
        String downloadLink = "http://localhost:8080/uploads/" + filename;

        try {
            fileService.sendEmailWithLink(email, downloadLink, filename);
            model.addAttribute("message", "Dosya başarıyla paylaşıldı!");
            logger.info("Dosya {} e-posta ile paylaşıldı: {}", filename, email);
        } catch (Exception e) {
            model.addAttribute("message", "Dosya paylaşılırken hata oluştu: " + e.getMessage());
            logger.error("Dosya paylaşım hatası ({}): {}", filename, e.getMessage(), e);
        }

        return "redirect:/list"; // Liste sayfasına geri dön
    }





}

