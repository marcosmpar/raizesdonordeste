"use client";

import { bff } from "./api";
import { mudarSacola } from "./sacola";
import type { PedidoDetalhado } from "./tipos";

/** Remonta a sacola com os itens de um pedido; preços e disponibilidade valem os de hoje. */
export async function pedirDeNovo(pedidoId: number) {
  const { dados } = await bff<PedidoDetalhado>(`/pedidos/${pedidoId}`);
  mudarSacola({
    unidadeId: dados.pedido.unidadeId,
    itens: Object.fromEntries(dados.pedido.itens.map((i) => [i.produtoId, i.quantidade])),
    codigo: "",
    pontos: "",
  });
}
