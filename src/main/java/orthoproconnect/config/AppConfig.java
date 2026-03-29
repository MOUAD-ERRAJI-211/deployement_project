package orthoproconnect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Single unified config — replaces the 6 separate config files that were in the original project.
 * CORS origins come from the CORS_ORIGINS env var so no code changes are needed when deploying.
 */
@Configuration
@EnableWebSecurity
public class AppConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsRaw;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.models-dir:uploads/models}")
    private String modelsDir;

    @Value("${app.qrcode-dir:uploads/qr-codes}")
    private String qrcodeDir;

    // ── Security ────────────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // ── CORS ─────────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse comma-separated origins from env var
        List<String> origins = Arrays.asList(allowedOriginsRaw.split(","));
        origins.replaceAll(String::trim);
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── Static resources (uploaded files served over HTTP) ───────────────

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded piece images: GET /uploads/pieces/image.jpg
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600);

        // Serve 3D models: GET /models/filename.stl
        registry.addResourceHandler("/models/**")
                .addResourceLocations("file:" + modelsDir + "/")
                .setCachePeriod(3600);

        // Serve QR code images: GET /qr-codes/student_123.png
        registry.addResourceHandler("/qr-codes/**")
                .addResourceLocations("file:" + qrcodeDir + "/")
                .setCachePeriod(3600);
    }
}
