package orthoproconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Service
public class FileService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.docs-dir:uploads/docs}")
    private String docsDirectory;

    @Value("${app.tests-dir:uploads/tests}")
    private String testsDirectory;

    public String saveFile(MultipartFile file, String directory) throws IOException {
        String targetDir = directory.equals("docs") ? docsDirectory : testsDirectory;
        Path dir = Paths.get(targetDir);
        Files.createDirectories(dir);
        String fileName = file.getOriginalFilename();
        Path filePath = dir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }

    public List<Map<String, Object>> listDocuments() throws IOException {
        return listFiles(docsDirectory);
    }

    public List<Map<String, Object>> listTests() throws IOException {
        return listFiles(testsDirectory);
    }

    private List<Map<String, Object>> listFiles(String directoryPath) throws IOException {
        List<Map<String, Object>> filesList = new ArrayList<>();
        Path directory = Paths.get(directoryPath);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            return filesList;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String fileName = file.getFileName().toString();
                    int dotIdx = fileName.lastIndexOf('.');
                    String nameNoExt = dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
                    String ext = dotIdx > 0 ? fileName.substring(dotIdx + 1).toLowerCase() : "";
                    long size = Files.size(file);
                    Map<String, Object> info = new HashMap<>();
                    info.put("name", fileName);
                    info.put("title", toTitleCase(nameNoExt.replace("_", " ").replace("-", " ")));
                    info.put("path", file.toString());
                    info.put("relativePath", "/" + directory.getFileName() + "/" + fileName);
                    info.put("type", ext);
                    info.put("size", formatSize(size));
                    info.put("lastModified", Files.getLastModifiedTime(file).toMillis());
                    if (directoryPath.equals(testsDirectory)) {
                        String level = guessLevel(fileName);
                        info.put("level", level);
                        info.put("duration", levelDuration(level));
                        info.put("questions", levelQuestions(level));
                    }
                    filesList.add(info);
                } catch (IOException e) { /* skip bad files */ }
            });
        }
        return filesList;
    }

    public boolean deleteFile(String filePath) {
        try { return Files.deleteIfExists(Paths.get(filePath)); }
        catch (IOException e) { return false; }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private String toTitleCase(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean next = true;
        for (char c : s.toCharArray()) {
            sb.append(Character.isSpaceChar(c) ? (next = true) && c : next ? (next = false) | (c = Character.toTitleCase(c)) | c : Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private String guessLevel(String name) {
        String n = name.toLowerCase();
        if (n.contains("advanced") || n.contains("expert") || n.contains("avance")) return "advanced";
        if (n.contains("intermediate") || n.contains("moyen")) return "intermediate";
        return "beginner";
    }
    private String levelDuration(String l) { return "advanced".equals(l) ? "60 min" : "intermediate".equals(l) ? "45 min" : "30 min"; }
    private int levelQuestions(String l) { return "advanced".equals(l) ? 35 : "intermediate".equals(l) ? 25 : 15; }
}
