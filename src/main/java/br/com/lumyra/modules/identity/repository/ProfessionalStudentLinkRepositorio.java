package br.com.lumyra.modules.identity.repository;

import br.com.lumyra.modules.identity.entity.ProfessionalStudentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfessionalStudentLinkRepositorio
    extends JpaRepository<ProfessionalStudentLink, Integer> {
}
