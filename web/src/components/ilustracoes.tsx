/** Ilustrações chapadas, no espírito das capas de cordel. Todas decorativas. */

export function Logo() {
  return (
    <svg width="32" height="32" viewBox="0 0 32 32" aria-hidden className="shrink-0">
      <circle cx="16" cy="16" r="16" fill="#FFC62E" />
      <path
        d="M14 27V8a2 2 0 0 1 4 0v8h2v-4a2 2 0 0 1 4 0v6a2 2 0 0 1-2 2h-4v7zM14 21h-4a2 2 0 0 1-2-2v-5a2 2 0 0 1 4 0v3h2z"
        fill="#17130F"
      />
    </svg>
  );
}

export function Sol({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 220 220" aria-hidden className={className} fill="#17130F">
      <circle cx="110" cy="110" r="48" />
      <path d="M104 8h12l-6 42zM104 212h12l-6-42zM8 104v12l42-6zM212 104v12l-42-6zM34 43l9-9 26 36zM186 43l-9-9-26 36zM34 177l9 9 26-36zM186 177l-9 9-26-36z" />
    </svg>
  );
}

export function Sertao({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 600 460" aria-hidden className={className}>
      <g fill="#17130F">
        <circle cx="430" cy="110" r="70" />
        <path d="M0 400q150-90 300-40t300-20v120H0z" />
        <path d="M150 380V150a30 30 0 0 1 60 0v110h24v-60a22 22 0 0 1 44 0v78a26 26 0 0 1-26 26h-42v76zM150 300h-40a26 26 0 0 1-26-26v-70a22 22 0 0 1 44 0v52h22z" />
      </g>
      <path
        d="M430 12v-8M430 216v8M332 110h-14M528 110h14M361 41l-10-10M499 41l10-10M361 179l-10 10M499 179l10 10"
        stroke="#17130F"
        strokeWidth="8"
        strokeLinecap="round"
      />
      <path d="M40 430q140-60 270-26M330 428q120-34 240-20M180 160v180" stroke="#FFC62E" strokeWidth="5" fill="none" />
    </svg>
  );
}

const PRATOS = {
  baiao: {
    fundo: "bg-tinta",
    desenho: (
      <>
        <circle cx="40" cy="40" r="34" fill="#fff" />
        <circle cx="40" cy="40" r="25" fill="#FFC62E" />
        <g fill="#17130F">
          <circle cx="30" cy="33" r="4" />
          <circle cx="47" cy="30" r="3" />
          <circle cx="50" cy="47" r="4" />
          <circle cx="34" cy="50" r="3" />
        </g>
        <path d="M36 40l10-4 3 8-9 3z" fill="#17693F" />
      </>
    ),
  },
  carne: {
    fundo: "bg-pimenta",
    desenho: (
      <>
        <circle cx="40" cy="40" r="34" fill="#fff" />
        <g fill="#17130F">
          <rect x="19" y="27" width="28" height="11" rx="5" transform="rotate(-18 33 32)" />
          <rect x="24" y="43" width="30" height="11" rx="5" transform="rotate(-10 39 48)" />
        </g>
        <circle cx="55" cy="30" r="7" fill="#FFC62E" />
        <circle cx="59" cy="43" r="5" fill="#FFC62E" />
      </>
    ),
  },
  tapioca: {
    fundo: "bg-anil",
    desenho: (
      <>
        <circle cx="40" cy="40" r="34" fill="#fff" />
        <path d="M16 46a24 24 0 0 1 48 0z" fill="#F4F2EE" stroke="#17130F" strokeWidth="3" />
        <path d="M19 46h42l-6 9H27z" fill="#FFC62E" />
        <g fill="#17130F">
          <circle cx="32" cy="36" r="2" />
          <circle cx="43" cy="31" r="2" />
          <circle cx="50" cy="39" r="2" />
        </g>
      </>
    ),
  },
  bebida: {
    fundo: "bg-mandacaru",
    desenho: (
      <>
        <path d="M25 22h30l-4 46H29z" fill="#fff" />
        <path d="M27 36h26l-3 29H30z" fill="#FFC62E" />
        <path d="M44 36l9-28 5 2-8 27z" fill="#17130F" />
        <circle cx="27" cy="24" r="10" fill="#FFC62E" stroke="#17130F" strokeWidth="3" />
      </>
    ),
  },
  geral: {
    fundo: "bg-cordel",
    desenho: (
      <>
        <circle cx="40" cy="40" r="34" fill="#fff" />
        <path d="M18 44a22 20 0 0 1 44 0z" fill="#17130F" />
        <path d="M16 49h48M32 17q-5 5 0 10M45 17q-5 5 0 10" stroke="#17130F" strokeWidth="4" fill="none" strokeLinecap="round" />
      </>
    ),
  },
};

function tipoDoPrato(nome: string, categoria: string): keyof typeof PRATOS {
  if (/bebida/i.test(categoria)) return "bebida";
  if (/bai[aã]o/i.test(nome)) return "baiao";
  if (/carne/i.test(nome)) return "carne";
  if (/tapioca/i.test(nome)) return "tapioca";
  return "geral";
}

export function ImagemPrato({ nome, categoria }: { nome: string; categoria: string }) {
  const { fundo, desenho } = PRATOS[tipoDoPrato(nome, categoria)];
  return (
    <div className={`flex size-20 shrink-0 items-center justify-center rounded-xl md:size-28 ${fundo}`}>
      <svg viewBox="0 0 80 80" aria-hidden className="size-[72%]">
        {desenho}
      </svg>
    </div>
  );
}
