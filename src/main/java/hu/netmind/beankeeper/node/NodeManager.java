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

package hu.netmind.beankeeper.node;

import hu.netmind.beankeeper.service.Service;
import java.util.List;

/**
 * This is the interface which provides communication services
 * to other nodes.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface NodeManager extends Service
{
   public enum NodeRole
   {
      CLIENT,  // Node is a client node in the network
      SERVER;  // Node is server node
   };

   public enum NodeState
   {
      OFFLINE(0),       // Manager created, but no identity yet
      UNINITIALIZED(1), // No identity, but it was tried
      INITIALIZED(2),   // Node has identity, but is not connected
      CONNECTED(4);     // Connected and ready to communicate

      private int level;

      private NodeState(int level)
      {
         this.level=level;
      }

      public int getLevel()
      {
         return level;
      }
   };
   
   /**
    * Get the node's unique id between nodes. This id may change during a
    * reconnect, but it is guaranteed to be unique among nodes at any given time.
    */
   Integer getId();

   /**
    * Get the server node's unique id. If this is a client node, it contains
    * the server node's id this is connected to, otherwise it's the same as
    * <code>getId()</code>.
    */
   Integer getServerId();

   /**
    * Get the node's role in the network.
    */
   NodeRole getRole();

   /**
    * Get the state of the node.
    */
   NodeState getState();

   /**
    * Make a potentially remote call to the given service, with given
    * parameters. This call will always go to the server
    * node for execution. If this is the server node, then
    * the service will be called locally.
    * @return The object that was returned from RPC.
    */
   Object callServer(String service, String method, Class[] parameterTypes,
         Object[] parameters);

   /**
    * Call this method on all nodes, including where the call originated.
    * Note, broadcast calls do not have return value. It is guaranteed however,
    * that any given node either received the call, or fell off the node
    * network before this call returns. It is not guaranteed however, 
    * that all calls were successful in their respective nodes.
    */
   void callAll(String service, String method, Class[] parameterTypes,
         Object[] parameters);

}


