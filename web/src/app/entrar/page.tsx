"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Sertao } from "@/components/ilustracoes";
import { useSessao } from "@/components/Sessao";
import { Aviso, botao, campo } from "@/components/ui";
import { bff, mensagemDe } from "@/lib/api";
import type { Usuario } from "@/lib/tipos";

export default function PaginaEntrar() {
  const { definir } = useSessao();
  const router = useRouter();
  const [cadastrando, setCadastrando] = useState(false);
  const [mostrar, setMostrar] = useState(false);
  const [erro, setErro] = useState("");
  const [enviando, setEnviando] = useState(false);

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const dados = Object.fromEntries(new FormData(evento.currentTarget));
    setErro("");
    setEnviando(true);
    try {
      if (cadastrando) await bff("/auth/cadastro", { metodo: "POST", corpo: dados });
      const { dados: r } = await bff<{ user: Usuario }>("/auth/login", {
        metodo: "POST",
        corpo: dados,
      });
      definir(r.user);
      router.push(r.user.perfil === "CLIENTE" ? "/" : "/painel");
    } catch (e) {
      setErro(mensagemDe(e));
      setEnviando(false);
    }
  }

  return (
    <div className="flex grow flex-col bg-cordel md:grid md:grid-cols-2 md:bg-papel">
      <section className="flex grow flex-col justify-between gap-6 bg-cordel p-6 md:p-16">
        <Link href="/" className="titulo self-start text-xl">
          ← Raízes do Nordeste
        </Link>
        <Sertao className="w-52 max-w-full md:w-full md:max-w-xl md:self-center" />
        <p className="titulo max-w-lg text-4xl leading-tight md:text-[56px] md:leading-none">
          Comida de raiz, pedido sem fila.
        </p>
      </section>
      <section className="flex flex-col justify-center rounded-t-3xl bg-papel px-6 py-8 md:items-center md:rounded-none">
        <div className="flex w-full flex-col gap-4 md:max-w-sm">
          <h1 className="titulo text-3xl md:text-5xl">{cadastrando ? "Criar conta" : "Entrar"}</h1>
          <form onSubmit={enviar} className="flex flex-col gap-4">
            {cadastrando && (
              <label className="flex flex-col gap-1.5 text-sm font-bold">
                Nome
                <input className={`${campo} font-normal`} name="nome" required maxLength={120} autoComplete="name" />
              </label>
            )}
            <label className="flex flex-col gap-1.5 text-sm font-bold">
              E-mail
              <input className={`${campo} font-normal`} name="email" type="email" required autoComplete="email" />
            </label>
            <div className="flex flex-col gap-1.5 text-sm font-bold">
              <div className="flex justify-between">
                <label htmlFor="senha">Senha</label>
                <button type="button" className="underline" aria-pressed={mostrar} onClick={() => setMostrar(!mostrar)}>
                  {mostrar ? "Ocultar" : "Mostrar"}
                </button>
              </div>
              <input
                id="senha"
                className={`${campo} font-normal`}
                name="senha"
                type={mostrar ? "text" : "password"}
                required
                minLength={cadastrando ? 8 : undefined}
                autoComplete={cadastrando ? "new-password" : "current-password"}
              />
            </div>
            <Aviso>{erro}</Aviso>
            <button className={`${botao} min-h-14 text-base`} disabled={enviando}>
              {enviando ? "Aguarde…" : cadastrando ? "Criar conta e entrar" : "Entrar"}
            </button>
          </form>
          <button
            className="min-h-11 text-sm font-bold underline"
            onClick={() => {
              setCadastrando(!cadastrando);
              setErro("");
            }}
          >
            {cadastrando ? "Já tenho conta" : "Primeira vez? Criar conta"}
          </button>
        </div>
      </section>
    </div>
  );
}
