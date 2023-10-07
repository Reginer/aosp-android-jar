package com.android.networkstack.tethering.companionproxy.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.List;

public class ProtoTestHelper {
    private static final int MAX_SIZE = 1024;

    public static void testSerialization(Object obj, Class protoClazz) throws Exception {
        // Save object to stream, and then parse into MessageLite-based proto.
        MessageLite proto = serializeObjectAndParseAsProto(obj, protoClazz);

        // Make sure "unexpected" field wasn't read from the serialized data.
        assertFalse(
            (Boolean) protoClazz.getDeclaredMethod("hasUnexpectedData").invoke(proto));

        // Set "unexpected" field in the proto message.
        Object protoBuilder = protoClazz.getMethod("toBuilder").invoke(proto);
        protoBuilder.getClass().getDeclaredMethod(
            "setUnexpectedData", int.class).invoke(protoBuilder, Integer.valueOf(1000));
        proto = (MessageLite) protoBuilder.getClass().getMethod(
            "build").invoke(protoBuilder);

        // Reconstruct original type from a MessageLite-based proto.
        Object obj2 = serializeProtoAndParseAsObj(proto, obj.getClass());

        // Make a copy of the original object.
        Object obj3 = callCopyConstructor(obj);

        assertEqualObjects(obj, obj2, obj3, proto);
    }

    private static void assertEqualObjects(Object obj, Object obj2, Object obj3, MessageLite proto)
            throws Exception {
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if ((field.getModifiers() & Modifier.STATIC) != 0) {
                continue;
            }

            String name = field.getName();
            String getterName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);

            field.setAccessible(true);
            Object objFieldValue = field.get(obj);
            Object obj2FieldValue = field.get(obj2);
            Object obj3FieldValue = field.get(obj3);

            if (objFieldValue instanceof List) {
                List objFieldAsList = (List) objFieldValue;
                List obj2FieldAsList = (List) obj2FieldValue;
                List obj3FieldAsList = (List) obj3FieldValue;
                List protoFieldAsList =
                    (List) proto.getClass().getDeclaredMethod(getterName + "List").invoke(proto);

                int listSize = objFieldAsList.size();
                assertEquals(listSize, obj2FieldAsList.size());
                assertEquals(listSize, obj3FieldAsList.size());
                assertEquals(listSize, protoFieldAsList.size());

                for (int i = 0; i < listSize; i++) {
                    assertEqualObjects(objFieldAsList.get(i), obj2FieldAsList.get(i),
                        obj3FieldAsList.get(i), (MessageLite) protoFieldAsList.get(i));
                }

                continue;
            }

            Object protoFieldValue = proto.getClass().getDeclaredMethod(getterName).invoke(proto);

            assertEquals(objFieldValue, obj2FieldValue);
            assertEquals(objFieldValue, obj3FieldValue);
            assertEquals(objFieldValue, protoFieldValue);
        }
    }

    private static MessageLite serializeObjectAndParseAsProto(
            Object obj, Class protoClazz) throws Exception {
        byte[] data = new byte[MAX_SIZE];
        CodedOutputStream out = CodedOutputStream.newInstance(data, 0, data.length);

        Method serializeToMethod = obj.getClass().getDeclaredMethod(
            "serializeTo", CodedOutputStream.class);
        serializeToMethod.setAccessible(true);
        serializeToMethod.invoke(obj, out);

        return (MessageLite) protoClazz.getDeclaredMethod(
            "parseFrom", ByteBuffer.class).invoke(
                null, ByteBuffer.wrap(data, 0, out.getTotalBytesWritten()));
    }

    private static Object serializeProtoAndParseAsObj(
            MessageLite proto, Class objClazz) throws Exception {
        byte[] data = new byte[MAX_SIZE];
        CodedOutputStream out = CodedOutputStream.newInstance(data, 0, data.length);

        proto.writeTo(out);

        Method parseFromMethod = objClazz.getDeclaredMethod("parseFrom", CodedInputStream.class);
        parseFromMethod.setAccessible(true);

        CodedInputStream input = CodedInputStream.newInstance(
            data, 0, out.getTotalBytesWritten());
        Object obj = parseFromMethod.invoke(null, input);

        assertTrue(input.isAtEnd());

        return obj;
    }

    private static Object callCopyConstructor(Object obj) throws Exception {
        Constructor constructor = obj.getClass().getDeclaredConstructor(obj.getClass());
        constructor.setAccessible(true);
        return constructor.newInstance(obj);
    }

    private ProtoTestHelper() {}
}
