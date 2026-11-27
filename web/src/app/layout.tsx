import type { Metadata } from "next";
import { Bricolage_Grotesque, Figtree } from "next/font/google";
import { Abas, Cabecalho } from "@/components/Navegacao";
import { SessaoProvider } from "@/components/Sessao";
import "./globals.css";

const fonteTitulo = Bricolage_Grotesque({ subsets: ["latin"], variable: "--fonte-titulo" });
const fonteCorpo = Figtree({ subsets: ["latin"], variable: "--fonte-corpo" });

export const metadata: Metadata = {
  title: "Raízes do Nordeste",
  description: "Pedidos online da rede de restaurantes Raízes do Nordeste",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="pt-BR" className={`${fonteTitulo.variable} ${fonteCorpo.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col font-sans">
        <SessaoProvider>
          <Cabecalho />
          <main className="flex flex-1 flex-col">{children}</main>
          <Abas />
        </SessaoProvider>
      </body>
    </html>
  );
}
