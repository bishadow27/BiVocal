// Local Windows launcher. Uses existing builds; never runs migrations or account helpers.
import { spawn } from "node:child_process";
import { createHash } from "node:crypto";
import {
  appendFileSync,
  existsSync,
  mkdirSync,
  renameSync,
  statSync,
} from "node:fs";
import net from "node:net";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const directory = path.dirname(fileURLToPath(import.meta.url));
const root = path.dirname(directory);
const identity = createHash("sha256")
  .update(root.toLowerCase())
  .digest("hex")
  .slice(0, 16);
const pipe = `\\\\.\\pipe\\bipassion-admin-${identity}`;
const logDirectory = path.join(directory, ".runtime");
const logFile = path.join(logDirectory, "local-admin.log");

function log(message) {
  mkdirSync(logDirectory, { recursive: true });
  if (existsSync(logFile) && statSync(logFile).size > 2 * 1024 * 1024) {
    renameSync(logFile, `${logFile}.previous`);
  }
  appendFileSync(logFile, `${new Date().toISOString()} ${message}\n`);
}

function request(command) {
  return new Promise((resolve, reject) => {
    const socket = net.createConnection(pipe);
    let response = "";
    socket.setEncoding("utf8");
    socket.setTimeout(3000, () =>
      socket.destroy(new Error("Launcher did not respond")),
    );
    socket.once("connect", () => socket.write(`${command}\n`));
    socket.on("data", (data) => {
      response += data;
      if (response.length > 4096)
        socket.destroy(new Error("Invalid launcher response"));
    });
    socket.once("end", () => {
      try {
        resolve(JSON.parse(response));
      } catch {
        reject(new Error("Invalid launcher response"));
      }
    });
    socket.once("error", (error) => {
      if (["ENOENT", "ECONNREFUSED"].includes(error.code))
        resolve({ running: false });
      else reject(error);
    });
  });
}

function portIsOpen(port) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ host: "127.0.0.1", port });
    const finish = (value) => {
      socket.destroy();
      resolve(value);
    };
    socket.setTimeout(1500, () => finish(false));
    socket.once("connect", () => finish(true));
    socket.once("error", () => finish(false));
  });
}

async function run() {
  let previewServer;
  let backend;
  let backendMode = "waiting";
  let checking = false;
  let stopping = false;
  let timer;

  async function stop() {
    if (stopping) return;
    stopping = true;
    clearInterval(timer);
    control.close();
    // Only the child created by this launcher is stopped. Existing servers are untouched.
    backend?.kill();
    if (previewServer) {
      previewServer.httpServer.closeAllConnections();
      await new Promise((resolve) => previewServer.httpServer.close(resolve));
    }
    log("Launcher stopped.");
  }

  const control = net.createServer((socket) => {
    socket.setEncoding("utf8");
    socket.setTimeout(3000, () => socket.destroy());
    socket.on("error", () => {});
    let input = "";
    socket.on("data", (data) => {
      input += data;
      if (input.length > 64) return socket.destroy();
      if (!input.includes("\n")) return;
      const command = input.trim();
      if (command === "status") {
        socket.end(
          JSON.stringify({
            running: true,
            pid: process.pid,
            url: "http://localhost:5173",
            frontendReady: Boolean(previewServer),
            backendMode,
            backendPid: backend?.pid ?? null,
          }),
        );
      } else if (command === "stop") {
        socket.end(JSON.stringify({ stopping: true }));
        void stop();
      } else {
        socket.end(JSON.stringify({ error: "Unknown command" }));
      }
    });
  });

  try {
    await new Promise((resolve, reject) => {
      control.once("error", reject);
      control.listen(pipe, resolve);
    });
  } catch (error) {
    if (error.code === "EADDRINUSE") return; // One launcher per checkout.
    throw error;
  }

  async function ensureBackend() {
    if (stopping || checking || backend) return;
    checking = true;
    try {
      if (await portIsOpen(3000)) {
        if (backendMode !== "existing")
          log("Port 3000 already in use; leaving its process running.");
        backendMode = "existing";
        return;
      }
      if (stopping) return;
      const child = spawn(
        process.execPath,
        [path.join(root, "backend", "dist", "server.js")],
        {
          cwd: path.join(root, "backend"),
          env: { ...process.env, HOST: "127.0.0.1", PORT: "3000" },
          windowsHide: true,
          stdio: "ignore",
        },
      );
      backend = child;
      backendMode = "managed";
      child.once("error", (error) =>
        log(`Backend launch failed (${error.code ?? "UNKNOWN"}).`),
      );
      child.once("close", (code) => {
        backend = undefined;
        backendMode = "waiting";
        if (!stopping)
          log(`Backend exited (${code}); retrying in up to 10 seconds.`);
      });
      log(`Backend started (PID ${child.pid ?? "pending"}).`);
    } finally {
      checking = false;
    }
  }

  try {
    for (const file of [
      "backend/dist/server.js",
      "admin-web/dist/index.html",
    ]) {
      if (!existsSync(path.join(root, file))) throw new Error("BUILD_MISSING");
    }
    const { preview } = await import(
      pathToFileURL(
        path.join(root, "admin-web/node_modules/vite/dist/node/index.js"),
      ).href
    );
    previewServer = await preview({
      root: path.join(root, "admin-web"),
      logLevel: "silent",
      clearScreen: false,
      preview: { host: "127.0.0.1", port: 5173, strictPort: true, open: false },
    });
    if (stopping) {
      await new Promise((resolve) => previewServer.httpServer.close(resolve));
      return;
    }
    log("Admin ready at http://localhost:5173.");
    await ensureBackend();
    if (stopping) return;
    timer = setInterval(
      () =>
        void ensureBackend().catch(() =>
          log("Backend check failed; will retry."),
        ),
      10000,
    );
    process.once("SIGINT", () => void stop());
    process.once("SIGTERM", () => void stop());
  } catch (error) {
    log(
      `Startup failed (${error.code ?? (error.message === "BUILD_MISSING" ? "BUILD_MISSING" : "CHECK_BUILD_OR_PORT_5173")}).`,
    );
    await stop();
    process.exitCode = 1;
  }
}

const command = process.argv[2] ?? "--run";
try {
  if (process.platform !== "win32")
    throw new Error("This launcher requires Windows");
  if (command === "--run") await run();
  else if (command === "--stop" || command === "--status")
    console.log(JSON.stringify(await request(command.slice(2))));
  else throw new Error("Use --run, --status, or --stop");
} catch {
  log("Launcher failed. Check Node.js, built files, and local ports.");
  process.exitCode = 1;
}
