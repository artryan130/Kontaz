import "dotenv/config";
import { z } from "zod";

const envSchema = z.object({
  HOST: z.string().default("0.0.0.0"),
  PORT: z.coerce.number().int().positive().default(3000),
  SUPABASE_URL: z.string().url(),
  SUPABASE_ANON_KEY: z.string().min(1),
  SUPABASE_REDIRECT_URL: z.string().default("kontaz://auth/recovery"),
  CORS_ORIGINS: z.string().default("http://localhost:5173,http://10.0.2.2:3000"),
});

const parsed = envSchema.safeParse(process.env);
if (!parsed.success) {
  throw new Error(`Invalid backend environment: ${parsed.error.issues.map((issue) => issue.path.join(".")).join(", ")}`);
}

export const env = {
  ...parsed.data,
  corsOrigins: parsed.data.CORS_ORIGINS.split(",").map((origin) => origin.trim()).filter(Boolean),
};
