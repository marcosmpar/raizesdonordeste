import { NextRequest } from "next/server";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { POST as login } from "@/app/api/auth/login/route";
import { POST as logout } from "@/app/api/auth/logout/route";
import { POST as pagar } from "@/app/api/pagamentos/route";
import { GET as painel } from "@/app/api/painel/route";
import { GET as detalhe } from "@/app/api/pedidos/[id]/route";
import { GET as listar, POST as criar } from "@/app/api/pedidos/route";

const api = vi.fn<typeof fetch>();
beforeEach(() => vi.stubGlobal("fetch", api));
afterEach(() => {
  vi.unstubAllGlobals();
  api.mockReset();
});

const json = (corpo: unknown, status = 200) => new Response(JSON.stringify(corpo), { status });

const cookiesDe = (perfil: string) => {
  const usuario = Buffer.from(JSON.stringify({ id: 1, nome: "Teste", perfil })).toString("base64url");
  return `rn_token=jwt-de-teste; rn_usuario=${usuario}`;
};

function pedir(
  caminho: string,
  { metodo = "GET", corpo, perfil, cabecalhos = {} }: {
    metodo?: string;
    corpo?: unknown;
    perfil?: string;
    cabecalhos?: Record<string, string>;
  } = {},
) {
  return new NextRequest(`http://localhost:3000${caminho}`, {
    method: metodo,
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
    headers: {
      host: "localhost:3000",
      ...(metodo === "GET" ? {} : { origin: "http://localhost:3000" }),
      ...(perfil ? { cookie: cookiesDe(perfil) } : {}),
      ...cabecalhos,
    },
  });
}

const semParams = { params: Promise.resolve({}) };
const comId = (id: string) => ({ params: Promise.resolve({ id }) });
const enviado = (n = 0) => {
  const [url, init] = api.mock.calls[n];
  return {
    url: String(url),
    headers: init?.headers as Record<string, string>,
    corpo: init?.body ? JSON.parse(String(init.body)) : undefined,
  };
};

describe("sessão", () => {
  it("guarda o token em cookie httpOnly e não o devolve ao navegador", async () => {
    api.mockResolvedValue(
      json({ accessToken: "segredo", expiresIn: 3600, user: { id: 1, nome: "Ana", perfil: "CLIENTE" } }),
    );
    const res = await login(
      pedir("/api/auth/login", { metodo: "POST", corpo: { email: "a@a.com", senha: "x" } }),
      semParams,
    );
    expect(res.status).toBe(200);
    expect(await res.text()).not.toContain("segredo");
    const cookie = res.cookies.get("rn_token");
    expect(cookie).toMatchObject({ value: "segredo", httpOnly: true, sameSite: "lax", maxAge: 3600 });
  });

  it("repassa o erro de credenciais da API", async () => {
    api.mockResolvedValue(json({ error: "CREDENCIAIS_INVALIDAS", message: "x" }, 401));
    const res = await login(pedir("/api/auth/login", { metodo: "POST", corpo: {} }), semParams);
    expect(res.status).toBe(401);
    expect((await res.json()).error).toBe("CREDENCIAIS_INVALIDAS");
  });

  it("logout apaga os cookies", async () => {
    const res = await logout(pedir("/api/auth/logout", { metodo: "POST" }), semParams);
    expect(res.status).toBe(204);
    expect(res.cookies.get("rn_token")?.maxAge).toBe(0);
  });

  it("descarta a sessão quando a API recusa o token", async () => {
    api.mockResolvedValue(json({ error: "NAO_AUTENTICADO" }, 401));
    const res = await listar(pedir("/api/pedidos", { perfil: "CLIENTE" }), semParams);
    expect(res.status).toBe(401);
    expect(res.cookies.get("rn_token")?.maxAge).toBe(0);
  });
});

describe("proteções", () => {
  it("exige login sem chamar a API", async () => {
    const res = await listar(pedir("/api/pedidos"), semParams);
    expect(res.status).toBe(401);
    expect(api).not.toHaveBeenCalled();
  });

  it("recusa mutação de outra origem", async () => {
    const res = await criar(
      pedir("/api/pedidos", {
        metodo: "POST",
        perfil: "CLIENTE",
        cabecalhos: { origin: "http://malicioso.example" },
      }),
      semParams,
    );
    expect(res.status).toBe(403);
    expect(api).not.toHaveBeenCalled();
  });

  it("não aceita id que não seja numérico", async () => {
    const res = await detalhe(pedir("/api/pedidos/x", { perfil: "ADMIN" }), comId("../usuarios"));
    expect(res.status).toBe(404);
    expect(api).not.toHaveBeenCalled();
  });

  it("nega o painel ao cliente", async () => {
    const res = await painel(pedir("/api/painel", { perfil: "CLIENTE" }), semParams);
    expect(res.status).toBe(403);
  });
});

describe("pedidos e pagamento", () => {
  it.each([
    ["CLIENTE", "WEB"],
    ["ATENDENTE", "BALCAO"],
  ])("define o canal pelo perfil %s, ignorando o cliente", async (perfil, canal) => {
    api.mockResolvedValue(json({ pedidoId: 1 }, 201));
    const res = await criar(
      pedir("/api/pedidos", {
        metodo: "POST",
        perfil,
        corpo: { unidadeId: 1, itens: [], canalPedido: "APP", clienteId: 99, total: 0 },
      }),
      semParams,
    );
    expect(res.status).toBe(201);
    expect(enviado().corpo).toEqual({ unidadeId: 1, itens: [], canalPedido: canal, formaPagamento: "MOCK" });
    expect(enviado().headers.Authorization).toBe("Bearer jwt-de-teste");
  });

  it("repassa a chave de idempotência e o status 202", async () => {
    api.mockResolvedValue(json({ status: "PENDENTE" }, 202));
    const res = await pagar(
      pedir("/api/pagamentos", {
        metodo: "POST",
        perfil: "CLIENTE",
        corpo: { pedidoId: 1, simulacao: "INDISPONIVEL" },
        cabecalhos: { "idempotency-key": "abc" },
      }),
      semParams,
    );
    expect(res.status).toBe(202);
    expect(enviado().headers["Idempotency-Key"]).toBe("abc");
  });

  it("monta o detalhe mesmo quando a cozinha não pode ver pagamentos", async () => {
    api.mockImplementation(async (url) => {
      const caminho = new URL(String(url)).pathname;
      if (caminho.endsWith("/pagamentos")) return json({ error: "ACESSO_NEGADO" }, 403);
      if (caminho.startsWith("/unidades")) return json({ id: 2, nome: "Recife" });
      return json({ pedidoId: 7, unidadeId: 2 });
    });
    const res = await detalhe(pedir("/api/pedidos/7", { perfil: "COZINHA" }), comId("7"));
    expect(await res.json()).toMatchObject({ pagamentos: [], unidade: { nome: "Recife" } });
  });
});

describe("lista de pedidos", () => {
  const pagina = { content: [{ pedidoId: 7, unidadeId: 2 }], page: 1, totalPages: 1 };

  it("acrescenta itens e nome da unidade e limita o tamanho da página", async () => {
    api.mockImplementation(async (url) => {
      const caminho = new URL(String(url)).pathname;
      if (caminho === "/pedidos") return json(pagina);
      if (caminho === "/unidades") return json({ content: [{ id: 2, nome: "Recife" }] });
      return json({ itens: [{ produtoId: 1, nome: "Tapioca", quantidade: 2, precoUnitario: 1 }] });
    });
    const res = await listar(pedir("/api/pedidos?limit=100", { perfil: "CLIENTE" }), semParams);
    expect((await res.json()).content[0]).toMatchObject({
      unidadeNome: "Recife",
      itens: [{ nome: "Tapioca", quantidade: 2 }],
    });
    expect(enviado().url).toContain("limit=20");
  });

  it("mantém a lista quando os complementos falham", async () => {
    api.mockImplementation(async (url) =>
      new URL(String(url)).pathname === "/pedidos" ? json(pagina) : json({}, 500),
    );
    const res = await listar(pedir("/api/pedidos", { perfil: "CLIENTE" }), semParams);
    expect(res.status).toBe(200);
    expect((await res.json()).content[0]).toMatchObject({ itens: [], unidadeNome: null });
  });
});

describe("falhas da API", () => {
  it("responde 502 quando a API está fora do ar", async () => {
    api.mockRejectedValue(new TypeError("fetch failed"));
    const res = await listar(pedir("/api/pedidos", { perfil: "CLIENTE" }), semParams);
    expect(res.status).toBe(502);
    expect((await res.json()).error).toBe("BACKEND_INDISPONIVEL");
  });

  it("responde 504 quando a API demora demais", async () => {
    api.mockRejectedValue(new DOMException("tempo esgotado", "TimeoutError"));
    const res = await listar(pedir("/api/pedidos", { perfil: "CLIENTE" }), semParams);
    expect(res.status).toBe(504);
  });
});
