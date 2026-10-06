import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { authenticate } from "../auth.js";

const dateSchema = z.string().regex(/^\d{4}-\d{2}-\d{2}$/).refine((value) => {
  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
});
const transactionSchema = z.object({
  amount: z.number().finite().positive().refine((value) => Number.isInteger(value * 100)),
  type: z.enum(["income", "expense", "investment"]),
  category: z.string().trim().min(1).max(100),
  description: z.string().trim().max(500).nullable().optional(),
  date: dateSchema,
});
const transactionUpdateSchema = transactionSchema.partial().refine((value) => Object.keys(value).length > 0);
const listQuerySchema = z.object({
  from: dateSchema.optional(),
  to: dateSchema.optional(),
  type: z.enum(["income", "expense", "investment"]).optional(),
  limit: z.coerce.number().int().min(1).max(100).default(50),
  offset: z.coerce.number().int().min(0).default(0),
});
const idSchema = z.string().uuid();

export async function registerTransactionRoutes(app: FastifyInstance) {
  app.get("/", { preHandler: authenticate }, async (request, reply) => {
    const filters = listQuerySchema.safeParse(request.query);
    if (!filters.success || (filters.data.from && filters.data.to && filters.data.from > filters.data.to)) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Transaction filters are invalid." } });
    }

    const { from, to, type, limit, offset } = filters.data;
    let query = request.supabase
      .from("transactions")
      .select("*")
      .eq("user_id", request.userId)
      .order("date", { ascending: false })
      .order("created_at", { ascending: false })
      .range(offset, offset + limit - 1);
    if (from) query = query.gte("date", from);
    if (to) query = query.lte("date", to);
    if (type) query = query.eq("type", type);

    const { data, error } = await query;
    if (error) {
      request.log.error({ err: error }, "Could not list transactions");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Transactions could not be loaded." } });
    }
    return reply.send({ items: data, limit, offset });
  });

  app.post("/", { preHandler: authenticate }, async (request, reply) => {
    const parsed = transactionSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Transaction data is invalid." } });
    }
    const { data, error } = await request.supabase
      .from("transactions")
      .insert({ ...parsed.data, user_id: request.userId })
      .select("*")
      .single();
    if (error) {
      request.log.error({ err: error }, "Could not create transaction");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Transaction could not be created." } });
    }
    return reply.code(201).send(data);
  });

  app.get("/:id", { preHandler: authenticate }, async (request, reply) => {
    const parsedId = idSchema.safeParse((request.params as { id: string }).id);
    if (!parsedId.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Transaction id is invalid." } });
    }
    const { data, error } = await request.supabase
      .from("transactions")
      .select("*")
      .eq("id", parsedId.data)
      .eq("user_id", request.userId)
      .maybeSingle();
    if (error) {
      request.log.error({ err: error }, "Could not load transaction");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Transaction could not be loaded." } });
    }
    if (!data) return reply.code(404).send({ error: { code: "NOT_FOUND", message: "Transaction was not found." } });
    return reply.send(data);
  });

  app.patch("/:id", { preHandler: authenticate }, async (request, reply) => {
    const parsedId = idSchema.safeParse((request.params as { id: string }).id);
    const parsedBody = transactionUpdateSchema.safeParse(request.body);
    if (!parsedId.success || !parsedBody.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Transaction id or data is invalid." } });
    }
    const { data, error } = await request.supabase
      .from("transactions")
      .update(parsedBody.data)
      .eq("id", parsedId.data)
      .eq("user_id", request.userId)
      .select("*")
      .maybeSingle();
    if (error) {
      request.log.error({ err: error }, "Could not update transaction");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Transaction could not be updated." } });
    }
    if (!data) return reply.code(404).send({ error: { code: "NOT_FOUND", message: "Transaction was not found." } });
    return reply.send(data);
  });

  app.delete("/:id", { preHandler: authenticate }, async (request, reply) => {
    const parsedId = idSchema.safeParse((request.params as { id: string }).id);
    if (!parsedId.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Transaction id is invalid." } });
    }
    const { data, error } = await request.supabase
      .from("transactions")
      .delete()
      .eq("id", parsedId.data)
      .eq("user_id", request.userId)
      .select("id")
      .maybeSingle();
    if (error) {
      request.log.error({ err: error }, "Could not delete transaction");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Transaction could not be deleted." } });
    }
    if (!data) return reply.code(404).send({ error: { code: "NOT_FOUND", message: "Transaction was not found." } });
    return reply.code(204).send();
  });
}
