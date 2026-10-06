import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { authenticate } from "../auth.js";

const periodSchema = z.string().regex(/^\d{4}-(0[1-9]|1[0-2])$/);

export async function registerDashboardRoutes(app: FastifyInstance) {
  app.get("/", { preHandler: authenticate }, async (request, reply) => {
    const parsed = z.object({ period: periodSchema }).safeParse(request.query);
    if (!parsed.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Period must use YYYY-MM format." } });
    }
    const [year, month] = parsed.data.period.split("-").map(Number);
    const from = `${parsed.data.period}-01`;
    const to = new Date(Date.UTC(year, month, 0)).toISOString().slice(0, 10);

    const { data, error } = await request.supabase
      .from("transactions")
      .select("amount,type")
      .eq("user_id", request.userId)
      .gte("date", from)
      .lte("date", to);
    if (error) {
      request.log.error({ err: error }, "Could not load dashboard totals");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Dashboard data could not be loaded." } });
    }

    const totalsInCents = { income: 0, expenses: 0, investments: 0 };
    for (const transaction of data) {
      const amount = Number(transaction.amount);
      if (!Number.isFinite(amount)) {
        request.log.error({ transactionType: transaction.type }, "Invalid transaction amount in database");
        return reply.code(500).send({ error: { code: "DATA_ERROR", message: "Dashboard data could not be calculated." } });
      }
      const cents = Math.round(amount * 100);
      if (transaction.type === "income") totalsInCents.income += cents;
      else if (transaction.type === "expense") totalsInCents.expenses += cents;
      else if (transaction.type === "investment") totalsInCents.investments += cents;
    }
    const income = totalsInCents.income / 100;
    const expenses = totalsInCents.expenses / 100;
    const investments = totalsInCents.investments / 100;
    return reply.send({
      period: parsed.data.period,
      income,
      expenses,
      investments,
      balance: (totalsInCents.income - totalsInCents.expenses - totalsInCents.investments) / 100,
    });
  });
}
