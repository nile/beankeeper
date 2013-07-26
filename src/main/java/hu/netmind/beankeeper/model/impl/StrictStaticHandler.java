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

package hu.netmind.beankeeper.model.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import java.util.Date;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.model.*;

/**
 * This strict handler works on member fields of the given class.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class StrictStaticHandler implements StrictClassHandler
{
   private static Logger logger = Logger.getLogger(StrictStaticHandler.class);
   
   private ClassEntry sourceEntry;
   private HashMap attributeTypes;
   private HashMap attributes;

   StrictStaticHandler(ClassEntry sourceEntry)
   {
      // Init
      this.sourceEntry=sourceEntry;
      attributes = new HashMap();
      attributeTypes = new HashMap();
      // If this class is a dynamic class, it has no static attributes!
      if ( sourceEntry.getDynamicName() != null )
         return;
      // Add this class' attribute to sets
      Field[] fields = sourceEntry.getSourceClass().getDeclaredFields();
      for ( int i=0; i<fields.length; i++ )
      {
         if ( (! Modifier.isTransient(fields[i].getModifiers())) && 
               (! Modifier.isStatic(fields[i].getModifiers()))
            )
         {
            String attributeName = fields[i].getName().toLowerCase();
            fields[i].setAccessible(true);
            attributes.put(attributeName,fields[i]);
            attributeTypes.put(attributeName,fields[i].getType());
         }
      }
   }

   public static boolean hasStaticAttributes(ClassEntry entry)
   {
      StrictStaticHandler tmpHandler = new StrictStaticHandler(entry);
      List attributeNames = tmpHandler.getAttributeNames();
      for ( int i=0; i<attributeNames.size(); i++ )
         if ( ! ((String) attributeNames.get(i)).startsWith("persistence") )
            return true;
      return false;
   }

   public ClassEntry getSourceEntry()
   {
      return sourceEntry;
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
    * Get the attribute value from a given object of this class and 
    * from given attribute.
    */
   public Object getAttributeValue(Object obj, String attributeName)
   {
      Field field = (Field) attributes.get(attributeName.toLowerCase());
      if ( field == null )
         return null;
      try
      {
         return field.get(obj);
      } catch ( Throwable e ) {
         throw new StoreException("object value cannot be get, name: "+attributeName,e);
      }
   }

   /**
    * Set an object as value into object given.
    */
   public void setAttributeValue(Object obj, String attributeName, Object value)
   {
      Field field = (Field) attributes.get(attributeName.toLowerCase());
      if ( field == null )
         return;
      Class attrClass = (Class) attributeTypes.get(attributeName.toLowerCase());
      try
      {
         field.set(obj,TypeUtils.getTypeValue(value,attrClass));
      } catch ( Throwable e ) {
         Class valueClass = null;
         if ( value != null )
            valueClass = value.getClass();
         throw new StoreException("object value cannot be set, objectclass: "+
               obj.getClass()+" name: "+attributeName+" (class: "+attrClass+", value: "+value+" (class: "+valueClass+"))",e);
      }
   }

   public boolean hasChanged()
   {
      return false;
   }

   public void update()
   {
   }
}


