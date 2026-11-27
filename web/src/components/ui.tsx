import Link from "next/link";
import type { ReactNode } from "react";
import { NOME_STATUS } from "@/lib/formato";
import type { StatusPedido } from "@/lib/tipos";

export const campo =
  "w-full rounded-xl border-[1.5px] border-linha bg-papel px-4 py-3 text-base focus:border-tinta";

const base =
  "inline-flex min-h-11 items-center justify-center rounded-full px-6 py-2.5 text-sm font-bold disabled:cursor-not-allowed disabled:opacity-50";
export const botao = `${base} bg-tinta text-white hover:opacity-90`;
export const botaoSecundario = `${base} border-[1.5px] border-tinta hover:bg-bagaco`;
/** Pílula de filtro; marque a ativa com aria-pressed. */
export const pilula =
  "min-h-11 shrink-0 rounded-full border-[1.5px] border-linha px-5 py-2 text-sm font-semibold aria-pressed:border-tinta aria-pressed:bg-tinta aria-pressed:text-white";

export function Aviso({ tipo = "erro", children }: { tipo?: "erro" | "ok"; children: ReactNode }) {
  if (!children) return null;
  const cor = tipo === "erro" ? "border-pimenta text-pimenta" : "border-mandacaru text-mandacaru";
  return (
    <p
      role={tipo === "erro" ? "alert" : "status"}
      className={`rounded-md border-l-[6px] bg-bagaco px-4 py-3 text-sm font-semibold ${cor}`}
    >
      {children}
    </p>
  );
}

const CORES: Record<StatusPedido, string> = {
  AGUARDANDO_PAGAMENTO: "bg-cordel text-tinta",
  EM_PREPARO: "bg-anil text-white",
  PRONTO: "bg-mandacaru text-white",
  ENTREGUE: "bg-bagaco text-tinta",
  CANCELADO: "border-[1.5px] border-pimenta text-pimenta",
};

export function SeloStatus({ status }: { status: StatusPedido }) {
  return (
    <span
      className={`inline-block whitespace-nowrap rounded-full px-3.5 py-1.5 text-xs font-bold ${CORES[status]}`}
    >
      {NOME_STATUS[status]}
    </span>
  );
}

export function Interruptor({
  ligado,
  rotulo,
  aoAlternar,
  disabled,
}: {
  ligado: boolean;
  rotulo: string;
  aoAlternar: () => void;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={ligado}
      aria-label={rotulo}
      disabled={disabled}
      onClick={aoAlternar}
      className="flex h-8 w-14 shrink-0 rounded-full bg-linha p-1 aria-checked:justify-end aria-checked:bg-mandacaru disabled:opacity-50"
    >
      <span className="size-6 rounded-full bg-white" />
    </button>
  );
}

export function PrecisaEntrar() {
  return (
    <div className="mx-auto flex w-full max-w-md flex-col items-start gap-4 px-5 py-16">
      <h1 className="titulo text-3xl">Entre para continuar</h1>
      <p className="text-tinta-suave">Esta página é da sua conta.</p>
      <Link href="/entrar" className={botao}>
        Entrar
      </Link>
    </div>
  );
}

/** Estado simples de carregamento ou erro de uma página inteira. */
export function Situacao({ erro }: { erro?: string }) {
  return (
    <div className="mx-auto w-full max-w-md px-5 py-16">
      {erro ? <Aviso>{erro}</Aviso> : <p>Carregando…</p>}
    </div>
  );
}
