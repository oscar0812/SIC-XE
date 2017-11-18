package com.bittle.SIC;


import com.bittle.SIC.utils.Error;
import com.bittle.SIC.utils.Opcode;

import java.io.*;
import java.util.HashMap;

// LABEL    OPCODE  OPERAND
public class Assembler {
    private String SOURCE_FILE = "source.asm";
    private final String INTERMEDIATE_FILE = "intermediate";
    private final String LISTING_FILE = "listing";
    private final String OBJECT_FILE = "object.obj";

    private final Error[] ERRTAB = {
            new Error("Missing or misplaced START statement ", 0)
    };

    private final Opcode[] OPTAB = {
            new Opcode("ADD", 0x18),    // 0
            new Opcode("AND", 0x58),
            new Opcode("COMP", 0x28),
            new Opcode("DIV", 0x24),
            new Opcode("J", 0x3C),
            new Opcode("JEQ", 0x30),    // 5
            new Opcode("JGT", 0x34),
            new Opcode("JLT", 0x38),
            new Opcode("JSUB", 0x48),
            new Opcode("LDA", 0x00),
            new Opcode("LDCH", 0x50),   // 10
            new Opcode("LDL", 0x08),
            new Opcode("LDX", 0x04),
            new Opcode("MUL", 0x20),
            new Opcode("OR", 0x44),
            new Opcode("RD", 0xD8),     // 15
            new Opcode("RSUB", 0x4C),
            new Opcode("STA", 0x0C),
            new Opcode("STCH", 0x54),
            new Opcode("STL", 0x14),
            new Opcode("STX", 0x10),    // 20
            new Opcode("SUB", 0x1C),
            new Opcode("TD", 0xE0),
            new Opcode("TIX", 0x2C),
            new Opcode("WD", 0xDC),     // 24
    };

    private final HashMap<String, Long> symbolTable = new HashMap<>();

    public Assembler(String sourceFile) {
        SOURCE_FILE = sourceFile;
    }

    private long LOCCTR = 0x0;
    private long STARTING_ADDRESS = 0x0;
    private long PROGRAM_LENGTH = 0x0;

    public void pass1() {
        StringBuilder intermediateText = new StringBuilder();
        try {
            BufferedReader bufferedReader = getReader(SOURCE_FILE);
            if (bufferedReader == null)
                throw new NullPointerException("COULDN\'T OPEN FILE " + SOURCE_FILE + " FOR READING");

            long lineNumber = 0;
            String line;
            while ((line = (bufferedReader.readLine())) != null) {
                String words[] = breakUp(line);
                final String LABEL = words[0];
                final String OPCODE = words[1];
                final String OPERAND = words[2];
                if (!empty(line)) {
                    intermediateText.append(line);
                    intermediateText.append("\n");

                    if (!isComment(line)) {
                        if (eq(OPCODE, "END")) {
                            System.out.println("END!");
                            break;
                        } else if (lineNumber == 0) {
                            // first line
                            if (eq(OPCODE, "START")) {
                                if (empty(OPERAND)) {
                                    // no operand
                                    LOCCTR = 0x0;
                                } else {
                                    LOCCTR = num(OPERAND, 16);
                                }
                            } else {
                                // no start on line 0
                                LOCCTR = 0x0;
                            }
                            STARTING_ADDRESS = LOCCTR;
                            intermediateText.append(Long.toHexString(LOCCTR));
                            intermediateText.append("\n");
                            intermediateText.append("START\n");
                            intermediateText.append(OPERAND);
                            intermediateText.append("\n\n");
                        } else {
                            intermediateText.append(Long.toHexString(LOCCTR));
                            intermediateText.append("\n");
                            // rest of file
                            if (!empty(LABEL)) {
                                if (!hasSymbol(LABEL)) {
                                    addSymbol(LABEL, LOCCTR);
                                } else {
                                    System.out.println("DUPLICATE LABEL " + LABEL);
                                }
                            }
                            int index = searchOpcode(OPCODE);
                            if (index >= 0) {
                                intermediateText.append(Long.toHexString(OPTAB[index].getHexCode()));
                                intermediateText.append("\n");
                                LOCCTR += 3;
                            }else {
                                // not in opcode table
                                if (eq(OPCODE, "WORD")) {
                                    LOCCTR += 3;
                                } else if (eq(OPCODE, "RESB")) {
                                    LOCCTR += num(OPERAND, 16);
                                } else if (eq(OPCODE, "RESW")) {
                                    LOCCTR += (3 * num(OPERAND, 16));
                                } else if (eq(OPCODE, "BYTE")) {
                                    // adding wrong here, fix
                                    LOCCTR += num(""+byteValue(OPERAND), 10);
                                } else {
                                    System.out.println("Invalid Operation Code");
                                }
                                intermediateText.append(OPCODE);
                                intermediateText.append("\n");
                            }

                            intermediateText.append(OPERAND);
                            intermediateText.append("\n\n");
                        }
                        lineNumber++;
                    }
                }

            }

            PROGRAM_LENGTH = LOCCTR - STARTING_ADDRESS;

            bufferedReader.close();

            write(INTERMEDIATE_FILE, intermediateText.toString());
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }


    public void pass2() {
        System.out.println("LOCCTR: " + Long.toHexString(LOCCTR) +
                ", START: " + Long.toHexString(STARTING_ADDRESS));
    }

    private boolean isComment(String source) {
        return (source.length() > 0 && source.charAt(0) == '.');
    }

    private boolean empty(String source) {
        return (source.trim().isEmpty());
    }

    private boolean startEmpty(String source) {
        return (source.length() > 0 && Character.isWhitespace(source.charAt(0)));
    }

    private String[] breakUp(String source) {
        String[] words = {"", "", ""};
        if (empty(source)) {
            return words;
        }

        String[] broken = source.split("\\s+");

        if (startEmpty(source)) {
            // no label
            words[1] = broken[1];
            if (broken.length > 2) {
                words[2] = broken[2];
            }

        } else {
            // has a label
            words[0] = broken[0];
            if (broken.length > 1) {
                words[1] = broken[1];
                if (broken.length > 2) {
                    words[2] = broken[2];
                }
            }
        }

        return words;
    }

    private BufferedReader getReader(String fileName) {
        try {
            return new BufferedReader(new FileReader(new File(fileName)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void write(String fileName, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printArr(String[] arr) {
        for (String a : arr) {
            System.out.println(a);
        }
    }

    private long num(String line, int base) {
        try {
            return Long.parseLong(line, base);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            return -1;
        }
    }

    private boolean eq(String first, String second) {
        return first.toUpperCase().trim().equals(second.toUpperCase().trim());
    }

    private int byteValue(String operand){
        // check for missing '
        // odd number in hex
        // character length in '...' > 30

        int first = operand.indexOf('\'')+1;
        int second = first + operand.substring(first+1).indexOf('\'')+1;
        int m = operand.substring(first, second).length();

        if (operand.charAt(0) == 'x' || operand.charAt(0) == 'X') {
            if (m > 32) {
                // byte hex too long
                //ErrorLog[11] = 1;
            }

            // m must be even
            if (m % 2 == 0) {
                return m / 2;
            } else {
                //ErrorLog[12] = 1;
                return 0;
            }
        } else if (operand.charAt(0) == 'c' || operand.charAt(0) == 'C'){
            if (m > 30) {
                // char directive too long
                //ErrorLog[10] = 1;
            }
            return m;
        }
        else{
            // illegal char before first '
            return 0;
        }
    }

    // symbol methods
    private boolean hasSymbol(String key) {
        return symbolTable.containsKey(key);
    }

    private long getSymbolAddr(String key) {
        return symbolTable.get(key);
    }

    private void addSymbol(String key, long address) {
        symbolTable.put(key, address);
    }

    // op table methods
    public int searchOpcode(String mnemonic) {
        Opcode opcode = new Opcode(mnemonic, 0);
        int start = 0;
        int middle = -1;
        int end = OPTAB.length - 1;
        boolean flag = false;
        // binary search
        while (start <= end) {

            middle = (start + end) / 2;
            if ((OPTAB[middle].equals(opcode))) {
                flag = true;
                break;
            } else if (OPTAB[middle].compareTo(opcode) < 0) {
                end = middle - 1;
            } else if (OPTAB[middle].compareTo(opcode) > 0) {
                start = middle + 1;
            }
        }

        return (flag) ? middle : -1;
    }

    public static void main(String[] args) {
        Assembler assembler = new Assembler("source.asm");
        assembler.pass1();
    }
}
