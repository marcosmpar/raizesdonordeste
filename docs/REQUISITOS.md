# Requisitos

Levantamento com foco em regras de negócio, persistência, integrações e segurança (não em telas). Prioridade pelo método MoSCoW: **Must** (essencial ao fluxo crítico), **Should** (importante), **Could** (desejável).

## Requisitos funcionais

| ID | Requisito | Prioridade | Por quê | Situação | Endpoints |
|---|---|---|---|---|---|
| RF01 | Cadastro de usuários e autenticação (login) com perfis/roles | Must | Sem identidade não há controle de acesso nem rastreabilidade | Implementado | `/auth/login`, `/usuarios`, `/usuarios/internos`, `/usuarios/me` |
| RF02 | Gestão de unidades da rede | Must | Estoque, cardápio e pedidos são por unidade | Implementado | `/unidades` |
| RF03 | Produtos e cardápio por unidade (preço e disponibilidade próprios) | Must | O pedido precisa de preço vigente na unidade | Implementado | `/produtos`, `/unidades/{id}/cardapio` |
| RF04 | Pedido com itens, valores e status; canal de origem obrigatório (`canalPedido`) | Must | É o fluxo crítico do negócio e a base da multicanalidade | Implementado | `POST /pedidos` |
| RF05 | Consulta/filtro de pedidos (por canal, status, unidade) | Must | Acompanhamento operacional entre canais | Implementado | `GET /pedidos?canalPedido=` |
| RF06 | Atualização de status (cozinha → pronto → entregue) e cancelamento | Must | Operação da cozinha e do atendimento | Implementado | `PATCH /pedidos/{id}/status`, `POST /pedidos/{id}/cancelamento` |
| RF07 | Controle de estoque por unidade (entrada/saída) e restrição de venda sem saldo | Must | Evita vender o que não existe | Implementado | `/estoque` |
| RF08 | Pagamento via serviço externo (mock) com registro, status e payload de retorno | Must | Fecha o fluxo crítico sem depender de provedor real | Implementado | `/pagamentos` |
| RF09 | Programa de fidelidade (pontos, saldo, histórico, resgate) com consentimento | Should | Retenção de clientes; exige base legal (LGPD) | Implementado | `/fidelidade` |
| RF10 | Promoções/campanhas aplicáveis por código | Should | Regra comercial do cenário | Implementado | `/promocoes`, `codigoPromocao` no pedido |
| RF11 | Auditoria de ações sensíveis | Should | Rastreabilidade e responsabilização | Implementado | `/auditoria` |
| RF12 | Refresh token e logout | Could | Conforto de sessão; JWT de curta duração já atende o MVP | **Não implementado** | — |

### Multicanalidade (obrigatório)

`canalPedido` ∈ {`APP`, `TOTEM`, `BALCAO`, `PICKUP`, `WEB`}. É obrigatório na criação (ausente → 422; valor inválido → 400), fica gravado no pedido e pode ser usado como filtro (`GET /pedidos?canalPedido=TOTEM`).

## Requisitos não funcionais

| ID | Requisito | Como foi atendido |
|---|---|---|
| RNF01 | Segurança: autenticação por token e autorização por perfil | JWT (HS256) stateless; `@PreAuthorize` por rota; 401/403 no padrão de erro |
| RNF02 | Senha com hash | BCrypt; o hash nunca é retornado |
| RNF03 | LGPD: minimização e consentimento | Fidelidade só com consentimento (revogável); respostas sem dados desnecessários; auditoria e logs sem e-mail/nome; CLIENTE só vê os próprios pedidos |
| RNF04 | Logs/auditoria de ações sensíveis | Tabela `auditoria_logs` gravada na mesma transação da ação, com `requestId` |
| RNF05 | Desempenho em horários de pico | Paginação em listagens, índices nos filtros, consultas com fetch join, lock pessimista curto só onde há disputa (estoque, pedido, conta de pontos) |
| RNF06 | Consistência sob concorrência | Saídas de estoque e pagamentos simultâneos são serializados por lock; testes automatizados comprovam (10 saídas concorrentes de 1 un. sobre saldo 5 → exatamente 5 sucessos; 6 pagamentos simultâneos → 1 aprovado) |
| RNF07 | Tolerância a falhas na integração de pagamento | Gateway indisponível gera pagamento `PENDENTE` (202) sem perder o pedido; repetição com a mesma `Idempotency-Key` conclui sem cobrar em duplicidade |
| RNF08 | Documentação | OpenAPI/Swagger gerado do código + documentos em `docs/` |
| RNF09 | Erro padronizado | Mesmo JSON em todas as falhas (`error`, `message`, `details`, `timestamp`, `path`, `requestId`) |
| RNF10 | Reprodutibilidade | Banco H2 embutido em `dev`, seed automático, coleção Postman reexecutável |

## Regras de negócio principais

1. O total é **sempre calculado no servidor**: subtotal (preço do cardápio da unidade) − promoção − resgate de pontos. O item guarda uma cópia do nome e do preço.
2. O estoque é **reservado na criação do pedido**, na mesma transação. Qualquer falha desfaz tudo (pedido, baixas, pontos, auditoria). O cancelamento devolve o estoque.
3. Status do pedido: `AGUARDANDO_PAGAMENTO → EM_PREPARO → PRONTO → ENTREGUE`; `CANCELADO` é possível antes de `PRONTO`. Transição inválida → 409 `TRANSICAO_INVALIDA`.
4. Pagamento aprovado leva o pedido a `EM_PREPARO`. Recusado mantém `AGUARDANDO_PAGAMENTO` (nova tentativa permitida). Cancelar pedido pago estorna o pagamento.
5. CLIENTE só cancela o próprio pedido e apenas enquanto aguarda pagamento; a equipe também cancela pedido em preparo.
6. Fidelidade: 1 ponto por R$ 1,00 pago (acúmulo na aprovação do pagamento); 1 ponto resgatado = R$ 0,10 de desconto. Sem consentimento não acumula nem resgata.
7. Promoção: percentual ou valor fixo sobre o subtotal, com vigência, valor mínimo e unidade opcionais; o desconto nunca excede o subtotal.

## Limitações conhecidas (e o que seria o próximo passo)

- Pedido não pago **não expira**: o estoque reservado só volta com o cancelamento. Próximo passo: expiração automática por tempo.
- A chamada ao gateway ocorre dentro da transação (com o pedido bloqueado). É aceitável para um mock instantâneo; com um gateway real seria necessário separar a chamada externa da transação.
- Sem refresh token/logout, sem limite de tentativas de login (rate limiting) e sem HTTPS próprio (esperado atrás de um proxy/ingress).
- O perfil `prod` (PostgreSQL) não foi exercitado contra um servidor real neste ambiente.
- A permissão de equipe não é restrita por unidade (um ATENDENTE vê pedidos de qualquer unidade).
