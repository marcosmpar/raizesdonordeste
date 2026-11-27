"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useSair } from "@/components/Navegacao";
import { useSessao } from "@/components/Sessao";
import { Aviso, PrecisaEntrar, Situacao } from "@/components/ui";
import { bff, mensagemDe } from "@/lib/api";
import type { Pedido, Unidade } from "@/lib/tipos";
import { useDados } from "@/lib/useDados";

type Fila = { total: number; pedidos: Pedido[] };

/** Minutos desde a criação a partir dos quais a comanda é destacada. */
const MINUTOS_ATENCAO = 15;

export default function PaginaPainel() {
  const { usuario } = useSessao();
  const sair = useSair();
  const equipe = !!usuario && usuario.perfil !== "CLIENTE";
  const [unidadeId, setUnidadeId] = useState("");
  const unidades = useDados<Unidade[]>(equipe ? "/unidades" : null);
  const { dados, erro, recarregar } = useDados<{ emPreparo: Fila; prontos: Fila }>(
    equipe ? `/painel${unidadeId && `?unidadeId=${unidadeId}`}` : null,
  );
  const [falha, setFalha] = useState("");
  const [aba, setAba] = useState<"preparo" | "prontos">("preparo");
  const [agora, setAgora] = useState(() => Date.now());

  useEffect(() => {
    if (!equipe) return;
    const relogio = setInterval(() => {
      setAgora(Date.now());
      recarregar();
    }, 10000);
    return () => clearInterval(relogio);
  }, [equipe, recarregar]);

  if (usuario === null) return <PrecisaEntrar />;
  if (usuario && !equipe) return <Situacao erro="Painel exclusivo da equipe." />;

  const cozinha = usuario?.perfil === "COZINHA";
  const nomeUnidade = (id: number) =>
    unidades.dados?.find((u) => u.id === id)?.nome.replace(/^Raízes - /, "") ?? `Unidade ${id}`;
  const minutos = (iso: string) => Math.max(0, Math.floor((agora - Date.parse(iso)) / 60000));
  const tempo = (iso: string) => {
    const m = minutos(iso);
    return m < 60 ? `${m} min` : m < 1440 ? `${Math.floor(m / 60)} h` : `${Math.floor(m / 1440)} d`;
  };

  async function avancar(pedidoId: number, status: "PRONTO" | "ENTREGUE") {
    setFalha("");
    try {
      await bff(`/pedidos/${pedidoId}/status`, { metodo: "PATCH", corpo: { status } });
    } catch (e) {
      setFalha(mensagemDe(e));
    }
    recarregar();
  }

  const abas = [
    { id: "preparo" as const, nome: "Em preparo", total: dados?.emPreparo.total },
    { id: "prontos" as const, nome: "Prontos", total: dados?.prontos.total },
  ];

  return (
    <div className="flex grow flex-col gap-6 bg-tinta px-5 py-6 text-white md:px-12 md:py-10">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-bold text-cordel">{cozinha ? "Cozinha" : "Equipe"}</p>
          <h1 className="titulo text-3xl md:text-5xl">Painel de preparo</h1>
        </div>
        <div className="flex flex-wrap items-center gap-3 text-sm font-semibold">
          <label>
            <span className="sr-only">Unidade</span>
            <select className="min-h-11 rounded-full bg-papel px-4 text-tinta" value={unidadeId} onChange={(e) => setUnidadeId(e.target.value)}>
              <option value="">Todas as unidades</option>
              {unidades.dados?.map((u) => (
                <option key={u.id} value={u.id}>
                  {nomeUnidade(u.id)}
                </option>
              ))}
            </select>
          </label>
          {!cozinha && (
            <>
              <Link href="/" className="underline">
                Cardápio
              </Link>
              <Link href="/pedidos" className="underline">
                Pedidos
              </Link>
            </>
          )}
          <button onClick={sair} className="min-h-11 rounded-full border-[1.5px] border-tinta-suave px-5">
            Sair
          </button>
        </div>
      </header>

      <p className="text-sm text-linha">Atualiza sozinho a cada 10 segundos.</p>
      <div className="text-tinta">
        <Aviso>{falha || erro}</Aviso>
      </div>
      {!dados && !erro && <p>Carregando…</p>}

      <div role="group" aria-label="Fila exibida" className="flex gap-1 rounded-full bg-white/10 p-1 text-sm font-bold md:hidden">
        {abas.map((a) => (
          <button key={a.id} aria-pressed={aba === a.id} onClick={() => setAba(a.id)} className="min-h-11 flex-1 rounded-full aria-pressed:bg-papel aria-pressed:text-tinta">
            {a.nome}
            {a.total !== undefined && ` · ${a.total}`}
          </button>
        ))}
      </div>

      {dados && (
        <div className="grid items-start gap-8 md:grid-cols-[2fr_1fr]">
          <section aria-label="Em preparo" className={`flex-col gap-4 md:flex ${aba === "preparo" ? "flex" : "hidden"}`}>
            <h2 className="hidden items-center gap-3 text-xl font-bold md:flex">
              <span className="size-3.5 rounded-full bg-[#7c9bff]" />
              Em preparo · {dados.emPreparo.total}
            </h2>
            {dados.emPreparo.pedidos.length === 0 && <p className="text-linha">Nada na fila.</p>}
            <ul className="grid items-start gap-4 lg:grid-cols-2 2xl:grid-cols-3">
              {dados.emPreparo.pedidos.map((p) => (
                <li key={p.pedidoId} className="overflow-hidden rounded-xl bg-papel text-tinta">
                  <div className={`flex items-center justify-between px-5 py-3.5 ${minutos(p.createdAt) >= MINUTOS_ATENCAO ? "bg-cordel" : "bg-bagaco"}`}>
                    <Link href={`/pedidos/${p.pedidoId}`} className="titulo text-3xl">
                      #{p.pedidoId}
                    </Link>
                    <span className="text-lg font-extrabold" title="Tempo desde o pedido">
                      {tempo(p.createdAt)}
                    </span>
                  </div>
                  <div className="flex flex-col gap-3.5 p-5">
                    <p className="text-[13px] font-bold text-tinta-suave">
                      {p.canalPedido} · {nomeUnidade(p.unidadeId)}
                    </p>
                    <ul className="flex flex-col gap-2 text-lg font-semibold">
                      {p.itens.map((i) => (
                        <li key={i.produtoId} className="flex gap-3">
                          <span className="w-8 shrink-0 font-extrabold">{i.quantidade}×</span>
                          {i.nome}
                        </li>
                      ))}
                    </ul>
                    <button className="min-h-13 rounded-full bg-tinta font-bold text-white" onClick={() => avancar(p.pedidoId, "PRONTO")}>
                      Marcar pronto
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </section>

          <section aria-label="Prontos" className={`flex-col gap-4 md:flex ${aba === "prontos" ? "flex" : "hidden"}`}>
            <h2 className="hidden items-center gap-3 text-xl font-bold md:flex">
              <span className="size-3.5 rounded-full bg-[#4fc27f]" />
              Prontos · {dados.prontos.total}
            </h2>
            {dados.prontos.pedidos.length === 0 && <p className="text-linha">Nenhum pedido pronto.</p>}
            <ul className="flex flex-col gap-4">
              {dados.prontos.pedidos.map((p) => {
                const itens = p.itens.reduce((s, i) => s + i.quantidade, 0);
                return (
                  <li key={p.pedidoId} className="flex items-center justify-between gap-3 rounded-xl bg-mandacaru px-6 py-5">
                    <div>
                      <Link href={`/pedidos/${p.pedidoId}`} className="titulo text-3xl">
                        #{p.pedidoId}
                      </Link>
                      <p className="text-sm">
                        {p.canalPedido} · {itens} {itens === 1 ? "item" : "itens"} · {nomeUnidade(p.unidadeId)}
                      </p>
                    </div>
                    {/* a cozinha não registra a entrega */}
                    {!cozinha && (
                      <button className="min-h-11 shrink-0 rounded-full bg-papel px-5 text-sm font-bold text-tinta" onClick={() => avancar(p.pedidoId, "ENTREGUE")}>
                        Entregue
                      </button>
                    )}
                  </li>
                );
              })}
            </ul>
          </section>
        </div>
      )}
    </div>
  );
}
