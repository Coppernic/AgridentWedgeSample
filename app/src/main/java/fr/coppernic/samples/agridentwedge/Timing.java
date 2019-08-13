package fr.coppernic.samples.agridentwedge;

public enum Timing {
    T100ms((byte) 0x00),
    T50ms((byte) 0x01),
    T70ms((byte) 0x02),
    VARIABLE_TIMING((byte) 0x03);

    private byte value;

    Timing(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}
