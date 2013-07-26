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
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class OneSuperclass implements ModifyableObject
{
   private String name;
   private int value;

   public OneSuperclass()
   {
   }

   public OneSuperclass(int number)
   {
      this.name="Name #"+number;
      this.value=number;
   }

   public void modify()
   {
      setName("Name is number "+value);
   }

   public String getName()
   {
      return name;
   }
   public void setName(String name)
   {
      this.name=name;
   }

   public int getValue()
   {
      return value;
   }
   public void setValue(int value)
   {
      this.value=value;
   }
}

