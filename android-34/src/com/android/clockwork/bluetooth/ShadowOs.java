package com.android.clockwork.bluetooth;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import java.io.FileDescriptor;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Enables reading an writing to shadow file descriptors in robolectric tests.
 *
 * <p>Should be used together with {@link ShadowParcelFileDescriptor}.
 */
@Implements(Os.class)
public class ShadowOs {
  @Implementation
  protected static int read(FileDescriptor fd, ByteBuffer byteBuffer) throws ErrnoException {
    try {
      byte[] bytes = new byte[byteBuffer.remaining()];
      int readCount = ShadowParcelFileDescriptor.findShadow(fd).getInputStream().read(bytes);
      if (readCount > 0) {
        byteBuffer.put(bytes, 0, readCount);
      }
      return readCount;
    } catch (IOException e) {
      throw new ErrnoException("read", OsConstants.EBADF, e);
    }
  }

  @Implementation
  protected static int write(FileDescriptor fd, ByteBuffer byteBuffer) throws ErrnoException {
    try {
      byte[] bytes = new byte[byteBuffer.remaining()];
      byteBuffer.get(bytes);
      OutputStream outputStream = ShadowParcelFileDescriptor.findShadow(fd).getOutputStream();
      outputStream.write(bytes);
      // Needed so that the other end of the pipe receives the partial data before buffer is full.
      ((Flushable) outputStream).flush();
      return bytes.length;
    } catch (IOException e) {
      throw new ErrnoException("write", OsConstants.EBADF, e);
    }
  }
}
