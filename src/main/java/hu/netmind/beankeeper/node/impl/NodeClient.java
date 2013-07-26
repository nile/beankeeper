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

package hu.netmind.beankeeper.node.impl;

import hu.netmind.beankeeper.common.StoreException;
import java.net.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * A node service provider implmenetation, which uses a connection
 * to a server node to delegate functionality.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class NodeClient extends NetEndpoint
{
   private static Logger logger = Logger.getLogger(NodeClient.class);
   private NodeManagerImpl nodeManager = null;
   
   public NodeClient(NodeManagerImpl nodeManager, Socket socket, int index, int serverIndex)
   {
      super(socket,"BeanKeeper Client (Node: "+index+" -> "+serverIndex+")");
      this.nodeManager=nodeManager;
   }

   /**
    * If a network error occurs, just disconnect node manager, which will eventually
    * call this object's close too.
    */
   public void onError()
   {
      logger.debug("handling communication error by disconnecting client");
      try
      {
         nodeManager.ensureDisconnect(this);
      } catch ( Exception e ) {
         logger.warn("could not ensure node manager is disconnected because of",e);
      }
   }

   /**
    * The only message a client is supposed to get is a broadcast call
    * message, so handle that.
    */
   public void onIncoming(CommObject obj)
   {
      if ( ! (obj instanceof CallMessage) )
         return;
      CallMessage msg = (CallMessage) obj;
      CallResponse response = null;
      // Do the call
      try
      {
         // Execute the call locally
         Object result = nodeManager.callLocal(msg.getService(),msg.getMethod(),
               msg.getParameterTypes(),msg.getParameters());
         response = new CallResponse(obj,result,null);
      } catch ( StoreException e ) {
         logger.warn("called operation yielded: ",e);
         response = new CallResponse(obj,null,e);
      }
      // Send the result back to server
      send(response);
   }
}


