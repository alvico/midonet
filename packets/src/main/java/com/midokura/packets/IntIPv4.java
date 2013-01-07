/*
 * Copyright 2011 Midokura KK
 */
package com.midokura.packets;


public class IntIPv4 implements Cloneable {
    private int address;
    private int maskLength;

    public int addressAsInt() {
        return address;
    }

    public int prefixLen() {
        return maskLength;
    }

    public boolean unicastEquals(IntIPv4 other) {
        return address == other.address;
    }

    public boolean subnetContains(int otherAddr) {
        // Zero out any address bits beyond the mask.
        int mask;
        // In Java, a shift by 32 is a no-op, so special case /0
        if (maskLength == 0)
            mask = 0;
        else
            mask = -1 << (32 - maskLength);
        return (address & mask) == (otherAddr & mask);
    }

    /* Default constructor for deserialization. */
    public IntIPv4() {
    }

    public IntIPv4(int addr) {
        this(addr, 32);
    }

    public IntIPv4(int address, int maskLength) {
        if (maskLength < 0 || maskLength > 32)
            maskLength = 32;
        this.address = address;
        this.maskLength = maskLength;
    }

    public IntIPv4(byte[] addr) {
        this(IPv4.toIPv4Address(addr), 32);
    }

    public IntIPv4 clone() { return new IntIPv4(address, maskLength); }

    public int getAddress() {
        return address;
    }

    public IntIPv4 toNetworkAddress() {
        // Zero out any address bits beyond the mask.
        int mask;
        // In Java, a shift by 32 is a no-op, so special case /0
        if (maskLength == 0)
            mask = 0;
        else
            mask = -1 << (32 - maskLength);
        return new IntIPv4(address & mask, maskLength);
    }

    public IntIPv4 toBroadcastAddress() {
        int mask;
        if (maskLength == 0)
            mask = 0;
        else
            mask = -1 << (32 - maskLength);
        return new IntIPv4(address | ~(mask), maskLength);
    }

    public IntIPv4 toHostAddress() {
        if (maskLength != 32) {
            return new IntIPv4(address, 32);
        }
        return this;
    }

    public int getMaskLength() {
        return maskLength;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public IntIPv4 setMaskLength(int maskLength) {
        this.maskLength = maskLength;
        return this;
    }

    /**
     * Convert a String into an IntIPv4 object.
     *
     * @param dottedQuad
     *      The String ip address or prefix to convert. This should be in one
     *      of the following formats: "192.168.0.0_24" for prefixes or
     *      "192.168.0.5" for a unicast address. Note that "192.168.0.5_32" is
     *      valid, but the resulting IntIPv4.toString will yield "192.168.0.5".
     * @return
     *      The IntIPv4 represented by the String.
     */
    public static IntIPv4 fromString(String dottedQuad) {
        String[] parts = dottedQuad.split("_", 2);
        // Because of String.split's contract, parts.length can only be 1 or 2
        return new IntIPv4(Net.convertStringAddressToInt(parts[0]),
                parts.length == 1 ? 32 : Integer.parseInt(parts[1]));
    }

    public static IntIPv4 fromString(String dottedQuad, int length) {
        return new IntIPv4(Net.convertStringAddressToInt(dottedQuad), length);
    }

    public static IntIPv4 fromBytes(byte[] ipAddress) {
        return fromBytes(ipAddress, 32);
    }

    public static IntIPv4 fromBytes(byte[] fourBytes, int length) {
        int ipAddr = 0;
        for (int i = 0; i < 4; i++) {
            ipAddr |= (fourBytes[i] & 0xff) << ((3-i)*8);
        }
        return new IntIPv4(ipAddr, length);
    }

     /**
        * Convert this object to its String representation.
        *
        *  @return
        *      A String like "192.168.0.0_24" for prefixes or "192.168.0.5" for a
        *      unicast address (i.e. if the mask length is 32).
        */
    @Override
    public String toString() {
        String dottedQuad = Net.convertIntAddressToString(address);
        return maskLength == 32
                ? dottedQuad
                :  (dottedQuad + "_" + Integer.toString(maskLength));
    }

    /**
     * Convert this object to a String representation that ignores the mask
     * length.
     *
     * @return
     *      A String like "10.0.0.6". The mask length is completely ignored.
     */
    public String toUnicastString() {
        return Net.convertIntAddressToString(address);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntIPv4 intIPv4 = (IntIPv4) o;

        if (address != intIPv4.address) return false;
        if (maskLength != intIPv4.maskLength) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = address;
        result = 31 * result + maskLength;
        return result;
    }
}