package br.com.lumyra.modules.identity.repository;

import br.com.lumyra.modules.identity.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepositorio extends JpaRepository<Student, Integer> {
}
