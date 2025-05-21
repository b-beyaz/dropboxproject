package com.example.dropboxproject;

import com.example.dropboxproject.model.FileModel;
import com.example.dropboxproject.model.FolderModel;
import com.example.dropboxproject.model.UserModel;
import com.example.dropboxproject.repository.FileRepository;
import com.example.dropboxproject.repository.UserRepository;
import com.example.dropboxproject.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileServiceTest {

    private FileService fileService;
    private FileRepository fileRepository;
    private UserRepository userRepository;
    private JavaMailSender mailSender;

    @TempDir
    Path tempDir; // Geçici yükleme dizini

    @BeforeEach
    void setUp() {
        fileRepository = mock(FileRepository.class);
        userRepository = mock(UserRepository.class);
        mailSender = mock(JavaMailSender.class);

        fileService = new FileService(fileRepository, userRepository, mailSender);
        fileService.setUploadDir(tempDir.toString());
        fileService.init();

    }

    @Test
    void testSaveFile() throws IOException {
        // Arrange
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello, World!".getBytes());
        UserModel user = new UserModel();
        FolderModel folder = new FolderModel();

        ArgumentCaptor<FileModel> captor = ArgumentCaptor.forClass(FileModel.class);
        when(fileRepository.save(any(FileModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FileModel savedFile = fileService.saveFile(multipartFile, user, folder);

        // Assert
        assertNotNull(savedFile);
        assertEquals("test.txt", savedFile.getOriginalFilename());
        assertEquals("text/plain", savedFile.getFileType());
        assertEquals(user, savedFile.getUser());
        assertEquals(folder, savedFile.getFolder());
        assertTrue(Files.exists(tempDir.resolve(savedFile.getFileName())));
    }

    @Test
    void testLoadFileAsResource() throws Exception {
        // Arrange
        String fileName = UUID.randomUUID().toString();
        Path filePath = tempDir.resolve(fileName);
        Files.write(filePath, "Sample content".getBytes());

        UserModel user = new UserModel();
        user.setUsername("testuser");

        FileModel fileModel = new FileModel();
        fileModel.setFileName(fileName);
        fileModel.setUser(user);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(fileRepository.findByFileNameAndUser(fileName, user)).thenReturn(Optional.of(fileModel));

        // Act
        Resource resource = fileService.loadFileAsResource(fileName, "testuser");

        // Assert
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
        assertEquals(fileName, Path.of(resource.getURI()).getFileName().toString());
    }

    @Test
    void testDeleteFile() throws IOException {
        // Arrange
        String fileName = UUID.randomUUID().toString();
        Path filePath = tempDir.resolve(fileName);
        Files.write(filePath, "To be deleted".getBytes());

        UserModel user = new UserModel();
        user.setUsername("deleter");

        FileModel fileModel = new FileModel();
        fileModel.setFileName(fileName);
        fileModel.setUser(user);

        when(userRepository.findByUsername("deleter")).thenReturn(Optional.of(user));
        when(fileRepository.findByIdAndUser(anyLong(), eq(user))).thenReturn(Optional.of(fileModel));

        // Act
        boolean result = fileService.deleteFile(1L, "deleter");

        // Assert
        assertTrue(result);
        assertFalse(Files.exists(filePath));
        verify(fileRepository).delete(fileModel);
    }

    @Test
    void testIsFileOwnedByUser_returnsTrueIfOwned() {
        // Arrange
        String fileName = "somefile.txt";
        String username = "ayse";

        UserModel user = new UserModel();
        user.setUsername(username);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(fileRepository.findByFileNameAndUser(fileName, user)).thenReturn(Optional.of(new FileModel()));

        // Act
        boolean owned = fileService.isFileOwnedByUser(fileName, username);

        // Assert
        assertTrue(owned);
    }

    @Test
    void testReadFileContent() throws IOException {
        // Arrange
        String fileName = "content.txt";
        String content = "This is test content.";
        Path filePath = tempDir.resolve(fileName);
        Files.write(filePath, content.getBytes());

        FileModel file = new FileModel();
        file.setFileName(fileName);

        // Act
        String result = fileService.readFileContent(file);

        // Assert
        assertEquals(content, result);
    }
}
