package com.example.dropboxproject.controller;

import com.example.dropboxproject.model.FolderModel;
import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.repository.UserRepository;
import com.example.dropboxproject.service.FolderService;
import com.example.dropboxproject.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/folders")
public class FolderController {
    private static final Logger logger = LoggerFactory.getLogger(FolderController.class);

    @Autowired
    private FolderService folderService;
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String listFolders(@AuthenticationPrincipal User user, Model model) {
        UserModel userModel = userRepository.findByUsername(user.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("folders", folderService.getRootFolders(userModel));
        return "folders";
    }

    @GetMapping("/{id}")
    public String viewFolder(@PathVariable Long id, 
                           @AuthenticationPrincipal User user, 
                           Model model) {
        try {
            UserModel userModel = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            FolderModel folder = folderService.getFolder(id);
            if (folder != null && folder.getUser().getId().equals(userModel.getId())) {
                model.addAttribute("currentFolder", folder);
                model.addAttribute("folders", folderService.getSubFolders(folder));
                model.addAttribute("files", folder.getFiles());
                return "index";
            } else {
                model.addAttribute("error", "Bu klasöre erişim izniniz yok!");
                return "redirect:/";
            }
        } catch (Exception e) {
            logger.error("Klasör görüntüleme hatası: {}", e.getMessage());
            model.addAttribute("error", "Klasör görüntülenirken bir hata oluştu: " + e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/create")
    public String createFolder(@RequestParam String name, 
                             @RequestParam(required = false) Long parentId,
                             @AuthenticationPrincipal User user,
                             RedirectAttributes redirectAttributes) {
        try {
            UserModel userModel = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            FolderModel parentFolder = null;
            if (parentId != null) {
                parentFolder = folderService.getFolder(parentId);
                if (parentFolder != null && !parentFolder.getUser().getId().equals(userModel.getId())) {
                    redirectAttributes.addFlashAttribute("error", "Bu klasöre erişim izniniz yok!");
                    return "redirect:/";
                }
            }

            folderService.createFolder(name, userModel, parentFolder);
            redirectAttributes.addFlashAttribute("message", "Klasör başarıyla oluşturuldu!");
            logger.info("Klasör oluşturuldu: {} (Kullanıcı: {})", name, user.getUsername());
        } catch (Exception e) {
            logger.error("Klasör oluşturma hatası: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Klasör oluşturulurken bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/{id}/delete")
    public String deleteFolder(@PathVariable Long id, 
                             @AuthenticationPrincipal User user,
                             RedirectAttributes redirectAttributes) {
        try {
            UserModel userModel = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            FolderModel folder = folderService.getFolder(id);
            if (folder != null && folder.getUser().getId().equals(userModel.getId())) {
                folderService.deleteFolder(id);
                redirectAttributes.addFlashAttribute("message", "Klasör başarıyla silindi!");
                logger.info("Klasör silindi. ID: {}, Kullanıcı: {}", id, user.getUsername());
            } else {
                redirectAttributes.addFlashAttribute("error", "Klasörü silme yetkiniz yok veya klasör bulunamadı!");
                logger.warn("Yetkisiz klasör silme denemesi. ID: {}, Kullanıcı: {}", id, user.getUsername());
            }
        } catch (Exception e) {
            logger.error("Klasör silme hatası. ID: {}, Hata: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Klasör silinirken bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/{id}/upload")
    public String handleFileUpload(@PathVariable Long id,
                                 @RequestParam("file") MultipartFile file,
                                 @AuthenticationPrincipal User user,
                                 RedirectAttributes redirectAttributes) {
        try {
            UserModel userModel = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            FolderModel folder = folderService.getFolder(id);
            if (folder == null || !folder.getUser().getId().equals(userModel.getId())) {
                redirectAttributes.addFlashAttribute("error", "Bu klasöre erişim izniniz yok!");
                return "redirect:/";
            }

            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Lütfen bir dosya seçin!");
                return "redirect:/folders/" + id;
            }

            fileService.saveFile(file, userModel, folder);
            redirectAttributes.addFlashAttribute("message", "Dosya başarıyla yüklendi: " + file.getOriginalFilename());
            logger.info("Dosya yüklendi: {} (Klasör: {}, Kullanıcı: {})", 
                file.getOriginalFilename(), folder.getName(), user.getUsername());

        } catch (Exception e) {
            logger.error("Dosya yükleme hatası: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Dosya yüklenirken bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/folders/" + id;
    }
} 