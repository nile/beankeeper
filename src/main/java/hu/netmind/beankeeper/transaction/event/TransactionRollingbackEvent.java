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
 * This event is sent when the transaction given is about to roll back.
 * The transaction is usable, but all operations will be rolled back anyway.
 * Exceptions thrown in the handler of this event will not cause the 
 * rollback process to abort, so all handlers a guaranteed to be invoked,
 * and the rollback is guaranteed to occur.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class TransactionRollingbackEvent extends TransactionEvent
{
   public TransactionRollingbackEvent(Transaction transaction)
   {
      super(transaction);
   }
}


