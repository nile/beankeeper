/**
 * Copyright (C) 2006 NetMind Consulting Bt.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package hu.netmind.beankeeper.transaction.impl;

import java.util.*;
import java.sql.Connection;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.transaction.*;
import hu.netmind.beankeeper.transaction.event.*;
import hu.netmind.beankeeper.node.NodeManager;
import hu.netmind.beankeeper.serial.SerialTracker;
import hu.netmind.beankeeper.db.Database;
import hu.netmind.beankeeper.operation.OperationTracker;
import hu.netmind.beankeeper.event.EventDispatcher;

/**
 * This class keeps track of all transaction objects allocated.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class TransactionTrackerImpl implements TransactionTracker
{
   private static Logger logger = Logger.getLogger(TransactionTrackerImpl.class);
   
   private ThreadLocal transactions;
   private LinkedList allTransactions;
   private HashSet allTransactionIds;

   private SerialTracker serialTracker = null; // Injected
   private NodeManager nodeManager = null; // Injected
   private Database database = null; // Injected
   private OperationTracker operationTracker = null; // Injected
   private EventDispatcher eventDispatcher = null; // Injected

   public void init(Map parameters)
   {
      transactions = new ThreadLocal();
      allTransactions = new LinkedList();
      allTransactionIds = new HashSet();
   }

   public void release()
   {
   }

   public boolean hasTransaction(Long serial)
   {
      synchronized ( allTransactions )
      {
         return allTransactionIds.contains(serial);
      }
   }

   /**
    * Get a transaction. Following modes are supported:
    * <ul>
    *    <li>TX_REQUIRED: A new transaction is allocated if no current
    *    transaction exists, otherwise the current transaction is returned.</li>
    *    <li>TX_NEW: A new transaction is allocated either way.</li>
    *    <li>TX_OPTIONAL: If there is a current transaction, that is returned,
    *    null otherwise.</li>
    * </ul>
    * Note, that each transaction can support multiple levels of begin-commit
    * blocks. Each transaction only commits/rollsback is the most outer
    * block is commited/rolled back.
    */
   public Transaction getTransaction(int mode)
   {
      LinkedList list = (LinkedList) transactions.get();
      if ( list == null )
      {
         // No list yet, initialize threadlocal
         list = new LinkedList();
         transactions.set(list);
      }
      if ( (list.size()==0) && (mode==TX_OPTIONAL) )
         return null;
      if ( (list.size() == 0) || (mode==TX_NEW) )
      {
         // No transaction, or new is explicitly required
         Long txSerial = serialTracker.getNextSerial();
         TransactionImpl transaction = new TransactionImpl(txSerial,
               nodeManager.getServerId());
         if ( logger.isDebugEnabled() )
            logger.debug("transaction created: "+transaction);
         if ( logger.isDebugEnabled() )
            logger.debug("transaction allocation trace: "+getStackTrace(transaction.getAllocateTrace()));
         transaction.setConnection(database.getConnectionSource().getConnection());
         list.add(transaction);
         synchronized ( allTransactions )
         {
            allTransactions.add(transaction);
            allTransactionIds.add(transaction.getSerial());
         }
      }
      return (Transaction) list.getLast();
   }

   /**
    * Commit a transaction. This is called from a Transaction object.
    * @throws Exception If commit was unsuccessful.
    */
   private void commitInternal(TransactionImpl transaction)
      throws Exception
   {
      if ( logger.isDebugEnabled() )
         logger.debug("commiting transaction: "+transaction);
      // Send notifications of commit, if there was an
      // exception, then skip to rollback
      eventDispatcher.notify(new TransactionCommittingEvent(transaction));
      // We need a serial for the transaction to end. This serial
      // will denote the commit itself. The beginning of a commit
      // must be marked, because while the commit is running, no
      // query can execute which has a higher serial, because this
      // would mean the query will change once this commit finishes.
      Long endSerial = operationTracker.startCommit(
               nodeManager.getId());
      transaction.setEndSerial(endSerial);
      try
      {
         // Check whether we are on the same server still as when the transaction begun.
         // This is important for the transaction to be synchronized with other nodes.
         int currentServerId = nodeManager.getServerId();
         int transactionServerId = transaction.getServerId();
         if ( currentServerId != transactionServerId )
            throw new StoreException("there was a reconnect during the transaction, rolling back to preserve consistency, current server id: "+
                  currentServerId+" vs. "+transactionServerId);
         // Notify listeners, that the end serial is ready. If
         // error occurs in this, rollback is invoked.
         eventDispatcher.notify(new TransactionCommitEndingEvent(transaction));
         // Commit physically. Note if this fails, rollback is still invoked,
         // which will close the transaction
         transaction.getConnection().commit();
         removeTransaction(transaction);
      } finally {
         // Whatever happens, disengage the commit semaphore. An error in this
         // should not cause the commit to fail, so we catch the exception here.
         try
         {
            operationTracker.endCommit(
                  nodeManager.getId(),transaction.getEndSerial(),transaction.getSerial());
            // Give warning if we're on different server now, this could mean,
            // although not probable, that queries on the new server became inconsistent
            if ( nodeManager.getServerId() != transaction.getServerId() )
               logger.warn("commit ended on different server than it begun, check for possible database inconsitencies");
         } catch ( Exception e ) {
            logger.warn("commit lock could not be disengaged, check database for possible inconsitencies",e);
         }
      }
      // Send local notifications
      eventDispatcher.notifyAll(new TransactionCommittedEvent(transaction));
      // Send remote notifications
      nodeManager.callAll(TransactionTracker.class.getName(),"notifyTransactionCommitted",
            new Class[] { Long.class }, new Object[] { transaction.getSerial() });
   }

   public void notifyTransactionCommitted(Long txSerial)
   {
      eventDispatcher.notifyAll(new RemoteTransactionPostCommitEvent(
               nodeManager.getId(),txSerial));
   }

   /**
    * Roll back a transaction. This is called from the Transaction object.
    * Note, that this method will never fail.
    */
   private void rollbackInternal(TransactionImpl transaction)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("rolling back transaction: "+transaction);
      // Send notifications of rollback, disregard exceptions
      eventDispatcher.notifyAll(new TransactionRollingbackEvent(transaction));
      // Physical rollback
      Connection connection = transaction.getConnection();
      try
      {
         connection.rollback();
      } catch ( Exception e ) {
         throw new StoreException("could not roll back transaction",e);
      } finally {
         // Remove transaction from queue
         removeTransaction(transaction);
      }
      // Send notifications of rollback, disregard exceptions
      eventDispatcher.notifyAll(new TransactionRolledbackEvent(transaction));
   }

   /**
    * Remove transaction from queue. Also, if this transaction is not
    * toplevel, then throw exception.
    */
   private void removeTransaction(TransactionImpl transaction)
   {
      // Remove from threadlocal list
      LinkedList list = (LinkedList) transactions.get();
      if ( (list == null) || (list.size()==0) )
         throw new StoreException("no transactions present, and tried to use one.");
      if ( ! list.getLast().equals(transaction) )
      {
         TransactionImpl top = (TransactionImpl) list.getLast();
         logger.warn("possible transaction leak, tried to commit/rollback transaction, which was not the currenct transaction, investigate! Top was: "+
               top+", but tried to close: "+transaction+". "+
               "Now follows the allocation trace of top and the transaction to be closed: \n"+
               getStackTrace(top.getAllocateTrace())+"\n"+getStackTrace(transaction.getAllocateTrace()));
         throw new StoreException("tried to commit/rollback a transaction which is not the current transaction, possible transaction leak!");
      }
      // Give back connection
      database.getConnectionSource().releaseConnection(transaction.getConnection());
      transaction.setConnection(null);
      // Remove from list
      list.removeLast();
      // Remove from global list
      synchronized ( allTransactions )
      {
         allTransactions.remove(transaction);
         allTransactionIds.remove(transaction.getSerial());
      }
   }

   private String getStackTrace(Exception e)
   {
      StringWriter trace = new StringWriter();
      PrintWriter writer = new PrintWriter(trace);
      e.printStackTrace(writer);
      return trace.toString();
   }

   /**
    * This is a high-level transaction object used throughout the persistence
    * layer. Also, it can store attributes, and register listeners.
    */
   private class TransactionImpl extends HashMap implements Transaction
   {
      private Logger statsLogger = Logger.getLogger(Transaction.class.getName()+".stats");

      private Connection connection;
      private boolean rollbackOnly = false;
      private int depth = 0;
      private Long serial;
      private Long endSerial;
      private Integer serverId;
      
      // Statistical attributes
      private TransactionStatistics stats;
      private Exception allocateTrace = null;

      public TransactionImpl(Long serial, Integer serverId)
      {
         super();
         this.serial=serial;
         this.serverId=serverId;
         stats=new TransactionStatistics();
         allocateTrace = new Exception("trace");
      }

      private Integer getServerId()
      {
         return serverId;
      }

      private Exception getAllocateTrace()
      {
         return allocateTrace;
      }

      public int getTransactionIsolation()
      {
         try
         {
            return connection.getTransactionIsolation();
         } catch ( Exception e ) {
            throw new StoreException("unable to determine transaction isolation level.");
         }
      }
        
      public void setTransactionIsolation(int level)
      {
         try
         {
            connection.setTransactionIsolation(level);
         } catch ( Exception e ) {
            throw new StoreException("cannot set transaction isolation level: "+level,e);
         }
      }

      public Connection getConnection()
      {
         return connection;
      }
      private void setConnection(Connection connection)
      {
         this.connection=connection;
      }

      /**
       * Start the transaction. A transaction must be always start with
       * the call to <code>begin()</code>. All subsequent calls to this method
       * will increase the transaction depth, and to commit the transaction
       * exactly that many <code>commit()</code> and <code>rollback()</code>
       * calls must occur.
       */
      public void begin()
      {
         // Increase depth
         logger.debug("transaction begins at depth: "+depth);
         depth++;
      }
      
      /**
       * Commit a transaction.
       */
      public void commit()
      {
         if ( logger.isDebugEnabled() )
            logger.debug("transaction "+this+" commits at depth: "+depth);
         if ( statsLogger.isDebugEnabled() )
            statsLogger.debug("transaction stats in commit: "+stats);
         depth--;
         if ( depth > 0 )
            return;
         if ( rollbackOnly )
         {
            rollbackInternal(this);
         } else {
            try
            {
               commitInternal(this);
            } catch ( Exception e ) {
               logger.error("commit threw exception, rolling back");
               rollbackInternal(this);
               if ( e instanceof StoreException )
                  throw (StoreException) e;
               else
                  throw new StoreException("rolling back in commit because of exception",e);
            }
         }
      }

      /**
       * Rollback this transaction.
       */
      public void rollback()
      {
         if ( logger.isDebugEnabled() )
            logger.debug("transaction "+this+" rolls back at depth: "+depth);
         if ( statsLogger.isDebugEnabled() )
            statsLogger.debug("transaction stats in rollback: "+stats);
         depth--;
         if ( depth > 0 )
         {
            markRollbackOnly();
            return;
         }
         rollbackInternal(this);
      }

      /**
       * Mark this transaction as rollback only. This means the next call
       * to either commit or rollback will rollback either way.
       */
      public void markRollbackOnly()
      {
         rollbackOnly=true;
      }

      /**
       * Returns whether this transaction was marked "rollback only".
       */
      public boolean isRollbackOnly()
      {
         return rollbackOnly;
      }

      /**
       * Get the serial of this transaction.
       */
      public Long getSerial()
      {
         return serial;
      }

      public TransactionStatistics getStats()
      {
         return stats;
      }

      public String toString()
      {
         return "[Tx: "+serial+" ("+serverId+")]";
      }

      public Long getEndSerial()
      {
         return endSerial;
      }
      private void setEndSerial(Long endSerial)
      {
         this.endSerial=endSerial;
      }

      public int hashCode()
      {
         return serial.intValue();
      }

      public boolean equals(Object o)
      {
         if ( (o==null) || (!(o instanceof TransactionImpl)) )
            return false;
         return serial.equals(((TransactionImpl)o).serial);
      }
   }

}


