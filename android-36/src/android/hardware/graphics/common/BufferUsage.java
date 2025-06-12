/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 37aa15ac89ae27f3f89099d79609f5aaa1717de5 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/graphics/common/aidl/android.hardware.graphics.common-V3-java-source/gen/android/hardware/graphics/common/BufferUsage.java.d -o out/soong/.intermediates/hardware/interfaces/graphics/common/aidl/android.hardware.graphics.common-V3-java-source/gen -Nhardware/interfaces/graphics/common/aidl/aidl_api/android.hardware.graphics.common/3 hardware/interfaces/graphics/common/aidl/aidl_api/android.hardware.graphics.common/3/android/hardware/graphics/common/BufferUsage.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.graphics.common;
/** @hide */
public @interface BufferUsage {
  public static final long CPU_READ_MASK = 15L;
  public static final long CPU_READ_NEVER = 0L;
  public static final long CPU_READ_RARELY = 2L;
  public static final long CPU_READ_OFTEN = 3L;
  public static final long CPU_WRITE_MASK = 240L;
  public static final long CPU_WRITE_NEVER = 0L;
  public static final long CPU_WRITE_RARELY = 32L;
  public static final long CPU_WRITE_OFTEN = 48L;
  public static final long GPU_TEXTURE = 256L;
  public static final long GPU_RENDER_TARGET = 512L;
  public static final long COMPOSER_OVERLAY = 2048L;
  public static final long COMPOSER_CLIENT_TARGET = 4096L;
  public static final long PROTECTED = 16384L;
  public static final long COMPOSER_CURSOR = 32768L;
  public static final long VIDEO_ENCODER = 65536L;
  public static final long CAMERA_OUTPUT = 131072L;
  public static final long CAMERA_INPUT = 262144L;
  public static final long RENDERSCRIPT = 1048576L;
  public static final long VIDEO_DECODER = 4194304L;
  public static final long SENSOR_DIRECT_DATA = 8388608L;
  public static final long GPU_DATA_BUFFER = 16777216L;
  public static final long GPU_CUBE_MAP = 33554432L;
  public static final long GPU_MIPMAP_COMPLETE = 67108864L;
  public static final long HW_IMAGE_ENCODER = 134217728L;
  public static final long FRONT_BUFFER = 4294967296L;
  public static final long VENDOR_MASK = 4026531840L;
  public static final long VENDOR_MASK_HI = -281474976710656L;
}
