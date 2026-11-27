import { NextResponse } from "next/server";

/** Resposta da API Java já lida. */
export type RespostaApi<T = unknown> = { status: number; corpo: T };

export class ErroApi extends Error {
  constructor(
    readonly status: number,
    readonly corpo: unknown,
  ) {
    super(`API respondeu ${status}`);
  }
}

type Opcoes = {
  metodo?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  token?: string;
  corpo?: unknown;
  cabecalhos?: Record<string, string>;
};

/** Monta o corpo de erro no mesmo padrão da API Java. */
export function respostaErro(status: number, error: string, message: string, path: string) {
  return NextResponse.json(
    {
      error,
      message,
      details: [],
      timestamp: new Date().toISOString(),
      path,
      requestId: crypto.randomUUID(),
    },
    { status },
  );
}

/** Chama a API Java. Status 4xx/5xx viram ErroApi; falha de rede vira 502 e demora vira 504. */
export async function chamarApi<T>(caminho: string, opcoes: Opcoes = {}): Promise<RespostaApi<T>> {
  const base = process.env.BACKEND_URL ?? "http://localhost:8080";
  const limite = Number(process.env.BACKEND_TIMEOUT_MS) || 15000;
  const headers: Record<string, string> = { Accept: "application/json", ...opcoes.cabecalhos };
  if (opcoes.token) headers.Authorization = `Bearer ${opcoes.token}`;
  if (opcoes.corpo !== undefined) headers["Content-Type"] = "application/json";

  let resposta: Response;
  let texto: string;
  try {
    resposta = await fetch(new URL(caminho, base), {
      method: opcoes.metodo ?? "GET",
      headers,
      body: opcoes.corpo === undefined ? undefined : JSON.stringify(opcoes.corpo),
      cache: "no-store",
      signal: AbortSignal.timeout(limite),
    });
    texto = await resposta.text();
  } catch (e) {
    const demorou = e instanceof Error && e.name === "TimeoutError";
    throw new ErroApi(demorou ? 504 : 502, {
      error: demorou ? "BACKEND_TIMEOUT" : "BACKEND_INDISPONIVEL",
      message: demorou
        ? "A API demorou demais para responder. Tente novamente."
        : "Não foi possível falar com a API. Tente novamente em instantes.",
      details: [],
      timestamp: new Date().toISOString(),
      path: caminho,
      requestId: crypto.randomUUID(),
    });
  }

  let corpo: unknown = null;
  if (texto) {
    try {
      corpo = JSON.parse(texto);
    } catch {
      throw new ErroApi(502, {
        error: "RESPOSTA_INVALIDA",
        message: "A API devolveu uma resposta inesperada.",
        details: [],
      });
    }
  }
  if (!resposta.ok) throw new ErroApi(resposta.status, corpo);
  return { status: resposta.status, corpo: corpo as T };
}
