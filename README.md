# Raízes do Nordeste

Monorepo do sistema da rede de restaurantes **Raízes do Nordeste**: cardápio por unidade, pedidos multicanal (APP, TOTEM, BALCAO, PICKUP, WEB), estoque por unidade, pagamento via gateway **simulado (mock)**, promoções, programa de fidelidade com consentimento (LGPD) e auditoria de ações sensíveis.

Nasceu como projeto acadêmico de Back-end (Java 17 + Spring Boot 4; a versão avaliada está na tag `v1.0-backend`). Fluxo principal entregue: **Pedido → Pagamento mock → Atualização de status**.

## Organização do repositório

| Pasta | Conteúdo |
|---|---|
| [`backend/`](backend) | API REST em Java/Spring Boot, dona das regras de negócio |
| [`web/`](web) | Front-end e BFF em Next.js (React, TypeScript) |
| [`docs/`](docs) | Documentação, diagramas e coleção Postman |

```
navegador ──► web: telas + BFF (/api/*) ──► backend: API REST ──► banco
              cookie httpOnly               JWT Bearer
```

O BFF guarda o JWT em cookie `httpOnly`, repassa as chamadas necessárias (lista fechada de rotas), agrega dados para as telas e define o canal do pedido (`WEB` para cliente, `BALCAO` para a equipe). Regras de negócio e autorização ficam na API.

## Requisitos

| Item | Versão |
|---|---|
| Node.js (front-end) | 20.19 ou superior (usado: 24+) |
| Java (JDK) | 17 ou superior |
| Maven | não precisa instalar (usa o wrapper `./mvnw`) |
| Banco (dev/testes) | H2 embutido — nada a instalar |
| Banco (prod) | PostgreSQL 14+ (opcional) |

Dependências principais: Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation, Flyway, springdoc-openapi, JJWT.

## Como executar (perfil `dev`, sem instalar banco)

```bash
git clone https://github.com/marcosmpar/raizesdonordeste.git
cd raizesdonordeste/backend
./mvnw spring-boot:run
```

Na primeira execução o Flyway cria o banco (H2 em arquivo, `./data/raizes`) e o seed popula os dados de demonstração. A API sobe em **http://localhost:8080**.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI (JSON):** http://localhost:8080/v3/api-docs

Para recomeçar do zero (recria o seed): pare a aplicação e apague a pasta `backend/data`.

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

## Front-end e BFF

Com a API no ar, em outro terminal (a partir da raiz):

```bash
cd web
npm install
npm run dev
```

Acesse **http://localhost:3000** e entre com um usuário do seed.

| Tela | Quem usa |
|---|---|
| `/` cardápio por unidade e categoria, sacola (no celular em `/sacola`), promoção e pontos | público (pedir exige login) |
| `/pedidos` e `/pedidos/{id}` acompanhamento, pagamento simulado e cancelamento | usuários logados |
| `/painel` fila de preparo e entrega | equipe |
| `/fidelidade` consentimento, saldo e histórico | cliente |

### Tudo de uma vez com Docker

```bash
docker compose up --build
```

## Variáveis de ambiente

Referências: `backend/.env.example` e `web/.env.example`. Em desenvolvimento nenhuma é obrigatória.

| Variável | Uso | Obrigatória |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` (padrão) ou `prod` | não |
| `JWT_SECRET` | chave de assinatura do JWT (mín. 32 caracteres) | **prod** |
| `JWT_EXPIRATION_SECONDS` | validade do token (padrão 3600) | não |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | conexão PostgreSQL | **prod** |
| `BACKEND_URL` (web) | endereço da API visto pelo BFF | em hospedagem |
| `BACKEND_TIMEOUT_MS` (web) | espera máxima pela API (padrão 15000) | não |
| `COOKIE_SECURE` (web) | `false` apenas para HTTP local em modo de produção | não |

### Perfil `prod` (PostgreSQL)

```bash
createdb raizesdonordeste        # ou crie o banco pelo seu cliente SQL
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:postgresql://localhost:5432/raizesdonordeste
export DB_USER=raizes DB_PASSWORD=sua-senha
export JWT_SECRET=um-segredo-longo-e-aleatorio-com-mais-de-32-caracteres
cd backend && ./mvnw spring-boot:run
```

O Flyway aplica as migrations (`backend/src/main/resources/db/migration`). **O seed não roda em `prod`**; crie o primeiro ADMIN diretamente no banco ou rode antes em `dev`. Observação: neste ambiente de desenvolvimento o perfil `prod` não foi exercitado contra um PostgreSQL real; o SQL das migrations usa apenas recursos padrão e é executado em H2 em modo PostgreSQL.

## Deploy de demonstração

```
navegador ──► Vercel (web: telas + BFF) ──► Render (backend: API)
```

Publique primeiro a API e depois o front-end.

### API no Render (plano gratuito)

O repositório traz o `backend/Dockerfile` e um `render.yaml` (Blueprint). Com o perfil `dev` (H2 embutido + seed automático) não é preciso criar banco: a API sobe com dados de demonstração e o **Swagger fica funcional**.

1. Crie uma conta em https://render.com e conecte o repositório do GitHub.
2. **New → Blueprint**, escolha este repositório e confirme (o `render.yaml` cria o serviço da API com plano `free`; mudanças fora de `backend/` não disparam deploy).
3. Aguarde o build. O endereço fica como `https://<nome>.onrender.com` — a raiz (`/`) redireciona para o Swagger (`/swagger-ui.html`).

Demonstração publicada: https://raizes-do-nordeste-ffjn.onrender.com/swagger-ui/index.html

Observações:

- O serviço gratuito **hiberna após ~15 min sem acesso**; o primeiro acesso depois disso pode levar cerca de 1 minuto.
- O banco H2 fica no disco efêmero: ao reiniciar, os dados voltam ao estado inicial (o seed é refeito). Os usuários de demonstração (senha `Senha@123`) ficam públicos, então use apenas para demonstração.
- A chave JWT é gerada pelo `render.yaml` (`JWT_SECRET`). A porta é lida da variável `PORT`.
- Os limites dos planos gratuitos mudam; confira a página de preços do provedor.
- Se o serviço foi criado manualmente (sem Blueprint), o `render.yaml` não o altera: no painel, use o Dockerfile `./backend/Dockerfile` e o contexto `./backend`.

### Front-end e BFF na Vercel

1. Crie uma conta em https://vercel.com e importe este repositório do GitHub (a partir da `main`).
2. Em **Root Directory**, escolha `web` (o framework detectado é Next.js).
3. Cadastre as variáveis de ambiente abaixo e faça o deploy.

| Variável | Valor |
|---|---|
| `BACKEND_URL` | endereço público da API, ex.: `https://raizes-do-nordeste-ffjn.onrender.com` |
| `BACKEND_TIMEOUT_MS` | `90000` (a API gratuita pode levar cerca de 1 minuto para acordar) |

Observações:

- Não defina `COOKIE_SECURE`: em produção o cookie de sessão já é enviado apenas por HTTPS.
- `BACKEND_URL` é usada só no servidor; não use o prefixo `NEXT_PUBLIC_`.
- A espera configurada precisa caber na duração máxima das funções do plano usado na Vercel.
- Confira os termos do plano escolhido (o plano gratuito é voltado a uso pessoal, não comercial).

## Testes

```bash
cd backend && ./mvnw test        # 63 testes automatizados (JUnit + MockMvc, H2 em memória)
```

```bash
cd web && npm test               # 16 testes do BFF (Vitest, API simulada)
```

Coleção Postman/Insomnia com **55 cenários** (34 positivos, 21 negativos) e testes automáticos em cada requisição:

- Arquivo: [`docs/postman/raizes-do-nordeste.postman_collection.json`](docs/postman/raizes-do-nordeste.postman_collection.json)
- Suba a API em `dev` com banco vazio, importe a coleção e execute a pasta **inteira, em ordem** (Runner). Variáveis (`baseUrl`, tokens, ids) são preenchidas automaticamente.
- Via linha de comando: `npx newman run docs/postman/raizes-do-nordeste.postman_collection.json`
- A coleção pode ser reexecutada: cada rodada cria seus próprios cliente, produto e pedidos.

O plano de testes completo (ID, pré-condição, entrada, resultado esperado e nome da requisição na coleção) está em [`docs/PLANO_DE_TESTES.md`](docs/PLANO_DE_TESTES.md).

## Estrutura do projeto

```
backend/src/main/java/br/com/marcosmpar/raizesdonordeste
├── api/             controllers, DTOs, tratamento de erros, segurança e OpenAPI
├── application/     casos de uso (pedido, pagamento, estoque, fidelidade, promoção, auditoria...)
├── domain/          entidades, regras de negócio, exceções e portas (interfaces de repositório)
└── infrastructure/  JPA/repositórios, JWT, gateway de pagamento mock, seed de dev
backend/src/main/resources/db/migration   migrations Flyway (V1..V6)

web/src
├── app/             telas e rotas do BFF (app/api)
├── server/          cliente da API, sessão e proteções do BFF
├── components/      componentes compartilhados
└── lib/             tipos, chamadas ao BFF e formatação
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
| [`docs/RELATORIO.md`](docs/RELATORIO.md) | Texto consolidado do relatório final |
| [`docs/relatorio/`](docs/relatorio) | Gerador do relatório em PDF no padrão ABNT (`python3 docs/relatorio/gerar_relatorio_pdf.py`). O PDF e os dados da capa (`capa.json`, copiado de `capa.exemplo.json`) não são versionados |

## Evidências

- Repositório: https://github.com/marcosmpar/raizesdonordeste
- Swagger online (demonstração no Render): https://raizes-do-nordeste-ffjn.onrender.com/swagger-ui/index.html — o serviço gratuito hiberna após ~15 min sem acesso, então o primeiro acesso pode demorar cerca de 1 minuto
- Swagger local: http://localhost:8080/swagger-ui.html (rota `/swagger-ui.html`)
- Coleção de testes: `docs/postman/raizes-do-nordeste.postman_collection.json`
