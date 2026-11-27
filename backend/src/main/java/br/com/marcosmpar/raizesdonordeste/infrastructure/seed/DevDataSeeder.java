package br.com.marcosmpar.raizesdonordeste.infrastructure.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import br.com.marcosmpar.raizesdonordeste.application.cardapio.CardapioService;
import br.com.marcosmpar.raizesdonordeste.application.estoque.EstoqueService;
import br.com.marcosmpar.raizesdonordeste.application.produto.ProdutoService;
import br.com.marcosmpar.raizesdonordeste.application.promocao.PromocaoService;
import br.com.marcosmpar.raizesdonordeste.domain.promocao.TipoPromocao;
import br.com.marcosmpar.raizesdonordeste.application.unidade.UnidadeService;
import br.com.marcosmpar.raizesdonordeste.application.usuario.UsuarioService;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.UsuarioRepository;

/** Seed APENAS do perfil dev: um usuário por perfil (senha "Senha@123"), 2 unidades, produtos e cardápios. */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
	private static final String SENHA = "Senha@123";

	private final UsuarioService usuarios;
	private final UsuarioRepository repo;
	private final UnidadeService unidades;
	private final ProdutoService produtos;
	private final CardapioService cardapio;
	private final EstoqueService estoque;
	private final PromocaoService promocoes;

	public DevDataSeeder(UsuarioService usuarios, UsuarioRepository repo, UnidadeService unidades,
			ProdutoService produtos, CardapioService cardapio, EstoqueService estoque,
			PromocaoService promocoes) {
		this.usuarios = usuarios;
		this.repo = repo;
		this.unidades = unidades;
		this.produtos = produtos;
		this.cardapio = cardapio;
		this.estoque = estoque;
		this.promocoes = promocoes;
	}

	@Override
	public void run(String... args) {
		for (Perfil p : Perfil.values()) {
			String email = p.name().toLowerCase() + "@raizes.com";
			if (!repo.existePorEmail(email)) {
				usuarios.cadastrar("Seed " + p.name(), email, SENHA, p);
				log.info("Seed: usuário {} criado", email);
			}
		}
		if (unidades.listar(false, 1, 1).totalElementos() == 0) {
			semearCatalogo();
		}
	}

	private void semearCatalogo() {
		var recife = unidades.criar("Raízes - Recife Antigo", "Recife", "PE", "Rua do Bom Jesus, 100", true);
		var salvador = unidades.criar("Raízes - Pelourinho", "Salvador", "BA", "Largo do Pelourinho, 20", true);
		var baiao = produtos.criar("Baião de Dois", "Arroz, feijão-de-corda e queijo coalho", "Pratos",
				new BigDecimal("39.90"));
		var carne = produtos.criar("Carne de Sol", "Carne de sol com macaxeira e vinagrete", "Pratos",
				new BigDecimal("49.90"));
		var tapioca = produtos.criar("Tapioca de Queijo", "Tapioca recheada com queijo coalho", "Lanches",
				new BigDecimal("14.90"));
		var cuscuz = produtos.criar("Cuscuz com Ovo", "Cuscuz nordestino com ovo mexido", "Lanches",
				new BigDecimal("16.50"));
		var caja = produtos.criar("Suco de Cajá", "Suco natural de cajá 400ml", "Bebidas", new BigDecimal("9.90"));
		for (var u : List.of(recife, salvador)) {
			cardapio.definir(u.getId(), baiao.getId(), baiao.getPrecoBase(), true);
			cardapio.definir(u.getId(), carne.getId(), carne.getPrecoBase(), true);
			cardapio.definir(u.getId(), tapioca.getId(), tapioca.getPrecoBase(), true);
			cardapio.definir(u.getId(), caja.getId(), caja.getPrecoBase(), true);
		}
		cardapio.definir(recife.getId(), cuscuz.getId(), cuscuz.getPrecoBase(), true);
		cardapio.definir(salvador.getId(), cuscuz.getId(), new BigDecimal("17.50"), false);
		for (var u : List.of(recife, salvador)) {
			for (var p : List.of(baiao, carne, tapioca, cuscuz, caja)) {
				estoque.registrarEntrada(u.getId(), p.getId(), 50, "Estoque inicial (seed)", null);
			}
		}
		var agora = Instant.now();
		promocoes.criar("BEMVINDO10", "10% de desconto em pedidos a partir de R$ 30,00", TipoPromocao.PERCENTUAL,
				new BigDecimal("10"), new BigDecimal("30.00"), null, agora.minus(1, ChronoUnit.DAYS),
				agora.plus(365, ChronoUnit.DAYS), null);
		promocoes.criar("RECIFE5", "R$ 5,00 de desconto na unidade Recife Antigo", TipoPromocao.VALOR_FIXO,
				new BigDecimal("5.00"), null, recife.getId(), agora.minus(1, ChronoUnit.DAYS),
				agora.plus(365, ChronoUnit.DAYS), null);
		log.info("Seed: 2 unidades, 5 produtos, cardápios, estoque inicial e 2 promoções criados");
	}
}
