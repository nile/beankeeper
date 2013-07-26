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

package hu.netmind.beankeeper.service.impl;

import hu.netmind.beankeeper.service.StoreContext;
import hu.netmind.beankeeper.service.Service;
import hu.netmind.beankeeper.service.ServiceFactory;
import hu.netmind.beankeeper.common.StoreException;
import java.util.*;
import java.lang.reflect.Field;
import org.apache.log4j.Logger;

/**
 * This is the context for all the services. Basically a mini
 * dependency injection engine.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class StoreContextImpl implements StoreContext
{
   private static final String[] SERVICES = 
   {
      "hu.netmind.beankeeper.event.impl.EventDispatcherImpl",
      "hu.netmind.beankeeper.management.impl.ManagementTrackerImpl",
      "hu.netmind.beankeeper.config.impl.ConfigurationTrackerImpl",
      "hu.netmind.beankeeper.logging.impl.SnapshotLoggerImpl",
      "hu.netmind.beankeeper.logging.impl.AggregatorLoggerImpl",
      "hu.netmind.beankeeper.db.impl.DatabaseFactory",
      "hu.netmind.beankeeper.transaction.impl.InternalTransactionTrackerImpl",
      "hu.netmind.beankeeper.node.impl.NodeManagerImpl",
      "hu.netmind.beankeeper.serial.impl.SerialTrackerImpl",
      "hu.netmind.beankeeper.operation.impl.OperationTrackerImpl",
      "hu.netmind.beankeeper.transaction.impl.TransactionTrackerImpl",
      "hu.netmind.beankeeper.type.impl.TypeHandlerTrackerImpl",
      "hu.netmind.beankeeper.model.impl.ClassTrackerImpl",
      "hu.netmind.beankeeper.schema.impl.SchemaManagerImpl",
      "hu.netmind.beankeeper.object.impl.ObjectTrackerImpl",
      "hu.netmind.beankeeper.cache.impl.MinimalResultsCache",
      "hu.netmind.beankeeper.query.impl.QueryServiceImpl",
      "hu.netmind.beankeeper.modification.impl.ModificationTrackerImpl",
      "hu.netmind.beankeeper.lock.impl.LockTrackerImpl",
      "hu.netmind.beankeeper.type.impl.DefaultHandlersService",
      "hu.netmind.beankeeper.store.impl.StoreServiceImpl",
   };
   private static final Logger logger = Logger.getLogger(StoreContext.class);

   private Map parameters = new HashMap();
   private Map<String,Service> services = new HashMap<String,Service>();
   private List<Service> initOrder = new ArrayList<Service>();
   private Thread shutdownHook = null;
   private boolean shutdownRunning = false;
   private boolean released = false;

   /**
    * Initialize this context. This method populates the services
    * and registers the shutdown hook.
    */
   public void init(Map parameters)
   {
      logger.debug("initializing store context with parameters: "+parameters);
      this.parameters.putAll(parameters);
      // First, insert self as a service
      services.put(StoreContext.class.getName(),this);
      // Initialize dependencies as given in the list
      logger.debug("initializing services");
      for ( String serviceClassName : SERVICES )
      {
         try
         {
            // Get the implementation class
            Class serviceClass = Class.forName(serviceClassName);
            // Initialize the service
            if ( logger.isDebugEnabled() )
               logger.debug("initializing service: "+serviceClassName);
            // Creating and adding the service
            Service service = (Service) serviceClass.newInstance();
            // Inject & Initialize
            injectServices(service);
            service.init(parameters);
            // If this is a factory, then create real service object
            if ( service instanceof ServiceFactory )
            {
               Service realService = ((ServiceFactory) service).getService();
               injectServices(realService);
               realService.init(parameters);
               service.release();
               service = realService;
               serviceClass = realService.getClass();
            }
            // Initialization ok, add
            Set<Class> serviceInterfaces = getServiceInterfaces(serviceClass);
            for ( Class serviceInterface : serviceInterfaces )
               services.put(serviceInterface.getName(),service);
            initOrder.add(service);
            if ( logger.isDebugEnabled() )
               logger.debug("initialized service '"+serviceClass.getName()+"' which implements: "+serviceInterfaces);
         } catch ( StoreException e ) {
            throw e;
         } catch ( Exception e ) {
            throw new StoreException("could not initialize service: "+serviceClassName,e);
         }
      }
      // Proper shutdown
      shutdownHook = new Thread(new ShutdownProcess());
      Runtime.getRuntime().addShutdownHook(shutdownHook);
      // Ok
      logger.debug("store initialization complete, good luck");
   }

   /**
    * Determine all the interfaces a service provides.
    */
   private Set<Class> getServiceInterfaces(Class serviceClass)
   {
      Class originalClass = serviceClass;
      // Find and register all service interfaces
      Set<Class> result = new HashSet<Class>();
      while ( serviceClass != null )
      {
         Class[] interfaces = serviceClass.getInterfaces();
         for ( Class interfaceClass : interfaces )
         {
            if ( Service.class.isAssignableFrom(interfaceClass) )
               result.add(interfaceClass);
         }
         serviceClass = serviceClass.getSuperclass();
      }
      if ( logger.isDebugEnabled() )
         logger.debug("service '"+originalClass+"' had following interfaces: "+result);
      return result;
   }

   /**
    * Inject services into the object given.
    */
   public void injectServices(Object obj)
   {
      Class objectClass = obj.getClass();
      while ( objectClass != null )
      {
         Field[] fields = objectClass.getDeclaredFields();
         for ( Field field : fields )
         {
            // Inject a single field
            if ( Service.class.isAssignableFrom(field.getType()) )
            {
               Service service = getService(field.getType().getName());
               if ( service == null )
                  throw new StoreException("missing service for field: "+field.getType().getName()+", into: "+obj+", class: "+objectClass);
               try
               {
                  field.setAccessible(true);
                  field.set(obj,service);
               } catch ( IllegalAccessException e ) {
                  throw new StoreException("no permission to inject service to: "+field,e);
               }
            }
         }
         objectClass = objectClass.getSuperclass();
      }
   }

   /**
    * Return whether the given service is available.
    * @return True if service is available.
    */
   public boolean hasService(String name)
   {
      return getService(name) != null;
   }

   /**
    * Get the service under the given name.
    * @param name The name of the service to return.
    * @return The service under the given name.
    */
   public Service getService(String name)
   {
      return services.get(name);
   }

   /**
    * Close this context and release all allocated resources.
    */
   public void release()
   {
      if ( released )
         return; // Don't release twice
      released = true;
      // Call release on all services in reverse order of initialization
      List<Service> closeOrder = new ArrayList<Service>(initOrder);
      Collections.reverse(closeOrder);
      for ( Service service : closeOrder )
      {
         try
         {
            service.release();
         } catch ( Exception e ) {
            logger.error("error while releasing service: "+service,e);
         }
      }
      // Clear everything
      initOrder = new ArrayList<Service>();
      services = new HashMap<String,Service>();
      // Remove shutdown hook when complete
      if ( ! shutdownRunning )
         Runtime.getRuntime().removeShutdownHook(shutdownHook); // Do not run twice
   }

   /**
    * Get the parameters this context was created with.
    */
   public Map getParameters()
   {
      return parameters;
   }

   /**
    * This is a shutdown logic, which simply calls <code>close()</code>
    * when the JVM exists.
    */
   private class ShutdownProcess implements Runnable
   {
      public void run()
      {
         shutdownRunning=true;
         release();
      }
   }

}


