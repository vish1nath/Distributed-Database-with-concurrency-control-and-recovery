package project.adb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A singleton class to hold the instance of the transaction manager.
 * This class manages which sites to contact and lock given an instruction.
 * It maintains a linked list of all the transactions.
 * 
 * @author Darren Levy
 * @author Viswanath Kammula
 *
 */
public class TransactionManager {
	 
  private static final TransactionManager INSTANCE = 
      new TransactionManager();
  private Outputter output = Outputter.getInstance();
  private LinkedList<Transaction> transactions = new LinkedList<Transaction>();
  private Site[] sites = new Site[11];
  /**
   * This private constructor populates the transaction manager's sites.
   * Author: Darren
   */
  private TransactionManager(){
    for (int i = 0; i < sites.length; i++) {
      sites[i] = new Site(i);
    }
  }
  
  /**
   * This is part of a singleton implementation.
   * Author: Darren
   * @return the instance of the transaction manager
   */
  public static TransactionManager getInstance() {
    return INSTANCE;
  }
  
  /**
   * Adds a transaction to the list of transactions.
   * Author: Darren
   * @param t a transaction to add.
   */
  private void addTransaction(Transaction t) {
    transactions.add(t);
  }
  
  /**
   * Finds the transaction in the transaction manager's list
   * Author: Darren
   * @param id the id of the transaction
   * @return the transaction with the given id
   */
  private Transaction findTransactionBy(int id) {
    for (Transaction transaction : transactions) {
      if (transaction.getID() == id) {
        return transaction;
      }
    }
    return null;
  }
  
  /**
   * Sets the transaction's state to aborted and removes any locks it held
   * on all sites.
   * Author: Darren
   * @param transaction the transaction to abort
   */
  private void abort(Transaction transaction, String reason) {
    transaction.setState("aborted");
    for (int i = 1; i < 11; i++){
      sites[i].removeWaitingTransaction(transaction);
      Transaction[] writeLockTable = sites[i].getWriteLockTable();
      List<ArrayList<Transaction>> readLockTable = sites[i].getReadLockTable();
      for (int z = 1; z < 21; z++) {
        if (writeLockTable[z] != null && writeLockTable[z].equals(transaction)) {
          writeLockTable[z] = null;
        }
        if (readLockTable.get(z).contains(transaction)) {
          readLockTable.get(z).remove(transaction);
        } 
      }
    }
    if(output.isVerbose()) {
      output.addOutput("Transaction " + transaction.getID() + " was aborted because " + reason + ".");
    }
  }
  
  /**
   * Checks if waiting transactions can be attempted again.
   * Author: Darren and Viswanath
   */
  public void checkWaitingTransactions() {
    for (Transaction transaction : transactions) {
      if (transaction.getState().equals("waiting")) {
        if (transaction.getReadOnly()) {
          output.addOutput("Read-only transaction " + transaction.getID() + " is checking if missing index(es) are readable yet.");
          readonlySnap(transaction);
        }
      } else if (transaction.getState().equals("no-ready-site")) {
        if(output.isVerbose()) {
          output.addOutput("No site was up so transaction " + transaction.getID() +
              " tries again to dispatch instruction:\n" + transaction.getLastInstruction());
        }
        intake(transaction.getLastInstruction(), transaction.getID());
      }
    }
    for (int i = 1; i < 11; i++) {
      sites[i].promoteWaitingTransactions();
      Transaction[] writeLockTable = sites[i].getWriteLockTable();
      List<ArrayList<Transaction>> readLockTable = sites[i].getReadLockTable();
      for (Transaction transaction : sites[i].getWaitingTransactions()) {
        transaction.setState("waiting");
        Instruction instruction = transaction.getLastInstruction();
        int index = instruction.getIndex();
        if (writeLockTable[index] == null) {
          for (Transaction lockHolder : readLockTable.get(index)) {
            if (lockHolder.getStartTime() < transaction.getStartTime()) {
              abort(transaction, "Transaction " + transaction.getID() + " is younger than " + lockHolder.getID());
              break;
            }        
          }  
        } else {
          Transaction lockHolder = writeLockTable[index];
          if (lockHolder.getStartTime() < transaction.getStartTime()) {
            abort(transaction, "Transaction " + transaction.getID() + " is younger than " + lockHolder.getID());
          }
        }
      }
    }
  }
  
  /**
   * This intake method is called when a transaction begins.
   * If it is a read-only transaction, then a snapshot for that
   * transaction is saved.
   * Author: Darren and Viswanath
   * @param tID the id of the transaction
   * @param readOnly true if the transaction is read-only, false otherwise
   * @param timestamp the time the transaction begins
   */
  public void intake(int tID, boolean readOnly, int timestamp) {
    Transaction transaction = new Transaction(tID, readOnly, timestamp);
    addTransaction(transaction);
    if (readOnly) {
      readonlySnap(transaction);
    }
  }
  
  /**
   * This intake method is called when an instruction is either read or write
   * Author: Darren and Viswanath
   * @param instruction the instruction with what to do
   * @param tID the id of the transaction
   */
  public void intake(Instruction instruction, int tID) { 
    Transaction transaction = findTransactionBy(tID);
    if (transaction.getState().equals("aborted")) {
      if(output.isVerbose()) {
        output.addOutput("This transaction was aborted already.");
      }
    } else {
      if (!transaction.getState().equals("waiting") &&
          !transaction.getState().equals("no-ready-site")) {
        transaction.addInstruction(instruction);
      }
      if (transaction.getReadOnly()) {
        Integer[] snapshot = transaction.getDatabaseSnapshot();
        if(output.isVerbose()) {
         output.addOutput("" +snapshot[instruction.getIndex()]);
         output.addOutput("Read-only transaction " + transaction.getID() + " reads value " + 
             snapshot[instruction.getIndex()] + " at index " + instruction.getIndex());
        }
      } else {
        if (instruction.getAction().equals("read")) {
         performRead(instruction, transaction);
        } else {
          performWrite(instruction, transaction);
        }
      }
    }
  }
  
  /**
   * Creates a snapshot of the database for the given transaction.
   * Author: Darren and Viswanath
   * @param transaction the read-only transaction
   */
  private void readonlySnap(Transaction transaction) {
    Boolean allSitesFail = true;
    transaction.setState("ready");
	for(int i=1; i<21; i++) {
	  if (transaction.getDatabaseSnapshot()[i] != null) {
	    continue;
	  }
	  if (transaction.getDatabaseSnapshot()[i] == null && transaction.getState() != "aborted") {	
        if (i % 2 == 0) {
	      for(int j=1; j<11; j++){
		    Site site = sites[j];
		    if(site.getState() == "ready" && site.isReadyToBeRead(i)) {
			  transaction.setDatabaseSnapshotAtIndex(i, site.getValueAtIndex(i));
			  allSitesFail = false;
		  	  break;
		    } 
		  }
	      if (allSitesFail) {
	        transaction.setState("waiting");
	      }
	    } else {
		  Site site = sites[(i % 10) + 1]; 
		  if(site.getState() == "ready" && site.isReadyToBeRead(i)) {
		    transaction.setDatabaseSnapshotAtIndex(i, site.getValueAtIndex(i));
		  } else {
		    transaction.setState("waiting");
		  }
	    }
	  }
	}
  }
 
  /**
   * Performs the locks required to prepare the site(s) for a write.
   * If the transaction cannot obtain the locks, it will wait or abort.
   * Author: Darren and Viswanath
   * @param instruction the write instruction
   * @param transaction the transaction that gave the instruction
   */
  private void performWrite(Instruction instruction, Transaction transaction) {
    if (instruction.getIndex() % 2 == 0) {
      int numberOfSitesDown = 0;
      int skippedCount = 0;
      for (int i = 1; i < 11; i++) {
        boolean skip = false;
        Site site = sites[i];
        if (site.getState().equals("failed")) {
          numberOfSitesDown++;
          if (numberOfSitesDown == 10) {
            transaction.setState("no-ready-site");
          }
          continue;
        } 
        List<Transaction> readLocks = site.getReadLockTable().get(instruction.getIndex());
        if (readLocks.size() > 1 || (readLocks.size() == 1 && !readLocks.contains(transaction.getID()))) {
          for (Transaction lockHolder : readLocks) {
            if (lockHolder.equals(transaction)) {
              continue;
            }
            if (lockHolder.getStartTime() >= transaction.getStartTime()) {
              transaction.setState("waiting");
              site.addWaitingTransaction(transaction);
              output.addOutput("Transaction " + transaction.getID() + " waits because it is older than " + lockHolder.getID());
              skip = true;
            } else {
              abort(transaction, "Transaction " + transaction.getID() + " is younger than " + lockHolder.getID());
              break;
            }        
          }
        }
        if (skip) {
          skippedCount++;
          continue;
        } else if (transaction.getState().equals("aborted")) {
          return;
        } else if (site.getWriteLockTable()[instruction.getIndex()] != null &&
            !site.getWriteLockTable()[instruction.getIndex()].equals(transaction)) {
          Transaction lockHolder = site.getWriteLockTable()[instruction.getIndex()];
          if (lockHolder.getStartTime() >= transaction.getStartTime()) {
            transaction.setState("waiting");
            site.addWaitingTransaction(transaction);
            output.addOutput("Transaction " + transaction.getID() + " waits because it is older than " + lockHolder.getID() +
                " on Site " + sites[i].getID());
            skippedCount++;
            continue;
          } else {
            abort(transaction, "Transaction " + transaction.getID() + " is younger than " + lockHolder.getID());
            break;
          }
        } else {
          transaction.setState("ready");
          removeReadLocksAtIndexForSite(instruction.getIndex(), i);
          site.setWriteLockAtIndex(instruction.getIndex(), transaction);
        }
      }
      if (skippedCount > 0) {
        transaction.setState("waiting");
        if(output.isVerbose()) {
          output.addOutput("Transaction " + transaction.getID() + " locked index " + 
              instruction.getIndex() + " on all available sites");
        }
      } else if (transaction.getState().equals("ready")) {
        if(output.isVerbose()) {
          output.addOutput("Transaction " + transaction.getID() + " locked index " + 
              instruction.getIndex() + " on all available sites");
        }
      } else if (transaction.getState().equals("no-ready-site")) {
        if(output.isVerbose()) {
          output.addOutput("Transaction " + transaction.getID() + " is waiting for an available site");
        }
      } 
    } else {
      int siteIndex = (instruction.getIndex() % 10) + 1;
      Site site = sites[siteIndex];
      List<Transaction> readLocks = site.getReadLockTable().get(instruction.getIndex());
      if (readLocks.size() > 1 || (readLocks.size() == 1 && !readLocks.contains(transaction.getID()))) {
        for (Transaction lockHolder : readLocks) {
          if (lockHolder.equals(transaction)) {
            continue;
          }
          if (lockHolder.getStartTime() >= transaction.getStartTime()) {
            transaction.setState("waiting");
            site.addWaitingTransaction(transaction);
            output.addOutput("Transaction " + transaction.getID() + " waits because it is older than " + lockHolder.getID());
          } else {
            abort(transaction, "Transaction " + transaction.getID() + " is younger than " + lockHolder.getID());
            break;
          }        
        }
      }
      if (transaction.getState().equals("aborted")) {
        transaction.setState("aborted");
        return;
      } else if (transaction.getState().equals("waiting")) {
        transaction.setState("waiting");
      } else if (site.getState().equals("failed")){
        transaction.setState("no-ready-site");
      } else if (site.getWriteLockTable()[instruction.getIndex()] != null &&
          !site.getWriteLockTable()[instruction.getIndex()].equals(transaction)) {
        Transaction lockHolder = site.getWriteLockTable()[instruction.getIndex()];
        if (lockHolder.getStartTime() >= transaction.getStartTime()) {
          transaction.setState("waiting");
          site.addWaitingTransaction(transaction);
          output.addOutput("Transaction " + transaction.getID() + " waits because it is older than " + lockHolder.getID());
        } else {
          abort(transaction, "Transaction " + transaction.getID() + " is younger than " + lockHolder.getID());
        }        
      } else {
        transaction.setState("ready");
        site.setWriteLockAtIndex(instruction.getIndex(), transaction);
        removeReadLocksAtIndexForSite(instruction.getIndex(), site.getID());
        if(output.isVerbose()) {
          output.addOutput("Transaction " + transaction.getID() + " locked index " + 
              instruction.getIndex() + " on Site " + siteIndex);
        }
      }
    }
    if (transaction.getState().equals("waiting")) {
      if(output.isVerbose()) {
        output.addOutput("Transaction " + transaction.getID() + " is waiting.");
      }
    }
  }
  
  /**
   * Reads the value for the instruction if it can. It also obtains the locks
   * for the given index. If it can't get the lock, it will either wait
   * or abort.
   * Author: Darren and Viswanath
   * @param instruction the read instruction
   * @param transaction the transaction that sent the instruction
   */
  private void performRead(Instruction instruction, Transaction transaction) {
    Site site = null;
    if (instruction.getIndex() % 2 == 0) {
      for (int i = 1; i < 21; i++) {
        if (sites[i].getState().equals("ready") && sites[i].isReadyToBeRead(instruction.getIndex())) {
          site = sites[i];
          break;
        }
      }
    } else {
      if (sites[(instruction.getIndex() % 10) + 1].isReadyToBeRead(instruction.getIndex())) {
        site = sites[(instruction.getIndex() % 10) + 1];
      }
    }
    if (site != null && site.getState().equals("ready")) {
      Transaction lockHolder = site.getWriteLockTable()[instruction.getIndex()];
      if (lockHolder == null || lockHolder.getID() == transaction.getID()) {
        int value = site.readValueAtIndex(instruction.getIndex(), transaction);
        output.addOutput("" +value);
        if(output.isVerbose()) {
          output.addOutput("Transaction " + transaction.getID() + " reads value " + value + 
              " at index " + instruction.getIndex() + " at Site " + site.getID());
        }
        transaction.setState("ready");
      } else if (lockHolder.getStartTime() >= transaction.getStartTime()) {
        transaction.setState("waiting");
        site.addWaitingTransaction(transaction);
        output.addOutput("Transaction " + transaction.getID() + " waits because it is older than " + lockHolder.getID());
      } else {
        abort(transaction, "Transaction " + transaction.getID() + " is younger than " + lockHolder.getID());
      }  
    } else {
      transaction.setState("waiting");
      if(output.isVerbose()) {
        output.addOutput("Transaction " + transaction.getID() + " is waiting.");
      }
      site.addWaitingTransaction(transaction);
    }
  }
  
  /**
   * Removes the read locks for a given index on a site.
   * Used when a read lock is upgraded to a write lock.
   * Author: Darren
   * @param index the index to remove its read locks
   */
  private void removeReadLocksAtIndexForSite(int index, int siteID) {
    sites[siteID].removeReadLocksAtIndex(index);
  }
  
  /**
   * When a site fails all transactions that have locks on its indexes
   * must abort.
   * Author: Darren
   * @param siteID the id of the failed site
   */
  public void siteFail(int siteID) {
    Site site = sites[siteID];
    Set<Integer> transactionWithLocks = new HashSet<Integer>();
    for (Transaction transaction : site.getWriteLockTable()) {
      if (transaction != null) {
        transactionWithLocks.add(transaction.getID());
      }
    }
    for (List<Transaction> list : site.getReadLockTable()) {
      for (Transaction transaction : list) {
 
        if (transaction != null) {
          transactionWithLocks.add(transaction.getID());
        }
      }
    }
    for (Integer tID : transactionWithLocks) {
      Transaction transaction = findTransactionBy(tID);
      abort(transaction, "Transaction " + transaction.getID() + " held a lock on site " + siteID + " that just failed");
    }
    site.fail();
  }
  
  /**
   * Used to recover a site.
   * Author: Darren
   * @param siteID the id of the site to recover
   */
  public void siteRecover(int siteID) {
    Site site = sites[siteID];
    site.recover();
  }
  
  /**
   * Called when a transaction ends, all its writes are committed and so
   * values at sites are updated.
   * Author: Darren
   * @param tID the id of the transaction to end.
   */
  public void endTransaction(int tID) {
    Transaction transaction = findTransactionBy(tID);
    for (Instruction instruction : transaction.getInstructions()) {
      if (transaction.getState().equals("ready") && instruction.getAction().equals("write")) {
        if(output.isVerbose()) {
          output.addOutput("Transaction " + tID + " commits write " + instruction.getValue()
              + " to index " + instruction.getIndex() + " to site(s)");
        }
        for (Site site : sites) {
          Transaction[] locks = site.getWriteLockTable();
          if (site.getState().equals("failed") || 
              locks[instruction.getIndex()] == null || transaction.getID() != locks[instruction.getIndex()].getID()) {
            continue;
          }
          site.writeValueAtIndex(instruction.getIndex(), instruction.getValue());
        }
      }
    }
    for (int i = 1; i < 11; i++){
      Transaction[] writeLockTable = sites[i].getWriteLockTable();
      List<ArrayList<Transaction>> readLockTable = sites[i].getReadLockTable();
      for (int z = 1; z < 21; z++) {
        if (writeLockTable[z] != null && writeLockTable[z].getID() == tID) {
          writeLockTable[z] = null;
        }
        if (readLockTable.get(z).contains(transaction)) {
          readLockTable.get(z).remove(transaction);
        } 
      }
      sites[i].promoteWaitingTransactions();
    }
  }
  
  /**
   * Called when dump() instruction sent,
   * this method dumps each site's indexes and corresponding values.
   * Author: Darren
   */
  public void dump() {
    for (int i = 1; i < 11; i++) {
      dumpSite(i);
    }
  }
  
  /**
   * Called when dump(i) instruction sent,
   * this method dumps the given site's indexes and corresponding values.
   * Author: Darren
   * @param siteID the id of the site to dump.
   */
  public void dumpSite(int siteID) {
    Site site = sites[siteID];
    Integer[] values = site.getCommittedValues();
    output.addOutput("Site " + siteID);
    for (int i = 1; i < 21; i++) {
      if (i % 2 == 0 || 1 + i % 10 == siteID) {
        output.addOutput("Index: " + i + " Value: " + values[i]);
      }
    }
  }
  
  /**
   * Called when dump(s) instruction sent,
   * this method dumps the committed values at each site.
   * Author: Darren and Viswanath
   */
  public void shorterDump() {
    int numberOfIndexesChanged = 0;
    for (int i = 1; i < 21; i++) {
	  if (i % 2 == 0 ) {
	    Set<Integer> siteValues = new HashSet<Integer>();
	    for(int j=1;j<11;j++){
		  Site site = sites[j];
		  Integer[] values = site.getCommittedValues(); 
		  siteValues.add(values[i]);
		}
	    if (siteValues.size() > 1 || siteValues.iterator().next() != 10) {
	      if (siteValues.size() > 1) {
	        output.addOutput("X" + i + ": Either " + siteValues + " at all sites");
	      } else {
	        output.addOutput("X" + i + ": " + siteValues.iterator().next() + " at all sites");
	      }
	      numberOfIndexesChanged++;
	    }
	  } else {
	    int j = (i % 10) + 1;
	    Site site = sites[j]; 
	    Integer[] values = site.getCommittedValues();
	    if(values[i]!=10){
	      output.addOutput("X" + i + ": " + values[i]+ " at site " + j);
	      numberOfIndexesChanged++;
	    }
	  }
    }
    if (numberOfIndexesChanged < 20) {
      output.addOutput("All other variables have their initial values.");
    }
  }
  
  /**
   * Called when dump(xi) instruction sent,
   * this method dumps the given index's corresponding values at each site.
   * Author: Darren
   * @param index the index to dump
   */
  public void dumpIndex(int index) {
    output.addOutput("Index " + index);
    for (int i = 1; i < 11; i++) {
      if (index % 2 == 0 || 1 + index % 10 == i) {
        output.addOutput("Site: " + i + " Value: " + sites[i].getCommittedValueAtIndex(index));
      }
    }
  }
  
  /**
   * Prints each site's state, indexes and corresponding values
   * Author: Darren
   */
  public String toString() {
    String val = "";
    for (int i = 1; i < 11; i++) {
      val += sites[i];
    }
    return val;
  }
}
