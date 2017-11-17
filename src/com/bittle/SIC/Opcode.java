package com.bittle.SIC;

public class Opcode {
    private String mnemonic;
    private int hexCode;
    public Opcode(String mnemonic, int hexCode){
        this.mnemonic = mnemonic;
        this.hexCode = hexCode;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public int getHexCode() {
        return hexCode;
    }
}
