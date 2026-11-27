"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { bff } from "@/lib/api";
import type { Usuario } from "@/lib/tipos";

type Sessao = {
  /** undefined enquanto carrega; null quando não há login */
  usuario: Usuario | null | undefined;
  definir: (usuario: Usuario | null) => void;
};

const Contexto = createContext<Sessao>({ usuario: undefined, definir: () => {} });

export function SessaoProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null | undefined>(undefined);
  useEffect(() => {
    bff<{ user: Usuario | null }>("/auth/sessao")
      .then((r) => setUsuario(r.dados.user))
      .catch(() => setUsuario(null));
  }, []);
  return <Contexto value={{ usuario, definir: setUsuario }}>{children}</Contexto>;
}

export const useSessao = () => useContext(Contexto);
