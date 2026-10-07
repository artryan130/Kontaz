# Kontaz Android

App nativo em Kotlin/Jetpack Compose. Requer Android Studio, JDK 17 e Android SDK 35.

Abra `android-app` no Android Studio e sincronize o projeto. O emulador usa por padrão a API local em `http://10.0.2.2:3000/`.

Para alterar a URL, adicione `API_BASE_URL=https://api.exemplo.com/` ao `android-app/local.properties` ou use a propriedade Gradle `-PAPI_BASE_URL=https://api.exemplo.com/`. O tráfego HTTP sem TLS só é permitido no manifest de debug; builds release devem apontar para HTTPS.

O MVP já tem a estrutura das telas para autenticação, resumo mensal, transações e histórico e o cliente HTTP correspondente. A validação de build Android depende de JDK 17 e SDK instalados.

## Interface do MVP

A interface usa cartões claros com cantos arredondados, verde para receitas e ações principais e laranja para despesas, baseada na home mobile do Kontaz web. A navegação inferior contém somente Início, adicionar transação e Transações; telas fora do MVP, como Lumini e Mais, não são exibidas.

O escopo foi ampliado com Metas, Calculadoras e Perfil. Metas e perfil usam a API autenticada; calculadoras executam as estimativas localmente. O menu inferior oferece Início, Metas, adicionar transação, Transações e Calculadora; Perfil é acessado pelo ícone no cabeçalho da Home. O ícone do aplicativo usa `app/src/main/res/drawable-nodpi/kontaz_launcher.png`.
