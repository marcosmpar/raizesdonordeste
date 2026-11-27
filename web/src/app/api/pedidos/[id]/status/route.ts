import { NextResponse } from "next/server";
import { chamarApi } from "@/server/backend";
import { corpoJson, id, rota } from "@/server/rota";

export const PATCH = rota<{ id: string }>(
  async ({ req, params, token }) => {
    const { status } = await corpoJson(req);
    const { corpo } = await chamarApi(`/pedidos/${id(params.id)}/status`, {
      metodo: "PATCH",
      token,
      corpo: { status },
    });
    return NextResponse.json(corpo);
  },
  { autenticada: true },
);
