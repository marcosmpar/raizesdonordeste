#!/usr/bin/env python3
"""Gera os diagramas do projeto em SVG (e PNG, se o Google Chrome estiver instalado).

Sem dependências externas. Uso (a partir da raiz do projeto):  python3 docs/diagramas/gerar_diagramas.py
Os arquivos são gravados em docs/diagramas/.
"""
import html
import os
import subprocess
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
FONTE = "Helvetica, Arial, sans-serif"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
CW = 6.6  # largura média de um caractere a 12px


def esc(s):
    return html.escape(str(s), quote=False)


def largura(textos, tamanho=12, minimo=0):
    return max([minimo] + [len(t) * CW * tamanho / 12 + 24 for t in textos])


class Svg:
    def __init__(self, w, h, titulo):
        self.w, self.h, self.titulo, self.p = w, h, titulo, []

    def add(self, s):
        self.p.append(s)

    def rect(self, x, y, w, h, fill="#fff", stroke="#37474f", sw=1.3, rx=0, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.add(f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{rx}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"{d}/>')

    def ellipse(self, cx, cy, rx, ry, fill="#fff", stroke="#37474f"):
        self.add(f'<ellipse cx="{cx:.1f}" cy="{cy:.1f}" rx="{rx:.1f}" ry="{ry:.1f}" fill="{fill}" stroke="{stroke}" stroke-width="1.3"/>')

    def circle(self, cx, cy, r, fill="#fff", stroke="#37474f"):
        self.add(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="{r}" fill="{fill}" stroke="{stroke}" stroke-width="1.3"/>')

    def text(self, x, y, t, size=12, anchor="start", weight="normal", fill="#1b1b1b", italic=False):
        st = ' font-style="italic"' if italic else ""
        self.add(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" text-anchor="{anchor}" font-weight="{weight}" fill="{fill}"{st}>{esc(t)}</text>')

    def linhas(self, x, y, textos, size=12, anchor="middle", passo=None, **kw):
        passo = passo or size * 1.35
        for i, t in enumerate(textos):
            self.text(x, y + i * passo, t, size, anchor, **kw)

    def poly(self, pts, fim=None, inicio=None, dash=None, stroke="#455a64", sw=1.3):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        m = ""
        if fim:
            m += f' marker-end="url(#{fim})"'
        if inicio:
            m += f' marker-start="url(#{inicio})"'
        pts_s = " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)
        self.add(f'<polyline points="{pts_s}" fill="none" stroke="{stroke}" stroke-width="{sw}"{d}{m}/>')

    def salvar(self, nome):
        defs = """<defs>
<marker id="seta" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="#455a64"/></marker>
<marker id="seta-aberta" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse"><path d="M1,1 L9,5 L1,9" fill="none" stroke="#455a64" stroke-width="1.4"/></marker>
<marker id="triangulo" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto-start-reverse"><path d="M1,1 L11,6 L1,11 z" fill="#fff" stroke="#455a64" stroke-width="1.3"/></marker>
<marker id="losango" viewBox="0 0 14 10" refX="1" refY="5" markerWidth="14" markerHeight="10" orient="auto-start-reverse"><path d="M1,5 L7,1 L13,5 L7,9 z" fill="#455a64"/></marker>
</defs>"""
        corpo = "\n".join(self.p)
        svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.w}" height="{self.h}" viewBox="0 0 {self.w} {self.h}" font-family="{FONTE}">\n'
               f'<title>{esc(self.titulo)}</title>\n{defs}\n<rect width="100%" height="100%" fill="#ffffff"/>\n{corpo}\n</svg>\n')
        caminho = os.path.join(AQUI, nome + ".svg")
        open(caminho, "w", encoding="utf-8").write(svg)
        if os.path.exists(CHROME):
            png = os.path.join(AQUI, nome + ".png")
            subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=2",
                            f"--window-size={self.w},{self.h}", f"--screenshot={png}", "file://" + caminho],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=60)
        print("gerado:", nome)


class Caixa:
    """Retângulo posicionado; fornece pontos de ancoragem nas bordas."""

    def __init__(self, x, y, w, h):
        self.x, self.y, self.w, self.h = x, y, w, h

    def pt(self, lado, f=0.5):
        if lado == "l":
            return (self.x, self.y + self.h * f)
        if lado == "r":
            return (self.x + self.w, self.y + self.h * f)
        if lado == "t":
            return (self.x + self.w * f, self.y)
        return (self.x + self.w * f, self.y + self.h)


def rotulo_extremo(svg, p, q, texto, desloc=16, lateral=11):
    """Escreve um rótulo junto ao ponto p, olhando na direção de q."""
    dx, dy = q[0] - p[0], q[1] - p[1]
    n = max((dx * dx + dy * dy) ** 0.5, 1)
    ux, uy = dx / n, dy / n
    x = p[0] + ux * desloc - uy * lateral
    y = p[1] + uy * desloc + ux * lateral + 4
    svg.text(x, y, texto, 11, "middle", fill="#b71c1c", weight="bold")


# ---------------------------------------------------------------- 1. arquitetura
def arquitetura():
    s = Svg(1260, 700, "Arquitetura em camadas")
    s.text(630, 34, "Arquitetura em camadas — Raízes do Nordeste", 18, "middle", "bold")

    def camada(y, h, titulo, linhas, cor):
        s.rect(60, y, 700, h, cor, "#37474f", 1.5, 10)
        s.text(80, y + 26, titulo, 15, weight="bold")
        s.linhas(80, y + 52, linhas, 12.5, "start", 19)

    camada(70, 150, "api — interface", ["Controllers e DTOs de request/response", "Segurança: filtro JWT, perfis (@PreAuthorize)", "GlobalExceptionHandler (padrão de erro)", "OpenAPI / Swagger"], "#e3f2fd")
    camada(270, 150, "application — casos de uso", ["PedidoService · PagamentoService · EstoqueService", "FidelidadeService · PromocaoService", "AutenticacaoService · UsuarioService · AuditoriaService", "Orquestra o fluxo e a transação; não conhece HTTP"], "#e8f5e9")
    camada(470, 150, "domain — regras de negócio", ["Entidades: Pedido, Estoque, Promocao, Pagamento...", "Comportamentos: Estoque.saida, Pedido.transitarPara", "Exceções de domínio", "Portas (interfaces): *Repository"], "#fff8e1")
    s.poly([(410, 220), (410, 270)], "seta")
    s.poly([(410, 420), (410, 470)], "seta")
    s.text(425, 250, "chama", 11, fill="#546e7a", italic=True)
    s.text(425, 450, "usa", 11, fill="#546e7a", italic=True)

    s.rect(940, 270, 270, 350, "#f3e5f5", "#37474f", 1.5, 10)
    s.text(960, 296, "infrastructure", 15, weight="bold")
    s.text(960, 316, "detalhes técnicos", 12, fill="#546e7a", italic=True)
    s.linhas(960, 348, ["Adapters JPA + Flyway", "(migrations V1..V6)", "", "JwtService", "(emite e valida o token)", "", "GatewayPagamentoMock", "", "Seed de desenvolvimento"], 12.5, "start", 19)
    s.poly([(940, 345), (760, 345)], "seta-aberta", dash="6 4")
    s.linhas(850, 312, ["implementa", "EmissorToken e", "GatewayPagamento"], 11, "middle", 14, fill="#546e7a", italic=True)
    s.poly([(940, 545), (760, 545)], "seta-aberta", dash="6 4")
    s.linhas(850, 512, ["implementa as", "portas (Repository)"], 11, "middle", 14, fill="#546e7a", italic=True)
    s.rect(60, 650, 1150, 34, "#eceff1", "#90a4ae", 1, 6)
    s.text(635, 672, "Dependências: api → application → domain.  A infraestrutura implementa interfaces definidas no domínio/aplicação.", 12, "middle", fill="#37474f")
    s.salvar("01-arquitetura-camadas")


# ---------------------------------------------------------------- 2. casos de uso
def casos_de_uso():
    s = Svg(1240, 1000, "Diagrama de casos de uso")
    s.text(620, 32, "Diagrama de casos de uso", 18, "middle", "bold")
    s.rect(330, 60, 600, 900, "#fafafa", "#607d8b", 1.5, 8)
    s.text(630, 86, "Sistema Raízes do Nordeste", 14, "middle", "bold")

    casos = [
        ("UC01 Cadastrar-se / autenticar", ["Cliente"]),
        ("UC02 Consultar cardápio da unidade", ["Cliente", "Atendente"]),
        ("UC03 Realizar pedido", ["Cliente", "Atendente"]),
        ("UC04 Solicitar pagamento", ["Cliente", "Atendente", "Gateway"]),
        ("UC05 Acompanhar / consultar pedidos", ["Cliente", "Atendente", "Cozinha", "Gerente"]),
        ("UC06 Cancelar pedido", ["Cliente", "Atendente", "Gerente"]),
        ("UC07 Atualizar status do pedido", ["Atendente", "Cozinha", "Gerente"]),
        ("UC08 Consultar e movimentar estoque", ["Atendente", "Gerente"]),
        ("UC09 Gerir unidades, produtos e cardápio", ["Gerente"]),
        ("UC10 Gerir promoções", ["Gerente"]),
        ("UC11 Fidelidade (consentir, saldo, resgatar)", ["Cliente"]),
        ("UC12 Consultar auditoria", ["Gerente"]),
        ("UC13 Gerir usuários internos", ["Gerente"]),
    ]
    atores = {"Cliente": (140, 190), "Atendente": (140, 430), "Cozinha": (140, 650), "Gerente": (140, 850), "Gateway": (1110, 300)}
    pos = {}
    y0, passo = 125, 63
    for i, (nome, _) in enumerate(casos):
        pos[nome] = (630, y0 + i * passo)
    # ligações (desenhadas antes para ficarem atrás)
    for nome, quem in casos:
        cx, cy = pos[nome]
        for a in quem:
            ax, ay = atores[a]
            if a == "Gateway":
                s.poly([(ax - 28, ay + 22), (cx + 150, cy)], stroke="#78909c", sw=1.1)
            else:
                s.poly([(ax + 28, ay + 22), (cx - 150, cy)], stroke="#78909c", sw=1.1)
    for nome, _ in casos:
        cx, cy = pos[nome]
        s.ellipse(cx, cy, 150, 25, "#e3f2fd", "#1565c0")
        s.text(cx, cy + 4, nome, 12, "middle")
    # relação include UC03 -> UC02
    s.poly([(560, pos[casos[2][0]][1] - 24), (540, pos[casos[1][0]][1] + 24)], "seta-aberta", dash="5 4", stroke="#c62828")
    s.text(470, 178, "«include»", 11, "middle", fill="#c62828", italic=True)

    def ator(x, y, nome, ext=False):
        cor = "#6a1b9a" if ext else "#37474f"
        s.circle(x, y, 10, "#fff", cor)
        s.poly([(x, y + 10), (x, y + 38)], stroke=cor)
        s.poly([(x - 18, y + 22), (x + 18, y + 22)], stroke=cor)
        s.poly([(x, y + 38), (x - 14, y + 58)], stroke=cor)
        s.poly([(x, y + 38), (x + 14, y + 58)], stroke=cor)
        s.text(x, y + 76, nome[0], 12.5, "middle", "bold")
        if len(nome) > 1:
            s.text(x, y + 91, nome[1], 11, "middle", fill="#546e7a")

    ator(140, 168, ("Cliente", "App · Web · Totem"))
    ator(140, 408, ("Atendente", "Balcão"))
    ator(140, 628, ("Cozinha",))
    ator(140, 828, ("Gerente / Administrador",))
    ator(1110, 278, ("Gateway de pagamento", "(sistema externo)"), True)
    s.salvar("02-casos-de-uso")


# ---------------------------------------------------------------- tabelas (DER)
def tabela(s, x, y, nome, linhas, cor="#e8f0fe"):
    rows = [t for t in linhas]
    w = max([210, len(nome) * 8.4 + 30] + [len(r) * 6.9 + 52 for r in rows])
    h = 28 + len(rows) * 17 + 8
    s.rect(x, y, w, h, "#ffffff", "#37474f", 1.4, 4)
    s.add(f'<path d="M{x},{y+28} L{x},{y+4} Q{x},{y} {x+4},{y} L{x+w-4},{y} Q{x+w},{y} {x+w},{y+4} L{x+w},{y+28} Z" fill="{cor}" stroke="#37474f" stroke-width="1.4"/>')
    s.text(x + w / 2, y + 19, nome, 13, "middle", "bold")
    for i, r in enumerate(rows):
        ty = y + 28 + 15 + i * 17
        if r.startswith("PK") or r.startswith("FK"):
            tag, resto = r.split(" ", 1)
            if r.startswith("PK/FK"):
                tag, resto = "PK/FK", r[6:]
            s.text(x + 8, ty, tag, 10.5, weight="bold", fill="#c62828" if tag.startswith("PK") else "#1565c0")
            s.text(x + 8 + (46 if tag == "PK/FK" else 26), ty, resto, 12)
        else:
            s.text(x + 34, ty, r, 12)
    return Caixa(x, y, w, h)


def der():
    OY = 36
    s = Svg(1560, 1200, "Diagrama Entidade-Relacionamento")
    s.text(780, 26, "Modelo de dados (DER) — Raízes do Nordeste", 18, "middle", "bold")
    T = {}
    T["USUARIOS"] = tabela(s, 30, 50 + OY, "USUARIOS", ["PK id", "nome", "email (único)", "senha_hash (BCrypt)", "perfil", "ativo", "criado_em"])
    T["CONTAS"] = tabela(s, 30, 270 + OY, "CONTAS_FIDELIDADE", ["PK/FK usuario_id", "consentimento", "consentimento_em", "revogado_em", "saldo_pontos (>= 0)"], "#fff3e0")
    T["MOVP"] = tabela(s, 30, 450 + OY, "MOVIMENTOS_PONTOS", ["PK id", "FK usuario_id", "FK pedido_id", "tipo (ACUMULO, RESGATE, ESTORNO)", "pontos (com sinal)"], "#fff3e0")
    T["PEDIDOS"] = tabela(s, 400, 50 + OY, "PEDIDOS", ["PK id", "FK cliente_id (opcional)", "FK unidade_id", "canal_pedido (APP, TOTEM, BALCAO, PICKUP, WEB)", "status", "forma_pagamento", "subtotal", "desconto_promocao", "desconto_fidelidade", "pontos_resgatados", "FK promocao_id (opcional)", "codigo_promocao (cópia)", "total (>= 0)", "criado_em"], "#e8f5e9")
    T["ITENS"] = tabela(s, 400, 400 + OY, "ITENS_PEDIDO", ["PK id", "FK pedido_id", "FK produto_id", "produto_nome (cópia)", "quantidade (> 0)", "preco_unitario (cópia)"], "#e8f5e9")
    T["PAG"] = tabela(s, 830, 50 + OY, "PAGAMENTOS", ["PK id", "FK pedido_id", "status (PENDENTE, APROVADO, RECUSADO, ESTORNADO)", "valor", "forma_pagamento", "chave_idempotencia (único com pedido_id)", "transacao_gateway", "codigo_gateway", "mensagem"], "#e8f5e9")
    T["PROMO"] = tabela(s, 830, 290 + OY, "PROMOCOES", ["PK id", "codigo (único)", "tipo (PERCENTUAL, VALOR_FIXO)", "valor", "valor_minimo_pedido", "FK unidade_id (opcional)", "inicio", "fim", "ativa"], "#fff3e0")
    T["PROD"] = tabela(s, 400, 660 + OY, "PRODUTOS", ["PK id", "nome (único)", "descricao", "categoria", "preco_base (> 0)", "ativo (exclusão lógica)"])
    T["UNI"] = tabela(s, 1250, 640 + OY, "UNIDADES", ["PK id", "nome (único)", "cidade", "uf", "endereco", "ativa"])
    T["CARD"] = tabela(s, 830, 570 + OY, "CARDAPIO_ITENS", ["PK id", "FK unidade_id", "FK produto_id", "preco (> 0)", "disponivel", "único (unidade_id, produto_id)"])
    T["EST"] = tabela(s, 830, 740 + OY, "ESTOQUES", ["PK id", "FK unidade_id", "FK produto_id", "quantidade (>= 0)", "único (unidade_id, produto_id)"])
    T["MOVE"] = tabela(s, 830, 890 + OY, "MOVIMENTACOES_ESTOQUE", ["PK id", "FK unidade_id", "FK produto_id", "FK usuario_id", "tipo (ENTRADA, SAIDA)", "quantidade (> 0)", "motivo", "criado_em"])
    T["AUD"] = tabela(s, 1250, 50 + OY, "AUDITORIA_LOGS", ["PK id", "ator_id", "acao", "entidade", "entidade_id", "detalhes (sem dados pessoais)", "request_id", "criado_em"], "#eceff1")
    s.text(1254, 50 + OY + T["AUD"].h + 16, "sem chaves estrangeiras: registro histórico", 10.5, fill="#78909c", italic=True)

    def rel(a, lado_a, fa, b, lado_b, fb, ca, cb, via=None):
        p, q = T[a].pt(lado_a, fa), T[b].pt(lado_b, fb)
        pts = [p] + (via or []) + [q]
        s.poly(pts, stroke="#546e7a")
        rotulo_extremo(s, pts[0], pts[1], ca)
        rotulo_extremo(s, pts[-1], pts[-2], cb)

    rel("USUARIOS", "r", .3, "PEDIDOS", "l", .1, "1", "N")
    rel("USUARIOS", "b", .5, "CONTAS", "t", .5, "1", "0..1")
    rel("CONTAS", "b", .5, "MOVP", "t", .5, "1", "N")
    rel("USUARIOS", "l", .5, "MOVP", "l", .5, "1", "N", via=[(16, 126 + OY), (16, 520 + OY)])
    rel("USUARIOS", "l", .85, "MOVE", "l", .5, "1", "N", via=[(8, 181 + OY), (8, 1005 + OY)])
    rel("PEDIDOS", "l", .55, "MOVP", "r", .5, "1", "N")
    rel("PEDIDOS", "b", .5, "ITENS", "t", .5, "1", "1..N")
    rel("PEDIDOS", "r", .3, "PAG", "l", .15, "1", "N")
    rel("PEDIDOS", "r", .8, "PROMO", "l", .15, "N", "0..1")
    rel("ITENS", "b", .5, "PROD", "t", .5, "N", "1")
    rel("PROD", "r", .25, "CARD", "l", .4, "1", "N")
    rel("PROD", "r", .55, "EST", "l", .4, "1", "N")
    rel("PROD", "r", .85, "MOVE", "l", .4, "1", "N")
    rel("UNI", "l", .2, "CARD", "r", .4, "1", "N")
    rel("UNI", "l", .5, "EST", "r", .4, "1", "N")
    rel("UNI", "l", .8, "MOVE", "r", .4, "1", "N")
    rel("UNI", "t", .5, "PROMO", "r", .5, "0..1", "N", via=[(1250 + T["UNI"].w / 2, 390 + OY)])
    px = 400 + T["PEDIDOS"].w * .75
    rel("PEDIDOS", "t", .75, "UNI", "r", .6, "N", "1", via=[(px, 58), (1530, 58), (1530, 640 + OY + 0.6 * T["UNI"].h)])
    s.salvar("03-der")


# ---------------------------------------------------------------- 4. classes
def classe(s, x, y, nome, attrs, metodos=(), estereotipo=None, cor="#e3f2fd", w_min=230):
    linhas = list(attrs) + list(metodos)
    w = largura(linhas + [nome], 12, w_min)
    cab = 30 if not estereotipo else 44
    h = cab + len(attrs) * 16 + 10 + (len(metodos) * 16 + 8 if metodos else 0)
    s.rect(x, y, w, h, "#fff", "#37474f", 1.4)
    s.rect(x, y, w, cab, cor, "#37474f", 1.4)
    if estereotipo:
        s.text(x + w / 2, y + 16, f"«{estereotipo}»", 11, "middle", fill="#546e7a", italic=True)
        s.text(x + w / 2, y + 34, nome, 13, "middle", "bold")
    else:
        s.text(x + w / 2, y + 20, nome, 13, "middle", "bold")
    for i, a in enumerate(attrs):
        s.text(x + 8, y + cab + 15 + i * 16, a, 12)
    if metodos:
        sep = y + cab + len(attrs) * 16 + 8
        s.poly([(x, sep), (x + w, sep)], stroke="#37474f", sw=1)
        for i, m in enumerate(metodos):
            s.text(x + 8, sep + 14 + i * 16, m, 12)
    return Caixa(x, y, w, h)


def classes():
    OY = 40
    s = Svg(1700, 940, "Diagrama de classes (domínio)")
    s.text(850, 26, "Diagrama de classes — visão de domínio", 18, "middle", "bold")
    C = {}
    C["Usuario"] = classe(s, 30, 50 + OY, "Usuario", ["id: Long", "nome: String", "email: String", "senhaHash: String", "perfil: Perfil", "ativo: boolean"])
    C["Conta"] = classe(s, 30, 270 + OY, "ContaFidelidade", ["consentimento: boolean", "saldoPontos: int"], ["conceder()", "revogar()", "creditar(pontos)", "debitar(pontos)"], cor="#fff3e0")
    C["Mov"] = classe(s, 30, 520 + OY, "MovimentoPontos", ["tipo: TipoMovimentoPontos", "pontos: int", "pedidoId: Long"], cor="#fff3e0")
    C["Pedido"] = classe(s, 380, 50 + OY, "Pedido", ["canalPedido: CanalPedido", "status: StatusPedido", "formaPagamento: FormaPagamento", "subtotal: BigDecimal", "descontoPromocao: BigDecimal", "descontoFidelidade: BigDecimal", "total: BigDecimal"],
                         ["adicionarItem(produto, qtd, preco)", "aplicarPromocao(promocao, desconto)", "aplicarResgate(pontos, desconto)", "transitarPara(status)", "confirmarPagamento()", "cancelar()"], cor="#e8f5e9", w_min=290)
    C["Item"] = classe(s, 780, 50 + OY, "ItemPedido", ["produtoNome: String", "quantidade: int", "precoUnitario: BigDecimal"], ["subtotal(): BigDecimal"], cor="#e8f5e9")
    C["Promo"] = classe(s, 780, 220 + OY, "Promocao", ["codigo: String", "tipo: TipoPromocao", "valor: BigDecimal", "valorMinimoPedido: BigDecimal", "inicio / fim: Instant"], ["validarPara(unidadeId, subtotal, agora)", "calcularDesconto(subtotal)"], cor="#fff3e0", w_min=290)
    C["Status"] = classe(s, 780, 470 + OY, "StatusPedido", ["AGUARDANDO_PAGAMENTO", "EM_PREPARO", "PRONTO", "ENTREGUE", "CANCELADO"], ["podeIrPara(destino): boolean"], "enumeration", "#eceff1")
    C["Canal"] = classe(s, 780, 690 + OY, "CanalPedido", ["APP", "TOTEM", "BALCAO", "PICKUP", "WEB"], (), "enumeration", "#eceff1")
    C["Pag"] = classe(s, 380, 420 + OY, "Pagamento", ["status: StatusPagamento", "valor: BigDecimal", "chaveIdempotencia: String", "transacaoGateway: String"], ["aprovar() / recusar()", "manterPendente()", "estornar()"], cor="#e8f5e9")
    C["Gw"] = classe(s, 380, 660 + OY, "GatewayPagamento", [], ["processar(solicitacao): Resposta", "estornar(transacaoId, valor)"], "interface", "#eceff1", 270)
    C["Mock"] = classe(s, 380, 810 + OY, "GatewayPagamentoMock", ["(simula aprovado, recusado e indisponível)"], cor="#eceff1", w_min=270)
    C["Prod"] = classe(s, 1150, 50 + OY, "Produto", ["id: Long", "nome: String", "categoria: String", "precoBase: BigDecimal", "ativo: boolean"], ["desativar()"])
    C["Card"] = classe(s, 1150, 270 + OY, "CardapioItem", ["preco: BigDecimal", "disponivel: boolean"], ["definir(preco, disponivel)"])
    C["Est"] = classe(s, 1150, 440 + OY, "Estoque", ["quantidade: int"], ["entrada(qtd)", "saida(qtd)  // nunca fica negativo"])
    C["MovE"] = classe(s, 1150, 640 + OY, "MovimentacaoEstoque", ["tipo: TipoMovimentacao", "quantidade: int", "motivo: String", "usuarioId: Long"])
    C["Uni"] = classe(s, 1470, 270 + OY, "Unidade", ["id: Long", "nome: String", "cidade: String", "ativa: boolean"], w_min=200)

    def ligar(a, la, fa, b, lb, fb, ca="", cb="", via=None, fim=None, inicio=None, dash=None):
        p, q = C[a].pt(la, fa), C[b].pt(lb, fb)
        pts = [p] + (via or []) + [q]
        s.poly(pts, fim=fim, inicio=inicio, dash=dash)
        if ca:
            rotulo_extremo(s, pts[0], pts[1], ca, 18, 12)
        if cb:
            rotulo_extremo(s, pts[-1], pts[-2], cb, 18, 12)

    ligar("Usuario", "r", .2, "Pedido", "l", .1, "0..1", "*")
    ligar("Usuario", "b", .5, "Conta", "t", .5, "1", "0..1")
    ligar("Conta", "b", .5, "Mov", "t", .5, "1", "*")
    ligar("Pedido", "r", .08, "Item", "l", .25, "1", "1..*", inicio="losango")
    ligar("Pedido", "r", .55, "Promo", "l", .2, "*", "0..1", fim="seta-aberta")
    ligar("Pedido", "r", .72, "Status", "l", .3, fim="seta-aberta", dash="5 4")
    ligar("Pedido", "r", .92, "Canal", "l", .3, via=[(700, 330 + OY), (740, 735 + OY)], fim="seta-aberta", dash="5 4")
    ligar("Pedido", "b", .3, "Pag", "t", .3, "1", "*")
    ligar("Item", "r", .3, "Prod", "l", .3, "*", "1", fim="seta-aberta")
    ligar("Prod", "b", .5, "Card", "t", .5, "1", "*")
    ligar("Card", "b", .5, "Est", "t", .5, "1", "*")
    ligar("Est", "b", .5, "MovE", "t", .5, "", "", dash="5 4", fim="seta-aberta")
    s.text(1290, 618 + OY, "gera", 11, "start", fill="#546e7a", italic=True)
    ligar("Uni", "l", .5, "Card", "r", .5, "1", "*")
    alvo = C["Est"].pt("r", .8)
    ligar("Uni", "b", .5, "Est", "r", .8, "1", "*", via=[(C["Uni"].x + C["Uni"].w / 2, alvo[1])])
    px = 380 + C["Pedido"].w * .75
    ligar("Uni", "t", .5, "Pedido", "t", .75, "1", "*", via=[(C["Uni"].x + C["Uni"].w / 2, 62), (px, 62)])
    s.poly([C["Prod"].pt("b", .08), (C["Prod"].x - 16, C["Prod"].pt("b", .08)[1]), (C["Prod"].x - 16, C["Est"].pt("l", .3)[1]), C["Est"].pt("l", .3)], stroke="#455a64")
    s.text(C["Prod"].x - 28, C["Prod"].pt("b", .08)[1] + 14, "1", 11, "middle", fill="#b71c1c", weight="bold")
    s.text(C["Est"].x - 12, C["Est"].pt("l", .3)[1] - 6, "*", 11, "middle", fill="#b71c1c", weight="bold")
    ligar("Mock", "t", .5, "Gw", "b", .5, via=None, fim="triangulo", dash="5 4")
    s.salvar("04-diagrama-de-classes")


# ---------------------------------------------------------------- 5. sequência
def sequencia():
    nomes = ["Cliente\n(APP/TOTEM/WEB)", "api\n(Controllers)", "PedidoService", "EstoqueService", "PagamentoService", "Gateway\nmock", "Banco de\ndados", "Cozinha /\nAtendente"]
    xs = [100 + i * 200 for i in range(len(nomes))]
    W, passo = xs[-1] + 120, 34
    eventos = [
        ("m", 0, 1, "POST /pedidos {canalPedido, unidadeId, itens, formaPagamento}", 0),
        ("m", 1, 2, "criar(...)", 0),
        ("m", 2, 6, "valida unidade e preços do cardápio", 0),
        ("m", 2, 6, "salva pedido (AGUARDANDO_PAGAMENTO, total calculado)", 0),
        ("m", 2, 3, "registrarSaida (reserva)", 0),
        ("m", 3, 6, "baixa estoque (com lock) + movimentação", 0),
        ("alt", "estoque insuficiente"),
        ("m", 3, 0, "409 ESTOQUE_INSUFICIENTE (tudo desfeito)", 1),
        ("else", "estoque ok"),
        ("m", 2, 6, "auditoria PEDIDO_CRIADO", 0),
        ("m", 1, 0, "201 {pedidoId, status, total}", 1),
        ("fim",),
        ("gap",),
        ("m", 0, 1, "POST /pagamentos {pedidoId} + Idempotency-Key", 0),
        ("m", 1, 4, "pagar(...)", 0),
        ("m", 4, 6, "bloqueia o pedido (lock)", 0),
        ("m", 4, 5, "processar(valor, forma)", 0),
        ("alt", "aprovado"),
        ("m", 5, 4, "APPROVED + transacaoId", 1),
        ("m", 4, 6, "pagamento APROVADO; pedido EM_PREPARO; pontos; auditoria", 0),
        ("m", 1, 0, "201 {status APROVADO, gateway{...}}", 1),
        ("else", "recusado"),
        ("m", 5, 4, "DECLINED", 1),
        ("m", 4, 6, "pagamento RECUSADO (pedido segue AGUARDANDO_PAGAMENTO)", 0),
        ("m", 1, 0, "201 {status RECUSADO, mensagem}", 1),
        ("else", "gateway indisponível"),
        ("m", 5, 4, "timeout / indisponível", 1),
        ("m", 4, 6, "pagamento PENDENTE", 0),
        ("m", 1, 0, "202 {status PENDENTE} - repetir com a mesma Idempotency-Key", 1),
        ("fim",),
        ("gap",),
        ("m", 7, 1, "PATCH /pedidos/{id}/status {PRONTO}", 0),
        ("m", 1, 2, "atualizarStatus (valida a transição)", 0),
        ("m", 1, 7, "200 PRONTO", 1),
        ("m", 7, 1, "PATCH /pedidos/{id}/status {ENTREGUE}", 0),
        ("m", 1, 7, "200 ENTREGUE", 1),
    ]

    def desenhar(s, h):
        for x, n in zip(xs, nomes):
            partes = n.split("\n")
            s.rect(x - 75, 48, 150, 44, "#e3f2fd", "#1565c0", 1.4, 6)
            for i, p in enumerate(partes):
                s.text(x, 68 + (0 if len(partes) == 1 else -6) + i * 14 + 4, p, 12, "middle", "bold")
            s.poly([(x, 92), (x, h - 24)], dash="4 4", stroke="#90a4ae", sw=1)
        y = 132
        quadros = []
        for ev in eventos:
            t = ev[0]
            if t == "m":
                _, a, b, txt, tracejada = ev
                xa, xb = xs[a], xs[b]
                s.poly([(xa, y), (xb, y)], fim="seta", dash="6 4" if tracejada else None)
                larg = len(txt) * 6.2
                if larg > abs(xb - xa) - 12:
                    s.text(min(xa, xb) + 8, y - 6, txt, 11.5, "start")
                else:
                    s.text((xa + xb) / 2, y - 6, txt, 11.5, "middle")
                y += passo
            elif t == "alt":
                quadros.append([y - 18, ev[1], []])
                y += 24
            elif t == "else":
                quadros[-1][2].append((y - 16, ev[1]))
                y += 26
            elif t == "fim":
                top, rot, divs = quadros.pop()
                bot = y - 14
                s.rect(40, top, W - 80, bot - top, "none", "#e65100", 1.3, 4)
                s.rect(40, top, 190, 20, "#fff3e0", "#e65100", 1.3)
                s.text(50, top + 14, "alt  [" + rot + "]", 11.5, fill="#e65100", weight="bold")
                for dy, r in divs:
                    s.poly([(40, dy), (W - 40, dy)], dash="6 4", stroke="#e65100", sw=1.1)
                    s.text(50, dy + 14, "[" + r + "]", 11.5, fill="#e65100", weight="bold")
                y += 8
            elif t == "gap":
                y += 14
        return y

    fim_y = desenhar(Svg(10, 10, ""), 0)
    h = fim_y + 30
    s = Svg(W, h, "Diagrama de sequência")
    s.text(W / 2, 28, "Diagrama de sequência — Pedido → Pagamento externo (mock) → Status", 18, "middle", "bold")
    desenhar(s, h)
    s.salvar("05-diagrama-de-sequencia")


# ---------------------------------------------------------------- 6. estados
def estados():
    s = Svg(1240, 530, "Máquina de estados do pedido")
    s.text(620, 30, "Máquina de estados do pedido", 18, "middle", "bold")
    OY = -40

    def estado(x, y, nome, w=210, cor="#e3f2fd"):
        s.rect(x, y, w, 56, cor, "#1565c0", 1.5, 14)
        s.text(x + w / 2, y + 33, nome, 13, "middle", "bold")
        return Caixa(x, y, w, 56)

    A = estado(70, 130 + OY, "AGUARDANDO_PAGAMENTO", 240)
    E = estado(430, 130 + OY, "EM_PREPARO")
    P = estado(720, 130 + OY, "PRONTO", 170)
    N = estado(1000, 130 + OY, "ENTREGUE", 170, "#e8f5e9")
    C = estado(250, 380 + OY, "CANCELADO", 210, "#ffebee")
    s.circle(30, 158 + OY, 8, "#37474f", "#37474f")
    s.poly([(38, 158 + OY), (70, 158 + OY)], "seta")
    s.text(14, 72, "POST /pedidos", 11, "start", fill="#546e7a", italic=True)
    s.poly([A.pt("r", .5), E.pt("l", .5)], "seta")
    s.linhas(370, 136 + OY, ["pagamento", "aprovado"], 11, "middle", 13, fill="#37474f")
    s.poly([E.pt("r", .5), P.pt("l", .5)], "seta")
    s.text(675, 152 + OY, "cozinha", 11, "middle", fill="#37474f")
    s.poly([P.pt("r", .5), N.pt("l", .5)], "seta")
    s.text(945, 152 + OY, "atendente", 11, "middle", fill="#37474f")
    s.poly([A.pt("b", .5), (190, 300 + OY), C.pt("l", .5)], "seta")
    s.text(40, 310 + OY, "cancelamento", 11, fill="#37474f")
    s.text(40, 324 + OY, "(devolve estoque e pontos)", 11, fill="#37474f")
    s.poly([E.pt("b", .5), (535, 300 + OY), (440, 300 + OY), C.pt("t", .85)], "seta")
    s.linhas(640, 280 + OY, ["cancelamento pela equipe", "(estorna o pagamento,", "devolve estoque e pontos)"], 11, "middle", 13, fill="#37474f")
    s.circle(1085, 330 + OY, 8, "#fff", "#37474f")
    s.circle(1085, 330 + OY, 4, "#37474f", "#37474f")
    s.poly([N.pt("b", .5), (1085, 322 + OY)], "seta")
    s.circle(355, 500 + OY, 8, "#fff", "#37474f")
    s.circle(355, 500 + OY, 4, "#37474f", "#37474f")
    s.poly([C.pt("b", .5), (355, 492 + OY)], "seta")
    s.text(620, 512, "Transição inválida → 409 TRANSICAO_INVALIDA.  EM_PREPARO só ocorre por pagamento aprovado; CANCELADO só pelo endpoint de cancelamento.", 12, "middle", fill="#37474f")
    s.salvar("06-maquina-de-estados")

if __name__ == "__main__":
    for f in (arquitetura, casos_de_uso, der, classes, sequencia, estados):
        f()
