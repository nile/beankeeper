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

package hu.netmind.beankeeper.event;

/**
 * An event listener can receive all generated events from the library.
 * A listener may be registered in the <code>EventDispatcher</code>.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public interface PersistenceEventListener
{
   /**
    * Handle the event from the library. This method may throw exceptions,
    * in which case the containing transaction (if there is one) will be
    * rolled back.
    */
   void handle(PersistenceEvent event)
      throws Exception;
}


