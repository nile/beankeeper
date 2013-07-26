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

import hu.netmind.beankeeper.service.Service;
import hu.netmind.beankeeper.service.StoreContext;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.config.ExtendedConfigurationListener;
import hu.netmind.beankeeper.config.ConfigurationTracker;
import hu.netmind.beankeeper.node.NodeManager;
import hu.netmind.beankeeper.node.event.NodeStateChangeEvent;
import hu.netmind.beankeeper.node.NodeManager.NodeState;
import hu.netmind.beankeeper.node.NodeManager.NodeRole;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.InternalTransactionTracker;
import hu.netmind.beankeeper.db.SearchResult;
import hu.netmind.beankeeper.db.Database;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.apache.log4j.Logger;
import org.apache.commons.configuration.event.ConfigurationEvent;

/**
 * This manager enables the Store to function on a peer-to-peer
 * fashion with other Store instances which are pointed to the same
 * database. The class takes care of all IP communication related
 * work, such as reconnecting, communication protocoll, etc. All
 * synchronization points must occur through this manager, which guarantees
 * synchronization across all other Store instances.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class NodeManagerImpl implements NodeManager, ExtendedConfigurationListener
{
   private static Logger logger = Logger.getLogger(NodeManagerImpl.class);

   public static int CLIENT_RECONNECT_TRIES = 2;
   private static int SOCKET_CONNECT_TIMEOUT = 3000;

   private int index = 0;
   private int serverIndex;
   private String ips;
   private NodeServer server;
   private NodeClient client;

   private boolean running = true;
   private NodeState state = NodeState.OFFLINE;
   private Object stateMutex = new Object();

   private ConfigurationTracker configurationTracker = null; // Injected
   private EventDispatcher eventDispatcher = null; // Injected
   private Database database = null; // Injected
   private StoreContext context = null; // Injected
   private InternalTransactionTracker transactionTracker = null; // Injected

   /**
    * Construct node manager, establish identity, and make
    * initial connection.
    */
   public void init(Map parameters)
   {
      // Configure
      configurationReload();
      configurationTracker.addListener(this);
      // Initialize node, so we have a node id
      ensureState(NodeState.INITIALIZED);
   }

   public void release()
   {
      logger.debug("closing node manager.");
      // Release all resources of state
      try
      {
         ensureState(NodeState.UNINITIALIZED);
      } catch ( Exception e ) {
         logger.error("error while shutting down node manager",e);
      }
      configurationTracker.removeListener(this);
   }

   public NodeState getState()
   {
      return state;
   }

   public Integer getServerId()
   {
      ensureState(NodeState.CONNECTED);
      if ( getState().getLevel() < NodeState.CONNECTED.getLevel() )
         throw new StoreException("node not yet connected, can not determine server id");
      return serverIndex;
   }

   public Integer getId()
   {
      if ( getState().getLevel() < NodeState.INITIALIZED.getLevel() )
         throw new StoreException("node not yet initialized, identity is not known");
      return index;
   }

   public NodeRole getRole()
   {
      ensureState(NodeState.CONNECTED);
      if ( getState().getLevel() < NodeState.CONNECTED.getLevel() )
         throw new StoreException("node not yet connected, can not determine role");
      if ( client == null )
         return NodeRole.SERVER;
      else
         return NodeRole.CLIENT;
   }

   /**
    * Get the server addresses from interfaces.
    */
   public static String getHostAddresses()
   {
      try
      {
         Enumeration interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
         // Copy from enumeration to addresses vector, but filter loopback addresses
         ArrayList addresses = new ArrayList();
         while ( interfaceEnumeration.hasMoreElements() )
         {
            NetworkInterface intf = (NetworkInterface) interfaceEnumeration.nextElement();
            // Remove loopback addresses
            Enumeration addressEnumeration = intf.getInetAddresses();
            while ( addressEnumeration.hasMoreElements() )
            {
               InetAddress address = (InetAddress) addressEnumeration.nextElement();
               // Insert to addresses only if not loopback and not link local
               if ( (! address.isLoopbackAddress()) && (! address.isLinkLocalAddress()) )
                  addresses.add(address);
            }
         }
         // Pick one address from the remaining address space
         logger.debug("server available local addresses: "+addresses);
         // Now, multiple addresses are in the list, so copy all of them
         // into the result string.
         StringBuffer ips = new StringBuffer();
         for ( int i=0; i<addresses.size(); i++ )
         {
            InetAddress address = (InetAddress) addresses.get(i);
            if ( ips.length() > 0 )
               ips.append(",");
            ips.append(address.getHostAddress());
         }
         return ips.toString();
      } catch ( StoreException e ) {
         throw e;
      } catch ( Exception e ) {
         throw new StoreException("exception while determining server address",e);
      }
   }

   /**
    * Determine if an address is available.
    */
   public static boolean isAlive(String ips, int port)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("trying to reach: "+ips+":"+port);
      try
      {
         if ( "".equals(ips) )
            ips = InetAddress.getLocalHost().getHostAddress();
      } catch ( Exception e ) {
         throw new StoreException("can not determine local adapter, but there is another node, which would need to be contacted.",e);
      }
      StringTokenizer tokens = new StringTokenizer(ips,",");
      while ( tokens.hasMoreTokens() )
      {
         String ip = tokens.nextToken();
         if ( logger.isDebugEnabled() )
            logger.debug("determining whether '"+ip+":"+port+"' is alive...");
         try
         {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip,port),SOCKET_CONNECT_TIMEOUT);
            socket.close();
            return true; // Success, so return true
         } catch ( Exception e ) {
            logger.debug("unreachable node at '"+ip+":"+port+", "+e.getMessage());
         }
      }
      logger.debug("could not reach any of the ips given for node");
      return false;
   }

   /**
    * Make the reflection call locally, for the local services.
    */
   public Object callLocal(String service, String method, Class[] parameterTypes,
         Object[] parameters)
   {
      ensureState(NodeState.CONNECTED);
      // Get the service, and with the help of relfection, call the named
      // method
      Service serviceObject = context.getService(service);
      try
      {
         Method methodObject = serviceObject.getClass().getMethod(method, parameterTypes);
         return methodObject.invoke(serviceObject,parameters);
      } catch ( InvocationTargetException e ) {
         if ( e.getCause() instanceof StoreException )
            throw (StoreException) e.getCause();
         throw new StoreException("received exception while invoking '"+method+"', of service: "+service,e.getCause());
      } catch ( StoreException e ) {
         throw e;
      } catch ( Exception e ) {
         throw new StoreException("could not invoke method '"+method+"', of service: "+service,e);
      }
   }

   /**
    * Make a potentially remote call to the given service, with given
    * parameters. This call will always go to the server
    * node for execution. If this is the server node, then
    * the service will be called locally. 
    * If this node is a client, and the communication fails
    * to the server, the communication is retried once.
    * @return The object that was returned from RPC.
    */
   public Object callServer(String service, String method, Class[] parameterTypes,
         Object[] parameters)
   {
      for ( int tries = 0; tries<CLIENT_RECONNECT_TRIES; tries++ )
      {
         try
         {
            if ( getRole() == NodeRole.SERVER )
            {
               // This node is the server, then call the method locally
               return callLocal(service,method,parameterTypes,parameters);
            } else {
               // This is a client node, so we must pass the call to server
               CallResponse response = (CallResponse) client.sendAndWaitForResponse(new CallMessage(
                        service,method,parameterTypes,parameters,false));
               if ( response.getException() != null )
                  throw response.getException();
               return response.getReturnValue();
            }
         } catch ( CommException e ) {
            // If all retries are exhausted, just throw the exception
            if ( tries+1 >= CLIENT_RECONNECT_TRIES )
               throw e;
            logger.debug("communication error, retrying client call");
         }
      }
      // Code shouldn't reach this
      return null;
   }

   /**
    * Call this method on all nodes, including where the call originated.
    * Note, broadcast calls do not have return value. It is guaranteed however,
    * that any given node either received the call, or fell off the node
    * network. It is not guaranteed however, that all calls were successful
    * in their respective nodes.
    */
   public void callAll(String service, String method, Class[] parameterTypes,
         Object[] parameters)
   {
      if ( getRole() == NodeRole.SERVER )
      {
         // We are the server, broadcast to all
         server.broadcast(new CallMessage(service, method, parameterTypes,
                  parameters,false));
         // Now call locally
         try
         {
            callLocal(service,method,parameterTypes,parameters);
         } catch ( StoreException e ) {
            logger.warn("local call after broadcast call failed for service method: "+service+"."+method,e);
         }
      } else {
         // We are client, send the call all message to server
         try
         {
            client.sendAndWaitForResponse(new CallMessage(service,method,parameterTypes,
                     parameters,true));
         } catch ( StoreException e ) {
            logger.warn("exception when sending broadcast call: "+service+"."+method,e);
         }
      }
   }

   /**
    * Change the state physically to the given state.
    */
   private void changeState(NodeState newState)
   {
      eventDispatcher.notifyAll(
            new NodeStateChangeEvent(index,state,newState));
      state = newState;
   }

   /**
    * Ensure that the client given is properly disconnected.
    * This is called by clients to ensure that the node knows about
    * the disconnect.
    */
   void ensureDisconnect(NodeClient client)
   {
      synchronized ( stateMutex )
      {
         // Check if the client is current (can happen, that
         // clients disconnect later due to threading, while another
         // client is already present)
         if ( client != this.client )
            return;
         // Ensure that the maximal level is INITIALIZED
         if ( state.getLevel() <= NodeState.INITIALIZED.getLevel() )
            return; // State is less or equal
         ensureState(NodeState.INITIALIZED);
      }
   }

   /**
    * Ensure that, if possible the given state is reached. This method
    * makes all the necessary calls of state changes up and down.
    */
   void ensureState(NodeState newState)
   {
      synchronized ( stateMutex )
      {
         logger.debug("ensuring state: "+newState+", current state: "+state);
         if ( state == newState )
            return;
         // Separate two events: When state is increased, and when
         // state is decreased.
         if ( newState.getLevel() > state.getLevel() )
         {
            // Node is offline, set to uninitialized (requires no additional
            // operations)
            if ( (state==NodeState.OFFLINE) && 
                  (newState.getLevel()>NodeState.OFFLINE.getLevel()) )
               changeState(NodeState.UNINITIALIZED);
            // Trying to initialize
            if ( (state==NodeState.UNINITIALIZED) && 
                  (newState.getLevel()>NodeState.UNINITIALIZED.getLevel()) )
            {
               // Initialize (determine identity, launch server)
               initialize();
               // Set state
               changeState(NodeState.INITIALIZED);
            }
            // Trying to connect
            if ( (state==NodeState.INITIALIZED) && 
                  (newState.getLevel()>NodeState.INITIALIZED.getLevel()) )
            {
               // Initialized, now determine server and connect to it.
               connect();
               changeState(NodeState.CONNECTED);
            }
         } else {
            // State is decreased
            if ( (state==NodeState.CONNECTED) && 
                  (newState.getLevel()<NodeState.CONNECTED.getLevel()) )
            {
               // Set state
               changeState(NodeState.INITIALIZED);
               // Disconnect client or server, whichever is used
               if ( client != null )
               {
                  client.close();
                  client = null;
               } 
            }
            if ( (state==NodeState.INITIALIZED) && 
                  (newState.getLevel()<NodeState.INITIALIZED.getLevel()) )
            {
               // Set state
               changeState(NodeState.UNINITIALIZED);
               // Remove identity from database
               clearNode();
               // Shutdown server
               server.close();
               server = null;
            }
         }
      }
      logger.debug("state: "+state+", successfully established, requested was: "+newState);
   }

   /**
    * Load the node list into a list.
    */
   private List loadNodeList(Transaction transaction, int searchIndex)
   {
      ArrayList resultList = new ArrayList();
      ArrayList orderByList = new ArrayList();
      orderByList.add(new OrderBy(
               new ReferenceTerm("persistence_nodes",null,"nodeindex"),
               OrderBy.ASCENDING));
      Expression expr = null;
      if ( searchIndex > 0 )
      {
         expr = new Expression();
         expr.add(new ReferenceTerm("persistence_nodes",null,"nodeindex"));
         expr.add("<");
         expr.add(new ConstantTerm(new Integer(searchIndex)));
      }
      QueryStatement stmt = new QueryStatement("persistence_nodes",expr,orderByList);
      SearchResult result = database.search(transaction,stmt,null);
      for ( int i=0; i<result.getResult().size(); i++ )
      {
         Map attributes = (Map) result.getResult().get(i);
         NodeEntry entry = new NodeEntry();
         entry.ips=(String) attributes.get("ips");
         entry.port=((Number) attributes.get("command_port")).intValue();
         entry.index=((Number) attributes.get("nodeindex")).intValue();
         resultList.add(entry);
      }
      return resultList;
   }

   /**
    * Connect this nodes to the node network. This method reads all nodes from the node
    * table which are below this node, determines the first node from that which is
    * alive. This method may block until the status of a node is clear. If there are no
    * suitable nodes, this node becomes the server.
    */
   private void connect()
   {
      logger.debug("node connecting to server...");
      // New client
      client = null;
      List<NodeEntry> nodeList = null;
      // Reload server node list, check if some nodes disapeared,
      // so we don't have to check those
      Transaction transaction = transactionTracker.getTransaction();
      transaction.begin();
      try
      {
         nodeList = loadNodeList(transaction,index);
      } catch ( Exception e ) {
         logger.error("could not load node list",e);
         transaction.markRollbackOnly();
      } finally {
         transaction.commit();
      }
      // Try to select a server node from list (the first alive)
      NodeEntry serverNode = null;
      for ( int i=0; (i<nodeList.size()) && (serverNode==null); i++ )
      {
         NodeEntry entry = (NodeEntry) nodeList.get(i);
         if ( isAlive(entry.ips,entry.port) )
            serverNode = entry;
      }
      // If server node is found, connect to that, else make this a server
      if ( serverNode == null )
      {
         logger.debug("node will be the appointed server.");
         // We are the server, so clear all nodes that are dead
         clearNodeList(nodeList);
         serverIndex = index;
      } else {
         logger.debug("determined to be client node, server is: "+serverNode.ips+":"+serverNode.port);
         // If server node is not null, we should connect to it
         Socket socket = connect(serverNode.ips,serverNode.port);
         client = new NodeClient(this,socket,index,serverNode.index);
         // If connection is established, send and wait for the first
         // "init" message
         CommResponse response = client.sendAndWaitForResponse(new InitMessage(getId()));
         if ( response.getResponseCode() != CommResponse.ACTION_SUCCESS )
            throw new StoreException("server was not ready to accept commands, response code: "+
                  response.getResponseCode());
         serverIndex = serverNode.index;
      }
   }

   /**
    * Connect to server. This does not need to be synchronized, because it is
    * called from node manager state update, which is synchronized.
    */
   private Socket connect(String hostips, int hostport)
   {
      // Cook ips
      try
      {
         if ( "".equals(hostips) )
            hostips = InetAddress.getLocalHost().getHostAddress();
      } catch ( Exception e ) {
         throw new StoreException("can not determine local adapter, but there is another node, which would need to be contacted.",e);
      }
      // Connect
      logger.debug("(re)connecting to server: "+hostips+":"+hostport);
      try
      {
         // Connect physically
         StringTokenizer tokens = new StringTokenizer(hostips,",");
         while ( tokens.hasMoreTokens() )
         {
            try
            {
               String ip = tokens.nextToken();
               Socket socket = new Socket();
               socket.connect(new InetSocketAddress(ip,hostport),SOCKET_CONNECT_TIMEOUT);
               logger.debug("established connection with: "+ip+", out of "+hostips);
               return socket;
            } catch ( Exception e ) {
               if ( ! tokens.hasMoreTokens() )
                  throw e; // If no more ips, throw exception
            }
         }
      } catch ( StoreException e ) {
         throw e;
      } catch ( Exception e ) {
         throw new StoreException("exception while trying to connect "+hostips+":"+hostport,e);
      }
      return null;
   }

   /**
    * Clear this node from the database.
    */
   private void clearNode()
   {
      logger.debug("clearing node from database: "+index);
      Connection conn = database.getConnectionSource().getConnection();
      try
      {
         PreparedStatement pstmt = conn.prepareStatement("delete from nodes where nodeindex = "+index);
         pstmt.executeUpdate();
         pstmt.close();
         conn.commit();
      } catch ( Exception e ) {
         logger.error("error while clearing node: "+index);
      } finally {
         database.getConnectionSource().releaseConnection(conn);
      }
      logger.debug("node cleared from database.");
   }

   /**
    * Clear node list. This is called as part of the initialization
    * process, if all previous node entries in the database
    * are dead.
    */
   private void clearNodeList(List<NodeEntry> nodeList)
   {
      Transaction transaction = transactionTracker.getTransaction();
      transaction.begin();
      try
      {
         // Go through all nodes on our list, and remove all dead ones
         for ( int i=0; i<nodeList.size(); i++ )
         {
            NodeEntry entry = (NodeEntry) nodeList.get(i);
            Map attrs = new HashMap();
            attrs.put("nodeindex",new Integer(entry.index));
            database.remove(transaction,"persistence_nodes",attrs);
         }
      } catch ( StoreException e ) {
         transaction.markRollbackOnly();
         throw e;
      } catch ( Exception e ) {
         transaction.markRollbackOnly();
         throw new StoreException("exception while deleting dead nodes from database",e);
      } finally {
         transaction.commit();
      }
   }

   /**
    * Initialize node identity.
    */
   private void initialize()
   {
      logger.debug("node initializing...");
      Transaction transaction = transactionTracker.getTransaction();
      transaction.begin();
      try
      {
         // First ensure that table exists
         HashMap tableAttrs = new HashMap();
         tableAttrs.put("nodeindex",Integer.class);
         tableAttrs.put("ips",String.class);
         tableAttrs.put("command_port",Integer.class);
         ArrayList tableKeys = new ArrayList();
         tableKeys.add("nodeindex");
         database.ensureTable(transaction,"persistence_nodes",
               tableAttrs,tableKeys,true);
         // Load nodes table
         List<NodeEntry> nodeList = loadNodeList(transaction,0);
         // Determine identity
         index = 1;
         if ( nodeList.size() > 0 )
            index = 1 + ((NodeEntry) nodeList.get(nodeList.size()-1)).index;
         // Start the node server
         server = new NodeServer(this,eventDispatcher,index);
         ips = getHostAddresses();
         server.bind();
         int port = server.getServerSocket().getLocalPort();
         // Insert my index, port and ip to the nodes table. Note, duplicate
         // indices will not be allowed because of primary key.
         if ( logger.isDebugEnabled() )
            logger.debug("node identity determined, index: "+index+", address: "+ips+":"+port);
         Map attrs = new HashMap();
         attrs.put("nodeindex",new Integer(index));
         attrs.put("ips",ips);
         attrs.put("command_port",new Integer(port));
         database.insert(transaction,"persistence_nodes",attrs);
      } catch ( StoreException e ) {
         logger.fatal("could not initialize node subsystem.",e);
         transaction.markRollbackOnly();
         throw e;
      } catch ( Throwable e ) {
         logger.fatal("could not initialize node subsystem.",e);
         transaction.markRollbackOnly();
         throw new StoreException("unexcepted error",e);
      } finally {
         transaction.commit();
      }
   }

   public static class NodeEntry
   {
      public String ips;
      public int port;
      public int index;

      public int hashCode()
      {
         return index;
      }

      public boolean equals(Object obj)
      {
         if ( ! (obj instanceof NodeEntry) )
            return false;
         return index == ((NodeEntry)obj).index;
      }
      
      public String toString()
      {
         return "[Node: "+ips+":"+port+", index: "+index+"]";
      }
   }

   /**
    * If anything changed, reload the configuration values.
    */
   public void configurationChanged(ConfigurationEvent event)
   {
      if ( (event.getPropertyName()!=null) && 
            (event.getPropertyName().startsWith("beankeeper.n")) )
         configurationReload();
   }

   /**
    * Just read in the new configuration values, and use them from now on.
    */
   public void configurationReload()
   {
      CLIENT_RECONNECT_TRIES = configurationTracker.getConfiguration().
         getInt("beankeeper.node.client_reconnect_tries",2);
      SOCKET_CONNECT_TIMEOUT = configurationTracker.getConfiguration().
         getInt("beankeeper.net.connect_timeout",3000);
   }
}


