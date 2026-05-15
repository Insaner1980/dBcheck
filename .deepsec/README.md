# dBcheck Deepsec

This is a separate Deepsec workspace for the dBcheck Android app.

Deepsec does not replace OWASP Dependency-Check. Use Dependency-Check for CVE/SCA results, and use Deepsec for agent-assisted review of project-specific Android security flows.

## One-command runs

From `C:\Dev\dBcheck\.deepsec`:

```powershell
pnpm install
pnpm deepsec:report
```

`pnpm deepsec:report` runs all Deepsec built-in matchers plus dBcheck custom matchers, recovers abandoned processing locks, processes the candidates, and exports Markdown findings.

If a previous `process` run was interrupted, `status` can show files stuck in `processing`. The report script automatically recovers stale or completed-run locks before processing. If it finds locks that still look active, it stops instead of exporting a misleading empty report and prints the manual recovery command:

```powershell
pnpm deepsec:recover -- --dry-run
pnpm deepsec:recover -- --force-run-id <run-id>
```

For only dBcheck custom matchers:

```powershell
pnpm deepsec:report:custom
```

For scan-only checks:

```powershell
pnpm deepsec:scan
pnpm deepsec:scan:custom
```

Generated runtime data such as `.deepsec/data/dbcheck/project.json`, `.deepsec/data/dbcheck/tech.json`, `.deepsec/data/dbcheck/files`, `.deepsec/data/dbcheck/runs`, `.deepsec/data/dbcheck/reports`, and `.deepsec/findings` is ignored. `.deepsec/data/dbcheck/config.json` is versioned so generated reports and build output stay outside Deepsec scans.
