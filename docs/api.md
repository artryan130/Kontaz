# Contrato HTTP inicial — `/v1`

Todos os valores financeiros são números decimais em reais. Datas são strings `YYYY-MM-DD`. Rotas de produto exigem `Authorization: Bearer <access_token>`. Respostas de erro usam `{ "error": { "code": "...", "message": "..." } }`.

## Autenticação

### `POST /v1/auth/login`

Request: `{ "email": "pessoa@exemplo.com", "password": "..." }`

Resposta de sucesso: sessão Supabase (`access_token`, `refresh_token`, `expires_in`, `token_type`, `user`). Credenciais inválidas retornam `401`.

### `POST /v1/auth/signup`

Request: `{ "email": "pessoa@exemplo.com", "password": "...", "fullName": "..." }`

Encaminha o cadastro ao Supabase Auth. O comportamento de confirmação de e-mail depende da configuração do projeto Supabase.

### `POST /v1/auth/recover`

Request: `{ "email": "pessoa@exemplo.com" }`

Solicita ao Supabase o envio do e-mail de recuperação.

Refresh e logout da sessão serão definidos antes do primeiro piloto Android.

## Produto

### `GET /v1/dashboard?period=YYYY-MM`

Retorna `{ "period": "YYYY-MM", "income": 0, "expenses": 0, "investments": 0, "balance": 0 }`.

### `GET /v1/transactions?from=YYYY-MM-DD&to=YYYY-MM-DD&type=income|expense|investment&limit=50&offset=0`

Filtros são opcionais. Resposta: `{ "items": [...], "limit": 50, "offset": 0 }`.

### `POST /v1/transactions`

Request: `{ "amount": 125.50, "type": "expense", "category": "Alimentação", "description": "Almoço", "date": "2026-10-06" }`. Amount must be positive with at most two decimal places.

### `GET /v1/transactions/{id}`, `PATCH /v1/transactions/{id}`, `DELETE /v1/transactions/{id}`

O `user_id` é derivado da sessão e nunca aceito como campo de entrada.

## Códigos de resposta

- `400`: parâmetros ou corpo inválidos.
- `401`: token ausente, inválido ou credenciais inválidas.
- `404`: recurso inexistente ou que não pertence ao usuário autenticado.
- `502`: falha em serviço upstream.
