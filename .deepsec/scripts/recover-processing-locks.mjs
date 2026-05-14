import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

export function recoverProcessingLocks({
  dataDir,
  projectId,
  staleMinutes = 60,
  forceRunIds = [],
  dryRun = false,
  now = new Date(),
}) {
  if (!dataDir) {
    throw new Error("dataDir is required");
  }
  if (!projectId) {
    throw new Error("projectId is required");
  }

  const projectDir = path.join(dataDir, projectId);
  const filesDir = path.join(projectDir, "files");
  const runsDir = path.join(projectDir, "runs");
  const forced = new Set(forceRunIds);
  const staleMs = staleMinutes * 60 * 1000;
  const recovered = [];
  const remaining = [];

  for (const file of listJsonFiles(filesDir)) {
    const record = readJson(file);
    if (record.status !== "processing") {
      continue;
    }

    const reason = recoverReason({
      record,
      runsDir,
      forced,
      staleMs,
      now,
    });
    if (!reason) {
      continue;
    }

    recovered.push({
      filePath: record.filePath,
      lockedByRunId: record.lockedByRunId,
      reason,
    });

    if (!dryRun) {
      record.status = "pending";
      delete record.lockedByRunId;
      delete record.lockedAt;
      writeJson(file, record);
    }
  }

  for (const file of listJsonFiles(filesDir)) {
    const record = readJson(file);
    if (record.status !== "processing") {
      continue;
    }

    remaining.push({
      filePath: record.filePath,
      lockedByRunId: record.lockedByRunId,
      lockedAt: record.lockedAt,
    });
  }

  if (!dryRun) {
    markRecoveredRuns({ runsDir, recovered, now });
  }

  return { recovered, remaining };
}

function recoverReason({ record, runsDir, forced, staleMs, now }) {
  if (!record.lockedByRunId) {
    return "missing-lock-owner";
  }
  if (forced.has(record.lockedByRunId)) {
    return "forced-run";
  }

  const runFile = path.join(runsDir, `${record.lockedByRunId}.json`);
  if (!fs.existsSync(runFile)) {
    return "missing-run";
  }

  const run = readJson(runFile);
  if (run.phase === "done" || run.phase === "error") {
    return `owner-${run.phase}`;
  }
  if (!record.lockedAt) {
    return "missing-locked-at";
  }

  const lockedAt = new Date(record.lockedAt);
  if (Number.isNaN(lockedAt.getTime())) {
    return "invalid-locked-at";
  }

  return now.getTime() - lockedAt.getTime() >= staleMs ? "stale-lock" : null;
}

function markRecoveredRuns({ runsDir, recovered, now }) {
  const recoveredRunIds = [
    ...new Set(recovered.map((entry) => entry.lockedByRunId).filter(Boolean)),
  ];

  for (const runId of recoveredRunIds) {
    const runFile = path.join(runsDir, `${runId}.json`);
    if (!fs.existsSync(runFile)) {
      continue;
    }

    const run = readJson(runFile);
    if (run.type !== "process" || run.phase !== "running") {
      continue;
    }

    run.phase = "error";
    run.completedAt = now.toISOString();
    run.stats = {
      ...(run.stats ?? {}),
      recoveredProcessingLocks: recovered.filter((entry) => entry.lockedByRunId === runId).length,
    };
    writeJson(runFile, run);
  }
}

function listJsonFiles(root) {
  if (!fs.existsSync(root)) {
    return [];
  }

  const files = [];
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const fullPath = path.join(root, entry.name);
    if (entry.isDirectory()) {
      files.push(...listJsonFiles(fullPath));
    } else if (entry.isFile() && entry.name.endsWith(".json")) {
      files.push(fullPath);
    }
  }
  return files;
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

function writeJson(file, value) {
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

function parseArgs(argv) {
  const args = {
    dataDir: path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "data"),
    projectId: "dbcheck",
    staleMinutes: 60,
    forceRunIds: [],
    dryRun: false,
    failOnActive: false,
  };

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--") {
      continue;
    } else if (arg === "--data-dir") {
      args.dataDir = path.resolve(argv[++i]);
    } else if (arg === "--project-id") {
      args.projectId = argv[++i];
    } else if (arg === "--stale-minutes") {
      args.staleMinutes = Number(argv[++i]);
    } else if (arg === "--force-run-id") {
      args.forceRunIds.push(...argv[++i].split(",").filter(Boolean));
    } else if (arg === "--dry-run") {
      args.dryRun = true;
    } else if (arg === "--fail-on-active") {
      args.failOnActive = true;
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }

  if (!Number.isFinite(args.staleMinutes) || args.staleMinutes < 0) {
    throw new Error("--stale-minutes must be a non-negative number");
  }

  return args;
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  const result = recoverProcessingLocks(args);
  const prefix = args.dryRun ? "Would recover" : "Recovered";
  console.log(`${prefix} ${result.recovered.length} processing file lock(s).`);
  for (const entry of result.recovered) {
    console.log(`- ${entry.filePath} (${entry.reason}, run ${entry.lockedByRunId ?? "none"})`);
  }

  if (result.remaining.length > 0) {
    const activeRunIds = [...new Set(result.remaining.map((entry) => entry.lockedByRunId).filter(Boolean))];
    console.error(`Still ${result.remaining.length} active processing file lock(s).`);
    for (const entry of result.remaining) {
      console.error(`- ${entry.filePath} (run ${entry.lockedByRunId ?? "none"}, lockedAt ${entry.lockedAt ?? "unknown"})`);
    }
    if (activeRunIds.length > 0) {
      console.error("If no deepsec process is still running, recover manually:");
      for (const runId of activeRunIds) {
        console.error(`  pnpm deepsec:recover -- --force-run-id ${runId}`);
      }
    }
    if (args.failOnActive) {
      process.exitCode = 1;
    }
  }
}

const currentFile = fileURLToPath(import.meta.url);
if (process.argv[1] && path.resolve(process.argv[1]) === currentFile) {
  main();
}
