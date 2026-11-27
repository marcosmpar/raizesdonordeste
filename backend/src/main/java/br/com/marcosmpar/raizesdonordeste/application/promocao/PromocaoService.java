package br.com.marcosmpar.raizesdonordeste.application.promocao;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.marcosmpar.raizesdonordeste.application.auditoria.AuditoriaService;
import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.promocao.Promocao;
import br.com.marcosmpar.raizesdonordeste.domain.promocao.PromocaoRepository;
import br.com.marcosmpar.raizesdonordeste.domain.promocao.TipoPromocao;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.UnidadeRepository;

@Service
public class PromocaoService {

	private final PromocaoRepository promocoes;
	private final UnidadeRepository unidades;
	private final AuditoriaService auditoria;

	public PromocaoService(PromocaoRepository promocoes, UnidadeRepository unidades, AuditoriaService auditoria) {
		this.promocoes = promocoes;
		this.unidades = unidades;
		this.auditoria = auditoria;
	}

	@Transactional
	public Promocao criar(String codigo, String descricao, TipoPromocao tipo, BigDecimal valor,
			BigDecimal valorMinimoPedido, Long unidadeId, Instant inicio, Instant fim, Long atorId) {
		if (promocoes.existePorCodigo(codigo)) {
			throw new RegraNegocioException("PROMOCAO_JA_CADASTRADA", "Já existe uma promoção com este código.");
		}
		Unidade unidade = unidadeId == null ? null
				: unidades.buscarPorId(unidadeId).orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", unidadeId));
		Promocao p = promocoes
				.salvar(new Promocao(codigo, descricao, tipo, valor, valorMinimoPedido, unidade, inicio, fim));
		auditoria.registrar(atorId, "PROMOCAO_CRIADA", "PROMOCAO", p.getId(),
				"codigo=" + p.getCodigo() + " tipo=" + tipo + " valor=" + valor);
		return p;
	}

	@Transactional
	public void desativar(Long id, Long atorId) {
		Promocao p = promocoes.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Promoção", id));
		p.desativar();
		promocoes.salvar(p);
		auditoria.registrar(atorId, "PROMOCAO_DESATIVADA", "PROMOCAO", id, "codigo=" + p.getCodigo());
	}

	@Transactional(readOnly = true)
	public Pagina<Promocao> listarVigentes(int page, int limit) {
		return promocoes.listarVigentes(Instant.now(), page, limit);
	}
}
