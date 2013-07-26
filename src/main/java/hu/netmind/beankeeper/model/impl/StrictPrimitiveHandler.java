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

package hu.netmind.beankeeper.model.impl;

import hu.netmind.beankeeper.service.StoreContext;
import org.apache.log4j.Logger;
import java.util.*;
import java.lang.reflect.*;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.model.*;

/**
 * This type is for boxed primitive objects which need to be stored. It has
 * a single attribute named 'value', with the type of the primitive object itself.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class StrictPrimitiveHandler implements StrictClassHandler
{
   private static Logger logger = Logger.getLogger(StrictPrimitiveHandler.class);
   
   private ClassEntry sourceEntry;
   private Map attributeTypes;

   StrictPrimitiveHandler(ClassEntry sourceEntry)
   {
      // Init
      this.sourceEntry=sourceEntry;
      attributeTypes = new HashMap();
      attributeTypes.put("value",sourceEntry.getSourceClass());
   }

   public ClassEntry getSourceEntry()
   {
      return sourceEntry;
   }

   public boolean hasChanged()
   {
      return false;
   }

   public void update()
   {
   }

   public Map getAttributeTypes()
   {
      return new HashMap(attributeTypes);
   }

   public List getAttributeNames()
   {
      return new ArrayList(attributeTypes.keySet());
   }

   /**
    * Construct the primitive object with the given value.
    * @param marshalledValues The map that contains the 'value' attribute.
    */
   public Object newInstance(Map marshalledValues)
   {
      Class clazz = sourceEntry.getSourceClass();
      Object value = marshalledValues.get("value");
      if ( value == null )
         return null;
      value = TypeUtils.getTypeValue(value,clazz);
      logger.debug("strict primitive handler instantiates object: "+clazz+", value: "+value+" ("+value.getClass()+")");
      // First try the exceptional primitive types, who have no
      // string constructor.
      if ( (clazz.equals(char.class)) || (clazz.equals(Character.class)) )
         return new Character( ((Character) value).charValue() );
      if ( clazz.equals(Date.class) )
         return new Date( ((Date) value).getTime() );
      // Others we construct with a String constructor. Most number types
      // can be constructed with string representations easily.
      try
      {
         Constructor ctor = clazz.getConstructor( new Class[] { String.class } );
         return ctor.newInstance( new Object[] { value.toString() } );
      } catch ( Exception e ) {
         throw new StoreException("could not instantiate primitive type: "+sourceEntry+", with values: "+marshalledValues+", using string constructor.");
      }
   }

   /**
    * Always throws exception.
    */
   public Object getAttributeValue(Object obj, String attributeName)
   {
      if ( ! attributeName.equalsIgnoreCase("value") )
         throw new StoreException("primitive handler has only 'value' attribute, but queried: "+attributeName);
      return obj;
   }

   /**
    * Always returns exception.
    */
   public void setAttributeValue(Object obj, String attributeName, Object value)
   {
      throw new StoreException("object value cannot be set, objectclass: "+
            obj.getClass()+" name: "+attributeName+" on primitive handler for type: "+sourceEntry);
   }

}


