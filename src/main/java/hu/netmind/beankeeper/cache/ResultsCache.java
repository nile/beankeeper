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

package hu.netmind.beankeeper.cache;

import hu.netmind.beankeeper.service.Service;
import java.util.*;
import hu.netmind.beankeeper.parser.QueryStatement;
import hu.netmind.beankeeper.db.SearchResult;
import hu.netmind.beankeeper.db.Limits;

/**
 * This service caches results.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface ResultsCache extends Service
{
   /**
    * Get an entry from the cache.
    * @param stmt The statement to look for.
    * @param limits The limits of the query.
    * @return A SearchResult object if the query was cached, null otherwise.
    */
   SearchResult getEntry(QueryStatement stmt, Limits limits);

   /**
    * Add an entry to the cache.
    * @param stmt The statement source of result.
    * @param limits The limits of result.
    * @param result The SearchResult object.
    */
   void addEntry(QueryStatement stmt, Limits limits, SearchResult result);

   /**
    * Clear the cache.
    */
   void clear();
}


