package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.marcosmpar.raizesdonordeste.domain.usuario.Usuario;

interface UsuarioJpaRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByEmail(String email);

	boolean existsByEmail(String email);
}
