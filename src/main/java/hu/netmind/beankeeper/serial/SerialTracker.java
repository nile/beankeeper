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

package hu.netmind.beankeeper.serial;

import hu.netmind.beankeeper.service.Service;
import java.util.Date;

/**
 * Use this service to get serial numbers for the runtime.
 * It is guaranteed, that each serial number will be greater than the
 * previous, even in subsequent executions. It is assumed though, that
 * at least a millisecond passes between executions. When multiples nodes
 * are participating, one nodes must supply serials to all others to guarantee
 * that serials are unique in each of the nodes.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface SerialTracker extends Service
{
   /**
    * Get the next serial for database functions.
    */
   Long getNextSerial();
}


