/**
 * Copyright (C) 2008 NetMind Consulting Bt.
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

package hu.netmind.beankeeper.type.impl;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.type.TypeHandler;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.transaction.Transaction;

/**
 * This is a type handler for those simple types, which can store their
 * content in a primitive type, but need some kind of wrapping and
 * unwrapping. Extend this class to provide type handlers for simple
 * types which translate to one primitive attribute.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public abstract class SimpleTypeHandler implements TypeHandler
{
   /**
    * This method is called to create the table backing a type like
    * this. Because we store the type's value in the object itself,
    * we don't need any additional tables, so this is empty.
    */
   public void ensureTableExists(ClassInfo parentInfo, String attributeName, boolean create)
   {
   }

   /**
    * Get the attribute types that are representing this type
    * in the original object. This type will be primitive type
    * which this type translates to.
    */
   public Map getAttributeTypes(String attributeName)
   {
      HashMap attributeTypes = new HashMap();
      attributeTypes.put(attributeName,getPrimitiveType());
      return attributeTypes;
   }

   /**
    * Implement this method to return the primitive type this handled
    * type translates to.
    */
   public abstract Class getPrimitiveType();

   /**
    * Determine whether the two values differ. Because these are simple
    * primitive values, we simply use the <code>equals()</code> method.
    */
   public boolean hasChanged(ClassInfo info, Object obj, String attributeName, Map dbAttributes,
         Long serial)
   {
      Object objectValue = info.getAttributeValue(obj,attributeName);
      Object dbValue = dbAttributes.get(attributeName);
      return ! (((objectValue==null) && (dbValue==null)) ||
         ((objectValue!=null) && (objectValue.equals(dbValue))));
   }

   /**
    * Get the umarshalled instance.
    * @param classInfo The info of the parent object.
    * @param obj The object itself.
    * @param attributeName The name of the attribute in question.
    * @param marshalledValues All the values of the parent object.
    * @param timeControl The time control in which the parent was selected.
    */
   public Object unmarshallType(ClassInfo classInfo, Object obj,
         String attributeName, Map marshalledValues, TimeControl timeControl)
   {
      Object primitive = marshalledValues.get(attributeName);
      return unmarshallType(primitive);
   }

   /**
    * Implement this method to create an instance of this handled type
    * from the primitive representation.
    */
   public abstract Object unmarshallType(Object primitiveValue);

   /**
    * Save the changes occured, this only has to marshall the object into
    * the primitive form.
    * @param classInfo The class info of the parent object.
    * @param current The parent object.
    * @param attributeName The name of the attribute about to save.
    * @param transaction The transaction the saves must occur in.
    * @param currentSerial The serial id the current saved are in.
    * @param newValue The new value.
    * @param waitingObjects List of object on which this operation depends on.
    * This is writable.
    * @param events The events the type save generated.
    * @param changedAttributes The attributes need to be updated in the
    * database.
    * @param dbAttributes The database attributes.
    * @param updatedAttributes The attributes that need to be updated
    * in the object tracker.
    * @return The new object of the attribute.
    */
   public Object save(ClassInfo classInfo, Object current, String attributeName,
         Transaction transaction, Long currentSerial,
         Object newValue, Set waitingObjects, Set saveTables, Set removeTables,
         List events, Map changedAttributes, Map dbAttributes)
   {
      changedAttributes.put(attributeName,marshallType(newValue));
      return newValue;
   }

   /**
    * Implement this method to translate an instance of this handled type
    * into a primitive representation.
    */
   public abstract Object marshallType(Object obj);

   /**
    * This method is empty, not used.
    */
   public void postSave(Object value)
   {
   }

   /**
    * Create the approriate symbol entry when parsing a query. Because
    * primitive types do not need a symbol entry, this method simply
    * returns null.
    */
   public WhereResolver.SymbolTableEntry getSymbolEntry(AttributeSpecifier spec,
         WhereResolver.SymbolTableEntry previousEntry, ClassInfo previousInfo,
         ReferenceTerm previousTerm)
      throws ParserException
   {
      return null;
   }

   /**
    * Determine the next class info after the given specifier. This is 
    * never invoked, because this handler does not produce a symbol entry.
    */
   public ClassInfo getSymbolInfo(WhereResolver.SymbolTableEntry entry,
         AttributeSpecifier spec)
      throws ParserException
   {
      throw new ParserException(ParserException.ABORT,"simple typehandler handles this attribute, so it's a primitive type, but it was dereferenced");
   }
}
