# Raízes do Nordeste — Back-end

API REST da rede de restaurantes **Raízes do Nordeste**: cardápio por unidade, pedidos multicanal (APP, TOTEM, BALCAO, PICKUP, WEB), estoque por unidade, pagamento via gateway **simulado (mock)**, promoções, programa de fidelidade com consentimento (LGPD) e auditoria de ações sensíveis.

Projeto acadêmico de Back-end (Java 17 + Spring Boot 4). Fluxo principal entregue: **Pedido → Pagamento mock → Atualização de status**.

## Requisitos

| Item | Versão |
|---|---|
| Java (JDK) | 17 ou superior |
| Maven | não precisa instalar (usa o wrapper `./mvnw`) |
| Banco (dev/testes) | H2 embutido — nada a instalar |
| Banco (prod) | PostgreSQL 14+ (opcional) |
| Node.js | opcional, apenas para rodar a coleção via `newman` |

Dependências principais: Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation, Flyway, springdoc-openapi, JJWT.

## Como executar (perfil `dev`, sem instalar banco)

```bash
git clone <URL-DO-REPOSITORIO>
cd raizesdonordeste
./mvnw spring-boot:run
```

Na primeira execução o Flyway cria o banco (H2 em arquivo, `./data/raizes`) e o seed popula os dados de demonstração. A API sobe em **http://localhost:8080**.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI (JSON):** http://localhost:8080/v3/api-docs

Para recomeçar do zero (recria o seed): pare a aplicação e apague a pasta `./data`.

### Usuários do seed (senha `Senha@123`)

| E-mail | Perfil |
|---|---|
| `admin@raizes.com` | ADMIN |
| `gerente@raizes.com` | GERENTE |
| `atendente@raizes.com` | ATENDENTE |
| `cozinha@raizes.com` | COZINHA |
| `cliente@raizes.com` | CLIENTE |

O seed também cria 2 unidades (Recife e Salvador), 5 produtos com cardápio, 50 unidades de estoque de cada produto por unidade e as promoções `BEMVINDO10` (10% a partir de R$ 30,00) e `RECIFE5` (R$ 5,00, só em Recife).

### Como autenticar no Swagger

1. `POST /auth/login` com `{"email":"cliente@raizes.com","senha":"Senha@123"}`.
2. Copie o `accessToken`, clique em **Authorize** e cole o token.

## Variáveis de ambiente

Copie `.env.example` como referência. No perfil `dev` nenhuma é obrigatória.

| Variável | Uso | Obrigatória |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` (padrão) ou `prod` | não |
| `JWT_SECRET` | chave de assinatura do JWT (mín. 32 caracteres) | **prod** |
| `JWT_EXPIRATION_SECONDS` | validade do token (padrão 3600) | não |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | conexão PostgreSQL | **prod** |

### Perfil `prod` (PostgreSQL)

```bash
createdb raizesdonordeste        # ou crie o banco pelo seu cliente SQL
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:postgresql://localhost:5432/raizesdonordeste
export DB_USER=raizes DB_PASSWORD=sua-senha
export JWT_SECRET=um-segredo-longo-e-aleatorio-com-mais-de-32-caracteres
./mvnw spring-boot:run
```

O Flyway aplica as migrations (`src/main/resources/db/migration`). **O seed não roda em `prod`**; crie o primeiro ADMIN diretamente no banco ou rode antes em `dev`. Observação: neste ambiente de desenvolvimento o perfil `prod` não foi exercitado contra um PostgreSQL real; o SQL das migrations usa apenas recursos padrão e é executado em H2 em modo PostgreSQL.

## Testes

```bash
./mvnw test                      # 62 testes automatizados (JUnit + MockMvc, H2 em memória)
```

Coleção Postman/Insomnia com **55 cenários** (34 positivos, 21 negativos) e testes automáticos em cada requisição:

- Arquivo: [`docs/postman/raizes-do-nordeste.postman_collection.json`](docs/postman/raizes-do-nordeste.postman_collection.json)
- Suba a API em `dev` com banco vazio, importe a coleção e execute a pasta **inteira, em ordem** (Runner). Variáveis (`baseUrl`, tokens, ids) são preenchidas automaticamente.
- Via linha de comando: `npx newman run docs/postman/raizes-do-nordeste.postman_collection.json`
- A coleção pode ser reexecutada: cada rodada cria seus próprios cliente, produto e pedidos.

O plano de testes completo (ID, pré-condição, entrada, resultado esperado e nome da requisição na coleção) está em [`docs/PLANO_DE_TESTES.md`](docs/PLANO_DE_TESTES.md).

## Estrutura do projeto

```
src/main/java/br/com/marcosmpar/raizesdonordeste
├── api/             controllers, DTOs, tratamento de erros, segurança e OpenAPI
├── application/     casos de uso (pedido, pagamento, estoque, fidelidade, promoção, auditoria...)
├── domain/          entidades, regras de negócio, exceções e portas (interfaces de repositório)
└── infrastructure/  JPA/repositórios, JWT, gateway de pagamento mock, seed de dev
src/main/resources/db/migration   migrations Flyway (V1..V6)
```

Dependências: `api → application → domain`; a `infrastructure` implementa as portas do `domain`.

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/REQUISITOS.md`](docs/REQUISITOS.md) | Requisitos funcionais/não funcionais, prioridades e limitações |
| [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) | Camadas, casos de uso (com descrição da feature crítica), DER, diagrama de classes e de sequência |
| [`docs/diagramas/`](docs/diagramas) | Os 6 diagramas em PNG e SVG (gerados por `gerar_diagramas.py`) |
| [`docs/API.md`](docs/API.md) | Contrato da API: endpoints, permissões, exemplos, status e padrão de erro |
| [`docs/PLANO_DE_TESTES.md`](docs/PLANO_DE_TESTES.md) | Cenários de teste e como executá-los |
| [`docs/RELATORIO.md`](docs/RELATORIO.md) | Texto consolidado para o relatório final (PDF) |

## Evidências

- Repositório: _preencher com a URL pública_
- Swagger local: http://localhost:8080/swagger-ui.html (rota `/swagger-ui.html`)
- Coleção de testes: `docs/postman/raizes-do-nordeste.postman_collection.json`
- Deploy: não há
