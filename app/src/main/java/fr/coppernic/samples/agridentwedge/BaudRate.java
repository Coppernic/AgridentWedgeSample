package fr.coppernic.samples.agridentwedge;

public enum BaudRate {
    B9600((byte) 0x00),
    B19200((byte) 0x01),
    B38400((byte) 0x02),
    B57600((byte) 0x03),
    B115200((byte) 0x04);

    private byte value;

    BaudRate(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}
