/**
 * Copyright (C) 2007 NetMind Consulting Bt.
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

package hu.netmind.beankeeper.transaction.event;

import hu.netmind.beankeeper.transaction.Transaction;

/**
 * This event notifies the listener that the end serial is
 * choosen for the transaction. It is delivered <i>after</i> the listeners 
 * for the committing event are notified, but before the commit physically
 * occurs. Because every operation in a single transaction will appear
 * to have occured at a precise atomic moment (serial) in time, a specific
 * serial must be choosen. When this event is delivered, the
 * <code>getEndSerial()</code> in the transaction will contain the
 * serial under which this transaction will be visible.
 * <strong>Note:</strong> Do not do any store operations during handling
 * of this event, because the commit lock is engaged when this event
 * is delivered. It is likely an operation will cause a deadlock.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class TransactionCommitEndingEvent extends TransactionEvent
{
   public TransactionCommitEndingEvent(Transaction transaction)
   {
      super(transaction);
   }
}


