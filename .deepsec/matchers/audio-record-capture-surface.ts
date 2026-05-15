import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const audioRecordCaptureSurface: MatcherPlugin = {
  slug: "audio-record-capture-surface",
  description:
    "AudioRecord capture setup and start paths that need runtime permission, buffer, failure, and session-state review",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!/AudioRecord|startRecording|readRecordingChunk|createAudioRecord/.test(content)) return [];

    return regexCandidates("audio-record-capture-surface", content, [
      { regex: /\bAudioRecord\b/, label: "AudioRecord API use" },
      { regex: /\.startRecording\s*\(/, label: "AudioRecord.startRecording call" },
      { regex: /\breadRecordingChunk\s*\(/, label: "AudioRecord read loop boundary" },
      { regex: /\bcreateAudioRecord\s*\(/, label: "AudioRecord construction boundary" },
    ]);
  },
};
