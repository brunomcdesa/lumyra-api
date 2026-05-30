package br.com.lumyra.modules.identity.repository;

import br.com.lumyra.modules.identity.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepositorio extends JpaRepository<Student, Integer> {

    Optional<Student> findByEmail(String email);

    /** Alunas marcadas para eliminação antes do limite de retenção (purge). */
    List<Student> findByDeletadoEmIsNotNullAndDeletadoEmBefore(OffsetDateTime limite);
}
