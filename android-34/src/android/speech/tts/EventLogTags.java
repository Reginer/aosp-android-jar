/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/core/java/android/speech/tts/EventLogTags.logtags
 */

package android.speech.tts;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 76001 tts_speak_success (engine|3),(caller_uid|1),(caller_pid|1),(length|1),(locale|3),(rate|1),(pitch|1),(engine_latency|2|3),(engine_total|2|3),(audio_latency|2|3) */
  public static final int TTS_SPEAK_SUCCESS = 76001;

  /** 76002 tts_speak_failure (engine|3),(caller_uid|1),(caller_pid|1),(length|1),(locale|3),(rate|1),(pitch|1) */
  public static final int TTS_SPEAK_FAILURE = 76002;

  /** 76003 tts_v2_speak_success (engine|3),(caller_uid|1),(caller_pid|1),(length|1),(request_config|3),(engine_latency|2|3),(engine_total|2|3),(audio_latency|2|3) */
  public static final int TTS_V2_SPEAK_SUCCESS = 76003;

  /** 76004 tts_v2_speak_failure (engine|3),(caller_uid|1),(caller_pid|1),(length|1),(request_config|3), (statusCode|1) */
  public static final int TTS_V2_SPEAK_FAILURE = 76004;

  public static void writeTtsSpeakSuccess(String engine, int callerUid, int callerPid, int length, String locale, int rate, int pitch, long engineLatency, long engineTotal, long audioLatency) {
    android.util.EventLog.writeEvent(TTS_SPEAK_SUCCESS, engine, callerUid, callerPid, length, locale, rate, pitch, engineLatency, engineTotal, audioLatency);
  }

  public static void writeTtsSpeakFailure(String engine, int callerUid, int callerPid, int length, String locale, int rate, int pitch) {
    android.util.EventLog.writeEvent(TTS_SPEAK_FAILURE, engine, callerUid, callerPid, length, locale, rate, pitch);
  }

  public static void writeTtsV2SpeakSuccess(String engine, int callerUid, int callerPid, int length, String requestConfig, long engineLatency, long engineTotal, long audioLatency) {
    android.util.EventLog.writeEvent(TTS_V2_SPEAK_SUCCESS, engine, callerUid, callerPid, length, requestConfig, engineLatency, engineTotal, audioLatency);
  }

  public static void writeTtsV2SpeakFailure(String engine, int callerUid, int callerPid, int length, String requestConfig, int statuscode) {
    android.util.EventLog.writeEvent(TTS_V2_SPEAK_FAILURE, engine, callerUid, callerPid, length, requestConfig, statuscode);
  }
}
