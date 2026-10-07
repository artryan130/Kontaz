import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { authenticate } from "../auth.js";

const profileUpdateSchema = z.object({
  full_name: z.string().trim().min(1).max(120),
});

export async function registerProfileRoutes(app: FastifyInstance) {
  app.get("/", { preHandler: authenticate }, async (request, reply) => {
    const profileResult = await request.supabase
      .from("profiles")
      .select("id,full_name,avatar_url")
      .eq("id", request.userId)
      .maybeSingle();
    if (profileResult.error) {
      request.log.error({ err: profileResult.error }, "Could not load profile data");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Profile could not be loaded." } });
    }
    return reply.send({
      id: request.userId,
      email: request.userEmail,
      full_name: profileResult.data?.full_name ?? request.userFullName,
      avatar_url: profileResult.data?.avatar_url ?? null,
    });
  });

  app.patch("/", { preHandler: authenticate }, async (request, reply) => {
    const parsed = profileUpdateSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Profile name is invalid." } });
    }
    const { data, error } = await request.supabase
      .from("profiles")
      .update({ full_name: parsed.data.full_name })
      .eq("id", request.userId)
      .select("id,full_name,avatar_url")
      .maybeSingle();
    if (error) {
      request.log.error({ err: error }, "Could not update profile");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Profile could not be updated." } });
    }
    if (!data) return reply.code(404).send({ error: { code: "NOT_FOUND", message: "Profile was not found." } });
    return reply.send({ ...data, email: request.userEmail });
  });
}
