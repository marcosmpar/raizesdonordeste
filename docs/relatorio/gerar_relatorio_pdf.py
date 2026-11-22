#!/usr/bin/env python3
"""Gera o relatório final em PDF, no padrão ABNT, a partir de docs/RELATORIO.md.

Sem dependências externas: converte o Markdown para HTML e usa o Google Chrome (modo headless)
para imprimir o PDF. O sumário e as listas recebem os números de página reais (duas passagens).

Uso (a partir da raiz do projeto):
    python3 docs/relatorio/gerar_relatorio_pdf.py

Dados da capa: copie docs/relatorio/capa.exemplo.json para capa.json e preencha (o capa.json não é versionado).
"""
import html
import json
import os
import re
import subprocess
import sys
import tempfile
import zlib

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
DOCS = os.path.join(RAIZ, "docs")
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

RESUMO = ("Este trabalho apresenta a análise, a modelagem e a implementação de um back-end REST para a rede de restaurantes "
          "Raízes do Nordeste, que atende clientes por múltiplos canais (aplicativo, totem, balcão, retirada e web) em diferentes "
          "unidades. A solução foi desenvolvida em Java 17 com Spring Boot, organizada em camadas (api, application, domain e "
          "infrastructure), com persistência relacional versionada por migrations, autenticação por JWT e autorização por perfis. "
          "O fluxo crítico — pedido, pagamento em gateway simulado e atualização de status — foi entregue de forma completa e "
          "testável, junto com estoque por unidade, promoções, programa de fidelidade com consentimento e auditoria de ações "
          "sensíveis. São descritos os requisitos, os diagramas de casos de uso, de entidade-relacionamento, de classes e de "
          "sequência, o contrato da API documentado em OpenAPI, os cuidados de segurança e de proteção de dados pessoais conforme "
          "a LGPD e o plano de testes. A validação combinou 63 testes automatizados, incluindo cenários de concorrência, e uma "
          "coleção de 55 cenários executáveis no Postman. Como resultado, obteve-se uma API reproduzível e documentada, com "
          "limitações e próximos passos explicitados.")
PALAVRAS_CHAVE = "Back-end. API REST. Spring Boot. Pedidos. LGPD."


# ============================================================ Markdown -> HTML
def esc(s):
    return html.escape(s, quote=False)


def inline(t):
    """Converte marcações inline (código, imagem, link, negrito, itálico)."""
    guardados = []

    def guarda(h):
        guardados.append(h)
        return f"\u0000{len(guardados) - 1}\u0000"

    t = re.sub(r"`([^`]+)`", lambda m: guarda(f"<code>{esc(m.group(1))}</code>"), t)
    t = esc(t)
    t = re.sub(r"\[([^\]]+)\]\((https?://[^)\s]+)\)", r'<a class="ext" href="\2">\1</a>', t)
    t = re.sub(r"\[([^\]]+)\]\((?!https?://)[^)]*\)", r"\1", t)  # links relativos: só o texto
    t = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", t)
    t = re.sub(r"(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])", r"<em>\1</em>", t)
    t = re.sub(r"(?<![\w])_(?!\s)(.+?)(?<!\s)_(?![\w])", r"<em>\1</em>", t)
    t = re.sub(r'(?<![">=/\w])(https?://[^\s<]+?)(?=[.,;:)]*(?:\s|<|$))', r'<span class="url">\1</span>', t)
    return re.sub(r"\u0000(\d+)\u0000", lambda m: guardados[int(m.group(1))], t)


def dividir_linha_tabela(l):
    l = l.strip().strip("|")
    partes, atual, i = [], "", 0
    while i < len(l):
        if l[i] == "\\" and i + 1 < len(l) and l[i + 1] == "|":
            atual += "|"
            i += 2
        elif l[i] == "|":
            partes.append(atual.strip())
            atual = ""
            i += 1
        else:
            atual += l[i]
            i += 1
    partes.append(atual.strip())
    return partes


class Doc:
    def __init__(self):
        self.contadores = [0, 0, 0, 0]
        self.sumario = []     # (nivel, numero, titulo, id)
        self.figuras = []     # (n, titulo, id)
        self.quadros = []     # (n, titulo, id)
        self.ultimo_titulo = ""
        self.n_id = 0
        self.refs = False

    def novo_id(self, prefixo):
        self.n_id += 1
        return f"{prefixo}{self.n_id}"


def limpar_titulo(t):
    return re.sub(r"^\d+(\.\d+)*\.?\s+", "", t.strip())


def blocos(linhas, d):
    """Converte uma lista de linhas Markdown em HTML."""
    out, i, n = [], 0, len(linhas)
    while i < n:
        l = linhas[i]
        s = l.strip()
        if not s:
            i += 1
            continue
        # código
        if s.startswith("```"):
            ind = len(l) - len(l.lstrip())
            i += 1
            cod = []
            while i < n and not linhas[i].strip().startswith("```"):
                cod.append(linhas[i][ind:] if linhas[i][:ind].strip() == "" else linhas[i])
                i += 1
            i += 1
            out.append(f"<pre><code>{esc(chr(10).join(cod))}</code></pre>")
            continue
        # título
        m = re.match(r"^(#{1,6})\s+(.*)$", s)
        if m:
            nivel = len(m.group(1)) - 1  # '##' -> 1 (seção primária)
            titulo = limpar_titulo(m.group(2))
            d.ultimo_titulo = titulo
            if nivel == 1 and titulo.upper() == "REFERÊNCIAS":
                hid = d.novo_id("s")
                d.sumario.append((1, "", "REFERÊNCIAS", hid))
                out.append(f'<h2 id="{hid}" class="sem-numero">REFERÊNCIAS</h2><div class="refs">')
                d.refs = True
            elif nivel >= 1:
                nivel = min(nivel, 4)
                d.contadores[nivel - 1] += 1
                for k in range(nivel, 4):
                    d.contadores[k] = 0
                num = ".".join(str(c) for c in d.contadores[:nivel])
                hid = d.novo_id("s")
                if nivel <= 3:
                    d.sumario.append((nivel, num, titulo, hid))
                tag = f"h{nivel + 1}"
                out.append(f'<{tag} id="{hid}"><span class="num">{num}</span> {inline(titulo)}</{tag}>')
            i += 1
            continue
        if re.match(r"^-{3,}$", s):
            i += 1
            continue
        # imagem
        m = re.match(r"^!\[([^\]]*)\]\(([^)]+)\)$", s)
        if m:
            alt, caminho = m.group(1), m.group(2)
            arq = os.path.join(DOCS, re.sub(r"\.png$", ".svg", caminho))
            fid = d.novo_id("f")
            d.figuras.append((len(d.figuras) + 1, alt, fid))
            out.append(f'<div class="fig-pagina"><figure id="{fid}"><figcaption>Figura {len(d.figuras)} – {inline(alt)}</figcaption>'
                       f'<img src="file://{arq}" alt="{esc(alt)}"><div class="fonte">Fonte: Elaborado pelo autor (2026).</div></figure></div>')
            i += 1
            continue
        if s.startswith("_Fonte vetorial"):
            i += 1
            continue
        # tabela
        if s.startswith("|") and i + 1 < n and re.match(r"^\|?\s*:?-{2,}", linhas[i + 1].strip().lstrip("|")):
            cab = dividir_linha_tabela(s)
            i += 2
            linhas_t = []
            while i < n and linhas[i].strip().startswith("|"):
                linhas_t.append(dividir_linha_tabela(linhas[i]))
                i += 1
            qid = d.novo_id("q")
            usos = sum(1 for _, ti, _ in d.quadros if ti.split(" (parte")[0] == d.ultimo_titulo)
            titulo_q = d.ultimo_titulo if usos == 0 else f"{d.ultimo_titulo} (parte {usos + 1})"
            d.quadros.append((len(d.quadros) + 1, titulo_q, qid))
            larga = len(cab) >= 7
            h = [f'<div class="quadro{" larga" if larga else ""}" id="{qid}"><div class="legenda">Quadro {len(d.quadros)} – {inline(titulo_q)}</div>',
                 "<table><thead><tr>" + "".join(f"<th>{inline(c)}</th>" for c in cab) + "</tr></thead><tbody>"]
            for r in linhas_t:
                r = (r + [""] * len(cab))[:len(cab)]
                h.append("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in r) + "</tr>")
            h.append('</tbody></table><div class="fonte">Fonte: Elaborado pelo autor (2026).</div></div>')
            out.append("".join(h))
            continue
        # citação
        if s.startswith(">"):
            q = []
            while i < n and linhas[i].strip().startswith(">"):
                q.append(linhas[i].strip().lstrip(">").strip())
                i += 1
            out.append(f"<blockquote>{inline(' '.join(q))}</blockquote>")
            continue
        # lista
        m = re.match(r"^(\s*)([-*]|\d+\.)\s+(.*)$", l)
        if m:
            ordenada = m.group(2)[0].isdigit()
            base = len(m.group(1))
            itens = []
            while i < n:
                mm = re.match(r"^(\s*)([-*]|\d+\.)\s+(.*)$", linhas[i])
                if mm and len(mm.group(1)) == base:
                    itens.append([mm.group(3)])
                    i += 1
                    # continuação: linhas mais indentadas (ou em branco seguidas de indentadas)
                    while i < n:
                        nxt = linhas[i]
                        if nxt.strip() == "":
                            j = i
                            while j < n and linhas[j].strip() == "":
                                j += 1
                            if j < n and (len(linhas[j]) - len(linhas[j].lstrip())) > base and not re.match(r"^(\s*)([-*]|\d+\.)\s", linhas[j]) or (j < n and (len(linhas[j]) - len(linhas[j].lstrip())) > base):
                                itens[-1].extend([""] * (j - i))
                                i = j
                                continue
                            break
                        if (len(nxt) - len(nxt.lstrip())) > base:
                            itens[-1].append(nxt)
                            i += 1
                            continue
                        break
                else:
                    break
            tag = "ol" if ordenada else "ul"
            corpo = []
            for it in itens:
                primeira, resto = it[0], it[1:]
                recuo = min([len(x) - len(x.lstrip()) for x in resto if x.strip()] or [0])
                resto = [x[recuo:] for x in resto]
                extra = blocos(resto, d) if resto else ""
                corpo.append(f"<li>{inline(primeira)}{extra}</li>")
            out.append(f"<{tag}>{''.join(corpo)}</{tag}>")
            continue
        # parágrafo
        par = [s]
        i += 1
        while i < n and linhas[i].strip() and not re.match(r"^(#{1,6}\s|```|\||>|!\[|\s*([-*]|\d+\.)\s)", linhas[i].strip() if not linhas[i].startswith(" ") else linhas[i]):
            par.append(linhas[i].strip())
            i += 1
        texto = " ".join(par)
        if texto.startswith("Os diagramas estão"):
            continue
        out.append(f"<p>{inline(texto)}</p>")
    return "".join(out)


# ============================================================ HTML / CSS
CSS = """
@page { size: A4; margin: 3cm 2cm 2cm 3cm;
        @top-right { content: counter(page); vertical-align: top; padding-top: 1.55cm; font: 12pt 'Times New Roman', Times, serif; } }
@page sem-numero { @top-right { content: none; } }
@page paisagem { size: A4 landscape; margin: 3cm 2cm 2cm 3cm;
        @top-right { content: counter(page); vertical-align: top; padding-top: 1.55cm; font: 12pt 'Times New Roman', Times, serif; } }
* { box-sizing: border-box; }
html { font-family: 'Times New Roman', Times, 'Liberation Serif', serif; font-size: 12pt; color: #000; }
body { margin: 0; line-height: 1.5; text-align: justify; hyphens: auto; }
p { margin: 0; text-indent: 1.25cm; }
h2, h3, h4, h5 { font-size: 12pt; font-weight: bold; margin: 0; text-align: left; break-after: avoid; line-height: 1.5; }
h2 { text-transform: uppercase; break-before: page; margin-bottom: 18pt; }
h2.sem-numero { text-align: center; }
h3 { margin: 18pt 0 12pt; }
h4 { margin: 14pt 0 8pt; font-weight: normal; font-style: normal; text-decoration: none; }
h4 .num { font-weight: bold; }
h5 { margin: 12pt 0 6pt; font-style: italic; }
h2 .num, h3 .num, h4 .num, h5 .num { margin-right: 0.3em; }
ul, ol { margin: 0 0 0 1.25cm; padding-left: 0.6cm; }
li { margin: 0; text-align: left; }
li p { text-indent: 0; }
code { font-family: Menlo, 'Courier New', monospace; font-size: 9.5pt; hyphens: none; }
.url { hyphens: none; overflow-wrap: anywhere; }
pre { font-family: Menlo, 'Courier New', monospace; font-size: 8.5pt; line-height: 1.25; text-align: left; margin: 6pt 0 10pt;
      padding: 6pt 8pt; border: 0.5pt solid #777; background: #f6f6f6; white-space: pre-wrap; word-break: break-word; break-inside: avoid; }
pre code { font-size: 8.5pt; }
blockquote { margin: 6pt 0 6pt 4cm; font-size: 10pt; line-height: 1.15; text-indent: 0; }
a { color: #000; text-decoration: none; }
a.ext { text-decoration: underline; }
.quadro { margin: 12pt 0; break-inside: auto; }
.legenda { font-size: 10pt; line-height: 1.2; text-align: left; font-weight: bold; margin-bottom: 4pt; break-after: avoid; }
.fonte { font-size: 10pt; line-height: 1.2; text-align: left; margin-top: 4pt; }
table { border-collapse: collapse; width: 100%; font-size: 9pt; line-height: 1.2; text-align: left; hyphens: none; }
th, td { border: 0.6pt solid #000; padding: 3pt 4pt; vertical-align: top; word-break: normal; overflow-wrap: break-word; }
td code, th code { font-size: 7.8pt; overflow-wrap: anywhere; }
th { background: #e6e6e6; }
tr { break-inside: avoid; }
thead { display: table-header-group; }
.larga { page: paisagem; break-before: page; break-after: page; }
.larga table { font-size: 7.5pt; }
.fig-pagina { page: paisagem; break-before: page; break-after: page; text-indent: 0; }
figure { margin: 0; text-align: center; }
figcaption { font-size: 10pt; line-height: 1.2; font-weight: bold; text-align: center; margin-bottom: 6pt; }
figure img { max-width: 24.7cm; max-height: 13.3cm; width: auto; height: auto; display: block; margin: 0 auto; }
figure .fonte { text-align: center; }
/* elementos pré-textuais */
.pre { page: sem-numero; break-before: page; line-height: 1.5; text-align: center; text-indent: 0; }
.pre p { text-indent: 0; }
.capa { page: sem-numero; height: 24.6cm; display: flex; flex-direction: column; justify-content: space-between; text-align: center; text-transform: uppercase; }
.capa, .rosto, .toc, .lista { hyphens: none; }
.capa .topo, .capa .base { line-height: 1.5; }
.capa .meio { font-weight: bold; }
.capa .meio .sub { font-weight: normal; text-transform: none; margin-top: 10pt; }
.rosto { page: sem-numero; break-before: page; height: 24.6cm; display: flex; flex-direction: column; justify-content: space-between; text-align: center; }
.rosto .natureza { margin: 0 0 0 7.5cm; font-size: 10pt; line-height: 1.15; text-align: justify; }
.titulo-pre { font-weight: bold; text-align: center; text-transform: uppercase; margin-bottom: 18pt; text-indent: 0; }
.resumo-texto { text-align: justify; line-height: 1.15; text-indent: 0; }
.palavras { margin-top: 14pt; line-height: 1.15; text-align: left; text-indent: 0; }
.toc { line-height: 1.5; text-align: left; }
.toc a, .lista a { display: flex; align-items: flex-end; }
.toc .t, .lista .t { flex: 0 1 auto; }
.toc .pt, .lista .pt { flex: 1 1 auto; border-bottom: 1.2px dotted #000; margin: 0 4px 5px; min-width: 12px; }
.toc .p, .lista .p { flex: 0 0 auto; width: 1.2cm; text-align: right; }
.toc .n1 { font-weight: bold; text-transform: uppercase; }
.toc .n2 { padding-left: 0.8cm; }
.toc .n3 { padding-left: 1.6cm; }
.lista { line-height: 1.5; text-align: left; }
.lista a { display: flex; align-items: flex-end; }
.refs p { text-indent: 0; text-align: left; line-height: 1.15; margin-bottom: 12pt; hyphens: none; overflow-wrap: anywhere; }
"""


def pagina_pre(classe, conteudo):
    return f'<section class="pre {classe}">{conteudo}</section>'


def montar_html(corpo, d, capa, paginas):
    def pg(i):
        return str(paginas.get(i, "000"))

    toc = []
    for nivel, num, titulo, hid in d.sumario:
        rotulo = (num + " " if num else "") + titulo
        toc.append(f'<a href="#{hid}" class="n{nivel}"><span class="t">{inline(rotulo)}</span><span class="pt"></span><span class="p">{pg(hid)}</span></a>')
    lista_fig = "".join(f'<a href="#{fid}"><span class="t">Figura {n} – {inline(t)}</span><span class="pt"></span><span class="p">{pg(fid)}</span></a>' for n, t, fid in d.figuras)
    lista_q = "".join(f'<a href="#{qid}"><span class="t">Quadro {n} – {inline(t)}</span><span class="pt"></span><span class="p">{pg(qid)}</span></a>' for n, t, qid in d.quadros)

    ru = f"<br>RU: {esc(capa['ru'])}" if capa.get("ru") else ""
    cabeca = f"""
<section class="capa">
  <div class="topo"><strong>{esc(capa['instituicao'])}</strong><br>{esc(capa['curso'])}<br><br>{esc(capa['autor'])}{ru}</div>
  <div class="meio"><div>{esc(capa['titulo'])}</div><div class="sub">{esc(capa['subtitulo'])}</div></div>
  <div class="base">{esc(capa['cidade'])}<br>{esc(capa['ano'])}</div>
</section>
<section class="rosto">
  <div>{esc(capa['autor'])}{ru}</div>
  <div style="text-transform:uppercase;font-weight:bold">{esc(capa['titulo'])}<div style="font-weight:normal;text-transform:none;margin-top:10pt">{esc(capa['subtitulo'])}</div></div>
  <div class="natureza">Relatório do projeto de Back-end apresentado à disciplina {esc(capa['disciplina'])}, do curso {esc(capa['curso'])}, da {esc(capa['instituicao'])}, como requisito de avaliação.<br><br>Orientação: {esc(capa['professor'])}.</div>
  <div>{esc(capa['cidade'])}<br>{esc(capa['ano'])}</div>
</section>
{pagina_pre('', f'<div class="titulo-pre">Resumo</div><p class="resumo-texto">{esc(RESUMO)}</p><p class="palavras"><strong>Palavras-chave:</strong> {esc(PALAVRAS_CHAVE)}</p>')}
{pagina_pre('', f'<div class="titulo-pre">Lista de figuras</div><div class="lista">{lista_fig}</div>')}
{pagina_pre('', f'<div class="titulo-pre">Lista de quadros</div><div class="lista">{lista_q}</div>')}
{pagina_pre('', f'<div class="titulo-pre">Sumário</div><div class="toc">{"".join(toc)}</div>')}
"""
    return f'<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>{esc(capa["titulo"])}</title><style>{CSS}</style></head><body>{cabeca}{corpo}</body></html>'


# ============================================================ PDF -> páginas
def paginas_do_pdf(caminho):
    """Lê o PDF gerado pelo Chrome e devolve {id_do_destino: número da página (1 = capa)}."""
    dados = open(caminho, "rb").read()
    objs = {}
    for m in re.finditer(rb"(\d+) 0 obj\s*(.*?)\s*endobj", dados, re.S):
        corpo = m.group(2)
        if b"stream" in corpo:
            corpo = corpo[:corpo.index(b"stream")]
        objs[int(m.group(1))] = corpo.decode("latin1")
    raiz = next(c for c in objs.values() if "/Type /Catalog" in c)
    pages_ref = int(re.search(r"/Pages (\d+) 0 R", raiz).group(1))
    folhas = []

    def percorre(ref):
        o = objs[ref]
        if "/Type /Pages" in o:
            kids = re.search(r"/Kids \[(.*?)\]", o, re.S).group(1)
            for k in re.findall(r"(\d+) 0 R", kids):
                percorre(int(k))
        else:
            folhas.append(ref)

    percorre(pages_ref)
    indice = {ref: i + 1 for i, ref in enumerate(folhas)}
    dests = re.search(r"/Dests (\d+) 0 R", raiz)
    tabela = objs[int(dests.group(1))] if dests else ""
    resultado = {}
    for nome, ref in re.findall(r"/([\w.-]+) \[(\d+) 0 R", tabela):
        resultado[nome] = indice[int(ref)]
    return resultado, len(folhas)


def imprimir(html_txt, saida):
    with tempfile.TemporaryDirectory() as tmp:
        arq = os.path.join(tmp, "relatorio.html")
        open(arq, "w", encoding="utf-8").write(html_txt)
        subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                        f"--print-to-pdf={saida}", "file://" + arq],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=300, check=True)


def main():
    if not os.path.exists(CHROME):
        sys.exit("Google Chrome não encontrado em " + CHROME)
    base = os.path.dirname(__file__)
    arq_capa = os.path.join(base, "capa.json")  # dados pessoais: fica fora do Git (.gitignore)
    if not os.path.exists(arq_capa):
        arq_capa = os.path.join(base, "capa.exemplo.json")
        print("capa.json não encontrado: usando capa.exemplo.json (copie-o para capa.json e preencha)")
    capa = json.load(open(arq_capa, encoding="utf-8"))
    texto = open(os.path.join(DOCS, "RELATORIO.md"), encoding="utf-8").read()
    texto = texto[texto.index("## 1 INTRODUÇÃO"):]  # descarta o cabeçalho de instruções do texto-base
    d = Doc()
    texto = texto.replace("{{", "{").replace("}}", "}").replace("Acesso em: _preencher_", "Acesso em: [data de acesso]")
    corpo = blocos(texto.splitlines(), d)
    if d.refs:
        corpo += "</div>"

    nome = f"{capa['ru']}_Projeto_Back_End.pdf" if capa.get("ru") else "Projeto_Back_End.pdf"
    saida = os.path.join(os.path.dirname(__file__), nome)
    paginas, anterior = {}, None
    for passo in range(4):
        imprimir(montar_html(corpo, d, capa, paginas), saida)
        novas, total = paginas_do_pdf(saida)
        print(f"passagem {passo + 1}: {total} páginas")
        if novas == paginas:
            break
        paginas = novas
    print("PDF:", saida)


if __name__ == "__main__":
    main()
