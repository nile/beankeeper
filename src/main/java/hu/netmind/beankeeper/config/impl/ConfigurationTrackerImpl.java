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

package hu.netmind.beankeeper.config.impl;

import hu.netmind.beankeeper.config.ConfigurationTracker;
import hu.netmind.beankeeper.config.ExtendedConfigurationListener;
import hu.netmind.beankeeper.common.StoreException;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.PropertiesConfiguration;
import java.util.*;

/**
 * Configuration can be queried and altered through this tracker.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ConfigurationTrackerImpl implements ConfigurationTracker
{
   private AbstractConfiguration configuration = null;
   private List listeners = new ArrayList();

   public void init(Map parameters)
   {
      try
      {
         // Initialize with property reader configuration
         setConfiguration(new PropertiesConfiguration("beankeeper.properties"));
      } catch ( Exception e ) {
         throw new StoreException("fatal error, could not load default configuration",e);
      }
   }

   public void release()
   {
   }

   public Configuration getConfiguration()
   {
      return configuration;
   }

   /**
    * Set a new configuration for beankeeper. If you set a new configuration
    * object, all values will be re-read on the fly.
    */
   public void setConfiguration(AbstractConfiguration configuration)
   {
      if ( configuration == null )
         return;
      // First clear all listeners from previous configuration if
      // there was one
      if ( this.configuration != null )
      {
         Iterator listenersIterator = listeners.iterator();
         while ( listenersIterator.hasNext() )
            this.configuration.removeConfigurationListener((ConfigurationListener) listenersIterator.next());
      }
      // Set new configuration
      this.configuration=configuration;
      // Set all listeners, and invoke reload on all
      Iterator listenersIterator = listeners.iterator();
      while ( listenersIterator.hasNext() )
      {
         ExtendedConfigurationListener listener = (ExtendedConfigurationListener) listenersIterator.next();
         this.configuration.addConfigurationListener(listener);
         listener.configurationReload();
      }
   }

   /**
    * Add a listener to the configuration tracker.
    */
   public void addListener(ExtendedConfigurationListener listener)
   {
      // Add to our internal listener list
      listeners.add(listener);
      // Add to current configuration
      configuration.addConfigurationListener(listener);
   }

   /**
    * Remove a listener from the configuration tracker.
    */
   public void removeListener(ExtendedConfigurationListener listener)
   {
      // Remove from our internal listener list
      listeners.remove(listener);
      // Remove from current configuration
      configuration.removeConfigurationListener(listener);
   }
}


