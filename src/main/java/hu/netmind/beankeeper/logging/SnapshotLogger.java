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

package hu.netmind.beankeeper.logging;

import hu.netmind.beankeeper.service.Service;

/**
 * This logger can recieve many log lines from different categories, but
 * for every category only periodic snapshots of the actual log lines are
 * written to the log. This enables periodic reporting of fast changing
 * non-aggregate values.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public interface SnapshotLogger extends Service
{
   /**
    * Log an event.
    * @param category The category identifier of the event. Each category
    * will have it's own snapshot periodically.
    * @param message The current message of the category. Note, that it's
    * possible that currently it's not time to output this message, in which
    * case it will be essentially dropped.
    */
   void log(String category,String message);
}


