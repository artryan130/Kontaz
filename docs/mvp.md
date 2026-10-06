# Escopo do MVP Android

## 1. Login

- Entrar com e-mail e senha.
- Criar conta e solicitar recuperação de senha.
- Exibir mensagens de validação e estados de carregamento/erro.
- Manter sessão de forma segura e permitir logout.

## 2. Dashboard

- Selecionar o mês de referência.
- Exibir receitas, despesas, investimentos e saldo do mês.
- Mostrar estado vazio quando não houver lançamentos.
- Exibir data de atualização e permitir atualizar os dados.

## 3. Transações

- Consultar, criar, editar e excluir receita, despesa ou investimento.
- Campos iniciais: valor, tipo, categoria, descrição opcional e data.
- Exibir confirmação antes da exclusão.
- Validar valor positivo, tipo suportado e data válida.

## 4. Histórico

- Listar lançamentos em ordem decrescente de data e criação.
- Filtrar por intervalo de datas e tipo.
- Preparar paginação antes de depender de grandes volumes de dados.

## Fora do primeiro corte

IA, assinatura/pagamentos, investimentos avançados, metas, orçamentos, notificações, importação/exportação e sincronização offline. Essas funcionalidades podem ser planejadas depois de validar os quatro fluxos do MVP.

## Critérios de aceite gerais

- Nenhum usuário pode consultar ou alterar dados de outro usuário.
- Falhas de rede e erros de autenticação têm estados visíveis, sem apresentar dados antigos como se fossem atuais.
- Valores são tratados como decimais monetários; datas de transação são datas civis (`YYYY-MM-DD`), sem deslocamento de fuso.
- Operações de gravação são refletidas no dashboard e no histórico após sucesso do servidor.
