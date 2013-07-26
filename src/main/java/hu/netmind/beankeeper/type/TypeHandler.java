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

package hu.netmind.beankeeper.type;

import java.util.*;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.transaction.Transaction;

/**
 * Classes implementing this interface can handle custom attribute types.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface TypeHandler
{
   /**
    * Ensure that the table or tables (if there are some) backing this 
    * attribute exist. Use the database service if tables need to be
    * created.
    * @param parentInfo The ClassInfo of the container class.
    * @param attributeName The attribute name of the handled attribute.
    * @param create Whether to physically create the tables or not.
    */
   void ensureTableExists(ClassInfo parentInfo, String attributeName, boolean create);

   /**
    * Get the attribute types that are representing this type
    * in the original object.
    * @param attributeName The attribute name that is handled by this handler.
    * @return A map of attribute name and class mappings. This is used to return primitive
    * attributes that should be embedded into the parent object instead of the handled
    * non-primitive attribute. There can be any number of substitute attributes from zero
    * to more.
    */
   Map getAttributeTypes(String attributeName);

   /**
    * Determine whether the current attribute in the object given differs from the
    * value in the database.
    * @param parentInfo The parent class info.
    * @param parent The parent object that holds the attribute to be compared.
    * @param attributeName The attribute to compare.
    * @param dbAttributes The primitive attributes as currently in the database.
    * @param serial The current serial.
    * @return False if the parent is holding the same value as represented in the
    * database.
    */
   boolean hasChanged(ClassInfo parentInfo, Object parent, String attributeName, Map dbAttributes,
         Long serial);

   /**
    * Get the umarshalled instance.
    * @param parentInfo The info of the parent object.
    * @param parent The parent object.
    * @param attributeName The name of the attribute that needs to be unmarshalled.
    * @param marshalledValues All the values of the parent object as in the database.
    * @param timeControl The time control in which the parent was selected.
    */
   Object unmarshallType(ClassInfo parentInfo, Object parent,
         String attributeName, Map marshalledValues, TimeControl timeControl);

   /**
    * Save the changes occured.
    * @param classInfo The class info of the parent object.
    * @param current The parent object.
    * @param attributeName The name of the attribute about to save.
    * @param transaction The transaction the saves must occur in.
    * @param currentSerial The serial id the current saved are in.
    * @param newValue The new value that must be saved.
    * @param waitingObjects List of object on which this operation depends on.
    * This is writable, insert any objects that also need to be saved for this
    * change to be consistent.
    * @param saveTables The tables that have saved entries after this operation. This is
    * writeable.
    * @param removeTables The tables that have removed entries in it after this
    * operation. This is writable.
    * @param events The events the type save generated, this is writable.
    * @param changedAttributes The attributes need to be updated in the
    * database. This is writable, these attributes will be saved directly
    * into the parent as primitive attributes.
    * @param dbAttributes The database attributes currently in the database.
    * @param updatedAttributes The attributes that need to be updated
    * in the object tracker. This is writable and should contain the
    * unmarshalled values of the changes.
    * @return The new value of the attribute.
    */
   Object save(ClassInfo classInfo, Object current, String attributeName,
         Transaction transaction, Long currentSerial,
         Object newValue, Set waitingObjects, Set saveTables, Set removeTables,
         List events, Map changedAttributes, Map dbAttributes);

   /**
    * Called when all save operations have completed, and all dependent
    * objects were saved.
    */
   void postSave(Object value);

   /**
    * Create the approriate symbol entry when parsing a query.
    */
   WhereResolver.SymbolTableEntry getSymbolEntry(AttributeSpecifier spec,
         WhereResolver.SymbolTableEntry previousEntry, ClassInfo previousInfo,
         ReferenceTerm previousTerm)
      throws ParserException;

   /**
    * Determine the next class info after the given specifier. If this type
    * can be dereferenced, then this method must return the class info of
    * this type.
    */
   ClassInfo getSymbolInfo(WhereResolver.SymbolTableEntry entry,
         AttributeSpecifier spec)
      throws ParserException;
}


