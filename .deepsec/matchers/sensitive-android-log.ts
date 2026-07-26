import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { candidate, findBalancedDelimiterEnd, isTestFile } from "./utils.js";

const sensitiveWords =
  "(?:session|sessionId|measurement|decibel|dbWeighted|dbcheck\\.db|audio|microphone|recording|sample|billing|purchase|token|backup|restore|health|heart|threshold|hearing|csv|pdf|export|uri|fileprovider)";
const sensitiveWordPattern = new RegExp(sensitiveWords, "i");
const logCallStart = /\b(?:Log|android\.util\.Log)\.(?:v|d|i|w|e|wtf)\s*\(/g;

export const sensitiveAndroidLog: MatcherPlugin = {
  slug: "sensitive-android-log",
  description:
    "Android log statements that may disclose audio measurements, health data, billing state, backup paths, or exported file URIs",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return [...content.matchAll(logCallStart)].flatMap((match) => {
      const start = match.index ?? 0;
      const openParenthesis = start + match[0].lastIndexOf("(");
      const end = findBalancedDelimiterEnd(content, openParenthesis, "(", ")");
      if (end === null || !sensitiveWordPattern.test(content.slice(openParenthesis + 1, end))) return [];

      return candidate("sensitive-android-log", content, start, "Sensitive term in Android log call");
    });
  },
};
