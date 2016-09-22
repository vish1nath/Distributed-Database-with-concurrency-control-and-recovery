package project.adb;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the class to hold each transaction and all of its instructions
 * @author Darren
 *
 */
public class Transaction {
  private int id;
  private boolean readOnly;
  private List<Instruction> instructions = new ArrayList<Instruction>();
  private String state;
  private int startTime;
  private Integer[] databaseSnapshot = new Integer[21];
  
  /**
   * Constructor initializes values
   * Author: Darren
   * @param id the id of the transaction
   * @param readOnly true if the transaction is read-only, false otherwise
   * @param startTime the time the transaction starts
   */
  Transaction(int id, boolean readOnly, int startTime) {
    this.id = id;
    this.setState("ready");
    this.readOnly = readOnly;
    this.startTime = startTime;
  }
  
  /**
   * Author: Darren
   * @return the ID of the transaction
   */
  public int getID() {
    return id;
  }
  
  /**
   * Author: Darren
   * @return true if the transaction is read-only, false otherwise
   */
  public boolean getReadOnly() {
    return readOnly;
  }
  
  /**
   * Author: Darren
   * @return the state of the transaction:
   */
  public String getState() {
    return state;
  }

  /**
   * Sets the state of the transaction
   * Author: Darren
   * @param state ready, waiting or aborted
   */
  public void setState(String state) {
    this.state = state;
  }
  
  /**
   * Author: Darren
   * @return the time the transaction started
   */
  public int getStartTime() {
    return startTime;
  }
  
  /**
   * Author: Darren
   * @return the list of instructions for the transaction so far
   */
  public List<Instruction> getInstructions() {
    return instructions;
  }

  /**
   * Adds a new instruction to the list of instructions
   * Author: Darren
   * @param i the instruction to add
   */
  public void addInstruction(Instruction i) {
    instructions.add(i);
  }

  /**
   * For read-only transactions a database snapshot is collected
   * Author: Darren
   * @return the snapshot of the database
   */
  public Integer[] getDatabaseSnapshot() {
    return databaseSnapshot;
  }

  /**
   * Sets the database snapshot with a given value for a given index
   * Author: Darren and Viswanath
   * @param index the index to set
   * @param value the value to set the index with
   */
  public void setDatabaseSnapshotAtIndex(int index, int value) {
    databaseSnapshot[index] = value;
  }
  
  /**
   * If the transaction is waiting, the last transaction will need to be
   * retried
   * Author: Darren
   * @return the last transaction added
   */
  public Instruction getLastInstruction() {
    return instructions.get(instructions.size() - 1);
  }

  /**
   * Author: Darren
   * Determines if two objects are equal
   * @return true if equal, false otherwise
   */
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Transaction)){
      return false;
    }
    Transaction t = (Transaction)o;
    return t.getID() == id;
  }
  
  /**
   * Author: Darren
   * @return the hash code for the object
   */
  public int hashCode() {
    return 31 * 17 + id;
  }
  
  /**
   * Author: Darren
   * @return a string containing each instruction associated to this
   * transaction along with the transaction's ID, state, start time and
   * whether it is read only or not.
   */
  public String toString() {
    String s = "";
    for (Instruction i : instructions) {
      s += "Instruction: " + i.toString();
    }
      
    return "ID: " + String.valueOf(getID()) + "\n" + 
        "State: " + state + "\n" + 
        "Read Only? " + String.valueOf(readOnly) + "\n" +
        "Start Time: " + String.valueOf(startTime) + "\n" + s;
  }
}
