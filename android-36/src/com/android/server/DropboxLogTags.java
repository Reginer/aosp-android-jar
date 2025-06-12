/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/core/java/com/android/server/DropboxLogTags.logtags
 */

package com.android.server;

/**
 * @hide
 */
public class DropboxLogTags {
  private DropboxLogTags() { }  // don't instantiate

  /** 81002 dropbox_file_copy (FileName|3),(Size|1),(Tag|3) */
  public static final int DROPBOX_FILE_COPY = 81002;

  public static void writeDropboxFileCopy(String filename, int size, String tag) {
    android.util.EventLog.writeEvent(DROPBOX_FILE_COPY, filename, size, tag);
  }
}
