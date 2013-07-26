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

import java.util.List;
import java.util.Map;

/**
 * This is a superclass with every kind of attributes.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class Superclass
{
   private int primitive;
   private List list;
   private Map map;
   private Superclass object;
   
   public Superclass()
   {
   }

   public Superclass(int primitive)
   {
      setPrimitive(primitive);
   }
   
   public int getPrimitive()
   {
      return primitive;
   }
   public void setPrimitive(int primitive)
   {
      this.primitive=primitive;
   }

   public List getList()
   {
      return list;
   }
   public void setList(List list)
   {
      this.list=list;
   }

   public Map getMap()
   {
      return map;
   }
   public void setMap(Map map)
   {
      this.map=map;
   }

   public Superclass getObject()
   {
      return object;
   }
   public void setObject(Superclass object)
   {
      this.object=object;
   }
      
}


