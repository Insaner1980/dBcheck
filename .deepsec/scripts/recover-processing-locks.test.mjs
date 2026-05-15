import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { recoverProcessingLocks } from "./recover-processing-locks.mjs";

test("does not recover a fresh processing lock without force", () => {
  const fixture = createFixture();
  writeRun(fixture, "run-fresh", { phase: "running" });
  writeRecord(fixture, "app/Fresh.kt", {
    status: "processing",
    lockedByRunId: "run-fresh",
    lockedAt: "2026-05-14T07:00:00.000Z",
  });

  const result = recoverProcessingLocks({
    dataDir: fixture.dataDir,
    projectId: fixture.projectId,
    now: new Date("2026-05-14T07:30:00.000Z"),
  });

  assert.equal(result.recovered.length, 0);
  assert.deepEqual(result.remaining.map((entry) => entry.filePath), ["app/Fresh.kt"]);
  assert.equal(readRecord(fixture, "app/Fresh.kt").status, "processing");
});

test("recovers and marks a forced processing run as error", () => {
  const fixture = createFixture();
  writeRun(fixture, "run-orphaned", { phase: "running" });
  writeRecord(fixture, "app/Locked.kt", {
    status: "processing",
    lockedByRunId: "run-orphaned",
    lockedAt: "2026-05-14T07:00:00.000Z",
  });

  const result = recoverProcessingLocks({
    dataDir: fixture.dataDir,
    projectId: fixture.projectId,
    forceRunIds: ["run-orphaned"],
    now: new Date("2026-05-14T07:30:00.000Z"),
  });

  assert.deepEqual(result.recovered.map((entry) => entry.reason), ["forced-run"]);
  assert.deepEqual(result.remaining, []);
  const record = readRecord(fixture, "app/Locked.kt");
  assert.equal(record.status, "pending");
  assert.equal(record.lockedByRunId, undefined);
  assert.equal(readRun(fixture, "run-orphaned").phase, "error");
});

test("recovers locks owned by completed process runs", () => {
  const fixture = createFixture();
  writeRun(fixture, "run-done", { phase: "done" });
  writeRecord(fixture, "app/Done.kt", {
    status: "processing",
    lockedByRunId: "run-done",
    lockedAt: "2026-05-14T07:00:00.000Z",
  });

  const result = recoverProcessingLocks({
    dataDir: fixture.dataDir,
    projectId: fixture.projectId,
    now: new Date("2026-05-14T07:30:00.000Z"),
  });

  assert.deepEqual(result.recovered.map((entry) => entry.reason), ["owner-done"]);
  assert.deepEqual(result.remaining, []);
  assert.equal(readRecord(fixture, "app/Done.kt").status, "pending");
});

test("reports multiple unrecovered processing locks by owner run", () => {
  const fixture = createFixture();
  writeRun(fixture, "run-active", { phase: "running" });
  writeRecord(fixture, "app/One.kt", {
    status: "processing",
    lockedByRunId: "run-active",
    lockedAt: "2026-05-14T07:20:00.000Z",
  });
  writeRecord(fixture, "app/Two.kt", {
    status: "processing",
    lockedByRunId: "run-active",
    lockedAt: "2026-05-14T07:25:00.000Z",
  });

  const result = recoverProcessingLocks({
    dataDir: fixture.dataDir,
    projectId: fixture.projectId,
    now: new Date("2026-05-14T07:30:00.000Z"),
  });

  assert.equal(result.recovered.length, 0);
  assert.deepEqual(
    result.remaining.map((entry) => [entry.filePath, entry.lockedByRunId]),
    [
      ["app/One.kt", "run-active"],
      ["app/Two.kt", "run-active"],
    ],
  );
});

test("cli accepts pnpm argument separator before flags", () => {
  const fixture = createFixture();
  const scriptPath = path.join(path.dirname(fileURLToPath(import.meta.url)), "recover-processing-locks.mjs");

  const output = execFileSync(
    process.execPath,
    [
      scriptPath,
      "--data-dir",
      fixture.dataDir,
      "--project-id",
      fixture.projectId,
      "--",
      "--dry-run",
    ],
    { encoding: "utf8" },
  );

  assert.match(output, /Would recover 0 processing file lock\(s\)\./);
});

function createFixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "deepsec-locks-"));
  const projectId = "dbcheck";
  const dataDir = path.join(root, "data");
  fs.mkdirSync(path.join(dataDir, projectId, "files", "app"), { recursive: true });
  fs.mkdirSync(path.join(dataDir, projectId, "runs"), { recursive: true });
  return { dataDir, projectId };
}

function writeRun(fixture, runId, overrides) {
  writeJson(path.join(fixture.dataDir, fixture.projectId, "runs", `${runId}.json`), {
    runId,
    projectId: fixture.projectId,
    type: "process",
    phase: "running",
    stats: {},
    ...overrides,
  });
}

function readRun(fixture, runId) {
  return readJson(path.join(fixture.dataDir, fixture.projectId, "runs", `${runId}.json`));
}

function writeRecord(fixture, filePath, overrides) {
  writeJson(path.join(fixture.dataDir, fixture.projectId, "files", `${filePath}.json`), {
    filePath,
    projectId: fixture.projectId,
    candidates: [],
    findings: [],
    analysisHistory: [],
    ...overrides,
  });
}

function readRecord(fixture, filePath) {
  return readJson(path.join(fixture.dataDir, fixture.projectId, "files", `${filePath}.json`));
}

function writeJson(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}
