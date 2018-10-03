package fr.coppernic.samples.agridentwedge;

public enum OutputFormat {
    ASCII((byte)0x01),
    BYTE_STRUCTURE((byte)0x02),
    COMPACT_CODING((byte)0x03),
    ISO_24631((byte)0x04),
    RAW_DATA((byte)0x06),
    SHORT_ASCII_15((byte)0x07),
    NLIS((byte)0x08),
    CUSTOM_OUTPUT_FORMAT((byte)0x09),
    SHORT_ASCII_16((byte)0x17),
    SCP_FORMAT((byte)0x21);

    private byte value;

    OutputFormat(byte value){
        this.value = value;
    }

    public byte getValue(){
        return value;
    }
}
