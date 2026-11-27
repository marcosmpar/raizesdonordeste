import { NextResponse } from "next/server";
import { chamarApi } from "@/server/backend";
import { rota } from "@/server/rota";

const alterar = (metodo: "POST" | "DELETE") =>
  rota(
    async ({ token }) => {
      const { corpo } = await chamarApi("/fidelidade/consentimento", { metodo, token });
      return NextResponse.json(corpo);
    },
    { autenticada: true },
  );

export const POST = alterar("POST");
export const DELETE = alterar("DELETE");
