package orthoproconnect.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import orthoproconnect.model.Teacher;
import orthoproconnect.repository.TeacherRepository;

/**
 * Creates a default teacher/admin account on first boot if none exists.
 * Credentials: admin@orthoproconnect.com / admin123
 * Change the password immediately after first login.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (teacherRepository.count() == 0) {
            Teacher admin = new Teacher();
            admin.setFirstname("Admin");
            admin.setLastname("OrthoPro");
            admin.setEmail("admin@orthoproconnect.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setInstitution("OrthoPro Academy");
            admin.setApproved(true);
            teacherRepository.save(admin);
            System.out.println("=== Default admin created: admin@orthoproconnect.com / admin123 ===");
            System.out.println("=== CHANGE THE PASSWORD AFTER FIRST LOGIN! ===");
        }
    }
}
