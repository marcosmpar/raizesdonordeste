import { NextResponse } from "next/server";
import type { ContaFidelidade, MovimentoPontos, Pagina } from "@/lib/tipos";
import { chamarApi } from "@/server/backend";
import { rota } from "@/server/rota";

export const GET = rota(
  async ({ token }) => {
    const [conta, historico] = await Promise.all([
      chamarApi<ContaFidelidade>("/fidelidade", { token }),
      chamarApi<Pagina<MovimentoPontos>>("/fidelidade/historico?limit=20", { token }),
    ]);
    return NextResponse.json({ conta: conta.corpo, movimentos: historico.corpo.content });
  },
  { autenticada: true },
);
