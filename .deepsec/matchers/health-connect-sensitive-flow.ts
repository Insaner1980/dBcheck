import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const healthConnectSensitiveFlow: MatcherPlugin = {
  slug: "health-connect-sensitive-flow",
  description:
    "Health Connect read/write surfaces where permission scope, user gating, and sensitive health data handling need review",
  noiseTier: "normal",
  filePatterns: ["app/src/main/**/*.kt", "app/src/main/AndroidManifest.xml"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!/HealthConnect|health\.|HeartRate|ExerciseSession|WRITE_EXERCISE|READ_HEART_RATE/.test(content)) return [];

    return regexCandidates("health-connect-sensitive-flow", content, [
      { regex: /WRITE_EXERCISE|HealthPermission\.getWritePermission/, label: "Health Connect write permission" },
      { regex: /READ_HEART_RATE|HealthPermission\.getReadPermission/, label: "Health Connect heart-rate read permission" },
      { regex: /\bHealthConnect(?:Manager|Client|Service)\b/, label: "Health Connect integration boundary" },
      { regex: /\bHeartRateRecord\b|\breadHeartRateForSession\s*\(/, label: "Heart-rate read flow" },
      { regex: /\bExerciseSessionRecord\b|\bwriteNoiseDose\s*\(/, label: "Noise-dose write flow" },
    ]);
  },
};
