package fr.coppernic.samples.agridentwedge;

public enum TagTypes {
    None((byte)0x00),
    FDX_B((byte)0x02),
    HDX((byte)0x04),
    FDX_B_HDX((byte)0x06),
    H4002((byte)0x08),
    FDX_B_H4002((byte)0x0A),
    HDX_H4002((byte)0x0C),
    FDX_B_HDX_H4002((byte)0x0E);

    private byte value;

    TagTypes(byte value){
        this.value = value;
    }

    public byte getValue(){
        return value;
    }
}
