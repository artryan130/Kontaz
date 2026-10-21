import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { authenticate } from "../auth.js";
import { canonicalizeCategory } from "./categories.js";

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
const transactionSeriesSchema = transactionSchema.extend({
  is_recurring: z.boolean(),
  end_date: dateSchema.nullable().optional().default(null),
  is_installment: z.boolean(),
  installment_count: z.number().int().min(2).max(600).nullable().optional().default(null),
}).superRefine((value, context) => {
  if (value.type !== "expense") {
    context.addIssue({ code: "custom", message: "Only expenses can be recurring or split into installments." });
  }
  if (value.is_recurring === value.is_installment) {
    context.addIssue({ code: "custom", message: "Choose exactly one transaction series type." });
  }
  if (value.is_recurring) {
    if (!value.end_date || value.end_date < value.date) {
      context.addIssue({ code: "custom", message: "A recurring expense needs an end date on or after its start date." });
    }
    if (value.installment_count !== null) {
      context.addIssue({ code: "custom", message: "Recurring expenses cannot include an installment count." });
    }
  }
  if (value.is_installment) {
    if (!value.installment_count || value.end_date !== null) {
      context.addIssue({ code: "custom", message: "Installments need a count and cannot include a recurring end date." });
    } else if (Math.round(value.amount * 100) < value.installment_count) {
      context.addIssue({ code: "custom", message: "The total must cover at least one cent per installment." });
    }
  }
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
  app.post("/series", { preHandler: authenticate }, async (request, reply) => {
    const parsed = transactionSeriesSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Transaction series data is invalid." } });
    }

    const transaction = parsed.data;
    const canonicalCategory = await canonicalizeCategory(
      request.supabase,
      request.userId,
      transaction.type,
      transaction.category
    );
    if (canonicalCategory.error) {
      request.log.error({ err: canonicalCategory.error }, "Could not resolve transaction series category");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Category could not be resolved." } });
    }
    const dates = transaction.is_recurring
      ? monthlyDates(transaction.date, transaction.end_date!)
      : monthlyDatesByCount(transaction.date, transaction.installment_count!);
    if (!dates) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "A transaction series cannot exceed 600 monthly entries." } });
    }

    const totalCents = Math.round(transaction.amount * 100);
    const baseInstallmentCents = transaction.is_installment
      ? Math.floor(totalCents / dates.length)
      : totalCents;
    const extraCents = transaction.is_installment ? totalCents % dates.length : 0;
    const rows = dates.map((date, index) => ({
      amount: (baseInstallmentCents + (index < extraCents ? 1 : 0)) / 100,
      type: transaction.type,
      category: canonicalCategory.name,
      description: transaction.description ?? null,
      date,
      user_id: request.userId,
      is_recurring: transaction.is_recurring,
      end_date: transaction.is_recurring ? transaction.end_date : null,
      is_installment: transaction.is_installment,
      installment_count: transaction.is_installment ? dates.length : null,
      installment_number: transaction.is_installment ? index + 1 : null,
    }));

    const { data, error } = await request.supabase
      .from("transactions")
      .insert(rows)
      .select("*");
    if (error) {
      request.log.error({ err: error }, "Could not create transaction series");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Transaction series could not be created." } });
    }
    return reply.code(201).send({ items: data, count: data.length });
  });

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
    const canonicalCategory = await canonicalizeCategory(
      request.supabase,
      request.userId,
      parsed.data.type,
      parsed.data.category
    );
    if (canonicalCategory.error) {
      request.log.error({ err: canonicalCategory.error }, "Could not resolve transaction category");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Category could not be resolved." } });
    }
    const { data, error } = await request.supabase
      .from("transactions")
      .insert({ ...parsed.data, category: canonicalCategory.name, user_id: request.userId })
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
    let updateData = parsedBody.data;
    if (updateData.category) {
      let categoryType = updateData.type;
      if (!categoryType) {
        const { data: existingTransaction, error: lookupError } = await request.supabase
          .from("transactions")
          .select("type")
          .eq("id", parsedId.data)
          .eq("user_id", request.userId)
          .maybeSingle();
        if (lookupError) {
          request.log.error({ err: lookupError }, "Could not load transaction type for category normalization");
          return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Transaction could not be loaded." } });
        }
        categoryType = existingTransaction?.type;
      }
      if (!categoryType) {
        return reply.code(404).send({ error: { code: "NOT_FOUND", message: "Transaction was not found." } });
      }
      const canonicalCategory = await canonicalizeCategory(
        request.supabase,
        request.userId,
        categoryType,
        updateData.category
      );
      if (canonicalCategory.error) {
        request.log.error({ err: canonicalCategory.error }, "Could not resolve updated transaction category");
        return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Category could not be resolved." } });
      }
      updateData = { ...updateData, category: canonicalCategory.name };
    }
    const { data, error } = await request.supabase
      .from("transactions")
      .update(updateData)
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

function monthlyDates(start: string, end: string): string[] | null {
  const [startYear, startMonth, startDay] = start.split("-").map(Number);
  const [endYear, endMonth] = end.split("-").map(Number);
  const monthDifference = (endYear - startYear) * 12 + endMonth - startMonth;
  if (monthDifference < 0 || monthDifference >= 600) return null;

  return Array.from({ length: monthDifference + 1 }, (_, index) => {
    const monthIndex = startMonth - 1 + index;
    const year = startYear + Math.floor(monthIndex / 12);
    const month = (monthIndex % 12) + 1;
    const lastDay = new Date(Date.UTC(year, month, 0)).getUTCDate();
    const day = Math.min(startDay, lastDay);
    return `${year.toString().padStart(4, "0")}-${month.toString().padStart(2, "0")}-${day.toString().padStart(2, "0")}`;
  }).filter((date) => date <= end);
}

function monthlyDatesByCount(start: string, count: number): string[] {
  const [startYear, startMonth, startDay] = start.split("-").map(Number);
  return Array.from({ length: count }, (_, index) => {
    const monthIndex = startMonth - 1 + index;
    const year = startYear + Math.floor(monthIndex / 12);
    const month = (monthIndex % 12) + 1;
    const lastDay = new Date(Date.UTC(year, month, 0)).getUTCDate();
    const day = Math.min(startDay, lastDay);
    return `${year.toString().padStart(4, "0")}-${month.toString().padStart(2, "0")}-${day.toString().padStart(2, "0")}`;
  });
}
