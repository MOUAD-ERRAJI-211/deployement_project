package orthoproconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orthoproconnect.model.Document;
import orthoproconnect.model.Teacher;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByCategory(String category);

    List<Document> findByFileType(String fileType);

    List<Document> findByUploadedBy(Teacher teacher);

    List<Document> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String titleKeyword, String descriptionKeyword);

    List<Document> findByCategoryAndFileType(String category, String fileType);
}