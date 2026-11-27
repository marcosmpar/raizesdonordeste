# Raízes do Nordeste

API REST (Spring Boot) para uma rede de restaurantes multicanal (APP, TOTEM, BALCAO, PICKUP, WEB).
Projeto acadêmico (Projeto Back End), mas deve ser tratado como solução profissional de mercado.
Fonte dos requisitos: `PROJETO.md` (roteiro original, não editar).

## Monorepo

- `backend/`: API Spring Boot (comandos Maven rodam dentro dela). `web/`: Next.js (front-end + BFF). `docs/` e `render.yaml` na raiz.
- Estado antes da reorganização: tag `v1.0-backend`.
- Web: `npm run dev|test|lint|build` em `web/`. Ler `web/AGENTS.md` antes de mexer no Next.js.
- BFF: rotas em `web/src/app/api` (lista fechada, sem proxy genérico), envelope `rota()` em `web/src/server`; JWT só em cookie httpOnly; sem regra de negócio no BFF.
- Servidores de preview: `.claude/launch.json` (`backend`, `web`).

## Stack (back-end)

- Java 17, Spring Boot 4.2.0, Maven (`./mvnw`)
- Pacote base: `br.com.marcosmpar.raizesdonordeste`
- A adicionar (pom hoje só tem webmvc + devtools + test): Spring Data JPA, Validation, Security + JWT,
  springdoc-openapi, Flyway, banco relacional (PostgreSQL; H2 para testes), Lombok (opcional)
- Comandos (em `backend/`): `./mvnw spring-boot:run`, `./mvnw test`

## Arquitetura (camadas obrigatórias, explícitas no código)

```
br.com.marcosmpar.raizesdonordeste
├── api/             Controllers, DTOs request/response, handler de erros, config Swagger/Security
├── application/     Casos de uso/serviços (CriarPedido, AtualizarStatus, ConfirmarPagamentoMock, AplicarFidelidade)
├── domain/          Entidades, enums, regras de negócio, exceções de domínio, interfaces de repositório/gateway
└── infrastructure/  Persistência (JPA, migrations), gateway de pagamento mock, segurança (JWT), auditoria
```

Regras: `api` → `application` → `domain`; `infrastructure` implementa interfaces do `domain`.
Domain não depende de Spring Web/DTOs. Controllers não contêm regra de negócio. Entidades nunca são expostas na API (usar DTOs).

## Domínio

Entidades: Usuario (perfil), Unidade, Produto, CardapioItem/ProdutoUnidade (preço/disponibilidade por unidade),
Estoque (por unidade+produto) e MovimentacaoEstoque, Pedido, ItemPedido, Pagamento (desacoplado do pedido),
ContaFidelidade/MovimentoPontos (com consentimento), Promocao, AuditoriaLog.

Perfis: `CLIENTE`, `ATENDENTE`, `COZINHA`, `GERENTE`, `ADMIN`.

Regras de negócio chave:
- `canalPedido` (ENUM: APP, TOTEM, BALCAO, PICKUP, WEB) é **obrigatório** na criação do pedido; inválido/ausente → 400/422. Listagem filtrável: `GET /pedidos?canalPedido=TOTEM&status=...`.
- Estoque é **por unidade**; pedido valida estoque, sem estoque → 409 `ESTOQUE_INSUFICIENTE`; o estoque é **reservado (baixa) na criação do pedido**, na mesma transação (falha desfaz tudo); é devolvido (entrada) apenas no **cancelamento** do pedido. Entrada manual via `/estoque`.
- Unidade/produto inexistente → 404. Total calculado no servidor (nunca confiar no cliente); preço unitário gravado no item.
- Status do pedido: `AGUARDANDO_PAGAMENTO → EM_PREPARO → PRONTO → ENTREGUE`, ou `CANCELADO`. Transições inválidas → 409. Quem atualiza: COZINHA/ATENDENTE/GERENTE.
- Pagamento **mock** (sem provedor real): registra solicitação, retorna status/payload (APROVADO/RECUSADO). Aprovado → pedido EM_PREPARO; recusado → pagamento RECUSADO com mensagem, pedido segue AGUARDANDO_PAGAMENTO (nova tentativa permitida; estoque continua reservado até cancelar). Idempotência (chave de idempotência) e tolerância a falha do gateway (timeout/retry, estado PENDENTE).
- Fidelidade: acúmulo e resgate simples de pontos, **apenas com consentimento** do cliente.
- Promoções: regra simples aplicada no cálculo do total, documentada no README.
- Auditoria: registrar ações sensíveis (criar pedido, mudar status, pagamento, movimentação de estoque).

## API — convenções (obrigatórias)

- Recursos no plural: `/auth`, `/usuarios`, `/unidades`, `/produtos`, `/estoque`, `/pedidos`, `/pagamentos`, `/fidelidade`
- ID no path (`/produtos/{id}`); paginação em listagens `?page=1&limit=10`
- Status: 200, 201, 204, 400/422 (validação), 401, 403, 404, 409 (regra de negócio)
- **Erro padronizado** em toda falha:
  ```json
  { "error": "NOME_DO_ERRO", "message": "legível", "details": [{"field": "campo", "issue": "problema"}],
    "timestamp": "ISO-8601", "path": "/rota", "requestId": "uuid" }
  ```
- Login: `POST /auth/login` → `{accessToken, tokenType: "Bearer", expiresIn, user{id,nome,perfil}}`
- Criar pedido (exemplo): `{ "canalPedido": "TOTEM", "clienteId": 12, "unidadeId": 2, "itens": [{"produtoId": 10, "quantidade": 2}], "formaPagamento": "MOCK" }`
- Swagger/OpenAPI gerado automaticamente, com exemplos e códigos de status refletindo o contrato real.

## Segurança e LGPD

- JWT stateless + autorização por role (`@PreAuthorize`/config); sem token → 401, sem permissão → 403
- Senha com BCrypt; nunca retornar senha/hash; respostas sem dados pessoais desnecessários
- Segredos via variáveis de ambiente (`.env.example`), nunca commitados
- Consentimento explícito para fidelidade; logs sem dados sensíveis

## Estado atual

- Passos 1 (base, erros, perfis), 2 (usuários + JWT + roles), 3 (unidades, produtos, cardápio por unidade) 4 (estoque por unidade) e 5 (pedidos) 6 (pagamento mock + status + cancelamento) 7 (fidelidade, promoções, auditoria) e 8 (documentação, coleção Postman, plano de testes) concluídos. Pendências manuais do usuário: URL do repositório público no README, preencher `docs/relatorio/capa.json` (instituição, curso, professor, cidade, RU) e as datas de acesso das referências em `docs/RELATORIO.md`, e regerar o PDF (`python3 docs/relatorio/gerar_relatorio_pdf.py`).
- GET de `/unidades/**` e `/produtos/**` são públicos; escritas: unidades só ADMIN; produtos e cardápio ADMIN/GERENTE. Produto usa exclusão lógica. Estoque: `/estoque` (saldo, ADMIN/GERENTE/ATENDENTE), `/estoque/entradas|saidas|movimentacoes` (ADMIN/GERENTE); saída usa lock pessimista, saldo nunca negativo (409 `ESTOQUE_INSUFICIENTE`); `EstoqueService.registrarSaida` será reutilizado pelo pedido. Seed dev: 50 un. de cada produto por unidade (ao mudar o seed, apagar `backend/data`).
- Pedidos: `POST /pedidos` (CLIENTE/ATENDENTE/GERENTE/ADMIN), `GET /pedidos` (filtros `canalPedido`, `status`, `unidadeId`), `GET /pedidos/{id}`. `clienteId` é opcional (CLIENTE usa o próprio id; outro id → 403). Total calculado com preço do cardápio da unidade; itens repetidos são somados; item guarda snapshot de nome/preço. CLIENTE só vê os próprios (404 nos alheios). Atualização de status e cancelamento ficam no passo 6.
- Pagamento: `POST /pagamentos {pedidoId, simulacao?}` + header opcional `Idempotency-Key`; 201 processado (APROVADO/RECUSADO), 200 repetição idempotente, 202 gateway indisponível (PENDENTE, repetir com a mesma chave). `simulacao` (APROVADO/RECUSADO/INDISPONIVEL) só existe por ser mock. `GET /pagamentos/{id}`, `GET /pedidos/{id}/pagamentos`. Pedido é bloqueado (lock) durante pagamento/status/cancelamento.
- Status: `PATCH /pedidos/{id}/status` (só PRONTO — COZINHA/ATENDENTE/GERENTE/ADMIN — e ENTREGUE — sem COZINHA); `POST /pedidos/{id}/cancelamento` (CLIENTE só o próprio e aguardando pagamento; equipe também EM_PREPARO, com estorno do pagamento e devolução de estoque). Máquina de estados em `StatusPedido.podeIrPara`; inválida → 409 `TRANSICAO_INVALIDA`.
- Total do pedido = subtotal (preço do cardápio) − promoção − resgate de pontos (nessa ordem). Campos opcionais no `POST /pedidos`: `codigoPromocao`, `pontosResgatar`.
- Promoções: `POST/DELETE /promocoes` (ADMIN/GERENTE), `GET /promocoes` público (só vigentes). Tipos PERCENTUAL/VALOR_FIXO, vigência, valor mínimo, unidade opcional; inválida → 409 `PROMOCAO_INVALIDA`, inexistente → 404. Seed dev: `BEMVINDO10` (10%, mín. R$ 30) e `RECIFE5` (R$ 5, só unidade 1).
- Fidelidade (só CLIENTE): `GET /fidelidade`, `POST|DELETE /fidelidade/consentimento`, `GET /fidelidade/historico`. 1 ponto por R$ 1,00 pago (acumula na aprovação do pagamento); 1 ponto = R$ 0,10 no resgate (debitado na criação do pedido). Sem consentimento não acumula nem resgata; revogar preserva saldo. Cancelamento devolve pontos resgatados e retira os acumulados.
- Auditoria: `AuditoriaService.registrar(ator, acao, entidade, id, detalhes)` na mesma transação da ação; `GET /auditoria?entidade=&entidadeId=&acao=` (ADMIN/GERENTE). Ações: PEDIDO_CRIADO/STATUS_ALTERADO/CANCELADO, PAGAMENTO_PROCESSADO, USUARIO_CRIADO, PROMOCAO_CRIADA/DESATIVADA, FIDELIDADE_*. Nunca registrar PII (e-mail/nome). Estoque usa o próprio livro de movimentações (com usuário) como trilha.
- Docs em `docs/` (REQUISITOS, ARQUITETURA com Mermaid, API, SEGURANCA_LGPD, PLANO_DE_TESTES, RELATORIO consolidado) e coleção em `docs/postman/` (55 cenários T01–T55; gerada por script — se mudar cenários, editar a coleção e o plano em conjunto). Rodar a coleção exige API em `dev` com seed.
- Diagramas: `docs/diagramas/*.png|svg`, gerados por `python3 docs/diagramas/gerar_diagramas.py` (sem dependências; PNG usa o Chrome).
- Deploy demo (só a API): `backend/Dockerfile` + `render.yaml` (Render free, perfil dev/H2 com seed); `server.port=${PORT:8080}` e `forward-headers-strategy=framework`; `GET /` redireciona ao Swagger. Imagem Docker não foi construída localmente (Docker ausente); o JAR foi validado simulando as variáveis do contêiner.
- Paginação `?page=1&limit=10` (limit máx. 100) via `PaginaResponse`.
- Seed dev (`DevDataSeeder`): `{cliente,atendente,cozinha,gerente,admin}@raizes.com`, senha `Senha@123`.
- Cadastro público `POST /usuarios` cria só CLIENTE; `POST /usuarios/internos` é ADMIN. Perfis por rota via `@PreAuthorize`.
- Validação de campos → 422; JSON malformado/enum inválido → 400.

## Persistência

- Migrations Flyway (`src/main/resources/db/migration`) + seed (usuários de cada perfil, unidades, produtos, estoque)
- Modelo do DER deve ser compatível com as migrations

## Escopo / MVP

Fluxo obrigatório escolhido: **Fluxo A — Pedido → Pagamento mock → Atualização de status**, completo e testável,
com persistência real, JWT com perfis e erro padronizado. Priorizar fluxo fechado sobre muitos endpoints.

## Entregáveis (checklist)

- [ ] Código em camadas, repo Git com ≥5 commits de evolução
- [ ] README: requisitos, `.env.example`, instalação, banco/migrations/seed, execução, URL do Swagger, como rodar testes, link do repo
- [ ] OpenAPI/Swagger
- [ ] Coleção Postman/Insomnia (`.json`) em pastas: Auth, Produtos, Pedidos, Pagamento, Erros
- [ ] DER (imagem/PDF), diagrama de casos de uso (Cliente, Atendente, Cozinha, Gerente/Admin, Gateway), diagrama de classes, sequência do fluxo crítico
- [ ] Descrição detalhada de ≥1 feature crítica (Realizar Pedido + Pagamento): fluxo, pré/pós-condições, exceções, regras
- [ ] Plano de testes: ≥10 cenários (≥6 positivos, ≥4 negativos), IDs T01…, refletidos na coleção e/ou testes automatizados
- [ ] Teste de auditoria (ou declarar “não implementado” com justificativa)
- [ ] Relatório final em PDF (ABNT: capa, sumário, seções, conclusão, referências) — nome `RU_Projeto_Back_End.PDF`

### Cenários de teste obrigatórios
Login válido; sem token (401); perfil sem permissão (403); campo obrigatório ausente e formato inválido (400/422);
pedido válido (201); produto/unidade inexistente (404); estoque insuficiente (409);
pagamento mock aprovado atualiza status; pagamento recusado com status/mensagem coerentes; auditoria de ação sensível.

## Convenções de trabalho

- Commits pequenos e frequentes, mensagens descritivas (histórico de evolução é avaliado)
- Teste junto com cada regra de negócio (JUnit + MockMvc); cobrir casos positivos e negativos
- Documentação do projeto em português; identificadores de domínio em português (Pedido, Unidade, canalPedido)
- Não implementar pagamento real

## Ordem sugerida de implementação

1. Dependências no pom, config de perfis/banco, tratamento global de erros
2. Usuário + auth JWT + roles
3. Unidades, produtos, cardápio por unidade
4. Estoque por unidade e movimentações
5. Pedido (canalPedido, validações, total, estoque)
6. Pagamento mock + atualização de status
7. Fidelidade, promoções, auditoria
8. Swagger, seed, coleção Postman, README, diagramas, relatório
