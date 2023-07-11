/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  The Android Open Source
 * Project designates this particular file as subject to the "Classpath"
 * exception as provided by The Android Open Source Project in the LICENSE
 * file that accompanied this code.
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
 */

package java.lang.invoke;

import static dalvik.system.EmulatedStackFrame.StackFrameAccessor.copyNext;

import dalvik.system.EmulatedStackFrame;
import dalvik.system.EmulatedStackFrame.Range;
import dalvik.system.EmulatedStackFrame.RandomOrderStackFrameReader;
import dalvik.system.EmulatedStackFrame.StackFrameAccessor;
import dalvik.system.EmulatedStackFrame.StackFrameReader;
import dalvik.system.EmulatedStackFrame.StackFrameWriter;

import sun.invoke.util.Wrapper;
import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** @hide Public for testing only. */
public class Transformers {
    private Transformers() {}

    static {
        try {
            TRANSFORM_INTERNAL =
                    MethodHandle.class.getDeclaredMethod(
                            "transformInternal", EmulatedStackFrame.class);
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError();
        }
    }

    /**
     * Method reference to the private {@code MethodHandle.transformInternal} method. This is cached
     * here because it's the point of entry for all transformers.
     */
    private static final Method TRANSFORM_INTERNAL;

    /** @hide */
    public abstract static class Transformer extends MethodHandle implements Cloneable {
        protected Transformer(MethodType type) {
            super(TRANSFORM_INTERNAL.getArtMethod(), MethodHandle.INVOKE_TRANSFORM, type);
        }

        protected Transformer(MethodType type, int invokeKind) {
            super(TRANSFORM_INTERNAL.getArtMethod(), invokeKind, type);
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        /**
         * Performs a MethodHandle.invoke() call with arguments held in an
         * EmulatedStackFrame.
         * @param target the method handle to invoke
         * @param stackFrame the stack frame containing arguments for the invocation
         */
        protected static void invokeFromTransform(MethodHandle target,
                                                  EmulatedStackFrame stackFrame) throws Throwable {
            if (target instanceof Transformer) {
                ((Transformer) target).transform(stackFrame);
            } else {
                final MethodHandle adaptedTarget = target.asType(stackFrame.getMethodType());
                adaptedTarget.invokeExactWithFrame(stackFrame);
            }
        }

        /**
         * Performs a MethodHandle.invokeExact() call with arguments held in an
         * EmulatedStackFrame.
         * @param target the method handle to invoke
         * @param stackFrame the stack frame containing arguments for the invocation
         */
        protected void invokeExactFromTransform(MethodHandle target,
                                                EmulatedStackFrame stackFrame) throws Throwable {
            if (target instanceof Transformer) {
                ((Transformer) target).transform(stackFrame);
            } else {
                target.invokeExactWithFrame(stackFrame);
            }
        }
    }

    /** Implements {@code MethodHandles.throwException}. */
    static class AlwaysThrow extends Transformer {
        private final Class<? extends Throwable> exceptionType;

        AlwaysThrow(Class<?> nominalReturnType, Class<? extends Throwable> exType) {
            super(MethodType.methodType(nominalReturnType, exType));
            this.exceptionType = exType;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            throw emulatedStackFrame.getReference(0, exceptionType);
        }
    }

    /** Implements {@code MethodHandles.dropArguments}. */
    static class DropArguments extends Transformer {
        private final MethodHandle delegate;
        private final EmulatedStackFrame.Range range1;
        private final EmulatedStackFrame.Range range2;

        DropArguments(MethodType type, MethodHandle delegate, int startPos, int numDropped) {
            super(type);

            this.delegate = delegate;

            // We pre-calculate the ranges of values we have to copy through to the delegate
            // handle at the time of instantiation so that the actual invoke is performant.
            this.range1 = EmulatedStackFrame.Range.of(type, 0, startPos);
            this.range2 = EmulatedStackFrame.Range.from(type, startPos + numDropped);
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame calleeFrame = EmulatedStackFrame.create(delegate.type());

            emulatedStackFrame.copyRangeTo(
                    calleeFrame, range1, 0 /* referencesStart */, 0 /* stackFrameStart */);
            emulatedStackFrame.copyRangeTo(
                    calleeFrame, range2, range1.numReferences, range1.numBytes);

            invokeFromTransform(delegate, calleeFrame);
            calleeFrame.copyReturnValueTo(emulatedStackFrame);
        }
    }

    /** Implements {@code MethodHandles.catchException}. */
    static class CatchException extends Transformer {
        private final MethodHandle target;
        private final MethodHandle handler;
        private final Class<?> exType;

        private final EmulatedStackFrame.Range handlerArgsRange;

        CatchException(MethodHandle target, MethodHandle handler, Class<?> exType) {
            super(target.type());

            this.target = target;
            this.handler = handler;
            this.exType = exType;

            // We only copy the first "count" args, dropping others if required. Note that
            // we subtract one because the first handler arg is the exception thrown by the
            // target.
            handlerArgsRange =
                    EmulatedStackFrame.Range.of(
                            target.type(), 0, (handler.type().parameterCount() - 1));
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            try {
                invokeFromTransform(target, emulatedStackFrame);
            } catch (Throwable th) {
                if (th.getClass() == exType) {
                    // We've gotten an exception of the appropriate type, so we need to call
                    // the handler. Create a new frame of the appropriate size.
                    EmulatedStackFrame fallback = EmulatedStackFrame.create(handler.type());

                    // The first argument to the handler is the actual exception.
                    fallback.setReference(0, th);

                    // We then copy other arguments that need to be passed through to the handler.
                    // Note that we might drop arguments at the end, if needed. Note that
                    // referencesStart == 1 because the first argument is the exception type.
                    emulatedStackFrame.copyRangeTo(
                            fallback,
                            handlerArgsRange,
                            1 /* referencesStart */,
                            0 /* stackFrameStart */);

                    // Perform the invoke and return the appropriate value.
                    invokeFromTransform(handler, fallback);
                    fallback.copyReturnValueTo(emulatedStackFrame);
                } else {
                    // The exception is not of the expected type, we throw it.
                    throw th;
                }
            }
        }
    }

    /** Implements {@code MethodHandles.tryFinally}. */
    static class TryFinally extends Transformer {
        /** The target handle to try. */
        private final MethodHandle target;

        /** The cleanup handle to invoke after the target. */
        private final MethodHandle cleanup;

        TryFinally(MethodHandle target, MethodHandle cleanup) {
            super(target.type());
            this.target = target;
            this.cleanup = cleanup;
        }

        @Override
        protected void transform(EmulatedStackFrame callerFrame) throws Throwable {
            Throwable throwable = null;
            try {
                invokeExactFromTransform(target, callerFrame);
            } catch (Throwable t) {
                throwable = t;
                throw t;
            } finally {
                final EmulatedStackFrame cleanupFrame = prepareCleanupFrame(callerFrame, throwable);
                invokeExactFromTransform(cleanup, cleanupFrame);
                if (cleanup.type().returnType() != void.class) {
                    cleanupFrame.copyReturnValueTo(callerFrame);
                }
            }
        }

        /** Prepares the frame used to invoke the cleanup handle. */
        private EmulatedStackFrame prepareCleanupFrame(final EmulatedStackFrame callerFrame,
                                                       final Throwable throwable) {
            final EmulatedStackFrame cleanupFrame = EmulatedStackFrame.create(cleanup.type());
            final StackFrameWriter cleanupWriter = new StackFrameWriter();
            cleanupWriter.attach(cleanupFrame);

            // The first argument to `cleanup` is (any) pending exception kind.
            cleanupWriter.putNextReference(throwable, Throwable.class);
            int added = 1;

            // The second argument to `cleanup` is the result from `target` (if not void).
            Class<?> targetReturnType = target.type().returnType();
            StackFrameReader targetReader = new StackFrameReader();
            targetReader.attach(callerFrame);
            if (targetReturnType != void.class) {
                targetReader.makeReturnValueAccessor();
                copyNext(targetReader, cleanupWriter, targetReturnType);
                added += 1;
                // Reset `targetReader` to reference the arguments in `callerFrame`.
                targetReader.attach(callerFrame);
            }

            // The final arguments from the invocation of target. As many are copied as the cleanup
            // handle expects (it may be fewer than the arguments provided to target).
            Class<?> [] cleanupTypes = cleanup.type().parameterArray();
            for (; added != cleanupTypes.length; ++added) {
                copyNext(targetReader, cleanupWriter, cleanupTypes[added]);
            }
            return cleanupFrame;
        }
    }

    /** Implements {@code MethodHandles.GuardWithTest}. */
    static class GuardWithTest extends Transformer {
        private final MethodHandle test;
        private final MethodHandle target;
        private final MethodHandle fallback;

        private final EmulatedStackFrame.Range testArgsRange;

        GuardWithTest(MethodHandle test, MethodHandle target, MethodHandle fallback) {
            super(target.type());

            this.test = test;
            this.target = target;
            this.fallback = fallback;

            // The test method might have a subset of the arguments of the handle / target.
            testArgsRange =
                    EmulatedStackFrame.Range.of(target.type(), 0, test.type().parameterCount());
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame testFrame = EmulatedStackFrame.create(test.type());
            emulatedStackFrame.copyRangeTo(testFrame, testArgsRange, 0, 0);

            // We know that the return value for test is going to be boolean.class.
            StackFrameReader reader = new StackFrameReader();
            reader.attach(testFrame);
            reader.makeReturnValueAccessor();
            invokeFromTransform(test, testFrame);
            final boolean testResult = (boolean) reader.nextBoolean();
            if (testResult) {
                invokeFromTransform(target, emulatedStackFrame);
            } else {
                invokeFromTransform(fallback, emulatedStackFrame);
            }
        }
    }

    /** Implements {@code MethodHandles.arrayElementGetter}. */
    static class ReferenceArrayElementGetter extends Transformer {
        private final Class<?> arrayClass;

        ReferenceArrayElementGetter(Class<?> arrayClass) {
            super(
                    MethodType.methodType(
                            arrayClass.getComponentType(), new Class<?>[] {arrayClass, int.class}));
            this.arrayClass = arrayClass;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            final StackFrameReader reader = new StackFrameReader();
            reader.attach(emulatedStackFrame);

            // Read the array object and the index from the stack frame.
            final Object[] array = (Object[]) reader.nextReference(arrayClass);
            final int index = reader.nextInt();

            // Write the array element back to the stack frame.
            final StackFrameWriter writer = new StackFrameWriter();
            writer.attach(emulatedStackFrame);
            writer.makeReturnValueAccessor();
            writer.putNextReference(array[index], arrayClass.getComponentType());
        }
    }

    /** Implements {@code MethodHandles.arrayElementSetter}. */
    static class ReferenceArrayElementSetter extends Transformer {
        private final Class<?> arrayClass;

        ReferenceArrayElementSetter(Class<?> arrayClass) {
            super(
                    MethodType.methodType(
                            void.class,
                            new Class<?>[] {arrayClass, int.class, arrayClass.getComponentType()}));
            this.arrayClass = arrayClass;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            final StackFrameReader reader = new StackFrameReader();
            reader.attach(emulatedStackFrame);

            // Read the array object, index and the value to write from the stack frame.
            final Object[] array = (Object[]) reader.nextReference(arrayClass);
            final int index = reader.nextInt();
            final Object value = reader.nextReference(arrayClass.getComponentType());

            array[index] = value;
        }
    }

    /** Implements {@code MethodHandles.identity}. */
    static class ReferenceIdentity extends Transformer {
        private final Class<?> type;

        ReferenceIdentity(Class<?> type) {
            super(MethodType.methodType(type, type));
            this.type = type;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            final StackFrameReader reader = new StackFrameReader();
            reader.attach(emulatedStackFrame);

            final StackFrameWriter writer = new StackFrameWriter();
            writer.attach(emulatedStackFrame);
            writer.makeReturnValueAccessor();
            writer.putNextReference(reader.nextReference(type), type);
        }
    }

    /** Implements {@code MethodHandles.makeZero}. */
    static class ZeroValue extends Transformer {
        public ZeroValue(Class<?> type) {
            super(MethodType.methodType(type));
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            // Return-value is zero-initialized in emulatedStackFrame.
        }
    }

    /** Implements {@code MethodHandles.arrayConstructor}. */
    static class ArrayConstructor extends Transformer {
        private final Class<?> componentType;

        ArrayConstructor(Class<?> arrayType) {
            super(MethodType.methodType(arrayType, int.class));
            componentType = arrayType.getComponentType();
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            final StackFrameReader reader = new StackFrameReader();
            reader.attach(emulatedStackFrame);
            final int length = reader.nextInt();
            final Object array = Array.newInstance(componentType, length);
            emulatedStackFrame.setReturnValueTo(array);
        }
    }

    /** Implements {@code MethodHandles.arrayLength}. */
    static class ArrayLength extends Transformer {
        private final Class<?> arrayType;

        ArrayLength(Class<?> arrayType) {
            super(MethodType.methodType(int.class, arrayType));
            this.arrayType = arrayType;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            final StackFrameReader reader = new StackFrameReader();
            reader.attach(emulatedStackFrame);
            final Object arrayObject = reader.nextReference(arrayType);

            int length;
            switch (Wrapper.basicTypeChar(arrayType.getComponentType())) {
                case 'L':
                    length = ((Object[]) arrayObject).length;
                    break;
                case 'Z':
                    length = ((boolean[]) arrayObject).length;
                    break;
                case 'B':
                    length = ((byte[]) arrayObject).length;
                    break;
                case 'C':
                    length = ((char[]) arrayObject).length;
                    break;
                case 'S':
                    length = ((short[]) arrayObject).length;
                    break;
                case 'I':
                    length = ((int[]) arrayObject).length;
                    break;
                case 'J':
                    length = ((long[]) arrayObject).length;
                    break;
                case 'F':
                    length = ((float[]) arrayObject).length;
                    break;
                case 'D':
                    length = ((double[]) arrayObject).length;
                    break;
                default:
                    throw new IllegalStateException("Unsupported type: " + arrayType);
            }

            final StackFrameWriter writer = new StackFrameWriter();
            writer.attach(emulatedStackFrame).makeReturnValueAccessor();
            writer.putNextInt(length);
        }
    }

    /** Implements {@code MethodHandles.createMethodHandleForConstructor}. */
    static class Construct extends Transformer {
        private final MethodHandle constructorHandle;
        private final EmulatedStackFrame.Range callerRange;

        Construct(MethodHandle constructorHandle, MethodType returnedType) {
            super(returnedType);
            this.constructorHandle = constructorHandle;
            this.callerRange = EmulatedStackFrame.Range.all(type());
        }

        MethodHandle getConstructorHandle() {
            return constructorHandle;
        }

        private static boolean isAbstract(Class<?> klass) {
            return (klass.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT;
        }

        private static void checkInstantiable(Class<?> klass) throws InstantiationException {
            if (isAbstract(klass)) {
                String s = klass.isInterface() ? "interface " : "abstract class ";
                throw new InstantiationException("Can't instantiate " + s + klass);
            }
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            final Class<?> receiverType = constructorHandle.type().parameterType(0);
            checkInstantiable(receiverType);

            // Allocate memory for receiver.
            Object receiver = Unsafe.getUnsafe().allocateInstance(receiverType);

            // The MethodHandle type for the caller has the form of
            // {rtype=T,ptypes=A1..An}. The constructor MethodHandle is of
            // the form {rtype=void,ptypes=T,A1...An}. So the frame for
            // the constructor needs to have a slot with the receiver
            // in position 0.
            EmulatedStackFrame constructorFrame =
                    EmulatedStackFrame.create(constructorHandle.type());
            constructorFrame.setReference(0, receiver);
            emulatedStackFrame.copyRangeTo(constructorFrame, callerRange, 1, 0);
            invokeExactFromTransform(constructorHandle, constructorFrame);

            // Set return result for caller.
            emulatedStackFrame.setReturnValueTo(receiver);
        }
    }

    /** Implements {@code MethodHandle.bindTo}. */
    static class BindTo extends Transformer {
        private final MethodHandle delegate;
        private final Object receiver;

        private final EmulatedStackFrame.Range range;

        BindTo(MethodHandle delegate, Object receiver) {
            super(delegate.type().dropParameterTypes(0, 1));

            this.delegate = delegate;
            this.receiver = receiver;

            this.range = EmulatedStackFrame.Range.all(this.type());
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            // Create a new emulated stack frame with the full type (including the leading
            // receiver reference).
            EmulatedStackFrame stackFrame = EmulatedStackFrame.create(delegate.type());

            // The first reference argument must be the receiver.
            stackFrame.setReference(0, receiver);
            // Copy all other arguments.
            emulatedStackFrame.copyRangeTo(
                    stackFrame, range, 1 /* referencesStart */, 0 /* stackFrameStart */);

            // Perform the invoke.
            invokeFromTransform(delegate, stackFrame);
            stackFrame.copyReturnValueTo(emulatedStackFrame);
        }
    }

    /** Implements {@code MethodHandle.filterReturnValue}. */
    static class FilterReturnValue extends Transformer {
        private final MethodHandle target;
        private final MethodHandle filter;

        private final EmulatedStackFrame.Range allArgs;

        FilterReturnValue(MethodHandle target, MethodHandle filter) {
            super(MethodType.methodType(filter.type().rtype(), target.type().ptypes()));

            this.target = target;
            this.filter = filter;

            allArgs = EmulatedStackFrame.Range.all(type());
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            // Create a new frame with the target's type and copy all arguments over.
            // This frame differs in return type with |emulatedStackFrame| but will have
            // the same parameter shapes.
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());
            emulatedStackFrame.copyRangeTo(targetFrame, allArgs, 0, 0);
            invokeFromTransform(target, targetFrame);

            // Create an emulated frame for the filter and move the return value from
            // target to the argument of the filter.
            final EmulatedStackFrame filterFrame = EmulatedStackFrame.create(filter.type());
            final Class<?> filterArgumentType = target.type().rtype();
            if (filterArgumentType != void.class) {
                final StackFrameReader returnValueReader = new StackFrameReader();
                returnValueReader.attach(targetFrame).makeReturnValueAccessor();

                final StackFrameWriter filterWriter = new StackFrameWriter();
                filterWriter.attach(filterFrame);
                StackFrameAccessor.copyNext(returnValueReader, filterWriter, filterArgumentType);
            }

            // Invoke the filter and copy its return value back to the original frame.
            invokeExactFromTransform(filter, filterFrame);
            filterFrame.copyReturnValueTo(emulatedStackFrame);
        }
    }

    /** Implements {@code MethodHandles.permuteArguments}. */
    static class PermuteArguments extends Transformer {
        private final MethodHandle target;
        private final int[] reorder;

        PermuteArguments(MethodType type, MethodHandle target, int[] reorder) {
            super(type);

            this.target = target;
            this.reorder = reorder;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            final RandomOrderStackFrameReader reader = new RandomOrderStackFrameReader();
            reader.attach(emulatedStackFrame);

            final EmulatedStackFrame calleeFrame = EmulatedStackFrame.create(target.type());
            final StackFrameWriter writer = new StackFrameWriter();
            writer.attach(calleeFrame);

            final Class<?> [] ptypes = emulatedStackFrame.getMethodType().parameterArray();
            for (int i = 0; i < reorder.length; ++i) {
                final int readerIndex = reorder[i];
                reader.moveTo(readerIndex);
                StackFrameAccessor.copyNext(reader, writer, ptypes[readerIndex]);
            }

            invokeFromTransform(target, calleeFrame);
            calleeFrame.copyReturnValueTo(emulatedStackFrame);
        }
    }

    /** Implements {@code MethodHandle.asVarargsCollector}. */
    static class VarargsCollector extends Transformer {
        final MethodHandle target;
        private final Class<?> arrayType;

        VarargsCollector(MethodHandle target) {
            super(target.type());

            Class<?>[] parameterTypes = target.type().ptypes();
            if (!lastParameterTypeIsAnArray(parameterTypes)) {
                throw new IllegalArgumentException("target does not have array as last parameter");
            }
            this.target = target;
            this.arrayType = parameterTypes[parameterTypes.length - 1];
        }

        private static boolean lastParameterTypeIsAnArray(Class<?>[] parameterTypes) {
            if (parameterTypes.length == 0) return false;
            return parameterTypes[parameterTypes.length - 1].isArray();
        }

        @Override
        public boolean isVarargsCollector() {
            return true;
        }

        @Override
        public MethodHandle asFixedArity() {
            return target;
        }

        @Override
        MethodHandle asTypeUncached(MethodType newType) {
            // asType() behavior is specialized per:
            //
            // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/invoke/MethodHandle.html#asVarargsCollector(java.lang.Class)
            //
            // "The behavior of asType is also specialized for variable arity adapters, to maintain
            //  the invariant that plain, inexact invoke is always equivalent to an asType call to
            //  adjust the target type, followed by invokeExact. Therefore, a variable arity
            //  adapter responds to an asType request by building a fixed arity collector, if and
            //  only if the adapter and requested type differ either in arity or trailing argument
            //  type. The resulting fixed arity collector has its type further adjusted
            //  (if necessary) to the requested type by pairwise conversion, as if by another
            //  application of asType."
            final MethodType currentType = type();
            final MethodHandle currentFixedArity = asFixedArity();
            if (currentType.parameterCount() == newType.parameterCount()
                    && currentType
                            .lastParameterType()
                            .isAssignableFrom(newType.lastParameterType())) {
                return asTypeCache = currentFixedArity.asType(newType);
            }

            final int arrayLength = newType.parameterCount() - currentType.parameterCount() + 1;
            if (arrayLength < 0) {
                // arrayType is definitely array per VarargsCollector constructor.
                throwWrongMethodTypeException(currentType, newType);
            }

            MethodHandle collector = null;
            try {
                collector = currentFixedArity.asCollector(arrayType, arrayLength).asType(newType);
            } catch (IllegalArgumentException ex) {
                throwWrongMethodTypeException(currentType, newType);
            }
            return asTypeCache = collector;
        }

        @Override
        public void transform(EmulatedStackFrame callerFrame) throws Throwable {
            MethodType callerFrameType = callerFrame.getMethodType();
            Class<?>[] callerPTypes = callerFrameType.ptypes();
            Class<?>[] targetPTypes = type().ptypes();

            int lastTargetIndex = targetPTypes.length - 1;
            if (callerPTypes.length == targetPTypes.length
                    && targetPTypes[lastTargetIndex].isAssignableFrom(
                            callerPTypes[lastTargetIndex])) {
                // Caller frame matches target frame in the arity array parameter. Invoke
                // immediately, and let the invoke() dispatch perform any necessary conversions
                // on the other parameters present.
                invokeFromTransform(target, callerFrame);
                return;
            }

            if (callerPTypes.length < targetPTypes.length - 1) {
                // Too few arguments to be compatible with variable arity invocation.
                throwWrongMethodTypeException(callerFrameType, type());
            }

            if (!MethodType.canConvert(type().rtype(), callerFrameType.rtype())) {
                // Incompatible return type.
                throwWrongMethodTypeException(callerFrameType, type());
            }

            Class<?> elementType = targetPTypes[lastTargetIndex].getComponentType();
            if (!arityArgumentsConvertible(callerPTypes, lastTargetIndex, elementType)) {
                // Wrong types to be compatible with variable arity invocation.
                throwWrongMethodTypeException(callerFrameType, type());
            }

            // Allocate targetFrame.
            MethodType targetFrameType = makeTargetFrameType(callerFrameType, type());
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(targetFrameType);
            prepareFrame(callerFrame, targetFrame);

            // Invoke target.
            invokeExactFromTransform(target, targetFrame);

            // Copy return value to the caller's frame.
            targetFrame.copyReturnValueTo(callerFrame);
        }

        @Override
        public MethodHandle withVarargs(boolean makeVarargs) {
            return makeVarargs ? this : target;
        }

        private static void throwWrongMethodTypeException(MethodType from, MethodType to) {
            throw new WrongMethodTypeException("Cannot convert " + from + " to " + to);
        }

        private static boolean arityArgumentsConvertible(
                Class<?>[] ptypes, int arityStart, Class<?> elementType) {
            if (ptypes.length - 1 == arityStart) {
                if (ptypes[arityStart].isArray()
                        && ptypes[arityStart].getComponentType() == elementType) {
                    // The last ptype is in the same position as the arity
                    // array and has the same type.
                    return true;
                }
            }

            for (int i = arityStart; i < ptypes.length; ++i) {
                if (!MethodType.canConvert(ptypes[i], elementType)) {
                    return false;
                }
            }
            return true;
        }

        private static Object referenceArray(
                StackFrameReader reader,
                Class<?>[] ptypes,
                Class<?> elementType,
                int offset,
                int length) {
            Object arityArray = Array.newInstance(elementType, length);
            for (int i = 0; i < length; ++i) {
                Class<?> argumentType = ptypes[i + offset];
                Object o = null;
                switch (Wrapper.basicTypeChar(argumentType)) {
                    case 'L':
                        o = reader.nextReference(argumentType);
                        break;
                    case 'I':
                        o = reader.nextInt();
                        break;
                    case 'J':
                        o = reader.nextLong();
                        break;
                    case 'B':
                        o = reader.nextByte();
                        break;
                    case 'S':
                        o = reader.nextShort();
                        break;
                    case 'C':
                        o = reader.nextChar();
                        break;
                    case 'Z':
                        o = reader.nextBoolean();
                        break;
                    case 'F':
                        o = reader.nextFloat();
                        break;
                    case 'D':
                        o = reader.nextDouble();
                        break;
                }
                Array.set(arityArray, i, elementType.cast(o));
            }
            return arityArray;
        }

        private static Object intArray(
                StackFrameReader reader, Class<?> ptypes[], int offset, int length) {
            int[] arityArray = new int[length];
            for (int i = 0; i < length; ++i) {
                Class<?> argumentType = ptypes[i + offset];
                switch (Wrapper.basicTypeChar(argumentType)) {
                    case 'I':
                        arityArray[i] = reader.nextInt();
                        break;
                    case 'S':
                        arityArray[i] = reader.nextShort();
                        break;
                    case 'B':
                        arityArray[i] = reader.nextByte();
                        break;
                    default:
                        arityArray[i] = (Integer) reader.nextReference(argumentType);
                        break;
                }
            }
            return arityArray;
        }

        private static Object longArray(
                StackFrameReader reader, Class<?> ptypes[], int offset, int length) {
            long[] arityArray = new long[length];
            for (int i = 0; i < length; ++i) {
                Class<?> argumentType = ptypes[i + offset];
                switch (Wrapper.basicTypeChar(argumentType)) {
                    case 'J':
                        arityArray[i] = reader.nextLong();
                        break;
                    case 'I':
                        arityArray[i] = reader.nextInt();
                        break;
                    case 'S':
                        arityArray[i] = reader.nextShort();
                        break;
                    case 'B':
                        arityArray[i] = reader.nextByte();
                        break;
                    default:
                        arityArray[i] = (Long) reader.nextReference(argumentType);
                        break;
                }
            }
            return arityArray;
        }

        private static Object byteArray(
                StackFrameReader reader, Class<?> ptypes[], int offset, int length) {
            byte[] arityArray = new byte[length];
            for (int i = 0; i < length; ++i) {
                Class<?> argumentType = ptypes[i + offset];
                switch (Wrapper.basicTypeChar(argumentType)) {
                    case 'B':
                        arityArray[i] = reader.nextByte();
                        break;
                    default:
                        arityArray[i] = (Byte) reader.nextReference(argumentType);
                        break;
                }
            }
            return arityArray;
        }

        private static Object shortArray(
                StackFrameReader reader, Class<?> ptypes[], int offset, int length) {
            short[] arityArray = new short[length];
            for (int i = 0; i < length; ++i) {
                Class<?> argumentType = ptypes[i + offset];
                switch (Wrapper.basicTypeChar(argumentType)) {
                    case 'S':
                        arityArray[i] = reader.nextShort();
                        break;
                    case 'B':
                        arityArray[i] = reader.nextByte();
                        break;
                    default:
                        arityArray[i] = (Short) reader.nextReference(argumentType);
                        break;
                }
            }
            return arityArray;
        }

        private static Object charArray(
                StackFrameReader reader, Class<?> ptypes[], int offset, int length) {
            char[] arityArray = new char[length];
            for (int i = 0; i < length; ++i) {
                Class<?> argumentType = ptypes[i + offset];
                switch (Wrapper.basicTypeChar(argumentType)) {
                    case 'C':
                        arityArray[i] = reader.nextChar();
                        break;
                    default:
                        arityArray[i] = (Character) reader.nextReference(argumentType);
                        break;
                }
            }
            return arityArray;
        }

        private static Object booleanArray(
                StackFrameReader reader, Class<?> ptypes[], int offset, int length) {
            boolean[] arityArray = new boolean[length];
            for (int i = 0; i < length; ++i) {
                Class<?> argumentType = ptypes[i + offset];
                switch (Wrapper.basicTypeChar(argumentType)) {
                    case 'Z':
                        arityArray[i] = reader.nextBoolean();
                        break;
                    default:
                        arityArray[i] = (Boolean) reader.nextReference(argumentType);
                        break;
                }
            }
            return arityArray;
        }

        private static Object floatArray(
                StackFrameReader reader, Class<?> ptypes[], int offset, int length) {
            float[] arityArray = new float[length];
            for (int i = 0; i < length; ++i) {
                Class<?> argumentType = ptypes[i + offset];
                switch (Wrapper.basicTypeChar(argumentType)) {
                    case 'F':
                        arityArray[i] = reader.nextFloat();
                        break;
                    case 'J':
                        arityArray[i] = reader.nextLong();
                        break;
                    case 'I':
                        arityArray[i] = reader.nextInt();
                        break;
                    case 'S':
                        arityArray[i] = reader.nextShort();
                        break;
                    case 'B':
                        arityArray[i] = reader.nextByte();
                        break;
                    default:
                        arityArray[i] = (Float) reader.nextReference(argumentType);
                        break;
                }
            }
            return arityArray;
        }

        private static Object doubleArray(
                StackFrameReader reader, Class<?> ptypes[], int offset, int length) {
            double[] arityArray = new double[length];
            for (int i = 0; i < length; ++i) {
                Class<?> argumentType = ptypes[i + offset];
                switch (Wrapper.basicTypeChar(argumentType)) {
                    case 'D':
                        arityArray[i] = reader.nextDouble();
                        break;
                    case 'F':
                        arityArray[i] = reader.nextFloat();
                        break;
                    case 'J':
                        arityArray[i] = reader.nextLong();
                        break;
                    case 'I':
                        arityArray[i] = reader.nextInt();
                        break;
                    case 'S':
                        arityArray[i] = reader.nextShort();
                        break;
                    case 'B':
                        arityArray[i] = reader.nextByte();
                        break;
                    default:
                        arityArray[i] = (Double) reader.nextReference(argumentType);
                        break;
                }
            }
            return arityArray;
        }

        private static Object makeArityArray(
                MethodType callerFrameType,
                StackFrameReader callerFrameReader,
                int indexOfArityArray,
                Class<?> arityArrayType) {
            int arityArrayLength = callerFrameType.ptypes().length - indexOfArityArray;
            Class<?> elementType = arityArrayType.getComponentType();
            Class<?>[] callerPTypes = callerFrameType.ptypes();

            char elementBasicType = Wrapper.basicTypeChar(elementType);
            switch (elementBasicType) {
                case 'L':
                    return referenceArray(
                            callerFrameReader,
                            callerPTypes,
                            elementType,
                            indexOfArityArray,
                            arityArrayLength);
                case 'I':
                    return intArray(
                            callerFrameReader, callerPTypes,
                            indexOfArityArray, arityArrayLength);
                case 'J':
                    return longArray(
                            callerFrameReader, callerPTypes,
                            indexOfArityArray, arityArrayLength);
                case 'B':
                    return byteArray(
                            callerFrameReader, callerPTypes,
                            indexOfArityArray, arityArrayLength);
                case 'S':
                    return shortArray(
                            callerFrameReader, callerPTypes,
                            indexOfArityArray, arityArrayLength);
                case 'C':
                    return charArray(
                            callerFrameReader, callerPTypes,
                            indexOfArityArray, arityArrayLength);
                case 'Z':
                    return booleanArray(
                            callerFrameReader, callerPTypes,
                            indexOfArityArray, arityArrayLength);
                case 'F':
                    return floatArray(
                            callerFrameReader, callerPTypes,
                            indexOfArityArray, arityArrayLength);
                case 'D':
                    return doubleArray(
                            callerFrameReader, callerPTypes,
                            indexOfArityArray, arityArrayLength);
            }
            throw new InternalError("Unexpected type: " + elementType);
        }

        public static Object collectArguments(
                char basicComponentType,
                Class<?> componentType,
                StackFrameReader reader,
                Class<?>[] types,
                int startIdx,
                int length) {
            switch (basicComponentType) {
                case 'L':
                    return referenceArray(reader, types, componentType, startIdx, length);
                case 'I':
                    return intArray(reader, types, startIdx, length);
                case 'J':
                    return longArray(reader, types, startIdx, length);
                case 'B':
                    return byteArray(reader, types, startIdx, length);
                case 'S':
                    return shortArray(reader, types, startIdx, length);
                case 'C':
                    return charArray(reader, types, startIdx, length);
                case 'Z':
                    return booleanArray(reader, types, startIdx, length);
                case 'F':
                    return floatArray(reader, types, startIdx, length);
                case 'D':
                    return doubleArray(reader, types, startIdx, length);
            }
            throw new InternalError("Unexpected type: " + basicComponentType);
        }

        private static void copyParameter(
                StackFrameReader reader, StackFrameWriter writer, Class<?> ptype) {
            switch (Wrapper.basicTypeChar(ptype)) {
                case 'L':
                    writer.putNextReference(reader.nextReference(ptype), ptype);
                    break;
                case 'I':
                    writer.putNextInt(reader.nextInt());
                    break;
                case 'J':
                    writer.putNextLong(reader.nextLong());
                    break;
                case 'B':
                    writer.putNextByte(reader.nextByte());
                    break;
                case 'S':
                    writer.putNextShort(reader.nextShort());
                    break;
                case 'C':
                    writer.putNextChar(reader.nextChar());
                    break;
                case 'Z':
                    writer.putNextBoolean(reader.nextBoolean());
                    break;
                case 'F':
                    writer.putNextFloat(reader.nextFloat());
                    break;
                case 'D':
                    writer.putNextDouble(reader.nextDouble());
                    break;
                default:
                    throw new InternalError("Unexpected type: " + ptype);
            }
        }

        private static void prepareFrame(
                EmulatedStackFrame callerFrame, EmulatedStackFrame targetFrame) {
            StackFrameWriter targetWriter = new StackFrameWriter();
            targetWriter.attach(targetFrame);
            StackFrameReader callerReader = new StackFrameReader();
            callerReader.attach(callerFrame);

            // Copy parameters from |callerFrame| to |targetFrame| leaving room for arity array.
            MethodType targetMethodType = targetFrame.getMethodType();
            int indexOfArityArray = targetMethodType.ptypes().length - 1;
            for (int i = 0; i < indexOfArityArray; ++i) {
                Class<?> ptype = targetMethodType.ptypes()[i];
                copyParameter(callerReader, targetWriter, ptype);
            }

            // Add arity array as last parameter in |targetFrame|.
            Class<?> arityArrayType = targetMethodType.ptypes()[indexOfArityArray];
            Object arityArray =
                    makeArityArray(
                            callerFrame.getMethodType(),
                            callerReader,
                            indexOfArityArray,
                            arityArrayType);
            targetWriter.putNextReference(arityArray, arityArrayType);
        }

        /**
         * Computes the frame type to invoke the target method handle with. This is the same as the
         * caller frame type, but with the trailing argument being the array type that is the
         * trailing argument in the target method handle.
         *
         * <p>Suppose the targetType is (T0, T1, T2[])RT and the callerType is (C0, C1, C2, C3)RC
         * then the constructed type is (C0, C1, T2[])RC.
         */
        private static MethodType makeTargetFrameType(
                MethodType callerType, MethodType targetType) {
            final int ptypesLength = targetType.ptypes().length;
            final Class<?>[] ptypes = new Class<?>[ptypesLength];
            // Copy types from caller types to new methodType.
            System.arraycopy(callerType.ptypes(), 0, ptypes, 0, ptypesLength - 1);
            // Set the last element in the type array to be the
            // varargs array of the target.
            ptypes[ptypesLength - 1] = targetType.ptypes()[ptypesLength - 1];
            return MethodType.methodType(callerType.rtype(), ptypes);
        }
    }

    /** Implements {@code MethodHandles.invoker} and {@code MethodHandles.exactInvoker}. */
    static class Invoker extends Transformer {
        private final MethodType targetType;
        private final boolean isExactInvoker;
        private final EmulatedStackFrame.Range copyRange;

        Invoker(MethodType targetType, boolean isExactInvoker) {
            super(targetType.insertParameterTypes(0, MethodHandle.class));
            this.targetType = targetType;
            this.isExactInvoker = isExactInvoker;
            copyRange = EmulatedStackFrame.Range.from(type(), 1);
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            // The first argument to the stack frame is the handle that needs to be invoked.
            MethodHandle target = emulatedStackFrame.getReference(0, MethodHandle.class);

            // All other arguments must be copied to the target frame.
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(targetType);
            emulatedStackFrame.copyRangeTo(targetFrame, copyRange, 0, 0);

            // Finally, invoke the handle and copy the return value.
            if (isExactInvoker) {
                invokeExactFromTransform(target, targetFrame);
            } else {
                invokeFromTransform(target, targetFrame);
            }
            targetFrame.copyReturnValueTo(emulatedStackFrame);
        }

        /**
         * Checks whether two method types are compatible as an exact match. The exact match is
         * based on the erased form (all reference types treated as Object).
         *
         * @param callsiteType the MethodType associated with the invocation.
         * @param targetType the MethodType of the MethodHandle being invoked.
         * @return true if {@code callsiteType} and {@code targetType} are an exact match.
         */
        private static boolean exactMatch(MethodType callsiteType, MethodType targetType) {
            final int parameterCount = callsiteType.parameterCount();
            if (callsiteType.parameterCount() != targetType.parameterCount()) {
                return false;
            }

            for (int i = 0; i < parameterCount; ++i) {
                Class argumentType = callsiteType.parameterType(i);
                Class parameterType = targetType.parameterType(i);
                if (!exactMatch(argumentType, parameterType)) {
                    return false;
                }
            }
            // Check return type, noting it's always okay to discard the return value.
            return callsiteType.returnType() == Void.TYPE
                    || exactMatch(callsiteType.returnType(), targetType.returnType());
        }

        /**
         * Checks whether two types are an exact match. The exact match is based on the erased types
         * so any two reference types match, but primitive types must be the same.
         *
         * @param lhs first class to compare.
         * @param rhs second class to compare.
         * @return true if both classes satisfy the exact match criteria.
         */
        private static boolean exactMatch(Class lhs, Class rhs) {
            if (lhs.isPrimitive() || rhs.isPrimitive()) {
                return lhs == rhs;
            }
            return true; // Both types are references, compatibility is checked at the point of use.
        }
    }

    /** Implements {@code MethodHandle.asSpreader}. */
    static class Spreader extends Transformer {
        /** The method handle we're delegating to. */
        private final MethodHandle target;

        /**
         * The offset of the trailing array argument in the list of arguments to this transformer.
         * The array argument is always the last argument.
         */
        private final int arrayOffset;

        /** The component type of the array. */
        private final Class<?> componentType;

        /**
         * The number of input arguments that will be present in the array. In other words, this is
         * the expected array length.
         */
        private final int numArrayArgs;

        /**
         * Range of arguments to copy verbatim from the input frame, This will cover all arguments
         * that aren't a part of the trailing array.
         */
        private final Range leadingRange;
        private final Range trailingRange;

        Spreader(MethodHandle target, MethodType spreaderType, int spreadArgPos, int numArrayArgs) {
            super(spreaderType);
            this.target = target;
            arrayOffset = spreadArgPos;
            componentType = spreaderType.ptypes()[arrayOffset].getComponentType();
            if (componentType == null) {
                throw new AssertionError("Argument " + spreadArgPos + " must be an array.");
            }
            this.numArrayArgs = numArrayArgs;
            // Copy all args except the spreader array.
            leadingRange = EmulatedStackFrame.Range.of(spreaderType, 0, arrayOffset);
            trailingRange = EmulatedStackFrame.Range.from(spreaderType, arrayOffset + 1);
        }

        @Override
        public void transform(EmulatedStackFrame callerFrame) throws Throwable {
            // Get the array reference and check that its length is as expected.
            final Class<?> arrayType = type().parameterType(arrayOffset);
            final Object arrayObj = callerFrame.getReference(arrayOffset, arrayType);

            // The incoming array may be null if the expected number of array arguments is zero.
            final int arrayLength =
                (numArrayArgs == 0 && arrayObj == null) ? 0 : Array.getLength(arrayObj);
            if (arrayLength != numArrayArgs) {
                throw new IllegalArgumentException(
                        "Invalid array length " + arrayLength + " expected " + numArrayArgs);
            }

            // Create a new stack frame for the callee.
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());

            // Copy ranges not affected by the spreading.
            callerFrame.copyRangeTo(targetFrame, leadingRange, 0, 0);
            if (componentType.isPrimitive()) {
                final int elementBytes = EmulatedStackFrame.getSize(componentType);
                final int spreadBytes = elementBytes * arrayLength;
                callerFrame.copyRangeTo(targetFrame, trailingRange,
                    leadingRange.numReferences, leadingRange.numBytes + spreadBytes);
            } else {
                callerFrame.copyRangeTo(targetFrame, trailingRange,
                    leadingRange.numReferences + numArrayArgs, leadingRange.numBytes);
            }

            if (arrayLength != 0) {
                StackFrameWriter writer = new StackFrameWriter();
                writer.attach(targetFrame,
                              arrayOffset,
                              leadingRange.numReferences,
                              leadingRange.numBytes);
                spreadArray(arrayType, arrayObj, writer);
            }

            invokeExactFromTransform(target, targetFrame);
            targetFrame.copyReturnValueTo(callerFrame);
        }

        private void spreadArray(Class<?> arrayType, Object arrayObj, StackFrameWriter writer) {
            final Class<?> componentType = arrayType.getComponentType();
            switch (Wrapper.basicTypeChar(componentType)) {
                case 'L':
                {
                    final Object[] array = (Object[]) arrayObj;
                    for (int i = 0; i < array.length; ++i) {
                        writer.putNextReference(array[i], componentType);
                    }
                    break;
                }
                case 'I':
                {
                    final int[] array = (int[]) arrayObj;
                    for (int i = 0; i < array.length; ++i) {
                        writer.putNextInt(array[i]);
                    }
                    break;
                }
                case 'J':
                {
                    final long[] array = (long[]) arrayObj;
                    for (int i = 0; i < array.length; ++i) {
                        writer.putNextLong(array[i]);
                    }
                    break;
                }
                case 'B':
                {
                    final byte[] array = (byte[]) arrayObj;
                    for (int i = 0; i < array.length; ++i) {
                        writer.putNextByte(array[i]);
                    }
                    break;
                }
                case 'S':
                {
                    final short[] array = (short[]) arrayObj;
                    for (int i = 0; i < array.length; ++i) {
                        writer.putNextShort(array[i]);
                    }
                    break;
                }
                case 'C':
                {
                    final char[] array = (char[]) arrayObj;
                    for (int i = 0; i < array.length; ++i) {
                        writer.putNextChar(array[i]);
                    }
                    break;
                }
                case 'Z':
                {
                    final boolean[] array = (boolean[]) arrayObj;
                    for (int i = 0; i < array.length; ++i) {
                        writer.putNextBoolean(array[i]);
                    }
                    break;
                }
                case 'F':
                {
                    final float[] array = (float[]) arrayObj;
                    for (int i = 0; i < array.length; ++i) {
                        writer.putNextFloat(array[i]);
                    }
                    break;
                }
                case 'D':
                {
                    final double[] array = (double[]) arrayObj;
                    for (int i = 0; i < array.length; ++i) {
                        writer.putNextDouble(array[i]);
                    }
                    break;
                }
            }
        }
    }

    /** Implements {@code MethodHandle.asCollector}. */
    static class Collector extends Transformer {
        private final MethodHandle target;

        /**
         * The array start is the position in the target outputs of the array collecting arguments
         * and the position in the source inputs where collection starts.
         */
        private final int arrayOffset;

        /**
         * The array length is the number of arguments to be collected.
         */
        private final int arrayLength;

        /** The type of the array. */
        private final Class arrayType;

        /**
         * Range of arguments to copy verbatim from the start of the input frame.
         */
        private final Range leadingRange;

        /**
         * Range of arguments to copy verbatim from the end of the input frame.
         */
        private final Range trailingRange;

        Collector(MethodHandle delegate, Class<?> arrayType, int start, int length) {
            super(delegate.type().asCollectorType(arrayType, start, length));
            this.target = delegate;
            this.arrayOffset = start;
            this.arrayLength = length;
            this.arrayType = arrayType;

            // Build ranges of arguments to be copied.
            leadingRange = EmulatedStackFrame.Range.of(type(), 0, arrayOffset);
            trailingRange = EmulatedStackFrame.Range.from(type(), arrayOffset + arrayLength);
        }

        @Override
        public void transform(EmulatedStackFrame callerFrame) throws Throwable {
            // Create a new stack frame for the callee.
            final EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());

            // Copy arguments before the collector array.
            callerFrame.copyRangeTo(targetFrame, leadingRange, 0, 0);

            // Copy arguments after the collector array.
            callerFrame.copyRangeTo(targetFrame, trailingRange,
                leadingRange.numReferences + 1, leadingRange.numBytes);

            // Collect arguments between arrayOffset and arrayOffset + arrayLength.
            final StackFrameWriter writer = new StackFrameWriter();
            writer.attach(targetFrame, arrayOffset, leadingRange.numReferences, leadingRange.numBytes);
            final StackFrameReader reader = new StackFrameReader();
            reader.attach(callerFrame, arrayOffset, leadingRange.numReferences, leadingRange.numBytes);

            final char arrayTypeChar = Wrapper.basicTypeChar(arrayType.getComponentType());
            switch (arrayTypeChar) {
                case 'L':
                    {
                        // Reference arrays are the only case where the component type of the
                        // array we construct might differ from the type of the reference we read
                        // from the stack frame.
                        final Class<?> targetType = target.type().ptypes()[arrayOffset];
                        final Class<?> arrayComponentType = arrayType.getComponentType();
                        Object[] arr =
                                (Object[]) Array.newInstance(arrayComponentType, arrayLength);
                        for (int i = 0; i < arrayLength; ++i) {
                            arr[i] = reader.nextReference(arrayComponentType);
                        }
                        writer.putNextReference(arr, targetType);
                        break;
                    }
                case 'I':
                    {
                        int[] array = new int[arrayLength];
                        for (int i = 0; i < arrayLength; ++i) {
                            array[i] = reader.nextInt();
                        }
                        writer.putNextReference(array, int[].class);
                        break;
                    }
                case 'J':
                    {
                        long[] array = new long[arrayLength];
                        for (int i = 0; i < arrayLength; ++i) {
                            array[i] = reader.nextLong();
                        }
                        writer.putNextReference(array, long[].class);
                        break;
                    }
                case 'B':
                    {
                        byte[] array = new byte[arrayLength];
                        for (int i = 0; i < arrayLength; ++i) {
                            array[i] = reader.nextByte();
                        }
                        writer.putNextReference(array, byte[].class);
                        break;
                    }
                case 'S':
                    {
                        short[] array = new short[arrayLength];
                        for (int i = 0; i < arrayLength; ++i) {
                            array[i] = reader.nextShort();
                        }
                        writer.putNextReference(array, short[].class);
                        break;
                    }
                case 'C':
                    {
                        char[] array = new char[arrayLength];
                        for (int i = 0; i < arrayLength; ++i) {
                            array[i] = reader.nextChar();
                        }
                        writer.putNextReference(array, char[].class);
                        break;
                    }
                case 'Z':
                    {
                        boolean[] array = new boolean[arrayLength];
                        for (int i = 0; i < arrayLength; ++i) {
                            array[i] = reader.nextBoolean();
                        }
                        writer.putNextReference(array, boolean[].class);
                        break;
                    }
                case 'F':
                    {
                        float[] array = new float[arrayLength];
                        for (int i = 0; i < arrayLength; ++i) {
                            array[i] = reader.nextFloat();
                        }
                        writer.putNextReference(array, float[].class);
                        break;
                    }
                case 'D':
                    {
                        double[] array = new double[arrayLength];
                        for (int i = 0; i < arrayLength; ++i) {
                            array[i] = reader.nextDouble();
                        }
                        writer.putNextReference(array, double[].class);
                        break;
                    }
            }

            invokeFromTransform(target, targetFrame);
            targetFrame.copyReturnValueTo(callerFrame);
        }
    }

    /** Implements {@code MethodHandles.filterArguments}. */
    static class FilterArguments extends Transformer {
        /** The target handle. */
        private final MethodHandle target;
        /** Index of the first argument to filter */
        private final int pos;
        /** The list of filters to apply */
        private final MethodHandle[] filters;

        FilterArguments(MethodHandle target, int pos, MethodHandle... filters) {
            super(deriveType(target, pos, filters));

            this.target = target;
            this.pos = pos;
            this.filters = filters;
        }

        private static MethodType deriveType(MethodHandle target, int pos, MethodHandle[] filters) {
            final Class<?>[] filterArgs = new Class<?>[filters.length];
            for (int i = 0; i < filters.length; ++i) {
                filterArgs[i] = filters[i].type().parameterType(0);
            }

            return target.type().replaceParameterTypes(pos, pos + filters.length, filterArgs);
        }

        @Override
        public void transform(EmulatedStackFrame stackFrame) throws Throwable {
            final StackFrameReader reader = new StackFrameReader();
            reader.attach(stackFrame);

            EmulatedStackFrame transformedFrame = EmulatedStackFrame.create(target.type());
            final StackFrameWriter writer = new StackFrameWriter();
            writer.attach(transformedFrame);

            final Class<?>[] ptypes = target.type().ptypes();
            for (int i = 0; i < ptypes.length; ++i) {
                // Check whether the current argument has a filter associated with it.
                // If it has no filter, no further action need be taken.
                final Class<?> ptype = ptypes[i];
                final MethodHandle filter;
                if (i < pos) {
                    filter = null;
                } else if (i >= pos + filters.length) {
                    filter = null;
                } else {
                    filter = filters[i - pos];
                }

                if (filter != null) {
                    // Note that filter.type() must be (ptype)ptype - this is checked before
                    // this transformer is created.
                    EmulatedStackFrame filterFrame = EmulatedStackFrame.create(filter.type());

                    //  Copy the next argument from the stack frame to the filter frame.
                    final StackFrameWriter filterWriter = new StackFrameWriter();
                    filterWriter.attach(filterFrame);
                    copyNext(reader, filterWriter, filter.type().ptypes()[0]);

                    invokeFromTransform(filter, filterFrame);

                    // Copy the argument back from the filter frame to the stack frame.
                    final StackFrameReader filterReader = new StackFrameReader();
                    filterReader.attach(filterFrame);
                    filterReader.makeReturnValueAccessor();
                    copyNext(filterReader, writer, ptype);
                } else {
                    // There's no filter associated with this frame, just copy the next argument
                    // over.
                    copyNext(reader, writer, ptype);
                }
            }

            invokeFromTransform(target, transformedFrame);
            transformedFrame.copyReturnValueTo(stackFrame);
        }
    }

    /** Implements {@code MethodHandles.collectArguments}. */
    static class CollectArguments extends Transformer {
        private final MethodHandle target;
        private final MethodHandle collector;
        private final int pos;

        /** The range of input arguments we copy to the collector. */
        private final Range collectorRange;

        /**
         * The first range of arguments we copy to the target. These are arguments in the range [0,
         * pos). Note that arg[pos] is the return value of the filter.
         */
        private final Range range1;

        /**
         * The second range of arguments we copy to the target. These are arguments in the range
         * (pos, N], where N is the number of target arguments.
         */
        private final Range range2;

        private final int referencesOffset;
        private final int stackFrameOffset;

        CollectArguments(
                MethodHandle target, MethodHandle collector, int pos, MethodType adapterType) {
            super(adapterType);

            this.target = target;
            this.collector = collector;
            this.pos = pos;

            final int numFilterArgs = collector.type().parameterCount();
            collectorRange = Range.of(type(), pos, pos + numFilterArgs);

            range1 = Range.of(type(), 0, pos);
            this.range2 = Range.from(type(), pos + numFilterArgs);

            // Calculate the number of primitive bytes (or references) we copy to the
            // target frame based on the return value of the combiner.
            final Class<?> collectorRType = collector.type().rtype();
            if (collectorRType == void.class) {
                stackFrameOffset = 0;
                referencesOffset = 0;
            } else if (collectorRType.isPrimitive()) {
                stackFrameOffset = EmulatedStackFrame.getSize(collectorRType);
                referencesOffset = 0;
            } else {
                stackFrameOffset = 0;
                referencesOffset = 1;
            }
        }

        @Override
        public void transform(EmulatedStackFrame stackFrame) throws Throwable {
            // First invoke the collector.
            EmulatedStackFrame filterFrame = EmulatedStackFrame.create(collector.type());
            stackFrame.copyRangeTo(filterFrame, collectorRange, 0, 0);
            invokeFromTransform(collector, filterFrame);

            // Start constructing the target frame.
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());
            stackFrame.copyRangeTo(targetFrame, range1, 0, 0);

            // If one of these offsets is not zero, we have a return value to copy.
            if (referencesOffset != 0 || stackFrameOffset != 0) {
                final StackFrameReader reader = new StackFrameReader();
                reader.attach(filterFrame).makeReturnValueAccessor();
                final StackFrameWriter writer = new StackFrameWriter();
                writer.attach(targetFrame, pos, range1.numReferences, range1.numBytes);
                copyNext(reader, writer, target.type().ptypes()[0]);
            }

            stackFrame.copyRangeTo(
                    targetFrame,
                    range2,
                    range1.numReferences + referencesOffset,
                    range2.numBytes + stackFrameOffset);

            invokeFromTransform(target, targetFrame);
            targetFrame.copyReturnValueTo(stackFrame);
        }
    }

    /** Implements {@code MethodHandles.foldArguments}. */
    static class FoldArguments extends Transformer {
        private final MethodHandle target;
        private final MethodHandle combiner;
        private final int position;

        /** The range of arguments in our frame passed to the combiner. */
        private final Range combinerArgs;

        /** The range of arguments in our frame copied to the start of the target frame. */
        private final Range leadingArgs;

        /** The range of arguments in our frame copied to the end of the target frame. */
        private final Range trailingArgs;

        private final int referencesOffset;
        private final int stackFrameOffset;

        FoldArguments(MethodHandle target, int position, MethodHandle combiner) {
            super(deriveType(target, position, combiner));

            this.target = target;
            this.combiner = combiner;
            this.position = position;

            this.combinerArgs =
                    Range.of(type(), position, position + combiner.type().parameterCount());
            this.leadingArgs = Range.of(type(), 0, position);
            this.trailingArgs = Range.from(type(), position);

            final Class<?> combinerRType = combiner.type().rtype();
            if (combinerRType == void.class) {
                stackFrameOffset = 0;
                referencesOffset = 0;
            } else if (combinerRType.isPrimitive()) {
                stackFrameOffset = EmulatedStackFrame.getSize(combinerRType);
                referencesOffset = 0;
            } else {
                // combinerRType is a reference.
                stackFrameOffset = 0;
                referencesOffset = 1;
            }
        }

        @Override
        public void transform(EmulatedStackFrame stackFrame) throws Throwable {
            // First construct the combiner frame and invoke the combiner.
            EmulatedStackFrame combinerFrame = EmulatedStackFrame.create(combiner.type());
            stackFrame.copyRangeTo(combinerFrame, combinerArgs, 0, 0);
            invokeExactFromTransform(combiner, combinerFrame);

            // Create the stack frame for the target and copy leading arguments to it.
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());
            stackFrame.copyRangeTo(targetFrame, leadingArgs, 0, 0);

            // If one of these offsets is not zero, we have to slot the return value from the
            // combiner into the target frame.
            if (referencesOffset != 0 || stackFrameOffset != 0) {
                final StackFrameReader reader = new StackFrameReader();
                reader.attach(combinerFrame).makeReturnValueAccessor();
                final StackFrameWriter writer = new StackFrameWriter();
                writer.attach(targetFrame,
                              position,
                              leadingArgs.numReferences,
                              leadingArgs.numBytes);
                copyNext(reader, writer, target.type().ptypes()[position]);
            }

            // Copy the arguments provided to the combiner to the tail of the target frame.
            stackFrame.copyRangeTo(
                targetFrame,
                trailingArgs,
                leadingArgs.numReferences + referencesOffset,
                leadingArgs.numBytes + stackFrameOffset);

            // Call the target and propagate return value.
            invokeExactFromTransform(target, targetFrame);
            targetFrame.copyReturnValueTo(stackFrame);
        }

        private static MethodType deriveType(MethodHandle target,
                                             int position,
                                             MethodHandle combiner) {
            if (combiner.type().rtype() == void.class) {
                return target.type();
            }
            return target.type().dropParameterTypes(position, position + 1);
        }
    }

    /** Implements {@code MethodHandles.insertArguments}. */
    static class InsertArguments extends Transformer {
        private final MethodHandle target;
        private final int pos;
        private final Object[] values;

        private final Range range1;
        private final Range range2;

        InsertArguments(MethodHandle target, int pos, Object[] values) {
            super(target.type().dropParameterTypes(pos, pos + values.length));
            this.target = target;
            this.pos = pos;
            this.values = values;

            final MethodType type = type();
            range1 = EmulatedStackFrame.Range.of(type, 0, pos);
            range2 = Range.of(type, pos, type.parameterCount());
        }

        @Override
        public void transform(EmulatedStackFrame stackFrame) throws Throwable {
            EmulatedStackFrame calleeFrame = EmulatedStackFrame.create(target.type());

            // Copy all arguments before |pos|.
            stackFrame.copyRangeTo(calleeFrame, range1, 0, 0);

            // Attach a stack frame writer so that we can copy the next |values.length|
            // arguments.
            final StackFrameWriter writer = new StackFrameWriter();
            writer.attach(calleeFrame, pos, range1.numReferences, range1.numBytes);

            // Copy all the arguments supplied in |values|.
            int referencesCopied = 0;
            int bytesCopied = 0;
            final Class<?>[] ptypes = target.type().ptypes();
            for (int i = 0; i < values.length; ++i) {
                final Class<?> ptype = ptypes[i + pos];
                final char typeChar = Wrapper.basicTypeChar(ptype);
                if (typeChar == 'L') {
                    writer.putNextReference(values[i], ptype);
                    referencesCopied++;
                } else {
                    switch (typeChar) {
                        case 'Z':
                            writer.putNextBoolean((boolean) values[i]);
                            break;
                        case 'B':
                            writer.putNextByte((byte) values[i]);
                            break;
                        case 'C':
                            writer.putNextChar((char) values[i]);
                            break;
                        case 'S':
                            writer.putNextShort((short) values[i]);
                            break;
                        case 'I':
                            writer.putNextInt((int) values[i]);
                            break;
                        case 'J':
                            writer.putNextLong((long) values[i]);
                            break;
                        case 'F':
                            writer.putNextFloat((float) values[i]);
                            break;
                        case 'D':
                            writer.putNextDouble((double) values[i]);
                            break;
                    }
                    bytesCopied += EmulatedStackFrame.getSize(ptype);
                }
            }

            // Copy all remaining arguments.
            if (range2 != null) {
                stackFrame.copyRangeTo(
                        calleeFrame,
                        range2,
                        range1.numReferences + referencesCopied,
                        range1.numBytes + bytesCopied);
            }

            invokeFromTransform(target, calleeFrame);
            calleeFrame.copyReturnValueTo(stackFrame);
        }
    }

    /** Implements {@code MethodHandle.asType}. */
    static class AsTypeAdapter extends Transformer {
        private final MethodHandle target;

        AsTypeAdapter(MethodHandle target, MethodType type) {
            super(type);
            this.target = target;
        }

        @Override
        public void transform(EmulatedStackFrame callerFrame) throws Throwable {
            final EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());
            final StackFrameReader reader = new StackFrameReader();
            final StackFrameWriter writer = new StackFrameWriter();

            // Adapt arguments
            reader.attach(callerFrame);
            writer.attach(targetFrame);
            adaptArguments(reader, writer);

            // Invoke target
            invokeFromTransform(target, targetFrame);

            if (callerFrame.getMethodType().rtype() != void.class) {
                // Adapt return value
                reader.attach(targetFrame).makeReturnValueAccessor();
                writer.attach(callerFrame).makeReturnValueAccessor();
                adaptReturnValue(reader, writer);
            }
        }

        private void adaptArguments(final StackFrameReader reader, final StackFrameWriter writer) {
            final Class<?>[] fromTypes = type().ptypes();
            final Class<?>[] toTypes = target.type().ptypes();
            for (int i = 0; i < fromTypes.length; ++i) {
                adaptArgument(reader, fromTypes[i], writer, toTypes[i]);
            }
        }

        private void adaptReturnValue(
                final StackFrameReader reader, final StackFrameWriter writer) {
            final Class<?> fromType = target.type().rtype();
            final Class<?> toType = type().rtype();
            adaptArgument(reader, fromType, writer, toType);
        }

        private void throwWrongMethodTypeException() throws WrongMethodTypeException {
            throw new WrongMethodTypeException(
                    "Cannot convert from " + type() + " to " + target.type());
        }

        private static void throwClassCastException(Class from, Class to)
                throws ClassCastException {
            throw new ClassCastException("Cannot cast from " + from + " to " + to);
        }

        private void writePrimitiveByteAs(final StackFrameWriter writer, char baseType, byte value)
                throws WrongMethodTypeException {
            switch (baseType) {
                case 'B':
                    writer.putNextByte(value);
                    return;
                case 'S':
                    writer.putNextShort((short) value);
                    return;
                case 'I':
                    writer.putNextInt((int) value);
                    return;
                case 'J':
                    writer.putNextLong((long) value);
                    return;
                case 'F':
                    writer.putNextFloat((float) value);
                    return;
                case 'D':
                    writer.putNextDouble((double) value);
                    return;
                default:
                    throwWrongMethodTypeException();
            }
        }

        private void writePrimitiveShortAs(
                final StackFrameWriter writer, char baseType, short value)
                throws WrongMethodTypeException {
            switch (baseType) {
                case 'S':
                    writer.putNextShort(value);
                    return;
                case 'I':
                    writer.putNextInt((int) value);
                    return;
                case 'J':
                    writer.putNextLong((long) value);
                    return;
                case 'F':
                    writer.putNextFloat((float) value);
                    return;
                case 'D':
                    writer.putNextDouble((double) value);
                    return;
                default:
                    throwWrongMethodTypeException();
            }
        }

        private void writePrimitiveCharAs(final StackFrameWriter writer, char baseType, char value)
                throws WrongMethodTypeException {
            switch (baseType) {
                case 'C':
                    writer.putNextChar(value);
                    return;
                case 'I':
                    writer.putNextInt((int) value);
                    return;
                case 'J':
                    writer.putNextLong((long) value);
                    return;
                case 'F':
                    writer.putNextFloat((float) value);
                    return;
                case 'D':
                    writer.putNextDouble((double) value);
                    return;
                default:
                    throwWrongMethodTypeException();
            }
        }

        private void writePrimitiveIntAs(final StackFrameWriter writer, char baseType, int value)
                throws WrongMethodTypeException {
            switch (baseType) {
                case 'I':
                    writer.putNextInt(value);
                    return;
                case 'J':
                    writer.putNextLong((long) value);
                    return;
                case 'F':
                    writer.putNextFloat((float) value);
                    return;
                case 'D':
                    writer.putNextDouble((double) value);
                    return;
                default:
                    throwWrongMethodTypeException();
            }
            throwWrongMethodTypeException();
        }

        private void writePrimitiveLongAs(final StackFrameWriter writer, char baseType, long value)
                throws WrongMethodTypeException {
            switch (baseType) {
                case 'J':
                    writer.putNextLong(value);
                    return;
                case 'F':
                    writer.putNextFloat((float) value);
                    return;
                case 'D':
                    writer.putNextDouble((double) value);
                    return;
                default:
                    throwWrongMethodTypeException();
            }
        }

        private void writePrimitiveFloatAs(
                final StackFrameWriter writer, char baseType, float value)
                throws WrongMethodTypeException {
            switch (baseType) {
                case 'F':
                    writer.putNextFloat(value);
                    return;
                case 'D':
                    writer.putNextDouble((double) value);
                    return;
                default:
                    throwWrongMethodTypeException();
            }
        }

        private void writePrimitiveDoubleAs(
                final StackFrameWriter writer, char baseType, double value)
                throws WrongMethodTypeException {
            switch (baseType) {
                case 'D':
                    writer.putNextDouble(value);
                    return;
                default:
                    throwWrongMethodTypeException();
            }
        }

        private void writePrimitiveVoidAs(final StackFrameWriter writer, char baseType) {
            switch (baseType) {
                case 'Z':
                    writer.putNextBoolean(false);
                    return;
                case 'B':
                    writer.putNextByte((byte) 0);
                    return;
                case 'S':
                    writer.putNextShort((short) 0);
                    return;
                case 'C':
                    writer.putNextChar((char) 0);
                    return;
                case 'I':
                    writer.putNextInt(0);
                    return;
                case 'J':
                    writer.putNextLong(0L);
                    return;
                case 'F':
                    writer.putNextFloat(0.0f);
                    return;
                case 'D':
                    writer.putNextDouble(0.0);
                    return;
                default:
                    throwWrongMethodTypeException();
            }
        }

        private static Class getBoxedPrimitiveClass(char baseType) {
            switch (baseType) {
                case 'Z':
                    return Boolean.class;
                case 'B':
                    return Byte.class;
                case 'S':
                    return Short.class;
                case 'C':
                    return Character.class;
                case 'I':
                    return Integer.class;
                case 'J':
                    return Long.class;
                case 'F':
                    return Float.class;
                case 'D':
                    return Double.class;
                default:
                    return null;
            }
        }

        private void adaptArgument(
                final StackFrameReader reader,
                final Class<?> from,
                final StackFrameWriter writer,
                final Class<?> to) {
            if (from.equals(to)) {
                StackFrameAccessor.copyNext(reader, writer, from);
                return;
            }

            if (to.isPrimitive()) {
                if (from.isPrimitive()) {
                    final char fromBaseType = Wrapper.basicTypeChar(from);
                    final char toBaseType = Wrapper.basicTypeChar(to);
                    switch (fromBaseType) {
                        case 'B':
                            writePrimitiveByteAs(writer, toBaseType, reader.nextByte());
                            return;
                        case 'S':
                            writePrimitiveShortAs(writer, toBaseType, reader.nextShort());
                            return;
                        case 'C':
                            writePrimitiveCharAs(writer, toBaseType, reader.nextChar());
                            return;
                        case 'I':
                            writePrimitiveIntAs(writer, toBaseType, reader.nextInt());
                            return;
                        case 'J':
                            writePrimitiveLongAs(writer, toBaseType, reader.nextLong());
                            return;
                        case 'F':
                            writePrimitiveFloatAs(writer, toBaseType, reader.nextFloat());
                            return;
                        case 'V':
                            writePrimitiveVoidAs(writer, toBaseType);
                            return;
                        default:
                            throwWrongMethodTypeException();
                    }
                } else {
                    final Object value = reader.nextReference(Object.class);
                    if (to == void.class) {
                        return;
                    }
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    if (!Wrapper.isWrapperType(value.getClass())) {
                        throwClassCastException(value.getClass(), to);
                    }
                    final Wrapper fromWrapper = Wrapper.forWrapperType(value.getClass());
                    final Wrapper toWrapper = Wrapper.forPrimitiveType(to);
                    if (!toWrapper.isConvertibleFrom(fromWrapper)) {
                        throwClassCastException(from, to);
                    }

                    final char toChar = toWrapper.basicTypeChar();
                    switch (fromWrapper.basicTypeChar()) {
                        case 'Z':
                            writer.putNextBoolean(((Boolean) value).booleanValue());
                            return;
                        case 'B':
                            writePrimitiveByteAs(writer, toChar, ((Byte) value).byteValue());
                            return;
                        case 'S':
                            writePrimitiveShortAs(writer, toChar, ((Short) value).shortValue());
                            return;
                        case 'C':
                            writePrimitiveCharAs(writer, toChar, ((Character) value).charValue());
                            return;
                        case 'I':
                            writePrimitiveIntAs(writer, toChar, ((Integer) value).intValue());
                            return;
                        case 'J':
                            writePrimitiveLongAs(writer, toChar, ((Long) value).longValue());
                            return;
                        case 'F':
                            writePrimitiveFloatAs(writer, toChar, ((Float) value).floatValue());
                            return;
                        case 'D':
                            writePrimitiveDoubleAs(writer, toChar, ((Double) value).doubleValue());
                            return;
                        default:
                            throw new IllegalStateException();
                    }
                }
            } else {
                if (from.isPrimitive()) {
                    // Boxing conversion
                    final char fromBaseType = Wrapper.basicTypeChar(from);
                    final Class fromBoxed = getBoxedPrimitiveClass(fromBaseType);
                    // 'to' maybe a super class of the boxed `from` type, e.g. Number.
                    if (fromBoxed != null && !to.isAssignableFrom(fromBoxed)) {
                        throwWrongMethodTypeException();
                    }

                    Object boxed;
                    switch (fromBaseType) {
                        case 'Z':
                            boxed = Boolean.valueOf(reader.nextBoolean());
                            break;
                        case 'B':
                            boxed = Byte.valueOf(reader.nextByte());
                            break;
                        case 'S':
                            boxed = Short.valueOf(reader.nextShort());
                            break;
                        case 'C':
                            boxed = Character.valueOf(reader.nextChar());
                            break;
                        case 'I':
                            boxed = Integer.valueOf(reader.nextInt());
                            break;
                        case 'J':
                            boxed = Long.valueOf(reader.nextLong());
                            break;
                        case 'F':
                            boxed = Float.valueOf(reader.nextFloat());
                            break;
                        case 'D':
                            boxed = Double.valueOf(reader.nextDouble());
                            break;
                        case 'V':
                            boxed = null;
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                    writer.putNextReference(boxed, to);
                    return;
                } else {
                    // Cast
                    Object value = reader.nextReference(Object.class);
                    if (value != null && !to.isAssignableFrom(value.getClass())) {
                        throwClassCastException(value.getClass(), to);
                    }
                    writer.putNextReference(value, to);
                }
            }
        }
    }

    /** Implements {@code MethodHandles.explicitCastArguments}. */
    static class ExplicitCastArguments extends Transformer {
        private final MethodHandle target;

        ExplicitCastArguments(MethodHandle target, MethodType type) {
            super(type);
            this.target = target;
        }

        @Override
        public void transform(EmulatedStackFrame callerFrame) throws Throwable {
            // Create a new stack frame for the target.
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());

            explicitCastArguments(callerFrame, targetFrame);
            invokeFromTransform(target, targetFrame);
            explicitCastReturnValue(callerFrame, targetFrame);
        }

        private void explicitCastArguments(
                final EmulatedStackFrame callerFrame, final EmulatedStackFrame targetFrame) {
            final StackFrameReader reader = new StackFrameReader();
            reader.attach(callerFrame);
            final StackFrameWriter writer = new StackFrameWriter();
            writer.attach(targetFrame);

            final Class<?>[] fromTypes = type().ptypes();
            final Class<?>[] toTypes = target.type().ptypes();
            for (int i = 0; i < fromTypes.length; ++i) {
                explicitCast(reader, fromTypes[i], writer, toTypes[i]);
            }
        }

        private void explicitCastReturnValue(
                final EmulatedStackFrame callerFrame, final EmulatedStackFrame targetFrame) {
            Class<?> from = target.type().rtype();
            Class<?> to = type().rtype();
            if (to != void.class) {
                final StackFrameWriter writer = new StackFrameWriter();
                writer.attach(callerFrame);
                writer.makeReturnValueAccessor();
                if (from == void.class) {
                    if (to.isPrimitive()) {
                        unboxNull(writer, to);
                    } else {
                        writer.putNextReference(null, to);
                    }
                } else {
                    final StackFrameReader reader = new StackFrameReader();
                    reader.attach(targetFrame);
                    reader.makeReturnValueAccessor();
                    explicitCast(reader, target.type().rtype(), writer, type().rtype());
                }
            }
        }

        private static void throwUnexpectedType(final Class<?> unexpectedType) {
            throw new InternalError("Unexpected type: " + unexpectedType);
        }

        @SuppressWarnings("unchecked")
        private static void badCast(final Class<?> from, final Class<?> to) {
            throw new ClassCastException("Cannot cast " + from.getName() + " to " + to.getName());
        }

        /**
         * Converts byte value to boolean according to {@link
         * java.lang.invoke.MethodHandles#explicitCast()}
         */
        private static boolean toBoolean(byte value) {
            return (value & 1) == 1;
        }

        private static byte readPrimitiveAsByte(
                final StackFrameReader reader, final Class<?> from) {
            switch (Wrapper.basicTypeChar(from)) {
                case 'B':
                    return (byte) reader.nextByte();
                case 'C':
                    return (byte) reader.nextChar();
                case 'S':
                    return (byte) reader.nextShort();
                case 'I':
                    return (byte) reader.nextInt();
                case 'J':
                    return (byte) reader.nextLong();
                case 'F':
                    return (byte) reader.nextFloat();
                case 'D':
                    return (byte) reader.nextDouble();
                case 'Z':
                    return reader.nextBoolean() ? (byte) 1 : (byte) 0;
                default:
                    throwUnexpectedType(from);
                    return 0;
            }
        }

        private static char readPrimitiveAsChar(
                final StackFrameReader reader, final Class<?> from) {
            switch (Wrapper.basicTypeChar(from)) {
                case 'B':
                    return (char) reader.nextByte();
                case 'C':
                    return (char) reader.nextChar();
                case 'S':
                    return (char) reader.nextShort();
                case 'I':
                    return (char) reader.nextInt();
                case 'J':
                    return (char) reader.nextLong();
                case 'F':
                    return (char) reader.nextFloat();
                case 'D':
                    return (char) reader.nextDouble();
                case 'Z':
                    return reader.nextBoolean() ? (char) 1 : (char) 0;
                default:
                    throwUnexpectedType(from);
                    return 0;
            }
        }

        private static short readPrimitiveAsShort(
                final StackFrameReader reader, final Class<?> from) {
            switch (Wrapper.basicTypeChar(from)) {
                case 'B':
                    return (short) reader.nextByte();
                case 'C':
                    return (short) reader.nextChar();
                case 'S':
                    return (short) reader.nextShort();
                case 'I':
                    return (short) reader.nextInt();
                case 'J':
                    return (short) reader.nextLong();
                case 'F':
                    return (short) reader.nextFloat();
                case 'D':
                    return (short) reader.nextDouble();
                case 'Z':
                    return reader.nextBoolean() ? (short) 1 : (short) 0;
                default:
                    throwUnexpectedType(from);
                    return 0;
            }
        }

        private static int readPrimitiveAsInt(final StackFrameReader reader, final Class<?> from) {
            switch (Wrapper.basicTypeChar(from)) {
                case 'B':
                    return (int) reader.nextByte();
                case 'C':
                    return (int) reader.nextChar();
                case 'S':
                    return (int) reader.nextShort();
                case 'I':
                    return (int) reader.nextInt();
                case 'J':
                    return (int) reader.nextLong();
                case 'F':
                    return (int) reader.nextFloat();
                case 'D':
                    return (int) reader.nextDouble();
                case 'Z':
                    return reader.nextBoolean() ? 1 : 0;
                default:
                    throwUnexpectedType(from);
                    return 0;
            }
        }

        private static long readPrimitiveAsLong(
                final StackFrameReader reader, final Class<?> from) {
            switch (Wrapper.basicTypeChar(from)) {
                case 'B':
                    return (long) reader.nextByte();
                case 'C':
                    return (long) reader.nextChar();
                case 'S':
                    return (long) reader.nextShort();
                case 'I':
                    return (long) reader.nextInt();
                case 'J':
                    return (long) reader.nextLong();
                case 'F':
                    return (long) reader.nextFloat();
                case 'D':
                    return (long) reader.nextDouble();
                case 'Z':
                    return reader.nextBoolean() ? 1L : 0L;
                default:
                    throwUnexpectedType(from);
                    return 0;
            }
        }

        private static float readPrimitiveAsFloat(
                final StackFrameReader reader, final Class<?> from) {
            switch (Wrapper.basicTypeChar(from)) {
                case 'B':
                    return (float) reader.nextByte();
                case 'C':
                    return (float) reader.nextChar();
                case 'S':
                    return (float) reader.nextShort();
                case 'I':
                    return (float) reader.nextInt();
                case 'J':
                    return (float) reader.nextLong();
                case 'F':
                    return (float) reader.nextFloat();
                case 'D':
                    return (float) reader.nextDouble();
                case 'Z':
                    return reader.nextBoolean() ? 1.0f : 0.0f;
                default:
                    throwUnexpectedType(from);
                    return 0;
            }
        }

        private static double readPrimitiveAsDouble(
                final StackFrameReader reader, final Class<?> from) {
            switch (Wrapper.basicTypeChar(from)) {
                case 'B':
                    return (double) reader.nextByte();
                case 'C':
                    return (double) reader.nextChar();
                case 'S':
                    return (double) reader.nextShort();
                case 'I':
                    return (double) reader.nextInt();
                case 'J':
                    return (double) reader.nextLong();
                case 'F':
                    return (double) reader.nextFloat();
                case 'D':
                    return (double) reader.nextDouble();
                case 'Z':
                    return reader.nextBoolean() ? 1.0 : 0.0;
                default:
                    throwUnexpectedType(from);
                    return 0;
            }
        }

        private static void explicitCastPrimitives(
                final StackFrameReader reader,
                final Class<?> from,
                final StackFrameWriter writer,
                final Class<?> to) {
            switch (Wrapper.basicTypeChar(to)) {
                case 'B':
                    writer.putNextByte(readPrimitiveAsByte(reader, from));
                    break;
                case 'C':
                    writer.putNextChar(readPrimitiveAsChar(reader, from));
                    break;
                case 'S':
                    writer.putNextShort(readPrimitiveAsShort(reader, from));
                    break;
                case 'I':
                    writer.putNextInt(readPrimitiveAsInt(reader, from));
                    break;
                case 'J':
                    writer.putNextLong(readPrimitiveAsLong(reader, from));
                    break;
                case 'F':
                    writer.putNextFloat(readPrimitiveAsFloat(reader, from));
                    break;
                case 'D':
                    writer.putNextDouble(readPrimitiveAsDouble(reader, from));
                    break;
                case 'Z':
                    writer.putNextBoolean(toBoolean(readPrimitiveAsByte(reader, from)));
                    break;
                default:
                    throwUnexpectedType(to);
                    break;
            }
        }

        private static void unboxNull(final StackFrameWriter writer, final Class<?> to) {
            switch (Wrapper.basicTypeChar(to)) {
                case 'Z':
                    writer.putNextBoolean(false);
                    break;
                case 'B':
                    writer.putNextByte((byte) 0);
                    break;
                case 'C':
                    writer.putNextChar((char) 0);
                    break;
                case 'S':
                    writer.putNextShort((short) 0);
                    break;
                case 'I':
                    writer.putNextInt((int) 0);
                    break;
                case 'J':
                    writer.putNextLong((long) 0);
                    break;
                case 'F':
                    writer.putNextFloat((float) 0);
                    break;
                case 'D':
                    writer.putNextDouble((double) 0);
                    break;
                default:
                    throwUnexpectedType(to);
                    break;
            }
        }

        private static void unboxNonNull(
                final Object ref,
                final StackFrameWriter writer,
                final Class<?> to) {
            final Class<?> from = ref.getClass();
            final Class<?> unboxedFromType = Wrapper.asPrimitiveType(from);
            switch (Wrapper.basicTypeChar(unboxedFromType)) {
                case 'Z':
                    boolean z = (boolean) ref;
                    switch (Wrapper.basicTypeChar(to)) {
                        case 'Z':
                            writer.putNextBoolean(z);
                            break;
                        case 'B':
                            writer.putNextByte(z ? (byte) 1 : (byte) 0);
                            break;
                        case 'S':
                            writer.putNextShort(z ? (short) 1 : (short) 0);
                            break;
                        case 'C':
                            writer.putNextChar(z ? (char) 1 : (char) 0);
                            break;
                        case 'I':
                            writer.putNextInt(z ? 1 : 0);
                            break;
                        case 'J':
                            writer.putNextLong(z ? 1l : 0l);
                            break;
                        case 'F':
                            writer.putNextFloat(z ? 1.0f : 0.0f);
                            break;
                        case 'D':
                            writer.putNextDouble(z ? 1.0 : 0.0);
                            break;
                        default:
                            badCast(from, to);
                            break;
                    }
                    break;
                case 'B':
                    byte b = (byte) ref;
                    switch (Wrapper.basicTypeChar(to)) {
                        case 'B':
                            writer.putNextByte(b);
                            break;
                        case 'Z':
                            writer.putNextBoolean(toBoolean(b));
                            break;
                        case 'S':
                            writer.putNextShort((short) b);
                            break;
                        case 'C':
                            writer.putNextChar((char) b);
                            break;
                        case 'I':
                            writer.putNextInt((int) b);
                            break;
                        case 'J':
                            writer.putNextLong((long) b);
                            break;
                        case 'F':
                            writer.putNextFloat((float) b);
                            break;
                        case 'D':
                            writer.putNextDouble((double) b);
                            break;
                        default:
                            badCast(from, to);
                            break;
                    }
                    break;
                case 'S':
                    short s = (short) ref;
                    switch (Wrapper.basicTypeChar(to)) {
                        case 'Z':
                            writer.putNextBoolean((s & 1) == 1);
                            break;
                        case 'B':
                            writer.putNextByte((byte) s);
                            break;
                        case 'S':
                            writer.putNextShort(s);
                            break;
                        case 'C':
                            writer.putNextChar((char) s);
                            break;
                        case 'I':
                            writer.putNextInt((int) s);
                            break;
                        case 'J':
                            writer.putNextLong((long) s);
                            break;
                        case 'F':
                            writer.putNextFloat((float) s);
                            break;
                        case 'D':
                            writer.putNextDouble((double) s);
                            break;
                        default:
                            badCast(from, to);
                            break;
                    }
                    break;
                case 'C':
                    char c = (char) ref;
                    switch (Wrapper.basicTypeChar(to)) {
                        case 'Z':
                            writer.putNextBoolean((c & (char) 1) == (char) 1);
                            break;
                        case 'B':
                            writer.putNextByte((byte) c);
                            break;
                        case 'S':
                            writer.putNextShort((short) c);
                            break;
                        case 'C':
                            writer.putNextChar(c);
                            break;
                        case 'I':
                            writer.putNextInt((int) c);
                            break;
                        case 'J':
                            writer.putNextLong((long) c);
                            break;
                        case 'F':
                            writer.putNextFloat((float) c);
                            break;
                        case 'D':
                            writer.putNextDouble((double) c);
                            break;
                        default:
                            badCast(from, to);
                            break;
                    }
                    break;
                case 'I':
                    int i = (int) ref;
                    switch (Wrapper.basicTypeChar(to)) {
                        case 'Z':
                            writer.putNextBoolean((i & 1) == 1);
                            break;
                        case 'B':
                            writer.putNextByte((byte) i);
                            break;
                        case 'S':
                            writer.putNextShort((short) i);
                            break;
                        case 'C':
                            writer.putNextChar((char) i);
                            break;
                        case 'I':
                            writer.putNextInt(i);
                            break;
                        case 'J':
                            writer.putNextLong((long) i);
                            break;
                        case 'F':
                            writer.putNextFloat((float) i);
                            break;
                        case 'D':
                            writer.putNextDouble((double) i);
                            break;
                        default:
                            badCast(from, to);
                    }
                    break;
                case 'J':
                    long j = (long) ref;
                    switch (Wrapper.basicTypeChar(to)) {
                        case 'Z':
                            writer.putNextBoolean((j & 1l) == 1l);
                            break;
                        case 'B':
                            writer.putNextByte((byte) j);
                            break;
                        case 'S':
                            writer.putNextShort((short) j);
                            break;
                        case 'C':
                            writer.putNextChar((char) j);
                            break;
                        case 'I':
                            writer.putNextInt((int) j);
                            break;
                        case 'J':
                            writer.putNextLong(j);
                            break;
                        case 'F':
                            writer.putNextFloat((float) j);
                            break;
                        case 'D':
                            writer.putNextDouble((double) j);
                            break;
                        default:
                            badCast(from, to);
                            break;
                    }
                    break;
                case 'F':
                    float f = (float) ref;
                    switch (Wrapper.basicTypeChar(to)) {
                        case 'Z':
                            writer.putNextBoolean(((byte) f & 1) != 0);
                            break;
                        case 'B':
                            writer.putNextByte((byte) f);
                            break;
                        case 'S':
                            writer.putNextShort((short) f);
                            break;
                        case 'C':
                            writer.putNextChar((char) f);
                            break;
                        case 'I':
                            writer.putNextInt((int) f);
                            break;
                        case 'J':
                            writer.putNextLong((long) f);
                            break;
                        case 'F':
                            writer.putNextFloat(f);
                            break;
                        case 'D':
                            writer.putNextDouble((double) f);
                            break;
                        default:
                            badCast(from, to);
                            break;
                    }
                    break;
                case 'D':
                    double d = (double) ref;
                    switch (Wrapper.basicTypeChar(to)) {
                        case 'Z':
                            writer.putNextBoolean(((byte) d & 1) != 0);
                            break;
                        case 'B':
                            writer.putNextByte((byte) d);
                            break;
                        case 'S':
                            writer.putNextShort((short) d);
                            break;
                        case 'C':
                            writer.putNextChar((char) d);
                            break;
                        case 'I':
                            writer.putNextInt((int) d);
                            break;
                        case 'J':
                            writer.putNextLong((long) d);
                            break;
                        case 'F':
                            writer.putNextFloat((float) d);
                            break;
                        case 'D':
                            writer.putNextDouble(d);
                            break;
                        default:
                            badCast(from, to);
                            break;
                    }
                    break;
                default:
                    badCast(from, to);
                    break;
            }
        }

        private static void unbox(
                final Object ref,
                final StackFrameWriter writer,
                final Class<?> to) {
            if (ref == null) {
                unboxNull(writer, to);
            } else {
                unboxNonNull(ref, writer, to);
            }
        }

        private static void box(
                final StackFrameReader reader,
                final Class<?> from,
                final StackFrameWriter writer,
                final Class<?> to) {
            Object boxed = null;
            switch (Wrapper.basicTypeChar(from)) {
                case 'Z':
                    boxed = Boolean.valueOf(reader.nextBoolean());
                    break;
                case 'B':
                    boxed = Byte.valueOf(reader.nextByte());
                    break;
                case 'C':
                    boxed = Character.valueOf(reader.nextChar());
                    break;
                case 'S':
                    boxed = Short.valueOf(reader.nextShort());
                    break;
                case 'I':
                    boxed = Integer.valueOf(reader.nextInt());
                    break;
                case 'J':
                    boxed = Long.valueOf(reader.nextLong());
                    break;
                case 'F':
                    boxed = Float.valueOf(reader.nextFloat());
                    break;
                case 'D':
                    boxed = Double.valueOf(reader.nextDouble());
                    break;
                default:
                    throwUnexpectedType(from);
                    break;
            }
            writer.putNextReference(to.cast(boxed), to);
        }

        private static void explicitCast(
                final StackFrameReader reader,
                final Class<?> from,
                final StackFrameWriter writer,
                final Class<?> to) {
            if (from.equals(to)) {
                StackFrameAccessor.copyNext(reader, writer, from);
                return;
            }

            if (from.isPrimitive()) {
                if (to.isPrimitive()) {
                    // |from| and |to| are primitive types.
                    explicitCastPrimitives(reader, from, writer, to);
                } else {
                    // |from| is a primitive type, |to| is a reference type.
                    box(reader, from, writer, to);
                }
            } else {
                // |from| is a reference type.
                Object ref = reader.nextReference(from);
                if (to.isPrimitive()) {
                    // |from| is a reference type, |to| is a primitive type,
                    unbox(ref, writer, to);
                } else if (to.isInterface()) {
                    // Pass from without a cast according to description for
                    // {@link java.lang.invoke.MethodHandles#explicitCastArguments()}.
                    writer.putNextReference(ref, to);
                } else {
                    // |to| and from |from| are reference types, perform class cast check.
                    writer.putNextReference(to.cast(ref), to);
                }
            }
        }
    }

    /** Implements {@code MethodHandles.loop}. */
    static class Loop extends Transformer {

        /** Loop variable initialization methods. */
        final MethodHandle[] inits;

        /** Loop variable step methods. */
        final MethodHandle[] steps;

        /** Loop variable predicate methods. */
        final MethodHandle[] preds;

        /** Loop return value calculating methods. */
        final MethodHandle[] finis;

        /** Synthetic method type for frame used to hold loop variables. */
        final MethodType loopVarsType;

        /** Range of loop variables in the frame used for loop variables. */
        final Range loopVarsRange;

        /** Range of suffix variables in the caller frame. */
        final Range suffixRange;

        public Loop(Class<?> loopReturnType,
                    List<Class<?>> commonSuffix,
                    MethodHandle[] finit,
                    MethodHandle[] fstep,
                    MethodHandle[] fpred,
                    MethodHandle[] ffini) {
            super(MethodType.methodType(loopReturnType, commonSuffix));

            inits = finit;
            steps = fstep;
            preds = fpred;
            finis = ffini;

            loopVarsType = deduceLoopVarsType(finit);
            loopVarsRange = EmulatedStackFrame.Range.all(loopVarsType);
            suffixRange = EmulatedStackFrame.Range.all(type());
        }

        @Override
        public void transform(EmulatedStackFrame callerFrame) throws Throwable {
            final EmulatedStackFrame loopVarsFrame = EmulatedStackFrame.create(loopVarsType);
            final StackFrameWriter loopVarsWriter = new StackFrameWriter();

            init(callerFrame, loopVarsFrame, loopVarsWriter);

            for (;;) {
                loopVarsWriter.attach(loopVarsFrame);
                for (int i = 0; i < steps.length; ++i) {
                    // Future optimization opportunity: there is a good deal of StackFrame
                    // allocation here, one is allocated per MH invocation. Consider caching
                    // frames <method-type:stack-frame> and passing the cache on the stack.
                    doStep(steps[i], callerFrame, loopVarsFrame, loopVarsWriter);
                    boolean keepGoing = doPredicate(preds[i], callerFrame, loopVarsFrame);
                    if (!keepGoing) {
                        doFinish(finis[i], callerFrame, loopVarsFrame);
                        return;
                    }
                }
            }
        }

        private static MethodType deduceLoopVarsType(final MethodHandle[] inits) {
            List<Class<?>> loopVarTypes = new ArrayList(inits.length);
            for (MethodHandle init : inits) {
                Class<?> returnType = init.type().returnType();
                if (returnType != void.class) {
                    loopVarTypes.add(returnType);
                }
            }
            return MethodType.methodType(void.class, loopVarTypes);
        }

        private void init(final EmulatedStackFrame callerFrame,
                          final EmulatedStackFrame loopVarsFrame,
                          final StackFrameWriter loopVarsWriter) throws Throwable {
            loopVarsWriter.attach(loopVarsFrame);
            for (MethodHandle init : inits) {
                EmulatedStackFrame initFrame = EmulatedStackFrame.create(init.type());
                callerFrame.copyRangeTo(initFrame, suffixRange, 0, 0);

                invokeExactFromTransform(init, initFrame);

                final Class<?> loopVarType = init.type().returnType();
                if (loopVarType != void.class) {
                    StackFrameReader initReader = new StackFrameReader();
                    initReader.attach(initFrame).makeReturnValueAccessor();
                    copyNext(initReader, loopVarsWriter, loopVarType);
                }
            }
        }

        /**
         * Creates a frame for invoking a method of specified type.
         *
         * The frame arguments are the loop variables followed by the arguments provided to the
         * loop MethodHandle.
         *
         * @param mt the type of the method to be invoked.
         * @param callerFrame the frame invoking the loop MethodHandle.
         * @param loopVarsFrame the frame holding loop variables.
         * @return an EmulatedStackFrame initialized with the required arguments.
         */
        private EmulatedStackFrame prepareFrame(final MethodType mt,
                                                final EmulatedStackFrame callerFrame,
                                                final EmulatedStackFrame loopVarsFrame) {
            EmulatedStackFrame frame = EmulatedStackFrame.create(mt);

            // Copy loop variables.
            loopVarsFrame.copyRangeTo(frame, loopVarsRange, 0, 0);

            // Copy arguments provided in the loop invoke().
            callerFrame.copyRangeTo(frame,
                                    suffixRange,
                                    loopVarsRange.numReferences,
                                    loopVarsRange.numBytes);
            return frame;
        }

        private void doStep(final MethodHandle step,
                            final EmulatedStackFrame callerFrame,
                            final EmulatedStackFrame loopVarsFrame,
                            final StackFrameWriter loopVarsWriter) throws Throwable {
            final EmulatedStackFrame stepFrame =
                prepareFrame(step.type(), callerFrame, loopVarsFrame);
            invokeExactFromTransform(step, stepFrame);

            final Class<?> loopVarType = step.type().returnType();
            if (loopVarType != void.class) {
                final StackFrameReader stepReader = new StackFrameReader();
                stepReader.attach(stepFrame).makeReturnValueAccessor();
                copyNext(stepReader, loopVarsWriter, loopVarType);
            }
        }

        private boolean doPredicate(final MethodHandle pred,
                                    final EmulatedStackFrame callerFrame,
                                    final EmulatedStackFrame loopVarsFrame) throws Throwable {
            final EmulatedStackFrame predFrame =
                    prepareFrame(pred.type(), callerFrame, loopVarsFrame);
            invokeExactFromTransform(pred, predFrame);

            final StackFrameReader predReader = new StackFrameReader();
            predReader.attach(predFrame).makeReturnValueAccessor();
            return predReader.nextBoolean();
        }

        private void doFinish(final MethodHandle fini,
                              final EmulatedStackFrame callerFrame,
                              final EmulatedStackFrame loopVarsFrame) throws Throwable {
            final EmulatedStackFrame finiFrame =
                    prepareFrame(fini.type(), callerFrame, loopVarsFrame);
            invokeExactFromTransform(fini, finiFrame);
            finiFrame.copyReturnValueTo(callerFrame);
        }
    }
}
