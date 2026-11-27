"use client";

import { useCallback, useEffect, useState } from "react";
import { bff, mensagemDe } from "./api";

/** Carrega um recurso do BFF; `caminho` nulo não busca nada. */
export function useDados<T>(caminho: string | null) {
  const [estado, setEstado] = useState<{ caminho?: string; dados?: T; erro?: string }>({});
  const [versao, setVersao] = useState(0);

  useEffect(() => {
    if (!caminho) return;
    let ativo = true;
    bff<T>(caminho)
      .then((r) => ativo && setEstado({ caminho, dados: r.dados }))
      .catch((e) => ativo && setEstado({ caminho, erro: mensagemDe(e) }));
    return () => {
      ativo = false;
    };
  }, [caminho, versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);
  // não exibe dados de um caminho anterior
  const atual = estado.caminho === caminho ? estado : {};
  return { dados: atual.dados, erro: atual.erro, recarregar };
}
