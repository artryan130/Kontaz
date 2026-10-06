# Kontaz

Workspace para a nova aplicação Android e o backend independente do Kontaz.

## Projetos

- [`android-app`](./android-app): aplicação Android nativa em Kotlin e Jetpack Compose.
- [`backend`](./backend): API REST em TypeScript/Fastify. O backend autentica requisições e acessa o Supabase em nome do usuário.
- [`docs`](./docs): arquitetura e escopo inicial do produto.

O diretório de trabalho inicial continha apenas este README; o app web de referência continua no repositório GitHub indicado durante a análise e não foi copiado nem alterado aqui.

## MVP

1. Login e recuperação de acesso.
2. Dashboard financeiro mensal.
3. Criação, edição e exclusão de transações.
4. Histórico com filtros de período e tipo.

## Começar pelo backend

Requer Node.js 20 ou superior.

```powershell
cd backend
npm install
Copy-Item .env.example .env
# Configure SUPABASE_URL e SUPABASE_ANON_KEY no .env
npm run dev
```

Por segurança, não use uma chave `service_role` neste backend: as consultas são feitas com a sessão do usuário e devem continuar protegidas pelas políticas RLS.

## Android

Abra a pasta `android-app` no Android Studio. Detalhes de ambiente e configuração da API estão em [`android-app/README.md`](./android-app/README.md). Para emulador Android, um backend executado no host normalmente fica acessível em `http://10.0.2.2:3000/`.

O backend e o Android são projetos independentes. O contrato HTTP é mantido em [`docs/api.md`](./docs/api.md).
