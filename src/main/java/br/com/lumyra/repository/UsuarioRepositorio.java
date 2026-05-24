package br.com.lumyra.repository;

import br.com.lumyra.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Integer> {

    @Query("SELECT uu FROM USUARIO uu WHERE uu.email = :email")
    Optional<Usuario> buscarPorEmail(@Param("email") String email);

    @Query("SELECT COUNT(uu) > 0 FROM USUARIO uu WHERE uu.email = :email")
    boolean existePorEmail(@Param("email") String email);
}
