package com.bittle.SIC;


import com.bittle.SIC.utils.Error;
import com.bittle.SIC.utils.Opcode;

import java.io.*;
import java.util.HashMap;
import java.util.stream.IntStream;

// LABEL    OPCODE  OPERAND
public class Assembler {
    private String SOURCE_FILE;
    private final String INTERMEDIATE_FILE = "intermediate";
    private final String LISTING_FILE = "listing";
    private final String OBJECT_FILE = "object.obj";

    private final Error[] ERRTAB = {
            new Error("Duplicate label", 0),
            new Error("Illegal label", 1), /* [0] != Alpha || label != AlphaNum */
            new Error("Missing or illegal operand on START directive", 2), /* isn't hex number or doesn't exist*/
            new Error("Missing or illegal operand on END directive", 3),
            new Error("Too many symbols in source program", 4), /* > 500 */
            new Error("Program too long", 5), /* > 32768 */
            new Error("Symbol too long", 6), /* Symbol len > 6 */
            new Error("Illegal BYTE directive", 7), /* !c || !x || not c' || not x' */
            new Error("BYTE character directive too long", 8), /* > 30 */
            new Error("BYTE hex directive too long", 9), /* > 32 */
            new Error("BYTE hex length is odd", 10),
            new Error("First \' missing from BYTE", 12),
            new Error("Last \' missing from BYTE", 13),
            new Error("Missing operand", 14),
            new Error("Invalid Operation Code", 15),
            new Error("Missing or misplaced operand in RESW statement ", 16),
            new Error("Illegal operand in RESW statement", 17),
            new Error("Missing or misplaced operand in RESB statement ", 18),
            new Error("Illegal operand in RESB statement", 19),
            new Error("Missing or misplaced operand in WORD statement", 20),
            new Error("Illegal operand in WORD statement", 21),
            new Error("Missing or misplaced operand in BYTE statement", 22),

            /* pass 2 */
            new Error("Undefined symbol in operand ", 23),
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

    // hash map for symbol since lots of insertions and retrievals
    private final HashMap<String, Long> symbolTable = new HashMap<>();

    public Assembler(String sourceFile) {
        SOURCE_FILE = sourceFile;
    }

    private long LOCCTR = 0x0;
    private long STARTING_ADDRESS = 0x0;
    private long PROGRAM_LENGTH = 0x0;

    public void pass1() {
        final long MAX_PROGRAM_LENGTH = 32768;
        StringBuilder intermediateText = new StringBuilder();
        try {
            BufferedReader bufferedReader = getReader(SOURCE_FILE);
            if (bufferedReader == null)
                throw new NullPointerException("COULDN\'T OPEN FILE " + SOURCE_FILE + " FOR READING");

            long lineNumber = 0;
            String line;
            boolean hasEnd = false;
            while ((line = (bufferedReader.readLine())) != null) {

                String words[] = breakUp(line);
                final String LABEL = words[0];
                final String OPCODE = words[1];
                final String OPERAND = words[2];
                if (!empty(line)) {
                    intermediateText.append(line);
                    intermediateText.append("\n");
                    if (empty(OPERAND)) {
                        // no operand
                        ERRTAB[14].setFlag(true);
                    }

                    if (!isComment(line)) {
                        if (eq(OPCODE, "END")) {
                            hasEnd = true;
                            intermediateText.append("END\n\n\n\n");
                            break;
                        } else if (lineNumber == 0) {
                            handleFirstLine(intermediateText, OPCODE, OPERAND);
                        } else {
                            intermediateText.append(Long.toHexString(LOCCTR));
                            intermediateText.append("\n");
                            // rest of file
                            if (!empty(LABEL)) {
                                addLabelToSymbolTable(LABEL);
                            }
                            int index = searchOpcode(OPCODE);
                            if (index >= 0) {
                                intermediateText.append(Long.toHexString(OPTAB[index].getHexCode()));
                                intermediateText.append("\n");
                                LOCCTR += 3;
                            } else {
                                handleSpecialOpcodes(intermediateText, OPCODE, OPERAND);
                            }
                            intermediateText.append(OPERAND);
                            intermediateText.append("\n");
                        }
                        lineNumber++;
                        // append any errors here
                        intermediateText.append(getErrors());
                        intermediateText.append("\n");
                        resetErrors();
                    }
                }

            }

            PROGRAM_LENGTH = LOCCTR - STARTING_ADDRESS;
            if (PROGRAM_LENGTH > MAX_PROGRAM_LENGTH) {
                // program too long
                ERRTAB[5].setFlag(true);
            }
            if (!hasEnd) {
                // no END found
                ERRTAB[3].setFlag(true);
            }
            intermediateText.append(getErrors());
            bufferedReader.close();
            write(INTERMEDIATE_FILE, intermediateText.toString());
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    public void pass2() {
        // only read from intermediate file on pass 2
        try {
            BufferedReader bufferedReader = getReader(INTERMEDIATE_FILE);
            if (bufferedReader == null)
                throw new NullPointerException("COULDN\'T OPEN FILE " + INTERMEDIATE_FILE + " FOR READING");

            // read the intermediate file every five lines

        } catch (NullPointerException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

    }

    // pass helper methods
    private void handleFirstLine(StringBuilder intermediateText, String OPCODE, String OPERAND){
        if (eq(OPCODE, "START")) {
            long num = num(OPERAND, 16);
            if (empty(OPERAND) || num < 0) {
                // no operand or not a hex
                ERRTAB[2].setFlag(true);
                LOCCTR = 0x0;
            } else {
                LOCCTR = num;
            }
        } else {
            // no start on line 0
            ERRTAB[2].setFlag(true);

            LOCCTR = 0x0;
        }
        STARTING_ADDRESS = LOCCTR;
        intermediateText.append(Long.toHexString(LOCCTR));
        intermediateText.append("\n");
        intermediateText.append("START\n");
        intermediateText.append(OPERAND);
        intermediateText.append("\n");
    }

    private void addLabelToSymbolTable(String LABEL){
        if (isAlphaNum(LABEL) && isAlpha(LABEL.charAt(0))) {
            // legal label
            if (!hasSymbol(LABEL)) {
                // not in symbol table
                if (tooManySymbols()) {
                    ERRTAB[4].setFlag(true);
                } else {
                    // fits in symbol table
                    if (symbolTooLong(LABEL)) {
                        ERRTAB[6].setFlag(true);
                    } else
                        addSymbol(LABEL, LOCCTR);
                }
            } else {
                // duplicate label
                ERRTAB[0].setFlag(true);
            }
        } else {
            // illegal label
            ERRTAB[1].setFlag(true);
        }
    }

    private void handleSpecialOpcodes(StringBuilder intermediateText, String OPCODE, String OPERAND){
        // not in opcode table
        if (eq(OPCODE, "RESW")) {
            if (empty(OPERAND)) {
                ERRTAB[16].setFlag(true);
            } else if (!isDigit(OPERAND)) {
                ERRTAB[17].setFlag(true);
            } else
                LOCCTR += (3 * num(OPERAND, 10));

        } else if (eq(OPCODE, "RESB")) {
            if (empty(OPERAND)) {
                ERRTAB[18].setFlag(true);
            } else if (!isDigit(OPERAND)) {
                ERRTAB[19].setFlag(true);
            } else
                LOCCTR += num(OPERAND, 10);

        } else if (eq(OPCODE, "WORD")) {
            if (empty(OPERAND)) {
                ERRTAB[20].setFlag(true);
            } else if (!isDigit(OPERAND)) {
                ERRTAB[21].setFlag(true);
            } else
                LOCCTR += 3;

        } else if (eq(OPCODE, "BYTE")) {
            if (empty(OPERAND))
                ERRTAB[22].setFlag(true);
            else
                LOCCTR += byteValue(OPERAND);
        } else {
            ERRTAB[15].setFlag(true);
        }
        intermediateText.append(OPCODE);
        intermediateText.append("\n");
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

    private int byteValue(String operand) {
        // error 7 = [0]!=c || [0]!=x
        // check for missing '
        // odd number in hex
        // character length in '...' > 30
        // hex length in '...'>32

        int first = operand.indexOf('\'');
        if (first < 0) {
            ERRTAB[12].setFlag(true);
            return 0;
        }
        int second = first + operand.substring(first + 1).indexOf('\'');

        if (second < 0) {
            ERRTAB[13].setFlag(true);
            return 0;
        }

        int m = operand.substring(first + 1, second + 1).length();

        if (operand.charAt(0) == 'x' || operand.charAt(0) == 'X') {
            if (m > 32) {
                // byte hex too long
                ERRTAB[9].setFlag(true);
                return 0;
            }

            // m must be even
            if (m % 2 == 0) {
                return m / 2;
            } else {
                ERRTAB[10].setFlag(true);
                return 0;
            }
        } else if (operand.charAt(0) == 'c' || operand.charAt(0) == 'C') {
            if (m > 30) {
                // char directive too long
                ERRTAB[8].setFlag(true);
                return 0;
            }
            return m;
        } else {
            // illegal char before first '
            ERRTAB[7].setFlag(true);
            return 0;
        }
    }

    // String methods
    private boolean eq(String first, String second) {
        return first.toUpperCase().trim().equals(second.toUpperCase().trim());
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

    private boolean isAlphaNum(String source) {
        for (int x = 0; x < source.length(); x++) {
            char c = source.charAt(x);
            if (!isAlpha(c) && !isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isDigit(String source){
        return IntStream.range(0, source.length()).allMatch(x -> isDigit(source.charAt(x)));
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
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

    private boolean tooManySymbols() {
        return symbolTable.size() > 500;
    }

    private boolean symbolTooLong(String symbol) {
        return symbol.length() > 6;
    }

    // op table methods
    private int searchOpcode(String mnemonic) {
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

    // error methods
    private void resetErrors() {
        IntStream.range(0, ERRTAB.length).forEachOrdered(x -> ERRTAB[x].setFlag(false));
    }

    private String getErrors() {
        StringBuilder builder = new StringBuilder();
        for (int x = 0; x < ERRTAB.length; x++) {
            builder.append((ERRTAB[x].isSet()) ? (x+" ") : "");
        }
        return builder.toString().trim();
    }

    public static void main(String[] args) {
        Assembler assembler = new Assembler("source.asm");
        assembler.pass1();
    }
}
