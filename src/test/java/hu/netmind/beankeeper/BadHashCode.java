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

/**
 * This object simply refers to another object, but it overrides the
 * <code>hashCode()</code> and <code>equals()</code> methods to behave
 * incorrectly.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class BadHashCode implements Comparable
{
   private int identity;
   private BadHashCode ref;
   
   public BadHashCode()
   {
   }

   public int getIdentity()
   {
      return identity;
   }
   public void setIdentity(int identity)
   {
      this.identity=identity;
   }

   public BadHashCode(int identity)
   {
      this.identity=identity;
   }

   public BadHashCode getRef()
   {
      return ref;
   }
   public void setRef(BadHashCode ref)
   {
      this.ref=ref;
   }

   public int compareTo(Object rhs)
   {
      return ((BadHashCode) rhs).identity-identity;
   }
   
   public int hashCode()
   {
      return 111111;
   }

   public boolean equals(Object obj)
   {
      return true;
   }

   public String toString()
   {
      return "[BadHashCode: "+identity+"]";
   }

}



