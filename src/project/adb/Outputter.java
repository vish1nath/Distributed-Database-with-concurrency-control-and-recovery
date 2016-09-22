package project.adb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This singleton class, keeps track of all the outputs to be written to
 * the output file.
 * @author Darren Levy
 * @author Viswanath Kammula
 */
public class Outputter {
  private static final Outputter INSTANCE = new Outputter();
  private Boolean verbose = true;
  private List<String> output = new ArrayList<String>();
  private FileWriter fw;
  private BufferedWriter bw;
  
  /**
   * Private constructor is part of a singleton class.
   * Create the output file.
   * Author: Darren and Viswanath
   */
  private Outputter(){
    try {
      File file = new File("dv-output.txt");
      if (!file.exists()) {
        file.createNewFile();
      }
      fw = new FileWriter(file.getAbsoluteFile());
      bw = new BufferedWriter(fw);
    } catch (IOException e) {
      e.printStackTrace();
    } 
  }
  
  /**
   * This is part of a singleton implementation.
   * Author: Darren
   * @return the instance of the transaction manager
   */
  public static Outputter getInstance() {
    return INSTANCE;
  }
  
  /**
   * Author: Darren
   * @return true if verbose output, false otherwise
   */
  public Boolean isVerbose() {
    return verbose;
  }
  
  /**
   * Author: Darren
   * @param verbose true if verbose output, false otherwise
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }
  
  /**
   * Writes output to file
   * Author: Darren and Viswanath
   * @param newOutput the string to write to file
   */
  public void addOutput(String newOutput) {
    output.add(newOutput);    
    try {
      bw.write(newOutput + "\n");   
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Close the buffer when at the end
   * Author: Darren and Viswanath
   */
  public void closeBufferWriter() {
    try {
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Author: Darren and Viswanath
   * @return all the output as a String
   */
  public String toString() {
    String outputString = "";
    for (int i = 0;i < output.size(); i++) {
      outputString += output.get(i) + "\n";
    }
    return outputString;
  }
}
