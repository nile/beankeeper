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

import hu.netmind.beankeeper.service.StoreContext;
import java.util.*;
import java.lang.reflect.Method;
import org.apache.log4j.Logger;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.model.*;

/**
 * Information and transformations on a given class.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class StrictDynamicHandler extends StrictStaticHandler
{
   private static Logger logger = Logger.getLogger(StrictDynamicHandler.class);

   private Map<String,Class> dynamicAttributes = null;;
   private Map<String,String> dynamicAttributeCase = null;

   private ClassTracker classTracker = null;
   
   StrictDynamicHandler(ClassTracker classTracker, ClassEntry sourceEntry)
   {
      // Init static attributes
      super(sourceEntry);
      this.classTracker=classTracker;
      update();
   }

   public void update()
   {
      dynamicAttributes = new HashMap<String,Class>();
      dynamicAttributeCase = new HashMap<String,String>();
      for ( Map.Entry<String,Class> entry : getPersistenceAttributeTypes(getSourceEntry()).entrySet() )
      {
         dynamicAttributeCase.put(entry.getKey().toLowerCase(),entry.getKey());
         dynamicAttributes.put(entry.getKey().toLowerCase(),entry.getValue());
      }
   }

   /**
    * Get the dynamic attributes types of an object.
    * @return The dynamic types map, or null, if object is not dynamic.
    */
   public static Map<String,Class> getPersistenceAttributeTypes(ClassEntry entry)
   {
      Method method = null;
      Class current = entry.getSourceClass();
      while ( (current != null) && (method==null) )
      {
         try
         {
            method = current.getDeclaredMethod("getPersistenceAttributeTypes",new Class[] { Class.class, String.class });
         } catch ( NoSuchMethodException e ) {
            // No problem
            if ( logger.isDebugEnabled() )
               logger.debug("given class had no getPersistenceAttributeTypes method for dynamic attributes: "+entry);
         }
         current = current.getSuperclass();
      }
      if ( method == null )
      {
         logger.debug("dyanmic attributes method not found in "+entry);
         return null; // If no such method, then this has no dynamic attrs
      }
      try
      {
         Map result = (Map) method.invoke(null,new Object[] { entry.getSourceClass(), entry.getDynamicName() });
         if ( logger.isDebugEnabled() )
            logger.debug("determined dynamic attributes for '"+entry+"': "+result);
         return result;
      } catch ( Exception e ) {
         throw new StoreException("error while getting dynamic attribute types",e);
      }
   }

   public Map getAttributeTypes()
   {
      Map result = new HashMap();
      result.putAll(super.getAttributeTypes());
      result.putAll(dynamicAttributes);
      return result;
   }

   public List getAttributeNames()
   {
      List result = new ArrayList();
      result.addAll(super.getAttributeNames());
      result.addAll(dynamicAttributes.keySet());
      return result;
   }

   public boolean hasChanged()
   {
      // Generate lower case attribute names map
      Map<String,Class> currentAttributes = new HashMap<String,Class>();
      for ( Map.Entry<String,Class> entry : getPersistenceAttributeTypes(getSourceEntry()).entrySet() )
         currentAttributes.put(entry.getKey().toLowerCase(),entry.getValue());
      // Compare
      return ! currentAttributes.equals(dynamicAttributes);
   }

   /**
    * Return whether given attribute is a static attribute, which has priority.
    */
   private boolean isStatic(String attributeName)
   {
      return super.getAttributeNames().contains(attributeName);
   }
      
   /**
    * Get the attribute value from a given object of this class and 
    * from given attribute.
    */
   public Object getAttributeValue(Object obj, String attributeName)
   {
      if ( isStatic(attributeName) )
         return super.getAttributeValue(obj,attributeName);
      return ((Map) obj).get(dynamicAttributeCase.get(attributeName.toLowerCase()));
   }

   /**
    * Set an object as value into object given.
    */
   public void setAttributeValue(Object obj, String attributeName, Object value)
   {
      // Handle static attribute
      if ( isStatic(attributeName) )
      {
         super.setAttributeValue(obj,attributeName,value);
         return;
      }
      // Get data
      attributeName = attributeName.toLowerCase();
      String realAttributeName = dynamicAttributeCase.get(attributeName);
      Class attributeClass = dynamicAttributes.get(attributeName);
      // Check if value is null. Remove the key, if the value is null.
      Map map = (Map) obj;
      Class valueClass = null;
      if ( value == null )
      {
         map.remove(realAttributeName);
         return;
      }
      valueClass = value.getClass();
      // Handle primitive types with exceptional circumstances
      if ( (attributeClass.equals(Character.class)) || (attributeClass.equals(char.class)) 
            && (valueClass.equals(String.class)) )
      {
         if ( ((String) value).length() > 0 )
            map.put(realAttributeName, new Character(((String) value).charAt(0)));
      }
      // Byte array
      else if ( attributeClass.equals(byte[].class) )
         map.put(realAttributeName,value);
      // Boolean
      else if ( (attributeClass.equals(Boolean.class) || attributeClass.equals(boolean.class))
            && ((valueClass.equals(Integer.class)) || 
               (valueClass.equals(Long.class))) )
      {
         map.put(realAttributeName, new Boolean(((Number) value).intValue() > 0));
      }
      // Easy primitive classes
      else if ( valueClass.equals(String.class) || 
           valueClass.equals(Date.class) || 
           valueClass.equals(Boolean.class)
         )
         map.put(realAttributeName,value);
      // Container classes
      else if ( (value instanceof Map) || (value instanceof List) )
         map.put(realAttributeName,value);
      // Not easy number classes
      else if ( value instanceof Number )
      {
         Number number = (Number) value;
         if ( attributeClass.equals(Byte.class) || attributeClass.equals(byte.class) )
            map.put(realAttributeName,new Byte(number.byteValue()));
         if ( attributeClass.equals(Double.class) || attributeClass.equals(double.class) )
            map.put(realAttributeName,new Double(number.doubleValue()));
         if ( attributeClass.equals(Float.class) || attributeClass.equals(float.class) )
            map.put(realAttributeName,new Float(number.floatValue()));
         if ( attributeClass.equals(Integer.class) || attributeClass.equals(int.class) )
            map.put(realAttributeName,new Integer(number.intValue()));
         if ( attributeClass.equals(Long.class) || attributeClass.equals(long.class) )
            map.put(realAttributeName,new Long(number.longValue()));
         if ( attributeClass.equals(Short.class) || attributeClass.equals(short.class) )
            map.put(realAttributeName,new Short(number.shortValue()));
      }
      // All other objects
      else {
         if ( (value!=null) || 
               (classTracker.getType(attributeClass)!=ClassTracker.ClassType.TYPE_PRIMITIVE) )
            map.put(realAttributeName,value);
      }
   }
}


