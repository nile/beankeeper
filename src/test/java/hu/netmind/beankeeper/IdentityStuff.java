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
 * A stuff with identity and a reference to authors.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class IdentityStuff
{
   private int identity;
   private Author author;
   
   public IdentityStuff()
   {
   }

   public IdentityStuff(int identity)
   {
      this.identity=identity;
   }

   public IdentityStuff(int identity, Author author)
   {
      this.identity=identity;
      this.author=author;
   }

   public int getIdentity()
   {
      return identity;
   }
   public void setIdentity(int identity)
   {
      this.identity=identity;
   }

   public Author getAuthor()
   {
      return author;
   }
   public void setAuthor(Author author)
   {
      this.author=author;
   }

   public int hashCode()
   {
      return identity;
   }

   public boolean equals(Object rhs)
   {
      if ( ! (rhs instanceof IdentityStuff) )
         return false;
      IdentityStuff i = (IdentityStuff) rhs;
      return (identity==i.identity) && author.equals(i.author);
   }
}


