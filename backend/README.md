# Backend Kontaz

API REST independente para o app Android. Requer Node.js 20+.

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
