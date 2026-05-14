import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

const sensitiveWords =
  "(?:session|sessionId|measurement|decibel|dbWeighted|dbcheck\\.db|audio|microphone|recording|sample|billing|purchase|token|backup|restore|health|heart|threshold|hearing|csv|pdf|export|uri|fileprovider)";

export const sensitiveAndroidLog: MatcherPlugin = {
  slug: "sensitive-android-log",
  description:
    "Android log statements that may disclose audio measurements, health data, billing state, backup paths, or exported file URIs",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return regexCandidates("sensitive-android-log", content, [
      {
        regex: new RegExp(
          String.raw`\b(?:Log|android\.util\.Log)\.(?:v|d|i|w|e)\s*\([^;\n]*${sensitiveWords}[^;\n]*\)`,
          "i",
        ),
        label: "Sensitive term in Android log call",
      },
    ]);
  },
};
