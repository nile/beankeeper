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

package hu.netmind.beankeeper.performance;

/**
 * A class which has another as an attribute.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class OneAttribute implements ModifyableObject
{
   private SimpleObject simple;
   private String name;

   public OneAttribute()
   {
   }

   public OneAttribute(int number)
   {
      this.name="Name #"+number;
      this.simple = new SimpleObject(number);      
   }

   public void modify()
   {
      setName("Name: "+getName());
   }

   public String getName()
   {
      return name;
   }
   public void setName(String name)
   {
      this.name=name;
   }

   public SimpleObject getSimple()
   {
      return simple;
   }
   public void setSimple(SimpleObject simple)
   {
      this.simple=simple;
   }

}

