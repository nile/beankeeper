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
 * This is an article, a form of writing.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class Article extends Writing
{
   private String magazine;

   public Article()
   {
      super();
   }

   public Article(String title,String magazine)
   {
      super(title);
      setMagazine(magazine);
   }
   
   public String getMagazine()
   {
      return magazine;
   }
   public void setMagazine(String magazine)
   {
      this.magazine=magazine;
   }

   public int hashCode()
   {
      return (magazine==null?0:magazine.hashCode()) ^ super.hashCode();
   }

   public boolean equals(Object rhs)
   {
      if ( (rhs == null) || (!(rhs instanceof Article)) )
         return false;
      return ((magazine==null) && ((Article)rhs).magazine==null) || 
         ((magazine!=null) && (magazine.equals(((Article) rhs).magazine) && super.equals(rhs)));
   } 

   public String toString()
   {
      return "[Article: '"+getTitle()+",' magazine: '"+magazine+"']";
   }
   
}


