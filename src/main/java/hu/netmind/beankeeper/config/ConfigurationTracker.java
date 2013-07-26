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

package hu.netmind.beankeeper.config;

import hu.netmind.beankeeper.service.Service;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;

/**
 * Configuration can be queried and altered through this tracker.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface ConfigurationTracker extends Service
{
   /**
    * Get the configuration for the library.
    * @return The configuration instance.
    */
   Configuration getConfiguration();

   /**
    * Set a new configuration for beankeeper. If you set a new configuration
    * object, all values will be re-read on the fly.
    */
   void setConfiguration(AbstractConfiguration configuration);

   /**
    * Add a listener to the configuration tracker. This listener
    * will get all update events regarding configuration.
    */
   void addListener(ExtendedConfigurationListener listener);

   /**
    * Remove a listener from the configuration tracker.
    */
   void removeListener(ExtendedConfigurationListener listener);
}


