# Arquitetura e diagramas

Os diagramas estão em [Mermaid](https://mermaid.js.org/) e são renderizados automaticamente pelo GitHub/GitLab. Para obter imagem ou PDF (ex.: para o relatório), cole o bloco em https://mermaid.live e exporte.

## 1. Arquitetura em camadas

```mermaid
flowchart TB
    subgraph API["api — interface"]
        C["Controllers + DTOs<br/>Segurança (JWT, roles)<br/>GlobalExceptionHandler<br/>OpenAPI"]
    end
    subgraph APP["application — casos de uso"]
        S["PedidoService · PagamentoService · EstoqueService<br/>FidelidadeService · PromocaoService<br/>AutenticacaoService · UsuarioService · AuditoriaService"]
    end
    subgraph DOM["domain — regras de negócio"]
        D["Entidades (Pedido, Estoque, Promocao...)<br/>Máquina de estados do pedido<br/>Exceções de domínio<br/>Portas: *Repository"]
    end
    subgraph INF["infrastructure — detalhes técnicos"]
        I["Adapters JPA + migrations Flyway<br/>JwtService / filtro JWT<br/>GatewayPagamentoMock<br/>Seed de desenvolvimento"]
    end
    API --> APP --> DOM
    INF -. implementa portas .-> DOM
    INF -. implementa EmissorToken / GatewayPagamento .-> APP
```

| Camada | Responsabilidade | Exemplos |
|---|---|---|
| **api** | Rotas, contratos request/response, validação de entrada, autenticação/autorização, mapeamento de exceções para o padrão de erro, Swagger | `PedidoController`, `GlobalExceptionHandler`, `SecurityConfig` |
| **application** | Orquestra o caso de uso e a transação; não conhece HTTP | `PedidoService.criar`, `PagamentoService.pagar` |
| **domain** | Entidades, validações e comportamentos (ex.: `Estoque.saida`, `Pedido.transitarPara`, `Promocao.validarPara`); define as portas de persistência | `Pedido`, `StatusPedido.podeIrPara` |
| **infrastructure** | Persistência (JPA), integrações (gateway mock), JWT e seed | `PedidoRepositoryAdapter`, `GatewayPagamentoMock` |

Sentido das dependências: `api → application → domain`. A infraestrutura implementa interfaces definidas no domínio/aplicação (inversão de dependência), por isso trocar o gateway mock por um real não afeta as regras.

## 2. Diagrama de casos de uso

```mermaid
flowchart LR
    Cli(["Cliente<br/>App · Web · Totem"])
    Atd(["Atendente<br/>Balcão"])
    Coz(["Cozinha"])
    Ger(["Gerente / Administrador"])
    Gw(["Gateway de pagamento<br/>(sistema externo)"])

    subgraph SIS["Sistema Raízes do Nordeste"]
        UC1(["Cadastrar-se / autenticar"])
        UC2(["Consultar cardápio da unidade"])
        UC3(["Realizar pedido"])
        UC4(["Solicitar pagamento"])
        UC5(["Acompanhar / consultar pedidos"])
        UC6(["Cancelar pedido"])
        UC7(["Atualizar status do pedido"])
        UC8(["Consultar e movimentar estoque"])
        UC9(["Gerir unidades, produtos e cardápio"])
        UC10(["Gerir promoções"])
        UC11(["Participar da fidelidade<br/>(consentir, ver saldo, resgatar)"])
        UC12(["Consultar auditoria"])
        UC13(["Gerir usuários internos"])
    end

    Cli --- UC1 & UC2 & UC3 & UC4 & UC5 & UC6 & UC11
    Atd --- UC2 & UC3 & UC4 & UC5 & UC6 & UC7 & UC8
    Coz --- UC5 & UC7
    Ger --- UC8 & UC9 & UC10 & UC12 & UC5 & UC6 & UC7
    Gw --- UC4
    UC3 -.->|include| UC2
    UC4 -.->|extend: falha do gateway| UC4
```

### Descrição da feature crítica — UC03/UC04: Realizar pedido + Solicitar pagamento

| Item | Descrição |
|---|---|
| **Atores** | Principal: Cliente (ou Atendente no balcão). Secundário: Gateway de pagamento (mock) |
| **Pré-condições** | Usuário autenticado (CLIENTE, ATENDENTE, GERENTE ou ADMIN); unidade ativa; produtos ofertados e disponíveis no cardápio da unidade; saldo de estoque suficiente |
| **Pós-condições (sucesso)** | Pedido persistido com itens, valores e `canalPedido`; estoque reservado; pagamento registrado; pedido `EM_PREPARO`; pontos de fidelidade acumulados (se houver consentimento); ações auditadas |

**Fluxo principal**

1. O ator envia `POST /pedidos` com `canalPedido`, `unidadeId`, itens e `formaPagamento` (opcionalmente `codigoPromocao` e `pontosResgatar`).
2. O sistema valida os campos, a unidade e o cliente.
3. O sistema resolve cada item no cardápio da unidade e obtém o preço vigente.
4. O sistema calcula o total (subtotal − promoção − resgate de pontos) e grava o pedido com status `AGUARDANDO_PAGAMENTO`.
5. O sistema baixa o estoque de cada item (com bloqueio) e grava a auditoria `PEDIDO_CRIADO`. Responde `201`.
6. O ator envia `POST /pagamentos` com o `pedidoId` (e o header `Idempotency-Key`).
7. O sistema bloqueia o pedido, envia a solicitação ao gateway mock e recebe status e payload.
8. Pagamento **aprovado**: o sistema registra o pagamento, move o pedido para `EM_PREPARO`, acumula pontos e audita. Responde `201` com `status=APROVADO` e o retorno do gateway.
9. A cozinha marca `PRONTO` e o atendente marca `ENTREGUE` (`PATCH /pedidos/{id}/status`).

**Fluxos alternativos e exceções**

| Situação | Tratamento | Resposta |
|---|---|---|
| `canalPedido` ausente / valor fora do enum | Rejeita antes de qualquer efeito | 422 / 400 |
| Quantidade ≤ 0, lista de itens vazia | Rejeita | 422 |
| Unidade ou produto inexistente | Rejeita | 404 |
| Produto fora do cardápio ou indisponível; unidade inativa | Rejeita | 409 `PRODUTO_INDISPONIVEL` / `UNIDADE_INATIVA` |
| Estoque insuficiente | Desfaz tudo (nenhuma baixa parcial) | 409 `ESTOQUE_INSUFICIENTE` (`itens[i].quantidade`) |
| Promoção inválida/expirada; pontos insuficientes ou sem consentimento | Desfaz tudo | 409 / 404 |
| Pagamento **recusado** | Registra a tentativa `RECUSADO`; pedido permanece `AGUARDANDO_PAGAMENTO`; cliente pode tentar de novo ou cancelar | 201 `status=RECUSADO` |
| Gateway **indisponível** | Registra `PENDENTE`; pedido não é perdido; repetir com a mesma `Idempotency-Key` | 202 `status=PENDENTE` |
| Requisição repetida (mesma `Idempotency-Key`) | Devolve o resultado original, sem nova cobrança | 200 |
| Pedido já pago / cancelado | Rejeita | 409 `PEDIDO_NAO_PAGAVEL` |
| Pagamentos simultâneos do mesmo pedido | Serializados por lock; só um aprova | 409 para os demais |

**Regras de negócio:** idempotência do pagamento; total sempre calculado no servidor; reserva de estoque atômica; transições de status validadas; CLIENTE só acessa os próprios pedidos (404 nos alheios).

## 3. Modelo de dados (DER)

```mermaid
erDiagram
    USUARIOS ||--o{ PEDIDOS : "faz (cliente_id, opcional)"
    USUARIOS ||--o| CONTAS_FIDELIDADE : possui
    USUARIOS ||--o{ MOVIMENTOS_PONTOS : acumula
    USUARIOS ||--o{ MOVIMENTACOES_ESTOQUE : registra
    UNIDADES ||--o{ CARDAPIO_ITENS : oferece
    PRODUTOS ||--o{ CARDAPIO_ITENS : "é ofertado em"
    UNIDADES ||--o{ ESTOQUES : "tem estoque próprio"
    PRODUTOS ||--o{ ESTOQUES : "saldo de"
    UNIDADES ||--o{ MOVIMENTACOES_ESTOQUE : "ocorre em"
    PRODUTOS ||--o{ MOVIMENTACOES_ESTOQUE : movimenta
    UNIDADES ||--o{ PEDIDOS : recebe
    PEDIDOS ||--|{ ITENS_PEDIDO : contem
    PRODUTOS ||--o{ ITENS_PEDIDO : "referenciado por"
    PEDIDOS ||--o{ PAGAMENTOS : "tentativas (desacoplado)"
    PROMOCOES |o--o{ PEDIDOS : "aplicada em"
    UNIDADES |o--o{ PROMOCOES : "restringe (opcional)"
    PEDIDOS ||--o{ MOVIMENTOS_PONTOS : origina

    USUARIOS {
        bigint id PK
        varchar nome
        varchar email UK
        varchar senha_hash
        varchar perfil "CLIENTE|ATENDENTE|COZINHA|GERENTE|ADMIN"
        boolean ativo
        timestamptz criado_em
    }
    UNIDADES {
        bigint id PK
        varchar nome UK
        varchar cidade
        varchar uf
        varchar endereco
        boolean ativa
    }
    PRODUTOS {
        bigint id PK
        varchar nome UK
        varchar descricao
        varchar categoria
        numeric preco_base "CHECK > 0"
        boolean ativo "exclusão lógica"
    }
    CARDAPIO_ITENS {
        bigint id PK
        bigint unidade_id FK
        bigint produto_id FK
        numeric preco "CHECK > 0"
        boolean disponivel
    }
    ESTOQUES {
        bigint id PK
        bigint unidade_id FK
        bigint produto_id FK
        int quantidade "CHECK >= 0"
    }
    MOVIMENTACOES_ESTOQUE {
        bigint id PK
        bigint unidade_id FK
        bigint produto_id FK
        varchar tipo "ENTRADA|SAIDA"
        int quantidade "CHECK > 0"
        varchar motivo
        bigint usuario_id FK
        timestamptz criado_em
    }
    PEDIDOS {
        bigint id PK
        bigint cliente_id FK "opcional"
        bigint unidade_id FK
        varchar canal_pedido "APP|TOTEM|BALCAO|PICKUP|WEB"
        varchar status
        varchar forma_pagamento
        numeric subtotal
        numeric desconto_promocao
        numeric desconto_fidelidade
        int pontos_resgatados
        bigint promocao_id FK "opcional"
        varchar codigo_promocao
        numeric total "CHECK >= 0"
    }
    ITENS_PEDIDO {
        bigint id PK
        bigint pedido_id FK
        bigint produto_id FK
        varchar produto_nome "cópia"
        int quantidade "CHECK > 0"
        numeric preco_unitario "cópia"
    }
    PAGAMENTOS {
        bigint id PK
        bigint pedido_id FK
        varchar status "PENDENTE|APROVADO|RECUSADO|ESTORNADO"
        numeric valor
        varchar chave_idempotencia "UK com pedido_id"
        varchar transacao_gateway
        varchar codigo_gateway
        varchar mensagem
    }
    PROMOCOES {
        bigint id PK
        varchar codigo UK
        varchar tipo "PERCENTUAL|VALOR_FIXO"
        numeric valor
        numeric valor_minimo_pedido
        bigint unidade_id FK "opcional"
        timestamptz inicio
        timestamptz fim
        boolean ativa
    }
    CONTAS_FIDELIDADE {
        bigint usuario_id PK, FK
        boolean consentimento
        timestamptz consentimento_em
        timestamptz revogado_em
        int saldo_pontos "CHECK >= 0"
    }
    MOVIMENTOS_PONTOS {
        bigint id PK
        bigint usuario_id FK
        varchar tipo "ACUMULO|RESGATE|ESTORNO"
        int pontos "com sinal"
        bigint pedido_id FK
    }
    AUDITORIA_LOGS {
        bigint id PK
        bigint ator_id
        varchar acao
        varchar entidade
        bigint entidade_id
        varchar detalhes "sem PII"
        varchar request_id
        timestamptz criado_em
    }
```

Observações de modelagem:

- **Estoque por unidade:** `estoques` tem chave única (`unidade_id`, `produto_id`); o saldo nunca fica negativo (`CHECK`). Toda alteração gera uma linha em `movimentacoes_estoque` (livro de movimentações).
- **Pagamento desacoplado:** `pagamentos` é uma tabela própria; um pedido pode ter várias tentativas (ex.: recusado e depois aprovado). A chave (`pedido_id`, `chave_idempotencia`) é única.
- **Histórico imutável:** `itens_pedido` copia nome e preço do produto; `pedidos.codigo_promocao` copia o código usado.
- **Integridade:** chaves estrangeiras e `CHECK` nas migrations Flyway (`src/main/resources/db/migration`); o Hibernate roda com `ddl-auto=validate`, garantindo que modelo e implementação coincidem.
- `auditoria_logs` não tem chaves estrangeiras de propósito: é um registro histórico que deve sobreviver a qualquer alteração dos dados.

## 4. Diagrama de classes (visão de domínio)

```mermaid
classDiagram
    class Usuario {
        Long id
        String nome
        String email
        String senhaHash
        Perfil perfil
        boolean ativo
    }
    class Unidade {
        Long id
        String nome
        String cidade
        boolean ativa
    }
    class Produto {
        Long id
        String nome
        String categoria
        BigDecimal precoBase
        boolean ativo
        desativar()
    }
    class CardapioItem {
        BigDecimal preco
        boolean disponivel
        definir(preco, disponivel)
    }
    class Estoque {
        int quantidade
        entrada(qtd)
        saida(qtd)
    }
    class MovimentacaoEstoque {
        TipoMovimentacao tipo
        int quantidade
        String motivo
    }
    class Pedido {
        CanalPedido canalPedido
        StatusPedido status
        FormaPagamento formaPagamento
        BigDecimal subtotal
        BigDecimal total
        adicionarItem(produto, qtd, preco)
        aplicarPromocao(promocao, desconto)
        aplicarResgate(pontos, desconto)
        transitarPara(status)
        confirmarPagamento()
        cancelar()
    }
    class ItemPedido {
        String produtoNome
        int quantidade
        BigDecimal precoUnitario
        subtotal()
    }
    class Pagamento {
        StatusPagamento status
        BigDecimal valor
        String chaveIdempotencia
        String transacaoGateway
        aprovar()
        recusar()
        manterPendente()
        estornar()
    }
    class Promocao {
        String codigo
        TipoPromocao tipo
        BigDecimal valor
        validarPara(unidadeId, subtotal, agora)
        calcularDesconto(subtotal)
    }
    class ContaFidelidade {
        boolean consentimento
        int saldoPontos
        conceder()
        revogar()
        creditar(pontos)
        debitar(pontos)
    }
    class MovimentoPontos {
        TipoMovimentoPontos tipo
        int pontos
    }
    class AuditoriaLog {
        String acao
        String entidade
        Long entidadeId
    }
    class StatusPedido {
        <<enumeration>>
        AGUARDANDO_PAGAMENTO
        EM_PREPARO
        PRONTO
        ENTREGUE
        CANCELADO
        podeIrPara(destino)
    }
    class CanalPedido {
        <<enumeration>>
        APP
        TOTEM
        BALCAO
        PICKUP
        WEB
    }
    class GatewayPagamento {
        <<interface>>
        processar(solicitacao)
        estornar(transacaoId, valor)
    }
    class GatewayPagamentoMock

    Unidade "1" --> "*" CardapioItem
    Produto "1" --> "*" CardapioItem
    Unidade "1" --> "*" Estoque
    Produto "1" --> "*" Estoque
    Estoque ..> MovimentacaoEstoque : gera
    Usuario "0..1" --> "*" Pedido : cliente
    Unidade "1" --> "*" Pedido
    Pedido "1" *-- "1..*" ItemPedido
    ItemPedido --> Produto
    Pedido "1" --> "*" Pagamento
    Pedido --> Promocao : aplica
    Pedido --> StatusPedido
    Pedido --> CanalPedido
    Usuario "1" --> "0..1" ContaFidelidade
    ContaFidelidade "1" --> "*" MovimentoPontos
    GatewayPagamentoMock ..|> GatewayPagamento
```

## 5. Diagrama de sequência — Pedido → Pagamento externo (mock) → Status

```mermaid
sequenceDiagram
    autonumber
    actor Cli as Cliente (APP/TOTEM/WEB)
    participant API as api (Controllers)
    participant PS as PedidoService
    participant ES as EstoqueService
    participant PG as PagamentoService
    participant GW as Gateway mock
    participant DB as Banco de dados
    actor Coz as Cozinha / Atendente

    Cli->>API: POST /pedidos {canalPedido, unidadeId, itens, formaPagamento}
    API->>PS: criar(...)
    PS->>DB: valida unidade e preços do cardápio
    PS->>DB: salva pedido (AGUARDANDO_PAGAMENTO, total calculado)
    PS->>ES: registrarSaida (reserva, com lock)
    ES->>DB: baixa estoque + movimentação
    alt estoque insuficiente
        ES-->>API: ESTOQUE_INSUFICIENTE
        API-->>Cli: 409 (tudo desfeito)
    else ok
        PS->>DB: auditoria PEDIDO_CRIADO
        API-->>Cli: 201 {pedidoId, status, total}
    end

    Cli->>API: POST /pagamentos {pedidoId} + Idempotency-Key
    API->>PG: pagar(...)
    PG->>DB: bloqueia o pedido (lock)
    PG->>GW: processar(valor, forma)
    alt aprovado
        GW-->>PG: APPROVED + transacaoId
        PG->>DB: pagamento APROVADO, pedido EM_PREPARO, pontos, auditoria
        API-->>Cli: 201 {status APROVADO, gateway{...}}
    else recusado
        GW-->>PG: DECLINED
        PG->>DB: pagamento RECUSADO (pedido segue AGUARDANDO_PAGAMENTO)
        API-->>Cli: 201 {status RECUSADO, mensagem}
    else gateway indisponível
        GW--xPG: timeout / indisponível
        PG->>DB: pagamento PENDENTE
        API-->>Cli: 202 {status PENDENTE} (repetir com a mesma chave)
    end

    Coz->>API: PATCH /pedidos/{id}/status {PRONTO}
    API->>PS: atualizarStatus (valida transição)
    API-->>Coz: 200 PRONTO
    Coz->>API: PATCH /pedidos/{id}/status {ENTREGUE}
    API-->>Coz: 200 ENTREGUE
```

## 6. Máquina de estados do pedido

```mermaid
stateDiagram-v2
    [*] --> AGUARDANDO_PAGAMENTO: POST /pedidos
    AGUARDANDO_PAGAMENTO --> EM_PREPARO: pagamento aprovado
    AGUARDANDO_PAGAMENTO --> CANCELADO: cancelamento
    EM_PREPARO --> PRONTO: cozinha
    EM_PREPARO --> CANCELADO: cancelamento da equipe (estorna pagamento)
    PRONTO --> ENTREGUE: atendente
    ENTREGUE --> [*]
    CANCELADO --> [*]
```
