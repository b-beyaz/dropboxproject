package com.example.dropboxproject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FileController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";  // Bu sayfa, dosya yükleme formunu içerecek
    }

    @GetMapping("/list")
    public String listFiles() {
        return "list";  // Bu sayfa, dosya listeleme işlemini gösterecek
    }

    @GetMapping("/delete")
    public String deleteFile() {
        return "delete";  // Bu sayfa, dosya silme işlemini gösterecek
    }

    @GetMapping("/download")
    public String downloadFile() {
        return "download";  // Bu sayfa, dosya indirme işlemini gösterecek
    }

    @GetMapping("/shareFile")
    public String shareFile() {
        return "shareFile";  // Bu sayfa, dosya indirme işlemini gösterecek
    }
}

