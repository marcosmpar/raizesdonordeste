import type { Erro } from "./tipos";

export class FalhaBff extends Error {
  constructor(
    readonly status: number,
    readonly erro: Erro,
  ) {
    super(erro.message);
  }
}

/** Chamada do navegador para o BFF (mesma origem; o cookie de sessão vai junto). */
export async function bff<T>(
  caminho: string,
  opcoes: { metodo?: string; corpo?: unknown; cabecalhos?: Record<string, string> } = {},
): Promise<{ status: number; dados: T }> {
  const resposta = await fetch(`/api${caminho}`, {
    method: opcoes.metodo ?? "GET",
    headers: {
      ...(opcoes.corpo === undefined ? {} : { "Content-Type": "application/json" }),
      ...opcoes.cabecalhos,
    },
    body: opcoes.corpo === undefined ? undefined : JSON.stringify(opcoes.corpo),
  });
  const dados = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    throw new FalhaBff(resposta.status, {
      error: dados?.error ?? "ERRO",
      message: dados?.message ?? "Algo deu errado. Tente novamente.",
      details: dados?.details ?? [],
    });
  }
  return { status: resposta.status, dados: dados as T };
}

export function mensagemDe(e: unknown): string {
  if (!(e instanceof FalhaBff)) return "Falha de conexão. Verifique sua rede e tente novamente.";
  const detalhes = e.erro.details?.map((d) => `${d.field}: ${d.issue}`).join("; ");
  return detalhes ? `${e.erro.message} (${detalhes})` : e.erro.message;
}
