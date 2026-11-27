import { NextResponse } from "next/server";
import type { Pagamento, Pedido, Unidade } from "@/lib/tipos";
import { chamarApi, ErroApi } from "@/server/backend";
import { id, rota } from "@/server/rota";

/** Agrega pedido, tentativas de pagamento e unidade para a tela de detalhe. */
export const GET = rota<{ id: string }>(
  async ({ params, token }) => {
    const pedidoId = id(params.id);
    const { corpo: pedido } = await chamarApi<Pedido>(`/pedidos/${pedidoId}`, { token });
    const [pagamentos, unidade] = await Promise.all([
      chamarApi<Pagamento[]>(`/pedidos/${pedidoId}/pagamentos`, { token })
        .then((r) => r.corpo)
        .catch((e) => {
          // a cozinha vê o pedido, mas não os pagamentos
          if (e instanceof ErroApi && e.status === 403) return [];
          throw e;
        }),
      chamarApi<Unidade>(`/unidades/${pedido.unidadeId}`)
        .then((r) => r.corpo)
        .catch(() => null),
    ]);
    return NextResponse.json({ pedido, pagamentos, unidade });
  },
  { autenticada: true },
);
