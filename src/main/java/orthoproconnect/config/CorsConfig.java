// In your CorsConfig.java file, make sure you add the origin you're using:
// It looks like you're using http://127.0.0.1:5500

package orthoproconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        // Allow credentials like cookies
        config.setAllowCredentials(true);

        // Make sure to include your actual frontend origin
        config.addAllowedOrigin("http://localhost:5500");
        config.addAllowedOrigin("http://127.0.0.1:5500");

        // All your existing origins
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://localhost:8080");
        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedOrigin("http://localhost");
        config.addAllowedOrigin("http://localhost:5501");
        config.addAllowedOrigin("http://127.0.0.1:5501");
        config.addAllowedOrigin("http://localhost:63342");
        config.addAllowedOrigin("http://localhost:52330");

        // Explicitly allow all HTTP methods
        config.addAllowedMethod("*");
        // Allow all headers
        config.addAllowedHeader("*");
        // Add exposed headers for better CORS support with auth
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Disposition");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}