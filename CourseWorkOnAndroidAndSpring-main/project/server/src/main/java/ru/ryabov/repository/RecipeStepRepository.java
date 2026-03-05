package ru.ryabov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ryabov.model.RecipeStep;

import java.util.List;

@Repository
public interface RecipeStepRepository extends JpaRepository<RecipeStep, Long> {
    List<RecipeStep> findByPostIdOrderByOrderAsc(Long postId);
    void deleteByPostId(Long postId);
}
