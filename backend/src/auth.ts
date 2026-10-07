import { createClient } from "@supabase/supabase-js";
import type { FastifyReply, FastifyRequest } from "fastify";
import { env } from "./env.js";
import type { Database } from "./database.types.js";

declare module "fastify" {
  interface FastifyRequest {
    supabase: ReturnType<typeof createClient<Database>>;
    userId: string;
    userEmail: string | null;
    userFullName: string | null;
  }
}

export async function authenticate(request: FastifyRequest, reply: FastifyReply) {
  const authorization = request.headers.authorization;
  const match = authorization?.match(/^Bearer\s+(.+)$/i);
  if (!match) {
    return reply.code(401).send({ error: { code: "UNAUTHORIZED", message: "Authentication is required." } });
  }

  const token = match[1];
  const client = createClient<Database>(env.SUPABASE_URL, env.SUPABASE_ANON_KEY, {
    auth: { persistSession: false, autoRefreshToken: false },
    global: { headers: { Authorization: `Bearer ${token}` } },
  });

  const { data, error } = await client.auth.getUser(token);
  if (error || !data.user) {
    return reply.code(401).send({ error: { code: "UNAUTHORIZED", message: "The access token is invalid or expired." } });
  }

  request.supabase = client;
  request.userId = data.user.id;
  request.userEmail = data.user.email ?? null;
  request.userFullName = typeof data.user.user_metadata.full_name === "string"
    ? data.user.user_metadata.full_name
    : null;
}
