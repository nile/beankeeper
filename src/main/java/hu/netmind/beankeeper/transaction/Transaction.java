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

package hu.netmind.beankeeper.transaction;

import java.util.Map;
import java.sql.Connection;
import org.apache.log4j.Logger;
import java.io.Serializable;

/**
 * This is a high-level transaction object used throughout the persistence
 * layer. Also, it can store attributes, which can be used to communicate
 * data through calling layers.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface Transaction extends Map
{
   /**
    * Start the transaction. A transaction must be always start with
    * the call to <code>begin()</code>. All subsequent calls to this method
    * will increase the transaction depth, and to commit the transaction
    * exactly that many <code>commit()</code> and <code>rollback()</code>
    * calls must occur.
    */
   void begin();
   
   /**
    * Commit a transaction.
    */
   void commit();

   /**
    * Rollback this transaction.
    */
   void rollback();

   /**
    * Mark this transaction as rollback only. This means the next call
    * to either commit or rollback will rollback either way.
    */
   void markRollbackOnly();

   /**
    * Returns whether this transaction was marked "rollback only".
    */
   boolean isRollbackOnly();

   /**
    * Get the serial number when this transaction began.
    */
   Long getSerial();

   /**
    * Get the serial when this transaction ended. This is also the
    * unique moment in time when all changes became visible.
    * @return The end serial. This may be null, if the end
    * of transaction is not yet known.
    */
   Long getEndSerial();

   /**
    * Get statistics from this transaction.
    */
   TransactionStatistics getStats();

   /**
    * Get the connection to the database from this transaction.
    */
   Connection getConnection();
}


