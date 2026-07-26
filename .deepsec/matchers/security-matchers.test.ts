import assert from "node:assert/strict";
import { test } from "node:test";
import { androidExportedComponent } from "./android-exported-component.js";
import { androidUriShareWithoutClipData } from "./android-uri-share-without-clipdata.js";
import { fileproviderBroadPath } from "./fileprovider-broad-path.js";
import { foregroundAudioServiceStart } from "./foreground-audio-service-start.js";
import { healthConnectSensitiveFlow } from "./health-connect-sensitive-flow.js";
import { sensitiveAndroidLog } from "./sensitive-android-log.js";

test("health matcher gates on permission helpers and noise-dose writes", () => {
  const writePermission = healthConnectSensitiveFlow.match(
    "HealthPermission.getWritePermission(ExerciseSessionRecord::class)",
    "app/src/main/java/com/dbcheck/app/HealthPermissions.kt",
  );
  const noiseDose = healthConnectSensitiveFlow.match(
    "suspend fun sync() = writeNoiseDose(report)",
    "app/src/main/java/com/dbcheck/app/HealthSync.kt",
  );

  assert.ok(writePermission.some((match) => match.matchedPattern === "Health Connect write permission"));
  assert.ok(noiseDose.some((match) => match.matchedPattern === "Noise-dose write flow"));
});

test("sensitive log matcher covers multiline calls", () => {
  const matches = sensitiveAndroidLog.match(
    `Log.i(
      TAG,
      "Exported session URI: $uri",
    )`,
    "app/src/main/java/com/dbcheck/app/ExportLogger.kt",
  );

  assert.equal(matches.length, 1);
});

test("sensitive log matcher covers Log.wtf calls", () => {
  const matches = sensitiveAndroidLog.match(
    'Log.wtf(TAG, "Health export URI: $uri")',
    "app/src/main/java/com/dbcheck/app/ExportLogger.kt",
  );

  assert.equal(matches.length, 1);
});

test("sensitive log matcher does not consume a later Kotlin statement", () => {
  const matches = sensitiveAndroidLog.match(
    `Log.i(TAG, "sync complete")
val exportedUri = uri`,
    "app/src/main/java/com/dbcheck/app/HealthSync.kt",
  );

  assert.deepEqual(matches, []);
});

test("sensitive log matcher ignores closing parentheses inside Kotlin comments", () => {
  const matches = sensitiveAndroidLog.match(
    `Log.i(
      TAG, // )
      "Exported session URI: $uri",
    )
Log.w(
  TAG, /* ) */
  "Backup file URI: $uri",
)`,
    "app/src/main/java/com/dbcheck/app/ExportLogger.kt",
  );

  assert.equal(matches.length, 2);
});

test("foreground matcher covers direct and ServiceCompat promotion", () => {
  const direct = foregroundAudioServiceStart.match(
    "class MeasurementForegroundService { fun promote() = startForeground(ID, notification) }",
    "app/src/main/java/com/dbcheck/app/MeasurementForegroundService.kt",
  );
  const compatible = foregroundAudioServiceStart.match(
    "class MeasurementForegroundService { fun promote() = ServiceCompat.startForeground(this, ID, notification, 0) }",
    "app/src/main/java/com/dbcheck/app/MeasurementForegroundService.kt",
  );

  assert.equal(direct.length, 1);
  assert.equal(compatible.length, 1);
});

test("FileProvider matcher covers single-quoted broad paths", () => {
  const matches = fileproviderBroadPath.match(
    "<paths><cache-path name='cache' path='.'/></paths>",
    "app/src/main/res/xml/file_paths.xml",
  );

  assert.equal(matches.length, 1);
});

test("exported component matcher covers single-quoted attributes", () => {
  const matches = androidExportedComponent.match(
    "<manifest><application><service android:name='.SyncService' android:exported='true'/></application></manifest>",
    "app/src/main/AndroidManifest.xml",
  );

  assert.equal(matches.length, 1);
});

test("URI share matcher evaluates each share construction independently", () => {
  const content = `
fun safe(uri: Uri): Intent =
  Intent(Intent.ACTION_SEND).apply {
    putExtra(Intent.EXTRA_STREAM, uri)
    clipData = ClipData.newUri(resolver, "safe", uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }

fun unsafe(uri: Uri): Intent =
  Intent(Intent.ACTION_SEND).apply {
    putExtra(Intent.EXTRA_STREAM, uri)
  }
`;

  const matches = androidUriShareWithoutClipData.match(
    content,
    "app/src/main/java/com/dbcheck/app/ShareFactory.kt",
  );

  assert.equal(matches.length, 1);
  assert.equal(
    matches[0]?.matchedPattern,
    "EXTRA_STREAM content URI share without FLAG_GRANT_READ_URI_PERMISSION",
  );
});

test("URI share matcher reports a missing ClipData in an otherwise granted scope", () => {
  const matches = androidUriShareWithoutClipData.match(
    `fun share(uri: Uri) = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
      putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri))
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }`,
    "app/src/main/java/com/dbcheck/app/ShareFactory.kt",
  );

  assert.equal(matches.length, 1);
  assert.equal(matches[0]?.matchedPattern, "EXTRA_STREAM content URI share without ClipData");
});

test("URI share matcher bounds an unsafe share to its builder block", () => {
  const matches = androidUriShareWithoutClipData.match(
    `fun share(uri: Uri): Intent {
      val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
      }
      val unrelatedClipData = ClipData.newPlainText("preview", "text")
      val unrelatedFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
      return intent
    }`,
    "app/src/main/java/com/dbcheck/app/ShareFactory.kt",
  );

  assert.equal(matches.length, 1);
  assert.equal(
    matches[0]?.matchedPattern,
    "EXTRA_STREAM content URI share without FLAG_GRANT_READ_URI_PERMISSION",
  );
});

test("URI share matcher continues from a nested constructor to its owning function", () => {
  const matches = androidUriShareWithoutClipData.match(
    `fun share(uri: Uri): Intent {
      val intent = run {
        Intent(Intent.ACTION_SEND)
      }
      intent.putExtra(Intent.EXTRA_STREAM, uri)
      return intent
    }`,
    "app/src/main/java/com/dbcheck/app/ShareFactory.kt",
  );

  assert.equal(matches.length, 1);
  assert.equal(
    matches[0]?.matchedPattern,
    "EXTRA_STREAM content URI share without FLAG_GRANT_READ_URI_PERMISSION",
  );
});

test("URI share matcher recognizes constructor trivia before the opening parenthesis", () => {
  const matches = androidUriShareWithoutClipData.match(
    `fun share(uri: Uri): Intent {
      val intent = Intent /* share constructor */ (
        Intent.ACTION_SEND
      ).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
      }
      val unrelatedClipData = ClipData.newPlainText("preview", "text")
      val unrelatedFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
      return intent
    }`,
    "app/src/main/java/com/dbcheck/app/ShareFactory.kt",
  );

  assert.equal(matches.length, 1);
  assert.equal(
    matches[0]?.matchedPattern,
    "EXTRA_STREAM content URI share without FLAG_GRANT_READ_URI_PERMISSION",
  );
});
