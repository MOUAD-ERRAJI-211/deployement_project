package orthoproconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orthoproconnect.model.Test;
import orthoproconnect.model.Teacher;

import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findByLevel(String level);

    List<Test> findByCreatedBy(Teacher teacher);

    List<Test> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String titleKeyword, String descriptionKeyword);

    List<Test> findByQuestionsLessThanEqual(Integer maxQuestions);

    List<Test> findByDurationContaining(String durationKeyword);
}