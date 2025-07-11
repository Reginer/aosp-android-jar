/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.jcajce.provider.symmetric.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class ClassUtil
{
    public static Class loadClass(Class sourceClass, final String className)
    {
        try
        {
            ClassLoader loader = sourceClass.getClassLoader();

            if (loader != null)
            {
                return loader.loadClass(className);
            }
            else
            {
                return (Class)AccessController.doPrivileged(new PrivilegedAction()
                {
                    public Object run()
                    {
                        try
                        {
                            ClassLoader classLoader = ClassLoader.getSystemClassLoader();

                            return classLoader.loadClass(className);
                        }
                        catch (Exception e)
                        {
                            // ignore - maybe log?
                        }

                        return null;
                    }
                });
            }
        }
        catch (ClassNotFoundException e)
        {
            // ignore - maybe log?
        }

        return null;
    }
}
