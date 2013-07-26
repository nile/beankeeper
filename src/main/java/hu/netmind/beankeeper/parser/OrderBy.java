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
 * Represent an order by requirement of a select statement.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class OrderBy
{
   public static int ASCENDING = 1;
   public static int DESCENDING = 2;
  
   private ReferenceTerm reference;
   private int direction;

   public OrderBy()
   {
      direction = ASCENDING;
   }

   public OrderBy(ReferenceTerm reference, int direction)
   {
      setReferenceTerm(reference);
      setDirection(direction);
   }
   
   public ReferenceTerm getReferenceTerm()
   {
      return reference;
   }
   public void setReferenceTerm(ReferenceTerm reference)
   {
      this.reference=reference;
   }

   public int getDirection()
   {
      return direction;
   }
   public void setDirection(int direction)
   {
      this.direction=direction;
   }

   public String toString()
   {
      return "[OrderBy: "+reference.toString()+", "+direction+"]";
   }

   public int hashCode()
   {
      return (getReferenceTerm().getName()+"."+getReferenceTerm().getColumnName()).hashCode();
   }

   public boolean equals(Object o)
   {
      if ( ! (o instanceof OrderBy) )
         return false;
      OrderBy orderBy = (OrderBy) o;
      return (getReferenceTerm().getName().equals(orderBy.getReferenceTerm().getName())) &&
         (getReferenceTerm().getColumnName().equals(orderBy.getReferenceTerm().getColumnName()));
               
   }
}


