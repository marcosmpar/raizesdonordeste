"use client";

import Link from "next/link";
import { Sacola } from "@/components/Sacola";
import { Aviso } from "@/components/ui";
import { useCardapio } from "@/lib/useCardapio";

/** Sacola em tela própria (no computador ela aparece ao lado do cardápio). */
export default function PaginaSacola() {
  const cardapio = useCardapio();
  return (
    <div className="flex grow flex-col bg-bagaco">
      <div className="mx-auto flex w-full max-w-lg grow flex-col gap-4 px-5 py-5">
        <Link href="/" className="flex min-h-11 items-center gap-2 self-start rounded-full bg-papel px-4 text-sm font-bold">
          <span aria-hidden>←</span> Cardápio
        </Link>
        <Aviso>{cardapio.erro}</Aviso>
        {cardapio.carregando ? <p>Carregando…</p> : <Sacola cardapio={cardapio} />}
      </div>
    </div>
  );
}
