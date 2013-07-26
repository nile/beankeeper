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
import hu.netmind.beankeeper.node.NodeManager.NodeRole;
import hu.netmind.beankeeper.node.NodeManager.NodeState;
import hu.netmind.beankeeper.node.event.RemoteStateChangeEvent;
import hu.netmind.beankeeper.event.EventDispatcher;
import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * This class is the implementation of the node's server component. It has a dual purpose,
 * wait for clients to connect and talk with clients, but this is only activated if this
 * node becomes the server node. If it's a client node, this implementation only allocates
 * the server port, but turns down clients who try to connect.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class NodeServer implements Runnable
{
   private static Logger logger = Logger.getLogger(NodeServer.class);
   private ServerSocket serverSocket;
   private ArrayList clientHandlers = new ArrayList();
   private boolean running = true;
   private int nodeIndex = 0;

   private NodeManagerImpl nodeManager = null;
   private EventDispatcher eventDispatcher = null;

   public NodeServer(NodeManagerImpl nodeManager, EventDispatcher eventDispatcher, int nodeIndex)
   {
      this.nodeManager=nodeManager;
      this.eventDispatcher=eventDispatcher;
      this.nodeIndex=nodeIndex;
   }

   public ServerSocket getServerSocket()
   {
      return serverSocket;
   }
   private void setServerSocket(ServerSocket serverSocket)
   {
      this.serverSocket=serverSocket;
   }

   /**
    * Broadcast a message to all connected clients. Messages to clients
    * do not have answers.
    */
   public void broadcast(CommObject obj)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("broadcasting message: "+obj);
      // Safe-copy all clients
      LinkedList<ClientHandler> handlers = null;
      synchronized ( clientHandlers )
      {
         handlers = new LinkedList<ClientHandler>(clientHandlers);
      }
      // Broadcast message. First, all requests are sent in batch,
      // and then all responses are waited for. This way each node
      // can work on the message in parallel.
      Iterator<ClientHandler> handlerIterator = handlers.iterator();
      while ( handlerIterator.hasNext() )
      {
         ClientHandler handler = handlerIterator.next();
         try
         {
            handler.send(obj);
         } catch ( Exception e ) {
            handlerIterator.remove();
            logger.warn("could not deliver message '"+obj+"' to client.",e);
         }
      }
      // So now watch for client to receive responses to those requests
      for ( ClientHandler handler : handlers )
      {
         if ( logger.isDebugEnabled() )
            logger.debug("waiting for broadcast response from: "+handler);
         handler.waitForResponse(obj);
      }
   }

   /**
    * Setup server and bind to a random port.
    */
   public void bind()
   {
      try
      {
         // Setup channel
         serverSocket = new ServerSocket(0); // Choose an ip:port
         // Start listening
         Thread listenerThread = new Thread(this);
         listenerThread.setName("BeanKeeper Accept (Node: "+nodeIndex+")");
         listenerThread.setDaemon(true);
         listenerThread.start();
      } catch ( Exception e ) {
         throw new StoreException("exception while binding to server port",e);
      }
   }

   /**
    * Disconnect the server.
    */
   public void close()
   {
      // Deactivate server
      List<ClientHandler> handlers = null;
      synchronized ( clientHandlers )
      {
         handlers = new ArrayList<ClientHandler>(clientHandlers);
      }
      for ( ClientHandler handler : handlers )
      {
         if ( logger.isDebugEnabled() )
            logger.debug("closing server node handler: "+handler);
         try
         {
            handler.close();
         } catch ( Exception e ) {
            logger.debug("couldn't close handler "+handler+", continuing");
         }
      }
      // Set to not running
      running = false;
      // Close server socket
      try
      {
         serverSocket.close();
      } catch ( Exception e ) {
         logger.error("error while disconnecting server socket",e);
      }
   }

   /**
    * Run listener thread. This thread accepts incoming connections,
    * and incoming socket data.
    */
   public void run()
   {
      try
      {
         while ( running )
         {
            // Listen for socket
            Socket socket = serverSocket.accept();
            logger.debug("server received connect from client...");
            // Create client handler
            try
            {
               ClientHandler handler = new ClientHandler(socket);
               synchronized ( clientHandlers )
               {
                  clientHandlers.add(handler);
               }
            } catch ( StoreException e ) {
               // Probably just testing connection, don't worry
               logger.debug("handler couldn't be created because of: "+e.getMessage());
            }
         }
         logger.debug("node server normal shutdown");
      } catch ( Exception e ) {
         if ( running )
            logger.warn("server socket threw error",e);
         else
            logger.debug("server socket was shutdown: "+e.getMessage());
      } finally {
         // Node manager should return to uninitialized
         nodeManager.ensureState(NodeState.UNINITIALIZED);
      }
   }

   /**
    * Handle socket communications.
    */
   public class ClientHandler extends NetEndpoint
   {
      private int index;

      public ClientHandler(Socket socket)
      {
         super(socket,"BeanKeeper Client Handler (Node: "+nodeIndex+")");
      }

      public String toString()
      {
         return "[Handler for node: "+index+"]";
      }

      /**
       * Override close to send events and to deregister from node server.
       */
      public void close()
      {
         super.close();
         // Remove from socket handlers
         synchronized ( clientHandlers )
         {
            clientHandlers.remove(this);
         }
         // Send disconnect event
         try
         {
            if ( index != 0 ) // If a real client was on
               eventDispatcher.notifyAll(
                     new RemoteStateChangeEvent(index));
         } catch ( StoreException e ) {
            // Service was no longer available, no problem,
            // don't have to handle the event then anyway
            logger.debug("could not send out disconnect event, probably already shut down",e);
         }
      }

      /**
       * Handle objects from clients.
       */
      public void onIncoming(CommObject obj)
      {
         try
         {
            // Initialization message from a client
            if ( obj instanceof InitMessage )
            {
               index = ((InitMessage) obj).getNodeId();
               // If not active, maybe the client node detected earlier
               // that the current server node is out, so check with
               // a re-connect. If the server node did not die, we lose
               // our cache and current transactions! But if the server
               // node really died, we reconnect and accept the client's
               // init request immediately.
               if ( nodeManager.getRole() == NodeRole.CLIENT )
               {
                  logger.info("server got init request from client: "+index+
                        " but was not yet activated, try to reconnect to server to check, whether it's available");
                  nodeManager.ensureState(NodeState.INITIALIZED);
                  nodeManager.ensureState(NodeState.CONNECTED);
               }
               // If active, accept init request
               CommResponse response = null;
               if ( nodeManager.getRole() == NodeRole.CLIENT )
                  response = new CommResponse(obj,CommResponse.SERVER_INACTIVE); // Server not active
               else
                  response = new CommResponse(obj,CommResponse.ACTION_SUCCESS); // Ok
               send(response);
            }
            // Message to call some method
            if ( obj instanceof CallMessage )
            {
               CallMessage msg = (CallMessage) obj;
               CommResponse response = null;
               if ( msg.isBroadcast() )
               {
                  // This is a broadcast and we're the server, so
                  // broadcast the signal, and return ok
                  nodeManager.callAll(msg.getService(),msg.getMethod(),
                        msg.getParameterTypes(),msg.getParameters());
                  response = new CallResponse(obj,null,null);
               } else {
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
               }
               send(response);
            }
         } catch ( StoreException e ) {
            logger.warn("message request was not successful.",e);
            send(new CallResponse(obj,null,e));
         }
      }
   }
   
}


