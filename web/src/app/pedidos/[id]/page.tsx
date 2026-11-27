"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useRef, useState } from "react";
import { useSessao } from "@/components/Sessao";
import { Aviso, botao, botaoSecundario, PrecisaEntrar, SeloStatus, Situacao } from "@/components/ui";
import { bff, mensagemDe } from "@/lib/api";
import { quando, reais } from "@/lib/formato";
import { pedirDeNovo } from "@/lib/pedirDeNovo";
import type { ContaFidelidade, Pagamento, PedidoDetalhado, Simulacao, StatusPedido } from "@/lib/tipos";
import { useDados } from "@/lib/useDados";

const SITUACAO: Record<StatusPedido, { titulo: string; texto: string; cor: string }> = {
  AGUARDANDO_PAGAMENTO: {
    titulo: "Falta só pagar.",
    texto: "Seus itens já estão reservados. Com o pagamento aprovado, a cozinha começa o preparo.",
    cor: "max-md:bg-cordel",
  },
  EM_PREPARO: {
    titulo: "Sua comida está no fogo.",
    texto: "Toque em Atualizar para ver se já ficou pronta.",
    cor: "max-md:bg-anil max-md:text-white",
  },
  PRONTO: { titulo: "Pode vir buscar!", texto: "Seu pedido está pronto.", cor: "max-md:bg-mandacaru max-md:text-white" },
  ENTREGUE: { titulo: "Pedido entregue.", texto: "Bom apetite e até a próxima.", cor: "max-md:bg-bagaco" },
  CANCELADO: { titulo: "Pedido cancelado.", texto: "Os itens voltaram para o estoque.", cor: "max-md:bg-bagaco" },
};

const ETAPAS: { status: StatusPedido; nome: string; barra: string }[] = [
  { status: "AGUARDANDO_PAGAMENTO", nome: "Aguardando pagamento", barra: "bg-cordel" },
  { status: "EM_PREPARO", nome: "Em preparo", barra: "bg-anil" },
  { status: "PRONTO", nome: "Pronto", barra: "bg-mandacaru" },
  { status: "ENTREGUE", nome: "Entregue", barra: "bg-tinta" },
];

const SIMULACOES: { valor: Simulacao; nome: string; efeito: string }[] = [
  { valor: "APROVADO", nome: "Aprovar", efeito: "Segue para o preparo" },
  { valor: "RECUSADO", nome: "Recusar", efeito: "Mantém aguardando" },
  { valor: "INDISPONIVEL", nome: "Indisponível", efeito: "Fica pendente" },
];

export default function PaginaPedido() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { usuario } = useSessao();
  const { dados, erro, recarregar } = useDados<PedidoDetalhado>(usuario ? `/pedidos/${id}` : null);
  const fidelidade = useDados<{ conta: ContaFidelidade }>(
    usuario?.perfil === "CLIENTE" ? "/fidelidade" : null,
  );
  const [simulacao, setSimulacao] = useState<Simulacao>("APROVADO");
  const [aviso, setAviso] = useState<{ tipo: "ok" | "erro"; texto: string }>();
  const [enviando, setEnviando] = useState(false);
  // mantida enquanto o resultado for incerto, para a repetição não cobrar duas vezes
  const chave = useRef<string | null>(null);

  if (usuario === null) return <PrecisaEntrar />;
  if (erro || !dados) return <Situacao erro={erro} />;

  const { pedido, pagamentos, unidade } = dados;
  const aguardando = pedido.status === "AGUARDANDO_PAGAMENTO";
  const equipe = usuario && usuario.perfil !== "CLIENTE" && usuario.perfil !== "COZINHA";
  const podePedir = usuario?.perfil !== "COZINHA";
  const podePagar = aguardando && podePedir;
  const podeCancelar = (aguardando && podePedir) || (equipe && pedido.status === "EM_PREPARO");
  const encerrado = pedido.status === "ENTREGUE" || pedido.status === "CANCELADO";
  const etapaAtual = ETAPAS.findIndex((e) => e.status === pedido.status);
  const situacao = SITUACAO[pedido.status];
  const ultimo = pagamentos.at(-1);
  const conta = fidelidade.dados?.conta;

  async function executar(acao: () => Promise<{ tipo: "ok" | "erro"; texto: string } | undefined>) {
    setAviso(undefined);
    setEnviando(true);
    try {
      setAviso(await acao());
    } catch (e) {
      setAviso({ tipo: "erro", texto: mensagemDe(e) });
    } finally {
      setEnviando(false);
      recarregar();
    }
  }

  const pagar = () =>
    executar(async () => {
      chave.current ??= crypto.randomUUID();
      const { dados: pagamento } = await bff<Pagamento>("/pagamentos", {
        metodo: "POST",
        corpo: { pedidoId: pedido.pedidoId, simulacao },
        cabecalhos: { "Idempotency-Key": chave.current },
      });
      if (pagamento.status !== "PENDENTE") chave.current = null;
      return {
        tipo: pagamento.status === "APROVADO" ? "ok" : "erro",
        texto: pagamento.gateway.mensagem ?? `Pagamento ${pagamento.status.toLowerCase()}.`,
      };
    });

  const cancelar = () => {
    if (!window.confirm("Cancelar este pedido?")) return;
    executar(async () => {
      await bff(`/pedidos/${pedido.pedidoId}/cancelamento`, { metodo: "POST" });
      return { tipo: "ok", texto: "Pedido cancelado." };
    });
  };

  return (
    <div className="flex grow flex-col md:grid md:grid-cols-[1fr_340px] lg:grid-cols-[1fr_440px]">
      <section className="flex flex-col gap-7 px-5 py-4 md:px-12 md:py-12 lg:px-20">
        <div className={`flex flex-col gap-3 rounded-3xl p-6 md:p-0 ${situacao.cor}`}>
          <p className="text-sm md:text-tinta-suave">
            Pedido #{pedido.pedidoId}
            {unidade && ` · ${unidade.nome.replace(/^Raízes - /, "")}`} · {pedido.canalPedido} · {quando(pedido.createdAt)}
          </p>
          <h1 className="titulo text-4xl leading-tight md:text-[64px] md:leading-none">{situacao.titulo}</h1>
          <p className="max-w-xl md:text-lg">{situacao.texto}</p>
          <div className="flex flex-wrap items-center gap-3">
            {/* no celular a cor do bloco já indica o status */}
            <span className="max-md:sr-only">
              <SeloStatus status={pedido.status} />
            </span>
            {!encerrado && (
              <button className="min-h-11 text-sm font-bold underline" onClick={recarregar}>
                Atualizar
              </button>
            )}
          </div>
        </div>

        {etapaAtual >= 0 && (
          <ol aria-label="Etapas do pedido" className="flex flex-col gap-3 md:flex-row md:gap-2">
            {ETAPAS.map((etapa, i) => {
              const feita = i <= etapaAtual;
              return (
                <li key={etapa.status} aria-current={i === etapaAtual ? "step" : undefined} className="flex items-center gap-4 md:flex-1 md:flex-col md:items-stretch md:gap-2.5">
                  <span className={`size-6 shrink-0 rounded-full md:h-2.5 md:w-full ${feita ? ETAPAS[etapaAtual].barra : "border-[3px] border-linha md:border-0 md:bg-linha"}`} />
                  <span className={`text-sm ${i === etapaAtual ? "font-extrabold" : feita ? "font-semibold" : "text-tinta-suave"}`}>
                    {etapa.nome}
                  </span>
                </li>
              );
            })}
          </ol>
        )}

        {aviso ? (
          <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>
        ) : (
          aguardando &&
          ultimo &&
          ultimo.status !== "APROVADO" && (
            <Aviso>
              {quando(ultimo.createdAt)}: {ultimo.gateway.mensagem ?? `pagamento ${ultimo.status.toLowerCase()}`}
            </Aviso>
          )
        )}

        {podePagar && (
          <fieldset className="flex flex-col gap-3">
            <legend className="titulo pb-3 text-xl">Pagamento simulado</legend>
            <div className="grid gap-3 sm:grid-cols-3">
              {SIMULACOES.map((s) => (
                <label key={s.valor} className="flex cursor-pointer flex-col gap-1 rounded-xl border-[1.5px] border-linha p-4 has-checked:border-tinta has-checked:ring-1 has-checked:ring-tinta has-focus-visible:outline-3 has-focus-visible:outline-anil">
                  <input type="radio" name="simulacao" className="sr-only" checked={simulacao === s.valor} onChange={() => setSimulacao(s.valor)} />
                  <span className="font-bold">{s.nome}</span>
                  <span className="text-[13px] text-tinta-suave">{s.efeito}</span>
                </label>
              ))}
            </div>
          </fieldset>
        )}

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:gap-6">
          {podePagar && (
            <button className={`${botao} min-h-14 px-14 text-base`} disabled={enviando} onClick={pagar}>
              {enviando ? "Processando…" : `Pagar ${reais(pedido.total)}`}
            </button>
          )}
          {encerrado && podePedir && (
            <button
              className={botao}
              disabled={enviando}
              onClick={() =>
                executar(async () => {
                  await pedirDeNovo(pedido.pedidoId);
                  router.push("/sacola");
                  return undefined;
                })
              }
            >
              Pedir de novo
            </button>
          )}
          {!podePagar && (
            <Link href="/" className={botaoSecundario}>
              Voltar ao cardápio
            </Link>
          )}
          {podeCancelar && (
            <button className="min-h-11 text-sm font-bold text-pimenta underline" disabled={enviando} onClick={cancelar}>
              Cancelar pedido
            </button>
          )}
        </div>
      </section>

      <aside className="flex grow flex-col gap-5 bg-bagaco px-5 py-8 md:px-10 md:py-12">
        <h2 className="titulo text-3xl">Resumo</h2>
        <ul className="rounded-2xl bg-papel px-5">
          {pedido.itens.map((i) => (
            <li key={i.produtoId} className="flex gap-3 border-b-[1.5px] border-linha py-4 text-[15px] last:border-0">
              <span className="w-8 shrink-0 font-bold">{i.quantidade}×</span>
              <span className="grow font-semibold">{i.nome}</span>
              <span>{reais(i.subtotal)}</span>
            </li>
          ))}
        </ul>
        <dl className="flex flex-col gap-2.5 text-[15px]">
          <Linha nome="Subtotal" valor={reais(pedido.subtotal)} />
          {pedido.descontoPromocao > 0 && (
            <Linha desconto nome={`Promoção ${pedido.codigoPromocao}`} valor={`− ${reais(pedido.descontoPromocao)}`} />
          )}
          {pedido.descontoFidelidade > 0 && (
            <Linha desconto nome={`${pedido.pontosResgatados} pontos`} valor={`− ${reais(pedido.descontoFidelidade)}`} />
          )}
          <div className="flex items-baseline justify-between border-t-2 border-tinta pt-3">
            <dt className="font-bold">Total</dt>
            <dd className="titulo text-3xl">{reais(pedido.total)}</dd>
          </div>
        </dl>
        {aguardando && conta?.consentimento && (
          <p className="text-sm text-tinta-suave">
            Você ganha {Math.floor(pedido.total * conta.regras.pontosPorReal)} pontos quando o pagamento for aprovado.
          </p>
        )}
        {pagamentos.length > 0 && (
          <details className="text-sm">
            <summary className="cursor-pointer font-bold">Tentativas de pagamento ({pagamentos.length})</summary>
            <ul className="flex flex-col gap-2 pt-3">
              {pagamentos.map((p) => (
                <li key={p.pagamentoId}>
                  <strong>{p.status}</strong> · {reais(p.valor)} · {quando(p.createdAt)}
                  <br />
                  <span className="text-tinta-suave">{p.gateway.mensagem}</span>
                </li>
              ))}
            </ul>
          </details>
        )}
      </aside>
    </div>
  );
}

function Linha({ nome, valor, desconto }: { nome: string; valor: string; desconto?: boolean }) {
  return (
    <div className={`flex justify-between gap-3 ${desconto ? "font-semibold text-mandacaru" : ""}`}>
      <dt>{nome}</dt>
      <dd>{valor}</dd>
    </div>
  );
}
