import path from "node:path";
import { fileURLToPath } from "node:url";
import config from "../vite.config.js";
import { build, createServer, preview } from "vite";

const currentFilePath = fileURLToPath(import.meta.url);
const frontendRoot = path.resolve(path.dirname(currentFilePath), "..");
const runtimeConfig = {
  ...config,
  configFile: false,
  root: frontendRoot
};

const command = process.argv[2] || "dev";

if (command === "dev") {
  const server = await createServer(runtimeConfig);
  await server.listen();
  server.printUrls();
} else if (command === "build") {
  await build(runtimeConfig);
} else if (command === "preview") {
  const previewServer = await preview(runtimeConfig);
  previewServer.printUrls();
} else {
  throw new Error(`Unsupported Vite command: ${command}`);
}
