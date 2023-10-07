/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

import java.util.Objects;

/**
 * An element in a stack trace, as returned by {@link
 * Throwable#getStackTrace()}.  Each element represents a single stack frame.
 * All stack frames except for the one at the top of the stack represent
 * a method invocation.  The frame at the top of the stack represents the
 * execution point at which the stack trace was generated.  Typically,
 * this is the point at which the throwable corresponding to the stack trace
 * was created.
 *
 * @since  1.4
 * @author Josh Bloch
 */
public final class StackTraceElement implements java.io.Serializable {

    // For Throwables and StackWalker, the VM initially sets this field to a
    // reference to the declaring Class.  The Class reference is used to
    // construct the 'format' bitmap, and then is cleared.
    //
    // For STEs constructed using the public constructors, this field is not used.
    // Android-removed: Remove classloader name and unsupported module name and version.
    // private transient Class<?> declaringClassObject;

    // Normally initialized by VM
    // Android-removed: Remove classloader name and unsupported module name and version.
    // private String classLoaderName;
    // private String moduleName;
    // private String moduleVersion;
    private String declaringClass;
    private String methodName;
    private String fileName;
    private int    lineNumber;
    // Android-removed: Remove classloader name and unsupported module name and version.
    // private byte   format = 0; // Default to show all


    // Android-changed: Remove javadoc related the unsupported module name and version.
    /**
     * Creates a stack trace element representing the specified execution
     * point.
     *
     * @param declaringClass the fully qualified name of the class containing
     *        the execution point represented by the stack trace element
     * @param methodName the name of the method containing the execution point
     *        represented by the stack trace element
     * @param fileName the name of the file containing the execution point
     *         represented by the stack trace element, or {@code null} if
     *         this information is unavailable
     * @param lineNumber the line number of the source line containing the
     *         execution point represented by this stack trace element, or
     *         a negative number if this information is unavailable. A value
     *         of -2 indicates that the method containing the execution point
     *         is a native method
     * @throws NullPointerException if {@code declaringClass} or
     *         {@code methodName} is null
     * @since 1.5
     * @revised 9
     * @spec JPMS
     */
    public StackTraceElement(String declaringClass, String methodName,
                             String fileName, int lineNumber) {
        // Android-changed: Avoid dependency on module-related codes.
        // this(null, null, null, declaringClass, methodName, fileName, lineNumber);
        this.declaringClass  = Objects.requireNonNull(declaringClass, "Declaring class is null");
        this.methodName      = Objects.requireNonNull(methodName, "Method name is null");
        this.fileName        = fileName;
        this.lineNumber      = lineNumber;
    }

    // BEGIN Android-removed: Remove module-related methods.
    /*
    /**
     * Creates a stack trace element representing the specified execution
     * point.
     *
     * @param classLoaderName the class loader name if the class loader of
     *        the class containing the execution point represented by
     *        the stack trace is named; otherwise {@code null}
     * @param moduleName the module name if the class containing the
     *        execution point represented by the stack trace is in a named
     *        module; otherwise {@code null}
     * @param moduleVersion the module version if the class containing the
     *        execution point represented by the stack trace is in a named
     *        module that has a version; otherwise {@code null}
     * @param declaringClass the fully qualified name of the class containing
     *        the execution point represented by the stack trace element
     * @param methodName the name of the method containing the execution point
     *        represented by the stack trace element
     * @param fileName the name of the file containing the execution point
     *        represented by the stack trace element, or {@code null} if
     *        this information is unavailable
     * @param lineNumber the line number of the source line containing the
     *        execution point represented by this stack trace element, or
     *        a negative number if this information is unavailable. A value
     *        of -2 indicates that the method containing the execution point
     *        is a native method
     *
     * @throws NullPointerException if {@code declaringClass} is {@code null}
     *         or {@code methodName} is {@code null}
     *
     * @since 9
     * @spec JPMS
     *
    public StackTraceElement(String classLoaderName,
                             String moduleName, String moduleVersion,
                             String declaringClass, String methodName,
                             String fileName, int lineNumber) {
        this.classLoaderName = classLoaderName;
        this.moduleName      = moduleName;
        this.moduleVersion   = moduleVersion;
        this.declaringClass  = Objects.requireNonNull(declaringClass, "Declaring class is null");
        this.methodName      = Objects.requireNonNull(methodName, "Method name is null");
        this.fileName        = fileName;
        this.lineNumber      = lineNumber;
    }
    */
    // END Android-removed: Remove module-related methods.

    /*
     * Private constructor for the factory methods to create StackTraceElement
     * for Throwable and StackFrameInfo
     */
    private StackTraceElement() {}

    /**
     * Returns the name of the source file containing the execution point
     * represented by this stack trace element.  Generally, this corresponds
     * to the {@code SourceFile} attribute of the relevant {@code class}
     * file (as per <i>The Java Virtual Machine Specification</i>, Section
     * 4.7.7).  In some systems, the name may refer to some source code unit
     * other than a file, such as an entry in source repository.
     *
     * @return the name of the file containing the execution point
     *         represented by this stack trace element, or {@code null} if
     *         this information is unavailable.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the line number of the source line containing the execution
     * point represented by this stack trace element.  Generally, this is
     * derived from the {@code LineNumberTable} attribute of the relevant
     * {@code class} file (as per <i>The Java Virtual Machine
     * Specification</i>, Section 4.7.8).
     *
     * @return the line number of the source line containing the execution
     *         point represented by this stack trace element, or a negative
     *         number if this information is unavailable.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    // BEGIN Android-removed: Remove classloader name and module-related methods.
    /*
    /**
     * Returns the module name of the module containing the execution point
     * represented by this stack trace element.
     *
     * @return the module name of the {@code Module} containing the execution
     *         point represented by this stack trace element; {@code null}
     *         if the module name is not available.
     * @since 9
     * @spec JPMS
     * @see Module#getName()
     *
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Returns the module version of the module containing the execution point
     * represented by this stack trace element.
     *
     * @return the module version of the {@code Module} containing the execution
     *         point represented by this stack trace element; {@code null}
     *         if the module version is not available.
     * @since 9
     * @spec JPMS
     * @see java.lang.module.ModuleDescriptor.Version
     *
    public String getModuleVersion() {
        return moduleVersion;
    }

    /**
     * Returns the name of the class loader of the class containing the
     * execution point represented by this stack trace element.
     *
     * @return the name of the class loader of the class containing the execution
     *         point represented by this stack trace element; {@code null}
     *         if the class loader is not named.
     *
     * @since 9
     * @spec JPMS
     * @see java.lang.ClassLoader#getName()
     *
    public String getClassLoaderName() {
        return classLoaderName;
    }
    */
    // END Android-removed: Remove classloader name and module-related methods.

    /**
     * Returns the fully qualified name of the class containing the
     * execution point represented by this stack trace element.
     *
     * @return the fully qualified name of the {@code Class} containing
     *         the execution point represented by this stack trace element.
     */
    public String getClassName() {
        return declaringClass;
    }

    /**
     * Returns the name of the method containing the execution point
     * represented by this stack trace element.  If the execution point is
     * contained in an instance or class initializer, this method will return
     * the appropriate <i>special method name</i>, {@code <init>} or
     * {@code <clinit>}, as per Section 3.9 of <i>The Java Virtual
     * Machine Specification</i>.
     *
     * @return the name of the method containing the execution point
     *         represented by this stack trace element.
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns true if the method containing the execution point
     * represented by this stack trace element is a native method.
     *
     * @return {@code true} if the method containing the execution point
     *         represented by this stack trace element is a native method.
     */
    public boolean isNativeMethod() {
        return lineNumber == -2;
    }

    // Android-changed: Remove javadoc related to the module system.
    /**
     * Returns a string representation of this stack trace element.
     *
     * @apiNote The format of this string depends on the implementation, but the
     * following examples may be regarded as typical:
     * <ul>
     * <li>
     *   {@code "MyClass.mash(MyClass.java:9)"} - Here, {@code "MyClass"}
     *   is the <i>fully-qualified name</i> of the class containing the
     *   execution point represented by this stack trace element,
     *   {@code "mash"} is the name of the method containing the execution
     *   point, {@code "MyClass.java"} is the source file containing the
     *   execution point, and {@code "9"} is the line number of the source
     *   line containing the execution point.
     * <li>
     *   {@code "MyClass.mash(MyClass.java)"} - As above, but the line
     *   number is unavailable.
     * <li>
     *   {@code "MyClass.mash(Unknown Source)"} - As above, but neither
     *   the file name nor the line  number are available.
     * <li>
     *   {@code "MyClass.mash(Native Method)"} - As above, but neither
     *   the file name nor the line  number are available, and the method
     *   containing the execution point is known to be a native method.
     * </ul>
     *
     * <p> The {@code toString} method may return two different values on two
     * {@code StackTraceElement} instances that are
     * {@linkplain #equals(Object) equal}, for example one created via the
     * constructor, and one obtained from {@link java.lang.Throwable} or
     * {@link java.lang.StackWalker.StackFrame}, where an implementation may
     * choose to omit some element in the returned string.
     *
     * @revised 9
     * @spec JPMS
     * @see    Throwable#printStackTrace()
     */
    public String toString() {
        // BEGIN Android-changed: Fall back Unknown Source:<dex_pc> for unknown lineNumber.
        // http://b/30183883
        // The only behavior change is that "Unknown Source" is followed by a number
        // (the value of the dex program counter, dex_pc), which never occurs on the
        // RI. This value isn't a line number, but can be useful for debugging and
        // avoids the need to ship line number information along with the dex code to
        // get an accurate stack trace.
        // Formatting it in this way might be more digestible to automated tools that
        // are not specifically written to expect this behavior.
        /*
        String s = "";
        if (!dropClassLoaderName() && classLoaderName != null &&
                !classLoaderName.isEmpty()) {
            s += classLoaderName + "/";
        }
        if (moduleName != null && !moduleName.isEmpty()) {
            s += moduleName;

            if (!dropModuleVersion() && moduleVersion != null &&
                    !moduleVersion.isEmpty()) {
                s += "@" + moduleVersion;
            }
        }
        s = s.isEmpty() ? declaringClass : s + "/" + declaringClass;

        return s + "." + methodName + "(" +
             (isNativeMethod() ? "Native Method)" :
              (fileName != null && lineNumber >= 0 ?
               fileName + ":" + lineNumber + ")" :
                (fileName != null ?  ""+fileName+")" : "Unknown Source)")));
        */
        StringBuilder result = new StringBuilder();
        result.append(getClassName()).append(".").append(methodName);
        if (isNativeMethod()) {
            result.append("(Native Method)");
        } else if (fileName != null) {
            if (lineNumber >= 0) {
                result.append("(").append(fileName).append(":").append(lineNumber).append(")");
            } else {
                result.append("(").append(fileName).append(")");
            }
        } else {
            if (lineNumber >= 0) {
                // The line number is actually the dex pc.
                result.append("(Unknown Source:").append(lineNumber).append(")");
            } else {
                result.append("(Unknown Source)");
            }
        }
        return result.toString();
        // END Android-changed: Fall back Unknown Source:<dex_pc> for unknown lineNumber.
    }

    // Android-changed: Remove javadoc related to the module system.
    /**
     * Returns true if the specified object is another
     * {@code StackTraceElement} instance representing the same execution
     * point as this instance.  Two stack trace elements {@code a} and
     * {@code b} are equal if and only if:
     * <pre>{@code
     *     equals(a.getFileName(), b.getFileName()) &&
     *     a.getLineNumber() == b.getLineNumber()) &&
     *     equals(a.getClassName(), b.getClassName()) &&
     *     equals(a.getMethodName(), b.getMethodName())
     * }</pre>
     * where {@code equals} has the semantics of {@link
     * java.util.Objects#equals(Object, Object) Objects.equals}.
     *
     * @param  obj the object to be compared with this stack trace element.
     * @return true if the specified object is another
     *         {@code StackTraceElement} instance representing the same
     *         execution point as this instance.
     *
     * @revised 9
     * @spec JPMS
     */
    public boolean equals(Object obj) {
        if (obj==this)
            return true;
        if (!(obj instanceof StackTraceElement))
            return false;
        StackTraceElement e = (StackTraceElement)obj;
        // Android-changed: Remove classloader name and module-related methods.
        // return Objects.equals(classLoaderName, e.classLoaderName) &&
        //     Objects.equals(moduleName, e.moduleName) &&
        //     Objects.equals(moduleVersion, e.moduleVersion) &&
        return
            e.declaringClass.equals(declaringClass) &&
            e.lineNumber == lineNumber &&
            Objects.equals(methodName, e.methodName) &&
            Objects.equals(fileName, e.fileName);
    }

    /**
     * Returns a hash code value for this stack trace element.
     */
    public int hashCode() {
        int result = 31*declaringClass.hashCode() + methodName.hashCode();
        // Android-changed: Remove classloader name and module-related methods.
        // result = 31*result + Objects.hashCode(classLoaderName);
        // result = 31*result + Objects.hashCode(moduleName);
        // result = 31*result + Objects.hashCode(moduleVersion);
        result = 31*result + Objects.hashCode(fileName);
        result = 31*result + lineNumber;
        return result;
    }


    // BEGIN Android-removed: Remove classloader name and unsupported module name and version.
    /*
    /**
     * Called from of() methods to set the 'format' bitmap using the Class
     * reference stored in declaringClassObject, and then clear the reference.
     *
     * <p>
     * If the module is a non-upgradeable JDK module, then set
     * JDK_NON_UPGRADEABLE_MODULE to omit its version string.
     * <p>
     * If the loader is one of the built-in loaders (`boot`, `platform`, or `app`)
     * then set BUILTIN_CLASS_LOADER to omit the first element (`<loader>/`).
     *
    private synchronized void computeFormat() {
        try {
            Class<?> cls = (Class<?>) declaringClassObject;
            ClassLoader loader = cls.getClassLoader0();
            Module m = cls.getModule();
            byte bits = 0;

            // First element - class loader name
            // Call package-private ClassLoader::name method

            if (loader instanceof BuiltinClassLoader) {
                bits |= BUILTIN_CLASS_LOADER;
            }

            // Second element - module name and version

            // Omit if is a JDK non-upgradeable module (recorded in the hashes
            // in java.base)
            if (isHashedInJavaBase(m)) {
                bits |= JDK_NON_UPGRADEABLE_MODULE;
            }
            format = bits;
        } finally {
            // Class reference no longer needed, clear it
            declaringClassObject = null;
        }
    }

    private static final byte BUILTIN_CLASS_LOADER       = 0x1;
    private static final byte JDK_NON_UPGRADEABLE_MODULE = 0x2;

    private boolean dropClassLoaderName() {
        return (format & BUILTIN_CLASS_LOADER) == BUILTIN_CLASS_LOADER;
    }

    private boolean dropModuleVersion() {
        return (format & JDK_NON_UPGRADEABLE_MODULE) == JDK_NON_UPGRADEABLE_MODULE;
    }

    /**
     * Returns true if the module is hashed with java.base.
     * <p>
     * This method returns false when running on the exploded image
     * since JDK modules are not hashed. They have no Version attribute
     * and so "@<version>" part will be omitted anyway.
     *
    private static boolean isHashedInJavaBase(Module m) {
        // return true if module system is not initialized as the code
        // must be in java.base
        if (!VM.isModuleSystemInited())
            return true;

        return ModuleLayer.boot() == m.getLayer() && HashedModules.contains(m);
    }

    /*
     * Finds JDK non-upgradeable modules, i.e. the modules that are
     * included in the hashes in java.base.
     *
    private static class HashedModules {
        static Set<String> HASHED_MODULES = hashedModules();

        static Set<String> hashedModules() {

            Optional<ResolvedModule> resolvedModule = ModuleLayer.boot()
                    .configuration()
                    .findModule("java.base");
            assert resolvedModule.isPresent();
            ModuleReference mref = resolvedModule.get().reference();
            assert mref instanceof ModuleReferenceImpl;
            ModuleHashes hashes = ((ModuleReferenceImpl)mref).recordedHashes();
            if (hashes != null) {
                Set<String> names = new HashSet<>(hashes.names());
                names.add("java.base");
                return names;
            }

            return Set.of();
        }

        static boolean contains(Module m) {
            return HASHED_MODULES.contains(m.getName());
        }
    }
    */
    // END Android-removed: Remove classloader name and unsupported module name and version.

    // BEGIN Android-removed: code unused by Throwable in libcore.
    /*
     * Returns an array of StackTraceElements of the given depth
     * filled from the backtrace of a given Throwable.
     *
    static StackTraceElement[] of(Throwable x, int depth) {
        StackTraceElement[] stackTrace = new StackTraceElement[depth];
        for (int i = 0; i < depth; i++) {
            stackTrace[i] = new StackTraceElement();
        }

        // VM to fill in StackTraceElement
        initStackTraceElements(stackTrace, x);

        // ensure the proper StackTraceElement initialization
        for (StackTraceElement ste : stackTrace) {
            ste.computeFormat();
        }
        return stackTrace;
    }
    */
    // END Android-removed: code unused by Throwable in libcore.

    /*
     * Returns a StackTraceElement from a given StackFrameInfo.
     */
    static StackTraceElement of(StackFrameInfo sfi) {
        // Android-changed: Simple java implementation without module name.
        // StackTraceElement ste = new StackTraceElement();
        // initStackTraceElement(ste, sfi);
        StackTraceElement ste = new StackTraceElement(sfi.getClassName(), sfi.getMethodName(),
            sfi.getFileName(), sfi.getLineNumber());

        // Android-removed: Remove classloader name and unsupported module name and version.
        // ste.computeFormat();
        return ste;
    }

    /*
     * Sets the given stack trace elements with the backtrace
     * of the given Throwable.
     */
    // Android-removed: code unused by Throwable in libcore.
    // private static native void initStackTraceElements(StackTraceElement[] elements,
    //                                                   Throwable x);

    /*
     * Sets the given stack trace element with the given StackFrameInfo
     */
    // Android-removed: unused dead code.
    // private static native void initStackTraceElement(StackTraceElement element,
    //                                                  StackFrameInfo sfi);

    private static final long serialVersionUID = 6992337162326171013L;
}
