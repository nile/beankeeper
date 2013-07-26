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
import java.sql.SQLException;
import org.apache.log4j.Logger;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.transaction.*;
import hu.netmind.beankeeper.db.Database;

/**
 * This class supplies the services with lightweight transactions.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class InternalTransactionTrackerImpl implements InternalTransactionTracker
{
   private static Logger logger = Logger.getLogger(InternalTransactionTrackerImpl.class);
   
   private Database database = null; // Injected

   public void init(Map parameters)
   {
   }

   public void release()
   {
   }

   /**
    * Get a lightweight transaction.
    */
   public Transaction getTransaction()
   {
      return new TransactionImpl(database.getConnectionSource().getConnection());
   }

   public class TransactionImpl extends HashMap implements Transaction
   {
      private Connection connection = null;
      private boolean rollbackOnly = false;
      private TransactionStatistics stats = new TransactionStatistics();
      private int depth = 0;

      public TransactionImpl(Connection connection)
      {
         this.connection=connection;
      }

      /**
       * Begin the transaction, but for safety don't support embedding transactions.
       */
      public void begin()
      {
         if ( depth != 0 )
            throw new StoreException("direct transaction does not support embedded blocks, transaction already begun");
         depth++;
      }

      /**
       * Commit a transaction.
       */
      public void commit()
      {
         try
         {
            if ( isRollbackOnly() )
               connection.rollback();
            else
               connection.commit();
         } catch ( SQLException e ) {
            throw new StoreException("exception while commit",e);
         } finally {
            database.getConnectionSource().releaseConnection(connection);
         }
      }

      /**
       * Rollback this transaction.
       */
      public void rollback()
      {
         try
         {
            connection.rollback();
         } catch ( SQLException e ) {
            throw new StoreException("exception while rollback",e);
         } finally {
            database.getConnectionSource().releaseConnection(connection);
         }
      }

      /**
       * Mark this transaction as rollback only.
       */
      public void markRollbackOnly()
      {
         rollbackOnly = true;
      }

      /**
       * Returns whether this transaction was marked "rollback only".
       */
      public boolean isRollbackOnly()
      {
         return rollbackOnly;
      }

      /**
       * No used.
       * @return Always null.
       */
      public Long getSerial()
      {
         return null;
      }

      /**
       * Not used.
       * @return Always null.
       */
      public Long getEndSerial()
      {
         return null;
      }

      /**
       * Get statistics from this transaction.
       */
      public TransactionStatistics getStats()
      {
         return stats;
      }

      /**
       * Get the connection to the database from this transaction.
       */
      public Connection getConnection()
      {
         return connection;
      }
   }


}


