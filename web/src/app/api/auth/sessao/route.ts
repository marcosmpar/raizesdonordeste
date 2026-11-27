import { NextResponse } from "next/server";
import { rota } from "@/server/rota";
import { lerUsuario } from "@/server/sessao";

export const GET = rota(async ({ req }) => NextResponse.json({ user: lerUsuario(req) }));
