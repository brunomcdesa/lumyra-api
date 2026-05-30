package br.com.lumyra.modules.identity.repository;

import br.com.lumyra.modules.identity.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentRepositorio extends JpaRepository<Consent, Integer> {

    /** Consentimento ativo (não revogado) mais recente da aluna. */
    Optional<Consent> findFirstByStudentIdAndRevogadoEmIsNullOrderByRegistradoEmDesc(
        Integer studentId);

    List<Consent> findByStudentIdOrderByRegistradoEmDesc(Integer studentId);

    void deleteByStudentId(Integer studentId);
}
