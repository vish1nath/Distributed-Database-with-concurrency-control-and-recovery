package project.adb;

/**
 * This class holds each instruction
 * @author Darren
 *
 */
public class Instruction {
  private String action;
  private int index;
  private Integer value;
  private int timestamp;
  
  /**
   * The constructor with a value is a write
   * Author: Darren
   * @param action write
   * @param index the index to write to
   * @param value the to write
   * @param timestamp the time it is sent to the transaction manager
   *
   */
  Instruction(String action, int index, int value, int timestamp) {
    this.action = action;
    this.index = index;
    this.value = value;
    this.timestamp = timestamp;
  }
  
  /**
   * The constructor without a value is a read
   * Author: Darren
   * @param action read
   * @param index the index to read from
   * @param timestamp the time the instruction is sent to the transaction manager
   */
  Instruction(String action, int index, int timestamp) {
    this.action = action;
    this.index = index;
    this.value = null;
    this.timestamp = timestamp;
  }
  
  /**
   * Author: Darren
   * @return read or write
   */
  public String getAction() {
    return action;
  }

  /**
   * Author: Darren
   * @return the index read or written
   */
  public int getIndex() {
    return index;
  }

  /**
   * Author: Darren
   * @return the value to write
   */
  public int getValue() {
    return value;
  }

  /**
   * Author: Darren
   * @return the time the instruction is sent to the transaction manager
   */
  public int getTimestamp() {
    return timestamp;
  }
  
  /**
   * Author: Darren
   * @return a string of the timestamp, action, index and value if available
   */
  public String toString() {
    String val = (action.equals("read")) ? "" : "Value: " + String.valueOf(value) + "\n";
    return "Timestamp: " + String.valueOf(timestamp) + "\n" +
        "Action: " + action + "\n" +
        "Index: " + String.valueOf(index) + "\n" + val;
  }
}
