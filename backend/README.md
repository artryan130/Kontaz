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

As operações normais usam a chave pública/anon do Supabase e executam consultas em nome do usuário, mantendo RLS ativa. A exclusão de conta é processada pela Edge Function `delete-account` do mesmo projeto Supabase, que valida o bearer token e deve apagar apenas os dados e a identidade do usuário autenticado. A chave anon é pública; nunca use uma chave `service_role` no Android ou no repositório.

## Publicação

- Render: configure `SUPABASE_URL` e `SUPABASE_ANON_KEY` para o mesmo projeto Supabase/Lovable da Edge Function. O backend usa essas credenciais para autenticação e para exibir a página pública de exclusão.
- O recurso público para solicitações de exclusão é `https://<domínio-da-api>/account-deletion`.
- A política de privacidade pública é `https://<domínio-da-api>/privacy`; configure `SUPPORT_EMAIL` com um endereço real monitorado pelo responsável pelo app.
- No Supabase Auth, permita `kontaz://auth/recovery` nas Redirect URLs para que o link de redefinição de senha abra o aplicativo.

O Android chama `POST https://<SUPABASE_URL>/functions/v1/delete-account` com o token de acesso do usuário e `SUPABASE_ANON_KEY`. A página pública faz login no backend e encaminha o token à mesma função. Publique e teste essa Edge Function antes de depender dela em produção. Sem `SUPPORT_EMAIL`, a política de privacidade responde `503`.
