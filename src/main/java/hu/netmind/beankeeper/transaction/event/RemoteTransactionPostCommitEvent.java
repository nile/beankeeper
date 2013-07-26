/**
 * Copyright (C) 2009 NetMind Consulting Bt.
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

import hu.netmind.beankeeper.event.PersistenceEvent;

/**
 * This event is sent when a transaction is committed. Note: this
 * event is delivered on every node for every transaction.
 * @author Robert Brautigam
 * @version CVS Revision: $Revision$
 */
public class RemoteTransactionPostCommitEvent implements PersistenceEvent
{
   private int nodeId;
   private Long txSerial;

   public RemoteTransactionPostCommitEvent(int nodeId, Long txSerial)
   {
      this.nodeId=nodeId;
      this.txSerial=txSerial;
   }

   public int getNodeId()
   {
      return nodeId;
   }

   public Long getTxSerial()
   {
      return txSerial;
   }
}


