import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { type DeepsecPlugin, defineConfig } from "deepsec/config";
import { androidExportedComponent } from "./matchers/android-exported-component.js";
import { androidUriShareWithoutClipData } from "./matchers/android-uri-share-without-clipdata.js";
import { audioRecordCaptureSurface } from "./matchers/audio-record-capture-surface.js";
import { backupRestoreDatabaseFile } from "./matchers/backup-restore-database-file.js";
import { fileproviderBroadPath } from "./matchers/fileprovider-broad-path.js";
import { foregroundAudioServiceStart } from "./matchers/foreground-audio-service-start.js";
import { healthConnectSensitiveFlow } from "./matchers/health-connect-sensitive-flow.js";
import { sensitiveAndroidLog } from "./matchers/sensitive-android-log.js";

const here = path.dirname(fileURLToPath(import.meta.url));

function dbCheckPlugin(): DeepsecPlugin {
  return {
    name: "dbcheck-android",
    matchers: [
      androidExportedComponent,
      fileproviderBroadPath,
      androidUriShareWithoutClipData,
      foregroundAudioServiceStart,
      audioRecordCaptureSurface,
      healthConnectSensitiveFlow,
      backupRestoreDatabaseFile,
      sensitiveAndroidLog,
    ],
  };
}

export default defineConfig({
  projects: [
    {
      id: "dbcheck",
      root: "..",
      infoMarkdown: fs.readFileSync(path.join(here, "data", "dbcheck", "INFO.md"), "utf-8"),
      promptAppend:
        "Prioritize microphone foreground-service ordering, AudioRecord failure handling, Health Connect permission scope, FileProvider exports, local backup/restore, billing state, and sensitive logging.",
      priorityPaths: [
        "app/src/main/AndroidManifest.xml",
        "app/src/main/java/com/dbcheck/app/service/",
        "app/src/main/java/com/dbcheck/app/domain/audio/",
        "app/src/main/java/com/dbcheck/app/sync/",
        "app/src/main/java/com/dbcheck/app/data/export/",
        "app/src/main/java/com/dbcheck/app/billing/",
      ],
    },
  ],
  plugins: [dbCheckPlugin()],
});
