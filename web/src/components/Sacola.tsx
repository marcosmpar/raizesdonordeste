"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { bff, mensagemDe } from "@/lib/api";
import { reais } from "@/lib/formato";
import { limparSacola, mudarQuantidade, mudarSacola } from "@/lib/sacola";
import type { ContaFidelidade, Pedido } from "@/lib/tipos";
import type { useCardapio } from "@/lib/useCardapio";
import { useDados } from "@/lib/useDados";
import { useSessao } from "./Sessao";
import { Aviso, botao, campo, Interruptor } from "./ui";

export function Sacola({ cardapio }: { cardapio: ReturnType<typeof useCardapio> }) {
  const { sacola, selecionados, subtotal, quantidade, unidadeId, promocoes, faltam } = cardapio;
  const { usuario } = useSessao();
  const router = useRouter();
  const fidelidade = useDados<{ conta: ContaFidelidade }>(
    usuario?.perfil === "CLIENTE" ? "/fidelidade" : null,
  );
  const conta = fidelidade.dados?.conta;
  const [digitado, setDigitado] = useState("");
  const [usandoPontos, setUsandoPontos] = useState(!!sacola.pontos);
  const [erro, setErro] = useState("");
  const [enviando, setEnviando] = useState(false);
  const promocao = promocoes.find((p) => p.codigo === sacola.codigo);
  const comDesconto = !!sacola.codigo || !!Number(sacola.pontos);

  async function finalizar() {
    setErro("");
    setEnviando(true);
    try {
      const { dados } = await bff<Pedido>("/pedidos", {
        metodo: "POST",
        corpo: {
          unidadeId,
          itens: selecionados.map((i) => ({
            produtoId: i.produtoId,
            quantidade: sacola.itens[i.produtoId],
          })),
          codigoPromocao: sacola.codigo,
          pontosResgatar: Number(sacola.pontos) || 0,
        },
      });
      limparSacola();
      router.push(`/pedidos/${dados.pedidoId}`);
    } catch (e) {
      setErro(mensagemDe(e));
      setEnviando(false);
    }
  }

  return (
    <div className="flex grow flex-col gap-4">
      <div className="flex items-baseline justify-between">
        <h2 className="titulo text-3xl">Sua sacola</h2>
        <span className="text-sm text-tinta-suave">
          {quantidade} {quantidade === 1 ? "item" : "itens"}
        </span>
      </div>

      {selecionados.length === 0 ? (
        <p className="rounded-2xl bg-papel p-5 text-sm text-tinta-suave">Adicione itens do cardápio.</p>
      ) : (
        <ul className="rounded-2xl bg-papel px-5">
          {selecionados.map((i) => (
            <li key={i.produtoId} className="flex items-center gap-3 border-b-[1.5px] border-linha py-4 last:border-0">
              <div className="grow">
                <p className="font-bold">{i.nome}</p>
                <p className="text-sm">{reais(i.preco * sacola.itens[i.produtoId])}</p>
              </div>
              <div className="flex shrink-0 items-center rounded-full border-[1.5px] border-linha font-bold">
                <button className="size-10" onClick={() => mudarQuantidade(i.produtoId, -1)} aria-label={`Remover um ${i.nome}`}>
                  −
                </button>
                <span className="w-6 text-center">{sacola.itens[i.produtoId]}</span>
                <button className="size-10" onClick={() => mudarQuantidade(i.produtoId, 1)} aria-label={`Adicionar mais um ${i.nome}`}>
                  +
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
      {faltam && <Aviso>Alguns itens não estão disponíveis nesta unidade e ficaram de fora.</Aviso>}

      {sacola.codigo ? (
        <div className="flex items-center justify-between gap-3 rounded-xl border-2 border-dashed border-tinta bg-cordel px-5 py-3.5">
          <div>
            <p className="font-extrabold tracking-wide">{sacola.codigo}</p>
            <p className="text-[13px]">{promocao?.descricao ?? "Será conferido ao fazer o pedido."}</p>
          </div>
          <button className="min-h-11 text-[13px] font-bold underline" onClick={() => mudarSacola({ codigo: "" })}>
            Remover
          </button>
        </div>
      ) : (
        <form
          className="flex gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            mudarSacola({ codigo: digitado.trim().toUpperCase() });
            setDigitado("");
          }}
        >
          <input
            className={campo}
            aria-label="Código de promoção"
            placeholder="Código de promoção"
            value={digitado}
            onChange={(e) => setDigitado(e.target.value)}
          />
          <button className="min-h-11 shrink-0 px-2 text-sm font-bold underline disabled:opacity-40" disabled={!digitado.trim()}>
            Aplicar
          </button>
        </form>
      )}

      {conta?.consentimento && conta.saldoPontos > 0 && (
        <div className="flex flex-col gap-3 rounded-2xl bg-papel p-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="font-bold">Usar meus pontos</p>
              <p className="text-[13px] text-tinta-suave">
                {conta.saldoPontos} pontos = {reais(conta.saldoPontos * conta.regras.valorPontoEmReais)}
              </p>
            </div>
            <Interruptor
              rotulo="Usar meus pontos"
              ligado={usandoPontos}
              aoAlternar={() => {
                setUsandoPontos(!usandoPontos);
                mudarSacola({ pontos: "" });
              }}
            />
          </div>
          {usandoPontos && (
            <input
              className={campo}
              type="number"
              inputMode="numeric"
              min={0}
              max={conta.saldoPontos}
              aria-label="Pontos a resgatar"
              placeholder={`Quantos pontos? (até ${conta.saldoPontos})`}
              value={sacola.pontos}
              onChange={(e) => mudarSacola({ pontos: e.target.value })}
            />
          )}
        </div>
      )}

      <div className="mt-auto flex flex-col gap-3 pt-2">
        <div className="flex items-baseline justify-between border-t-2 border-tinta pt-3">
          <span className="font-bold">{comDesconto ? "Antes dos descontos" : "Total"}</span>
          <span className="titulo text-3xl">{reais(subtotal)}</span>
        </div>
        {comDesconto && (
          <p className="text-[13px] text-tinta-suave">O total com descontos aparece depois de fazer o pedido.</p>
        )}
        <Aviso>{erro}</Aviso>
        {usuario === null ? (
          <button className={`${botao} min-h-14 text-base`} onClick={() => router.push("/entrar")}>
            Entrar para pedir
          </button>
        ) : (
          <button
            className={`${botao} min-h-14 text-base`}
            disabled={!usuario || usuario.perfil === "COZINHA" || selecionados.length === 0 || enviando}
            onClick={finalizar}
          >
            {enviando ? "Enviando…" : "Fazer pedido"}
          </button>
        )}
      </div>
    </div>
  );
}
