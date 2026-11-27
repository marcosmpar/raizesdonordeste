"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { bff } from "@/lib/api";
import { limparSacola } from "@/lib/sacola";
import type { ContaFidelidade, Usuario } from "@/lib/tipos";
import { useDados } from "@/lib/useDados";
import { Logo } from "./ilustracoes";
import { useSessao } from "./Sessao";
import { botaoSecundario } from "./ui";

/** Telas de tela cheia, com navegação própria. */
const SEM_MOLDURA = ["/entrar", "/painel"];

const ICONES = {
  cardapio: "M4 6h14M4 11h14M4 16h9",
  pedidos: "M5 3h12v16l-3-2-3 2-3-2-3 2zM8 8h6M8 12h6",
  pontos: "M11 2.5l2.6 5.4 5.9.8-4.3 4.1 1.1 5.8L11 15.8l-5.3 2.8 1.1-5.8-4.3-4.1 5.9-.8z",
  painel: "M3 4h7v14H3zM13 4h6v8h-6z",
};

function destinos(usuario: Usuario | null | undefined) {
  const cliente = usuario?.perfil === "CLIENTE";
  return [
    { href: "/", texto: "Cardápio", curto: "Cardápio", icone: ICONES.cardapio, visivel: true },
    { href: "/pedidos", texto: cliente ? "Meus pedidos" : "Pedidos", curto: "Pedidos", icone: ICONES.pedidos, visivel: true },
    { href: "/fidelidade", texto: "Fidelidade", curto: "Pontos", icone: ICONES.pontos, visivel: cliente },
    { href: "/painel", texto: "Painel", curto: "Painel", icone: ICONES.painel, visivel: !!usuario && !cliente },
  ].filter((d) => d.visivel);
}

const ativo = (rota: string, href: string) => (href === "/" ? rota === "/" : rota.startsWith(href));

export function useSair() {
  const { definir } = useSessao();
  const router = useRouter();
  return async () => {
    await bff("/auth/logout", { metodo: "POST" }).catch(() => {});
    limparSacola();
    definir(null);
    router.push("/");
  };
}

export function Cabecalho() {
  const { usuario } = useSessao();
  const rota = usePathname();
  const sair = useSair();
  const { dados, recarregar } = useDados<{ conta: ContaFidelidade }>(
    usuario?.perfil === "CLIENTE" ? "/fidelidade" : null,
  );
  // o saldo muda ao pagar ou cancelar: busca de novo a cada navegação
  useEffect(() => recarregar(), [rota, recarregar]);

  if (SEM_MOLDURA.includes(rota)) return null;

  return (
    <header className="border-tinta md:border-b-2">
      <div className="flex h-16 items-center gap-4 px-5 md:h-[76px] md:gap-12 md:px-12">
        <Link href="/" className="flex items-center gap-2.5">
          <Logo />
          <span className="titulo text-lg md:text-[22px]">Raízes do Nordeste</span>
        </Link>
        <nav aria-label="Principal" className="hidden grow gap-8 text-[15px] font-medium md:flex">
          {destinos(usuario).map((d) => (
            <Link
              key={d.href}
              href={d.href}
              aria-current={ativo(rota, d.href) ? "page" : undefined}
              className="border-b-4 border-transparent pt-1 aria-[current=page]:border-cordel aria-[current=page]:font-bold"
            >
              {d.texto}
            </Link>
          ))}
        </nav>
        <div className="ml-auto flex items-center gap-3 text-sm">
          {usuario ? (
            <>
              {dados && (
                <Link href="/fidelidade" className="hidden rounded-full bg-bagaco px-3.5 py-2 font-semibold md:block">
                  {dados.conta.saldoPontos} pontos
                </Link>
              )}
              <span
                title={`${usuario.nome} (${usuario.perfil})`}
                className="flex size-9 items-center justify-center rounded-full bg-tinta font-bold text-white"
              >
                {usuario.nome.replace(/^Seed /, "").charAt(0).toUpperCase()}
                <span className="sr-only">
                  {usuario.nome} ({usuario.perfil})
                </span>
              </span>
              <button onClick={sair} className="min-h-11 font-bold underline">
                Sair
              </button>
            </>
          ) : usuario === null ? (
            <Link href="/entrar" className={botaoSecundario}>
              Entrar
            </Link>
          ) : null}
        </div>
      </div>
    </header>
  );
}

/** Barra de abas do celular. */
export function Abas() {
  const { usuario } = useSessao();
  const rota = usePathname();
  if (SEM_MOLDURA.includes(rota) || rota === "/sacola") return null;

  return (
    <nav
      aria-label="Principal"
      className="sticky bottom-0 z-10 flex border-t-[1.5px] border-linha bg-papel pb-[max(env(safe-area-inset-bottom),10px)] pt-2.5 text-xs md:hidden"
    >
      {destinos(usuario).map((d) => (
        <Link
          key={d.href}
          href={d.href}
          aria-current={ativo(rota, d.href) ? "page" : undefined}
          className="group flex flex-1 flex-col items-center gap-1 aria-[current=page]:font-bold"
        >
          <span className="flex w-14 justify-center rounded-full py-1 group-aria-[current=page]:bg-cordel">
            <svg width="22" height="22" viewBox="0 0 22 22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
              <path d={d.icone} />
            </svg>
          </span>
          {d.curto}
        </Link>
      ))}
    </nav>
  );
}
