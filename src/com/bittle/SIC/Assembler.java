package com.bittle.SIC;

import com.bittle.SIC.utils.Error;
import com.bittle.SIC.utils.Opcode;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.stream.IntStream;

// LABEL    OPCODE  OPERAND
public class Assembler {
    private String SOURCE_FILE;
    private final String INTERMEDIATE_FILE = "intermediate";

    private final Error[] ERRTAB = {
            new Error("Duplicate label"),
            new Error("Illegal label"), /* [0] != Alpha || label != AlphaNum */
            new Error("Missing or illegal operand on START directive"), /* isn't hex number or doesn't exist*/
            new Error("Missing or illegal operand on END directive"),
            new Error("Too many symbols in source program"), /* > 500 */
            new Error("Program too long"), /* > 32768 */                    /* LINE 5 */
            new Error("Symbol too long"), /* Symbol len > 6 */
            new Error("Illegal BYTE directive"), /* !c || !x || not c' || not x' */
            new Error("BYTE character directive too long"), /* > 30 */
            new Error("BYTE hex directive too long"), /* > 32 */
            new Error("BYTE hex length is odd"),                            /* LINE 10 */
            new Error("First \' missing from BYTE"),
            new Error("Last \' missing from BYTE"),
            new Error("Missing operand"),
            new Error("Invalid Operation Code"),
            new Error("Missing or misplaced operand in RESW statement "),   /* LINE 15 */
            new Error("Illegal operand in RESW statement"),
            new Error("Missing or misplaced operand in RESB statement "),
            new Error("Illegal operand in RESB statement"),
            new Error("Missing or misplaced operand in WORD statement"),
            new Error("Illegal operand in WORD statement"),                 /* LINE 20 */
            new Error("Missing or misplaced operand in BYTE statement"),
            new Error("Missing Program Name"),
            /* pass 2 */
            new Error("Undefined symbol in operand "),

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

    private String PROGRAM_NAME = "";
    private long LOCCTR = 0x0;
    private long STARTING_ADDRESS = 0x0;
    private long PROGRAM_LENGTH = 0x0;

    public void assemble() {
        pass1();
        pass2();
    }

    private void pass1() {
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

                    if (!isComment(line)) {

                        if (empty(OPERAND)) {
                            // no operand
                            ERRTAB[13].setFlag(true);
                        }

                        if (eq(OPCODE, "END")) {
                            hasEnd = true;
                            intermediateText.append("END\n\n\n\n");
                            break;
                        } else if (lineNumber == 0) {
                            handleFirstLine(intermediateText, LABEL, OPCODE, OPERAND);
                        } else {
                            // handle rest of source lines
                            handlePassOne(intermediateText, LABEL, OPCODE, OPERAND);
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

            System.out.println("STARTING ADDRESS: " + Long.toHexString(STARTING_ADDRESS)
                    + "\nPROGRAM LENGTH: " + Long.toHexString(PROGRAM_LENGTH));
            System.out.println("INTERMEDIATE FILE CREATED...");
            write(INTERMEDIATE_FILE, intermediateText.toString());
        } catch (NullPointerException | IOException e) {
            //e.printStackTrace();
            System.out.println(e.getMessage());
            // exit the program, no point in going if main source can't be loaded
            System.exit(0);
        }
    }

    private void pass2() {
        // only read from intermediate file on pass 2, and write to object and listing files
        final String LISTING_FILE = "listing";
        final String OBJECT_FILE = "object.obj";
        try {
            BufferedReader bufferedReader = getReader(INTERMEDIATE_FILE);
            if (bufferedReader == null)
                throw new NullPointerException("COULDN\'T OPEN FILE " + INTERMEDIATE_FILE + " FOR READING");

            StringBuilder startAddress = new StringBuilder();
            StringBuilder listingText = new StringBuilder();
            StringBuilder objectText = new StringBuilder();
            StringBuilder objectTextRecord = new StringBuilder();
            StringBuilder[] builders = {startAddress, listingText, objectText, objectTextRecord}; /*for parameters*/

            objectText.append(headerRecord());

            // read the intermediate file every five lines
            String[] lines = {"", "", "", "", ""};
            int lineNumber = 0;
            int MAX_WORDS = 5;
            // put it in an array in order to be able to modify them, this or take them outside method
            Boolean[] flags = {false, false};   // hasErrors, fromRES

            String line;
            while ((line = (bufferedReader.readLine())) != null) {
                resetErrors();
                if (!isComment(line)) {
                    if (lineNumber % MAX_WORDS == 0 && lineNumber > 0) {
                        // already have lines needed
                        handlePassTwo(lines, flags, builders);
                    }
                    lines[lineNumber % MAX_WORDS] = line;
                    lineNumber++;
                } else {
                    // comment
                    listingText.append(line);
                    listingText.append("\n");
                }

            }

            // last text record once the file is read (the one before E record)
            if (objectTextRecord.length() > 0) {
                objectText.append(textRecord(startAddress.toString(), objectTextRecord.toString()));
                objectTextRecord.setLength(0);
            }

            if (lineNumber % MAX_WORDS == 0 && !flags[0]) {
                // no errors
                listingText.append(makeListingLine("", "", lines[0]));
                // append End record to object file
                objectText.append(endRecord());
                System.out.println("OBJECT FILE CREATED...");
                System.out.println("\nObject code:\n" + objectText.toString() + "\n\n");
            } else {
                handlePassTwoErrors(lines, OBJECT_FILE, listingText);
            }
            write(LISTING_FILE, listingText.toString());
            System.out.println("INTERMEDIATE FILE CREATED...");

        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

        System.out.println("PROGRAM FINISHED");
    }

    // pass 1 helper methods
    private void handleFirstLine(StringBuilder intermediateText, String LABEL, String OPCODE, String OPERAND) {
        if (empty(LABEL)) {
            ERRTAB[22].setFlag(true);
        } else {
            PROGRAM_NAME = LABEL;
        }
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

    private void handlePassOne(StringBuilder intermediateText, String LABEL, String OPCODE, String OPERAND) {
        intermediateText.append(Long.toHexString(LOCCTR));
        intermediateText.append("\n");
        // rest of file
        if (!empty(LABEL)) {
            addToSymTable(LABEL);
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

    private void addToSymTable(String LABEL) {
        if (isAlphaNum(LABEL) && Character.isAlphabetic(LABEL.charAt(0))) {
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

    private void handleSpecialOpcodes(StringBuilder intermediateText, String OPCODE, String OPERAND) {
        // not in opcode table
        if (eq(OPCODE, "RESW")) {
            if (empty(OPERAND)) {
                ERRTAB[15].setFlag(true);
            } else if (!isDigit(OPERAND)) {
                ERRTAB[16].setFlag(true);
            } else
                LOCCTR += (3 * num(OPERAND, 10));

        } else if (eq(OPCODE, "RESB")) {
            if (empty(OPERAND)) {
                ERRTAB[17].setFlag(true);
            } else if (!isDigit(OPERAND)) {
                ERRTAB[18].setFlag(true);
            } else
                LOCCTR += num(OPERAND, 10);

        } else if (eq(OPCODE, "WORD")) {
            if (empty(OPERAND)) {
                ERRTAB[19].setFlag(true);
            } else if (!isDigit(OPERAND)) {
                ERRTAB[20].setFlag(true);
            } else
                LOCCTR += 3;

        } else if (eq(OPCODE, "BYTE")) {
            if (empty(OPERAND))
                ERRTAB[21].setFlag(true);
            else
                LOCCTR += byteValue(OPERAND);
        } else {
            ERRTAB[14].setFlag(true);
        }
        intermediateText.append(OPCODE);
        intermediateText.append("\n");
    }

    // pass 2 helper methods
    private void handlePassTwo(String[] lines, Boolean[] flags, StringBuilder[] builders) {
        StringBuilder startAddress = builders[0];
        StringBuilder listingText = builders[1];
        StringBuilder objectText = builders[2];
        StringBuilder objectTextRecord = builders[3];

        String objectCode;
        String SOURCE_LINE = lines[0];
        String ADDRESS = lines[1];
        String OPCODE = lines[2];
        String OPERAND = lines[3];
        String ERRORS = lines[4];
        if (startAddress.length() == 0) {
            startAddress.setLength(0);
            startAddress.append(ADDRESS);
        }
        if (noObjectCode(OPCODE)) {
            // these don't have an object code
            objectCode = "";
        } else {
            objectCode = getObjectCode(OPCODE, OPERAND);
        }
        String text;
        String err = getErrors(ERRORS);
        if (!empty(err)) {
            // Couldn't find symbol when getting object code
            // has errors
            objectCode = "";
            ERRORS += " " + (getErrors());
            flags[0] = true;    // has errors
            text = makeListingLine(ADDRESS, "", SOURCE_LINE);
        } else {
            if (!noObjectCode(OPCODE) && !objectCode.isEmpty() && !eq(OPCODE, "BYTE")) // if has object code
                objectCode = prependZero(objectCode, 6 - objectCode.length());
            text = makeListingLine(ADDRESS, objectCode, SOURCE_LINE);
        }

        listingText.append(text);
        listingText.append("\n");
        // append any errors
        if (!empty(err)) {
            listingText.append(getErrors(ERRORS));
            listingText.append("\n");
        }

        // object text check
        if (eq(OPCODE, "RESW") || eq(OPCODE, "RESB")) {
            if (objectTextRecord.length() > 0) {
                objectText.append(textRecord(startAddress.toString(), objectTextRecord.toString()));
                objectTextRecord.setLength(0);
                startAddress.setLength(0);
                startAddress.append(ADDRESS);
                flags[1] = true;    // fromRES = true
            }
        } else if (flags[1]) {      // if fromRES
            startAddress.setLength(0);
            startAddress.append(ADDRESS);
            flags[1] = false;       // fromRES = false
        }

        if (objectTextRecord.length() + objectCode.length() < 61) {
            // keep appending
            objectTextRecord.append(objectCode);
        } else {
            // too long, start new record
            objectText.append(textRecord(startAddress.toString(), objectTextRecord.toString()));
            objectTextRecord.setLength(0);
            objectTextRecord.append(objectCode);
            startAddress.setLength(0);
            startAddress.append(ADDRESS);
        }

    }

    private void handlePassTwoErrors(String[] lines, String OBJECT_FILE, StringBuilder listingText) {
        // has errors, check if last line is also error line
        String err = getErrors(lines[0]);
        if (!empty(err)) {
            listingText.append(err);
            listingText.append("\n");
        }
        File object = new File(OBJECT_FILE);
        // if error found, delete object file if it exists
        if (!object.exists() || object.delete())
            System.out.println("OBJECT FILE COULDN'T BE CREATED, LOOK AT LISTING FILE...");
    }

    private BufferedReader getReader(String fileName) {
        try {
            return new BufferedReader(new FileReader(new File(fileName)));
        } catch (Exception e) {
            //e.printStackTrace();
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

    private long num(String line, int base) {
        try {
            line = line.toUpperCase();
            return Long.parseLong(line, base);
        } catch (NumberFormatException nfe) {
            //nfe.printStackTrace();
            return -1;
        }
    }

    // byte methods
    private int byteValue(String operand) {
        // error 7 = [0]!=c || [0]!=x
        // check for missing '
        // odd number in hex
        // character length in '...' > 30
        // hex length in '...'>32
        String inside = getInsideByteCode(operand);
        if (inside == null)
            return 0;
        int m = inside.length();

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

    private String getInsideByteCode(String operand) {
        int first = operand.indexOf('\'');
        if (first < 0) {
            ERRTAB[11].setFlag(true);
            return null;
        }
        int second = first + operand.substring(first + 1).indexOf('\'');

        if (second < 0) {
            ERRTAB[12].setFlag(true);
            return null;
        }
        return operand.substring(first + 1, second + 1);
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
            if (broken.length > 2) words[2] = broken[2];

        } else {
            // has a label
            words[0] = broken[0];
            if (broken.length > 1) {
                words[1] = broken[1];
                if (broken.length > 2) words[2] = broken[2];
            }
        }

        return words;
    }

    private boolean isAlphaNum(String source) {
        for (int x = 0; x < source.length(); x++) {
            char c = source.charAt(x);
            if (!Character.isAlphabetic(c) && !Character.isDigit(c)) return false;
        }
        return true;
    }

    private boolean isDigit(String source) {
        return IntStream.range(0, source.length()).allMatch(x -> Character.isDigit(source.charAt(x)));
    }

    // symbol methods
    private boolean hasSymbol(String key) {
        return symbolTable.containsKey(key);
    }

    private long symbolAddress(String key) {
        try {
            return symbolTable.get(key);
        } catch (NullPointerException e) {
            return -1;
        }
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
        for (int x = 0; x < ERRTAB.length; x++) builder.append((ERRTAB[x].isSet()) ? (x + " ") : "");
        return builder.toString().trim();
    }

    // listing file helpers
    private String makeListingLine(String address, String objectCode, String source) {
        if (empty(address) && empty(objectCode)) {
            return "\t\t\t\t" + source;
        }
        String builder = String.format("%-7s", address.toUpperCase()) +
                String.format("%-9s", objectCode) +
                source;
        return builder.trim();
    }

    private boolean noObjectCode(String OPCODE) {
        return eq(OPCODE, "START") || eq(OPCODE, "RESW") || eq(OPCODE, "RESB");
    }

    private String getErrors(String errorLine) {

        if (empty(errorLine)) {
            return "";
        }
        String[] arr = errorLine.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String anArr : arr) {
            if (!empty(anArr)) {
                int err = (int) num(anArr, 10);
                if (err != -1) {
                    // found error
                    builder.append("> ");
                    builder.append(ERRTAB[err].getDescription());
                    builder.append("\n");
                }
            }
        }

        return builder.toString().trim();
    }

    // object code methods
    private String getObjectCode(String OPCODE, String OPERAND) {
        boolean x = false;
        String objectCode = OPCODE;
        if (OPERAND.toUpperCase().endsWith(",X")) {
            x = true;
            OPERAND = OPERAND.substring(0, OPERAND.toUpperCase().indexOf(",X"));
        }
        long address = symbolAddress(OPERAND);

        if (x && address != -1) {
            // if has ,x and is in symbol table
            address = address | 32768;
        }

        if (address == -1) {
            if (eq(OPCODE, "4c")) {
                // rsub code
                objectCode += "0000";
            } else if (eq(OPCODE, "BYTE")) {
                // BYTE
                String inside = getInsideByteCode(OPERAND);
                if (inside == null) {
                    return "";
                }
                if (OPERAND.toUpperCase().charAt(0) == 'C') {
                    StringBuilder builder = new StringBuilder();
                    for (int y = 0; y < inside.length(); y++) {
                        String hex = Integer.toHexString((int) (inside.charAt(y)));
                        builder.append(hex);
                    }
                    objectCode = builder.toString();
                } else if (OPERAND.toUpperCase().charAt(0) == 'X') {
                    objectCode = inside;
                }
            } else if (eq(OPCODE, "WORD")) {
                // WORD
                long n = num(OPERAND, 10);
                if (n == -1) {
                    ERRTAB[20].setFlag(true);
                    return "";
                } else
                    objectCode = Long.toHexString(n);
            } else {
                // no such symbol
                ERRTAB[23].setFlag(true);
                return "";
            }
        } else {
            // normal cases, found symbol
            objectCode += Long.toHexString(address);
        }

        objectCode = objectCode.toUpperCase();

        return objectCode;
    }

    private String prependZero(String source, int num) {
        StringBuilder sourceBuilder = new StringBuilder(source);
        for (int x = 0; x < num; x++) {
            sourceBuilder.insert(0, '0');
        }
        source = sourceBuilder.toString();

        return source;
    }

    // object file methods
    private String headerRecord() {
        String hex = Long.toHexString(STARTING_ADDRESS);
        String builder = "H" +
                PROGRAM_NAME + "  " +
                prependZero(hex, 6 - hex.length()) +
                Long.toHexString(PROGRAM_LENGTH) + "\n";
        return builder.toUpperCase();
    }

    private String textRecord(String address, String text) {
        address = prependZero(address, 6 - address.length());
        String length = Long.toHexString(text.length() / 2);
        length = prependZero(length, 2 - length.length());
        return ("T" + address + length + text + "\n").toUpperCase();
    }

    private String endRecord() {
        String hex = Long.toHexString(STARTING_ADDRESS);
        String builder = "E" + prependZero(hex, 6 - hex.length());
        return builder.toUpperCase();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Type in SIC asm file:\n> ");
        String in = scanner.next().trim();
        Assembler assembler = new Assembler(in);
        assembler.assemble();
    }
}
