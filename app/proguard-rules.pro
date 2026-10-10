# dBcheck ProGuard rules

# MediaPipe's generated messages use field-name reflection in the Protobuf Lite runtime.
# Keep only message fields; the remaining MediaPipe code can still be optimized.
-keepclassmembers class com.google.mediapipe.** extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# MediaPipe graph parsing reflectively creates Any and reads its message fields.
-keep class com.google.protobuf.Any {
    <fields>;
}

# Native MediaPipe output invokes this interface method, including generated lambdas.
-keep interface com.google.mediapipe.framework.PacketListCallback {
    public void process(java.util.List);
}

# Native output creates Java packets through this static factory.
-keepclassmembers class com.google.mediapipe.framework.Packet {
    public static com.google.mediapipe.framework.Packet create(long);
}

# Flogger's stack scan requires both the logger factory and caller-finder frames.
-keepclassmembers,allowobfuscation class com.google.common.flogger.FluentLogger {
    public static com.google.common.flogger.FluentLogger forEnclosingClass();
}
-keepclassmembers,allowobfuscation class com.google.common.flogger.backend.system.StackBasedCallerFinder {
    public java.lang.String findLoggingClass(java.lang.Class);
}
