import { execSync } from "node:child_process";
import * as fs from "node:fs";
import * as path from "node:path";

const REPO_ROOT = path.join(__dirname, "..");
const FLAG_FILE = path.join(__dirname, ".e2e-docker-started");

const API_HEALTH_URL = process.env.E2E_API_HEALTH_URL ?? "http://localhost:8080/api/news";
const WEB_HEALTH_URL = process.env.E2E_WEB_HEALTH_URL ?? "http://localhost:5173/";

async function fetchWithTimeout(url: string, timeoutMs: number): Promise<Response> {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { signal: controller.signal });
  } finally {
    clearTimeout(id);
  }
}

async function waitForServices(): Promise<void> {
  const deadline = Date.now() + 240_000;
  let lastError: unknown;
  while (Date.now() < deadline) {
    try {
      const [apiRes, webRes] = await Promise.all([
        fetchWithTimeout(API_HEALTH_URL, 10_000),
        fetchWithTimeout(WEB_HEALTH_URL, 10_000)
      ]);
      if (apiRes.ok && webRes.ok) {
        return;
      }
      lastError = new Error(`API ${apiRes.status} / Web ${webRes.status}`);
    } catch (e) {
      lastError = e;
    }
    await new Promise((r) => setTimeout(r, 2000));
  }
  throw new Error(
    `Timed out waiting for services.\nAPI: ${API_HEALTH_URL}\nWeb: ${WEB_HEALTH_URL}\nLast error: ${lastError}`
  );
}

export default async function globalSetup(): Promise<void> {
  if (process.env.E2E_SKIP_DOCKER === "1") {
    await waitForServices();
    return;
  }

  execSync("docker compose up -d --build", { cwd: REPO_ROOT, stdio: "inherit" });
  fs.writeFileSync(FLAG_FILE, "1", "utf8");
  await waitForServices();
}
