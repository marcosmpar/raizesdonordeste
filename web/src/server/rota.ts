import { NextRequest, NextResponse } from "next/server";
import { ErroApi, respostaErro } from "./backend";
import { encerrarSessao, lerToken } from "./sessao";

type Contexto<P> = { params: Promise<P> };
type Entrada<P> = { req: NextRequest; params: P; token: string | undefined };

/**
 * Envelope comum das rotas do BFF: bloqueia mutações vindas de outra origem,
 * exige sessão quando pedido e traduz erros da API para a resposta HTTP.
 */
export function rota<P = Record<string, never>>(
  tratar: (entrada: Entrada<P>) => Promise<NextResponse>,
  { autenticada = false } = {},
) {
  return async (req: NextRequest, contexto: Contexto<P>) => {
    const caminho = req.nextUrl.pathname;
    if (req.method !== "GET" && !mesmaOrigem(req)) {
      return respostaErro(403, "ORIGEM_INVALIDA", "Requisição de origem não permitida.", caminho);
    }
    const token = lerToken(req);
    if (autenticada && !token) {
      return respostaErro(401, "NAO_AUTENTICADO", "Faça login para continuar.", caminho);
    }
    try {
      const params = await contexto.params;
      return await tratar({ req, params, token });
    } catch (e) {
      if (!(e instanceof ErroApi)) throw e;
      const res = NextResponse.json(e.corpo, { status: e.status });
      // token expirado ou inválido: descarta a sessão
      if (e.status === 401 && token) encerrarSessao(res);
      return res;
    }
  };
}

function mesmaOrigem(req: NextRequest): boolean {
  const origem = req.headers.get("origin");
  // navegadores sempre enviam Origin em mutações; ausência indica cliente que não é navegador
  if (!origem) return req.headers.get("sec-fetch-site") !== "cross-site";
  const host = req.headers.get("x-forwarded-host") ?? req.headers.get("host");
  try {
    return new URL(origem).host === host;
  } catch {
    return false;
  }
}

/** Aceita apenas ids numéricos no caminho, evitando montar URLs arbitrárias para a API. */
export function id(valor: string): string {
  if (!/^\d{1,18}$/.test(valor)) {
    throw new ErroApi(404, {
      error: "RECURSO_NAO_ENCONTRADO",
      message: "Recurso não encontrado.",
      details: [],
    });
  }
  return valor;
}

/** Corpo JSON do pedido; vazio se ausente ou malformado (a API valida os campos). */
export async function corpoJson(req: NextRequest): Promise<Record<string, unknown>> {
  try {
    const dados: unknown = await req.json();
    return dados && typeof dados === "object" ? (dados as Record<string, unknown>) : {};
  } catch {
    return {};
  }
}
