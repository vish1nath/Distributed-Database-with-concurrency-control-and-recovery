package project.adb;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * This class starts the application. It contains the main method.
 * Project Due: December 2, 2014
 * @author Darren Levy
 * @author Viswanath Kammula
 */
public class DatabaseApp {
  private static TransactionManager transactionManager = TransactionManager.getInstance();
  private static Outputter output = Outputter.getInstance();

  /**
   * This method shows the state of the transaction manager, which shows the state
   * of the sites.
   * Author: Darren
   */
  private static void queryState() {
    System.out.println("Tranasaction manager: " + transactionManager.toString());
  }
  
  /**
   * This is the main method of the application. It takes in a path to a script file
   * as args[0]. It then parses it and sends each instruction to the transaction manager.
   * Author: Darren and Viswanath
   */
  public static void main(String[] args) {
    boolean debugState = false;
	BufferedReader br = null;
	boolean verbose = true;
	if (args.length > 1) {
	  verbose = Boolean.valueOf(args[1]);
	}
	output.setVerbose(verbose);
	int time = 0;
	try {
      String sCurrentLine;
      br = new BufferedReader(new FileReader(args[0]));
      while ((sCurrentLine = br.readLine()) != null) {
	    if(sCurrentLine.length() == 0 || sCurrentLine.substring(0,2).matches("//")) {
	      continue;
        }
	    if(output.isVerbose()) {
	      output.addOutput("Time: " + time + "\n" + "--------");
	    }
	    List<String> inputs = Arrays.asList(sCurrentLine.split(";"));
		for (String input : inputs) {
		  input = input.replaceAll("\\s","");
		  int index=input.indexOf('(');
		  String op=input.substring(0, index);
		  op = op.replaceAll("\\s","");
		  if(op.equals("begin") || op.equals("beginRO")) {
		    int tID = Integer.parseInt(input.substring(input.indexOf('T')+1, input.length()-1));
		    if (op.equals("beginRO")) {
	          if(output.isVerbose()){
	            output.addOutput("Begin read-only transaction " + String.valueOf(tID));
	          }
		    } else {
	          if(output.isVerbose()) {
	            output.addOutput("Begin transaction " + String.valueOf(tID));
	          }
		    }
		    transactionManager.intake(tID, op.equals("beginRO"), time);
		  } else if(op.equals("R")) {
		    int tID = Integer.parseInt(input.substring(input.indexOf('T')+1,input.indexOf(',')));
		    int i = Integer.parseInt(input.substring(input.indexOf('x')+1, input.length()-1));
		    Instruction instruction = new Instruction("read", i, time);
	        if(output.isVerbose()) {
	          output.addOutput("Transaction " + tID + " dispatched instruction:\n" + instruction);
	        }
            transactionManager.intake(instruction, tID);
		  } else if (op.equals("W")) {
	        int tID = Integer.parseInt(input.substring(input.indexOf('T')+1,input.indexOf(',')));
	        int i = Integer.parseInt(input.substring(input.indexOf('x')+1,input.lastIndexOf(',')));
	        int value = Integer.parseInt(input.substring(input.lastIndexOf(',')+1,input.lastIndexOf(')')));
	        Instruction instruction = new Instruction("write", i, value, time);
	        if(output.isVerbose()) {
	          output.addOutput("Transaction " + tID + " dispatched instruction:\n" + instruction);
	        }
	        transactionManager.intake(instruction, tID);
          } else if(op.equals("fail")) {
		    int siteID = Integer.parseInt(input.substring(input.indexOf('(')+1, input.length()-1));
	        if(output.isVerbose()) {
	          output.addOutput("Site failure: " + siteID);
	        }
		    transactionManager.siteFail(siteID);
		  } else if(op.equals("recover")) {
            int siteID = Integer.parseInt(input.substring(input.indexOf('(')+1, input.length()-1));
            if(output.isVerbose()) {
              output.addOutput("Site recovered: " + siteID);
		    }
            transactionManager.siteRecover(siteID);
          } else if(op.equals("end")) {
            int tID = Integer.parseInt(input.substring(input.indexOf('T')+1,input.indexOf(')')));
            if(output.isVerbose()) {
              output.addOutput("End transaction : " + tID);
            }
            transactionManager.endTransaction(tID);
          } else if(op.equals("dump")){
            char dump = input.charAt(5);
            if (dump == ')') {
              output.addOutput("Dump all");
              transactionManager.dump();
            } else if (dump == 'x'){
              int variable = Integer.parseInt(input.substring(input.indexOf('x')+1,input.indexOf(')')));
              output.addOutput("Dump all copies of variable at index " + variable);
              transactionManager.dumpIndex(variable);
            } else if (dump == 's'){
              output.addOutput("Dump all the committed values");
              transactionManager.shorterDump();
            }  else {
              int siteIndex = Integer.parseInt(input.substring(input.indexOf('(')+1,input.indexOf(')')));
              output.addOutput("Dump all variables at site " + siteIndex);
              transactionManager.dumpSite(siteIndex);
            }
          }
	      transactionManager.checkWaitingTransactions();
		}
		time++;
		if(debugState) {
		  queryState();
		}
	  }
	} catch (Exception e) {
	  output.addOutput("Error:\n" + e.toString());
	  for (StackTraceElement element :e.getStackTrace()) {
	    output.addOutput(element.toString());  
	  }
      e.printStackTrace();
	} finally {
	  try {
		if (br != null) {
		  br.close();
		  output.closeBufferWriter();
	      System.out.println("Done");
		}
	  } catch (IOException ex) {
	    ex.printStackTrace();
	  }
    }
  }
}
