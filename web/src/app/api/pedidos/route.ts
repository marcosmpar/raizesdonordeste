import { NextResponse } from "next/server";
import type { Pagina, Pedido, PedidoNaLista, PedidoResumo, Unidade } from "@/lib/tipos";
import { chamarApi } from "@/server/backend";
import { corpoJson, rota } from "@/server/rota";
import { lerUsuario } from "@/server/sessao";

export const GET = rota(
  async ({ req, token }) => {
    const filtros = new URLSearchParams();
    for (const nome of ["canalPedido", "status", "unidadeId", "page", "limit"]) {
      const valor = req.nextUrl.searchParams.get(nome);
      if (valor) filtros.set(nome, valor);
    }
    // cada linha busca o detalhe do pedido, então a página é limitada
    const limite = Number(filtros.get("limit"));
    if (limite > 20) filtros.set("limit", "20");
    const { corpo } = await chamarApi<Pagina<PedidoResumo>>(`/pedidos?${filtros}`, { token });
    if (corpo.content.length === 0) return NextResponse.json(corpo);

    // complementos são opcionais: se falharem, a lista continua útil
    const unidades = await chamarApi<Pagina<Unidade>>("/unidades?limit=100")
      .then((r) => r.corpo.content)
      .catch(() => []);
    const content: PedidoNaLista[] = await Promise.all(
      corpo.content.map(async (p) => ({
        ...p,
        itens: await chamarApi<Pedido>(`/pedidos/${p.pedidoId}`, { token })
          .then((r) => r.corpo.itens.map(({ nome, quantidade }) => ({ nome, quantidade })))
          .catch(() => []),
        unidadeNome: unidades.find((u) => u.id === p.unidadeId)?.nome ?? null,
      })),
    );
    return NextResponse.json({ ...corpo, content });
  },
  { autenticada: true },
);

/** O canal é decidido aqui: cliente neste site é sempre WEB; a equipe registra no balcão. */
export const POST = rota(
  async ({ req, token }) => {
    const { unidadeId, itens, codigoPromocao, pontosResgatar } = await corpoJson(req);
    const canalPedido = lerUsuario(req)?.perfil === "CLIENTE" ? "WEB" : "BALCAO";
    const { status, corpo } = await chamarApi("/pedidos", {
      metodo: "POST",
      token,
      corpo: {
        canalPedido,
        unidadeId,
        itens,
        formaPagamento: "MOCK",
        codigoPromocao: codigoPromocao || undefined,
        pontosResgatar: pontosResgatar || undefined,
      },
    });
    return NextResponse.json(corpo, { status });
  },
  { autenticada: true },
);
