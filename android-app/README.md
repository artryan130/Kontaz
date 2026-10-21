# Kontaz Android

App nativo em Kotlin/Jetpack Compose. Requer Android Studio, JDK 17 e Android SDK 36.

Abra `android-app` no Android Studio e sincronize o projeto. Por padrão, o app usa a API hospedada em `https://kontaz-backend.onrender.com/`.

Para gerar o APK debug com a API hospedada, execute `.\gradlew.bat :app:assembleDebug` na pasta `android-app`. Para desenvolvimento local no emulador, sobrescreva o endereço com `-PAPI_BASE_URL=http://10.0.2.2:3000/` ou configure `API_BASE_URL=http://10.0.2.2:3000/` em `android-app/local.properties`. Também é possível passar outra URL HTTPS com `-PAPI_BASE_URL=https://api.exemplo.com/`. O tráfego HTTP sem TLS só é permitido no manifest de debug; builds release devem apontar para HTTPS.

O app suporta autenticação por e-mail/senha, resumo mensal, transações e histórico, metas, calculadoras e perfil. A Home limita as listas de transações recentes e despesas fixas a cinco itens, com atalhos para o histórico completo e o filtro de despesas fixas. O histórico exibe o saldo restante do mês e pagina os resultados para carregar todas as transações do período. Despesas podem ser lançadas mensalmente até uma data final ou divididas em parcelas, que aparecem nos respectivos meses; o total parcelado é distribuído em centavos para preservar o valor total. No perfil, é possível ativar o modo escuro e exportar as transações de um mês em CSV ou PDF para um local escolhido no dispositivo. Login com Google não faz parte do MVP. Os campos de senha permitem revelar/ocultar o conteúdo. O link `kontaz://auth/recovery` abre a tela de redefinição de senha.

As despesas recorrentes e parceladas usam o endpoint autenticado `POST /v1/transactions/series` no backend e os campos de recorrência/parcelamento da tabela `transactions`.

As categorias de transação incluem opções padrão e categorias personalizadas por usuário e tipo. A API lista/cria categorias em `/v1/categories`, normaliza acentos, espaços e caixa para evitar duplicatas e grava cada lançamento com a grafia canônica. Os gráficos agrupam variações históricas equivalentes. Antes de publicar o backend atualizado, aplique a migração `../backend/supabase/migrations/20261021173000_transaction_categories.sql` no Supabase (SQL Editor ou pipeline de migrations); ela cria a tabela com isolamento por usuário via RLS e inicializa categorias personalizadas a partir do histórico.

A exclusão no perfil chama a Edge Function `delete-account` do Supabase com o token de acesso do usuário e a chave pública anon. A função precisa ser publicada no ambiente de produção Lovable/Supabase antes do uso; um `404` significa que ela não existe naquele projeto/ambiente ou está com outro nome. A mesma solicitação pode ser feita pela página pública `/account-deletion` do backend. Configure `SUPABASE_URL` e `SUPABASE_ANON_KEY` em `android-app/local.properties` ou como propriedades Gradle para substituir os valores padrão de produção. Essa anon key é pública e fica incluída no app; nunca inclua uma chave `service_role`. A política pública em `/privacy` requer `SUPPORT_EMAIL` configurado no backend.

## Interface do MVP

A interface usa cartões claros com cantos arredondados, verde para receitas e ações principais e laranja para despesas, baseada na home mobile do Kontaz web. A navegação inferior contém somente Início, adicionar transação e Transações; telas fora do MVP, como Lumini e Mais, não são exibidas.

O escopo foi ampliado com Metas, Calculadoras e Perfil. Metas e perfil usam a API autenticada; calculadoras executam as estimativas localmente. O menu inferior oferece Início, Metas, adicionar transação, Transações e Calculadora; Perfil é acessado pelo ícone no cabeçalho da Home. O ícone do aplicativo usa `app/src/main/res/drawable-nodpi/kontaz_launcher.png`.

## Build de release

O app usa `compileSdk` e `targetSdk` 36. Gere um keystore de upload fora do repositório (por exemplo, com `keytool -genkeypair -v -keystore kontaz-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias kontaz-upload`) e configure as variáveis de ambiente `KONTAZ_UPLOAD_STORE_FILE`, `KONTAZ_UPLOAD_STORE_PASSWORD`, `KONTAZ_UPLOAD_KEY_ALIAS` e `KONTAZ_UPLOAD_KEY_PASSWORD`. Depois execute `.\gradlew.bat :app:bundleRelease`; o Android App Bundle será criado em `app\build\outputs\bundle\release\app-release.aab`. O build de release exige assinatura e URL de API HTTPS. Guarde o keystore e suas senhas em local seguro; não os versione nem os perca.

Adicione `kontaz://auth/recovery` às Redirect URLs permitidas em Supabase Auth para concluir a redefinição de senha. Antes da publicação, teste recuperação e exclusão de conta em uma conta de teste.
