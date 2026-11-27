import type { StatusPedido } from "./tipos";

const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

export const reais = (valor: number) => moeda.format(valor);
export const quando = (iso: string) => dataHora.format(new Date(iso));

/** "2× Carne de sol, 1× Suco de cajá" */
export const resumoItens = (itens: { quantidade: number; nome: string }[]) =>
  itens.map((i) => `${i.quantidade}× ${i.nome}`).join(", ");

export const NOME_STATUS: Record<StatusPedido, string> = {
  AGUARDANDO_PAGAMENTO: "Aguardando pagamento",
  EM_PREPARO: "Em preparo",
  PRONTO: "Pronto",
  ENTREGUE: "Entregue",
  CANCELADO: "Cancelado",
};
