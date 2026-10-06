import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { env } from "../env.js";

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

const signupSchema = loginSchema.extend({
  password: z.string().min(6).max(128),
  fullName: z.string().trim().min(1).max(120),
});

const recoverSchema = z.object({ email: z.string().email() });

async function supabaseAuthRequest(path: string, body: unknown) {
  return fetch(`${env.SUPABASE_URL}/auth/v1/${path}`, {
    method: "POST",
    headers: {
      apikey: env.SUPABASE_ANON_KEY,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
}

function upstreamFailureStatus(status: number, invalidCredentialsStatus: 400 | 401) {
  if (status === 429) return 429;
  if (status === 400 || status === 401 || status === 422) return invalidCredentialsStatus;
  return 502;
}

export async function registerAuthRoutes(app: FastifyInstance) {
  app.post("/login", async (request, reply) => {
    const parsed = loginSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Provide a valid email and password." } });
    }

    let response: Response;
    try {
      response = await supabaseAuthRequest("token?grant_type=password", parsed.data);
    } catch (error) {
      request.log.error({ err: error }, "Supabase login request failed");
      return reply.code(502).send({ error: { code: "AUTH_PROVIDER_UNAVAILABLE", message: "Login is temporarily unavailable." } });
    }
    if (!response.ok) {
      request.log.warn({ statusCode: response.status }, "Supabase rejected login");
      const status = upstreamFailureStatus(response.status, 401);
      return reply.code(status).send({
        error: {
          code: status === 401 ? "INVALID_CREDENTIALS" : status === 429 ? "RATE_LIMITED" : "AUTH_PROVIDER_ERROR",
          message: status === 401
            ? "Email or password is incorrect."
            : status === 429
              ? "Too many authentication attempts. Try again later."
              : "Login is temporarily unavailable.",
        },
      });
    }
    return reply.send(await response.json());
  });

  app.post("/signup", async (request, reply) => {
    const parsed = signupSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Provide a valid name, email, and password." } });
    }

    let response: Response;
    try {
      response = await supabaseAuthRequest("signup", {
        email: parsed.data.email,
        password: parsed.data.password,
        data: { full_name: parsed.data.fullName },
      });
    } catch (error) {
      request.log.error({ err: error }, "Supabase signup request failed");
      return reply.code(502).send({ error: { code: "AUTH_PROVIDER_UNAVAILABLE", message: "Account creation is temporarily unavailable." } });
    }
    if (!response.ok) {
      request.log.warn({ statusCode: response.status }, "Supabase rejected signup");
      const status = upstreamFailureStatus(response.status, 400);
      return reply.code(status).send({
        error: {
          code: status === 400 ? "SIGNUP_FAILED" : status === 429 ? "RATE_LIMITED" : "AUTH_PROVIDER_ERROR",
          message: status === 400
            ? "The account could not be created. Check your details and try again."
            : status === 429
              ? "Too many authentication attempts. Try again later."
              : "Account creation is temporarily unavailable.",
        },
      });
    }
    return reply.code(201).send(await response.json());
  });

  app.post("/recover", async (request, reply) => {
    const parsed = recoverSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Provide a valid email address." } });
    }

    let response: Response;
    try {
      response = await supabaseAuthRequest("recover", {
        email: parsed.data.email,
        redirect_to: env.SUPABASE_REDIRECT_URL,
      });
    } catch (error) {
      request.log.error({ err: error }, "Supabase recovery request failed");
      return reply.code(502).send({ error: { code: "AUTH_PROVIDER_UNAVAILABLE", message: "Password recovery is temporarily unavailable." } });
    }
    if (!response.ok) {
      if (response.status === 429) {
        return reply.code(429).send({ error: { code: "RATE_LIMITED", message: "Too many recovery requests. Try again later." } });
      }
      request.log.error({ statusCode: response.status }, "Supabase recovery request was rejected");
      return reply.code(502).send({ error: { code: "RECOVERY_FAILED", message: "Password recovery is temporarily unavailable." } });
    }
    return reply.code(202).send({ message: "If the account exists, recovery instructions have been sent." });
  });

  app.post("/refresh", async (request, reply) => {
    const schema = z.object({ refresh_token: z.string().min(1) });
    const parsed = schema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "A refresh token is required." } });
    }

    let response: Response;
    try {
      response = await supabaseAuthRequest("token?grant_type=refresh_token", parsed.data);
    } catch (error) {
      request.log.error({ err: error }, "Supabase token refresh failed");
      return reply.code(502).send({ error: { code: "AUTH_PROVIDER_UNAVAILABLE", message: "Session refresh is temporarily unavailable." } });
    }
    if (!response.ok) {
      request.log.warn({ statusCode: response.status }, "Supabase rejected refresh token");
      const status = upstreamFailureStatus(response.status, 401);
      return reply.code(status).send({
        error: {
          code: status === 401 ? "INVALID_REFRESH_TOKEN" : status === 429 ? "RATE_LIMITED" : "AUTH_PROVIDER_ERROR",
          message: status === 401
            ? "The session must be renewed by signing in again."
            : status === 429
              ? "Session refresh is temporarily rate limited."
              : "Session refresh is temporarily unavailable.",
        },
      });
    }
    return reply.send(await response.json());
  });
}
