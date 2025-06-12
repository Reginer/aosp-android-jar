/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package android.filterfw.core;

import android.filterfw.core.Filter;
import android.util.Log;

import dalvik.system.PathClassLoader;

import java.lang.reflect.Constructor;
import java.lang.ClassLoader;
import java.lang.Thread;
import java.util.HashSet;

/**
 * @hide
 */
public class FilterFactory {

    private static FilterFactory mSharedFactory;
    private HashSet<String> mPackages = new HashSet<String>();

    private static ClassLoader mCurrentClassLoader;
    private static HashSet<String> mLibraries;
    private static Object mClassLoaderGuard;

    static {
        mCurrentClassLoader = Thread.currentThread().getContextClassLoader();
        mLibraries = new HashSet<String>();
        mClassLoaderGuard = new Object();
    }

    private static final String TAG = "FilterFactory";
    private static boolean mLogVerbose = Log.isLoggable(TAG, Log.VERBOSE);

    public static FilterFactory sharedFactory() {
        if (mSharedFactory == null) {
            mSharedFactory = new FilterFactory();
        }
        return mSharedFactory;
    }

    /**
     * Adds a new Java library to the list to be scanned for filters.
     * libraryPath must be an absolute path of the jar file.  This needs to be
     * static because only one classloader per process can open a shared native
     * library, which a filter may well have.
     */
    public static void addFilterLibrary(String libraryPath) {
        if (mLogVerbose) Log.v(TAG, "Adding filter library " + libraryPath);
        synchronized(mClassLoaderGuard) {
            if (mLibraries.contains(libraryPath)) {
                if (mLogVerbose) Log.v(TAG, "Library already added");
                return;
            }
            mLibraries.add(libraryPath);
            // Chain another path loader to the current chain
            mCurrentClassLoader = new PathClassLoader(libraryPath, mCurrentClassLoader);
        }
    }

    public void addPackage(String packageName) {
        if (mLogVerbose) Log.v(TAG, "Adding package " + packageName);
        /* TODO: This should use a getPackage call in the caller's context, but no such method exists.
        Package pkg = Package.getPackage(packageName);
        if (pkg == null) {
            throw new IllegalArgumentException("Unknown filter package '" + packageName + "'!");
        }
        */
        mPackages.add(packageName);
    }

    public Filter createFilterByClassName(String className, String filterName) {
        if (mLogVerbose) Log.v(TAG, "Looking up class " + className);
        Class filterClass = null;

        // Look for the class in the imported packages
        for (String packageName : mPackages) {
            try {
                if (mLogVerbose) Log.v(TAG, "Trying "+packageName + "." + className);
                synchronized(mClassLoaderGuard) {
                    filterClass = mCurrentClassLoader.loadClass(packageName + "." + className);
                }
            } catch (ClassNotFoundException e) {
                continue;
            }
            // Exit loop if class was found.
            if (filterClass != null) {
                break;
            }
        }
        if (filterClass == null) {
            throw new IllegalArgumentException("Unknown filter class '" + className + "'!");
        }
        return createFilterByClass(filterClass, filterName);
    }

    public Filter createFilterByClass(Class filterClass, String filterName) {
        // Make sure this is a Filter subclass
        if (!Filter.class.isAssignableFrom(filterClass)) {
            throw new IllegalArgumentException("Attempting to allocate class '" + filterClass
                + "' which is not a subclass of Filter!");
        }

        // Look for the correct constructor
        Constructor filterConstructor = null;
        try {
            filterConstructor = filterClass.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The filter class '" + filterClass
                + "' does not have a constructor of the form <init>(String name)!");
        }

        // Construct the filter
        Filter filter = null;
        try {
            filter = (Filter)filterConstructor.newInstance(filterName);
        } catch (Throwable t) {
            // Condition checked below
        }

        if (filter == null) {
            throw new IllegalArgumentException("Could not construct the filter '"
                + filterName + "'!");
        }
        return filter;
    }
}
