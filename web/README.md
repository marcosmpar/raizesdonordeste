# Web — front-end e BFF

Aplicação Next.js (App Router, TypeScript, Tailwind). As telas ficam em `src/app`; o BFF são as
rotas em `src/app/api`, apoiadas em `src/server`.

```bash
npm install
npm run dev     # http://localhost:3000 (a API precisa estar em http://localhost:8080)
npm test        # testes do BFF
npm run lint
npm run build
```

Variáveis de ambiente: ver `.env.example`.
