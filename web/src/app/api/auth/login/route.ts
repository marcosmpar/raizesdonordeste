import { NextResponse } from "next/server";
import type { Usuario } from "@/lib/tipos";
import { chamarApi } from "@/server/backend";
import { corpoJson, rota } from "@/server/rota";
import { abrirSessao } from "@/server/sessao";

type Login = { accessToken: string; expiresIn: number; user: Usuario };

/** Faz login na API e guarda o token em cookie; o navegador recebe só os dados do usuário. */
export const POST = rota(async ({ req }) => {
  const { email, senha } = await corpoJson(req);
  const { corpo } = await chamarApi<Login>("/auth/login", {
    metodo: "POST",
    corpo: { email, senha },
  });
  const res = NextResponse.json({ user: corpo.user });
  abrirSessao(res, corpo.accessToken, corpo.user, corpo.expiresIn);
  return res;
});
