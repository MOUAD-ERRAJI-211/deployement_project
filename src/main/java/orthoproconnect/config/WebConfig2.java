package orthoproconnect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig2 implements WebMvcConfigurer {

    @Value("${app.docs.dir}")
    private String docsDirectory;

    @Value("${app.tests.dir}")
    private String testsDirectory;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Exposer le répertoire 'docs' pour servir les fichiers directement
        exposeDirectory("docs", registry, docsDirectory);

        // Exposer le répertoire 'tests' pour servir les fichiers directement
        exposeDirectory("tests", registry, testsDirectory);
    }

    private void exposeDirectory(String dirName, ResourceHandlerRegistry registry, String dirPath) {
        Path uploadDir = Paths.get(dirPath);
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // Définir le chemin d'accès URL et le chemin physique du répertoire
        registry.addResourceHandler("/" + dirName + "/**")
                .addResourceLocations("file:/" + uploadPath + "/");
    }
}