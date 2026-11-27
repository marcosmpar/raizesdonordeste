import { NextResponse } from "next/server";
import type { Pagina, Unidade } from "@/lib/tipos";
import { chamarApi } from "@/server/backend";
import { rota } from "@/server/rota";

export const GET = rota(async () => {
  const { corpo } = await chamarApi<Pagina<Unidade>>("/unidades?limit=100");
  return NextResponse.json(corpo.content);
});
