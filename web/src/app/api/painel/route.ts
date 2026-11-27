import { NextResponse } from "next/server";
import type { Pagina, Pedido, PedidoResumo } from "@/lib/tipos";
import { chamarApi, respostaErro } from "@/server/backend";
import { rota } from "@/server/rota";
import { lerUsuario } from "@/server/sessao";

/** Fila da equipe: pedidos em preparo e prontos, já com os itens. */
export const GET = rota(
  async ({ req, token }) => {
    // a API já restringe o cliente aos próprios pedidos; o painel é só da equipe
    if (lerUsuario(req)?.perfil === "CLIENTE") {
      return respostaErro(403, "ACESSO_NEGADO", "Painel exclusivo da equipe.", "/api/painel");
    }
    const unidadeId = req.nextUrl.searchParams.get("unidadeId");
    const filtroUnidade = unidadeId && /^\d{1,18}$/.test(unidadeId) ? `&unidadeId=${unidadeId}` : "";
    const fila = async (status: string) => {
      const { corpo } = await chamarApi<Pagina<PedidoResumo>>(
        `/pedidos?status=${status}&limit=30${filtroUnidade}`,
        { token },
      );
      const pedidos = await Promise.all(
        corpo.content.map((p) =>
          chamarApi<Pedido>(`/pedidos/${p.pedidoId}`, { token }).then((r) => r.corpo),
        ),
      );
      return { total: corpo.totalElements, pedidos };
    };
    const [emPreparo, prontos] = await Promise.all([fila("EM_PREPARO"), fila("PRONTO")]);
    return NextResponse.json({ emPreparo, prontos });
  },
  { autenticada: true },
);
