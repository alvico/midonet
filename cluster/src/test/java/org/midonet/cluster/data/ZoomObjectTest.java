/*
 * Copyright (c) 2014 Midokura SARL, All Rights Reserved.
 */
package org.midonet.cluster.data;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Random;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageOrBuilder;

import org.junit.Test;

import org.midonet.cluster.models.TestModels;
import org.midonet.cluster.util.UUIDUtil;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.midonet.cluster.models.TestModels.FakeDevice;
import static org.midonet.cluster.models.TestModels.TestMessage;

public class ZoomObjectTest {

    @Test
    public void testConversionWithInstanceMethods() {
        TestMessage message = buildMessage();

        TestableZoomObject pojo = new TestableZoomObject(message);

        assertPojo(message, pojo);

        TestMessage proto = pojo.toProto(TestMessage.class);

        assertProto(message, proto);
    }

    @Test
    public void testConversionWithStaticMethods() {
        TestMessage message = buildMessage();

        TestableZoomObject pojo =
            ZoomConvert.fromProto(message, TestableZoomObject.class);

        assertPojo(message, pojo);

        TestMessage proto = pojo.toProto(TestMessage.class);

        assertProto(message, proto);
    }

    @Test
    public void testConversionToBuilder() {
        // Build the prototype message and convert to POJO.
        TestMessage proto = buildMessage();
        TestableZoomObject pojo = new TestableZoomObject(proto);

        // Convert pojo back to proto builder, and use builder to alter id.
        java.util.UUID uuid2 = java.util.UUID.randomUUID();
        TestMessage.Builder builder =
            (TestMessage.Builder)pojo.toProtoBuilder(TestMessage.class);
        builder.setUuidField(UUIDUtil.toProto(uuid2));
        TestMessage message = builder.build();

        // Check that message is equal to proto other than the id change.
        assertNotEquals(message, proto);
        assertProto(message,
                    TestMessage.newBuilder(proto)
                        .setUuidField(UUIDUtil.toProto(uuid2)).build());
    }

    static TestMessage buildMessage() {
        byte bytes[] = new byte[16];
        Random random = new Random();
        random.nextBytes(bytes);

        return TestModels.TestMessage.newBuilder()
            .setDoubleField(random.nextDouble())
            .setFloatField(random.nextFloat())
            .setInt32Field(random.nextInt())
            .setInt64Field(random.nextLong())
            .setUint32Field(random.nextInt())
            .setUint64Field(random.nextLong())
            .setSint32Field(random.nextInt())
            .setSint64Field(random.nextLong())
            .setFixed32Field(random.nextInt())
            .setFixed64Field(random.nextLong())
            .setSfixed32Field(random.nextInt())
            .setSfixed64Field(random.nextLong())
            .setBoolField(random.nextBoolean())
            .setStringField(java.util.UUID.randomUUID().toString())
            .setByteField((byte) (random.nextInt() & 0x7F))
            .setShortField((short) (random.nextInt() & 0x7FFF))
            .setByteStringField(ByteString.copyFrom(bytes))
            .setByteArrayField(ByteString.copyFrom(bytes))
            .setBaseField(random.nextInt())
            .addInt32PrimitiveArray(random.nextInt())
            .addInt32PrimitiveArray(random.nextInt())
            .addInt32InstanceArray(random.nextInt())
            .addInt32InstanceArray(random.nextInt())
            .addInt32List(random.nextInt())
            .addInt32List(random.nextInt())
            .addStringList(java.util.UUID.randomUUID().toString())
            .addStringList(java.util.UUID.randomUUID().toString())
            .addBoolList(random.nextBoolean())
            .addBoolList(random.nextBoolean())
            .setUuidField(UUIDUtil.toProto(java.util.UUID.randomUUID()))
            .addUuidArray(UUIDUtil.toProto(java.util.UUID.randomUUID()))
            .addUuidArray(UUIDUtil.toProto(java.util.UUID.randomUUID()))
            .addUuidList(UUIDUtil.toProto(java.util.UUID.randomUUID()))
            .addUuidList(UUIDUtil.toProto(java.util.UUID.randomUUID()))
            .setDeviceField(buildDevice())
            .addDeviceArray(buildDevice())
            .addDeviceArray(buildDevice())
            .addDeviceList(buildDevice())
            .addDeviceList(buildDevice())
            .setEnumField(TestMessage.Enum.SECOND)
            .build();
    }

    static FakeDevice buildDevice() {
        return FakeDevice.newBuilder()
            .setId(UUIDUtil.randomUuidProto())
            .setName(UUIDUtil.randomUuidProto().toString())
            .addPortIds(UUIDUtil.randomUuidProto().toString())
            .addPortIds(UUIDUtil.randomUuidProto().toString())
            .build();
    }

    static void assertPojo(TestMessage message, TestableZoomObject pojo) {
        assertEquals(pojo.doubleField, message.getDoubleField(), 0.1);
        assertEquals(pojo.floatField, message.getFloatField(), 0.1);
        assertEquals(pojo.int32Field, message.getInt32Field());
        assertEquals(pojo.int64Field, message.getInt64Field());
        assertEquals(pojo.uint32Field, message.getUint32Field());
        assertEquals(pojo.uint64Field, message.getUint64Field());
        assertEquals(pojo.sint32Field, message.getSint32Field());
        assertEquals(pojo.sint64Field, message.getSint64Field());
        assertEquals(pojo.fixed32Field, message.getFixed32Field());
        assertEquals(pojo.fixed64Field, message.getFixed64Field());
        assertEquals(pojo.sfixed32Field, message.getSfixed32Field());
        assertEquals(pojo.sfixed64Field, message.getSfixed64Field());
        assertEquals(pojo.boolField, message.getBoolField());
        assertEquals(pojo.stringField, message.getStringField());
        assertEquals(pojo.byteField, message.getByteField());
        assertEquals(pojo.shortField, message.getShortField());
        assertEquals(pojo.byteStringField, message.getByteStringField());
        assertEquals(pojo.baseField, message.getBaseField());
        assertArrayEquals(pojo.byteArrayField,
                          message.getByteArrayField().toByteArray());

        assertArray(pojo.int32PrimitiveArray,
                    message.getInt32PrimitiveArrayList().toArray());
        assertArray(pojo.int32InstanceArray,
                    message.getInt32InstanceArrayList().toArray());
        assertEquals(pojo.int32List, message.getInt32ListList());
        assertEquals(pojo.stringList, message.getStringListList());
        assertEquals(pojo.boolList, message.getBoolListList());

        assertEquals(pojo.uuidField, UUIDUtil.fromProto(message.getUuidField()));
        for (int index = 0; index < message.getUuidArrayCount(); index++) {
            assertEquals(UUIDUtil.toProto(pojo.uuidArray[index]),
                         message.getUuidArray(index));
        }
        for (int index = 0; index < message.getUuidListCount(); index++) {
            assertEquals(UUIDUtil.toProto(pojo.uuidList.get(index)),
                         message.getUuidList(index));
        }

        assertDevice(pojo.deviceField, message.getDeviceField());
        for (int index = 0; index < message.getDeviceArrayCount(); index++) {
            assertDevice(pojo.deviceArray[index],
                         message.getDeviceArray(index));
        }
        for (int index = 0; index < message.getDeviceListCount(); index++) {
            assertDevice(pojo.deviceList.get(index),
                         message.getDeviceList(index));
        }
        assertEquals(pojo.enumField, TestableZoomObject.Enum.SECOND);
    }

    static void assertProto(TestMessage message, TestMessage proto) {
        assertEquals(proto, message);
    }

    static void assertArray(Object left, Object right) {
        assertEquals(Array.getLength(left), Array.getLength(right));
        for (int index = 0; index < Array.getLength(left); index++) {
            assertEquals(Array.get(left, index), Array.get(right, index));
        }
    }

    static void assertDevice(Device pojo, FakeDevice proto) {
        assertEquals(UUIDUtil.toProto(pojo.id), proto.getId());
        assertEquals(pojo.name, proto.getName());
        for (int index = 0; index < proto.getPortIdsCount(); index++) {
            assertEquals(pojo.portIds.get(index), proto.getPortIds(index));
        }
    }

    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    @ZoomClass(clazz = TestModels.FakeDevice.class)
    static class Device extends ZoomObject {
        @ZoomField(name = "id", converter = UUIDUtil.Converter.class)
        private java.util.UUID id;
        @ZoomField(name = "name")
        private String name;
        @ZoomField(name = "port_ids")
        private List<String> portIds;

        public Device() { }

        public Device(FakeDevice device) {
            super(device);
        }
    }

    static abstract class BaseZoomObject extends ZoomObject {
        @ZoomField(name = "base_field")
        protected int baseField;

        public BaseZoomObject() { }

        public BaseZoomObject(MessageOrBuilder proto) {
            super(proto);
        }
    }

    @SuppressWarnings({"unused", "MismatchedReadAndWriteOfArray",
                       "MismatchedQueryAndUpdateOfCollection"})
    static class TestableZoomObject extends BaseZoomObject {

        @ZoomEnum(clazz = TestMessage.Enum.class)
        enum Enum {
            @ZoomEnumValue(value = "FIRST")
            FIRST,
            @ZoomEnumValue(value = "SECOND")
            SECOND,
            @ZoomEnumValue(value = "THIRD")
            THIRD
        }

        @ZoomField(name = "double_field")
        private double doubleField;
        @ZoomField(name = "float_field")
        private float floatField;
        @ZoomField(name = "int32_field")
        private int int32Field;
        @ZoomField(name = "int64_field")
        private long int64Field;
        @ZoomField(name = "uint32_field")
        private int uint32Field;
        @ZoomField(name = "uint64_field")
        private long uint64Field;
        @ZoomField(name = "sint32_field")
        private int sint32Field;
        @ZoomField(name = "sint64_field")
        private long sint64Field;
        @ZoomField(name = "fixed32_field")
        private int fixed32Field;
        @ZoomField(name = "fixed64_field")
        private long fixed64Field;
        @ZoomField(name = "sfixed32_field")
        private int sfixed32Field;
        @ZoomField(name = "sfixed64_field")
        private long sfixed64Field;
        @ZoomField(name = "bool_field")
        private boolean boolField;
        @ZoomField(name = "string_field")
        private String stringField;
        @ZoomField(name = "byte_field")
        private byte byteField;
        @ZoomField(name = "short_field")
        private short shortField;
        @ZoomField(name = "byte_string_field")
        private ByteString byteStringField;
        @ZoomField(name = "byte_array_field")
        private byte[] byteArrayField;

        @ZoomField(name = "int32_primitive_array")
        private int[] int32PrimitiveArray;
        @ZoomField(name = "int32_instance_array")
        private Integer[] int32InstanceArray;

        @ZoomField(name = "int32_list")
        private List<Integer> int32List;
        @ZoomField(name = "string_list")
        private List<String> stringList;
        @ZoomField(name = "bool_list")
        private List<Boolean> boolList;

        @ZoomField(name = "uuid_field", converter = UUIDUtil.Converter.class)
        private java.util.UUID uuidField;
        @ZoomField(name = "uuid_array", converter = UUIDUtil.Converter.class)
        private java.util.UUID[] uuidArray;
        @ZoomField(name = "uuid_list", converter = UUIDUtil.Converter.class)
        private List<java.util.UUID> uuidList;

        @ZoomField(name = "device_field")
        private Device deviceField;
        @ZoomField(name = "device_array")
        private Device[] deviceArray;
        @ZoomField(name = "device_list")
        private List<Device> deviceList;

        @ZoomField(name = "enum_field")
        private Enum enumField;

        public TestableZoomObject() { }

        public TestableZoomObject(TestMessage proto) {
            super(proto);
        }
    }
}