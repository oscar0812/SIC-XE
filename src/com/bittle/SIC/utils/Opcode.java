package com.bittle.SIC.utils;

public class Opcode implements Comparable<Opcode> {
    private String mnemonic;
    private int hexCode;

    public Opcode(String mnemonic, int hexCode) {
        this.mnemonic = mnemonic;
        this.hexCode = hexCode;
    }

    private String getMnemonic() {
        return mnemonic;
    }

    public int getHexCode() {
        return hexCode;
    }

    @Override
    public int compareTo(Opcode o) {
        return o.getMnemonic().compareTo(getMnemonic());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != getClass())
            return false;
        Opcode o = (Opcode) obj;
        return o.getMnemonic().equals(getMnemonic());
    }
}
