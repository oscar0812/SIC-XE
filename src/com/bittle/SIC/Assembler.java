package com.bittle.SIC;


import java.io.*;

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
            new Opcode("ADD", 0x18),
            new Opcode("AND", 0x58),
            new Opcode("COMP", 0x28),
            new Opcode("DIV", 0x24),
            new Opcode("J", 0x3C),
            new Opcode("JEQ", 0x30),
            new Opcode("JGT", 0x34),
            new Opcode("JLT", 0x38),
            new Opcode("JSUB", 0x48),
            new Opcode("LDA", 0x00),
            new Opcode("LDCH", 0x50),
            new Opcode("LDL", 0x08),
            new Opcode("LDX", 0x04),
            new Opcode("MUL", 0x20),
            new Opcode("OR", 0x44),
            new Opcode("RD", 0xD8),
            new Opcode("RSUB", 0x4C),
            new Opcode("STA", 0x0C),
            new Opcode("STCH", 0x54),
            new Opcode("STL", 0x14),
            new Opcode("STX", 0x10),
            new Opcode("SUB", 0x1C),
            new Opcode("TD", 0xE0),
            new Opcode("TIX", 0x2C),
            new Opcode("WD", 0xDC),
    };

    public Assembler(String sourceFile) {
        SOURCE_FILE = sourceFile;
    }

    public void pass1() {
        StringBuilder intermediateText = new StringBuilder();
        try {
            BufferedReader bufferedReader = getReader(INTERMEDIATE_FILE);

            int lineNumber = 0;
            String line;
            while ((line = (bufferedReader.readLine())) != null) {
                String[] arr = line.split("\\s+");
                if (isEmptyLine(line)) {
                    continue;
                } else if (isComment(line)) {
                    intermediateText.append(line);
                    intermediateText.append("\n");
                } else {
                    String LABEL = "";
                    String OPCODE = "";
                    String OPERAND = "";
                    if (!startEmpty(line)) {
                        // no label
                        LABEL = arr[0];
                    }

                    if (lineNumber == 0) {
                        // first line


                    }else{

                    }

                    lineNumber++;
                }

                System.out.println(line);
            }

            bufferedReader.close();

            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(INTERMEDIATE_FILE)));
            writer.write(intermediateText.toString());

            writer.flush();
            writer.close();
        } catch (NullPointerException | IOException e ) {
            e.printStackTrace();
            System.out.println("COULDN\'T OPEN FILE " + SOURCE_FILE);
        }
    }


    public void pass2() {

    }

    private boolean isComment(String source) {
        return (source.length() > 0 && source.charAt(0) == '.');
    }

    private boolean isEmptyLine(String source) {
        return (source.trim().isEmpty());
    }

    private boolean startEmpty(String source) {
        return (source.length() > 0 && Character.isWhitespace(source.charAt(0)));
    }

    private BufferedReader getReader(String fileName){
        try {
            return new BufferedReader(new FileReader(new File(fileName)));
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        Assembler assembler = new Assembler("source.asm");
        assembler.pass1();
    }
}
