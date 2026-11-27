import { NextResponse } from "next/server";
import type { ItemCardapio, Pagina, Promocao } from "@/lib/tipos";
import { chamarApi } from "@/server/backend";
import { id, rota } from "@/server/rota";

/** Tela de cardápio em uma chamada: itens da unidade + promoções que valem nela. */
export const GET = rota<{ id: string }>(async ({ params }) => {
  const unidadeId = id(params.id);
  const [cardapio, promocoes] = await Promise.all([
    chamarApi<Pagina<ItemCardapio>>(`/unidades/${unidadeId}/cardapio?limit=100`),
    chamarApi<Pagina<Promocao>>("/promocoes?limit=100").catch(() => null),
  ]);
  const vigentes = promocoes?.corpo.content ?? [];
  return NextResponse.json({
    itens: cardapio.corpo.content,
    promocoes: vigentes.filter((p) => p.unidadeId === null || String(p.unidadeId) === unidadeId),
  });
});
