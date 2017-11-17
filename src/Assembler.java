import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Assembler {
    private String SOURCE_FILE = "source.asm";

    public Assembler(String sourceFile) {
        SOURCE_FILE = sourceFile;
    }

    public void pass1() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(SOURCE_FILE)));

            // get all the text into builder
            String line;
            while ((line = (bufferedReader.readLine())) != null) {
                
            }

            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("COULDN\'T OPEN FILE "+SOURCE_FILE);
        }
    }



    public void pass2(){

    }

    public static void main(String[] args){
        Assembler assembler = new Assembler("source.asm");
        assembler.pass1();
    }
}
