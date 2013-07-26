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

package hu.netmind.beankeeper;

import hu.netmind.beankeeper.model.DynamicObject;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a simple test dynamic object.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class DynamicObjectWithClass extends HashMap implements DynamicObject
{
   private static HashMap attributes;
   private transient String dynamicName;

   public static Map getPersistenceAttributeTypes(Class cl, String dynamicName)
   {
      if ( dynamicName == null )
         return (Map) attributes.get("none");
      else
         return (Map) attributes.get(dynamicName);
   }

   public String getPersistenceDynamicName()
   {
      return dynamicName;
   }
   public void setPersistenceDynamicName(String dynamicName)
   {
      this.dynamicName=dynamicName;
   }

   public static void init()
   {
      attributes = new HashMap();
      
      HashMap attrs = new HashMap();
      attrs.put("name",String.class);
      attrs.put("index",Integer.class);
      attrs.put("male",Boolean.class);
      attributes.put("none",attrs);

      attrs = new HashMap();
      attrs.put("model",String.class);
      attrs.put("doors",Integer.class);
      attributes.put("CarClass",attrs);
   }

   static
   {
      init();
   }
}



