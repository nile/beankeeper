/**
 * Copyright (C) 2007 NetMind Consulting Bt.
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

package hu.netmind.beankeeper.store.event;

import hu.netmind.beankeeper.service.StoreContext;
import hu.netmind.beankeeper.event.PersistenceEvent;

/**
 * All events which revolve around the change to one object extend
 * this base. These events are only delivered on the originating node.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class ObjectEvent implements PersistenceEvent
{
   private Object object;

   public ObjectEvent(Object object)
   {
      this.object=object;
   }
   
   public Object getObject()
   {
      return object;
   }

}


