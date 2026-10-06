import Fastify from "fastify";
import cors from "@fastify/cors";
import { env } from "./env.js";
import { registerAuthRoutes } from "./routes/auth.js";
import { registerDashboardRoutes } from "./routes/dashboard.js";
import { registerTransactionRoutes } from "./routes/transactions.js";

const app = Fastify({ logger: true });

app.setErrorHandler((error, request, reply) => {
  request.log.error({ err: error }, "Unhandled API error");
  const candidateStatus = error && typeof error === "object" && "statusCode" in error
    ? error.statusCode
    : undefined;
  const statusCode = candidateStatus === 400 || candidateStatus === 413 ? candidateStatus : 500;
  const code = statusCode === 413 ? "PAYLOAD_TOO_LARGE" : statusCode === 400 ? "INVALID_REQUEST" : "INTERNAL_ERROR";
  const message = statusCode === 413
    ? "Request body is too large."
    : statusCode === 400
      ? "Request could not be parsed."
      : "An unexpected error occurred.";
  return reply.code(statusCode).send({ error: { code, message } });
});

await app.register(cors, {
  origin: env.corsOrigins,
  methods: ["GET", "POST", "PATCH", "DELETE", "OPTIONS"],
  allowedHeaders: ["authorization", "content-type"],
});

app.get("/health", async () => ({ status: "ok" }));
await app.register(registerAuthRoutes, { prefix: "/v1/auth" });
await app.register(registerDashboardRoutes, { prefix: "/v1/dashboard" });
await app.register(registerTransactionRoutes, { prefix: "/v1/transactions" });

try {
  await app.listen({ host: env.HOST, port: env.PORT });
} catch (error) {
  app.log.error(error);
  process.exitCode = 1;
}
