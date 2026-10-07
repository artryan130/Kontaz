import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { authenticate } from "../auth.js";

const targetDateSchema = z.string().regex(/^\d{4}-\d{2}-\d{2}$/).refine((value) => {
  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
});

const goalSchema = z.object({
  title: z.string().trim().min(1).max(100),
  target_amount: z.number().finite().positive().refine((value) => Number.isInteger(value * 100)),
  current_amount: z.number().finite().min(0).refine((value) => Number.isInteger(value * 100)).default(0),
  category: z.string().trim().min(1).max(50).default("financial"),
  icon_type: z.string().trim().min(1).max(30).default("piggy"),
  target_date: targetDateSchema.nullable().optional(),
});

const updateGoalSchema = goalSchema.partial().refine((value) => Object.keys(value).length > 0);
const idSchema = z.string().uuid();

export async function registerGoalRoutes(app: FastifyInstance) {
  app.get("/", { preHandler: authenticate }, async (request, reply) => {
    const { data, error } = await request.supabase
      .from("goals")
      .select("*")
      .eq("user_id", request.userId)
      .order("created_at", { ascending: false });
    if (error) {
      request.log.error({ err: error }, "Could not list goals");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Goals could not be loaded." } });
    }
    return reply.send({ items: data });
  });

  app.post("/", { preHandler: authenticate }, async (request, reply) => {
    const parsed = goalSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Goal data is invalid." } });
    }
    const { data, error } = await request.supabase
      .from("goals")
      .insert({ ...parsed.data, user_id: request.userId })
      .select("*")
      .single();
    if (error) {
      request.log.error({ err: error }, "Could not create goal");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Goal could not be created." } });
    }
    return reply.code(201).send(data);
  });

  app.patch("/:id", { preHandler: authenticate }, async (request, reply) => {
    const id = idSchema.safeParse((request.params as { id: string }).id);
    const body = updateGoalSchema.safeParse(request.body);
    if (!id.success || !body.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Goal id or data is invalid." } });
    }
    const { data, error } = await request.supabase
      .from("goals")
      .update(body.data)
      .eq("id", id.data)
      .eq("user_id", request.userId)
      .select("*")
      .maybeSingle();
    if (error) {
      request.log.error({ err: error }, "Could not update goal");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Goal could not be updated." } });
    }
    if (!data) return reply.code(404).send({ error: { code: "NOT_FOUND", message: "Goal was not found." } });
    return reply.send(data);
  });

  app.delete("/:id", { preHandler: authenticate }, async (request, reply) => {
    const id = idSchema.safeParse((request.params as { id: string }).id);
    if (!id.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Goal id is invalid." } });
    }
    const { data, error } = await request.supabase
      .from("goals")
      .delete()
      .eq("id", id.data)
      .eq("user_id", request.userId)
      .select("id")
      .maybeSingle();
    if (error) {
      request.log.error({ err: error }, "Could not delete goal");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Goal could not be deleted." } });
    }
    if (!data) return reply.code(404).send({ error: { code: "NOT_FOUND", message: "Goal was not found." } });
    return reply.code(204).send();
  });
}
