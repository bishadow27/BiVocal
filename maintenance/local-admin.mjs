// Local Windows launcher. Uses existing builds; never runs migrations or account helpers.
import { spawn, execFile } from "node:child_process";
import { createHash, randomUUID } from "node:crypto";
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

const controlChannel = 'bivoca-local-control';
const requestIdPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const shutdownAckTimeoutMs = 3000;
const childExitTimeoutMs = 15000;
const backendStartupTimeoutMs = 15000;

export function requestBackendGracefulShutdown(child, options = {}) {
  return new Promise((resolve, reject) => {
    const requestId = options.requestId ?? randomUUID();
    if (typeof requestId !== 'string' || !requestIdPattern.test(requestId)) return reject(new Error('INVALID_REQUEST_ID'));
    if (!child?.connected || typeof child.send !== 'function') return reject(new Error('BACKEND_IPC_UNAVAILABLE'));
    let accepted = false, completed = false, settled = false, exitTimer;
    const cleanup = () => {
      clearTimeout(ackTimer); clearTimeout(exitTimer);
      child.off('message', onMessage); child.off('exit', onExit); child.off('error', onError);
    };
    const fail = code => { if (!settled) { settled = true; cleanup(); reject(new Error(code)); } };
    const onMessage = message => {
      if (!message || message.channel !== controlChannel || message.version !== 1 || message.requestId !== requestId) return;
      if (message.type === 'shutdown-error') return fail('BACKEND_SHUTDOWN_ERROR');
      if (message.type === 'shutdown-accepted' && !accepted) {
        accepted = true; clearTimeout(ackTimer);
        exitTimer = setTimeout(() => fail('BACKEND_EXIT_TIMEOUT'), options.exitTimeoutMs ?? childExitTimeoutMs);
      } else if (message.type === 'shutdown-complete' && accepted) completed = true;
    };
    const onExit = (code, signal) => {
      if (code !== 0 || signal) return fail('BACKEND_EXIT_NOT_ZERO');
      if (!accepted || !completed) return fail('BACKEND_SHUTDOWN_RECEIPT_MISSING');
      settled = true; cleanup(); resolve({ requestId, accepted, completed, exitCode: code });
    };
    const onError = () => fail('BACKEND_IPC_ERROR');
    const ackTimer = setTimeout(() => fail('SHUTDOWN_ACK_TIMEOUT'), options.ackTimeoutMs ?? shutdownAckTimeoutMs);
    child.on('message', onMessage); child.once('exit', onExit); child.once('error', onError);
    try {
      child.send({ channel: controlChannel, version: 1, type: 'shutdown', requestId }, error => {
        if (error) fail('BACKEND_IPC_SEND_FAILED');
      });
    } catch { fail('BACKEND_IPC_SEND_FAILED'); }
  });
}

export function waitForBackendReady(child, options = {}) {
  return new Promise((resolve, reject) => {
    if (!child?.connected) return reject(new Error('BACKEND_IPC_UNAVAILABLE'));
    let settled = false;
    const cleanup = () => {
      clearTimeout(timer); child.off('message', onMessage); child.off('exit', onExit); child.off('error', onError);
    };
    const fail = code => { if (!settled) { settled = true; cleanup(); reject(new Error(code)); } };
    const onMessage = message => {
      if (!message || message.channel !== controlChannel || message.version !== 1 || message.type !== 'backend-ready'
        || message.pid !== child.pid || !Number.isInteger(message.port)) return;
      settled = true; cleanup(); resolve(message);
    };
    const onExit = () => fail('BACKEND_EXIT_BEFORE_READY');
    const onError = () => fail('BACKEND_STARTUP_ERROR');
    const timer = setTimeout(() => fail('BACKEND_STARTUP_TIMEOUT'), options.startupTimeoutMs ?? backendStartupTimeoutMs);
    child.on('message', onMessage); child.once('exit', onExit); child.once('error', onError);
  });
}

export async function startBackendAndWait(options) {
  const child = options.spawnBackend();
  const ready = await waitForBackendReady(child, options);
  if (!await options.verifyListener(child, ready)) throw new Error('BACKEND_LISTENER_MISMATCH');
  return { pid: child.pid };
}

export function createBackendRestarter(options) {
  let busy = false, blocked = false;
  return {
    get busy() { return busy; },
    get blocked() { return blocked; },
    block() { blocked = true; },
    async restart() {
      if (busy) throw new Error('BACKEND_RESTART_IN_PROGRESS');
      if (blocked) throw new Error('BACKEND_RESTART_BLOCKED');
      busy = true;
      try {
        const oldChild = options.getChild();
        await requestBackendGracefulShutdown(oldChild, options);
        if (await options.isPortOpen()) throw new Error('BACKEND_PORT_STILL_OPEN');
        const ready = await startBackendAndWait(options);
        return { restarted: true, pid: ready.pid };
      } catch (error) {
        blocked = true;
        throw error;
      } finally { busy = false; }
    },
  };
}

function verifyBackendListener(child, ready) {
  if (!Number.isSafeInteger(child.pid) || ready.pid !== child.pid || ready.port !== 3000) return Promise.resolve(false);
  return new Promise(resolve => {
    const script = `$ErrorActionPreference='Stop'; $listeners=@(Get-NetTCPConnection -State Listen -LocalPort 3000); if($listeners.Count -gt 0 -and @($listeners|Where-Object {$_.OwningProcess -ne ${child.pid}}).Count -eq 0){'yes'}else{'no'}`;
    execFile('powershell.exe', ['-NoLogo', '-NoProfile', '-NonInteractive', '-Command', script],
      { windowsHide: true, timeout: 5000 }, (error, stdout) => resolve(!error && stdout.trim() === 'yes'));
  });
}

function request(command) {
  return new Promise((resolve, reject) => {
    const socket = net.createConnection(pipe);
    let response = "";
    socket.setEncoding("utf8");
    socket.setTimeout(command === "restart-backend" ? 45000 : 3000, () =>
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

async function run(backendOnly = false) {
  let previewServer;
  let backend;
  let backendMode = "waiting";
  let backendReadyPid = null;
  let checking = false;
  let stopping = false;
  let timer;

  const restarter = createBackendRestarter({
    getChild: () => backend,
    spawnBackend: () => spawnManagedBackend(),
    isPortOpen: () => portIsOpen(3000),
    verifyListener: verifyBackendListener,
  });

  async function stop() {
    if (stopping) return;
    if (restarter.busy) { log("Stop refused: backend restart in progress."); return; }
    stopping = true;
    // Keep Admin and control alive if a managed child cannot stop gracefully.
    try {
      if (backend) await requestBackendGracefulShutdown(backend);
    } catch {
      restarter.block();
      backendMode = "restart-blocked";
      stopping = false;
      log("Graceful backend stop failed; Admin left running. No force-kill attempted.");
      return;
    }
    clearInterval(timer);
    control.close();
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
    let input = "", dispatched = false;
    socket.on("data", (data) => {
      if (dispatched) return;
      input += data;
      if (input.length > 64) return socket.destroy();
      if (!input.includes("\n")) return;
      dispatched = true;
      const command = input.trim();
      if (command === "status") {
        socket.end(
          JSON.stringify({
            running: true,
            pid: process.pid,
            url: backendOnly ? "http://127.0.0.1:3000/api/v1/health" : "http://localhost:5173",
            backendOnly,
            frontendReady: Boolean(previewServer),
            backendMode,
            backendPid: backend?.pid ?? null,
            backendReadyPid,
            backendIpcConnected: Boolean(backend?.connected),
            backendRestarting: restarter.busy,
            backendRestartBlocked: restarter.blocked,
          }),
        );
      } else if (command === "restart-backend") {
        socket.setTimeout(45000, () => socket.destroy());
        if (stopping) { socket.end(JSON.stringify({ error: "LAUNCHER_STOPPING" })); return; }
        void restarter.restart().then(result => {
          backendMode = "managed";
          backendReadyPid = result.pid;
          log(`Backend gracefully restarted (PID ${result.pid}).`);
          if (!socket.destroyed) socket.end(JSON.stringify(result));
        }).catch(error => {
          if (restarter.blocked) backendMode = "restart-blocked";
          const code = /^(BACKEND_|SHUTDOWN_)[A-Z_]+$/.test(error.message) ? error.message : "BACKEND_RESTART_FAILED";
          log(`Backend restart refused or failed (${code}); no force-kill or retry.`);
          if (!socket.destroyed) socket.end(JSON.stringify({ error: code }));
        });
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

  function spawnManagedBackend() {
      const child = spawn(
        process.execPath,
        [path.join(root, "backend", "dist", "server.js")],
        {
          cwd: path.join(root, "backend"),
          env: backendOnly ? { ...process.env } : { ...process.env, HOST: "127.0.0.1", PORT: "3000" },
          windowsHide: true,
          stdio: ["ignore", "ignore", "ignore", "ipc"],
        },
      );
      backend = child;
      backendReadyPid = null;
      backendMode = "starting";
      child.once("error", (error) =>
        log(`Backend launch failed (${error.code ?? "UNKNOWN"}).`),
      );
      child.once("close", (code) => {
        if (backend !== child) return;
        backend = undefined;
        backendReadyPid = null;
        backendMode = restarter.blocked ? "restart-blocked" : "waiting";
        if (!stopping && !restarter.busy && !restarter.blocked)
          log(`Backend exited (${code}); retrying in up to 10 seconds.`);
      });
      log(`Backend started (PID ${child.pid ?? "pending"}).`);
      return child;
  }

  async function ensureBackend() {
    if (stopping || checking || backend || restarter.busy || restarter.blocked) return;
    checking = true;
    try {
      if (await portIsOpen(3000)) {
        if (backendMode !== "existing")
          log("Port 3000 already in use; leaving its process running.");
        backendMode = "existing";
        return;
      }
      if (stopping || restarter.busy || restarter.blocked || backend) return;
      try {
        const ready = await startBackendAndWait({
          spawnBackend: () => spawnManagedBackend(),
          verifyListener: verifyBackendListener,
        });
        backendReadyPid = ready.pid;
        backendMode = "managed";
        log(`Backend ready via IPC (PID ${ready.pid}).`);
      } catch (error) {
        restarter.block();
        backendMode = "startup-blocked";
        const code = /^(BACKEND_|SHUTDOWN_)[A-Z_]+$/.test(error.message) ? error.message : "BACKEND_STARTUP_FAILED";
        log(`Backend initial startup blocked (${code}); no retry or force-kill.`);
      }
    } finally {
      checking = false;
    }
  }

  try {
    if (backendOnly) {
      if (!existsSync(path.join(root, "backend/dist/server.js"))) throw new Error("BUILD_MISSING");
      log("Backend-only launcher ready.");
    } else {
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
    }
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

const directEntrypoint = process.argv[1] ? path.resolve(process.argv[1]) : "";
const moduleEntrypoint = fileURLToPath(import.meta.url);
if (directEntrypoint && (process.platform === "win32"
  ? directEntrypoint.toLowerCase() === moduleEntrypoint.toLowerCase() : directEntrypoint === moduleEntrypoint)) {
const command = process.argv[2] ?? "--run";
try {
  if (process.platform !== "win32")
    throw new Error("This launcher requires Windows");
  if (command === "--run" || command === "--backend-only") await run(command === "--backend-only");
  else if (command === "--stop" || command === "--status" || command === "--restart-backend")
    console.log(JSON.stringify(await request(command.slice(2))));
  else throw new Error("Use --run, --backend-only, --status, --stop, or --restart-backend");
} catch {
  log("Launcher failed. Check Node.js, built files, and local ports.");
  process.exitCode = 1;
}

}
