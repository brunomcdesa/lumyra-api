package br.com.lumyra.modules.identity.repository;

import br.com.lumyra.modules.identity.entity.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InviteRepositorio extends JpaRepository<Invite, Integer> {

    Optional<Invite> findByTokenHash(String tokenHash);

    void deleteByStudentId(Integer studentId);
}
