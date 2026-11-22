package br.com.marcosmpar.raizesdonordeste.api.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import br.com.marcosmpar.raizesdonordeste.domain.exception.CredenciaisInvalidasException;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Traduz exceções em respostas no padrão {@link ErroResponse}.
 * 400 = requisição malformada / tipo inválido; 422 = falha de validação de campos;
 * 404 = inexistente; 409 = regra de negócio; 401/403 = segurança.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final ErroFactory erros;

	public GlobalExceptionHandler(ErroFactory erros) {
		this.erros = erros;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
		List<ErroResponse.Detalhe> detalhes = ex.getBindingResult().getFieldErrors().stream()
				.map(f -> new ErroResponse.Detalhe(f.getField(), f.getDefaultMessage())).toList();
		return resposta(HttpStatus.UNPROCESSABLE_ENTITY, req, "VALIDACAO_INVALIDA",
				"Um ou mais campos são inválidos.", detalhes);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ErroResponse> validacaoParametros(HandlerMethodValidationException ex, HttpServletRequest req) {
		List<ErroResponse.Detalhe> detalhes = ex.getParameterValidationResults().stream()
				.flatMap(r -> r.getResolvableErrors().stream()
						.map(e -> new ErroResponse.Detalhe(r.getMethodParameter().getParameterName(),
								e.getDefaultMessage())))
				.toList();
		return resposta(HttpStatus.UNPROCESSABLE_ENTITY, req, "VALIDACAO_INVALIDA",
				"Um ou mais parâmetros são inválidos.", detalhes);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ErroResponse> restricoes(ConstraintViolationException ex, HttpServletRequest req) {
		List<ErroResponse.Detalhe> detalhes = ex.getConstraintViolations().stream()
				.map(v -> new ErroResponse.Detalhe(v.getPropertyPath().toString(), v.getMessage())).toList();
		return resposta(HttpStatus.UNPROCESSABLE_ENTITY, req, "VALIDACAO_INVALIDA",
				"Um ou mais parâmetros são inválidos.", detalhes);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException ex, HttpServletRequest req) {
		return resposta(HttpStatus.BAD_REQUEST, req, "REQUISICAO_INVALIDA",
				"Corpo da requisição ausente, malformado ou com valor inválido.", List.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
		return resposta(HttpStatus.BAD_REQUEST, req, "PARAMETRO_INVALIDO", "Parâmetro com tipo/valor inválido.",
				List.of(new ErroResponse.Detalhe(ex.getName(), "Valor inválido: " + ex.getValue())));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException ex,
			HttpServletRequest req) {
		return resposta(HttpStatus.BAD_REQUEST, req, "PARAMETRO_OBRIGATORIO", "Parâmetro obrigatório ausente.",
				List.of(new ErroResponse.Detalhe(ex.getParameterName(), "obrigatório")));
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
		return resposta(HttpStatus.NOT_FOUND, req, ex.getCode(), ex.getMessage(), List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
		return resposta(HttpStatus.NOT_FOUND, req, "ROTA_NAO_ENCONTRADA", "Rota não encontrada.", List.of());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest req) {
		return resposta(HttpStatus.METHOD_NOT_ALLOWED, req, "METODO_NAO_SUPORTADO",
				"Método HTTP não suportado para esta rota.", List.of());
	}

	@ExceptionHandler(RegraNegocioException.class)
	ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
		List<ErroResponse.Detalhe> detalhes = ex.getDetalhes().stream()
				.map(d -> new ErroResponse.Detalhe(d.field(), d.issue())).toList();
		return resposta(HttpStatus.CONFLICT, req, ex.getCode(), ex.getMessage(), detalhes);
	}

	@ExceptionHandler(CredenciaisInvalidasException.class)
	ResponseEntity<ErroResponse> credenciais(CredenciaisInvalidasException ex, HttpServletRequest req) {
		return resposta(HttpStatus.UNAUTHORIZED, req, ex.getCode(), ex.getMessage(), List.of());
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ErroResponse> naoAutenticado(AuthenticationException ex, HttpServletRequest req) {
		return resposta(HttpStatus.UNAUTHORIZED, req, "NAO_AUTENTICADO", "Autenticação necessária ou inválida.",
				List.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException ex, HttpServletRequest req) {
		return resposta(HttpStatus.FORBIDDEN, req, "ACESSO_NEGADO", "Sem permissão para este recurso.", List.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex, HttpServletRequest req) {
		log.warn("Violação de integridade em {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
		return resposta(HttpStatus.CONFLICT, req, "CONFLITO_DE_DADOS",
				"A operação conflita com dados existentes. Tente novamente.", List.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErroResponse> inesperado(Exception ex, HttpServletRequest req) {
		log.error("Erro inesperado em {}", req.getRequestURI(), ex);
		return resposta(HttpStatus.INTERNAL_SERVER_ERROR, req, "ERRO_INTERNO",
				"Erro interno. Informe o requestId ao suporte.", List.of());
	}

	private ResponseEntity<ErroResponse> resposta(HttpStatus status, HttpServletRequest req, String error,
			String message, List<ErroResponse.Detalhe> detalhes) {
		return ResponseEntity.status(status).body(erros.criar(req, error, message, detalhes));
	}
}
