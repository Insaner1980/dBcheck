import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const foregroundAudioServiceStart: MatcherPlugin = {
  slug: "foreground-audio-service-start",
  description:
    "Foreground microphone service start paths where the agent should verify foreground promotion happens before audio capture",
  noiseTier: "precise",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!content.includes("MeasurementForegroundService") && !content.includes("startForeground")) return [];

    return regexCandidates("foreground-audio-service-start", content, [
      {
        regex: /audioSessionManager\.startSession\s*\(/,
        label: "Audio session start from foreground service",
      },
      {
        regex: /ServiceCompat\.startForeground\s*\(/,
        label: "Foreground promotion call",
      },
    ]);
  },
};
