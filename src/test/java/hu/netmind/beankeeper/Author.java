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
 * Author.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class Author implements Comparable
{
   private long persistenceId;
   private String firstName;
   private String lastName;

   public Author()
   {
   }
   
   public Author(String firstName, String lastName)
   {
      setFirstName(firstName);
      setLastName(lastName);
   }

   public String getFirstName()
   {
      return firstName;
   }
   public void setFirstName(String firstName)
   {
      this.firstName=firstName;
   }

   public String getLastName()
   {
      return lastName;
   }
   public void setLastName(String lastName)
   {
      this.lastName=lastName;
   }

   public String toString()
   {
      return "["+lastName+", "+firstName+"]";
   }

   public int compareTo(Object obj)
   {
      return toString().compareTo(obj.toString());
   }

   public int hashCode()
   {
      return toString().hashCode();
   }

   public boolean equals(Object rhs)
   {
      if ( ! (rhs instanceof Author) )
         return false;
      Author a = (Author) rhs;
      return (firstName.equals(a.firstName)) && (lastName.equals(a.lastName));
   }

   public long getPersistenceId()
   {
      return persistenceId;
   }
   public void setPersistenceId(long persistenceId)
   {
      this.persistenceId=persistenceId;
   }

}


