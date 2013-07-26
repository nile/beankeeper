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
 * This is a test dynamic object with static attributes too.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class DynamicObjectWithStaticAttributes extends HashMap implements DynamicObject
{
   private static HashMap attributes;

   private String ownAttribute;

   public static Map getPersistenceAttributeTypes(Class cl, String dynamicName)
   {
      return attributes;
   }

   public String getPersistenceDynamicName()
   {
      return null;
   }
   public void setPersistenceDynamicName(String name)
   {
      return;
   }

   public static void init()
   {
      attributes = new HashMap();
      attributes.put("name",String.class);
      attributes.put("index",Integer.class);
      attributes.put("male",Boolean.class);
   }

   public String getOwnAttribute()
   {
      return ownAttribute;
   }
   public void setOwnAttribute(String ownAttribute)
   {
      this.ownAttribute=ownAttribute;
   }

   static
   {
      init();
   }
}



