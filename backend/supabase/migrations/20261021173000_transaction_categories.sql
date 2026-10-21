create extension if not exists unaccent with schema extensions;

create table if not exists public.transaction_categories (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null check (type in ('income', 'expense', 'investment')),
  name text not null check (char_length(name) between 1 and 100),
  normalized_name text not null check (char_length(normalized_name) between 1 and 100),
  created_at timestamptz not null default now(),
  unique (user_id, type, normalized_name)
);

alter table public.transaction_categories enable row level security;

create policy "Users can read their transaction categories"
  on public.transaction_categories for select
  using (auth.uid() = user_id);

create policy "Users can create their transaction categories"
  on public.transaction_categories for insert
  with check (auth.uid() = user_id);

create policy "Users can update their transaction categories"
  on public.transaction_categories for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "Users can delete their transaction categories"
  on public.transaction_categories for delete
  using (auth.uid() = user_id);

with normalized_transactions as (
  select
    user_id,
    type,
    btrim(regexp_replace(category, '\s+', ' ', 'g')) as name,
    lower(extensions.unaccent(btrim(regexp_replace(category, '\s+', ' ', 'g')))) as normalized_name
  from public.transactions
  where nullif(btrim(category), '') is not null
),
ranked_categories as (
  select
    user_id,
    type,
    name,
    normalized_name,
    row_number() over (
      partition by user_id, type, normalized_name
      order by count(*) desc, name
    ) as name_rank
  from normalized_transactions
  group by user_id, type, name, normalized_name
)
insert into public.transaction_categories (user_id, type, name, normalized_name)
select user_id, type, name, normalized_name
from ranked_categories
where name_rank = 1
  and not (
    (type = 'income' and normalized_name in (
      'salario', 'freelance', 'investimentos', 'vendas', 'beneficios', 'outros'
    ))
    or (type = 'expense' and normalized_name in (
      'alimentacao', 'moradia', 'transporte', 'saude', 'educacao', 'lazer', 'contas', 'outros'
    ))
    or (type = 'investment' and normalized_name in (
      'renda variavel', 'renda fixa', 'fundos', 'criptomoedas', 'previdencia', 'outros'
    ))
  )
on conflict (user_id, type, normalized_name) do nothing;
