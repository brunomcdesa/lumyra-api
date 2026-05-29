package br.com.lumyra.modules.identity.repository;

import br.com.lumyra.modules.identity.entity.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfessionalRepositorio extends JpaRepository<Professional, Integer> {

    Optional<Professional> findByEmail(String email);
}
