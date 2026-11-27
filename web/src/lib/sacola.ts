"use client";

import { useSyncExternalStore } from "react";

/** Sacola em memória: sobrevive à navegação entre telas e some ao recarregar a página. */
export type Sacola = {
  unidadeId?: number;
  /** produtoId → quantidade */
  itens: Record<number, number>;
  codigo: string;
  pontos: string;
};

const VAZIA: Sacola = { itens: {}, codigo: "", pontos: "" };
let atual = VAZIA;
const ouvintes = new Set<() => void>();

export function mudarSacola(mudanca: Partial<Sacola> | ((s: Sacola) => Partial<Sacola>)) {
  atual = { ...atual, ...(typeof mudanca === "function" ? mudanca(atual) : mudanca) };
  ouvintes.forEach((avisar) => avisar());
}

/** Esvazia a sacola, mantendo a unidade escolhida. */
export const limparSacola = () => mudarSacola({ itens: {}, codigo: "", pontos: "" });

export function mudarQuantidade(produtoId: number, delta: number) {
  mudarSacola((s) => ({
    itens: { ...s.itens, [produtoId]: Math.min(99, Math.max(0, (s.itens[produtoId] ?? 0) + delta)) },
  }));
}

function assinar(avisar: () => void) {
  ouvintes.add(avisar);
  return () => {
    ouvintes.delete(avisar);
  };
}

export const useSacola = () =>
  useSyncExternalStore(
    assinar,
    () => atual,
    () => VAZIA,
  );
