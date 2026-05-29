package br.com.lumyra.modules.identity.repository;

import br.com.lumyra.modules.identity.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepositorio extends JpaRepository<Tenant, Integer> {
}
