import { NextResponse } from "next/server";
import { rota } from "@/server/rota";
import { encerrarSessao } from "@/server/sessao";

export const POST = rota(async () => {
  const res = new NextResponse(null, { status: 204 });
  encerrarSessao(res);
  return res;
});
