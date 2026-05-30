package br.com.lumyra.modules.identity.repository;

import br.com.lumyra.modules.identity.entity.ProfessionalStudentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfessionalStudentLinkRepositorio
    extends JpaRepository<ProfessionalStudentLink, Integer> {

    boolean existsByProfessionalIdAndStudentId(Integer professionalId, Integer studentId);

    List<ProfessionalStudentLink> findByStudentId(Integer studentId);

    void deleteByStudentId(Integer studentId);
}
