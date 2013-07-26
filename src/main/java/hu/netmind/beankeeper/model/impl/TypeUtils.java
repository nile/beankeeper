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

import java.util.*;

/**
 * Type, class and value conversion methods.
 */
public class TypeUtils
{
   /**
    * Convert a value of a type to a given target type.
    */
   public static Object getTypeValue(Object value, Class attrClass)
   {
      Class valueClass = null;
      // Check if value is null, note that primitive types will
      // have a sane default if confronted with null
      if ( value == null )
      {
         if ( attrClass.equals(byte.class) )
            return new Byte((byte) 0);
         else if ( attrClass.equals(double.class) )
            return new Double(0);
         else if ( attrClass.equals(float.class) )
            return new Float(0);
         else if ( attrClass.equals(int.class) )
            return new Integer(0);
         else if ( attrClass.equals(long.class) )
            return new Long(0);
         else if ( attrClass.equals(short.class) )
            return new Short((short) 0);
         else if ( attrClass.equals(boolean.class) )
            return new Boolean(false);
         else if ( attrClass.equals(char.class) )
            return new Character('\u0000');
         return null;
      }
      valueClass = value.getClass();
      // Handle primitive types with exceptional circumstances
      if ( (attrClass.equals(Character.class)) || (attrClass.equals(char.class)) 
            && (valueClass.equals(String.class)) )
      {
         if ( ((String) value).length() > 0 )
            return new Character(((String) value).charAt(0));
      }
      // Byte array
      else if ( attrClass.equals(byte[].class) )
         return value;
      // Boolean
      else if ( (attrClass.equals(Boolean.class) || attrClass.equals(boolean.class))
            && ((valueClass.equals(Integer.class)) || 
               (valueClass.equals(Long.class))) )
      {
         return new Boolean(((Number) value).intValue() > 0);
      }
      // Easy primitive classes
      else if ( valueClass.equals(String.class) || 
           valueClass.equals(Date.class) || 
           valueClass.equals(Boolean.class)
         )
         return value;
      // Container classes
      else if ( (value instanceof Map) || (value instanceof List) )
         return value;
      // Not easy number classes
      else if ( value instanceof Number )
      {
         Number number = (Number) value;
         if ( attrClass.equals(Byte.class) || attrClass.equals(byte.class) )
            return new Byte(number.byteValue());
         if ( attrClass.equals(Double.class) || attrClass.equals(double.class) )
            return new Double(number.doubleValue());
         if ( attrClass.equals(Float.class) || attrClass.equals(float.class) )
            return new Float(number.floatValue());
         if ( attrClass.equals(Integer.class) || attrClass.equals(int.class) )
            return new Integer(number.intValue());
         if ( attrClass.equals(Long.class) || attrClass.equals(long.class) )
            return new Long(number.longValue());
         if ( attrClass.equals(Short.class) || attrClass.equals(short.class) )
            return new Short(number.shortValue());
      }
      // All other objects
      return value;
   }

}
