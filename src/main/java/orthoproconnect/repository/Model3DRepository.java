package orthoproconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orthoproconnect.model.Model3D;
import orthoproconnect.model.Teacher;

import java.util.List;

@Repository
public interface Model3DRepository extends JpaRepository<Model3D, Long> {

    // Find all models created by a specific teacher
    List<Model3D> findByCreatedBy(Teacher teacher);

    // Find private models for a specific teacher
    List<Model3D> findByCreatedByAndIsPublicFalse(Teacher teacher);

    // Find public models for a specific teacher
    List<Model3D> findByCreatedByAndIsPublicTrue(Teacher teacher);

    // Find all public models
    List<Model3D> findByIsPublicTrue();

    // Find all models by category
    List<Model3D> findByCategory(String category);

    // Find public models by category
    List<Model3D> findByCategoryAndIsPublicTrue(String category);

    // Search models by name containing keyword
    List<Model3D> findByNameContainingIgnoreCase(String keyword);

    // Search public models by name
    List<Model3D> findByNameContainingIgnoreCaseAndIsPublicTrue(String keyword);
}