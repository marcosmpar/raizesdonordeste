import type { NextRequest, NextResponse } from "next/server";
import type { Usuario } from "@/lib/tipos";

/**
 * Sessão do BFF: o JWT fica em cookie httpOnly, inacessível ao JavaScript da página.
 * O cookie de usuário serve só para a interface; quem autoriza é sempre a API Java.
 */
export const COOKIE_TOKEN = "rn_token";
export const COOKIE_USUARIO = "rn_usuario";

function opcoes(maxAge: number) {
  const seguro = process.env.COOKIE_SECURE
    ? process.env.COOKIE_SECURE !== "false"
    : process.env.NODE_ENV === "production";
  return { httpOnly: true, sameSite: "lax" as const, secure: seguro, path: "/", maxAge };
}

export function abrirSessao(res: NextResponse, token: string, usuario: Usuario, segundos: number) {
  res.cookies.set(COOKIE_TOKEN, token, opcoes(segundos));
  res.cookies.set(
    COOKIE_USUARIO,
    Buffer.from(JSON.stringify(usuario)).toString("base64url"),
    opcoes(segundos),
  );
}

export function encerrarSessao(res: NextResponse) {
  res.cookies.set(COOKIE_TOKEN, "", opcoes(0));
  res.cookies.set(COOKIE_USUARIO, "", opcoes(0));
}

export function lerToken(req: NextRequest): string | undefined {
  return req.cookies.get(COOKIE_TOKEN)?.value || undefined;
}

export function lerUsuario(req: NextRequest): Usuario | null {
  const valor = req.cookies.get(COOKIE_USUARIO)?.value;
  if (!valor || !lerToken(req)) return null;
  try {
    return JSON.parse(Buffer.from(valor, "base64url").toString("utf8")) as Usuario;
  } catch {
    return null;
  }
}
