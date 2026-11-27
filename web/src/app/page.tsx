"use client";

import Link from "next/link";
import { useState } from "react";
import { ImagemPrato, Sol } from "@/components/ilustracoes";
import { Sacola } from "@/components/Sacola";
import { Aviso, pilula } from "@/components/ui";
import { reais } from "@/lib/formato";
import { mudarQuantidade, mudarSacola } from "@/lib/sacola";
import { useCardapio } from "@/lib/useCardapio";

export default function PaginaCardapio() {
  const cardapio = useCardapio();
  const { sacola, unidades, unidadeId, itens, promocoes, quantidade, subtotal, carregando, erro } = cardapio;
  const [escolhida, setEscolhida] = useState("");
  const categorias = [...new Set(itens.map((i) => i.categoria))];
  // a categoria escolhida pode não existir na unidade atual
  const categoria = categorias.includes(escolhida) ? escolhida : "";
  const visiveis = categoria ? itens.filter((i) => i.categoria === categoria) : itens;

  return (
    <div className="flex grow flex-col md:grid md:grid-cols-[1fr_340px] lg:grid-cols-[1fr_400px]">
      <section className="flex grow flex-col gap-5 px-5 pb-6 md:px-12 md:py-8">
        <div className="relative flex flex-col justify-end gap-3.5 overflow-hidden rounded-2xl bg-cordel p-5 md:min-h-52 md:p-8">
          <Sol className="absolute -right-8 -top-9 size-32 md:-right-6 md:-top-10 md:size-56" />
          <h1 className="titulo relative max-w-[230px] text-3xl leading-tight md:max-w-lg md:text-[56px] md:leading-none">
            O que vai ser hoje?
          </h1>
          <label className="relative self-start">
            <span className="sr-only">Unidade</span>
            <select
              className="max-w-full rounded-full bg-papel px-4 py-2.5 text-sm font-semibold"
              value={unidadeId ?? ""}
              onChange={(e) =>
                // preços e disponibilidade são por unidade
                mudarSacola({ unidadeId: Number(e.target.value), itens: {} })
              }
            >
              {unidades.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.nome.replace(/^Raízes - /, "")} · {u.cidade}/{u.uf}
                </option>
              ))}
            </select>
          </label>
        </div>

        <Aviso>{erro}</Aviso>
        {carregando && <p>Carregando…</p>}

        {categorias.length > 1 && (
          <div role="group" aria-label="Categorias" className="-mx-5 flex gap-2 overflow-x-auto px-5 md:mx-0 md:px-0">
            {["", ...categorias].map((c) => (
              <button key={c} className={pilula} aria-pressed={c === categoria} onClick={() => setEscolhida(c)}>
                {c || "Tudo"}
              </button>
            ))}
          </div>
        )}

        {promocoes.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {promocoes.map((p) => (
              <button
                key={p.id}
                onClick={() => mudarSacola({ codigo: p.codigo })}
                aria-pressed={sacola.codigo === p.codigo}
                className="rounded-xl border-2 border-dashed border-tinta px-4 py-2.5 text-left text-[13px] aria-pressed:bg-cordel"
              >
                <strong className="tracking-wide">{p.codigo}</strong> · {p.descricao}
              </button>
            ))}
          </div>
        )}

        <ul className="grid gap-x-10 xl:grid-cols-2">
          {visiveis.map((item) => {
            const q = sacola.itens[item.produtoId] ?? 0;
            return (
              <li key={item.produtoId} className="flex items-center gap-4 border-b-[1.5px] border-linha py-4 md:py-5">
                <ImagemPrato nome={item.nome} categoria={item.categoria} />
                <div className="flex grow flex-col gap-1">
                  <h2 className="titulo text-lg leading-6 md:text-xl">{item.nome}</h2>
                  <p className="text-[13px] leading-5 text-tinta-suave md:text-sm">{item.descricao}</p>
                  <p className="pt-1 font-bold">{reais(item.preco)}</p>
                </div>
                <button
                  className="relative flex size-11 shrink-0 items-center justify-center rounded-full bg-tinta text-xl text-white"
                  onClick={() => mudarQuantidade(item.produtoId, 1)}
                  aria-label={`Adicionar ${item.nome}${q ? `, ${q} na sacola` : ""}`}
                >
                  +
                  {q > 0 && (
                    <span aria-hidden className="absolute -right-1 -top-1 flex size-5 items-center justify-center rounded-full bg-cordel text-xs font-extrabold text-tinta">
                      {q}
                    </span>
                  )}
                </button>
              </li>
            );
          })}
        </ul>
      </section>

      {quantidade > 0 && (
        <div className="sticky bottom-[78px] z-10 px-4 pb-2 md:hidden">
          <Link href="/sacola" className="flex items-center gap-3 rounded-full bg-tinta py-2 pl-2 pr-6 font-bold text-white">
            <span className="flex size-10 items-center justify-center rounded-full bg-cordel font-extrabold text-tinta">
              {quantidade}
            </span>
            <span className="grow">Ver sacola</span>
            <span>{reais(subtotal)}</span>
          </Link>
        </div>
      )}

      <aside aria-label="Sacola" className="hidden bg-bagaco md:block">
        <div className="sticky top-0 flex max-h-dvh min-h-full flex-col overflow-y-auto p-8">
          <Sacola cardapio={cardapio} />
        </div>
      </aside>
    </div>
  );
}
