# Kontaz Android

App nativo em Kotlin/Jetpack Compose. Requer Android Studio, JDK 17 e Android SDK 35.

Abra `android-app` no Android Studio e sincronize o projeto. O emulador usa por padrão a API local em `http://10.0.2.2:3000/`.

Para alterar a URL, adicione `API_BASE_URL=https://api.exemplo.com/` ao `android-app/local.properties` ou use a propriedade Gradle `-PAPI_BASE_URL=https://api.exemplo.com/`. O tráfego HTTP sem TLS só é permitido no manifest de debug; builds release devem apontar para HTTPS.

O MVP já tem a estrutura das telas para autenticação, resumo mensal, transações e histórico e o cliente HTTP correspondente. A validação de build Android depende de JDK 17 e SDK instalados.
