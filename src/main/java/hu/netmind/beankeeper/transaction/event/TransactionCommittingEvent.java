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
 * This event is sent when the transaction given is about to commit.
 * Exceptions thrown in this handler will cause the commit process to
 * abort, and start the rollback process, so it is not guaranteed that
 * after sending out this event, the transaction will commit physically.
 * The transaction inside this event can be used to perform additional
 * operations.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class TransactionCommittingEvent extends TransactionEvent
{
   public TransactionCommittingEvent(Transaction transaction)
   {
      super(transaction);
   }
}


