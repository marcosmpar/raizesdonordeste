import { NextResponse } from "next/server";
import { chamarApi } from "@/server/backend";
import { corpoJson, rota } from "@/server/rota";

/** Repassa a chave de idempotência e preserva o status (201, 200 repetido, 202 pendente). */
export const POST = rota(
  async ({ req, token }) => {
    const { pedidoId, simulacao } = await corpoJson(req);
    const chave = req.headers.get("idempotency-key");
    const { status, corpo } = await chamarApi("/pagamentos", {
      metodo: "POST",
      token,
      corpo: { pedidoId, simulacao },
      cabecalhos: chave ? { "Idempotency-Key": chave } : undefined,
    });
    return NextResponse.json(corpo, { status });
  },
  { autenticada: true },
);
