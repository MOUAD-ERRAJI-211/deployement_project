package orthoproconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orthoproconnect.model.Admin;
import orthoproconnect.model.Teacher;
import orthoproconnect.model.TeacherApproval;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherApprovalRepository extends JpaRepository<TeacherApproval, Long> {

    List<TeacherApproval> findByTeacher(Teacher teacher);

    List<TeacherApproval> findByAdmin(Admin admin);

    List<TeacherApproval> findByStatus(String status);

    Optional<TeacherApproval> findTopByTeacherOrderByDateCreatedDesc(Teacher teacher);
}