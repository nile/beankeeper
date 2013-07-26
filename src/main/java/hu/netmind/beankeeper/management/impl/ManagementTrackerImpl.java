/**
 * Copyright (C) 2008 NetMind Consulting Bt.
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

package hu.netmind.beankeeper.management.impl;

import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.management.ManagementTracker;
import hu.netmind.beankeeper.event.PersistenceEventListener;
import hu.netmind.beankeeper.event.PersistenceEvent;
import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.node.NodeManager;
import hu.netmind.beankeeper.node.event.NodeStateChangeEvent;
import java.util.*;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import org.apache.log4j.Logger;

/**
 * This implementation is waiting for the node to become connected to register the mbeans.
 * If a reconnect occurs, the beans are re-registered with the new node id.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ManagementTrackerImpl implements ManagementTracker, PersistenceEventListener
{
   private static final Logger logger = Logger.getLogger(ManagementTrackerImpl.class);

   private Map<String,Object> beans = new HashMap<String,Object>();
   private int nodeId = 0;

   private EventDispatcher eventDispatcher = null; // Injected

   /**
    * Register as listener.
    */
   public void init(Map parameters)
   {
      eventDispatcher.registerListener(this);
   }

   /**
    * Remove all beans from JMX, and unregister from event
    * dispatcher.
    */
   public void release()
   {
      eventDispatcher.unregisterListener(this);
      deregisterBeans();
   }
   
   /**
    * Register a JMX bean with the given name.
    */
   public void registerBean(String name, Object bean)
   {
      beans.put(name,bean);
      registerBeanInternal(name,bean);
   }

   /**
    * Deregister the bean with the given name.
    */
   public void deregisterBean(String name)
   {
      beans.remove(name);
      deregisterBeanInternal(name);
   }

   /**
    * Register all beans to JMX.
    */
   private void registerBeans()
   {
      for ( Map.Entry<String,Object> entry : beans.entrySet() )
         registerBeanInternal(entry.getKey(),entry.getValue());
   }

   /**
    * Deregister all beans from JMX.
    */
   private void deregisterBeans()
   {
      for ( Map.Entry<String,Object> entry : beans.entrySet() )
         deregisterBeanInternal(entry.getKey());
   }

   /**
    * Register a single bean into JMX.
    */
   private void registerBeanInternal(String name, Object object)
   {
      if ( nodeId == 0 )
         return; // Do nothing, node not initialized
      try
      {
         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         mbs.registerMBean(object,new ObjectName(
                  "hu.netmind.beankeeper:type="+name+",node="+nodeId));
         if ( logger.isDebugEnabled() )
            logger.debug("registered JMX bean: "+name);
      } catch ( Exception e ) {
         throw new StoreException("could not register JMX bean: "+name,e);
      }
   }

   /**
    * Deregister a single bean from JMX.
    */
   private void deregisterBeanInternal(String name)
   {
      if ( nodeId == 0 )
         return; // Do nothing, node not initialized
      try
      {
         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         mbs.unregisterMBean(new ObjectName(
                  "hu.netmind.beankeeper:type="+name+",node="+nodeId));
         if ( logger.isDebugEnabled() )
            logger.debug("deregistered JMX bean: "+name);
      } catch ( Exception e ) {
         throw new StoreException("could not deregister JMX bean: "+name,e);
      }
   }

   /**
    * Listen to node events, and deregister all beans
    * when the node disconnects, register all beans when
    * node initializes.
    */
   public void handle(PersistenceEvent event)
      throws Exception
   {
      if ( event instanceof NodeStateChangeEvent )
      {
         NodeStateChangeEvent stateEvent = (NodeStateChangeEvent) event;
         if ( (stateEvent.getNewState() == NodeManager.NodeState.INITIALIZED) &&
              (stateEvent.getOldState().getLevel() < NodeManager.NodeState.INITIALIZED.getLevel()) )
         {
            // Node entered initialized state from a lower state, so register all beans
            // and remember the node's id
            nodeId = stateEvent.getNodeId();
            if ( logger.isDebugEnabled() )
               logger.debug("node initialized, with id '"+nodeId+"', registering all beans");
            registerBeans();
         }
         if ( (stateEvent.getNewState().getLevel() < NodeManager.NodeState.INITIALIZED.getLevel()) &&
              (stateEvent.getOldState() == NodeManager.NodeState.INITIALIZED) )
         {
            // Node left initialized state for a lower state, so deregister beans
            if ( logger.isDebugEnabled() )
               logger.debug("node uninitialized, removing all JMX beans");
            try
            {
               deregisterBeans();
            } finally {
               nodeId=0;
            }
         }
      }
   }
}


