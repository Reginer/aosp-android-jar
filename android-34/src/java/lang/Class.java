/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1994, 2019, Oracle and/or its affiliates. All rights reserved.
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

import dalvik.annotation.optimization.FastNative;
import dalvik.system.ClassExt;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.ref.SoftReference;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamField;
import java.lang.invoke.TypeDescriptor;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import libcore.reflect.GenericSignatureParser;
import libcore.reflect.InternalNames;
import libcore.reflect.RecordComponents;
import libcore.reflect.Types;
import libcore.util.BasicLruCache;
import libcore.util.CollectionUtils;
import libcore.util.EmptyArray;

import sun.security.util.SecurityConstants;
import dalvik.system.ClassExt;
import sun.invoke.util.Wrapper;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;
import sun.reflect.misc.ReflectUtil;

/**
 * Instances of the class {@code Class} represent classes and interfaces
 * in a running Java application. An enum type is a kind of class and an
 * annotation type is a kind of interface. Every array also
 * belongs to a class that is reflected as a {@code Class} object
 * that is shared by all arrays with the same element type and number
 * of dimensions.  The primitive Java types ({@code boolean},
 * {@code byte}, {@code char}, {@code short},
 * {@code int}, {@code long}, {@code float}, and
 * {@code double}), and the keyword {@code void} are also
 * represented as {@code Class} objects.
 *
 * <p> {@code Class} has no public constructor. Instead a {@code Class}
 * object is constructed automatically by the Java Virtual Machine
 * when a class loader invokes one of the
 * {@link ClassLoader#defineClass(String,byte[], int,int) defineClass} methods
 * and passes the bytes of a {@code class} file.
 *
 * <p> The methods of class {@code Class} expose many characteristics of a
 * class or interface. Most characteristics are derived from the {@code class}
 * file that the class loader passed to the Java Virtual Machine. A few
 * characteristics are determined by the class loading environment at run time.
 *
 * <p> Some methods of class {@code Class} expose whether the declaration of
 * a class or interface in Java source code was <em>enclosed</em> within
 * another declaration. Other methods describe how a class or interface
 * is situated in a <em>nest</em>. A <a id="nest">nest</a> is a set of
 * classes and interfaces, in the same run-time package, that
 * allow mutual access to their {@code private} members.
 * The classes and interfaces are known as <em>nestmates</em>.
 * One nestmate acts as the
 * <em>nest host</em>, and enumerates the other nestmates which
 * belong to the nest; each of them in turn records it as the nest host.
 * The classes and interfaces which belong to a nest, including its host, are
 * determined when
 * {@code class} files are generated, for example, a Java compiler
 * will typically record a top-level class as the host of a nest where the
 * other members are the classes and interfaces whose declarations are
 * enclosed within the top-level class declaration.
 *
 * <p> The following example uses a {@code Class} object to print the
 * class name of an object:
 *
 * <blockquote><pre>
 *     void printClassName(Object obj) {
 *         System.out.println("The class of " + obj +
 *                            " is " + obj.getClass().getName());
 *     }
 * </pre></blockquote>
 *
 * <p> It is also possible to get the {@code Class} object for a named
 * type (or for void) using a class literal.  See Section 15.8.2 of
 * <cite>The Java&trade; Language Specification</cite>.
 * For example:
 *
 * <blockquote>
 *     {@code System.out.println("The name of class Foo is: "+Foo.class.getName());}
 * </blockquote>
 *
 * <p> Some methods of class {@code Class} expose whether the declaration of
 * a class or interface in Java source code was <em>enclosed</em> within
 * another declaration. Other methods describe how a class or interface
 * is situated in a <em>nest</em>. A <a id="nest">nest</a> is a set of
 * classes and interfaces, in the same run-time package, that
 * allow mutual access to their {@code private} members.
 * The classes and interfaces are known as <em>nestmates</em>.
 * One nestmate acts as the
 * <em>nest host</em>, and enumerates the other nestmates which
 * belong to the nest; each of them in turn records it as the nest host.
 * The classes and interfaces which belong to a nest, including its host, are
 * determined when
 * {@code class} files are generated, for example, a Java compiler
 * will typically record a top-level class as the host of a nest where the
 * other members are the classes and interfaces whose declarations are
 * enclosed within the top-level class declaration.
 *
 * @param <T> the type of the class modeled by this {@code Class}
 * object.  For example, the type of {@code String.class} is {@code
 * Class<String>}.  Use {@code Class<?>} if the class being modeled is
 * unknown.
 *
 * @author  unascribed
 * @see     java.lang.ClassLoader#defineClass(byte[], int, int)
 * @since   1.0
 */
public final class Class<T> implements java.io.Serializable,
                              GenericDeclaration,
                              Type,
                              AnnotatedElement,
                              TypeDescriptor.OfField<Class<?>> {
    private static final int ANNOTATION= 0x00002000;
    private static final int ENUM      = 0x00004000;
    private static final int SYNTHETIC = 0x00001000;
    private static final int FINALIZABLE = 0x80000000;

    /** defining class loader, or null for the "bootstrap" system loader. */
    private transient ClassLoader classLoader;

    /**
     * For array classes, the component class object for instanceof/checkcast (for String[][][],
     * this will be String[][]). null for non-array classes.
     */
    private transient Class<?> componentType;

    /**
     * DexCache of resolved constant pool entries. Will be null for certain runtime-generated classes
     * e.g. arrays and primitive classes.
     */
    private transient Object dexCache;

    /**
     * Extra data that only some classes possess. This is allocated lazily as needed.
     */
    private transient ClassExt extData;

    /**
     * The interface table (iftable_) contains pairs of a interface class and an array of the
     * interface methods. There is one pair per interface supported by this class.  That
     * means one pair for each interface we support directly, indirectly via superclass, or
     * indirectly via a superinterface.  This will be null if neither we nor our superclass
     * implement any interfaces.
     *
     * Why we need this: given "class Foo implements Face", declare "Face faceObj = new Foo()".
     * Invoke faceObj.blah(), where "blah" is part of the Face interface.  We can't easily use a
     * single vtable.
     *
     * For every interface a concrete class implements, we create an array of the concrete vtable_
     * methods for the methods in the interface.
     */
    private transient Object[] ifTable;

    /** Lazily computed name of this class; always prefer calling getName(). */
    private transient String name;

    /** The superclass, or null if this is java.lang.Object, an interface or primitive type. */
    private transient Class<? super T> superClass;

    /**
     * Virtual method table (vtable), for use by "invoke-virtual". The vtable from the superclass
     * is copied in, and virtual methods from our class either replace those from the super or are
     * appended. For abstract classes, methods may be created in the vtable that aren't in
     * virtual_ methods_ for miranda methods.
     */
    private transient Object vtable;

    /**
     * Instance fields. These describe the layout of the contents of an Object. Note that only the
     * fields directly declared by this class are listed in iFields; fields declared by a
     * superclass are listed in the superclass's Class.iFields.
     *
     * All instance fields that refer to objects are guaranteed to be at the beginning of the field
     * list.  {@link Class#numReferenceInstanceFields} specifies the number of reference fields.
     */
    private transient long iFields;

    /** All methods with this class as the base for virtual dispatch. */
    private transient long methods;

    /** Static fields */
    private transient long sFields;

    /** access flags; low 16 bits are defined by VM spec */
    private transient int accessFlags;

    /** Class flags to help the GC with object scanning. */
    private transient int classFlags;

    /**
     * Total size of the Class instance; used when allocating storage on GC heap.
     * See also {@link Class#objectSize}.
     */
    private transient int classSize;

    /**
     * tid used to check for recursive static initializer invocation.
     */
    private transient int clinitThreadId;

    /**
     * Class def index from dex file. An index of 65535 indicates that there is no class definition,
     * for example for an array type.
     * TODO: really 16bits as type indices are 16bit.
     */
    private transient int dexClassDefIndex;

    /**
     * Class type index from dex file, lazily computed. An index of 65535 indicates that the type
     * index isn't known. Volatile to avoid double-checked locking bugs.
     * TODO: really 16bits as type indices are 16bit.
     */
    private transient volatile int dexTypeIndex;

    /** Number of instance fields that are object references. */
    private transient int numReferenceInstanceFields;

    /** Number of static fields that are object references. */
    private transient int numReferenceStaticFields;

    /**
     * Total object size; used when allocating storage on GC heap. For interfaces and abstract
     * classes this will be zero. See also {@link Class#classSize}.
     */
    private transient int objectSize;

    /**
     * Aligned object size for allocation fast path. The value is max int if the object is
     * uninitialized or finalizable, otherwise the aligned object size.
     */
    private transient int objectSizeAllocFastPath;

    /**
     * The lower 16 bits is the primitive type value, or 0 if not a primitive type; set for
     * generated primitive classes.
     */
    private transient int primitiveType;

    /** Bitmap of offsets of iFields. */
    private transient int referenceInstanceOffsets;

    /** State of class initialization */
    private transient int status;

    /** Offset of the first virtual method copied from an interface in the methods array. */
    private transient short copiedMethodsOffset;

    /** Offset of the first virtual method defined in this class in the methods array. */
    private transient short virtualMethodsOffset;

    /*
     * Private constructor. Only the Java Virtual Machine creates Class objects.
     * This constructor is not used and prevents the default constructor being
     * generated.
     */
    private Class() {}

    private Class(ClassLoader loader, Class<?> arrayComponentType) {
        // Initialize final field for classLoader.  The initialization value of non-null
        // prevents future JIT optimizations from assuming this final field is null.
        classLoader = loader;
        componentType = arrayComponentType;
    }

    /**
     * Converts the object to a string. The string representation is the
     * string "class" or "interface", followed by a space, and then by the
     * fully qualified name of the class in the format returned by
     * {@code getName}.  If this {@code Class} object represents a
     * primitive type, this method returns the name of the primitive type.  If
     * this {@code Class} object represents void this method returns
     * "void". If this {@code Class} object represents an array type,
     * this method returns "class " followed by {@code getName}.
     *
     * @return a string representation of this class object.
     */
    public String toString() {
        return (isInterface() ? "interface " : (isPrimitive() ? "" : "class "))
            + getName();
    }

    /**
     * Returns a string describing this {@code Class}, including
     * information about modifiers and type parameters.
     *
     * The string is formatted as a list of type modifiers, if any,
     * followed by the kind of type (empty string for primitive types
     * and {@code class}, {@code enum}, {@code interface}, or
     * <code>&#64;</code>{@code interface}, as appropriate), followed
     * by the type's name, followed by an angle-bracketed
     * comma-separated list of the type's type parameters, if any.
     *
     * A space is used to separate modifiers from one another and to
     * separate any modifiers from the kind of type. The modifiers
     * occur in canonical order. If there are no type parameters, the
     * type parameter list is elided.
     *
     * For an array type, the string starts with the type name,
     * followed by an angle-bracketed comma-separated list of the
     * type's type parameters, if any, followed by a sequence of
     * {@code []} characters, one set of brackets per dimension of
     * the array.
     *
     * <p>Note that since information about the runtime representation
     * of a type is being generated, modifiers not present on the
     * originating source code or illegal on the originating source
     * code may be present.
     *
     * @return a string describing this {@code Class}, including
     * information about modifiers and type parameters
     *
     * @since 1.8
     */
    public String toGenericString() {
        if (isPrimitive()) {
            return toString();
        } else {
            StringBuilder sb = new StringBuilder();
            Class<?> component = this;
            int arrayDepth = 0;

            if (isArray()) {
                do {
                    arrayDepth++;
                    component = component.getComponentType();
                } while (component.isArray());
                sb.append(component.getName());
            } else {
                // Class modifiers are a superset of interface modifiers
                int modifiers = getModifiers() & Modifier.classModifiers();
                if (modifiers != 0) {
                    sb.append(Modifier.toString(modifiers));
                    sb.append(' ');
                }

                if (isAnnotation()) {
                    sb.append('@');
                }
                if (isInterface()) { // Note: all annotation types are interfaces
                    sb.append("interface");
                } else {
                    if (isEnum())
                        sb.append("enum");
                    else
                        sb.append("class");
                }
                sb.append(' ');
                sb.append(getName());
            }

            TypeVariable<?>[] typeparms = component.getTypeParameters();
            if (typeparms.length > 0) {
                StringJoiner sj = new StringJoiner(",", "<", ">");
                for(TypeVariable<?> typeparm: typeparms) {
                    sj.add(typeparm.getTypeName());
                }
                sb.append(sj.toString());
            }

            for (int i = 0; i < arrayDepth; i++)
                sb.append("[]");

            return sb.toString();
        }
    }

    /**
     * Returns the {@code Class} object associated with the class or
     * interface with the given string name.  Invoking this method is
     * equivalent to:
     *
     * <blockquote>
     *  {@code Class.forName(className, true, currentLoader)}
     * </blockquote>
     *
     * where {@code currentLoader} denotes the defining class loader of
     * the current class.
     *
     * <p> For example, the following code fragment returns the
     * runtime {@code Class} descriptor for the class named
     * {@code java.lang.Thread}:
     *
     * <blockquote>
     *   {@code Class t = Class.forName("java.lang.Thread")}
     * </blockquote>
     * <p>
     * A call to {@code forName("X")} causes the class named
     * {@code X} to be initialized.
     *
     * @param      className   the fully qualified name of the desired class.
     * @return     the {@code Class} object for the class with the
     *             specified name.
     * @exception LinkageError if the linkage fails
     * @exception ExceptionInInitializerError if the initialization provoked
     *            by this method fails
     * @exception ClassNotFoundException if the class cannot be located
     */
    @CallerSensitive
    public static Class<?> forName(String className)
                throws ClassNotFoundException {
        Class<?> caller = Reflection.getCallerClass();
        return forName(className, true, ClassLoader.getClassLoader(caller));
    }

    // Android-changed: Remove SecurityException javadoc.
    /**
     * Returns the {@code Class} object associated with the class or
     * interface with the given string name, using the given class loader.
     * Given the fully qualified name for a class or interface (in the same
     * format returned by {@code getName}) this method attempts to
     * locate, load, and link the class or interface.  The specified class
     * loader is used to load the class or interface.  If the parameter
     * {@code loader} is null, the class is loaded through the bootstrap
     * class loader.  The class is initialized only if the
     * {@code initialize} parameter is {@code true} and if it has
     * not been initialized earlier.
     *
     * <p> If {@code name} denotes a primitive type or void, an attempt
     * will be made to locate a user-defined class in the unnamed package whose
     * name is {@code name}. Therefore, this method cannot be used to
     * obtain any of the {@code Class} objects representing primitive
     * types or void.
     *
     * <p> If {@code name} denotes an array class, the component type of
     * the array class is loaded but not initialized.
     *
     * <p> For example, in an instance method the expression:
     *
     * <blockquote>
     *  {@code Class.forName("Foo")}
     * </blockquote>
     *
     * is equivalent to:
     *
     * <blockquote>
     *  {@code Class.forName("Foo", true, this.getClass().getClassLoader())}
     * </blockquote>
     *
     * Note that this method throws errors related to loading, linking or
     * initializing as specified in Sections 12.2, 12.3 and 12.4 of <em>The
     * Java Language Specification</em>.
     * Note that this method does not check whether the requested class
     * is accessible to its caller.
     *
     * @param name       fully qualified name of the desired class
     * @param initialize if {@code true} the class will be initialized.
     *                   See Section 12.4 of <em>The Java Language Specification</em>.
     * @param loader     class loader from which the class must be loaded
     * @return           class object representing the desired class
     *
     * @exception LinkageError if the linkage fails
     * @exception ExceptionInInitializerError if the initialization provoked
     *            by this method fails
     * @exception ClassNotFoundException if the class cannot be located by
     *            the specified class loader
     *
     * @see       java.lang.Class#forName(String)
     * @see       java.lang.ClassLoader
     * @since     1.2
     */
    @CallerSensitive
    public static Class<?> forName(String name, boolean initialize,
                                   ClassLoader loader)
        throws ClassNotFoundException
    {
        if (loader == null) {
            loader = BootClassLoader.getInstance();
        }
        Class<?> result;
        try {
            result = classForName(name, initialize, loader);
        } catch (ClassNotFoundException e) {
            Throwable cause = e.getCause();
            if (cause instanceof LinkageError) {
                throw (LinkageError) cause;
            }
            throw e;
        }
        return result;
    }

    /** Called after security checks have been made. */
    @FastNative
    static native Class<?> classForName(String className, boolean shouldInitialize,
            ClassLoader classLoader) throws ClassNotFoundException;

    // Android-removed: Remove unsupported forName(Module, String) method.
    /*
     * Returns the {@code Class} with the given <a href="ClassLoader.html#name">
     * binary name</a> in the given module.
     *
     * <p> This method attempts to locate, load, and link the class or interface.
     * It does not run the class initializer.  If the class is not found, this
     * method returns {@code null}. </p>
     *
     * <p> If the class loader of the given module defines other modules and
     * the given name is a class defined in a different module, this method
     * returns {@code null} after the class is loaded. </p>
     *
     * <p> This method does not check whether the requested class is
     * accessible to its caller. </p>
     *
     * @apiNote
     * This method returns {@code null} on failure rather than
     * throwing a {@link ClassNotFoundException}, as is done by
     * the {@link #forName(String, boolean, ClassLoader)} method.
     * The security check is a stack-based permission check if the caller
     * loads a class in another module.
     *
     * @param  module   A module
     * @param  name     The <a href="ClassLoader.html#name">binary name</a>
     *                  of the class
     * @return {@code Class} object of the given name defined in the given module;
     *         {@code null} if not found.
     *
     * @throws NullPointerException if the given module or name is {@code null}
     *
     * @throws LinkageError if the linkage fails
     *
     * @throws SecurityException
     *         <ul>
     *         <li> if the caller is not the specified module and
     *         {@code RuntimePermission("getClassLoader")} permission is denied; or</li>
     *         <li> access to the module content is denied. For example,
     *         permission check will be performed when a class loader calls
     *         {@link ModuleReader#open(String)} to read the bytes of a class file
     *         in a module.</li>
     *         </ul>
     *
     * @since 9
     * @spec JPMS
     *
    @CallerSensitive
    public static Class<?> forName(Module module, String name) {
        Objects.requireNonNull(module);
        Objects.requireNonNull(name);

        ClassLoader cl;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            Class<?> caller = Reflection.getCallerClass();
            if (caller != null && caller.getModule() != module) {
                // if caller is null, Class.forName is the last java frame on the stack.
                // java.base has all permissions
                sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
            }
            PrivilegedAction<ClassLoader> pa = module::getClassLoader;
            cl = AccessController.doPrivileged(pa);
        } else {
            cl = module.getClassLoader();
        }

        if (cl != null) {
            return cl.loadClass(module, name);
        } else {
            return BootLoader.loadClass(module, name);
        }
    }
    */

    // Android-changed: Remove SecurityException javadoc.
    /**
     * Creates a new instance of the class represented by this {@code Class}
     * object.  The class is instantiated as if by a {@code new}
     * expression with an empty argument list.  The class is initialized if it
     * has not already been initialized.
     *
     * @deprecated This method propagates any exception thrown by the
     * nullary constructor, including a checked exception.  Use of
     * this method effectively bypasses the compile-time exception
     * checking that would otherwise be performed by the compiler.
     * The {@link
     * java.lang.reflect.Constructor#newInstance(java.lang.Object...)
     * Constructor.newInstance} method avoids this problem by wrapping
     * any exception thrown by the constructor in a (checked) {@link
     * java.lang.reflect.InvocationTargetException}.
     *
     * <p>The call
     *
     * <pre>{@code
     * clazz.newInstance()
     * }</pre>
     *
     * can be replaced by
     *
     * <pre>{@code
     * clazz.getDeclaredConstructor().newInstance()
     * }</pre>
     *
     * The latter sequence of calls is inferred to be able to throw
     * the additional exception types {@link
     * InvocationTargetException} and {@link
     * NoSuchMethodException}. Both of these exception types are
     * subclasses of {@link ReflectiveOperationException}.
     *
     * @return  a newly allocated instance of the class represented by this
     *          object.
     * @throws  IllegalAccessException  if the class or its nullary
     *          constructor is not accessible.
     * @throws  InstantiationException
     *          if this {@code Class} represents an abstract class,
     *          an interface, an array class, a primitive type, or void;
     *          or if the class has no nullary constructor;
     *          or if the instantiation fails for some other reason.
     * @throws  ExceptionInInitializerError if the initialization
     *          provoked by this method fails.
     */
    // Android-changed: Implement newInstance() by native code.
    /*
    @CallerSensitive
    public T newInstance()
        throws InstantiationException, IllegalAccessException
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), false);
        }

        // NOTE: the following code may not be strictly correct under
        // the current Java memory model.

        // Constructor lookup
        if (cachedConstructor == null) {
            if (this == Class.class) {
                throw new IllegalAccessException(
                    "Can not call newInstance() on the Class for java.lang.Class"
                );
            }
            try {
                Class<?>[] empty = {};
                final Constructor<T> c = getReflectionFactory().copyConstructor(
                    getConstructor0(empty, Member.DECLARED));
                // Disable accessibility checks on the constructor
                // since we have to do the security check here anyway
                // (the stack depth is wrong for the Constructor's
                // security check to work)
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<>() {
                        public Void run() {
                                c.setAccessible(true);
                                return null;
                            }
                        });
                cachedConstructor = c;
            } catch (NoSuchMethodException e) {
                throw (InstantiationException)
                    new InstantiationException(getName()).initCause(e);
            }
        }
        Constructor<T> tmpConstructor = cachedConstructor;
        // Security check (same as in java.lang.reflect.Constructor)
        Class<?> caller = Reflection.getCallerClass();
        if (newInstanceCallerCache != caller) {
            int modifiers = tmpConstructor.getModifiers();
            Reflection.ensureMemberAccess(caller, this, this, modifiers);
            newInstanceCallerCache = caller;
        }
        // Run constructor
        try {
            return tmpConstructor.newInstance((Object[])null);
        } catch (InvocationTargetException e) {
            Unsafe.getUnsafe().throwException(e.getTargetException());
            // Not reached
            return null;
        }
    }
    */
    @FastNative
    @Deprecated(since="9")
    public native T newInstance() throws InstantiationException, IllegalAccessException;

    /**
     * Determines if the specified {@code Object} is assignment-compatible
     * with the object represented by this {@code Class}.  This method is
     * the dynamic equivalent of the Java language {@code instanceof}
     * operator. The method returns {@code true} if the specified
     * {@code Object} argument is non-null and can be cast to the
     * reference type represented by this {@code Class} object without
     * raising a {@code ClassCastException.} It returns {@code false}
     * otherwise.
     *
     * <p> Specifically, if this {@code Class} object represents a
     * declared class, this method returns {@code true} if the specified
     * {@code Object} argument is an instance of the represented class (or
     * of any of its subclasses); it returns {@code false} otherwise. If
     * this {@code Class} object represents an array class, this method
     * returns {@code true} if the specified {@code Object} argument
     * can be converted to an object of the array class by an identity
     * conversion or by a widening reference conversion; it returns
     * {@code false} otherwise. If this {@code Class} object
     * represents an interface, this method returns {@code true} if the
     * class or any superclass of the specified {@code Object} argument
     * implements this interface; it returns {@code false} otherwise. If
     * this {@code Class} object represents a primitive type, this method
     * returns {@code false}.
     *
     * @param   obj the object to check
     * @return  true if {@code obj} is an instance of this class
     *
     * @since 1.1
     */
    @HotSpotIntrinsicCandidate
    // Android-changed: JNI code can be replaced by simple java code.
    // public native boolean isInstance(Object obj);
    public boolean isInstance(Object obj) {
        if (obj == null) {
            return false;
        }
        return isAssignableFrom(obj.getClass());
    }


    /**
     * Determines if the class or interface represented by this
     * {@code Class} object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * {@code Class} parameter. It returns {@code true} if so;
     * otherwise it returns {@code false}. If this {@code Class}
     * object represents a primitive type, this method returns
     * {@code true} if the specified {@code Class} parameter is
     * exactly this {@code Class} object; otherwise it returns
     * {@code false}.
     *
     * <p> Specifically, this method tests whether the type represented by the
     * specified {@code Class} parameter can be converted to the type
     * represented by this {@code Class} object via an identity conversion
     * or via a widening reference conversion. See <em>The Java Language
     * Specification</em>, sections 5.1.1 and 5.1.4 , for details.
     *
     * @param cls the {@code Class} object to be checked
     * @return the {@code boolean} value indicating whether objects of the
     * type {@code cls} can be assigned to objects of this class
     * @exception NullPointerException if the specified Class parameter is
     *            null.
     * @since 1.1
     */
    @HotSpotIntrinsicCandidate
    // Android-changed: JNI code can be replaced by simple java code.
    // public native boolean isAssignableFrom(Class<?> cls);
    public boolean isAssignableFrom(Class<?> cls) {
        if (this == cls) {
            return true;  // Can always assign to things of the same type.
        } else if (this == Object.class) {
            return !cls.isPrimitive();  // Can assign any reference to java.lang.Object.
        } else if (isArray()) {
            return cls.isArray() && componentType.isAssignableFrom(cls.componentType);
        } else if (isInterface()) {
            // Search iftable which has a flattened and uniqued list of interfaces.
            Object[] iftable = cls.ifTable;
            if (iftable != null) {
                for (int i = 0; i < iftable.length; i += 2) {
                    if (iftable[i] == this) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            if (!cls.isInterface()) {
                for (cls = cls.superClass; cls != null; cls = cls.superClass) {
                    if (cls == this) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Determines if the specified {@code Class} object represents an
     * interface type.
     *
     * @return  {@code true} if this object represents an interface;
     *          {@code false} otherwise.
     */
    @HotSpotIntrinsicCandidate
    // Android-changed: JNI code can be replaced by simple java code.
    // public native boolean isInterface();
    public boolean isInterface() {
        return (accessFlags & Modifier.INTERFACE) != 0;
    }

    /**
     * Determines if this {@code Class} object represents an array class.
     *
     * @return  {@code true} if this object represents an array class;
     *          {@code false} otherwise.
     * @since   1.1
     */
    @HotSpotIntrinsicCandidate
    // Android-changed: JNI code can be replaced by simple java code.
    // public native boolean isArray();
    public boolean isArray() {
        return getComponentType() != null;
    }

    /**
     * Determines if the specified {@code Class} object represents a
     * primitive type.
     *
     * <p> There are nine predefined {@code Class} objects to represent
     * the eight primitive types and void.  These are created by the Java
     * Virtual Machine, and have the same names as the primitive types that
     * they represent, namely {@code boolean}, {@code byte},
     * {@code char}, {@code short}, {@code int},
     * {@code long}, {@code float}, and {@code double}.
     *
     * <p> These objects may only be accessed via the following public static
     * final variables, and are the only {@code Class} objects for which
     * this method returns {@code true}.
     *
     * @return true if and only if this class represents a primitive type
     *
     * @see     java.lang.Boolean#TYPE
     * @see     java.lang.Character#TYPE
     * @see     java.lang.Byte#TYPE
     * @see     java.lang.Short#TYPE
     * @see     java.lang.Integer#TYPE
     * @see     java.lang.Long#TYPE
     * @see     java.lang.Float#TYPE
     * @see     java.lang.Double#TYPE
     * @see     java.lang.Void#TYPE
     * @since 1.1
     */
    @HotSpotIntrinsicCandidate
    // Android-changed: JNI code can be replaced by simple java code.
    // public native boolean isPrimitive();
    public boolean isPrimitive() {
      return (primitiveType & 0xFFFF) != 0;
    }

    /**
     * Indicates whether this {@code Class} or its parents override finalize.
     *
     * @return {@code true} if and if this class or its parents override
     *         finalize;
     *
     * @hide
     */
    public boolean isFinalizable() {
        return (getModifiers() & FINALIZABLE) != 0;
    }

    /**
     * Returns true if this {@code Class} object represents an annotation
     * type.  Note that if this method returns true, {@link #isInterface()}
     * would also return true, as all annotation types are also interfaces.
     *
     * @return {@code true} if this class object represents an annotation
     *      type; {@code false} otherwise
     * @since 1.5
     */
    public boolean isAnnotation() {
        return (getModifiers() & ANNOTATION) != 0;
    }

    /**
     * Returns {@code true} if this class is a synthetic class;
     * returns {@code false} otherwise.
     * @return {@code true} if and only if this class is a synthetic class as
     *         defined by the Java Language Specification.
     * @jls 13.1 The Form of a Binary
     * @since 1.5
     */
    public boolean isSynthetic() {
        return (getModifiers() & SYNTHETIC) != 0;
    }

    /**
     * Returns the  name of the entity (class, interface, array class,
     * primitive type, or void) represented by this {@code Class} object,
     * as a {@code String}.
     *
     * <p> If this class object represents a reference type that is not an
     * array type then the binary name of the class is returned, as specified
     * by
     * <cite>The Java&trade; Language Specification</cite>.
     *
     * <p> If this class object represents a primitive type or void, then the
     * name returned is a {@code String} equal to the Java language
     * keyword corresponding to the primitive type or void.
     *
     * <p> If this class object represents a class of arrays, then the internal
     * form of the name consists of the name of the element type preceded by
     * one or more '{@code [}' characters representing the depth of the array
     * nesting.  The encoding of element type names is as follows:
     *
     * <blockquote><table class="striped">
     * <caption style="display:none">Element types and encodings</caption>
     * <thead>
     * <tr><th scope="col"> Element Type <th scope="col"> Encoding
     * </thead>
     * <tbody style="text-align:left">
     * <tr><th scope="row"> boolean      <td style="text-align:center"> Z
     * <tr><th scope="row"> byte         <td style="text-align:center"> B
     * <tr><th scope="row"> char         <td style="text-align:center"> C
     * <tr><th scope="row"> class or interface
     *                                   <td style="text-align:center"> L<i>classname</i>;
     * <tr><th scope="row"> double       <td style="text-align:center"> D
     * <tr><th scope="row"> float        <td style="text-align:center"> F
     * <tr><th scope="row"> int          <td style="text-align:center"> I
     * <tr><th scope="row"> long         <td style="text-align:center"> J
     * <tr><th scope="row"> short        <td style="text-align:center"> S
     * </tbody>
     * </table></blockquote>
     *
     * <p> The class or interface name <i>classname</i> is the binary name of
     * the class specified above.
     *
     * <p> Examples:
     * <blockquote><pre>
     * String.class.getName()
     *     returns "java.lang.String"
     * byte.class.getName()
     *     returns "byte"
     * (new Object[3]).getClass().getName()
     *     returns "[Ljava.lang.Object;"
     * (new int[3][4][5][6][7][8][9]).getClass().getName()
     *     returns "[[[[[[[I"
     * </pre></blockquote>
     *
     * @return  the name of the class or interface
     *          represented by this object.
     */
    public String getName() {
        String name = this.name;
        // Android-changed: ART has a different JNI layer.
        // return name != null ? name : initClassName();
        if (name == null)
            this.name = name = getNameNative();
        return name;

    }

    // Android-changed: ART has a different JNI layer.
    // private native String initClassName();
    @FastNative
    private native String getNameNative();

    // Android-changed: Remove SecurityException javadoc.
    /**
     * Returns the class loader for the class.  Some implementations may use
     * null to represent the bootstrap class loader. This method will return
     * null in such implementations if this class was loaded by the bootstrap
     * class loader.
     *
     * <p>If this object
     * represents a primitive type or void, null is returned.
     *
     * @return  the class loader that loaded the class or interface
     *          represented by this object.
     * @see java.lang.ClassLoader
     * @see SecurityManager#checkPermission
     * @see java.lang.RuntimePermission
     */
    // Android-changed: Remove unused annotation.
    // @CallerSensitive
    // @ForceInline // to ensure Reflection.getCallerClass optimization
    public ClassLoader getClassLoader() {
        if (isPrimitive()) {
            return null;
        }
        // Android-note: The RI returns null in the case where Android returns BootClassLoader.
        // Noted in http://b/111850480#comment3
        return (classLoader == null) ? BootClassLoader.getInstance() : classLoader;
    }

    // Android-removed: Remove unsupported getModule().
    /*
     * Returns the module that this class or interface is a member of.
     *
     * If this class represents an array type then this method returns the
     * {@code Module} for the element type. If this class represents a
     * primitive type or void, then the {@code Module} object for the
     * {@code java.base} module is returned.
     *
     * If this class is in an unnamed module then the {@linkplain
     * ClassLoader#getUnnamedModule() unnamed} {@code Module} of the class
     * loader for this class is returned.
     *
     * @return the module that this class or interface is a member of
     *
     * @since 9
     * @spec JPMS
     *
    public Module getModule() {
        return module;
    }

    // set by VM
    private transient Module module;
    */

    /**
     * Returns an array of {@code TypeVariable} objects that represent the
     * type variables declared by the generic declaration represented by this
     * {@code GenericDeclaration} object, in declaration order.  Returns an
     * array of length 0 if the underlying generic declaration declares no type
     * variables.
     *
     * @return an array of {@code TypeVariable} objects that represent
     *     the type variables declared by this generic declaration
     * @throws java.lang.reflect.GenericSignatureFormatError if the generic
     *     signature of this generic declaration does not conform to
     *     the format specified in
     *     <cite>The Java&trade; Virtual Machine Specification</cite>
     * @since 1.5
     */
    @Override
    public synchronized TypeVariable<Class<T>>[] getTypeParameters() {
        String annotationSignature = getSignatureAttribute();
        if (annotationSignature == null) {
            return EmptyArray.TYPE_VARIABLE;
        }
        GenericSignatureParser parser = new GenericSignatureParser(getClassLoader());
        parser.parseForClass(this, annotationSignature);
        return parser.formalTypeParameters;
    }


    /**
     * Returns the {@code Class} representing the direct superclass of the
     * entity (class, interface, primitive type or void) represented by
     * this {@code Class}.  If this {@code Class} represents either the
     * {@code Object} class, an interface, a primitive type, or void, then
     * null is returned.  If this object represents an array class then the
     * {@code Class} object representing the {@code Object} class is
     * returned.
     *
     * @return the direct superclass of the class represented by this object
     */
    @HotSpotIntrinsicCandidate
    // Android-changed: ART has a different JNI layer.
    // public native Class<? super T> getSuperclass();
    public Class<? super T> getSuperclass() {
        // For interfaces superClass is Object (which agrees with the JNI spec)
        // but not with the expected behavior here.
        if (isInterface()) {
            return null;
        } else {
            return superClass;
        }
    }

    /**
     * Returns the {@code Type} representing the direct superclass of
     * the entity (class, interface, primitive type or void) represented by
     * this {@code Class}.
     *
     * <p>If the superclass is a parameterized type, the {@code Type}
     * object returned must accurately reflect the actual type
     * parameters used in the source code. The parameterized type
     * representing the superclass is created if it had not been
     * created before. See the declaration of {@link
     * java.lang.reflect.ParameterizedType ParameterizedType} for the
     * semantics of the creation process for parameterized types.  If
     * this {@code Class} represents either the {@code Object}
     * class, an interface, a primitive type, or void, then null is
     * returned.  If this object represents an array class then the
     * {@code Class} object representing the {@code Object} class is
     * returned.
     *
     * @throws java.lang.reflect.GenericSignatureFormatError if the generic
     *     class signature does not conform to the format specified in
     *     <cite>The Java&trade; Virtual Machine Specification</cite>
     * @throws TypeNotPresentException if the generic superclass
     *     refers to a non-existent type declaration
     * @throws java.lang.reflect.MalformedParameterizedTypeException if the
     *     generic superclass refers to a parameterized type that cannot be
     *     instantiated  for any reason
     * @return the direct superclass of the class represented by this object
     * @since 1.5
     */
    public Type getGenericSuperclass() {
        Type genericSuperclass = getSuperclass();
        // This method is specified to return null for all cases where getSuperclass
        // returns null, i.e, for primitives, interfaces, void and java.lang.Object.
        if (genericSuperclass == null) {
            return null;
        }

        String annotationSignature = getSignatureAttribute();
        if (annotationSignature != null) {
            GenericSignatureParser parser = new GenericSignatureParser(getClassLoader());
            parser.parseForClass(this, annotationSignature);
            genericSuperclass = parser.superclassType;
        }
        return Types.getType(genericSuperclass);
    }

    /**
     * Gets the package of this class.
     *
     * <p>If this class represents an array type, a primitive type or void,
     * this method returns {@code null}.
     *
     * @return the package of this class.
     * @revised 9
     * @spec JPMS
     */
    public Package getPackage() {
        // Android-changed: ART has a different JNI layer.
        /*
        if (isPrimitive() || isArray()) {
            return null;
        }
        ClassLoader cl = getClassLoader0();
        return cl != null ? cl.definePackage(this)
            : BootLoader.definePackage(this);
        */
        ClassLoader loader = getClassLoader();
        if (loader != null) {
            String packageName = getPackageName();
            return packageName != null ? loader.getPackage(packageName) : null;
        }
        return null;
    }

    /**
     * Returns the fully qualified package name.
     *
     * <p> If this class is a top level class, then this method returns the fully
     * qualified name of the package that the class is a member of, or the
     * empty string if the class is in an unnamed package.
     *
     * <p> If this class is a member class, then this method is equivalent to
     * invoking {@code getPackageName()} on the {@linkplain #getEnclosingClass
     * enclosing class}.
     *
     * <p> If this class is a {@linkplain #isLocalClass local class} or an {@linkplain
     * #isAnonymousClass() anonymous class}, then this method is equivalent to
     * invoking {@code getPackageName()} on the {@linkplain #getDeclaringClass
     * declaring class} of the {@linkplain #getEnclosingMethod enclosing method} or
     * {@linkplain #getEnclosingConstructor enclosing constructor}.
     *
     * <p> If this class represents an array type then this method returns the
     * package name of the element type. If this class represents a primitive
     * type or void then the package name "{@code java.lang}" is returned.
     *
     * @return the fully qualified package name
     *
     * @since 9
     * @spec JPMS
     * @jls 6.7  Fully Qualified Names
     */
    public String getPackageName() {
        // BEGIN Android-changed: Don't use a private field as a cache.
        /*
        String pn = this.packageName;
        if (pn == null) {
            Class<?> c = this;
            while (c.isArray()) {
                c = c.getComponentType();
            }
            if (c.isPrimitive()) {
                pn = "java.lang";
            } else {
                String cn = c.getName();
                int dot = cn.lastIndexOf('.');
                pn = (dot != -1) ? cn.substring(0, dot).intern() : "";
            }
            this.packageName = pn;
        }
        return pn;
        */
            Class<?> c = this;
            while (c.isArray()) {
                c = c.getComponentType();
            }
            if (c.isPrimitive()) {
                return "java.lang";
            } else {
                String cn = c.getName();
                int dot = cn.lastIndexOf('.');
                return (dot != -1) ? cn.substring(0, dot).intern() : "";
            }
        // END Android-changed: Don't use a private field as a cache.
    }

    // cached package name
    // Android-removed: Don't use a private field as a cache.
    // private transient String packageName;

    /**
     * Returns the interfaces directly implemented by the class or interface
     * represented by this object.
     *
     * <p>If this object represents a class, the return value is an array
     * containing objects representing all interfaces directly implemented by
     * the class.  The order of the interface objects in the array corresponds
     * to the order of the interface names in the {@code implements} clause of
     * the declaration of the class represented by this object.  For example,
     * given the declaration:
     * <blockquote>
     * {@code class Shimmer implements FloorWax, DessertTopping { ... }}
     * </blockquote>
     * suppose the value of {@code s} is an instance of
     * {@code Shimmer}; the value of the expression:
     * <blockquote>
     * {@code s.getClass().getInterfaces()[0]}
     * </blockquote>
     * is the {@code Class} object that represents interface
     * {@code FloorWax}; and the value of:
     * <blockquote>
     * {@code s.getClass().getInterfaces()[1]}
     * </blockquote>
     * is the {@code Class} object that represents interface
     * {@code DessertTopping}.
     *
     * <p>If this object represents an interface, the array contains objects
     * representing all interfaces directly extended by the interface.  The
     * order of the interface objects in the array corresponds to the order of
     * the interface names in the {@code extends} clause of the declaration of
     * the interface represented by this object.
     *
     * <p>If this object represents a class or interface that implements no
     * interfaces, the method returns an array of length 0.
     *
     * <p>If this object represents a primitive type or void, the method
     * returns an array of length 0.
     *
     * <p>If this {@code Class} object represents an array type, the
     * interfaces {@code Cloneable} and {@code java.io.Serializable} are
     * returned in that order.
     *
     * @return an array of interfaces directly implemented by this class
     */
    public Class<?>[] getInterfaces() {
        // Android-changed: ART has a different implenmentation.
        // return getInterfaces(true);
        if (isArray()) {
            return new Class<?>[] { Cloneable.class, Serializable.class };
        }

        final Class<?>[] ifaces = getInterfacesInternal();
        if (ifaces == null) {
            return EmptyArray.CLASS;
        }

        return ifaces;
    }

    // Android-revmoed: Remove unused getInterfaces(boolean) method.
    /*
    private Class<?>[] getInterfaces(boolean cloneArray) {
        ReflectionData<T> rd = reflectionData();
        if (rd == null) {
            // no cloning required
            return getInterfaces0();
        } else {
            Class<?>[] interfaces = rd.interfaces;
            if (interfaces == null) {
                interfaces = getInterfaces0();
                rd.interfaces = interfaces;
            }
            // defensively copy if requested
            return cloneArray ? interfaces.clone() : interfaces;
        }
    }
    */

    @FastNative
    private native Class<?>[] getInterfacesInternal();


    /**
     * Returns the {@code Type}s representing the interfaces
     * directly implemented by the class or interface represented by
     * this object.
     *
     * <p>If a superinterface is a parameterized type, the
     * {@code Type} object returned for it must accurately reflect
     * the actual type parameters used in the source code. The
     * parameterized type representing each superinterface is created
     * if it had not been created before. See the declaration of
     * {@link java.lang.reflect.ParameterizedType ParameterizedType}
     * for the semantics of the creation process for parameterized
     * types.
     *
     * <p>If this object represents a class, the return value is an array
     * containing objects representing all interfaces directly implemented by
     * the class.  The order of the interface objects in the array corresponds
     * to the order of the interface names in the {@code implements} clause of
     * the declaration of the class represented by this object.
     *
     * <p>If this object represents an interface, the array contains objects
     * representing all interfaces directly extended by the interface.  The
     * order of the interface objects in the array corresponds to the order of
     * the interface names in the {@code extends} clause of the declaration of
     * the interface represented by this object.
     *
     * <p>If this object represents a class or interface that implements no
     * interfaces, the method returns an array of length 0.
     *
     * <p>If this object represents a primitive type or void, the method
     * returns an array of length 0.
     *
     * <p>If this {@code Class} object represents an array type, the
     * interfaces {@code Cloneable} and {@code java.io.Serializable} are
     * returned in that order.
     *
     * @throws java.lang.reflect.GenericSignatureFormatError
     *     if the generic class signature does not conform to the format
     *     specified in
     *     <cite>The Java&trade; Virtual Machine Specification</cite>
     * @throws TypeNotPresentException if any of the generic
     *     superinterfaces refers to a non-existent type declaration
     * @throws java.lang.reflect.MalformedParameterizedTypeException
     *     if any of the generic superinterfaces refer to a parameterized
     *     type that cannot be instantiated for any reason
     * @return an array of interfaces directly implemented by this class
     * @since 1.5
     */
    public Type[] getGenericInterfaces() {
        Type[] result;
        synchronized (Caches.genericInterfaces) {
            result = Caches.genericInterfaces.get(this);
            if (result == null) {
                String annotationSignature = getSignatureAttribute();
                if (annotationSignature == null) {
                    result = getInterfaces();
                } else {
                    GenericSignatureParser parser = new GenericSignatureParser(getClassLoader());
                    parser.parseForClass(this, annotationSignature);
                    result = Types.getTypeArray(parser.interfaceTypes, false);
                }
                Caches.genericInterfaces.put(this, result);
            }
        }
        return (result.length == 0) ? result : result.clone();
    }


    /**
     * Returns the {@code Class} representing the component type of an
     * array.  If this class does not represent an array class this method
     * returns null.
     *
     * @return the {@code Class} representing the component type of this
     * class if this class is an array
     * @see     java.lang.reflect.Array
     * @since 1.1
     */
    public Class<?> getComponentType() {
        // Android-changed: Android can return componentType field directly.
        /*
        if (isArray()) {
            return componentType;
        } else {
            return null;
        }
        */
      return componentType;
    }
    /**
     * Returns the component type of this {@code Class}, if it describes
     * an array type, or {@code null} otherwise.
     *
     * @implSpec
     * Equivalent to {@link Class#getComponentType()}.
     *
     * @return a {@code Class} describing the component type, or {@code null}
     * if this {@code Class} does not describe an array type
     * @since 12
     */
    @Override
    public Class<?> componentType() {
        return isArray() ? componentType : null;
    }

    /**
     * Returns a {@code Class} for an array type whose component type
     * is described by this {@linkplain Class}.
     *
     * @return a {@code Class} describing the array type
     * @since 12
     */
    @Override
    public Class<?> arrayType() {
        return Array.newInstance(this, 0).getClass();
    }

    // Android-changed: Remove Class#isHidden() and ClassDesc from javadoc.
    /**
     * Returns the descriptor string of the entity (class, interface, array class,
     * primitive type, or {@code void}) represented by this {@code Class} object.
     *
     * <p> If this {@code Class} object represents a class or interface,
     * not an array class, then:
     * <ul>
     * <li> The result is a field descriptor (JVMS {@jvms 4.3.2})
     *      for the class or interface.
     * </ul>
     *
     * <p> If this {@code Class} object represents an array class, then
     * the result is a string consisting of one or more '{@code [}' characters
     * representing the depth of the array nesting, followed by the
     * descriptor string of the element type.
     * <ul>
     * <li> This array class can be described nominally.
     * </ul>
     *
     * <p> If this {@code Class} object represents a primitive type or
     * {@code void}, then the result is a field descriptor string which
     * is a one-letter code corresponding to a primitive type or {@code void}
     * ({@code "B", "C", "D", "F", "I", "J", "S", "Z", "V"}) (JVMS {@jvms 4.3.2}).
     *
     * @apiNote
     * This is not a strict inverse of {@link #forName};
     * distinct classes which share a common name but have different class loaders
     * will have identical descriptor strings.
     *
     * @return the descriptor string for this {@code Class} object
     * @jvms 4.3.2 Field Descriptors
     * @since 12
     */
    @Override
    public String descriptorString() {
        if (isPrimitive())
            return Wrapper.forPrimitiveType(this).basicTypeString();

        if (isArray()) {
            return "[" + componentType.descriptorString();
        // Android-changed: Remove check for isHidden()
        /*
        } else if (isHidden()) {
            String name = getName();
            int index = name.indexOf('/');
            return new StringBuilder(name.length() + 2)
                    .append('L')
                    .append(name.substring(0, index).replace('.', '/'))
                    .append('.')
                    .append(name, index + 1, name.length())
                    .append(';')
                    .toString();
        */
        } else {
            String name = getName().replace('.', '/');
            return new StringBuilder(name.length() + 2)
                    .append('L')
                    .append(name)
                    .append(';')
                    .toString();
        }
    }

    /**
     * Returns the Java language modifiers for this class or interface, encoded
     * in an integer. The modifiers consist of the Java Virtual Machine's
     * constants for {@code public}, {@code protected},
     * {@code private}, {@code final}, {@code static},
     * {@code abstract} and {@code interface}; they should be decoded
     * using the methods of class {@code Modifier}.
     *
     * <p> If the underlying class is an array class, then its
     * {@code public}, {@code private} and {@code protected}
     * modifiers are the same as those of its component type.  If this
     * {@code Class} represents a primitive type or void, its
     * {@code public} modifier is always {@code true}, and its
     * {@code protected} and {@code private} modifiers are always
     * {@code false}. If this object represents an array class, a
     * primitive type or void, then its {@code final} modifier is always
     * {@code true} and its interface modifier is always
     * {@code false}. The values of its other modifiers are not determined
     * by this specification.
     *
     * <p> The modifier encodings are defined in <em>The Java Virtual Machine
     * Specification</em>, table 4.1.
     *
     * @return the {@code int} representing the modifiers for this class
     * @see     java.lang.reflect.Modifier
     * @since 1.1
     */
    @HotSpotIntrinsicCandidate
    // Android-changed: Android can use a simple java implementation.
    // public native int getModifiers();
    public int getModifiers() {
        // Array classes inherit modifiers from their component types, but in the case of arrays
        // of an inner class, the class file may contain "fake" access flags because it's not valid
        // for a top-level class to private, say. The real access flags are stored in the InnerClass
        // attribute, so we need to make sure we drill down to the inner class: the accessFlags
        // field is not the value we want to return, and the synthesized array class does not itself
        // have an InnerClass attribute. https://code.google.com/p/android/issues/detail?id=56267
        if (isArray()) {
            int componentModifiers = getComponentType().getModifiers();
            if ((componentModifiers & Modifier.INTERFACE) != 0) {
                componentModifiers &= ~(Modifier.INTERFACE | Modifier.STATIC);
            }
            return Modifier.ABSTRACT | Modifier.FINAL | componentModifiers;
        }
        int JAVA_FLAGS_MASK = 0xffff;
        int modifiers = this.getInnerClassFlags(accessFlags & JAVA_FLAGS_MASK);
        return modifiers & JAVA_FLAGS_MASK;
    }

    /**
     * Gets the signers of this class.
     *
     * @return  the signers of this class, or null if there are no signers.  In
     *          particular, this method returns null if this object represents
     *          a primitive type or void.
     * @since   1.1
     */
    public Object[] getSigners() {
        return null;
    }

    @FastNative
    private native Method getEnclosingMethodNative();

    /**
     * If this {@code Class} object represents a local or anonymous
     * class within a method, returns a {@link
     * java.lang.reflect.Method Method} object representing the
     * immediately enclosing method of the underlying class. Returns
     * {@code null} otherwise.
     *
     * In particular, this method returns {@code null} if the underlying
     * class is a local or anonymous class immediately enclosed by a type
     * declaration, instance initializer or static initializer.
     *
     * @return the immediately enclosing method of the underlying class, if
     *     that class is a local or anonymous class; otherwise {@code null}.
     * @since 1.5
     */
    // Android-changed: Removed SecurityException.
    public Method getEnclosingMethod() {
        if (classNameImpliesTopLevel()) {
            return null;
        }
        /*
        else {
            if (!enclosingInfo.isMethod())
                return null;

            MethodRepository typeInfo = MethodRepository.make(enclosingInfo.getDescriptor(),
                                                              getFactory());
            Class<?>   returnType       = toClass(typeInfo.getReturnType());
            Type []    parameterTypes   = typeInfo.getParameterTypes();
            Class<?>[] parameterClasses = new Class<?>[parameterTypes.length];

            // Convert Types to Classes; returned types *should*
            // be class objects since the methodDescriptor's used
            // don't have generics information
            for(int i = 0; i < parameterClasses.length; i++)
                parameterClasses[i] = toClass(parameterTypes[i]);

            // Perform access check
            final Class<?> enclosingCandidate = enclosingInfo.getEnclosingClass();
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                enclosingCandidate.checkMemberAccess(sm, Member.DECLARED,
                                                     Reflection.getCallerClass(), true);
            }
            Method[] candidates = enclosingCandidate.privateGetDeclaredMethods(false);

            /*
             * Loop over all declared methods; match method name,
             * number of and type of parameters, *and* return
             * type.  Matching return type is also necessary
             * because of covariant returns, etc.
             *
            ReflectionFactory fact = getReflectionFactory();
            for (Method m : candidates) {
                if (m.getName().equals(enclosingInfo.getName()) &&
                    arrayContentsEq(parameterClasses,
                                    fact.getExecutableSharedParameterTypes(m))) {
                    // finally, check return type
                    if (m.getReturnType().equals(returnType)) {
                        return fact.copyMethod(m);
                    }
                }
            }

            throw new InternalError("Enclosing method not found");
        }
        */
        return getEnclosingMethodNative();
    }

    // Android-removed: Remove unused getEnclosingMethodInfo().
    /*
    private native Object[] getEnclosingMethod0();

    private EnclosingMethodInfo getEnclosingMethodInfo() {
        Object[] enclosingInfo = getEnclosingMethod0();
        if (enclosingInfo == null)
            return null;
        else {
            return new EnclosingMethodInfo(enclosingInfo);
        }
    }

    private static final class EnclosingMethodInfo {
        private final Class<?> enclosingClass;
        private final String name;
        private final String descriptor;

        static void validate(Object[] enclosingInfo) {
            if (enclosingInfo.length != 3)
                throw new InternalError("Malformed enclosing method information");
            try {
                // The array is expected to have three elements:

                // the immediately enclosing class
                Class<?> enclosingClass = (Class<?>)enclosingInfo[0];
                assert(enclosingClass != null);

                // the immediately enclosing method or constructor's
                // name (can be null).
                String name = (String)enclosingInfo[1];

                // the immediately enclosing method or constructor's
                // descriptor (null iff name is).
                String descriptor = (String)enclosingInfo[2];
                assert((name != null && descriptor != null) || name == descriptor);
            } catch (ClassCastException cce) {
                throw new InternalError("Invalid type in enclosing method information", cce);
            }
        }

        EnclosingMethodInfo(Object[] enclosingInfo) {
            validate(enclosingInfo);
            this.enclosingClass = (Class<?>)enclosingInfo[0];
            this.name = (String)enclosingInfo[1];
            this.descriptor = (String)enclosingInfo[2];
        }

        boolean isPartial() {
            return enclosingClass == null || name == null || descriptor == null;
        }

        boolean isConstructor() { return !isPartial() && "<init>".equals(name); }

        boolean isMethod() { return !isPartial() && !isConstructor() && !"<clinit>".equals(name); }

        Class<?> getEnclosingClass() { return enclosingClass; }

        String getName() { return name; }

        String getDescriptor() { return descriptor; }

    }

    private static Class<?> toClass(Type o) {
        if (o instanceof GenericArrayType)
            return Array.newInstance(toClass(((GenericArrayType)o).getGenericComponentType()),
                                     0)
                .getClass();
        return (Class<?>)o;
     }
    */

    /**
     * If this {@code Class} object represents a local or anonymous
     * class within a constructor, returns a {@link
     * java.lang.reflect.Constructor Constructor} object representing
     * the immediately enclosing constructor of the underlying
     * class. Returns {@code null} otherwise.  In particular, this
     * method returns {@code null} if the underlying class is a local
     * or anonymous class immediately enclosed by a type declaration,
     * instance initializer or static initializer.
     *
     * @return the immediately enclosing constructor of the underlying class, if
     *     that class is a local or anonymous class; otherwise {@code null}.
     * @since 1.5
     */
    // Android-changed: Removed SecurityException.
    public Constructor<?> getEnclosingConstructor() {
        if (classNameImpliesTopLevel()) {
            return null;
        }
        /*
        else {
            if (!enclosingInfo.isConstructor())
                return null;

            ConstructorRepository typeInfo = ConstructorRepository.make(enclosingInfo.getDescriptor(),
                                                                        getFactory());
            Type []    parameterTypes   = typeInfo.getParameterTypes();
            Class<?>[] parameterClasses = new Class<?>[parameterTypes.length];

            // Convert Types to Classes; returned types *should*
            // be class objects since the methodDescriptor's used
            // don't have generics information
            for(int i = 0; i < parameterClasses.length; i++)
                parameterClasses[i] = toClass(parameterTypes[i]);

            // Perform access check
            final Class<?> enclosingCandidate = enclosingInfo.getEnclosingClass();
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                enclosingCandidate.checkMemberAccess(sm, Member.DECLARED,
                                                     Reflection.getCallerClass(), true);
            }

            Constructor<?>[] candidates = enclosingCandidate
                    .privateGetDeclaredConstructors(false);
            /*
             * Loop over all declared constructors; match number
             * of and type of parameters.
             *
            ReflectionFactory fact = getReflectionFactory();
            for (Constructor<?> c : candidates) {
                if (arrayContentsEq(parameterClasses,
                                    fact.getExecutableSharedParameterTypes(c))) {
                    return fact.copyConstructor(c);
                }
            }

            throw new InternalError("Enclosing constructor not found");

        }
        */
        return getEnclosingConstructorNative();
    }

    @FastNative
    private native Constructor<?> getEnclosingConstructorNative();

    private boolean classNameImpliesTopLevel() {
        return !getName().contains("$");
    }


    /**
     * If the class or interface represented by this {@code Class} object
     * is a member of another class, returns the {@code Class} object
     * representing the class in which it was declared.  This method returns
     * null if this class or interface is not a member of any other class.  If
     * this {@code Class} object represents an array class, a primitive
     * type, or void,then this method returns null.
     *
     * @return the declaring class for this class
     * @since 1.1
     */
    // Android-changed: Removed SecurityException.
    /*
    @CallerSensitive
    public Class<?> getDeclaringClass() throws SecurityException {
        final Class<?> candidate = getDeclaringClass0();

        if (candidate != null) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                candidate.checkPackageAccess(sm,
                    ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
            }
        }
        return candidate;
    }

    private native Class<?> getDeclaringClass0();
    */
    @FastNative
    public native Class<?> getDeclaringClass();

    /**
     * Returns the immediately enclosing class of the underlying
     * class.  If the underlying class is a top level class this
     * method returns {@code null}.
     * @return the immediately enclosing class of the underlying class
     * @since 1.5
     */
    // Android-changed: Removed SecurityException.
    /*
    @CallerSensitive
    public Class<?> getEnclosingClass() throws SecurityException {
        // There are five kinds of classes (or interfaces):
        // a) Top level classes
        // b) Nested classes (static member classes)
        // c) Inner classes (non-static member classes)
        // d) Local classes (named classes declared within a method)
        // e) Anonymous classes


        // JVM Spec 4.7.7: A class must have an EnclosingMethod
        // attribute if and only if it is a local class or an
        // anonymous class.
        EnclosingMethodInfo enclosingInfo = getEnclosingMethodInfo();
        Class<?> enclosingCandidate;

        if (enclosingInfo == null) {
            // This is a top level or a nested class or an inner class (a, b, or c)
            enclosingCandidate = getDeclaringClass0();
        } else {
            Class<?> enclosingClass = enclosingInfo.getEnclosingClass();
            // This is a local class or an anonymous class (d or e)
            if (enclosingClass == this || enclosingClass == null)
                throw new InternalError("Malformed enclosing method information");
            else
                enclosingCandidate = enclosingClass;
        }

        if (enclosingCandidate != null) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                enclosingCandidate.checkPackageAccess(sm,
                    ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
            }
        }
        return enclosingCandidate;
    }
    */
    @FastNative
    public native Class<?> getEnclosingClass();

    /**
     * Returns the simple name of the underlying class as given in the
     * source code. Returns an empty string if the underlying class is
     * anonymous.
     *
     * <p>The simple name of an array is the simple name of the
     * component type with "[]" appended.  In particular the simple
     * name of an array whose component type is anonymous is "[]".
     *
     * @return the simple name of the underlying class
     * @since 1.5
     */
    public String getSimpleName() {
        // Android-changed: ART has a different JNI layer.
        if (isArray())
            return getComponentType().getSimpleName()+"[]";

        if (isAnonymousClass()) {
            return "";
        }

        if (isMemberClass() || isLocalClass()) {
            // Note that we obtain this information from getInnerClassName(), which uses
            // dex system annotations to obtain the name. It is possible for this information
            // to disagree with the actual enclosing class name. For example, if dex
            // manipulation tools have renamed the enclosing class without adjusting
            // the system annotation to match. See http://b/28800927.
            return getInnerClassName();
        }

        String simpleName = getName();
        final int dot = simpleName.lastIndexOf(".");
        if (dot > 0) {
            return simpleName.substring(simpleName.lastIndexOf(".")+1); // strip the package name
        }

        return simpleName;
    }

    /**
     * Return an informative string for the name of this type.
     *
     * @return an informative string for the name of this type
     * @since 1.8
     */
    public String getTypeName() {
        if (isArray()) {
            try {
                Class<?> cl = this;
                int dimensions = 0;
                do {
                    dimensions++;
                    cl = cl.getComponentType();
                } while (cl.isArray());
                StringBuilder sb = new StringBuilder();
                sb.append(cl.getName());
                for (int i = 0; i < dimensions; i++) {
                    sb.append("[]");
                }
                return sb.toString();
            } catch (Throwable e) { /*FALLTHRU*/ }
        }
        return getName();
    }

    /**
     * Returns the canonical name of the underlying class as
     * defined by the Java Language Specification.  Returns null if
     * the underlying class does not have a canonical name (i.e., if
     * it is a local or anonymous class or an array whose component
     * type does not have a canonical name).
     * @return the canonical name of the underlying class if it exists, and
     * {@code null} otherwise.
     * @since 1.5
     */
    public String getCanonicalName() {
        // Android-changed: Android has no ReflectionData class.
        if (isArray()) {
            String canonicalName = getComponentType().getCanonicalName();
            if (canonicalName != null)
                return canonicalName + "[]";
            else
                return null;
        }
        if (isLocalOrAnonymousClass())
            return null;
        Class<?> enclosingClass = getEnclosingClass();
        if (enclosingClass == null) { // top level class
            return getName();
        } else {
            String enclosingName = enclosingClass.getCanonicalName();
            if (enclosingName == null)
                return null;
            return enclosingName + "." + getSimpleName();
        }
    }

    /**
     * Returns {@code true} if and only if the underlying class
     * is an anonymous class.
     *
     * @return {@code true} if and only if this class is an anonymous class.
     * @since 1.5
     */
    // Android-changed: ART has a different JNI layer.
    /*
    public boolean isAnonymousClass() {
        return !isArray() && isLocalOrAnonymousClass() &&
                getSimpleBinaryName0() == null;
    }
    */
    @FastNative
    public native boolean isAnonymousClass();

    /**
     * Returns {@code true} if and only if the underlying class
     * is a local class.
     *
     * @return {@code true} if and only if this class is a local class.
     * @since 1.5
     */
    public boolean isLocalClass() {
        // Android-changed: ART has a different JNI layer.
        // return isLocalOrAnonymousClass() &&
        //     (isArray() || getSimpleBinaryName0() != null);
        return (getEnclosingMethod() != null || getEnclosingConstructor() != null)
                && !isAnonymousClass();
    }

    /**
     * Returns {@code true} if and only if the underlying class
     * is a member class.
     *
     * @return {@code true} if and only if this class is a member class.
     * @since 1.5
     */
    public boolean isMemberClass() {
        // Android-changed: ART has a different JNI layer.
        // return !isLocalOrAnonymousClass() && getDeclaringClass0() != null;
        return !isLocalOrAnonymousClass() && getDeclaringClass() != null;
    }

    /**
     * Returns {@code true} if this is a top level class.  Returns {@code false}
     * otherwise.
     */
    private boolean isTopLevelClass() {
        // Android-changed: ART has a different JNI layer.
        // return !isLocalOrAnonymousClass() && getDeclaringClass0() == null;
        return !isLocalOrAnonymousClass() && getDeclaringClass() == null;
    }

    /**
     * Returns {@code true} if this is a local class or an anonymous
     * class.  Returns {@code false} otherwise.
     */
    private boolean isLocalOrAnonymousClass() {
        // Android-changed: ART has a different JNI layer.
        /*
        // JVM Spec 4.7.7: A class must have an EnclosingMethod
        // attribute if and only if it is a local class or an
        // anonymous class.
        return hasEnclosingMethodInfo();
        */
        return isLocalClass() || isAnonymousClass();
    }

    /**
     * Returns an array containing {@code Class} objects representing all
     * the public classes and interfaces that are members of the class
     * represented by this {@code Class} object.  This includes public
     * class and interface members inherited from superclasses and public class
     * and interface members declared by the class.  This method returns an
     * array of length 0 if this {@code Class} object has no public member
     * classes or interfaces.  This method also returns an array of length 0 if
     * this {@code Class} object represents a primitive type, an array
     * class, or void.
     *
     * @return the array of {@code Class} objects representing the public
     *         members of this class
     *
     * @since 1.1
     */
    @CallerSensitive
    public Class<?>[] getClasses() {
        // Android-changed: Removed SecurityManager check.
        List<Class<?>> result = new ArrayList<Class<?>>();
        for (Class<?> c = this; c != null; c = c.superClass) {
            for (Class<?> member : c.getDeclaredClasses()) {
                if (Modifier.isPublic(member.getModifiers())) {
                    result.add(member);
                }
            }
        }
        return result.toArray(new Class[result.size()]);
    }


    /**
     * Returns an array containing {@code Field} objects reflecting all
     * the accessible public fields of the class or interface represented by
     * this {@code Class} object.
     *
     * <p> If this {@code Class} object represents a class or interface with
     * no accessible public fields, then this method returns an array of length
     * 0.
     *
     * <p> If this {@code Class} object represents a class, then this method
     * returns the public fields of the class and of all its superclasses and
     * superinterfaces.
     *
     * <p> If this {@code Class} object represents an interface, then this
     * method returns the fields of the interface and of all its
     * superinterfaces.
     *
     * <p> If this {@code Class} object represents an array type, a primitive
     * type, or void, then this method returns an array of length 0.
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * @return the array of {@code Field} objects representing the
     *         public fields
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since 1.1
     * @jls 8.2 Class Members
     * @jls 8.3 Field Declarations
     */
    @CallerSensitive
    public Field[] getFields() throws SecurityException {
        // Android-changed: Removed SecurityManager check.
        List<Field> fields = new ArrayList<Field>();
        getPublicFieldsRecursive(fields);
        return fields.toArray(new Field[fields.size()]);
    }

    /**
     * Populates {@code result} with public fields defined by this class, its
     * superclasses, and all implemented interfaces.
     */
    private void getPublicFieldsRecursive(List<Field> result) {
        // search superclasses
        for (Class<?> c = this; c != null; c = c.superClass) {
            Collections.addAll(result, c.getPublicDeclaredFields());
        }

        // search iftable which has a flattened and uniqued list of interfaces
        Object[] iftable = ifTable;
        if (iftable != null) {
            for (int i = 0; i < iftable.length; i += 2) {
                Collections.addAll(result, ((Class<?>) iftable[i]).getPublicDeclaredFields());
            }
        }
    }

    /**
     * Returns an array containing {@code Method} objects reflecting all the
     * public methods of the class or interface represented by this {@code
     * Class} object, including those declared by the class or interface and
     * those inherited from superclasses and superinterfaces.
     *
     * <p> If this {@code Class} object represents an array type, then the
     * returned array has a {@code Method} object for each of the public
     * methods inherited by the array type from {@code Object}. It does not
     * contain a {@code Method} object for {@code clone()}.
     *
     * <p> If this {@code Class} object represents an interface then the
     * returned array does not contain any implicitly declared methods from
     * {@code Object}. Therefore, if no methods are explicitly declared in
     * this interface or any of its superinterfaces then the returned array
     * has length 0. (Note that a {@code Class} object which represents a class
     * always has public methods, inherited from {@code Object}.)
     *
     * <p> The returned array never contains methods with names "{@code <init>}"
     * or "{@code <clinit>}".
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * <p> Generally, the result is computed as with the following 4 step algorithm.
     * Let C be the class or interface represented by this {@code Class} object:
     * <ol>
     * <li> A union of methods is composed of:
     *   <ol type="a">
     *   <li> C's declared public instance and static methods as returned by
     *        {@link #getDeclaredMethods()} and filtered to include only public
     *        methods.</li>
     *   <li> If C is a class other than {@code Object}, then include the result
     *        of invoking this algorithm recursively on the superclass of C.</li>
     *   <li> Include the results of invoking this algorithm recursively on all
     *        direct superinterfaces of C, but include only instance methods.</li>
     *   </ol></li>
     * <li> Union from step 1 is partitioned into subsets of methods with same
     *      signature (name, parameter types) and return type.</li>
     * <li> Within each such subset only the most specific methods are selected.
     *      Let method M be a method from a set of methods with same signature
     *      and return type. M is most specific if there is no such method
     *      N != M from the same set, such that N is more specific than M.
     *      N is more specific than M if:
     *   <ol type="a">
     *   <li> N is declared by a class and M is declared by an interface; or</li>
     *   <li> N and M are both declared by classes or both by interfaces and
     *        N's declaring type is the same as or a subtype of M's declaring type
     *        (clearly, if M's and N's declaring types are the same type, then
     *        M and N are the same method).</li>
     *   </ol></li>
     * <li> The result of this algorithm is the union of all selected methods from
     *      step 3.</li>
     * </ol>
     *
     * @apiNote There may be more than one method with a particular name
     * and parameter types in a class because while the Java language forbids a
     * class to declare multiple methods with the same signature but different
     * return types, the Java virtual machine does not.  This
     * increased flexibility in the virtual machine can be used to
     * implement various language features.  For example, covariant
     * returns can be implemented with {@linkplain
     * java.lang.reflect.Method#isBridge bridge methods}; the bridge
     * method and the overriding method would have the same
     * signature but different return types.
     *
     * @return the array of {@code Method} objects representing the
     *         public methods of this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @since 1.1
     */
    @CallerSensitive
    public Method[] getMethods() throws SecurityException {
        // Android-changed: Removed SecurityManager check.
        List<Method> methods = new ArrayList<Method>();
        getPublicMethodsInternal(methods);
        /*
         * Remove duplicate methods defined by superclasses and
         * interfaces, preferring to keep methods declared by derived
         * types.
         */
        CollectionUtils.removeDuplicates(methods, Method.ORDER_BY_SIGNATURE);
        return methods.toArray(new Method[methods.size()]);
    }

    /**
     * Populates {@code result} with public methods defined by this class, its
     * superclasses, and all implemented interfaces, including overridden methods.
     */
    private void getPublicMethodsInternal(List<Method> result) {
        Collections.addAll(result, getDeclaredMethodsUnchecked(true));
        if (!isInterface()) {
            // Search superclasses, for interfaces don't search java.lang.Object.
            for (Class<?> c = superClass; c != null; c = c.superClass) {
                Collections.addAll(result, c.getDeclaredMethodsUnchecked(true));
            }
        }
        // Search iftable which has a flattened and uniqued list of interfaces.
        Object[] iftable = ifTable;
        if (iftable != null) {
            for (int i = 0; i < iftable.length; i += 2) {
                Class<?> ifc = (Class<?>) iftable[i];
                Collections.addAll(result, ifc.getDeclaredMethodsUnchecked(true));
            }
        }
    }

    /**
     * Returns an array containing {@code Constructor} objects reflecting
     * all the public constructors of the class represented by this
     * {@code Class} object.  An array of length 0 is returned if the
     * class has no public constructors, or if the class is an array class, or
     * if the class reflects a primitive type or void.
     *
     * Note that while this method returns an array of {@code
     * Constructor<T>} objects (that is an array of constructors from
     * this class), the return type of this method is {@code
     * Constructor<?>[]} and <em>not</em> {@code Constructor<T>[]} as
     * might be expected.  This less informative return type is
     * necessary since after being returned from this method, the
     * array could be modified to hold {@code Constructor} objects for
     * different classes, which would violate the type guarantees of
     * {@code Constructor<T>[]}.
     *
     * @return the array of {@code Constructor} objects representing the
     *         public constructors of this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since 1.1
     */
    @CallerSensitive
    public Constructor<?>[] getConstructors() throws SecurityException {
        // Android-changed: Removed SecurityManager check.
        return getDeclaredConstructorsInternal(true);
    }


    /**
     * Returns a {@code Field} object that reflects the specified public member
     * field of the class or interface represented by this {@code Class}
     * object. The {@code name} parameter is a {@code String} specifying the
     * simple name of the desired field.
     *
     * <p> The field to be reflected is determined by the algorithm that
     * follows.  Let C be the class or interface represented by this object:
     *
     * <OL>
     * <LI> If C declares a public field with the name specified, that is the
     *      field to be reflected.</LI>
     * <LI> If no field was found in step 1 above, this algorithm is applied
     *      recursively to each direct superinterface of C. The direct
     *      superinterfaces are searched in the order they were declared.</LI>
     * <LI> If no field was found in steps 1 and 2 above, and C has a
     *      superclass S, then this algorithm is invoked recursively upon S.
     *      If C has no superclass, then a {@code NoSuchFieldException}
     *      is thrown.</LI>
     * </OL>
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method does not find the {@code length} field of the array type.
     *
     * @param name the field name
     * @return the {@code Field} object of this class specified by
     *         {@code name}
     * @throws NoSuchFieldException if a field with the specified name is
     *         not found.
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since 1.1
     * @jls 8.2 Class Members
     * @jls 8.3 Field Declarations
     */
    // Android-changed: Removed SecurityException.
    public Field getField(String name) throws NoSuchFieldException {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        Field result = getPublicFieldRecursive(name);
        if (result == null) {
            throw new NoSuchFieldException(name);
        }
        return result;
    }

    /**
     * The native implementation of the {@code getField} method.
     *
     * @throws NullPointerException
     *            if name is null.
     * @see #getField(String)
     */
    @FastNative
    private native Field getPublicFieldRecursive(String name);

    /**
     * Returns a {@code Method} object that reflects the specified public
     * member method of the class or interface represented by this
     * {@code Class} object. The {@code name} parameter is a
     * {@code String} specifying the simple name of the desired method. The
     * {@code parameterTypes} parameter is an array of {@code Class}
     * objects that identify the method's formal parameter types, in declared
     * order. If {@code parameterTypes} is {@code null}, it is
     * treated as if it were an empty array.
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method finds any public method inherited by the array type from
     * {@code Object} except method {@code clone()}.
     *
     * <p> If this {@code Class} object represents an interface then this
     * method does not find any implicitly declared method from
     * {@code Object}. Therefore, if no methods are explicitly declared in
     * this interface or any of its superinterfaces, then this method does not
     * find any method.
     *
     * <p> This method does not find any method with name "{@code <init>}" or
     * "{@code <clinit>}".
     *
     * <p> Generally, the method to be reflected is determined by the 4 step
     * algorithm that follows.
     * Let C be the class or interface represented by this {@code Class} object:
     * <ol>
     * <li> A union of methods is composed of:
     *   <ol type="a">
     *   <li> C's declared public instance and static methods as returned by
     *        {@link #getDeclaredMethods()} and filtered to include only public
     *        methods that match given {@code name} and {@code parameterTypes}</li>
     *   <li> If C is a class other than {@code Object}, then include the result
     *        of invoking this algorithm recursively on the superclass of C.</li>
     *   <li> Include the results of invoking this algorithm recursively on all
     *        direct superinterfaces of C, but include only instance methods.</li>
     *   </ol></li>
     * <li> This union is partitioned into subsets of methods with same
     *      return type (the selection of methods from step 1 also guarantees that
     *      they have the same method name and parameter types).</li>
     * <li> Within each such subset only the most specific methods are selected.
     *      Let method M be a method from a set of methods with same VM
     *      signature (return type, name, parameter types).
     *      M is most specific if there is no such method N != M from the same
     *      set, such that N is more specific than M. N is more specific than M
     *      if:
     *   <ol type="a">
     *   <li> N is declared by a class and M is declared by an interface; or</li>
     *   <li> N and M are both declared by classes or both by interfaces and
     *        N's declaring type is the same as or a subtype of M's declaring type
     *        (clearly, if M's and N's declaring types are the same type, then
     *        M and N are the same method).</li>
     *   </ol></li>
     * <li> The result of this algorithm is chosen arbitrarily from the methods
     *      with most specific return type among all selected methods from step 3.
     *      Let R be a return type of a method M from the set of all selected methods
     *      from step 3. M is a method with most specific return type if there is
     *      no such method N != M from the same set, having return type S != R,
     *      such that S is a subtype of R as determined by
     *      R.class.{@link #isAssignableFrom}(S.class).
     * </ol>
     *
     * @apiNote There may be more than one method with matching name and
     * parameter types in a class because while the Java language forbids a
     * class to declare multiple methods with the same signature but different
     * return types, the Java virtual machine does not.  This
     * increased flexibility in the virtual machine can be used to
     * implement various language features.  For example, covariant
     * returns can be implemented with {@linkplain
     * java.lang.reflect.Method#isBridge bridge methods}; the bridge
     * method and the overriding method would have the same
     * signature but different return types. This method would return the
     * overriding method as it would have a more specific return type.
     *
     * @param name the name of the method
     * @param parameterTypes the list of parameters
     * @return the {@code Method} object that matches the specified
     *         {@code name} and {@code parameterTypes}
     * @throws NoSuchMethodException if a matching method is not found
     *         or if the name is "&lt;init&gt;"or "&lt;clinit&gt;".
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @since 1.1
     */
    @CallerSensitive
    public Method getMethod(String name, Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        // Android-changed: Removed SecurityManager check.
        return getMethod(name, parameterTypes, true);
    }


    /**
     * Returns a {@code Constructor} object that reflects the specified
     * public constructor of the class represented by this {@code Class}
     * object. The {@code parameterTypes} parameter is an array of
     * {@code Class} objects that identify the constructor's formal
     * parameter types, in declared order.
     *
     * If this {@code Class} object represents an inner class
     * declared in a non-static context, the formal parameter types
     * include the explicit enclosing instance as the first parameter.
     *
     * <p> The constructor to reflect is the public constructor of the class
     * represented by this {@code Class} object whose formal parameter
     * types match those specified by {@code parameterTypes}.
     *
     * @param parameterTypes the parameter array
     * @return the {@code Constructor} object of the public constructor that
     *         matches the specified {@code parameterTypes}
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since 1.1
     */
    public Constructor<T> getConstructor(Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        // Android-changed: Removed SecurityManager check.
        return getConstructor0(parameterTypes, Member.PUBLIC);
    }


    /**
     * Returns an array of {@code Class} objects reflecting all the
     * classes and interfaces declared as members of the class represented by
     * this {@code Class} object. This includes public, protected, default
     * (package) access, and private classes and interfaces declared by the
     * class, but excludes inherited classes and interfaces.  This method
     * returns an array of length 0 if the class declares no classes or
     * interfaces as members, or if this {@code Class} object represents a
     * primitive type, an array class, or void.
     *
     * @return the array of {@code Class} objects representing all the
     *         declared members of this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and any of the
     *         following conditions is met:
     *
     *         <ul>
     *
     *         <li> the caller's class loader is not the same as the
     *         class loader of this class and invocation of
     *         {@link SecurityManager#checkPermission
     *         s.checkPermission} method with
     *         {@code RuntimePermission("accessDeclaredMembers")}
     *         denies access to the declared classes within this class
     *
     *         <li> the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class
     *
     *         </ul>
     *
     * @since 1.1
     */
    // Android-changed: Removed SecurityException.
    /*
    @CallerSensitive
    public Class<?>[] getDeclaredClasses() throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), false);
        }
        return getDeclaredClasses0();
    }
    */
    @FastNative
    public native Class<?>[] getDeclaredClasses();

    /**
     * Returns an array of {@code Field} objects reflecting all the fields
     * declared by the class or interface represented by this
     * {@code Class} object. This includes public, protected, default
     * (package) access, and private fields, but excludes inherited fields.
     *
     * <p> If this {@code Class} object represents a class or interface with no
     * declared fields, then this method returns an array of length 0.
     *
     * <p> If this {@code Class} object represents an array type, a primitive
     * type, or void, then this method returns an array of length 0.
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * @return  the array of {@code Field} objects representing all the
     *          declared fields of this class
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared fields within this class
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @since 1.1
     * @jls 8.2 Class Members
     * @jls 8.3 Field Declarations
     */
    // Android-changed: Removed SecurityException.
    /*
    @CallerSensitive
    public Field[] getDeclaredFields() throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        return copyFields(privateGetDeclaredFields(false));
    }
    */
    @FastNative
    public native Field[] getDeclaredFields();

    /**
     * Returns an array of {@code RecordComponent} objects representing all the
     * record components of this record class, or {@code null} if this class is
     * not a record class.
     *
     * <p> The components are returned in the same order that they are declared
     * in the record header. The array is empty if this record class has no
     * components. If the class is not a record class, that is {@link
     * #isRecord()} returns {@code false}, then this method returns {@code null}.
     * Conversely, if {@link #isRecord()} returns {@code true}, then this method
     * returns a non-null value.
     *
     * @apiNote
     * <p> The following method can be used to find the record canonical constructor:
     *
     * <pre>{@code
     * static <T extends Record> Constructor<T> getCanonicalConstructor(Class<T> cls)
     *     throws NoSuchMethodException {
     *   Class<?>[] paramTypes =
     *     Arrays.stream(cls.getRecordComponents())
     *           .map(RecordComponent::getType)
     *           .toArray(Class<?>[]::new);
     *   return cls.getDeclaredConstructor(paramTypes);
     * }}</pre>
     *
     * @return  An array of {@code RecordComponent} objects representing all the
     *          record components of this record class, or {@code null} if this
     *          class is not a record class
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared methods within this class
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @jls 8.10 Record Classes
     * @since 16
     */
    @CallerSensitive
    public RecordComponent[] getRecordComponents() {
        // Android-removed: Android doesn't support SecurityManager.
        /*
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        */
        if (!isRecord()) {
            return null;
        }
        return getRecordComponents0();
    }

    /**
     * Populates a list of fields without performing any security or type
     * resolution checks first. If no fields exist, the list is not modified.
     *
     * @param publicOnly Whether to return only public fields.
     * @hide
     */
    @FastNative
    public native Field[] getDeclaredFieldsUnchecked(boolean publicOnly);

    /**
     * Returns an array containing {@code Method} objects reflecting all the
     * declared methods of the class or interface represented by this {@code
     * Class} object, including public, protected, default (package)
     * access, and private methods, but excluding inherited methods.
     *
     * <p> If this {@code Class} object represents a type that has multiple
     * declared methods with the same name and parameter types, but different
     * return types, then the returned array has a {@code Method} object for
     * each such method.
     *
     * <p> If this {@code Class} object represents a type that has a class
     * initialization method {@code <clinit>}, then the returned array does
     * <em>not</em> have a corresponding {@code Method} object.
     *
     * <p> If this {@code Class} object represents a class or interface with no
     * declared methods, then the returned array has length 0.
     *
     * <p> If this {@code Class} object represents an array type, a primitive
     * type, or void, then the returned array has length 0.
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * @return  the array of {@code Method} objects representing all the
     *          declared methods of this class
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared methods within this class
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @since 1.1
     */
    public Method[] getDeclaredMethods() throws SecurityException {
        // Android-changed: Removed SecurityManager check.
        Method[] result = getDeclaredMethodsUnchecked(false);
        for (Method m : result) {
            // Throw NoClassDefFoundError if types cannot be resolved.
            m.getReturnType();
            m.getParameterTypes();
        }
        return result;
    }

    /**
     * Populates a list of methods without performing any security or type
     * resolution checks first. If no methods exist, the list is not modified.
     *
     * @param publicOnly Whether to return only public methods.
     * @hide
     */
    @FastNative
    public native Method[] getDeclaredMethodsUnchecked(boolean publicOnly);

    /**
     * Returns an array of {@code Constructor} objects reflecting all the
     * constructors declared by the class represented by this
     * {@code Class} object. These are public, protected, default
     * (package) access, and private constructors.  The elements in the array
     * returned are not sorted and are not in any particular order.  If the
     * class has a default constructor, it is included in the returned array.
     * This method returns an array of length 0 if this {@code Class}
     * object represents an interface, a primitive type, an array class, or
     * void.
     *
     * <p> See <em>The Java Language Specification</em>, section 8.2.
     *
     * @return  the array of {@code Constructor} objects representing all the
     *          declared constructors of this class
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared constructors within this class
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @since 1.1
     */
    public Constructor<?>[] getDeclaredConstructors() throws SecurityException {
        // Android-changed: Removed SecurityManager check.
        return getDeclaredConstructorsInternal(false);
    }


    /**
     * Returns the constructor with the given parameters if it is defined by this class;
     * {@code null} otherwise. This may return a non-public member.
     */
    @FastNative
    private native Constructor<?>[] getDeclaredConstructorsInternal(boolean publicOnly);

    /**
     * Returns a {@code Field} object that reflects the specified declared
     * field of the class or interface represented by this {@code Class}
     * object. The {@code name} parameter is a {@code String} that specifies
     * the simple name of the desired field.
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method does not find the {@code length} field of the array type.
     *
     * @param name the name of the field
     * @return  the {@code Field} object for the specified field in this
     *          class
     * @throws  NoSuchFieldException if a field with the specified name is
     *          not found.
     * @throws  NullPointerException if {@code name} is {@code null}
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared field
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @since 1.1
     * @jls 8.2 Class Members
     * @jls 8.3 Field Declarations
     */
    // Android-changed: ART has a different JNI layer.
    // Android-changed: Removed SecurityException.
    /*
    @CallerSensitive
    public Field getDeclaredField(String name)
        throws NoSuchFieldException, SecurityException {
        Objects.requireNonNull(name);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        Field field = searchFields(privateGetDeclaredFields(false), name);
        if (field == null) {
            throw new NoSuchFieldException(name);
        }
        return getReflectionFactory().copyField(field);
    }
     */
    @FastNative
    public native Field getDeclaredField(String name) throws NoSuchFieldException;

    /**
     * Returns the subset of getDeclaredFields which are public.
     */
    @FastNative
    private native Field[] getPublicDeclaredFields();

    /**
     * Returns a {@code Method} object that reflects the specified
     * declared method of the class or interface represented by this
     * {@code Class} object. The {@code name} parameter is a
     * {@code String} that specifies the simple name of the desired
     * method, and the {@code parameterTypes} parameter is an array of
     * {@code Class} objects that identify the method's formal parameter
     * types, in declared order.  If more than one method with the same
     * parameter types is declared in a class, and one of these methods has a
     * return type that is more specific than any of the others, that method is
     * returned; otherwise one of the methods is chosen arbitrarily.  If the
     * name is "&lt;init&gt;"or "&lt;clinit&gt;" a {@code NoSuchMethodException}
     * is raised.
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method does not find the {@code clone()} method.
     *
     * @param name the name of the method
     * @param parameterTypes the parameter array
     * @return  the {@code Method} object for the method of this class
     *          matching the specified name and parameters
     * @throws  NoSuchMethodException if a matching method is not found.
     * @throws  NullPointerException if {@code name} is {@code null}
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared method
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @since 1.1
     */
    @CallerSensitive
    public Method getDeclaredMethod(String name, Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        // Android-changed: ART has a different JNI layer.
        return getMethod(name, parameterTypes, false);
    }

    // BEGIN Android-added: Internal methods to implement getMethod(...).
    private Method getMethod(String name, Class<?>[] parameterTypes, boolean recursivePublicMethods)
            throws NoSuchMethodException {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (parameterTypes == null) {
            parameterTypes = EmptyArray.CLASS;
        }
        for (Class<?> c : parameterTypes) {
            if (c == null) {
                throw new NoSuchMethodException("parameter type is null");
            }
        }
        Method result = recursivePublicMethods ? getPublicMethodRecursive(name, parameterTypes)
                                               : getDeclaredMethodInternal(name, parameterTypes);
        // Fail if we didn't find the method or it was expected to be public.
        if (result == null ||
            (recursivePublicMethods && !Modifier.isPublic(result.getAccessFlags()))) {
            throw new NoSuchMethodException(getName() + "." + name + " "
                    + Arrays.toString(parameterTypes));
        }
        return result;
    }
    private Method getPublicMethodRecursive(String name, Class<?>[] parameterTypes) {
        // search superclasses
        for (Class<?> c = this; c != null; c = c.getSuperclass()) {
            Method result = c.getDeclaredMethodInternal(name, parameterTypes);
            if (result != null && Modifier.isPublic(result.getAccessFlags())) {
                return result;
            }
        }

        return findInterfaceMethod(name, parameterTypes);
    }

    /**
     * Returns an instance method that's defined on this class or any super classes, regardless
     * of its access flags. Constructors are excluded.
     *
     * This function does not perform access checks and its semantics don't match any dex byte code
     * instruction or public reflection API. This is used by {@code MethodHandles.findVirtual}
     * which will perform access checks on the returned method.
     *
     * @hide
     */
    public Method getInstanceMethod(String name, Class<?>[] parameterTypes)
            throws NoSuchMethodException, IllegalAccessException {
        for (Class<?> c = this; c != null; c = c.getSuperclass()) {
            Method result = c.getDeclaredMethodInternal(name, parameterTypes);
            if (result != null && !Modifier.isStatic(result.getModifiers())) {
                return result;
            }
        }

        return findInterfaceMethod(name, parameterTypes);
    }

    private Method findInterfaceMethod(String name, Class<?>[] parameterTypes) {
        Object[] iftable = ifTable;
        if (iftable != null) {
            // Search backwards so more specific interfaces are searched first. This ensures that
            // the method we return is not overridden by one of it's subtypes that this class also
            // implements.
            for (int i = iftable.length - 2; i >= 0; i -= 2) {
                Class<?> ifc = (Class<?>) iftable[i];
                Method result = ifc.getPublicMethodRecursive(name, parameterTypes);
                if (result != null && Modifier.isPublic(result.getAccessFlags())) {
                    return result;
                }
            }
        }

        return null;
    }
    // END Android-added: Internal methods to implement getMethod(...).

    // Android-removed: unused method.
    /*
     * Returns the list of {@code Method} objects for the declared public
     * methods of this class or interface that have the specified method name
     * and parameter types.
     *
     * @param name the name of the method
     * @param parameterTypes the parameter array
     * @return the list of {@code Method} objects for the public methods of
     *         this class matching the specified name and parameters
     *
    List<Method> getDeclaredPublicMethods(String name, Class<?>... parameterTypes) {
        Method[] methods = privateGetDeclaredMethods(/* publicOnly * true);
        ReflectionFactory factory = getReflectionFactory();
        List<Method> result = new ArrayList<>();
        for (Method method : methods) {
            if (method.getName().equals(name)
                && Arrays.equals(
                    factory.getExecutableSharedParameterTypes(method),
                    parameterTypes)) {
                result.add(factory.copyMethod(method));
            }
        }
        return result;
    }
    */

    /**
     * Returns a {@code Constructor} object that reflects the specified
     * constructor of the class or interface represented by this
     * {@code Class} object.  The {@code parameterTypes} parameter is
     * an array of {@code Class} objects that identify the constructor's
     * formal parameter types, in declared order.
     *
     * If this {@code Class} object represents an inner class
     * declared in a non-static context, the formal parameter types
     * include the explicit enclosing instance as the first parameter.
     *
     * @param parameterTypes the parameter array
     * @return  The {@code Constructor} object for the constructor with the
     *          specified parameter list
     * @throws  NoSuchMethodException if a matching method is not found.
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared constructor
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @since 1.1
     */
    @CallerSensitive
    public Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        // Android-changed: Removed SecurityManager check.
        return getConstructor0(parameterTypes, Member.DECLARED);
    }

    // Android-changed: Removed javadoc related to Module.
    /**
     * Finds a resource with a given name.
     *
     * <p> The rules for
     * searching resources associated with a given class are implemented by the
     * defining {@linkplain ClassLoader class loader} of the class.  This method
     * delegates to this object's class loader.  If this object was loaded by
     * the bootstrap class loader, the method delegates to {@link
     * ClassLoader#getSystemResourceAsStream}.
     *
     * <p> Before delegation, an absolute resource name is constructed from the
     * given resource name using this algorithm:
     *
     * <ul>
     *
     * <li> If the {@code name} begins with a {@code '/'}
     * (<code>'&#92;u002f'</code>), then the absolute name of the resource is the
     * portion of the {@code name} following the {@code '/'}.
     *
     * <li> Otherwise, the absolute name is of the following form:
     *
     * <blockquote>
     *   {@code modified_package_name/name}
     * </blockquote>
     *
     * <p> Where the {@code modified_package_name} is the package name of this
     * object with {@code '/'} substituted for {@code '.'}
     * (<code>'&#92;u002e'</code>).
     *
     * </ul>
     *
     * @param  name name of the desired resource
     * @return  A {@link java.io.InputStream} object; {@code null} if no
     *          resource with this name is found.
     * @throws  NullPointerException If {@code name} is {@code null}
     * @since  1.1
     * @revised 9
     * @spec JPMS
     */
    @CallerSensitive
    public InputStream getResourceAsStream(String name) {
        name = resolveName(name);
        // unnamed module
        // Android-changed: Removed BuiltinClassLoader, Module usage, and SecurityManager check.
        ClassLoader cl = getClassLoader();
        if (cl == null) {
            return ClassLoader.getSystemResourceAsStream(name);
        } else {
            return cl.getResourceAsStream(name);
        }
    }

    // Android-changed: Remove javadoc related to the module system.
    /**
     * Finds a resource with a given name.
     *
     * <p> The rules for
     * searching resources associated with a given class are implemented by the
     * defining {@linkplain ClassLoader class loader} of the class.  This method
     * delegates to this object's class loader. If this object was loaded by
     * the bootstrap class loader, the method delegates to {@link
     * ClassLoader#getSystemResource}.
     *
     * <p> Before delegation, an absolute resource name is constructed from the
     * given resource name using this algorithm:
     *
     * <ul>
     *
     * <li> If the {@code name} begins with a {@code '/'}
     * (<code>'&#92;u002f'</code>), then the absolute name of the resource is the
     * portion of the {@code name} following the {@code '/'}.
     *
     * <li> Otherwise, the absolute name is of the following form:
     *
     * <blockquote>
     *   {@code modified_package_name/name}
     * </blockquote>
     *
     * <p> Where the {@code modified_package_name} is the package name of this
     * object with {@code '/'} substituted for {@code '.'}
     * (<code>'&#92;u002e'</code>).
     *
     * </ul>
     *
     * @param  name name of the desired resource
     * @return A {@link java.net.URL} object; {@code null} if no resource with
     *         this name is found.
     * @throws NullPointerException If {@code name} is {@code null}
     * @since  1.1
     * @revised 9
     * @spec JPMS
     */
    @CallerSensitive
    public URL getResource(String name) {
        name = resolveName(name);
        // unnamed module
        // Android-changed: Removed BuiltinClassLoader, Module usage, and SecurityManager check.
        ClassLoader cl = getClassLoader();
        if (cl == null) {
            return ClassLoader.getSystemResource(name);
        } else {
            return cl.getResource(name);
        }
    }

    // Android-removed: Remove unused method.
    /*
     * Returns true if a resource with the given name can be located by the
     * given caller. All resources in a module can be located by code in
     * the module. For other callers, then the package needs to be open to
     * the caller.
     *
    private boolean isOpenToCaller(String name, Class<?> caller) {
        // assert getModule().isNamed();
        Module thisModule = getModule();
        Module callerModule = (caller != null) ? caller.getModule() : null;
        if (callerModule != thisModule) {
            String pn = Resources.toPackageName(name);
            if (thisModule.getDescriptor().packages().contains(pn)) {
                if (callerModule == null && !thisModule.isOpen(pn)) {
                    // no caller, package not open
                    return false;
                }
                if (!thisModule.isOpen(pn, callerModule)) {
                    // package not open to caller
                    return false;
                }
            }
        }
        return true;
    }


    /** protection domain returned when the internal domain is null *
    private static java.security.ProtectionDomain allPermDomain;
    */

    /**
     * Returns the {@code ProtectionDomain} of this class.  If there is a
     * security manager installed, this method first calls the security
     * manager's {@code checkPermission} method with a
     * {@code RuntimePermission("getProtectionDomain")} permission to
     * ensure it's ok to get the
     * {@code ProtectionDomain}.
     *
     * @return the ProtectionDomain of this class
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        {@code checkPermission} method doesn't allow
     *        getting the ProtectionDomain.
     *
     * @see java.security.ProtectionDomain
     * @see SecurityManager#checkPermission
     * @see java.lang.RuntimePermission
     * @since 1.2
     */
    public java.security.ProtectionDomain getProtectionDomain() {
        return null;
    }

    /*
     * Return the runtime's Class object for the named
     * primitive type.
     */
    @FastNative
    static native Class<?> getPrimitiveClass(String name);

    // Android-removed: Remove unused method.
    /*
     * Check if client is allowed to access members.  If access is denied,
     * throw a SecurityException.
     *
     * This method also enforces package access.
     *
     * <p> Default policy: allow all clients access with normal Java access
     * control.
     *
     * <p> NOTE: should only be called if a SecurityManager is installed
     *
    private void checkMemberAccess(SecurityManager sm, int which,
                                   Class<?> caller, boolean checkProxyInterfaces) {
        *//* Default policy allows access to all {@link Member#PUBLIC} members,
         * as well as access to classes that have the same class loader as the caller.
         * In all other cases, it requires RuntimePermission("accessDeclaredMembers")
         * permission.
         *//*
        final ClassLoader ccl = ClassLoader.getClassLoader(caller);
        if (which != Member.PUBLIC) {
            final ClassLoader cl = getClassLoader0();
            if (ccl != cl) {
                sm.checkPermission(SecurityConstants.CHECK_MEMBER_ACCESS_PERMISSION);
            }
        }
        this.checkPackageAccess(sm, ccl, checkProxyInterfaces);
    }

    /*
     * Checks if a client loaded in ClassLoader ccl is allowed to access this
     * class under the current package access policy. If access is denied,
     * throw a SecurityException.
     *
     * NOTE: this method should only be called if a SecurityManager is active
     *
    private void checkPackageAccess(SecurityManager sm, final ClassLoader ccl,
                                    boolean checkProxyInterfaces) {
        final ClassLoader cl = getClassLoader0();

        if (ReflectUtil.needsPackageAccessCheck(ccl, cl)) {
            String pkg = this.getPackageName();
            if (pkg != null && !pkg.isEmpty()) {
                // skip the package access check on a proxy class in default proxy package
                if (!Proxy.isProxyClass(this) || ReflectUtil.isNonPublicProxyClass(this)) {
                    sm.checkPackageAccess(pkg);
                }
            }
        }
        // check package access on the proxy interfaces
        if (checkProxyInterfaces && Proxy.isProxyClass(this)) {
            ReflectUtil.checkProxyPackageAccess(ccl, this.getInterfaces());
        }
    }
    */

    /**
     * Add a package name prefix if the name is not absolute Remove leading "/"
     * if name is absolute
     */
    private String resolveName(String name) {
        if (!name.startsWith("/")) {
            Class<?> c = this;
            while (c.isArray()) {
                c = c.getComponentType();
            }
            String baseName = c.getPackageName();
            if (baseName != null && !baseName.isEmpty()) {
                name = baseName.replace('.', '/') + "/" + name;
            }
        } else {
            name = name.substring(1);
        }
        return name;
    }

    private Constructor<T> getConstructor0(Class<?>[] parameterTypes,
                                        int which) throws NoSuchMethodException
    {
        if (parameterTypes == null) {
            parameterTypes = EmptyArray.CLASS;
        }
        for (Class<?> c : parameterTypes) {
            if (c == null) {
                throw new NoSuchMethodException("parameter type is null");
            }
        }
        Constructor<T> result = getDeclaredConstructorInternal(parameterTypes);
        if (result == null || which == Member.PUBLIC && !Modifier.isPublic(result.getAccessFlags())) {
            throw new NoSuchMethodException(getName() + ".<init> "
                    + Arrays.toString(parameterTypes));
        }
        return result;
    }

    /*
    /**
     * Atomic operations support.
     *//*
    private static class Atomic {
        // initialize Unsafe machinery here, since we need to call Class.class instance method
        // and have to avoid calling it in the static initializer of the Class class...
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        // offset of Class.reflectionData instance field
        private static final long reflectionDataOffset
                = unsafe.objectFieldOffset(Class.class, "reflectionData");
        // offset of Class.annotationType instance field
        private static final long annotationTypeOffset
                = unsafe.objectFieldOffset(Class.class, "annotationType");
        // offset of Class.annotationData instance field
        private static final long annotationDataOffset
                = unsafe.objectFieldOffset(Class.class, "annotationData");

        static <T> boolean casReflectionData(Class<?> clazz,
                                             SoftReference<ReflectionData<T>> oldData,
                                             SoftReference<ReflectionData<T>> newData) {
            return unsafe.compareAndSetObject(clazz, reflectionDataOffset, oldData, newData);
        }

        static <T> boolean casAnnotationType(Class<?> clazz,
                                             AnnotationType oldType,
                                             AnnotationType newType) {
            return unsafe.compareAndSetObject(clazz, annotationTypeOffset, oldType, newType);
        }

        static <T> boolean casAnnotationData(Class<?> clazz,
                                             AnnotationData oldData,
                                             AnnotationData newData) {
            return unsafe.compareAndSetObject(clazz, annotationDataOffset, oldData, newData);
        }
    }

    *//**
     * Reflection support.
     *//*

    // Reflection data caches various derived names and reflective members. Cached
    // values may be invalidated when JVM TI RedefineClasses() is called
    private static class ReflectionData<T> {
        volatile Field[] declaredFields;
        volatile Field[] publicFields;
        volatile Method[] declaredMethods;
        volatile Method[] publicMethods;
        volatile Constructor<T>[] declaredConstructors;
        volatile Constructor<T>[] publicConstructors;
        // Intermediate results for getFields and getMethods
        volatile Field[] declaredPublicFields;
        volatile Method[] declaredPublicMethods;
        volatile Class<?>[] interfaces;

        // Cached names
        String simpleName;
        String canonicalName;
        static final String NULL_SENTINEL = new String();

        // Value of classRedefinedCount when we created this ReflectionData instance
        final int redefinedCount;

        ReflectionData(int redefinedCount) {
            this.redefinedCount = redefinedCount;
        }
    }

    private transient volatile SoftReference<ReflectionData<T>> reflectionData;

    // Incremented by the VM on each call to JVM TI RedefineClasses()
    // that redefines this class or a superclass.
    private transient volatile int classRedefinedCount;

    // Lazily create and cache ReflectionData
    private ReflectionData<T> reflectionData() {
        SoftReference<ReflectionData<T>> reflectionData = this.reflectionData;
        int classRedefinedCount = this.classRedefinedCount;
        ReflectionData<T> rd;
        if (reflectionData != null &&
            (rd = reflectionData.get()) != null &&
            rd.redefinedCount == classRedefinedCount) {
            return rd;
        }
        // else no SoftReference or cleared SoftReference or stale ReflectionData
        // -> create and replace new instance
        return newReflectionData(reflectionData, classRedefinedCount);
    }

    private ReflectionData<T> newReflectionData(SoftReference<ReflectionData<T>> oldReflectionData,
                                                int classRedefinedCount) {
        while (true) {
            ReflectionData<T> rd = new ReflectionData<>(classRedefinedCount);
            // try to CAS it...
            if (Atomic.casReflectionData(this, oldReflectionData, new SoftReference<>(rd))) {
                return rd;
            }
            // else retry
            oldReflectionData = this.reflectionData;
            classRedefinedCount = this.classRedefinedCount;
            if (oldReflectionData != null &&
                (rd = oldReflectionData.get()) != null &&
                rd.redefinedCount == classRedefinedCount) {
                return rd;
            }
        }
    }

    // Generic signature handling
    private native String getGenericSignature0();

    // Generic info repository; lazily initialized
    private transient volatile ClassRepository genericInfo;

    // accessor for factory
    private GenericsFactory getFactory() {
        // create scope and factory
        return CoreReflectionFactory.make(this, ClassScope.make(this));
    }

    // accessor for generic info repository;
    // generic info is lazily initialized
    private ClassRepository getGenericInfo() {
        ClassRepository genericInfo = this.genericInfo;
        if (genericInfo == null) {
            String signature = getGenericSignature0();
            if (signature == null) {
                genericInfo = ClassRepository.NONE;
            } else {
                genericInfo = ClassRepository.make(signature, getFactory());
            }
            this.genericInfo = genericInfo;
        }
        return (genericInfo != ClassRepository.NONE) ? genericInfo : null;
    }

    // Annotations handling
    native byte[] getRawAnnotations();
    // Since 1.8
    native byte[] getRawTypeAnnotations();
    static byte[] getExecutableTypeAnnotationBytes(Executable ex) {
        return getReflectionFactory().getExecutableTypeAnnotationBytes(ex);
    }

    native ConstantPool getConstantPool();

    //
    //
    // java.lang.reflect.Field handling
    //
    //

    // Returns an array of "root" fields. These Field objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyField.
    private Field[] privateGetDeclaredFields(boolean publicOnly) {
        Field[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = publicOnly ? rd.declaredPublicFields : rd.declaredFields;
            if (res != null) return res;
        }
        // No cached value available; request value from VM
        res = Reflection.filterFields(this, getDeclaredFields0(publicOnly));
        if (rd != null) {
            if (publicOnly) {
                rd.declaredPublicFields = res;
            } else {
                rd.declaredFields = res;
            }
        }
        return res;
    }

    // Returns an array of "root" fields. These Field objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyField.
    private Field[] privateGetPublicFields() {
        Field[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = rd.publicFields;
            if (res != null) return res;
        }

        // Use a linked hash set to ensure order is preserved and
        // fields from common super interfaces are not duplicated
        LinkedHashSet<Field> fields = new LinkedHashSet<>();

        // Local fields
        addAll(fields, privateGetDeclaredFields(true));

        // Direct superinterfaces, recursively
        for (Class<?> si : getInterfaces()) {
            addAll(fields, si.privateGetPublicFields());
        }

        // Direct superclass, recursively
        Class<?> sc = getSuperclass();
        if (sc != null) {
            addAll(fields, sc.privateGetPublicFields());
        }

        res = fields.toArray(new Field[0]);
        if (rd != null) {
            rd.publicFields = res;
        }
        return res;
    }

    private static void addAll(Collection<Field> c, Field[] o) {
        for (Field f : o) {
            c.add(f);
        }
    }


    //
    //
    // java.lang.reflect.Constructor handling
    //
    //

    // Returns an array of "root" constructors. These Constructor
    // objects must NOT be propagated to the outside world, but must
    // instead be copied via ReflectionFactory.copyConstructor.
    private Constructor<T>[] privateGetDeclaredConstructors(boolean publicOnly) {
        Constructor<T>[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = publicOnly ? rd.publicConstructors : rd.declaredConstructors;
            if (res != null) return res;
        }
        // No cached value available; request value from VM
        if (isInterface()) {
            @SuppressWarnings("unchecked")
            Constructor<T>[] temporaryRes = (Constructor<T>[]) new Constructor<?>[0];
            res = temporaryRes;
        } else {
            res = getDeclaredConstructors0(publicOnly);
        }
        if (rd != null) {
            if (publicOnly) {
                rd.publicConstructors = res;
            } else {
                rd.declaredConstructors = res;
            }
        }
        return res;
    }

    //
    //
    // java.lang.reflect.Method handling
    //
    //

    // Returns an array of "root" methods. These Method objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private Method[] privateGetDeclaredMethods(boolean publicOnly) {
        Method[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = publicOnly ? rd.declaredPublicMethods : rd.declaredMethods;
            if (res != null) return res;
        }
        // No cached value available; request value from VM
        res = Reflection.filterMethods(this, getDeclaredMethods0(publicOnly));
        if (rd != null) {
            if (publicOnly) {
                rd.declaredPublicMethods = res;
            } else {
                rd.declaredMethods = res;
            }
        }
        return res;
    }

    // Returns an array of "root" methods. These Method objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private Method[] privateGetPublicMethods() {
        Method[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = rd.publicMethods;
            if (res != null) return res;
        }

        // No cached value available; compute value recursively.
        // Start by fetching public declared methods...
        PublicMethods pms = new PublicMethods();
        for (Method m : privateGetDeclaredMethods(*//* publicOnly *//* true)) {
            pms.merge(m);
        }
        // ...then recur over superclass methods...
        Class<?> sc = getSuperclass();
        if (sc != null) {
            for (Method m : sc.privateGetPublicMethods()) {
                pms.merge(m);
            }
        }
        // ...and finally over direct superinterfaces.
        for (Class<?> intf : getInterfaces(*//* cloneArray *//* false)) {
            for (Method m : intf.privateGetPublicMethods()) {
                // static interface methods are not inherited
                if (!Modifier.isStatic(m.getModifiers())) {
                    pms.merge(m);
                }
            }
        }

        res = pms.toArray();
        if (rd != null) {
            rd.publicMethods = res;
        }
        return res;
    }


    //
    // Helpers for fetchers of one field, method, or constructor
    //

    // This method does not copy the returned Field object!
    private static Field searchFields(Field[] fields, String name) {
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }

    // Returns a "root" Field object. This Field object must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyField.
    private Field getField0(String name) {
        // Note: the intent is that the search algorithm this routine
        // uses be equivalent to the ordering imposed by
        // privateGetPublicFields(). It fetches only the declared
        // public fields for each class, however, to reduce the number
        // of Field objects which have to be created for the common
        // case where the field being requested is declared in the
        // class which is being queried.
        Field res;
        // Search declared public fields
        if ((res = searchFields(privateGetDeclaredFields(true), name)) != null) {
            return res;
        }
        // Direct superinterfaces, recursively
        Class<?>[] interfaces = getInterfaces(*//* cloneArray *//* false);
        for (Class<?> c : interfaces) {
            if ((res = c.getField0(name)) != null) {
                return res;
            }
        }
        // Direct superclass, recursively
        if (!isInterface()) {
            Class<?> c = getSuperclass();
            if (c != null) {
                if ((res = c.getField0(name)) != null) {
                    return res;
                }
            }
        }
        return null;
    }

    // This method does not copy the returned Method object!
    private static Method searchMethods(Method[] methods,
                                        String name,
                                        Class<?>[] parameterTypes)
    {
        ReflectionFactory fact = getReflectionFactory();
        Method res = null;
        for (Method m : methods) {
            if (m.getName().equals(name)
                && arrayContentsEq(parameterTypes,
                                   fact.getExecutableSharedParameterTypes(m))
                && (res == null
                    || (res.getReturnType() != m.getReturnType()
                        && res.getReturnType().isAssignableFrom(m.getReturnType()))))
                res = m;
        }
        return res;
    }

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    // Returns a "root" Method object. This Method object must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private Method getMethod0(String name, Class<?>[] parameterTypes) {
        PublicMethods.MethodList res = getMethodsRecursive(
            name,
            parameterTypes == null ? EMPTY_CLASS_ARRAY : parameterTypes,
            *//* includeStatic *//* true);
        return res == null ? null : res.getMostSpecific();
    }

    // Returns a list of "root" Method objects. These Method objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private PublicMethods.MethodList getMethodsRecursive(String name,
                                                         Class<?>[] parameterTypes,
                                                         boolean includeStatic) {
        // 1st check declared public methods
        Method[] methods = privateGetDeclaredMethods(*//* publicOnly *//* true);
        PublicMethods.MethodList res = PublicMethods.MethodList
            .filter(methods, name, parameterTypes, includeStatic);
        // if there is at least one match among declared methods, we need not
        // search any further as such match surely overrides matching methods
        // declared in superclass(es) or interface(s).
        if (res != null) {
            return res;
        }

        // if there was no match among declared methods,
        // we must consult the superclass (if any) recursively...
        Class<?> sc = getSuperclass();
        if (sc != null) {
            res = sc.getMethodsRecursive(name, parameterTypes, includeStatic);
        }

        // ...and coalesce the superclass methods with methods obtained
        // from directly implemented interfaces excluding static methods...
        for (Class<?> intf : getInterfaces(*//* cloneArray *//* false)) {
            res = PublicMethods.MethodList.merge(
                res, intf.getMethodsRecursive(name, parameterTypes,
                                              *//* includeStatic *//* false));
        }

        return res;
    }

    // Returns a "root" Constructor object. This Constructor object must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyConstructor.
    private Constructor<T> getConstructor0(Class<?>[] parameterTypes,
                                        int which) throws NoSuchMethodException
    {
        ReflectionFactory fact = getReflectionFactory();
        Constructor<T>[] constructors = privateGetDeclaredConstructors((which == Member.PUBLIC));
        for (Constructor<T> constructor : constructors) {
            if (arrayContentsEq(parameterTypes,
                                fact.getExecutableSharedParameterTypes(constructor))) {
                return constructor;
            }
        }
        throw new NoSuchMethodException(methodToString("<init>", parameterTypes));
    }

    //
    // Other helpers and base implementation
    //

    private static boolean arrayContentsEq(Object[] a1, Object[] a2) {
        if (a1 == null) {
            return a2 == null || a2.length == 0;
        }

        if (a2 == null) {
            return a1.length == 0;
        }

        if (a1.length != a2.length) {
            return false;
        }

        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }

        return true;
    }

    private static Field[] copyFields(Field[] arg) {
        Field[] out = new Field[arg.length];
        ReflectionFactory fact = getReflectionFactory();
        for (int i = 0; i < arg.length; i++) {
            out[i] = fact.copyField(arg[i]);
        }
        return out;
    }

    private static Method[] copyMethods(Method[] arg) {
        Method[] out = new Method[arg.length];
        ReflectionFactory fact = getReflectionFactory();
        for (int i = 0; i < arg.length; i++) {
            out[i] = fact.copyMethod(arg[i]);
        }
        return out;
    }

    private static <U> Constructor<U>[] copyConstructors(Constructor<U>[] arg) {
        Constructor<U>[] out = arg.clone();
        ReflectionFactory fact = getReflectionFactory();
        for (int i = 0; i < out.length; i++) {
            out[i] = fact.copyConstructor(out[i]);
        }
        return out;
    }

    private native Field[]       getDeclaredFields0(boolean publicOnly);
    private native Method[]      getDeclaredMethods0(boolean publicOnly);
    private native Constructor<T>[] getDeclaredConstructors0(boolean publicOnly);
    private native Class<?>[]   getDeclaredClasses0();

    */

    /*
     * Returns an array containing the components of the Record attribute,
     * or null if the attribute is not present.
     *
     * Note that this method returns non-null array on a class with
     * the Record attribute even if this class is not a record.
     */
    // BEGIN Android-changed: Re-implement getRecordComponents0() on ART.
    // private native RecordComponent[] getRecordComponents0();
    private RecordComponent[] getRecordComponents0() {
        RecordComponents libcoreComponents = new RecordComponents(this);

        // Every component should have a type and a name.
        String[] names = libcoreComponents.getNames();
        Class<?>[] types = libcoreComponents.getTypes();
        if (names == null || types == null) {
            // Return non-null array as per getRecordComponent() javadoc if isRecord()
            // returns true.
            return new RecordComponent[0];
        }

        // class_linker.cc should verify that names and types have the same length, or otherwise,
        // the class isn't loaded.
        int size = Math.min(names.length, types.length);
        RecordComponent[] components = new RecordComponent[size];
        for (int i = 0; i < size; i++) {
            String name = names[i];
            Class<?> type = types[i];
            components[i] = new RecordComponent(this, name, type, libcoreComponents, i);
        }
        return components;
    }

    /**
     * Used by {@link libcore.reflect.RecordComponents}
     *
     * @hide
     */
    @FastNative
    public native <T2> T2[] getRecordAnnotationElement(String elementName, Class<T2[]> arrayClass);
    // END Android-changed: Re-implement getRecordComponents0() on ART.

    @FastNative
    private native boolean       isRecord0();

    /**
     * Helper method to get the method name from arguments.
     *//*
    private String methodToString(String name, Class<?>[] argTypes) {
        StringJoiner sj = new StringJoiner(", ", getName() + "." + name + "(", ")");
        if (argTypes != null) {
            for (int i = 0; i < argTypes.length; i++) {
                Class<?> c = argTypes[i];
                sj.add((c == null) ? "null" : c.getName());
            }
        }
        return sj.toString();
    }
    */

    /** use serialVersionUID from JDK 1.1 for interoperability */
    private static final long serialVersionUID = 3206093459760846163L;


    /**
     * Returns the constructor with the given parameters if it is defined by this class;
     * {@code null} otherwise. This may return a non-public member.
     *
     * @param args the types of the parameters to the constructor.
     */
    @FastNative
    private native Constructor<T> getDeclaredConstructorInternal(Class<?>[] args);

    /**
     * Returns the assertion status that would be assigned to this
     * class if it were to be initialized at the time this method is invoked.
     * If this class has had its assertion status set, the most recent
     * setting will be returned; otherwise, if any package default assertion
     * status pertains to this class, the most recent setting for the most
     * specific pertinent package default assertion status is returned;
     * otherwise, if this class is not a system class (i.e., it has a
     * class loader) its class loader's default assertion status is returned;
     * otherwise, the system class default assertion status is returned.
     * <p>
     * Few programmers will have any need for this method; it is provided
     * for the benefit of the JRE itself.  (It allows a class to determine at
     * the time that it is initialized whether assertions should be enabled.)
     * Note that this method is not guaranteed to return the actual
     * assertion status that was (or will be) associated with the specified
     * class when it was (or will be) initialized.
     *
     * Android-note: AssertionStatuses are unsupported. This method will always return false.
     *
     * @return the desired assertion status of the specified class.
     * @see    java.lang.ClassLoader#setClassAssertionStatus
     * @see    java.lang.ClassLoader#setPackageAssertionStatus
     * @see    java.lang.ClassLoader#setDefaultAssertionStatus
     * @since  1.4
     */
    public boolean desiredAssertionStatus() {
        // Android-changaed: AssertionStatuses are unsupported.
        return false;
    }

    /**
     * Returns the simple name of a member or local class, or {@code null} otherwise.
     */
    @FastNative
    private native String getInnerClassName();

    @FastNative
    private native int getInnerClassFlags(int defaultValue);

    /**
     * Returns true if and only if this class was declared as an enum in the
     * source code.
     *
     * @return true if and only if this class was declared as an enum in the
     *     source code
     * @since 1.5
     */
    public boolean isEnum() {
        // An enum must both directly extend java.lang.Enum and have
        // the ENUM bit set; classes for specialized enum constants
        // don't do the former.
        return (this.getModifiers() & ENUM) != 0 &&
        this.getSuperclass() == java.lang.Enum.class;
    }

    /**
     * Returns {@code true} if and only if this class is a record class.
     *
     * <p> The {@linkplain #getSuperclass() direct superclass} of a record
     * class is {@code java.lang.Record}. A record class is {@linkplain
     * Modifier#FINAL final}. A record class has (possibly zero) record
     * components; {@link #getRecordComponents()} returns a non-null but
     * possibly empty value for a record.
     *
     * <p> Note that class {@link Record} is not a record class and thus
     * invoking this method on class {@code Record} returns {@code false}.
     *
     * @return true if and only if this class is a record class, otherwise false
     * @jls 8.10 Record Classes
     * @since 16
     */
    public boolean isRecord() {
        // this superclass and final modifier check is not strictly necessary
        // they are intrinsified and serve as a fast-path check
        return getSuperclass() == java.lang.Record.class &&
                (this.getModifiers() & Modifier.FINAL) != 0 &&
                isRecord0();
    }

    // Android-remvoed: Remove unsupported ReflectionFactory.
    /*
    // Fetches the factory for reflective objects
    private static ReflectionFactory getReflectionFactory() {
        if (reflectionFactory == null) {
            reflectionFactory =
                java.security.AccessController.doPrivileged
                    (new ReflectionFactory.GetReflectionFactoryAction());
        }
        return reflectionFactory;
    }
    private static ReflectionFactory reflectionFactory;
    */

    /**
     * Returns the elements of this enum class or null if this
     * Class object does not represent an enum type.
     *
     * @return an array containing the values comprising the enum class
     *     represented by this Class object in the order they're
     *     declared, or null if this Class object does not
     *     represent an enum type
     * @since 1.5
     */
    public T[] getEnumConstants() {
        T[] values = getEnumConstantsShared();
        return (values != null) ? values.clone() : null;
    }

    // Android-changed: Made public/hidden instead of using sun.misc.SharedSecrets.
    /**
     * Returns the elements of this enum class or null if this
     * Class object does not represent an enum type;
     * identical to getEnumConstants except that the result is
     * uncloned, cached, and shared by all callers.
     * @hide
     */
    public T[] getEnumConstantsShared() {
        if (!isEnum()) return null;
        return (T[]) Enum.getSharedConstants((Class) this);
    }
    /*
    T[] getEnumConstantsShared() {
        T[] constants = enumConstants;
        if (constants == null) {
            if (!isEnum()) return null;
            try {
                final Method values = getMethod("values");
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<>() {
                        public Void run() {
                                values.setAccessible(true);
                                return null;
                            }
                        });
                @SuppressWarnings("unchecked")
                T[] temporaryConstants = (T[])values.invoke(null);
                enumConstants = constants = temporaryConstants;
            }
            // These can happen when users concoct enum-like classes
            // that don't comply with the enum spec.
            catch (InvocationTargetException | NoSuchMethodException |
                   IllegalAccessException ex) { return null; }
        }
        return constants;
    }
    private transient volatile T[] enumConstants;
    */

    // Android-removed: Remove unused method.
    /*
    *//**
     * Returns a map from simple name to enum constant.  This package-private
     * method is used internally by Enum to implement
     * {@code public static <T extends Enum<T>> T valueOf(Class<T>, String)}
     * efficiently.  Note that the map is returned by this method is
     * created lazily on first use.  Typically it won't ever get created.
     *//*
    Map<String, T> enumConstantDirectory() {
        Map<String, T> directory = enumConstantDirectory;
        if (directory == null) {
            T[] universe = getEnumConstantsShared();
            if (universe == null)
                throw new IllegalArgumentException(
                    getName() + " is not an enum type");
            directory = new HashMap<>((int)(universe.length / 0.75f) + 1);
            for (T constant : universe) {
                directory.put(((Enum<?>)constant).name(), constant);
            }
            enumConstantDirectory = directory;
        }
        return directory;
    }
    private transient volatile Map<String, T> enumConstantDirectory;
    */

    /**
     * Casts an object to the class or interface represented
     * by this {@code Class} object.
     *
     * @param obj the object to be cast
     * @return the object after casting, or null if obj is null
     *
     * @throws ClassCastException if the object is not
     * null and is not assignable to the type T.
     *
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    @HotSpotIntrinsicCandidate
    public T cast(Object obj) {
        if (obj != null && !isInstance(obj))
            throw new ClassCastException(cannotCastMsg(obj));
        return (T) obj;
    }

    private String cannotCastMsg(Object obj) {
        return "Cannot cast " + obj.getClass().getName() + " to " + getName();
    }

    /**
     * Casts this {@code Class} object to represent a subclass of the class
     * represented by the specified class object.  Checks that the cast
     * is valid, and throws a {@code ClassCastException} if it is not.  If
     * this method succeeds, it always returns a reference to this class object.
     *
     * <p>This method is useful when a client needs to "narrow" the type of
     * a {@code Class} object to pass it to an API that restricts the
     * {@code Class} objects that it is willing to accept.  A cast would
     * generate a compile-time warning, as the correctness of the cast
     * could not be checked at runtime (because generic types are implemented
     * by erasure).
     *
     * @param <U> the type to cast this class object to
     * @param clazz the class of the type to cast this class object to
     * @return this {@code Class} object, cast to represent a subclass of
     *    the specified class object.
     * @throws ClassCastException if this {@code Class} object does not
     *    represent a subclass of the specified class (here "subclass" includes
     *    the class itself).
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public <U> Class<? extends U> asSubclass(Class<U> clazz) {
        if (clazz.isAssignableFrom(this))
            return (Class<? extends U>) this;
        else
            throw new ClassCastException(this.toString() +
                " cannot be cast to " + clazz.getName());
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        Objects.requireNonNull(annotationClass);

        A annotation = getDeclaredAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }

        if (annotationClass.isDeclaredAnnotationPresent(Inherited.class)) {
            for (Class<?> sup = getSuperclass(); sup != null; sup = sup.getSuperclass()) {
                annotation = sup.getDeclaredAnnotation(annotationClass);
                if (annotation != null) {
                    return annotation;
                }
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        if (annotationClass == null) {
            throw new NullPointerException("annotationClass == null");
        }

        if (isDeclaredAnnotationPresent(annotationClass)) {
            return true;
        }

        if (annotationClass.isDeclaredAnnotationPresent(Inherited.class)) {
            for (Class<?> sup = getSuperclass(); sup != null; sup = sup.getSuperclass()) {
                if (sup.isDeclaredAnnotationPresent(annotationClass)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
      // Find any associated annotations [directly or repeatably (indirectly) present on this].
      A[] annotations = GenericDeclaration.super.getAnnotationsByType(annotationClass);

      if (annotations.length != 0) {
        return annotations;
      }

      // Nothing was found, attempt looking for associated annotations recursively up to the root
      // class if and only if:
      // * The annotation class was marked with @Inherited.
      //
      // Inherited annotations are not coalesced into a single set: the first declaration found is
      // returned.

      if (annotationClass.isDeclaredAnnotationPresent(Inherited.class)) {
        Class<?> superClass = getSuperclass();  // Returns null if klass's base is Object.
        if (superClass != null) {
          return superClass.getAnnotationsByType(annotationClass);
        }
      }

      // Annotated was not marked with @Inherited, or no superclass.
      return (A[]) Array.newInstance(annotationClass, 0);  // Safe by construction.
    }


    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
        Objects.requireNonNull(annotationClass);

        // Android-changed: Altered method implementation for getDeclaredAnnotationsByType(Class).
        // return AnnotationSupport.getDirectlyAndIndirectlyPresent(annotationData().declaredAnnotations,
        //     annotationClass);
        return GenericDeclaration.super.getDeclaredAnnotationsByType(annotationClass);
    }

    /**
     * @since 1.5
     */
    @Override
    public Annotation[] getAnnotations() {
        /*
         * We need to get the annotations declared on this class, plus the
         * annotations from superclasses that have the "@Inherited" annotation
         * set.  We create a temporary map to use while we accumulate the
         * annotations and convert it to an array at the end.
         *
         * It's possible to have duplicates when annotations are inherited.
         * We use a Map to filter those out.
         *
         * HashMap might be overkill here.
         */
        HashMap<Class<?>, Annotation> map = new HashMap<Class<?>, Annotation>();
        for (Annotation declaredAnnotation : getDeclaredAnnotations()) {
            map.put(declaredAnnotation.annotationType(), declaredAnnotation);
        }
        for (Class<?> sup = getSuperclass(); sup != null; sup = sup.getSuperclass()) {
            for (Annotation declaredAnnotation : sup.getDeclaredAnnotations()) {
                Class<? extends Annotation> clazz = declaredAnnotation.annotationType();
                if (!map.containsKey(clazz) && clazz.isDeclaredAnnotationPresent(Inherited.class)) {
                    map.put(clazz, declaredAnnotation);
                }
            }
        }

        /* Convert annotation values from HashMap to array. */
        Collection<Annotation> coll = map.values();
        return coll.toArray(new Annotation[coll.size()]);
    }

    // Android-removed: Remove unused fields and methods.
    /*
    // Annotation types cache their internal (AnnotationType) form

    @SuppressWarnings("UnusedDeclaration")
    private transient volatile AnnotationType annotationType;

    boolean casAnnotationType(AnnotationType oldType, AnnotationType newType) {
        return Atomic.casAnnotationType(this, oldType, newType);
    }

    AnnotationType getAnnotationType() {
        return annotationType;
    }

    Map<Class<? extends Annotation>, Annotation> getDeclaredAnnotationMap() {
        return annotationData().declaredAnnotations;
    }

    *//* Backing store of user-defined values pertaining to this class.
     * Maintained by the ClassValue class.
     *//*
    transient ClassValue.ClassValueMap classValueMap;
    */

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    @FastNative
    public native <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass);

    /**
     * @since 1.5
     */
    @Override
    @FastNative
    public native Annotation[] getDeclaredAnnotations();
    /*
    public Annotation[] getDeclaredAnnotations()  {
        return AnnotationParser.toArray(annotationData().declaredAnnotations);
    }
    */

    /**
     * Returns true if the annotation exists.
     */
    @FastNative
    private native boolean isDeclaredAnnotationPresent(Class<? extends Annotation> annotationClass);

    private String getSignatureAttribute() {
        String[] annotation = getSignatureAnnotation();
        if (annotation == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (String s : annotation) {
            result.append(s);
        }
        return result.toString();
    }

    @FastNative
    private native String[] getSignatureAnnotation();

    /**
     * Is this a runtime created proxy class?
     *
     * @hide
     */
    public boolean isProxy() {
        return (accessFlags & 0x00040000) != 0;
    }

    /**
     * @hide
     */
    public int getAccessFlags() {
        return accessFlags;
    }

    /**
     * Returns the method if it is defined by this class; {@code null} otherwise. This may return a
     * non-public member.
     *
     * @param name the method name
     * @param args the method's parameter types
     */
    @FastNative
    private native Method getDeclaredMethodInternal(String name, Class<?>[] args);

    @FastNative
    native ClassExt ensureExtDataPresent();

    // Android-changed: Removed SecurityException.
    /**
     * Returns the nest host of the <a href=#nest>nest</a> to which the class
     * or interface represented by this {@code Class} object belongs.
     * Every class and interface belongs to exactly one nest.
     *
     * If the nest host of this class or interface has previously
     * been determined, then this method returns the nest host.
     * If the nest host of this class or interface has
     * not previously been determined, then this method determines the nest
     * host using the algorithm of JVMS 5.4.4, and returns it.
     *
     * Often, a class or interface belongs to a nest consisting only of itself,
     * in which case this method returns {@code this} to indicate that the class
     * or interface is the nest host.
     *
     * <p>If this {@code Class} object represents a primitive type, an array type,
     * or {@code void}, then this method returns {@code this},
     * indicating that the represented entity belongs to the nest consisting only of
     * itself, and is the nest host.
     *
     * @return the nest host of this class or interface
     * @since 11
     * @jvms 4.7.28 The {@code NestHost} Attribute
     * @jvms 4.7.29 The {@code NestMembers} Attribute
     * @jvms 5.4.4 Access Control
     */
    public Class<?> getNestHost() {
        // Android-removed: Android has a different JNI layer.
        /*
        if (isPrimitive() || isArray()) {
            return this;
        }
        Class<?> host;
        try {
            host = getNestHost0();
        } catch (LinkageError e) {
            // if we couldn't load our nest-host then we
            // act as-if we have no nest-host attribute
            return this;
        }
        // if null then nest membership validation failed, so we
        // act as-if we have no nest-host attribute
        if (host == null || host == this) {
            return this;
        }
        // returning a different class requires a security check
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkPackageAccess(sm,
                               ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
        }
        return host;
        */
        if (isPrimitive() || isArray() || Void.TYPE.equals(this)) {
            return this;
        }

        Class host = getNestHostFromAnnotation();
        if (host == null) {
            return this;
        }
        return (nestHostHasMember(host, this) ? host : this);
    }

    private static boolean nestHostHasMember(Class<?> host, Class<?> member) {
        if (host.equals(member)) {
            return true;
        }
        return nestMembersIncludeMember(host.getNestMembersFromAnnotation(), member);
    }

    private static boolean nestMembersIncludeMember(Class<?>[] members, Class<?> member) {
        if (members == null) {
            return false;
        }
        for (Class m : members) {
            if (member.equals(m)) {
                return true;
            }
        }
        return false;
    }

    @FastNative
    private native Class<?> getNestHostFromAnnotation();

    // Android-changed: This implementation comes from OpenJDK 17.
    /**
     * Determines if the given {@code Class} is a nestmate of the
     * class or interface represented by this {@code Class} object.
     * Two classes or interfaces are nestmates
     * if they have the same {@linkplain #getNestHost() nest host}.
     *
     * @param c the class to check
     * @return {@code true} if this class and {@code c} are members of
     * the same nest; and {@code false} otherwise.
     *
     * @since 11
     */
    public boolean isNestmateOf(Class<?> c) {
        if (this == c) {
            return true;
        }
        if (isPrimitive() || isArray() ||
            c.isPrimitive() || c.isArray()) {
            return false;
        }

        return getNestHost() == c.getNestHost();
    }

    // Android-changed: Removed references to MethodHandles.Lookup#defineHiddenClass.
    /**
     * Returns an array containing {@code Class} objects representing all the
     * classes and interfaces that are members of the nest to which the class
     * or interface represented by this {@code Class} object belongs.
     *
     * First, this method obtains the {@linkplain #getNestHost() nest host},
     * {@code H}, of the nest to which the class or interface represented by
     * this {@code Class} object belongs. The zeroth element of the returned
     * array is {@code H}.
     *
     * Then, for each class or interface {@code C} which is recorded by {@code H}
     * as being a member of its nest, this method attempts to obtain the {@code Class}
     * object for {@code C} (using {@linkplain #getClassLoader() the defining class
     * loader} of the current {@code Class} object), and then obtains the
     * {@linkplain #getNestHost() nest host} of the nest to which {@code C} belongs.
     * The classes and interfaces which are recorded by {@code H} as being members
     * of its nest, and for which {@code H} can be determined as their nest host,
     * are indicated by subsequent elements of the returned array. The order of
     * such elements is unspecified. Duplicates are permitted.
     *
     * <p>If this {@code Class} object represents a primitive type, an array type,
     * or {@code void}, then this method returns a single-element array containing
     * {@code this}.
     *
     * @apiNote
     * The returned array includes only the nest members recorded in the {@code NestMembers}
     * attribute.
     *
     * @return an array of all classes and interfaces in the same nest as
     * this class or interface
     *
     * @since 11
     * @see #getNestHost()
     * @jvms 4.7.28 The {@code NestHost} Attribute
     * @jvms 4.7.29 The {@code NestMembers} Attribute
     */
    public Class<?>[] getNestMembers() {
        // Android-changed: ART has a different JNI layer.
        /*
        if (isPrimitive() || isArray()) {
            return new Class<?>[] { this };
        }
        Class<?>[] members = getNestMembers0();
        // Can't actually enable this due to bootstrapping issues
        // assert(members.length != 1 || members[0] == this); // expected invariant from VM

        if (members.length > 1) {
            // If we return anything other than the current class we need
            // a security check
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                checkPackageAccess(sm,
                                   ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
            }
        }
        return members;
        */
        if (isPrimitive() || isArray() || Void.TYPE.equals(this)) {
            return new Class[] { this };
        }

        Class host = getNestHostFromAnnotation();
        if (host != null && !host.equals(this)) {
            if (host.isPrimitive() || host.isArray() || Void.TYPE.equals(host)) {
                return new Class[] { this };
            }
            return host.getNestMembers(this);
        }
        return getNestMembers(this);
    }

    private Class<?>[] getNestMembers(Class<?> originatingMember) {

        Class[] members = getNestMembersFromAnnotation();
        if (members == null) {
            return new Class[] { originatingMember };
        }
        if (originatingMember != this && !nestMembersIncludeMember(members, originatingMember)) {
            return new Class[] { originatingMember };
        }

        Class[] result = new Class[members.length+1];
        result[0] = this;
        int idx = 1;
        for (Class m : members) {
            if (m == null || !this.equals(m.getNestHostFromAnnotation())) {
                continue;
            }
            result[idx] = m;
            ++idx;
        }

        if (idx < result.length) {
            Class[] tmp = new Class[idx];
            for (int i = 0; i < tmp.length; ++i) {
                tmp[i] = result[i];
            }
            result = tmp;
        }

        return result;
    }

    @FastNative
    private native Class<?>[] getNestMembersFromAnnotation();

    private static class Caches {
        /**
         * Cache to avoid frequent recalculation of generic interfaces, which is generally uncommon.
         * Sized sufficient to allow ConcurrentHashMapTest to run without recalculating its generic
         * interfaces (required to avoid time outs). Validated by running reflection heavy code
         * such as applications using Guice-like frameworks.
         */
        private static final BasicLruCache<Class, Type[]> genericInterfaces
            = new BasicLruCache<Class, Type[]>(8);
    }

    // Android-changed: Removed @jls tags.
    /**
     * Returns an array containing {@code Class} objects representing the
     * direct subinterfaces or subclasses permitted to extend or
     * implement this class or interface if it is sealed.  The order of such elements
     * is unspecified. The array is empty if this sealed class or interface has no
     * permitted subclass. If this {@code Class} object represents a primitive type,
     * {@code void}, an array type, or a class or interface that is not sealed,
     * that is {@link #isSealed()} returns {@code false}, then this method returns {@code null}.
     * Conversely, if {@link #isSealed()} returns {@code true}, then this method
     * returns a non-null value.
     *
     * For each class or interface {@code C} which is recorded as a permitted
     * direct subinterface or subclass of this class or interface,
     * this method attempts to obtain the {@code Class}
     * object for {@code C} (using {@linkplain #getClassLoader() the defining class
     * loader} of the current {@code Class} object).
     * The {@code Class} objects which can be obtained and which are direct
     * subinterfaces or subclasses of this class or interface,
     * are indicated by elements of the returned array. If a {@code Class} object
     * cannot be obtained, it is silently ignored, and not included in the result
     * array.
     *
     * @return an array of {@code Class} objects of the permitted subclasses of this class or interface,
     *         or {@code null} if this class or interface is not sealed.
     *
     * @since 17
     */
    // Android-removed: CallerSensitive annotation
    // @CallerSensitive
    public Class<?>[] getPermittedSubclasses() {
        Class<?>[] subClasses;
        // Android-changed: A final class cannot be sealed.
        // if (isArray() || isPrimitive() || (subClasses = getPermittedSubclassesFromAnnotation()) == null) {
        if (isArray() || isPrimitive() || Modifier.isFinal( getModifiers() ) ||
                (subClasses = getPermittedSubclassesFromAnnotation()) == null) {
            return null;
        }
        // Android-changed: Avoid using lambdas.
        /*
        if (subClasses.length > 0) {
            if (Arrays.stream(subClasses).anyMatch(c -> !isDirectSubType(c))) {
                subClasses = Arrays.stream(subClasses)
                                   .filter(this::isDirectSubType)
                                   .toArray(s -> new Class<?>[s]);
            }
        }
        */
        int directSubClassCount = getDirectSubClassCount(subClasses);
        if (directSubClassCount < subClasses.length) {
            Class[] tmp = new Class[directSubClassCount];
            int idx = 0;
            for (Class<?> c : subClasses) {
                if (isDirectSubType(c)) {
                    tmp[idx] = c;
                    ++idx;
                }
            }
            subClasses = tmp;
        }

        // Android-removed: SecurityManager usage.
        /*
        if (subClasses.length > 0) {
            // If we return some classes we need a security check:
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                checkPackageAccessForPermittedSubclasses(sm,
                                             ClassLoader.getClassLoader(Reflection.getCallerClass()),
                                             subClasses);
            }
        }
        */
        return subClasses;
    }

    private int getDirectSubClassCount(Class<?>[] subClasses) {
        int result = 0;
        for (Class<?> c : subClasses) {
            if (isDirectSubType(c)) {
                 ++result;
            }
        }
        return result;
    }

    private boolean isDirectSubType(Class<?> c) {
        if (isInterface()) {
            // Android-changed: Use of getInterfaces
            //for (Class<?> i : c.getInterfaces(/* cloneArray */ false)) {
            for (Class<?> i : c.getInterfaces()) {
                if (i == this) {
                    return true;
                }
            }
        } else {
            return c.getSuperclass() == this;
        }
        return false;
    }

    // Android-changed: Removed @jls tags.
    /**
     * Returns {@code true} if and only if this {@code Class} object represents
     * a sealed class or interface. If this {@code Class} object represents a
     * primitive type, {@code void}, or an array type, this method returns
     * {@code false}. A sealed class or interface has (possibly zero) permitted
     * subclasses; {@link #getPermittedSubclasses()} returns a non-null but
     * possibly empty value for a sealed class or interface.
     *
     * @return {@code true} if and only if this {@code Class} object represents
     * a sealed class or interface.
     *
     * @since 17
     */
    public boolean isSealed() {
        if (isArray() || isPrimitive()) {
            return false;
        }
        return getPermittedSubclasses() != null;
    }

    @FastNative
    private native Class<?>[] getPermittedSubclassesFromAnnotation();
}
