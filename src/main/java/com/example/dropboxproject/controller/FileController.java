package com.example.dropboxproject.controller;

import com.example.dropboxproject.model.FileModel;
import com.example.dropboxproject.model.FolderModel;
import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.repository.FileRepository;
import com.example.dropboxproject.repository.UserRepository;
import com.example.dropboxproject.service.FileService;
import com.example.dropboxproject.service.FolderService;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;

@Controller
public class FileController {
    private static final Logger logger = LogManager.getLogger(FileController.class);

    private final FileService fileService;
    private final FolderService folderService;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private final Path rootLocation;

    public FileController(FileService fileService, FolderService folderService, @Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.fileService = fileService;
        this.folderService = folderService;
        this.rootLocation = Paths.get(uploadDir);
    }

    @GetMapping("/")
    public String home(Model model, 
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "5") int size) {
        try {
            // Get current logged in user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            UserModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Page<FileModel> filesPage = fileService.listUploadedFiles(username, page, size);
            List<FolderModel> folders = folderService.getRootFolders(user);
            
            model.addAttribute("files", filesPage.getContent());
            model.addAttribute("folders", folders);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", filesPage.getTotalPages());
            model.addAttribute("totalItems", filesPage.getTotalElements());
            
            logger.info("Home page: Listing {} files for user {} (Page {}/{})", 
                filesPage.getNumberOfElements(), username, page + 1, filesPage.getTotalPages());
            
            if (filesPage.isEmpty()) {
                model.addAttribute("message", "You don't have any files yet.");
            }
            
            return "index";
        } catch (Exception e) {
            logger.error("Home page: Error listing files: {}", e.getMessage());
            model.addAttribute("error", "An error occurred while listing files: " + e.getMessage());
            model.addAttribute("files", List.of());
            return "index";
        }
    }

    @PostMapping("/")
    public String handleHomeFileUpload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "folderId", required = false) Long folderId,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (file == null || file.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "Please select a file!");
                logger.error("No file selected");
                return "redirect:/";
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                redirectAttributes.addFlashAttribute("message", "You must be logged in to upload files!");
                logger.error("Unauthorized file upload attempt");
                return "redirect:/login";
            }

            String username = auth.getName();
            UserModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Find folder if folderId is specified
            FolderModel folder = null;
            if (folderId != null) {
                folder = folderService.getFolder(folderId);
                if (folder != null && !folder.getUser().getId().equals(user.getId())) {
                    redirectAttributes.addFlashAttribute("message", "You don't have access to this folder!");
                    return "redirect:/";
                }
            }
            
            logger.info("Starting file upload - User: {}, File: {}, Folder: {}", 
                username, file.getOriginalFilename(), folder != null ? folder.getName() : "Root Folder");
            
            FileModel savedFile = fileService.saveFile(file, user, folder);
            redirectAttributes.addFlashAttribute("message", "File uploaded successfully: " + file.getOriginalFilename());
            logger.info("File uploaded successfully: {}", savedFile.getFileName());
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", "Error: " + e.getMessage());
            logger.error("File upload error (invalid argument): {}", e.getMessage());
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("message", "Security error: File could not be uploaded!");
            logger.error("File upload security error: {}", e.getMessage());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "Error occurred while uploading file: " + e.getMessage());
            logger.error("File upload I/O error: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "An unexpected error occurred!");
            logger.error("Unexpected file upload error: {}", e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/upload")
    public String uploadForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        UserModel user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Kullanıcının klasörlerini getir
        List<FolderModel> folders = folderService.getRootFolders(user);
        model.addAttribute("folders", folders);
        
        logger.info("Yükleme sayfası açıldı - Kullanıcı: {}", username);
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "folderId", required = false) Long folderId,
                                 Model model) {
        try {
            if (file == null || file.isEmpty()) {
                model.addAttribute("message", "Lütfen bir dosya seçin!");
                logger.error("Dosya seçilmedi");
                return "upload";
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                model.addAttribute("message", "Dosya yüklemek için giriş yapmalısınız!");
                logger.error("Yetkisiz dosya yükleme denemesi");
                return "redirect:/login";
            }

            String username = auth.getName();
            UserModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Klasörü bul (eğer folderId belirtilmişse)
            FolderModel folder = null;
            if (folderId != null) {
                folder = folderService.getFolder(folderId);
                if (folder != null && !folder.getUser().getId().equals(user.getId())) {
                    model.addAttribute("message", "Bu klasöre erişim izniniz yok!");
                    return "upload";
                }
            }
            
            logger.info("Dosya yükleme başladı - Kullanıcı: {}, Dosya: {}, Klasör: {}", 
                username, file.getOriginalFilename(), folder != null ? folder.getName() : "Ana Klasör");
            
            FileModel savedFile = fileService.saveFile(file, user, folder);
            model.addAttribute("message", "Dosya başarıyla yüklendi: " + file.getOriginalFilename());
            logger.info("Dosya başarıyla yüklendi: {}", savedFile.getFileName());
            
            // Klasörleri tekrar yükle
            List<FolderModel> folders = folderService.getRootFolders(user);
            model.addAttribute("folders", folders);
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("message", "Hata: " + e.getMessage());
            logger.error("Dosya yükleme hatası (geçersiz argüman): {}", e.getMessage());
        } catch (SecurityException e) {
            model.addAttribute("message", "Güvenlik hatası: Dosya yüklenemedi!");
            logger.error("Dosya yükleme güvenlik hatası: {}", e.getMessage());
        } catch (IOException e) {
            model.addAttribute("message", "Dosya yükleme sırasında hata oluştu: " + e.getMessage());
            logger.error("Dosya yükleme I/O hatası: {}", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("message", "Beklenmeyen bir hata oluştu!");
            logger.error("Beklenmeyen dosya yükleme hatası: {}", e.getMessage());
        }
        return "upload";
    }

    @GetMapping("/list")
    public String listUploadedFiles(Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "5") int size) {
        try {
            // Mevcut oturum açmış kullanıcıyı al
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Page<FileModel> filesPage = fileService.listUploadedFiles(username, page, size);
            model.addAttribute("files", filesPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", filesPage.getTotalPages());
            model.addAttribute("totalItems", filesPage.getTotalElements());
            
            logger.info("Kullanıcı {} için {} dosya listeleniyor (Sayfa {}/{})", 
                username, filesPage.getNumberOfElements(), page + 1, filesPage.getTotalPages());
            
            if (filesPage.isEmpty()) {
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

    @GetMapping("/uploads/{id}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable Long id) {
        try {
            // Mevcut oturum açmış kullanıcıyı al
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Resource resource = fileService.loadFileAsResourceById(id, username);
            
            if (resource.exists() || resource.isReadable()) {
                logger.info("Dosya indiriliyor. ID: {}", id);
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                logger.warn("Dosya bulunamadı. ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Dosya erişim hatası. ID: {}, Hata: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteFile(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Mevcut oturum açmış kullanıcıyı al
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                logger.error("Yetkisiz silme denemesi. ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Dosya silmek için giriş yapmalısınız!");
                return "redirect:/login";
            }

            String username = auth.getName();
            logger.info("Dosya silme işlemi başlatıldı. ID: {}, Kullanıcı: {}", id, username);
            
            if (fileService.deleteFile(id, username)) {
                logger.info("Dosya başarıyla silindi. ID: {}, Kullanıcı: {}", id, username);
                redirectAttributes.addFlashAttribute("message", "Dosya başarıyla silindi!");
            } else {
                logger.warn("Dosya silme başarısız. ID: {}, Kullanıcı: {}", id, username);
                redirectAttributes.addFlashAttribute("error", "Dosya silinirken hata oluştu veya dosya bulunamadı!");
            }
        } catch (Exception e) {
            logger.error("Dosya silme hatası. ID: {}, Hata: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Dosya silinirken bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/";
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

        return "redirect:/list";
    }

    @GetMapping("/preview/{id}")
    @ResponseBody
    public ResponseEntity<?> previewFile(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            FileModel file = fileService.getFileById(id, username);
            Resource resource = fileService.loadFileAsResourceById(id, username);
            
            // Handle different file types
            String contentType = file.getFileType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            // For images and PDFs, we'll display them directly
            if (contentType.startsWith("image/") || contentType.equals("application/pdf")) {
                return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header("Content-Disposition", "inline; filename=\"" + file.getOriginalFilename() + "\"")
                    .body(resource);
            }
            
            // For text files, return the content as text
            if (contentType.startsWith("text/") || 
                contentType.equals("application/json") ||
                contentType.equals("application/xml") ||
                file.getOriginalFilename().endsWith(".md") ||
                file.getOriginalFilename().endsWith(".csv")) {
                    
                String content = fileService.readFileContent(file);
                return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                    .body(content);
            }
            
            // For other files, return file info
            return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new FilePreviewInfo(file.getOriginalFilename(), file.getFileType(), file.getSize()));
                
        } catch (Exception e) {
            logger.error("File preview error. ID: {}, Error: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error previewing file: " + e.getMessage());
        }
    }
    @GetMapping("/search")
    @ResponseBody
    public Page<FileModel> searchFilesAjax(@RequestParam("keyword") String keyword,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "5") int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return fileService.searchUserFilesByName(username, keyword, page, size);
    }


}

class FilePreviewInfo {
    private String filename;
    private String fileType;
    private Long size;
    
    public FilePreviewInfo(String filename, String fileType, Long size) {
        this.filename = filename;
        this.fileType = fileType;
        this.size = size;
    }
    
    // Getters
    public String getFilename() { return filename; }
    public String getFileType() { return fileType; }
    public Long getSize() { return size; }
}
