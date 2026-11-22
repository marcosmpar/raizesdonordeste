# Segurança e LGPD

Mínimo técnico de segurança e privacidade adotado no back-end.

## Autenticação e autorização

| Tema | Implementação |
|---|---|
| Autenticação | `POST /auth/login` valida e-mail e senha e emite um **JWT** assinado (HS256) com `sub` (id), `perfil`, `iat` e `exp` (padrão 1 h, configurável por `JWT_EXPIRATION_SECONDS`). A chave vem de `JWT_SECRET` (mínimo 32 caracteres; a aplicação não sobe com chave fraca). |
| Sessão | Stateless: sem cookie nem sessão no servidor (CSRF desnecessário). |
| Autorização | Perfis `CLIENTE`, `ATENDENTE`, `COZINHA`, `GERENTE`, `ADMIN` aplicados por rota com `@PreAuthorize`. Sem token → 401; perfil sem permissão → 403. |
| Propriedade dos dados | CLIENTE só vê/cancela/paga os **próprios** pedidos e pagamentos; para pedidos alheios a API responde 404 (não revela existência). |
| Senha | Armazenada com **BCrypt**; o hash nunca é retornado em nenhuma resposta. |
| Login | Mensagem idêntica para e-mail inexistente, senha errada ou usuário inativo (não permite enumerar contas). |
| Cadastro | O autocadastro cria sempre perfil CLIENTE; perfis internos só são criados por ADMIN. |

## Tratamento de erros sem exposição

- Erros seguem um JSON padrão e **nunca** expõem stack trace, SQL ou nomes internos; falhas inesperadas retornam `ERRO_INTERNO` com um `requestId` para consulta nos logs.
- Violações de integridade do banco viram `409 CONFLITO_DE_DADOS` genérico.

## LGPD (Lei 13.709/2018)

| Aspecto | Como foi tratado |
|---|---|
| **Dados pessoais tratados** | Nome, e-mail, hash de senha, histórico de pedidos e pontos de fidelidade. Não são coletados CPF, telefone, endereço do cliente nem dados de cartão (o gateway é mock). |
| **Minimização** | Apenas o necessário para identificar o usuário e operar pedidos. Pedidos de balcão/totem podem ser feitos **sem identificar o cliente** (`clienteId` opcional). |
| **Base legal e finalidade** | Pedidos: execução do contrato/serviço. Fidelidade: **consentimento** explícito, específico para esse fim. |
| **Consentimento** | `POST /fidelidade/consentimento` registra data e hora; sem ele não há acúmulo nem resgate de pontos. |
| **Revogação** | `DELETE /fidelidade/consentimento` interrompe imediatamente o tratamento para fidelidade (o saldo existente é preservado e a data da revogação é registrada). |
| **Acesso do titular** | `GET /usuarios/me`, `GET /fidelidade`, `GET /fidelidade/historico`, `GET /pedidos`. |
| **Registros sem dados pessoais** | A tabela de auditoria e os logs usam identificadores (ids) e **não** gravam e-mail, nome nem senha; há teste automatizado para isso. |
| **Exposição indevida** | Respostas devolvem só o necessário; listagens de pedidos são filtradas por dono; e-mail só aparece para o próprio usuário. |
| **Segredos** | Chave JWT e credenciais do banco por variáveis de ambiente (`.env` está no `.gitignore`; há `.env.example`). |

### Fora do escopo desta entrega (declarado)

- **Eliminação/anonimização** de conta sob pedido do titular (direito de exclusão) e política de retenção automática.
- Criptografia em repouso, HTTPS próprio (esperado atrás de proxy/ingress com TLS), rate limiting e bloqueio por tentativas de login.
- Trilha de acesso para leituras (a auditoria cobre ações de escrita sensíveis).
