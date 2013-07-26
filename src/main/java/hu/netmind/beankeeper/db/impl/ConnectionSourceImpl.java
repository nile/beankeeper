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

package hu.netmind.beankeeper.db.impl;

import java.util.LinkedList;
import java.util.HashMap;
import java.sql.Connection;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.util.Iterator;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.apache.log4j.Logger;
import org.apache.commons.configuration.event.ConfigurationEvent;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.db.ConnectionSource;
import hu.netmind.beankeeper.config.ExtendedConfigurationListener;
import hu.netmind.beankeeper.config.ConfigurationTracker;
import hu.netmind.beankeeper.logging.SnapshotLogger;

/**
 * This is the connection source for database implementations. It pools
 * the connections so the database implementation does not need to
 * bother with connection allocation details. Be sure to return the
 * connection to this pool when ready.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ConnectionSourceImpl implements ConnectionSource,ExtendedConfigurationListener
{
   private static Logger logger = Logger.getLogger(ConnectionSource.class);
   
   // Timeout for a single connection before it gets dumped.
   // Note: This could be exchanged for validating a connection, but
   // there is no certain way to do it, except running an sql statement.
   private static long TIMEOUT = 10*60*1000; // 10 mins
      
   private DataSource dataSource;

   private int maxConnections;
   private LinkedList pool;
   private HashMap wrappers;

   private ConfigurationTracker configurationTracker = null;
   private SnapshotLogger snapshotLogger = null;

   public ConnectionSourceImpl(ConfigurationTracker configurationTracker, SnapshotLogger snapshotLogger, DataSource dataSource)
   {
      // Initialize variables
      this.dataSource=dataSource;
      this.configurationTracker=configurationTracker;
      this.snapshotLogger=snapshotLogger;
      pool = new LinkedList();
      wrappers = new HashMap();
      // Set max connection count to infinite for start
      maxConnections = 0;
      // Load configuration
      configurationReload();
      configurationTracker.addListener(this);
   }

   DataSource getDataSource()
   {
      return dataSource;
   }

   /**
    * Returns whether the datasource is pooled. Currently, all
    * datasources allocated through JNDI are considered pooled.
    */
   private boolean isDataSourcePooled()
   {
      return ! (dataSource instanceof DriverDataSource);
   }

   /**
    * Release all database connections. After calling this method, the
    * connection source is considered unusable.
    */
   public synchronized void release()
   {
      // Close all connections
      for ( int i=0; i<pool.size(); i++)
      {
         ConnectionWrapper wrapper = (ConnectionWrapper) pool.get(i);
         try
         {
            if ( wrapper.connection != null )
               wrapper.connection.close();
         } catch ( Exception e ) {
            logger.error("could not close connection on release.",e);
         }
      }
      // Empty pool
      pool = new LinkedList();
      wrappers = new HashMap();
   }

   /**
    * Get a new pooled connection. This method get a connection from the
    * pool, or allocates a new connection is no pooled ones are available.
    * @return A new connection object.
    */
   public synchronized Connection getConnection()
   {
      // Find an available connection, if a connection becomes unusable, it
      // is removed
      long currentTime = System.currentTimeMillis();
      ConnectionWrapper wrapper = null;
      Iterator iterator = new LinkedList(pool).iterator();
      int usedCount = 0;
      while ( iterator.hasNext() )
      {
         ConnectionWrapper current = (ConnectionWrapper) iterator.next();
         boolean usable = true;
         // If the connection is pooled elsewhere, and it is used,
         // then we have no business with it
         if ( (current.used) && (isDataSourcePooled()) )
            continue;
         // Check whether connection became closed somehow
         try
         {
            usable = ! current.connection.isClosed();
         } catch ( Exception e ) {
            // Ok, probably closed
            logger.debug("while checking closed status, connection threw",e);
            usable = false;            
         }
         // Check for timeout
         if ( current.lastUsed+TIMEOUT < currentTime )
         {
            usable = false;
            if ( current.used )
               logger.warn("a connection is used, but reached timeout and will be reclaimed, possible unbalanced transaction handling!");
         }
         // If usable, then return this wrapper, else close it
         if ( usable )
         {
            if ( ! current.used )
            {
               // This is usable but not used, so return it. Next iterations 
               // can override this still, so always the last usable 
               // connection will be used.
               wrapper = current; 
            } else {
               // Connection is used
               usedCount++;
            }
         }
      }
      // Allocate connection if not yet found
      if ( wrapper == null )
      {
         // Allocate connection
         wrapper = new ConnectionWrapper();
         try
         {
            logger.debug("connection pool has: "+pool.size()+
                  " connections, maximum connections allocated at any given time: "+maxConnections+", need new connection.");
            wrapper.connection=dataSource.getConnection();
            wrapper.connection.setAutoCommit(false);
         } catch ( StoreException e ) {
            throw e;
         } catch ( Exception e ) {
            throw new StoreException("could not get a new connection from datasouce, current pool size: "+pool.size(),e);
         }
         // Add to pool
         pool.add(wrapper);
         wrappers.put(wrapper.connection,wrapper);
         // Successful connection
         if ( pool.size() > maxConnections )
            maxConnections = pool.size();
      }
      // Profile
      snapshotLogger.log("connectionpool","Connection used/pool/wrappers: "+usedCount+"/"+pool.size()+"/"+wrappers.size());
      // Return connection if one has been found
      wrapper.used=true;
      wrapper.lastUsed=currentTime;
      // Create a wrapper on the connection object, so each use
      // of it's prepareStatement() can update it's wrapper's last used time.
      try
      {
         return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
               new Class[] { Connection.class },
               new WrapperHandler(wrapper));
      } catch ( Exception e ) {
         throw new StoreException("exception while creating connection proxy",e);
      }
   }

   /**
    * Close a wrapper, and drop from pool.
    */
   private void closeWrapper(ConnectionWrapper wrapper)
   {
      pool.remove(wrapper);
      wrappers.remove(wrapper.connection);
      // Close it too
      try
      {
         wrapper.connection.close();
      } catch ( Exception e ) {
         logger.debug("could not close wrapper, this is no problem, it may already been closed",e);
      }
   }

   /**
    * Release a connection back to the pool.
    * @param connection The connection to release.
    */
   public synchronized void releaseConnection(Connection connection)
   {
      ConnectionWrapper wrapper = (ConnectionWrapper) wrappers.get(connection);
      if ( wrapper != null )
         wrapper.used=false;
      // If the data source is pool, then drop this wrapper and
      // do not return it to the pool
      if ( isDataSourcePooled() )
         closeWrapper(wrapper);
   }
   
   private class WrapperHandler implements InvocationHandler
   {
      private ConnectionWrapper wrapper;

      public WrapperHandler(ConnectionWrapper wrapper)
      {
         this.wrapper=wrapper;
      }
      
      public Object invoke(Object proxy, Method method, Object[] args)
         throws Throwable
      {
         // If method is prepareStatement(), then update wrapper's
         // last used indicator.
         if ( method.getName().equals("prepareStatement") )
            wrapper.lastUsed = System.currentTimeMillis();
         // Call wrapped connection
         return method.invoke(wrapper.connection,args);
      }
   }
   
   private class ConnectionWrapper
   {
      public volatile boolean used = false;
      public volatile long lastUsed = 0;
      public Connection connection = null;
   }

   public void configurationChanged(ConfigurationEvent event)
   {
      if ( "beankeeper.pool.connection_timeout".equals(event.getPropertyName()) )
         configurationReload();
   }

   public void configurationReload()
   {
      TIMEOUT = configurationTracker.getConfiguration().
         getInt("beankeeper.pool.connection_timeout",10*60*1000);
   }
}



