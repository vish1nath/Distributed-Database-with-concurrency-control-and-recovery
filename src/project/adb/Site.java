package project.adb;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This class holds all the info for each Site. It maintains its own lock table and keeps
 * track of which indexes are ready to be read in case of a failure/recovery.
 * Sites have two states: ready and failed.
 * When a site fails it loses its lock table.
 * 
 * @author Darren Levy
 * @author Viswanath Kammula
 */

public class Site {
  private Integer id = 0;
  private Integer[] values = {0, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10};
  private Boolean[] readyToBeRead = {false, true, true, true, true, true, true, true, true, true, true, true, true,
		      true, true, true, true, true, true, true, true};
  private Transaction[] writeLockTable = new Transaction[21];
  private List<ArrayList<Transaction>> readLockTable = new ArrayList<ArrayList<Transaction>>(21);
  private Set<Transaction> waitingTransactions = new LinkedHashSet<Transaction>();
  private String state = "ready"; 
  private Outputter output = Outputter.getInstance();

  
  /**
   * The constructor a site takes in an ID used to keep track of the site.
   * Also, the read lock table is initialized.
   * Author: Darren
   * @param id the id of the site
   */
  public Site(int id) {
    for (int i = 0; i < 21; i++) {
      readLockTable.add(new ArrayList<Transaction>());
    }
    this.id = id;
  }
  
  /**
   * Author: Darren
   * @return the id of the site.
   */
  public int getID() {
    return id;
  }
  
  /**
   * Author: Darren
   * @return the current state of the Site. Either ready or failed.
   */
  public String getState() {
    return state;
  }
 
  /**
   * Author: Darren and Viswanath
   * @param index the index to retrieve.
   * @return the value at the index given.
   */
  public int getValueAtIndex(int index) {
    return values[index];
  }
  
  /**
   * Author: Darren
   * @return the actual write lock table.
   */
  public Transaction[] getWriteLockTable() {
    return writeLockTable;
  }
  
  /**
   * Sets a write lock for a given index
   * Author: Darren
   * @param index the index to lock
   * @param transaction the transaction that will hold the lock
   */
  public void setWriteLockAtIndex(int index, Transaction transaction) {
    writeLockTable[index] = transaction;
  }
  
  /**
   * Author: Darren
   * @return the actual read lock table.
   */
  public List<ArrayList<Transaction>> getReadLockTable() {
    return readLockTable;
  }
  
  /**
   * Author: Darren
   * Since read locks can be shared, each index has a list of read locks.
   * This method will clear that list at a given index.
   * @param index the index to remove any read locks
   */
  public void removeReadLocksAtIndex(int index) {
    readLockTable.get(index).clear();
  }
  
  /**
   * Author: Darren
   * Add a new waiting transaction
   * @param t the transaction to add to the waiting list
   */
  public void addWaitingTransaction(Transaction t) {
    waitingTransactions.add(t);
  }
  
  /**
   * Author: Darren
   * @return waiting transactions set
   */
  public List<Transaction> getWaitingTransactions() {
    List<Transaction> transactions = new ArrayList<Transaction>();
    for (Transaction t : waitingTransactions) {
      transactions.add(t);
    }
    return transactions;
  }
  
  /**
   * Author: Darren
   * Performs instructions and acquires locks for waiting transactions.
   */
  public void promoteWaitingTransactions() {
    if (state.equals("failed")){
      return;
    }
    Set<Transaction> transactionsNoLongerWaiting = new LinkedHashSet<Transaction>();
    for (Transaction transaction : waitingTransactions) {
      Instruction instruction = transaction.getLastInstruction();
      String action = instruction.getAction();
      int index = instruction.getIndex();
      if (action.equals("read") && readyToBeRead[index] &&
          (writeLockTable[index] == null || transaction.equals(writeLockTable[index]))) {
        Integer value = readValueAtIndex(index, transaction);
        if (value != null){
          transactionsNoLongerWaiting.add(transaction);
          transaction.setState("ready");
          output.addOutput("" +value);
          if(output.isVerbose()) {
            output.addOutput("Transaction " + transaction.getID() + " reads value " + value + 
                " at index " + index + " at Site " + id);
          }
        }
      } else {
        if (writeLockTable[index] == null) {
          if (readLockTable.get(index).isEmpty() || 
              (readLockTable.get(index).size() == 1 && readLockTable.get(index).get(0).equals(transaction))) {
            transaction.setState("ready");
            writeLockTable[index] = transaction;
            removeReadLocksAtIndex(instruction.getIndex());
            transactionsNoLongerWaiting.add(transaction);
            if(output.isVerbose()) {
              output.addOutput("Transaction " + transaction.getID() + " locked index " + 
                  instruction.getIndex() + " on Site " + id);
            }
            break;
          }
        }
      }
    }
    for (Transaction t : transactionsNoLongerWaiting) {
      waitingTransactions.remove(t);
    }
  }
  
  /**
   * Removes waiting transaction
   * Author: Darren
   * @param t the transaction to remove
   */
  public void removeWaitingTransaction(Transaction t) {
    waitingTransactions.remove(t);
  }
  
  /**
   * Author: Darren and Viswanath
   * @param index the index to read from
   * @param transaction the transaction that wants to read
   * @return the value read
   */
  public Integer readValueAtIndex(int index, Transaction transaction) {
    Integer value = null;
    if (writeLockTable[index] != null && transaction.equals(writeLockTable[index])) {
      for (Instruction instruction : transaction.getInstructions()) {
        if (instruction.getAction().equals("write") && instruction.getIndex() == index) {
          value = instruction.getValue();
        }
      }
    } else {
      if (!readLockTable.get(index).contains(transaction)) {
        readLockTable.get(index).add(transaction);
      }
      value = values[index];
    }
    return value;  
  }
  
  /**
   * Author: Darren and Viswanath
   * When a value is written to a site its index is ready to be read again.
   * This takes place at the end of the transaction when the values can be committed.
   * @param index the index to write
   * @param value the value to write
   */
  public void writeValueAtIndex(int index, int value) {
    values[index] = value;
    readyToBeRead[index] = true;
  }
  
  /**
   * Author: Darren
   * @return the committed values
   */
  public Integer[] getCommittedValues() {
    return values.clone();
  }
  
  /**
   * Author: Darren
   * @param index the committed value at index
   * @return the value committed to the given index
   */
  public Integer getCommittedValueAtIndex(int index) {
    return values[index];
  }
  
  /**
   * Author: Darren and Viswanath
   * When a Site fails, its read and write lock tables must be cleared and all the replicated
   * indexes are unable to be read.
   */
  public void fail() {
    readLockTable.clear();
    for (int i = 0; i < 21; i++) {
      readLockTable.add(new ArrayList<Transaction>());
      writeLockTable[i] = null;
      if (i % 2 == 0) {
        readyToBeRead[i] = false;
      }
    }
    state = "failed";
  }
  
  /**
   * Author: Darren
   * @param index the index to check if it's ready to be read
   * @return true or false depending on if the given index is ready to be read or not
   */
  public Boolean isReadyToBeRead(int index) {
	  return readyToBeRead[index];
  }
  
  /**
   * Author: Darren and Viswanath
   * When a site recovers its state is set to ready and its waiting transactions get promoted.
   */
  public void recover() {
    state = "ready";
    promoteWaitingTransactions();
  }
  
  /**
   * Author: Darren
   * @return a string containing the state and value at each index along with the locks held
   * at each index.
   */
  public String toString() {
    String val = "Site " + this.getID() + "\nState: " + state + "\n";
    for (int i = 1; i < 21; i++) {
      if (writeLockTable[i] == null && readLockTable.get(i).isEmpty() ) {
        val += "Index: " + i + " Value: " + values[i] + "\n";
      } else if (writeLockTable[i] == null)  {
        val += "Index: " + i + " Value: " + values[i] + " Read locked by: " + readLockTable.get(i) + "\n";
      } else {
        val += "Index: " + i + " Value: " + values[i] + " Write locked by: " + writeLockTable[i] + "\n";
      }
    }
    return val;
  }
}
