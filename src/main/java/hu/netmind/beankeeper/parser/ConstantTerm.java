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

package hu.netmind.beankeeper.parser;

/**
 * A constant term has only a value.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ConstantTerm
{
   private Object value;
   private boolean id = false;

   public boolean isId()
   {
      return id;
   }

   public void setId()
   {
      this.id=true;
   }

   public ConstantTerm(Object value)
   {
      setValue(value);
   }

   public Object getValue()
   {
      return value;
   }
   public void setValue(Object value)
   {
      this.value=value;
   }

   public String toString()
   {
      if ( value == null )
         return "null";
      return value.toString();
   }
}


