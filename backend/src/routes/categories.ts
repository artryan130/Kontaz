import type { FastifyInstance } from "fastify";
import { z } from "zod";
import type { SupabaseClient } from "@supabase/supabase-js";
import { authenticate } from "../auth.js";
import type { Database } from "../database.types.js";

const categoryTypeSchema = z.enum(["income", "expense", "investment"]);
const categoryQuerySchema = z.object({ type: categoryTypeSchema.optional() });
const categoryCreateSchema = z.object({
  type: categoryTypeSchema,
  name: z.string().trim().min(1).max(50),
});

const builtInCategories = {
  income: ["Salário", "Freelance", "Investimentos", "Vendas", "Benefícios", "Outros"],
  expense: ["Alimentação", "Moradia", "Transporte", "Saúde", "Educação", "Lazer", "Contas", "Outros"],
  investment: ["Renda Variável", "Renda Fixa", "Fundos", "Criptomoedas", "Previdência", "Outros"],
} as const;

type CategoryType = keyof typeof builtInCategories;
type CategoryRecord = {
  id: string;
  type: CategoryType;
  name: string;
  is_custom: boolean;
};

export async function registerCategoryRoutes(app: FastifyInstance) {
  app.get("/", { preHandler: authenticate }, async (request, reply) => {
    const parsedQuery = categoryQuerySchema.safeParse(request.query);
    if (!parsedQuery.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Category type is invalid." } });
    }

    let query = request.supabase
      .from("transaction_categories")
      .select("id,type,name")
      .eq("user_id", request.userId)
      .order("name", { ascending: true });
    if (parsedQuery.data.type) query = query.eq("type", parsedQuery.data.type);
    const { data, error } = await query;
    if (error) {
      request.log.error({ err: error }, "Could not load transaction categories");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Categories could not be loaded." } });
    }

    const types = parsedQuery.data.type ? [parsedQuery.data.type] : Object.keys(builtInCategories) as CategoryType[];
    const builtIns: CategoryRecord[] = types.flatMap((type) =>
      builtInCategories[type].map((name) => ({
        id: `builtin:${type}:${normalizeCategoryName(name)}`,
        type,
        name,
        is_custom: false,
      }))
    );
    const custom = (data ?? []).map((category) => ({ ...category, is_custom: true }));
    const merged = [...builtIns, ...custom].filter((category, index, categories) =>
      categories.findIndex((candidate) =>
        candidate.type === category.type &&
        normalizeCategoryName(candidate.name) === normalizeCategoryName(category.name)
      ) === index
    );
    return reply.send({ items: merged });
  });

  app.post("/", { preHandler: authenticate }, async (request, reply) => {
    const parsed = categoryCreateSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: { code: "INVALID_INPUT", message: "Category data is invalid." } });
    }

    const { type } = parsed.data;
    const name = parsed.data.name.replace(/\s+/g, " ");
    const normalizedName = normalizeCategoryName(name);
    const existingBuiltIn = builtInCategories[type].find(
      (category) => normalizeCategoryName(category) === normalizedName
    );
    if (existingBuiltIn) {
      return reply.send({
        id: `builtin:${type}:${normalizedName}`,
        type,
        name: existingBuiltIn,
        is_custom: false,
      });
    }

    const { data: existing, error: lookupError } = await request.supabase
      .from("transaction_categories")
      .select("id,type,name")
      .eq("user_id", request.userId)
      .eq("type", type)
      .eq("normalized_name", normalizedName)
      .maybeSingle();
    if (lookupError) {
      request.log.error({ err: lookupError }, "Could not check for an existing transaction category");
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Category could not be checked." } });
    }
    if (existing) return reply.send({ ...existing, is_custom: true });

    const { data, error } = await request.supabase
      .from("transaction_categories")
      .insert({
        user_id: request.userId,
        type,
        name,
        normalized_name: normalizedName,
      })
      .select("id,type,name")
      .single();
    if (error) {
      if (error.code === "23505") {
        const { data: duplicate, error: duplicateError } = await request.supabase
          .from("transaction_categories")
          .select("id,type,name")
          .eq("user_id", request.userId)
          .eq("type", type)
          .eq("normalized_name", normalizedName)
          .single();
        if (!duplicateError && duplicate) return reply.send({ ...duplicate, is_custom: true });
        request.log.error({ err: duplicateError }, "Could not load the concurrently created transaction category");
      } else {
        request.log.error({ err: error }, "Could not create transaction category");
      }
      return reply.code(500).send({ error: { code: "DATABASE_ERROR", message: "Category could not be created." } });
    }
    return reply.code(201).send({ ...data, is_custom: true });
  });
}

export function normalizeCategoryName(name: string): string {
  return name
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .replace(/\s+/g, " ")
    .toLocaleLowerCase("pt-BR");
}

export async function canonicalizeCategory(
  supabase: SupabaseClient<Database>,
  userId: string,
  type: CategoryType,
  name: string
): Promise<{ name: string; error: Error | null }> {
  const normalizedName = normalizeCategoryName(name);
  const builtIn = builtInCategories[type].find(
    (category) => normalizeCategoryName(category) === normalizedName
  );
  if (builtIn) return { name: builtIn, error: null };

  const { data, error } = await supabase
    .from("transaction_categories")
    .select("name")
    .eq("user_id", userId)
    .eq("type", type)
    .eq("normalized_name", normalizedName)
    .maybeSingle();
  if (error) return { name, error };
  return { name: data?.name ?? name.trim().replace(/\s+/g, " "), error: null };
}
