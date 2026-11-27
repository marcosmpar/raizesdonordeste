# Plano de Testes

Total: **55 cenários** (34 positivos e 21 negativos). Todos estão implementados como requisições com testes automáticos na coleção [`postman/raizes-do-nordeste.postman_collection.json`](postman/raizes-do-nordeste.postman_collection.json) (importe no Postman ou Insomnia, ou rode com newman).

## Como executar

1. Suba a API no perfil `dev` com banco vazio (apague `backend/data` se já existir): `cd backend && ./mvnw spring-boot:run`. O seed cria usuários, unidades, produtos, estoque e as promoções `BEMVINDO10` e `RECIFE5`.
2. Importe a coleção e execute a pasta inteira **na ordem** (Runner), ou via linha de comando:
   ```bash
   npx newman run docs/postman/raizes-do-nordeste.postman_collection.json
   ```
3. A coleção é reexecutável: cada execução cria seus próprios cliente, produto e pedidos (sufixo único) e guarda tokens e ids em variáveis de coleção.

Além da coleção, os mesmos fluxos (e outros, como concorrência) são cobertos por testes automatizados: `cd backend && ./mvnw test`.

## Cobertura dos requisitos do roteiro

| Requisito | Cenários |
|---|---|
| Autenticação e autorização (token válido, 401, 403) | T01-T05, T07, T09, T11, T16, T23, T29, T38, T43, T55 |
| Validação de dados (campo ausente, tipo/formato inválido) | T10, T18, T25, T26 |
| Regra do fluxo principal (pedido válido, 404, estoque insuficiente 409) | T24, T27, T28 |
| Pagamento mock aprovado/recusado e status do pedido | T32, T33, T34, T35, T41, T42 |
| Auditoria de ação sensível | T54 |

## Cenários

| ID | Tipo | Cenário | Endpoint | Pré-condição | Entrada | Esperado (status + resposta) | Evidência (coleção) |
|---|---|---|---|---|---|---|---|
| T01 | positivo | Login válido (cliente do seed) | `POST /auth/login` | Seed dev aplicado | {email, senha} | 200 + accessToken, tokenType Bearer, user.perfil CLIENTE | `1 - Auth/T01` |
| T02 | positivo | Login admin (guarda token) | `POST /auth/login` | Seed dev aplicado | {email, senha} | 200 + accessToken (ADMIN) | `1 - Auth/T02` |
| T03 | positivo | Login gerente (guarda token) | `POST /auth/login` | Seed dev aplicado | {email, senha} | 200 + accessToken (GERENTE) | `1 - Auth/T03` |
| T04 | positivo | Login atendente (guarda token) | `POST /auth/login` | Seed dev aplicado | {email, senha} | 200 + accessToken (ATENDENTE) | `1 - Auth/T04` |
| T05 | positivo | Login cozinha (guarda token) | `POST /auth/login` | Seed dev aplicado | {email, senha} | 200 + accessToken (COZINHA) | `1 - Auth/T05` |
| T06 | positivo | Cadastro público de cliente | `POST /usuarios` | — | {nome, email, senha} | 201 + perfil CLIENTE, sem senha/hash na resposta | `1 - Auth/T06` |
| T07 | positivo | Login do cliente recém-cadastrado (guarda token) | `POST /auth/login` | T06 executado | {email, senha} | 200 + accessToken | `1 - Auth/T07` |
| T08 | negativo | Login com senha errada | `POST /auth/login` | Seed dev aplicado | {email, senha incorreta} | 401 + error CREDENCIAIS_INVALIDAS | `1 - Auth/T08` |
| T09 | negativo | Acesso sem token | `GET /usuarios/me` | — | sem header Authorization | 401 + error NAO_AUTENTICADO | `1 - Auth/T09` |
| T10 | negativo | Cadastro com e-mail inválido e senha curta | `POST /usuarios` | — | {email inválido, senha < 8} | 422 + VALIDACAO_INVALIDA com 2 itens em details | `1 - Auth/T10` |
| T11 | negativo | CLIENTE tenta cadastrar usuário interno | `POST /usuarios/internos` | T07 executado | {perfil ADMIN} como CLIENTE | 403 + error ACESSO_NEGADO | `1 - Auth/T11` |
| T12 | positivo | ADMIN cadastra usuário interno | `POST /usuarios/internos` | T02 executado | {perfil COZINHA} | 201 + perfil COZINHA | `1 - Auth/T12` |
| T13 | positivo | Listar unidades (público, paginado) | `GET /unidades` | Seed dev aplicado | page=1&limit=10 | 200 + content[] e metadados de paginação | `2 - Catálogo/T13` |
| T14 | positivo | Cardápio da unidade (público) | `GET /unidades/{{unidadeId}}/cardapio` | T13 executado | path unidadeId | 200 + itens com preço da unidade | `2 - Catálogo/T14` |
| T15 | negativo | Unidade inexistente | `GET /unidades/999999` | — | id=999999 | 404 + RECURSO_NAO_ENCONTRADO | `2 - Catálogo/T15` |
| T16 | negativo | CLIENTE tenta criar produto | `POST /produtos` | T07 executado | {nome, categoria, precoBase} | 403 + ACESSO_NEGADO | `2 - Catálogo/T16` |
| T17 | positivo | GERENTE cria produto (guarda id) | `POST /produtos` | T03 executado | {nome, categoria, precoBase 20.00} | 201 + id | `2 - Catálogo/T17` |
| T18 | negativo | Produto com preço inválido | `POST /produtos` | T03 executado | {precoBase -5} | 422 + details[0].field = precoBase | `2 - Catálogo/T18` |
| T19 | positivo | GERENTE define preço do produto na unidade | `PUT /unidades/{{unidadeId}}/cardapio/{{produtoId}}` | T13 e T17 executados | {preco 20.00, disponivel true} | 200 + preco 20 | `2 - Catálogo/T19` |
| T20 | positivo | GERENTE registra entrada de estoque (10 un.) | `POST /estoque/entradas` | T17 e T19 executados | {unidadeId, produtoId, quantidade 10} | 201 + tipo ENTRADA + usuarioId | `3 - Estoque/T20` |
| T21 | positivo | Histórico de movimentações da unidade/produto | `GET /estoque/movimentacoes` | T20 executado | query unidadeId, produtoId | 200 + 1ª movimentação ENTRADA de 10 | `3 - Estoque/T21` |
| T22 | negativo | Saída maior que o saldo | `POST /estoque/saidas` | T20 executado | {quantidade 999} | 409 + ESTOQUE_INSUFICIENTE (Disponível: 10) | `3 - Estoque/T22` |
| T23 | negativo | CLIENTE tenta registrar entrada de estoque | `POST /estoque/entradas` | T07 executado | {quantidade 5} | 403 + ACESSO_NEGADO | `3 - Estoque/T23` |
| T24 | positivo | Criar pedido válido (canal TOTEM) | `POST /pedidos` | T07, T19 e T20 executados | {canalPedido TOTEM, 2 × produto de R$ 20,00} | 201 + AGUARDANDO_PAGAMENTO, total 40.00, canalPedido TOTEM | `4 - Pedidos/T24` |
| T25 | negativo | Pedido sem canalPedido | `POST /pedidos` | T07 executado | body sem canalPedido | 422 + details[0].field = canalPedido | `4 - Pedidos/T25` |
| T26 | negativo | Pedido com canalPedido inválido | `POST /pedidos` | T07 executado | {canalPedido XYZ} | 400 + REQUISICAO_INVALIDA | `4 - Pedidos/T26` |
| T27 | negativo | Pedido com produto inexistente | `POST /pedidos` | T07 executado | {produtoId 999999} | 404 + RECURSO_NAO_ENCONTRADO | `4 - Pedidos/T27` |
| T28 | negativo | Pedido com estoque insuficiente | `POST /pedidos` | T24 executado (restam 8 un.) | {quantidade 11} | 409 + ESTOQUE_INSUFICIENTE em itens[0].quantidade | `4 - Pedidos/T28` |
| T29 | negativo | Listar pedidos sem token | `GET /pedidos` | — | sem header Authorization | 401 + NAO_AUTENTICADO | `4 - Pedidos/T29` |
| T30 | positivo | Filtrar pedidos por canal e status | `GET /pedidos` | T24 executado | ?canalPedido=TOTEM&status=AGUARDANDO_PAGAMENTO | 200 + todos com canalPedido TOTEM | `4 - Pedidos/T30` |
| T31 | positivo | Detalhar o pedido (dono) | `GET /pedidos/{{pedidoId}}` | T24 executado | path pedidoId | 200 + itens com preço unitário 20.00 | `4 - Pedidos/T31` |
| T32 | positivo | Pagamento recusado | `POST /pagamentos` | T24 executado | {pedidoId, simulacao RECUSADO} | 201 + status RECUSADO, pedido segue AGUARDANDO_PAGAMENTO, gateway.codigo DECLINED | `5 - Pagamento (mock)/T32` |
| T33 | positivo | Novo pagamento aprovado (retentativa) | `POST /pagamentos` | T32 executado | {pedidoId, simulacao APROVADO} | 201 + APROVADO, pedido EM_PREPARO, gateway.transacaoId | `5 - Pagamento (mock)/T33` |
| T34 | positivo | Repetir a mesma Idempotency-Key | `POST /pagamentos` | T33 executado | mesma Idempotency-Key do T33 | 200 + mesmo pagamentoId (sem nova cobrança) | `5 - Pagamento (mock)/T34` |
| T35 | negativo | Pagar pedido que já foi pago | `POST /pagamentos` | T33 executado | {pedidoId} sem chave | 409 + PEDIDO_NAO_PAGAVEL | `5 - Pagamento (mock)/T35` |
| T36 | negativo | ENTREGUE antes de PRONTO (transição inválida) | `PATCH /pedidos/{{pedidoId}}/status` | T33 executado | {status ENTREGUE} | 409 + TRANSICAO_INVALIDA | `6 - Status do pedido/T36` |
| T37 | positivo | COZINHA marca PRONTO | `PATCH /pedidos/{{pedidoId}}/status` | T33 executado | {status PRONTO} | 200 + status PRONTO | `6 - Status do pedido/T37` |
| T38 | negativo | COZINHA tenta marcar ENTREGUE | `PATCH /pedidos/{{pedidoId}}/status` | T37 executado | {status ENTREGUE} | 403 + ACESSO_NEGADO | `6 - Status do pedido/T38` |
| T39 | positivo | ATENDENTE marca ENTREGUE | `PATCH /pedidos/{{pedidoId}}/status` | T37 executado | {status ENTREGUE} | 200 + status ENTREGUE | `6 - Status do pedido/T39` |
| T40 | positivo | Criar segundo pedido (APP) | `POST /pedidos` | T20 executado | {canalPedido APP, 1 un.} | 201 + AGUARDANDO_PAGAMENTO | `7 - Falha do gateway e cancelamento/T40` |
| T41 | positivo | Gateway indisponível (tolerância a falhas) | `POST /pagamentos` | T40 executado | {simulacao INDISPONIVEL} + Idempotency-Key | 202 + PENDENTE, pedido continua AGUARDANDO_PAGAMENTO | `7 - Falha do gateway e cancelamento/T41` |
| T42 | positivo | Retentativa com a mesma chave conclui o pagamento | `POST /pagamentos` | T41 executado | mesma Idempotency-Key do T41 | 201 + APROVADO, pedido EM_PREPARO | `7 - Falha do gateway e cancelamento/T42` |
| T43 | negativo | CLIENTE tenta cancelar pedido já pago | `POST /pedidos/{{pedidoId2}}/cancelamento` | T42 executado | path pedidoId2 | 409 + CANCELAMENTO_NAO_PERMITIDO | `7 - Falha do gateway e cancelamento/T43` |
| T44 | positivo | ATENDENTE cancela pedido pago (estorno + devolve estoque) | `POST /pedidos/{{pedidoId2}}/cancelamento` | T42 executado | path pedidoId2 | 200 + status CANCELADO | `7 - Falha do gateway e cancelamento/T44` |
| T45 | positivo | Pagamento foi estornado | `GET /pedidos/{{pedidoId2}}/pagamentos` | T44 executado | path pedidoId2 | 200 + [ESTORNADO] | `7 - Falha do gateway e cancelamento/T45` |
| T46 | positivo | GERENTE cria promoção | `POST /promocoes` | T03 executado | {codigo, tipo VALOR_FIXO, valor 3.00} | 201 + codigo | `8 - Promoções e fidelidade/T46` |
| T47 | positivo | Pedido com promoção BEMVINDO10 (10%, mín. R$ 30) | `POST /pedidos` | Seed dev aplicado; T20 executado | {canalPedido WEB, codigoPromocao BEMVINDO10, 2 × R$ 20,00} | 201 + subtotal 40.00, descontoPromocao 4.00, total 36.00 | `8 - Promoções e fidelidade/T47` |
| T48 | negativo | Promoção inexistente | `POST /pedidos` | T07 executado | {codigoPromocao NAOEXISTE} | 404 + RECURSO_NAO_ENCONTRADO | `8 - Promoções e fidelidade/T48` |
| T49 | positivo | Cliente concede consentimento de fidelidade (LGPD) | `POST /fidelidade/consentimento` | T07 executado | — | 200 + consentimento true, saldo 0 | `8 - Promoções e fidelidade/T49` |
| T50 | positivo | Pagar pedido com promoção | `POST /pagamentos` | T47 e T49 executados | {pedidoId} | 201 + APROVADO, valor 36.00 | `8 - Promoções e fidelidade/T50` |
| T51 | positivo | Saldo de pontos após o pagamento | `GET /fidelidade` | T50 executado | — | 200 + saldoPontos 36 (1 ponto por R$ 1,00) | `8 - Promoções e fidelidade/T51` |
| T52 | positivo | Resgatar 10 pontos no pedido (R$ 1,00 de desconto) | `POST /pedidos` | T51 executado | {pontosResgatar 10, 1 × R$ 20,00} | 201 + descontoFidelidade 1.00, total 19.00 | `8 - Promoções e fidelidade/T52` |
| T53 | negativo | Resgatar mais pontos do que o saldo | `POST /pedidos` | T52 executado (saldo 26) | {pontosResgatar 5000} | 409 + PONTOS_INSUFICIENTES | `8 - Promoções e fidelidade/T53` |
| T54 | positivo | GERENTE consulta auditoria do pedido | `GET /auditoria` | T24 a T39 executados | ?entidade=PEDIDO&entidadeId | 200 + PEDIDO_CRIADO e PEDIDO_STATUS_ALTERADO | `9 - Auditoria/T54` |
| T55 | negativo | CLIENTE tenta consultar auditoria | `GET /auditoria` | T07 executado | — | 403 + ACESSO_NEGADO | `9 - Auditoria/T55` |
