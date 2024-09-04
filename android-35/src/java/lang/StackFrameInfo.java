/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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


import java.lang.StackWalker.StackFrame;
import java.lang.invoke.MethodType;

class StackFrameInfo implements StackFrame {

    // Android-removed: libcore doesn't have JavaLangInvokeAccess yet.
    // private static final JavaLangInvokeAccess JLIA =
    //     SharedSecrets.getJavaLangInvokeAccess();

    private final boolean retainClassRef;
    // Android-removed: Unused internal fields.
    // private final Object memberName;    // MemberName initialized by VM
    private int bci;                    // initialized by VM to >= 0
    private volatile StackTraceElement ste;

    // Android-added: Add Android-specific internal fields.
    private Class<?> declaringClass; // initialized by VM
    private MethodType methodType; // initialized by VM
    private String methodName; // initialized by VM
    private String fileName; // initialized by VM
    private int lineNumber; // initialized by VM

    /*
     * Construct an empty StackFrameInfo object that will be filled by the VM
     * during stack walking.
     *
     * @see StackStreamFactory.AbstractStackWalker#callStackWalk
     * @see StackStreamFactory.AbstractStackWalker#fetchStackFrames
     */
    StackFrameInfo(StackWalker walker) {
        this.retainClassRef = walker.retainClassRef;
        // Android-removed: Unused internal fields.
        // this.memberName = JLIA.newMemberName();
    }

    // Android-added: Additional constructor
    StackFrameInfo(boolean retainClassRef) {
        this.retainClassRef = retainClassRef;
    }


    // package-private called by StackStreamFactory to skip
    // the capability check
    Class<?> declaringClass() {
        // Android-changed: Android own implementation.
        // return JLIA.getDeclaringClass(memberName);
        return declaringClass;
    }

    // ----- implementation of StackFrame methods

    @Override
    public String getClassName() {
        return declaringClass().getName();
    }

    @Override
    public Class<?> getDeclaringClass() {
        ensureRetainClassRefEnabled();
        return declaringClass();
    }

    @Override
    public String getMethodName() {
        // Android-changed: Android own implementation.
        // return JLIA.getName(memberName);
        return methodName;
    }

    @Override
    public MethodType getMethodType() {
        ensureRetainClassRefEnabled();
        // Android-changed: Android own implementation.
        // return JLIA.getMethodType(memberName);
        return methodType;
    }

    @Override
    public String getDescriptor() {
        // Android-changed: Android own implementation.
        // return JLIA.getMethodDescriptor(memberName);
        return methodType.toMethodDescriptorString();
    }

    @Override
    public int getByteCodeIndex() {
        // bci not available for native methods
        if (isNativeMethod())
            return -1;

        return bci;
    }

    @Override
    public String getFileName() {
        // Android-changed: Android own implementation.
        // return toStackTraceElement().getFileName();
        return fileName;
    }

    @Override
    public int getLineNumber() {
        // line number not available for native methods
        if (isNativeMethod())
            return -2;

        // Android-changed: Android own implementation.
        // return toStackTraceElement().getLineNumber();
        return lineNumber;
    }


    @Override
    public boolean isNativeMethod() {
        // Android-changed: Android own implementation.
        // return JLIA.isNative(memberName);
        // StackTraceElement.isNativeMethod() uses -2 in line number to indicate native method.
        return lineNumber == -2;
    }

    @Override
    public String toString() {
        return toStackTraceElement().toString();
    }

    @Override
    public StackTraceElement toStackTraceElement() {
        StackTraceElement s = ste;
        if (s == null) {
            synchronized (this) {
                s = ste;
                if (s == null) {
                    ste = s = StackTraceElement.of(this);
                }
            }
        }
        return s;
    }

    private void ensureRetainClassRefEnabled() {
        if (!retainClassRef) {
            throw new UnsupportedOperationException("No access to RETAIN_CLASS_REFERENCE");
        }
    }
}
