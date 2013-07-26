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
 * This logger can receive log messages with an array of values, and reports
 * an aggregated view of those values periodically. This enables periodic
 * reporting of statistics of fast changing values.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public interface AggregatorLogger extends Service
{
   /**
    * Log an event. This event will not be logged directly, so this
    * method can be called as many times as necessary.
    * @param message The message of event. This message is also used as
    * a key, so values with the same message will be aggregated into one message.
    * @param values The values for given message.
    */
   void log(String message, int[] values);
}


