# Backend Kontaz

API REST independente para o app Android. Requer Node.js 20+.

Além de autenticação, dashboard e transações, expõe `/v1/goals` para metas e `/v1/profile` para consultar/editar o nome do perfil. Todas as rotas de usuário verificam a sessão e respeitam as políticas RLS.

## Desenvolvimento local

```powershell
npm install
Copy-Item .env.example .env
# Preencha SUPABASE_URL e SUPABASE_ANON_KEY
npm run dev
```

Verificação rápida: `GET http://localhost:3000/health`.

## Segurança

Use somente a chave pública/anon do Supabase neste serviço. Cada rota protegida valida o bearer token e executa consultas em nome do usuário, mantendo RLS ativa. Nunca configure `service_role` neste processo.
