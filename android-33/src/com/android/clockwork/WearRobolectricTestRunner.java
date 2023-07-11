package com.android.clockwork;

import java.io.File;
import java.io.IOException;
import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.internal.dependency.DependencyResolver;
import org.robolectric.internal.dependency.LocalDependencyResolver;
import org.robolectric.internal.dependency.PropertiesDependencyResolver;
import org.robolectric.res.Fs;

/**
 * Load targeted jar file to provide a simulation of the Android runtime environment.
 *
 * Wear core may bypass public APIs so when private APIs change they may break
 * the tests.
 *
 * Address the problem here by providing a replacement runtime environment with targetted
 * private API changes taken from newer code.
 *
 * 1. To examine under the hood a class within an SDK jar file:
 * e.g.
 * javap -cp android-all-ibluetooth-mod-8.0.0_r4-robolectric-r1.jar \
 *     com.android.internal.util.IndentingPrintWriter
 *
 * This command will show the framework methods in the class built for this version.
 *
 * 2. To update an SDK jar file with a more recent version due to private API changes do this:
 *
 *   a. Navigate to the directory in a recent build for this class.
 *   cd $ANDROID_BUILD_TOP/out/target/common/obj/JAVA_LIBRARIES/\
 *      robolectric_android-all-stub_intermediates/classes
 *
 *   b. Touch the desired class to ensure the zip utility will pick up the fact it is newer
 *   touch ./com/android/internal/util/IndentingPrintWriter.class
 *
 *   c. Update the target jar file
 *   export SDK_JAR_FILE=android-all-ibluetooth-mod-8.0.0_r4-robolectric-r1.jar
 *   zip -u $ANDROID_BUILD_TOP/vendor/unbundled_google/packages/ClockworkPrebuilts/\
 *          libs/robolectric/android-all/$SDK_JAR_FILE \
 *     ./com/android/internal/util/IndentingPrintWriter.class
 *
 * 3. Update the .patches file to indicate the classes that have been modified.
 */
public class WearRobolectricTestRunner extends RobolectricTestRunner {
  private transient DependencyResolver dependencyResolver;

  /**
   * Creates a runner to run {@code testClass}.
   *
   * @param testClass the test class to be run
   * @throws InitializationError if junit says so
   */
  public WearRobolectricTestRunner(final Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  @Override
  protected DependencyResolver getJarResolver() {
      if (Boolean.getBoolean("robolectric.offline")) {
        String propPath = System.getProperty("robolectric-deps.properties");
        if (propPath != null) {
          try {
            dependencyResolver = new PropertiesDependencyResolver(
                Fs.newFile(propPath),
                null);
          } catch (IOException e) {
            throw new RuntimeException("couldn't read dependencies" , e);
          }
        } else {
          String newdir
              = "vendor/unbundled_google/packages/ClockworkPrebuilts/libs/robolectric/android-all";
          String dependencyDir = System.getProperty("robolectric.dependency.dir", ".");
          dependencyResolver = new LocalDependencyResolver(new File(newdir));
        }
      }
    return dependencyResolver;
  }
}
