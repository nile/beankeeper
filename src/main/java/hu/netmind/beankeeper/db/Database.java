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

package hu.netmind.beankeeper.db;

import hu.netmind.beankeeper.service.Service;
import java.util.Map;
import java.util.List;
import hu.netmind.beankeeper.parser.QueryStatement;
import hu.netmind.beankeeper.transaction.Transaction;

/**
 * This is a service accessing the database.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface Database extends Service
{
   /**
    * Get the connection source of this database.
    */
   ConnectionSource getConnectionSource();

   /**
    * Modifies an object already in database with given fields.
    * @param tableName The table to save attributes to.
    * @param keys The keys of object to save (All object entries have keys).
    * @param attributes The attributes in form of name:value pairs.
    */
   void save(Transaction transaction, String tableName, 
         Map keys, Map attributes);

   /**
    * Insert an object into the database.
    * @param tableName The table to save attributes to.
    * @param id The id of object to save (All object entries have an id).
    * @param attributes The attributes in form of name:value pairs.
    */
   void insert(Transaction transaction, String tableName, 
         Map attributes);

   /**
    * Remove an entry from database.
    * @param tableName The table to remove object from.
    * @param attributes The attributes which identify the object.
    * Equality is assumed with each attribute and it's value.
    */
   void remove(Transaction transaction, String tableName,
         Map attributes);

   /**
    * Ensure that table exists in database.
    * @param tableName The table to check.
    * @param attributeTypes The attribute names together with which
    * java class they should hold.
    * @param create If true, create table physically, if false, only
    * update internal representations, but do not create table.
    */
   void ensureTable(Transaction transaction, String tableName,
         Map attributeTypes, List keyAttributeNames, boolean create);

   /**
    * Select objects from database as ordered list of attribute maps.
    * @param transaction The transaction to run in.
    * @param stmt The query statement.
    * @param limits The limits of the result. (Offset, maximum result count)
    * @return The result object.
    */
   SearchResult search(Transaction transaction, 
         QueryStatement stmt, Limits limits);
}


