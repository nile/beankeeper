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

package hu.netmind.beankeeper.schema;

import hu.netmind.beankeeper.service.Service;
import hu.netmind.beankeeper.model.ClassEntry;

/**
 * Manages the schema in the database. All schema decision
 * questions go into here.
 */
public interface SchemaManager extends Service
{
   /**
    * Ensure that the schema for the given entry is in sync
    * with the model.
    */
   void ensureSchema(ClassEntry entry);

   /**
    * Get the table name for a given class entry.
    */
   String getTableName(ClassEntry entry);

   /**
    * Get a class entry for a table name.
    */
   ClassEntry getClassEntry(String tableName);

   /**
    * Get the table name for an attribute in an entry.
    * This can be used for attributes that need
    * a separate table for information, like many-to-many
    * relations, etc.
    */
   String getTableName(ClassEntry entry, String attributeName);
}

