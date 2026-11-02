package com.Geopolitica.Proyecto.cache;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsultaCacheRepository extends JpaRepository<ConsultaCache, Long> {

	Optional<ConsultaCache> findByTipoAndClave(String tipo, String clave);
}
