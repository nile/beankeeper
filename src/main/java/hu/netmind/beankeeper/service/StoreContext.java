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

package hu.netmind.beankeeper.service;

import java.util.Map;
import java.util.Set;

/**
 * Holder of all services, a mini dependency injection engine.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface StoreContext extends Service
{
   public static final String PARAM_DRIVERURL = "db.driver.url";
   public static final String PARAM_DRIVERCLASS = "db.driver.class";
   public static final String PARAM_DATASOURCE = "db.driver.datasource";
   public static final String PARAM_CONNECTIONSOURCE = "db.connection.source";

   /**
    * Return whether the given service is available.
    * @return True if service is available.
    */
   boolean hasService(String name);

   /**
    * Get the service under the given name.
    * @param name The name of the service to return.
    * @return The service under the given name.
    */
   Service getService(String name);

   /**
    * Get the parameters this context was created with.
    */
   Map getParameters();

   /**
    * Inject services into the object given.
    */
   void injectServices(Object obj);
}


