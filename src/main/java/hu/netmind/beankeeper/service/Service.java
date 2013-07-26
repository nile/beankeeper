/**
 * Copyright (C) 2009 NetMind Consulting Bt.
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

/**
 * This is the base interface for all services. Note that in order
 * to create services, first the full service interface has to be
 * defined, which should extend this interface. Then the service
 * can be implemented as a local and/or remote service.
 */
public interface Service
{
   /**
    * Initialize this service with the context and creation parameters.
    * @param context The context this service will be a part of. The 
    * initialization parameters are contained in this context.
    */
   void init(Map parameters);

   /**
    * Release all resources associated with this service.
    */
   void release();
}

