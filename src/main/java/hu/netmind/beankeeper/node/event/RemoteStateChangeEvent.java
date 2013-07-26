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

package hu.netmind.beankeeper.node.event;

import hu.netmind.beankeeper.service.StoreContext;
import hu.netmind.beankeeper.event.PersistenceEvent;

/**
 * This event is delivered if a remote node changes it's state. This event is only
 * delivered on the server node, and means that the indicated node changed it's state
 * out of 'connected' state into a lower unknown state. In short it means the
 * client disconnected.
 * @author Robert Brautigam
 * @version CVS Revision: $Revision$
 */
public class RemoteStateChangeEvent implements PersistenceEvent
{
   private int nodeId = 0;

   public RemoteStateChangeEvent(int nodeId)
   {
      this.nodeId=nodeId;
   }
   
   public int getNodeId()
   {
      return nodeId;
   }
}


