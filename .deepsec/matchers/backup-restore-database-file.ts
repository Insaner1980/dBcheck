import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const backupRestoreDatabaseFile: MatcherPlugin = {
  slug: "backup-restore-database-file",
  description:
    "Local database backup and restore flows that should validate backup files and avoid corrupt Room/WAL state",
  noiseTier: "normal",
  filePatterns: [
    "app/src/main/java/com/dbcheck/app/sync/**/*.kt",
    "app/src/main/java/com/dbcheck/app/service/BackupService.kt",
    "app/src/main/java/com/dbcheck/app/ui/settings/SettingsViewModel.kt",
  ],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!/backup|Backup|restore|Restore|dbcheck\.db|wal|shm/.test(content)) return [];

    return regexCandidates("backup-restore-database-file", content, [
      {
        regex: /\bfun\s+(?:createLocalBackup|restoreFromBackup|createBackup|validateBackup|listBackups|confirmRestoreBackup)\b/,
        label: "Backup or restore entry point",
      },
      { regex: /["']dbcheck\.db["']|\bdatabasePath\b/, label: "Local database backup file path" },
      { regex: /\brestoreFromBackup\s*\(/, label: "Restore operation boundary" },
      { regex: /\bcopyTo\s*\(|\bcopyRecursively\s*\(/, label: "Database file copy" },
      { regex: /\bdelete(?:Recursively)?\s*\(/, label: "Backup/restore delete operation" },
      { regex: /\bwal\b|\bshm\b/i, label: "Room WAL/SHM handling" },
    ]);
  },
};
