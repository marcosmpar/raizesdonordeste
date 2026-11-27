"use client";

import { useSacola } from "./sacola";
import type { ItemCardapio, Promocao, Unidade } from "./tipos";
import { useDados } from "./useDados";

/** Unidades, cardápio da unidade escolhida e os itens que estão na sacola. */
export function useCardapio() {
  const sacola = useSacola();
  const unidades = useDados<Unidade[]>("/unidades");
  const unidadeId = sacola.unidadeId ?? unidades.dados?.[0]?.id;
  const cardapio = useDados<{ itens: ItemCardapio[]; promocoes: Promocao[] }>(
    unidadeId ? `/unidades/${unidadeId}/cardapio` : null,
  );
  const itens = cardapio.dados?.itens ?? [];
  const selecionados = itens.filter((i) => sacola.itens[i.produtoId] > 0);
  const disponiveis = new Set(itens.map((i) => i.produtoId));

  return {
    sacola,
    unidades: unidades.dados ?? [],
    unidadeId,
    itens,
    promocoes: cardapio.dados?.promocoes ?? [],
    selecionados,
    quantidade: selecionados.reduce((s, i) => s + sacola.itens[i.produtoId], 0),
    subtotal: selecionados.reduce((s, i) => s + i.preco * sacola.itens[i.produtoId], 0),
    /** há itens guardados que o cardápio desta unidade não oferece */
    faltam:
      !!cardapio.dados &&
      Object.entries(sacola.itens).some(([id, q]) => q > 0 && !disponiveis.has(Number(id))),
    carregando: !cardapio.dados && !cardapio.erro && !unidades.erro,
    erro: unidades.erro ?? cardapio.erro,
  };
}
