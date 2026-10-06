# Arquitetura inicial

## Componentes

```text
android-app (Kotlin / Jetpack Compose)
        │ HTTPS + token da sessão
        ▼
backend (TypeScript / Fastify)
        │ Supabase Auth + Postgres sob identidade do usuário
        ▼
Supabase
```

- O app Android apresenta as telas e consome somente a API do Kontaz para funcionalidades do produto.
- O backend verifica o token de acesso com Supabase Auth e encaminha as consultas com esse mesmo token. As políticas RLS continuam sendo uma camada obrigatória de autorização.
- Nenhuma chave `service_role`, senha de banco ou segredo de provedor deve ser distribuído no aplicativo.
- O backend não deve confiar em `user_id` enviado pelo cliente: a identidade vem do token verificado.
- A API usa o prefixo `/v1`; mudanças incompatíveis devem introduzir uma nova versão.

## Projetos separados

`android-app` e `backend` têm dependências, comandos de build e ciclos de entrega independentes. Permanecem no mesmo repositório por enquanto para simplificar o desenvolvimento e permitir uma futura separação sem alterar a arquitetura em runtime.

## Autenticação inicial

O login é mediado pelo backend, que encaminha credenciais ao endpoint oficial do Supabase Auth e devolve a sessão. Nas rotas privadas, o app envia o access token em `Authorization: Bearer <token>`. O backend valida o token com `auth.getUser(token)` e usa a sessão do usuário para consultar o banco protegido por RLS.

O Android mantém access e refresh tokens em armazenamento criptografado e tenta renovar a sessão após receber `401`. O refresh é encaminhado pelo backend ao Supabase Auth. O logout local remove os tokens armazenados. Não persistir tokens em preferências sem criptografia.

## Próximos passos arquiteturais

- Gerar tipos do banco Supabase e compartilhá-los apenas dentro do backend.
- Definir ambiente de staging separado de produção.
- Adicionar telemetria sem registrar tokens, senhas ou conteúdo financeiro pessoal.
- Revisar autorização e limites da IA e dos planos antes de introduzir essas funcionalidades.
