"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useSessao } from "@/components/Sessao";
import { Aviso, botao, botaoSecundario, campo, pilula, PrecisaEntrar, SeloStatus } from "@/components/ui";
import { mensagemDe } from "@/lib/api";
import { NOME_STATUS, quando, reais, resumoItens } from "@/lib/formato";
import { pedirDeNovo } from "@/lib/pedirDeNovo";
import type { Pagina, PedidoNaLista } from "@/lib/tipos";
import { useDados } from "@/lib/useDados";

export default function PaginaPedidos() {
  const { usuario } = useSessao();
  const router = useRouter();
  const [status, setStatus] = useState("");
  const [canal, setCanal] = useState("");
  const [pagina, setPagina] = useState(1);
  const [falha, setFalha] = useState("");
  const consulta = new URLSearchParams({ page: String(pagina), limit: "10" });
  if (status) consulta.set("status", status);
  if (canal) consulta.set("canalPedido", canal);
  const { dados, erro } = useDados<Pagina<PedidoNaLista>>(usuario ? `/pedidos?${consulta}` : null);

  if (usuario === null) return <PrecisaEntrar />;

  const cliente = usuario?.perfil === "CLIENTE";
  const podePedir = usuario && usuario.perfil !== "COZINHA";

  async function repetir(pedidoId: number) {
    setFalha("");
    try {
      await pedirDeNovo(pedidoId);
      router.push("/sacola");
    } catch (e) {
      setFalha(mensagemDe(e));
    }
  }

  return (
    <section className="mx-auto flex w-full max-w-5xl flex-col gap-5 px-5 py-4 md:py-12">
      <h1 className="titulo text-4xl md:text-5xl">{cliente ? "Meus pedidos" : "Pedidos"}</h1>
      <div role="group" aria-label="Filtrar por status" className="-mx-5 flex gap-2 overflow-x-auto px-5 md:mx-0 md:flex-wrap md:px-0">
        {[["", "Todos"], ...Object.entries(NOME_STATUS)].map(([valor, nome]) => (
          <button
            key={valor}
            className={pilula}
            aria-pressed={status === valor}
            onClick={() => {
              setStatus(valor);
              setPagina(1);
            }}
          >
            {nome}
          </button>
        ))}
      </div>
      {usuario && !cliente && (
        <label className="flex max-w-56 flex-col gap-1 text-sm font-bold">
          Canal
          <select
            className={campo}
            value={canal}
            onChange={(e) => {
              setCanal(e.target.value);
              setPagina(1);
            }}
          >
            <option value="">Todos</option>
            {["APP", "TOTEM", "BALCAO", "PICKUP", "WEB"].map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </label>
      )}
      <Aviso>{falha || erro}</Aviso>
      {!dados && !erro && <p>Carregando…</p>}
      {dados?.content.length === 0 && <p className="text-tinta-suave">Nenhum pedido encontrado.</p>}

      <ul>
        {dados?.content.map((p) => {
          const pagar = p.status === "AGUARDANDO_PAGAMENTO" && podePedir;
          const repetivel = (p.status === "ENTREGUE" || p.status === "CANCELADO") && podePedir;
          return (
            <li key={p.pedidoId} className="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-2 border-b-[1.5px] border-linha py-5 md:grid-cols-[6.5rem_1fr_13rem_7rem_10rem] md:gap-x-6">
              <Link href={`/pedidos/${p.pedidoId}`} className="titulo text-2xl underline-offset-4 hover:underline md:text-3xl">
                #{p.pedidoId}
              </Link>
              <div className="order-3 col-span-2 md:order-none md:col-span-1">
                <p className="font-bold">{resumoItens(p.itens) || "Ver detalhes"}</p>
                <p className="text-sm text-tinta-suave">
                  {[p.unidadeNome?.replace(/^Raízes - /, ""), p.canalPedido, quando(p.createdAt)].filter(Boolean).join(" · ")}
                </p>
              </div>
              <div className="order-4 md:order-none">
                <SeloStatus status={p.status} />
              </div>
              <strong className="order-2 text-right text-lg md:order-none">{reais(p.total)}</strong>
              <div className="order-5 flex justify-end md:order-none">
                {repetivel ? (
                  <button className={`${botaoSecundario} md:w-full`} onClick={() => repetir(p.pedidoId)}>
                    Pedir de novo
                  </button>
                ) : (
                  <Link href={`/pedidos/${p.pedidoId}`} className={`${pagar ? botao : botaoSecundario} md:w-full`}>
                    {pagar ? "Pagar" : "Acompanhar"}
                  </Link>
                )}
              </div>
            </li>
          );
        })}
      </ul>

      {dados && dados.totalPages > 1 && (
        <div className="flex items-center justify-between gap-3 text-sm md:justify-start">
          <button className={botaoSecundario} disabled={pagina <= 1} onClick={() => setPagina(pagina - 1)}>
            Anterior
          </button>
          <span>
            Página {dados.page} de {dados.totalPages}
          </span>
          <button className={botaoSecundario} disabled={pagina >= dados.totalPages} onClick={() => setPagina(pagina + 1)}>
            Próxima
          </button>
        </div>
      )}
    </section>
  );
}
