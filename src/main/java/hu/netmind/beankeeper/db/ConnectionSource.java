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

package hu.netmind.beankeeper.db;

import java.sql.Connection;

/**
 * This is a source of connections. Note: remember to release a connection
 * back to this source if you're done with it instead of closing it.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface ConnectionSource
{
   /**
    * Get a new pooled connection. This method get a connection from the
    * pool, or allocates a new connection is no pooled ones are available.
    * @return A new connection object.
    */
   Connection getConnection();

   /**
    * Release a connection back to the pool.
    * @param connection The connection to release.
    */
   void releaseConnection(Connection connection);

   /**
    * Release all connections.
    */
   void release();
}



