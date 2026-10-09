import type { FastifyInstance } from "fastify";
import { env } from "../env.js";

const deletionPage = `<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Excluir conta Kontaz</title>
</head>
<body>
  <main>
    <h1>Excluir conta Kontaz</h1>
    <p>Entre com sua conta para solicitar a exclusão permanente. Isso remove sua conta e os dados associados, incluindo perfil, transações e metas, tanto do aplicativo Kontaz quanto do app web que usa esta mesma conta.</p>
    <p>Esta ação não pode ser desfeita.</p>
    <form id="deletion-form">
      <label for="email">E-mail da conta</label><br>
      <input id="email" name="email" type="email" autocomplete="username" required><br><br>
      <label for="password">Senha</label><br>
      <input id="password" name="password" type="password" autocomplete="current-password" required><br><br>
      <label><input id="confirm" type="checkbox" required> Entendo que a conta e os dados associados serão excluídos permanentemente.</label><br><br>
      <button id="submit" type="submit">Excluir minha conta</button>
    </form>
    <p id="status" role="status" aria-live="polite"></p>
    <p>Se você não se lembra da senha, recupere o acesso pelo aplicativo Kontaz antes de solicitar a exclusão.</p>
  </main>
  <script src="/account-deletion.js" defer></script>
</body>
</html>`;

const deletionScript = `const form = document.querySelector("#deletion-form");
const submit = document.querySelector("#submit");
const status = document.querySelector("#status");
const supabaseUrl = ${JSON.stringify(env.SUPABASE_URL)};
const supabaseAnonKey = ${JSON.stringify(env.SUPABASE_ANON_KEY)};

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!form.reportValidity()) return;

  submit.disabled = true;
  status.textContent = "Verificando a conta e processando a solicitação...";
  const values = new FormData(form);

  try {
    const loginResponse = await fetch("/v1/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: values.get("email"),
        password: values.get("password")
      })
    });
    if (!loginResponse.ok) throw new Error("Não foi possível confirmar a conta. Verifique o e-mail e a senha.");
    const session = await loginResponse.json();
    if (!session.access_token) throw new Error("A conta não retornou uma sessão válida.");

    const deleteResponse = await fetch(supabaseUrl + "/functions/v1/delete-account", {
      method: "POST",
      headers: {
        "Authorization": "Bearer " + session.access_token,
        "apikey": supabaseAnonKey
      }
    });
    const result = await deleteResponse.json().catch(() => null);
    if (!deleteResponse.ok) {
      throw new Error(result?.error?.message || "Não foi possível excluir a conta. Tente novamente mais tarde.");
    }
    if (result?.success !== true) throw new Error("O servidor não confirmou a exclusão da conta. Tente novamente mais tarde.");

    form.remove();
    status.textContent = "Sua conta e os dados associados foram excluídos permanentemente.";
  } catch (error) {
    status.textContent = error instanceof Error ? error.message : "Ocorreu um erro ao processar a solicitação.";
    submit.disabled = false;
  }
});`;

function escapeHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export async function registerAccountRoutes(app: FastifyInstance) {
  app.get("/privacy", async (request, reply) => {
    if (!env.SUPPORT_EMAIL) {
      request.log.error("Privacy policy is unavailable because SUPPORT_EMAIL is not configured");
      return reply.code(503).send({
        error: { code: "PRIVACY_POLICY_UNAVAILABLE", message: "Privacy policy is temporarily unavailable." },
      });
    }
    const contact = escapeHtml(env.SUPPORT_EMAIL);
    const privacyPolicy = `<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Política de Privacidade — Kontaz</title>
</head>
<body>
  <main>
    <h1>Política de Privacidade do Kontaz</h1>
    <p>Última atualização: 8 de outubro de 2026</p>
    <p>O Kontaz é um aplicativo de organização financeira pessoal. O responsável pelo tratamento é o desenvolvedor do Kontaz identificado na ficha do aplicativo na Google Play.</p>

    <h2>Dados tratados</h2>
    <ul>
      <li>Dados da conta: endereço de e-mail, nome e credenciais de autenticação.</li>
      <li>Dados financeiros que você registra: valores, tipos e categorias de transações, descrições, datas e informações das metas financeiras.</li>
      <li>Dados técnicos necessários para autenticação, segurança e funcionamento da API.</li>
    </ul>
    <p>O aplicativo guarda os tokens de sessão no armazenamento criptografado do Android e transmite dados à API Kontaz por HTTPS.</p>

    <h2>Como os dados são usados e compartilhados</h2>
    <p>Usamos os dados para autenticar sua conta e fornecer as funções de painel, transações, histórico, metas, calculadoras e perfil. O backend é hospedado no Render e os dados de autenticação e do aplicativo são armazenados no projeto Supabase configurado pelo Kontaz (atualmente o projeto Supabase usado pelo app web Lovable). Esses fornecedores processam dados para operar os serviços. O Kontaz não vende dados pessoais e esta versão Android não integra SDKs de publicidade ou análise.</p>
    <p>Os dados podem ser tratados quando necessário para cumprir obrigações legais ou proteger a segurança do serviço.</p>

    <h2>Retenção e exclusão</h2>
    <p>Os dados associados à conta são mantidos enquanto ela estiver ativa. Você pode solicitar a exclusão pelo perfil no aplicativo ou em <a href="/account-deletion">Excluir conta Kontaz</a>. A solicitação exclui a conta Supabase e os dados do Kontaz associados, incluindo perfil, transações, metas e configurações financeiras. Cópias de segurança dos provedores podem permanecer durante os ciclos técnicos de retenção desses serviços.</p>

    <h2>Segurança e seus direitos</h2>
    <p>Usamos HTTPS nas comunicações com o serviço e armazenamento criptografado para os tokens de sessão no dispositivo. Nenhum método de transmissão ou armazenamento pode garantir segurança absoluta. Você pode corrigir o nome do perfil no aplicativo e solicitar a exclusão da conta e dos dados associados.</p>

    <h2>Alterações e contato</h2>
    <p>Esta política pode ser atualizada quando as práticas do aplicativo mudarem. Dúvidas ou solicitações relacionadas a privacidade: <a href="mailto:${contact}">${contact}</a>.</p>
  </main>
</body>
</html>`;

    return reply
      .header("Cache-Control", "no-store")
      .header("Content-Security-Policy", "default-src 'none'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'")
      .header("Referrer-Policy", "no-referrer")
      .header("X-Content-Type-Options", "nosniff")
      .type("text/html; charset=utf-8")
      .send(privacyPolicy);
  });

  app.get("/account-deletion", async (_request, reply) => {
    return reply
      .header("Cache-Control", "no-store")
      .header("Content-Security-Policy", `default-src 'none'; script-src 'self'; connect-src 'self' ${new URL(env.SUPABASE_URL).origin}; form-action 'self'; base-uri 'none'; frame-ancestors 'none'`)
      .header("Referrer-Policy", "no-referrer")
      .header("X-Content-Type-Options", "nosniff")
      .type("text/html; charset=utf-8")
      .send(deletionPage);
  });

  app.get("/account-deletion.js", async (_request, reply) => {
    return reply
      .header("Cache-Control", "no-store")
      .header("Content-Security-Policy", "default-src 'none'")
      .header("X-Content-Type-Options", "nosniff")
      .type("application/javascript; charset=utf-8")
      .send(deletionScript);
  });

}
