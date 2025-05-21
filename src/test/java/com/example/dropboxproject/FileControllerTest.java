package com.example.dropboxproject;

import com.example.dropboxproject.controller.FileController;
import com.example.dropboxproject.model.FileModel;
import com.example.dropboxproject.model.FolderModel;
import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.service.FileService;
import com.example.dropboxproject.service.FolderService;
import com.example.dropboxproject.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(FileController.class)
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @MockBean
    private FolderService folderService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "testuser")  // Mock kullanıcı ile oturum açmış gibi davranır
    public void testHomePageReturnsFilesAndFolders() throws Exception {
        UserModel user = new UserModel();
        user.setId(1L);
        user.setUsername("testuser");

        // Kullanıcı repository mockla
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // FileService sayfa döndürme mockla
        FileModel file1 = new FileModel();
        file1.setFileName("testfile.txt");
        file1.setSize(2048L);
        Page<FileModel> filesPage = new PageImpl<>(List.of(file1), PageRequest.of(0, 5), 1);
        Mockito.when(fileService.listUploadedFiles("testuser", 0, 5)).thenReturn(filesPage);

        // FolderService mockla
        FolderModel folder1 = new FolderModel();
        folder1.setName("Root Folder");
        Mockito.when(folderService.getRootFolders(user)).thenReturn(List.of(folder1));

        // HTTP GET isteği yap, model attribute'lar ve view kontrolü
        mockMvc.perform(get("/").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("files"))
                .andExpect(model().attributeExists("folders"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("totalItems"));
    }
}
