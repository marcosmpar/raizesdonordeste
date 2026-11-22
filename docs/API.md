# Contrato da API

A documentação interativa e sempre atualizada é gerada do código: **http://localhost:8080/swagger-ui.html** (OpenAPI em `/v3/api-docs`). Este documento resume o contrato e traz exemplos reais.

## Convenções

- Recursos no **plural** e IDs no path (`/produtos/{id}`). JSON em UTF-8.
- **Autenticação:** `Authorization: Bearer <accessToken>` (JWT obtido em `POST /auth/login`). Perfis: `CLIENTE`, `ATENDENTE`, `COZINHA`, `GERENTE`, `ADMIN`.
- **Paginação** em todas as listagens: `?page=1&limit=10` (`page` ≥ 1; `limit` de 1 a 100). Resposta:

  ```json
  { "content": [ ... ], "page": 1, "limit": 10, "totalElements": 42, "totalPages": 5 }
  ```
- **Datas** em ISO-8601 UTC; valores monetários como números com 2 casas (ex.: `39.90`).
- Todo response traz o header `X-Request-Id` (o mesmo valor vai no campo `requestId` dos erros e na auditoria).

### Status codes

| Código | Quando |
|---|---|
| 200 / 201 / 204 | sucesso / criado / sem conteúdo |
| 202 | pagamento pendente por indisponibilidade do gateway |
| 400 | corpo malformado, enum ou parâmetro com valor inválido (ex.: `canalPedido=XYZ`) |
| 401 | sem token, token inválido/expirado ou credenciais inválidas |
| 403 | perfil sem permissão |
| 404 | recurso inexistente (também para pedidos de outro cliente) |
| 409 | conflito/regra de negócio (estoque insuficiente, transição inválida, duplicidade...) |
| 422 | falha de validação de campos (campo obrigatório ausente, quantidade negativa, e-mail inválido...) |
| 500 | erro inesperado (sem detalhes internos; use o `requestId`) |

### Padrão de erro (idêntico em todas as falhas)

```json
{
  "error": "ESTOQUE_INSUFICIENTE",
  "message": "Não há quantidade suficiente para um ou mais itens.",
  "details": [ { "field": "itens[0].quantidade", "issue": "Disponível: 1" } ],
  "timestamp": "2026-11-22T19:20:36.512Z",
  "path": "/pedidos",
  "requestId": "3f6c2c9e-6a1e-4e0b-9d57-0c1d7a2b5f10"
}
```

Códigos de `error`: `NAO_AUTENTICADO`, `ACESSO_NEGADO`, `CREDENCIAIS_INVALIDAS`, `VALIDACAO_INVALIDA`, `REQUISICAO_INVALIDA`, `PARAMETRO_INVALIDO`, `PARAMETRO_OBRIGATORIO`, `RECURSO_NAO_ENCONTRADO`, `ROTA_NAO_ENCONTRADA`, `METODO_NAO_SUPORTADO`, `EMAIL_JA_CADASTRADO`, `UNIDADE_JA_CADASTRADA`, `PRODUTO_JA_CADASTRADO`, `PROMOCAO_JA_CADASTRADA`, `UNIDADE_INATIVA`, `PRODUTO_INATIVO`, `PRODUTO_INDISPONIVEL`, `ESTOQUE_INSUFICIENTE`, `PEDIDO_NAO_PAGAVEL`, `TRANSICAO_INVALIDA`, `CANCELAMENTO_NAO_PERMITIDO`, `CLIENTE_INVALIDO`, `PROMOCAO_INVALIDA`, `FIDELIDADE_REQUER_CLIENTE`, `FIDELIDADE_SEM_CONSENTIMENTO`, `PONTOS_INSUFICIENTES`, `RESGATE_EXCEDE_TOTAL`, `CONFLITO_DE_DADOS`, `ERRO_INTERNO`.

## Endpoints por recurso

Legenda de acesso: **público** = sem token; **JWT** = qualquer perfil autenticado; demais = perfis permitidos.

### /auth e /usuarios

| Método e rota | Acesso | Finalidade | Status |
|---|---|---|---|
| `POST /auth/login` | público | Autentica e retorna o JWT | 200, 400/422, 401 |
| `POST /usuarios` | público | Autocadastro de cliente (sempre perfil CLIENTE) | 201, 409, 422 |
| `POST /usuarios/internos` | ADMIN | Cria usuário interno com perfil | 201, 401, 403, 409, 422 |
| `GET /usuarios/me` | JWT | Perfil do usuário autenticado | 200, 401 |

### /unidades, /produtos e cardápio

| Método e rota | Acesso | Finalidade | Status |
|---|---|---|---|
| `GET /unidades` | público | Lista unidades ativas (paginado) | 200, 422 |
| `GET /unidades/{id}` | público | Detalha unidade | 200, 404 |
| `POST /unidades` | ADMIN | Cria unidade | 201, 401, 403, 409, 422 |
| `PUT /unidades/{id}` | ADMIN | Atualiza unidade | 200, 404, 409, 422 |
| `GET /produtos` | público | Lista produtos ativos (paginado) | 200 |
| `GET /produtos/{id}` | público | Detalha produto | 200, 404 |
| `POST /produtos` | ADMIN, GERENTE | Cria produto | 201, 403, 409, 422 |
| `PUT /produtos/{id}` | ADMIN, GERENTE | Atualiza produto | 200, 404, 409, 422 |
| `DELETE /produtos/{id}` | ADMIN, GERENTE | Desativa produto (exclusão lógica) | 204, 404 |
| `GET /unidades/{unidadeId}/cardapio` | público | Itens disponíveis na unidade, com o preço da unidade | 200, 404, 409 |
| `PUT /unidades/{unidadeId}/cardapio/{produtoId}` | ADMIN, GERENTE | Define preço/disponibilidade (cria ou atualiza) | 200, 404, 409, 422 |

### /estoque

| Método e rota | Acesso | Finalidade | Status |
|---|---|---|---|
| `GET /estoque?unidadeId=` | ADMIN, GERENTE, ATENDENTE | Saldo por produto na unidade | 200, 404 |
| `GET /estoque/movimentacoes?unidadeId=&produtoId=` | ADMIN, GERENTE | Histórico de movimentações | 200, 404 |
| `POST /estoque/entradas` | ADMIN, GERENTE | Registra entrada | 201, 404, 422 |
| `POST /estoque/saidas` | ADMIN, GERENTE | Registra saída manual (perda/ajuste) | 201, 404, 409, 422 |

### /pedidos

| Método e rota | Acesso | Finalidade | Status |
|---|---|---|---|
| `POST /pedidos` | CLIENTE, ATENDENTE, GERENTE, ADMIN | Cria pedido (valida, calcula total, reserva estoque) | 201, 400, 401, 403, 404, 409, 422 |
| `GET /pedidos?canalPedido=&status=&unidadeId=` | JWT (CLIENTE vê só os seus) | Lista com filtros e paginação | 200, 400, 401 |
| `GET /pedidos/{id}` | JWT (CLIENTE só o seu) | Detalha com itens | 200, 401, 404 |
| `PATCH /pedidos/{id}/status` | COZINHA (só PRONTO), ATENDENTE, GERENTE, ADMIN | Avança para `PRONTO` ou `ENTREGUE` | 200, 403, 404, 409, 422 |
| `POST /pedidos/{id}/cancelamento` | CLIENTE (o seu, aguardando pagamento), ATENDENTE, GERENTE, ADMIN | Cancela, estorna pagamento e devolve estoque | 200, 403, 404, 409 |

### /pagamentos

| Método e rota | Acesso | Finalidade | Status |
|---|---|---|---|
| `POST /pagamentos` (+ header `Idempotency-Key`) | CLIENTE, ATENDENTE, GERENTE, ADMIN | Solicita o pagamento ao gateway mock | 201, 200, 202, 404, 409, 422 |
| `GET /pagamentos/{id}` | JWT (CLIENTE só os seus) | Consulta um pagamento | 200, 404 |
| `GET /pedidos/{pedidoId}/pagamentos` | JWT (CLIENTE só os seus) | Tentativas de pagamento do pedido | 200, 404 |

### /fidelidade (somente CLIENTE) e /promocoes

| Método e rota | Acesso | Finalidade | Status |
|---|---|---|---|
| `GET /fidelidade` | CLIENTE | Saldo, consentimento e regras | 200 |
| `POST /fidelidade/consentimento` | CLIENTE | Concede consentimento (LGPD) | 200 |
| `DELETE /fidelidade/consentimento` | CLIENTE | Revoga (saldo preservado) | 200 |
| `GET /fidelidade/historico` | CLIENTE | Acúmulos, resgates e estornos | 200 |
| `GET /promocoes` | público | Promoções vigentes (paginado) | 200 |
| `POST /promocoes` | ADMIN, GERENTE | Cria promoção | 201, 409, 422 |
| `DELETE /promocoes/{id}` | ADMIN, GERENTE | Encerra promoção | 204, 404 |

### /auditoria

| Método e rota | Acesso | Finalidade | Status |
|---|---|---|---|
| `GET /auditoria?entidade=&entidadeId=&acao=` | ADMIN, GERENTE | Consulta o log de ações sensíveis | 200, 403 |

## Exemplos

### POST /auth/login — público

```json
// request
{ "email": "cliente@raizes.com", "senha": "Senha@123" }

// 200
{ "accessToken": "eyJhbGciOiJIUzM4NCJ9...", "tokenType": "Bearer", "expiresIn": 3600,
  "user": { "id": 1, "nome": "Seed CLIENTE", "perfil": "CLIENTE" } }

// 401
{ "error": "CREDENCIAIS_INVALIDAS", "message": "E-mail ou senha inválidos.", "details": [],
  "timestamp": "2026-11-22T19:20:36Z", "path": "/auth/login", "requestId": "..." }
```

### POST /pedidos — fluxo crítico (JWT; CLIENTE, ATENDENTE, GERENTE, ADMIN)

Campos: `canalPedido` (obrigatório: APP, TOTEM, BALCAO, PICKUP, WEB), `unidadeId`, `itens[]` (`produtoId`, `quantidade` 1–99), `formaPagamento` (MOCK, PIX, CARTAO_CREDITO, CARTAO_DEBITO); opcionais: `clienteId` (CLIENTE usa o próprio; informar outro id → 403), `codigoPromocao`, `pontosResgatar`.

```json
// request
{ "canalPedido": "TOTEM", "unidadeId": 1,
  "itens": [ { "produtoId": 1, "quantidade": 2 }, { "produtoId": 5, "quantidade": 1 } ],
  "formaPagamento": "MOCK", "codigoPromocao": "BEMVINDO10" }

// 201
{ "pedidoId": 9001, "status": "AGUARDANDO_PAGAMENTO", "canalPedido": "TOTEM", "unidadeId": 1,
  "clienteId": 1, "formaPagamento": "MOCK",
  "subtotal": 89.70, "descontoPromocao": 8.97, "descontoFidelidade": 0.00,
  "codigoPromocao": "BEMVINDO10", "pontosResgatados": 0, "total": 80.73,
  "itens": [
    { "produtoId": 1, "nome": "Baião de Dois", "quantidade": 2, "precoUnitario": 39.90, "subtotal": 79.80 },
    { "produtoId": 5, "nome": "Suco de Cajá", "quantidade": 1, "precoUnitario": 9.90, "subtotal": 9.90 } ],
  "createdAt": "2026-11-22T19:14:01Z" }
```

Erros: `422` (`canalPedido` ausente, quantidade ≤ 0, itens vazios), `400` (`canalPedido` inválido), `404` (unidade/produto/promoção inexistente), `409` (`PRODUTO_INDISPONIVEL`, `ESTOQUE_INSUFICIENTE`, `PROMOCAO_INVALIDA`, `PONTOS_INSUFICIENTES`...), `401`, `403`.

Filtro: `GET /pedidos?canalPedido=APP&status=AGUARDANDO_PAGAMENTO&page=1&limit=10`.

### POST /pagamentos — gateway mock

Header opcional (recomendado): `Idempotency-Key: <texto único por tentativa, até 80 caracteres>`.

`simulacao` existe **somente porque o gateway é um mock**: `APROVADO` (padrão), `RECUSADO` ou `INDISPONIVEL`.

```json
// request
{ "pedidoId": 9001, "simulacao": "APROVADO" }

// 201 — processado
{ "pagamentoId": 55, "pedidoId": 9001, "status": "APROVADO", "statusPedido": "EM_PREPARO",
  "valor": 80.73, "formaPagamento": "MOCK",
  "gateway": { "transacaoId": "MOCK-110ab37e-0081-4035-aa56-12bf81f05d47", "codigo": "APPROVED",
               "mensagem": "Pagamento aprovado (simulado)." },
  "createdAt": "2026-11-22T19:18:21Z" }
```

| Resultado | Status HTTP | `status` do pagamento | `statusPedido` |
|---|---|---|---|
| Aprovado | 201 | `APROVADO` | `EM_PREPARO` |
| Recusado (`gateway.codigo = DECLINED`) | 201 | `RECUSADO` | `AGUARDANDO_PAGAMENTO` (nova tentativa permitida) |
| Gateway indisponível (`GATEWAY_UNAVAILABLE`) | 202 | `PENDENTE` | `AGUARDANDO_PAGAMENTO` — repetir com a mesma `Idempotency-Key` |
| Mesma `Idempotency-Key` já concluída | 200 | o original | — (nenhuma nova cobrança) |
| Pedido já pago/cancelado | 409 `PEDIDO_NAO_PAGAVEL` | — | — |

### PATCH /pedidos/{id}/status

```json
// request (COZINHA só pode PRONTO; ENTREGUE exige ATENDENTE/GERENTE/ADMIN)
{ "status": "PRONTO" }
// 200 → pedido completo com status PRONTO
// 409 TRANSICAO_INVALIDA se a transição não for permitida (ex.: ENTREGUE direto de EM_PREPARO)
```

`EM_PREPARO` só é alcançado pelo pagamento aprovado e `CANCELADO` pelo endpoint de cancelamento; pedi-los aqui retorna 409.

### POST /estoque/saidas — erro de estoque

```json
// request
{ "unidadeId": 1, "produtoId": 1, "quantidade": 999, "motivo": "Perda" }
// 409
{ "error": "ESTOQUE_INSUFICIENTE", "message": "Não há quantidade suficiente para um ou mais itens.",
  "details": [ { "field": "quantidade", "issue": "Disponível: 50" } ],
  "timestamp": "2026-11-22T19:09:27Z", "path": "/estoque/saidas", "requestId": "..." }
```

### GET /fidelidade

```json
{ "consentimento": true, "consentimentoEm": "2026-11-22T19:31:44Z", "revogadoEm": null, "saldoPontos": 44,
  "regras": { "pontosPorReal": 1, "valorPontoEmReais": 0.10 } }
```

### GET /auditoria?entidade=PEDIDO&entidadeId=9001 (ADMIN/GERENTE)

```json
{ "content": [
  { "id": 12, "atorId": 1, "acao": "PEDIDO_CRIADO", "entidade": "PEDIDO", "entidadeId": 9001,
    "detalhes": "canal=TOTEM unidade=1 itens=2 total=80.73 promo=BEMVINDO10",
    "requestId": "3f6c2c9e-...", "createdAt": "2026-11-22T19:14:01Z" } ],
  "page": 1, "limit": 10, "totalElements": 1, "totalPages": 1 }
```

Ações registradas: `PEDIDO_CRIADO`, `PEDIDO_STATUS_ALTERADO`, `PEDIDO_CANCELADO`, `PAGAMENTO_PROCESSADO`, `USUARIO_CRIADO`, `PROMOCAO_CRIADA`, `PROMOCAO_DESATIVADA`, `FIDELIDADE_RESGATE`, `FIDELIDADE_CONSENTIMENTO_CONCEDIDO`, `FIDELIDADE_CONSENTIMENTO_REVOGADO`. Os detalhes nunca contêm dados pessoais.
