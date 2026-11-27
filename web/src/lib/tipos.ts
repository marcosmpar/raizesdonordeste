/** Contratos trocados entre as telas e o BFF (espelham os DTOs da API Java). */
export type Perfil = "CLIENTE" | "ATENDENTE" | "COZINHA" | "GERENTE" | "ADMIN";
export type CanalPedido = "APP" | "TOTEM" | "BALCAO" | "PICKUP" | "WEB";
export type StatusPedido =
  | "AGUARDANDO_PAGAMENTO"
  | "EM_PREPARO"
  | "PRONTO"
  | "ENTREGUE"
  | "CANCELADO";
export type StatusPagamento = "PENDENTE" | "APROVADO" | "RECUSADO" | "ESTORNADO";
export type Simulacao = "APROVADO" | "RECUSADO" | "INDISPONIVEL";

export type Usuario = { id: number; nome: string; perfil: Perfil };

export type Pagina<T> = {
  content: T[];
  page: number;
  limit: number;
  totalElements: number;
  totalPages: number;
};

export type Unidade = { id: number; nome: string; cidade: string; uf: string; endereco: string };

export type ItemCardapio = {
  produtoId: number;
  nome: string;
  descricao: string;
  categoria: string;
  preco: number;
  disponivel: boolean;
};

export type Promocao = { id: number; codigo: string; descricao: string; unidadeId: number | null };

export type PedidoResumo = {
  pedidoId: number;
  status: StatusPedido;
  canalPedido: CanalPedido;
  unidadeId: number;
  clienteId: number | null;
  total: number;
  createdAt: string;
};

/** Linha da lista de pedidos montada pelo BFF. */
export type PedidoNaLista = PedidoResumo & {
  itens: { nome: string; quantidade: number }[];
  unidadeNome: string | null;
};

export type Pedido = PedidoResumo & {
  subtotal: number;
  descontoPromocao: number;
  descontoFidelidade: number;
  codigoPromocao: string | null;
  pontosResgatados: number;
  itens: {
    produtoId: number;
    nome: string;
    quantidade: number;
    precoUnitario: number;
    subtotal: number;
  }[];
};

export type Pagamento = {
  pagamentoId: number;
  pedidoId: number;
  status: StatusPagamento;
  statusPedido: StatusPedido;
  valor: number;
  gateway: { transacaoId: string | null; codigo: string | null; mensagem: string | null };
  createdAt: string;
};

export type PedidoDetalhado = {
  pedido: Pedido;
  pagamentos: Pagamento[];
  unidade: Unidade | null;
};

export type ContaFidelidade = {
  consentimento: boolean;
  saldoPontos: number;
  regras: { pontosPorReal: number; valorPontoEmReais: number };
};

export type MovimentoPontos = {
  id: number;
  tipo: string;
  pontos: number;
  pedidoId: number | null;
  descricao: string;
  createdAt: string;
};

export type Erro = {
  error: string;
  message: string;
  details?: { field: string; issue: string }[];
};
