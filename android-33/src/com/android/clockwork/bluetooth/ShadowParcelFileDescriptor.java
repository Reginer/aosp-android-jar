package com.android.clockwork.bluetooth;


import android.os.ParcelFileDescriptor;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadow.api.Shadow;

/**
 * Enables using ParcelFileDescriptor.createSocketPair in robolectric tests.
 *
 * <p>Uses in-memory input and output streams for the descriptor.
 */
@Implements(ParcelFileDescriptor.class)
public class ShadowParcelFileDescriptor {
  private @RealObject ParcelFileDescriptor realObject;
  private PipedOutputStream outputStream;
  private PipedInputStream inputStream;

  /** Returns the stream that is used to write data to this descriptor. */
  public OutputStream getOutputStream() {
    return outputStream;
  }

  /** Returns the stream that is used to read data from this descriptor. */
  public InputStream getInputStream() {
    return inputStream;
  }

  @Implementation
  protected static ParcelFileDescriptor[] createSocketPair(int option) throws IOException {
    ShadowParcelFileDescriptor fd1 = create();
    ShadowParcelFileDescriptor fd2 = create();
    fd1.inputStream = new PipedInputStream(fd2.outputStream);
    fd2.inputStream = new PipedInputStream(fd1.outputStream);
    return new ParcelFileDescriptor[] {fd1.realObject, fd2.realObject};
  }

  private static ShadowParcelFileDescriptor create() {
    ParcelFileDescriptor fd = new ParcelFileDescriptor(new FileDescriptor());
    ShadowParcelFileDescriptor shadow = Shadow.extract(fd);
    shadow.outputStream = new PipedOutputStream();
    shadows.add(shadow);
    return shadow;
  }

  @Implementation
  protected ParcelFileDescriptor dup() {
    // Does not really duplicate the descriptor but this is enough for the tests.
    return realObject;
  }

  @Implementation
  protected void close() throws IOException {
    if (inputStream != null) {
      inputStream.close();
    }
    if (outputStream != null) {
      outputStream.close();
    }
  }

  @Resetter
  public static void resetShadows() {
    shadows.clear();
  }

  private static final List<ShadowParcelFileDescriptor> shadows = new ArrayList<>();

  /**
   * Returns the shadow that wraps the given file descriptor or null if the shadow is not found.
   *
   * <p>NOTE: Only works if the descriptor was created with {@link #create} function.
   */
  static ShadowParcelFileDescriptor findShadow(FileDescriptor fd) {
    for (ShadowParcelFileDescriptor shadow : shadows) {
      if (shadow.realObject.getFileDescriptor() == fd) {
        return shadow;
      }
    }
    return null;
  }
}
