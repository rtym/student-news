import { execSync } from "node:child_process";
import * as fs from "node:fs";
import * as path from "node:path";

const REPO_ROOT = path.join(__dirname, "..");
const FLAG_FILE = path.join(__dirname, ".e2e-docker-started");

export default async function globalTeardown(): Promise<void> {
  if (process.env.E2E_SKIP_DOCKER === "1") {
    return;
  }
  if (!fs.existsSync(FLAG_FILE)) {
    return;
  }
  try {
    execSync("docker compose down -v", { cwd: REPO_ROOT, stdio: "inherit" });
  } finally {
    fs.rmSync(FLAG_FILE, { force: true });
  }
}
