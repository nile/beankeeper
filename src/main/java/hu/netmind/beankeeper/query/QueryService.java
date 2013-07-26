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

package hu.netmind.beankeeper.query;

import hu.netmind.beankeeper.service.Service;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.db.SearchResult;
import hu.netmind.beankeeper.db.Limits;
import java.util.Map;

/**
 * Offers methods to query objects from the store.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface QueryService extends Service
{
   /**
    * Select object lazily from a given statement.
    */
   LazyList find(String statement);

   /**
    * Select object lazily from a given statement
    * and parameters.
    */
   LazyList find(String statement, Object[] parameters);

   /**
    * Select with time control.
    */
   LazyList find(String statement, Object[] parameters, TimeControl timeControl,
         Map unmashalledObjects);

   /**
    * Select object lazily from a given statement.
    */
   Object findSingle(String statement);

   /**
    * Select object lazily from a given statement
    * and parameters.
    */
   Object findSingle(String statement, Object[] parameters);

   /**
    * Select objects from a given range, with a programmatic
    * query.
    */
   SearchResult find(QueryStatement stmt, Limits limits, Map unmarshalledObjects);

   /**
    * Select objects from a given range, with a programmatic
    * query.
    */
   SearchResult find(QueryStatement stmt, Limits limits);
}


