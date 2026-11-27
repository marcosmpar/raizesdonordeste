import { NextResponse } from "next/server";
import { chamarApi } from "@/server/backend";
import { corpoJson, rota } from "@/server/rota";

export const POST = rota(async ({ req }) => {
  const { nome, email, senha } = await corpoJson(req);
  const { status } = await chamarApi("/usuarios", {
    metodo: "POST",
    corpo: { nome, email, senha },
  });
  return new NextResponse(null, { status });
});
