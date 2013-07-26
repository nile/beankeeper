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

package hu.netmind.beankeeper.node.event;

import hu.netmind.beankeeper.node.NodeManager.NodeState;
import hu.netmind.beankeeper.event.PersistenceEvent;

/**
 * This event is generated if the state of current node has changed.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class NodeStateChangeEvent implements PersistenceEvent
{
   private NodeState oldState;
   private NodeState newState;
   private int nodeId = 0;

   public NodeStateChangeEvent(int nodeId, NodeState oldState, NodeState newState)
   {
      this.nodeId=nodeId;
      this.oldState=oldState;
      this.newState=newState;
   }

   public int getNodeId()
   {
      return nodeId;
   }

   public NodeState getOldState()
   {
      return oldState;
   }

   public NodeState getNewState()
   {
      return newState;
   }
}


