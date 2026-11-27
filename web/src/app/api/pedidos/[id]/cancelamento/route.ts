import { NextResponse } from "next/server";
import { chamarApi } from "@/server/backend";
import { id, rota } from "@/server/rota";

export const POST = rota<{ id: string }>(
  async ({ params, token }) => {
    const { corpo } = await chamarApi(`/pedidos/${id(params.id)}/cancelamento`, {
      metodo: "POST",
      token,
    });
    return NextResponse.json(corpo);
  },
  { autenticada: true },
);
