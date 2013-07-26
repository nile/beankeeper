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

package hu.netmind.beankeeper.management;

import hu.netmind.beankeeper.service.Service;

/**
 * This service tracks management (JMX) beans. When a bean is registered,
 * this tracker will try to register the bean as soon as possible (when
 * the node connects), and re-register it if the node id changes.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface ManagementTracker extends Service
{
   /**
    * Register a JMX bean with the given name.
    */
   void registerBean(String name, Object bean);

   /**
    * Deregister the bean with the given name.
    */
   void deregisterBean(String name);
}


