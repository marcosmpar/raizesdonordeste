"use client";

import { useState } from "react";
import { useSessao } from "@/components/Sessao";
import { Aviso, Interruptor, PrecisaEntrar, Situacao } from "@/components/ui";
import { bff, mensagemDe } from "@/lib/api";
import { quando, reais } from "@/lib/formato";
import type { ContaFidelidade, MovimentoPontos } from "@/lib/tipos";
import { useDados } from "@/lib/useDados";

export default function PaginaFidelidade() {
  const { usuario } = useSessao();
  const cliente = usuario?.perfil === "CLIENTE";
  const { dados, erro, recarregar } = useDados<{
    conta: ContaFidelidade;
    movimentos: MovimentoPontos[];
  }>(cliente ? "/fidelidade" : null);
  const [falha, setFalha] = useState("");
  const [enviando, setEnviando] = useState(false);

  if (usuario === null) return <PrecisaEntrar />;
  if (usuario && !cliente) return <Situacao erro="O programa de fidelidade é só para clientes." />;
  if (erro || !dados) return <Situacao erro={erro} />;

  const { conta, movimentos } = dados;

  async function alternar() {
    setFalha("");
    setEnviando(true);
    try {
      await bff("/fidelidade/consentimento", { metodo: conta.consentimento ? "DELETE" : "POST" });
    } catch (e) {
      setFalha(mensagemDe(e));
    }
    setEnviando(false);
    recarregar();
  }

  return (
    <div className="mx-auto grid w-full max-w-5xl content-start gap-8 px-5 py-4 md:grid-cols-[minmax(0,30rem)_1fr] md:gap-16 md:py-12">
      <section className="flex flex-col gap-5">
        <h1 className="titulo text-4xl md:sr-only">Seus pontos</h1>
        <div className="flex flex-col gap-6 rounded-2xl border-[3px] border-dashed border-tinta bg-cordel p-6 md:p-10">
          <p className="font-bold">Seu saldo</p>
          <div>
            <p className="titulo text-8xl leading-none md:text-[128px]">{conta.saldoPontos}</p>
            <p className="text-lg font-semibold">
              pontos · valem {reais(conta.saldoPontos * conta.regras.valorPontoEmReais)}
            </p>
          </div>
          <div className="flex flex-wrap justify-between gap-2 border-t-2 border-tinta pt-4 text-sm">
            <span>R$ 1,00 pago = {conta.regras.pontosPorReal} ponto</span>
            <span>1 ponto = {reais(conta.regras.valorPontoEmReais)}</span>
          </div>
        </div>
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between gap-3">
            <p className="font-bold">Participar do programa</p>
            <Interruptor rotulo="Participar do programa" ligado={conta.consentimento} aoAlternar={alternar} disabled={enviando} />
          </div>
          <p className="text-sm leading-relaxed text-tinta-suave">
            {conta.consentimento
              ? "Você autorizou o uso dos seus dados de compra para acumular e resgatar pontos. Pode revogar quando quiser; o saldo fica guardado."
              : "Ao ativar, você autoriza o uso dos seus dados de compra para acumular e resgatar pontos."}
          </p>
          <Aviso>{falha}</Aviso>
        </div>
      </section>
      <section>
        <h2 className="titulo pb-2 text-3xl">Histórico</h2>
        {movimentos.length === 0 && <p className="text-sm text-tinta-suave">Nenhuma movimentação ainda.</p>}
        <ul>
          {movimentos.map((m) => (
            <li key={m.id} className="flex items-center justify-between gap-3 border-b-[1.5px] border-linha py-5">
              <div>
                <p className="font-bold">{m.descricao}</p>
                <p className="text-sm text-tinta-suave">{quando(m.createdAt)}</p>
              </div>
              <strong className={`text-xl ${m.pontos < 0 ? "text-pimenta" : "text-mandacaru"}`}>
                {m.pontos < 0 ? "−" : "+"}
                {Math.abs(m.pontos)}
              </strong>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
