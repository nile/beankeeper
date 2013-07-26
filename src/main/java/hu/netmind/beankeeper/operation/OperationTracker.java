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

package hu.netmind.beankeeper.operation;

import hu.netmind.beankeeper.service.Service;
import hu.netmind.beankeeper.service.StoreContext;
import org.apache.log4j.Logger;
import java.util.*;

/**
 * This class tracks current commits and queries. All operations should be implemented
 * on the server node.
 * @author Robert Brautigam
 * @version Revision: $Revision$
 */
public interface OperationTracker extends Service
{
   /**
    * Get the operations mutex. This mutex can be used to synchronize
    * commits and queries to some other operation. All operations
    * in this service are synchronized to this mutex.
    */
   Object getMutex();

   /**
    * Request to start commit in the given node. The method allocates
    * a new serial for the commit operation.
    * @return A new serial to use in the commit.
    */
   Long startCommit(int nodeId);

   /**
    * End a commit that was started on the given node, with the given serial.
    */
   void endCommit(int index, Long serial, Long txSerial);

   /**
    * This method waits until the query returning values on given serial can be executed
    * with consistent results. This means, wait for all commits to finish that are in 
    * progress and have a lower serial value. This is needed, because if the query operation
    * is allowed to continue with commits with lower serials pending, then when those commits
    * are physically executed, the result of the query might change. Commits with higher
    * serials do not influence resultsets, so query does not need lock against later operations.
    */
   void waitForQuery(Long serial);
}


