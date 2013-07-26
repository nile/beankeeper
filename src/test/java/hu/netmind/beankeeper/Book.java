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
import java.io.Serializable;

/**
 * Book class.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class Book implements Serializable
{
   private String title;
   private String isbn;
   private List authors;
   private Author mainAuthor;

   public Book()
   {
   }

   public Book(String title, String isbn)
   {
      setTitle(title);
      setIsbn(isbn);
   }
   
   public String getTitle()
   {
      return title;
   }
   public void setTitle(String title)
   {
      this.title=title;
   }

   public String getIsbn()
   {
      return isbn;
   }
   public void setIsbn(String isbn)
   {
      this.isbn=isbn;
   }

   public List getAuthors()
   {
      return authors;
   }
   public void setAuthors(List authors)
   {
      this.authors=authors;
   }

   public Author getMainAuthor()
   {
      return mainAuthor;
   }
   public void setMainAuthor(Author mainAuthor)
   {
      this.mainAuthor=mainAuthor;
   }

   public String toString()
   {
      return "[Book: "+title+" ("+isbn+"), author: "+mainAuthor+"]";
   }

   public int hashCode()
   {
      return (""+title+isbn).hashCode();
   }

   public boolean equals(Object rhs)
   {
      if ( ! (rhs instanceof Book) )
         return false;
      Book b = (Book) rhs;
      return ( ((title==null) && (b.title==null)) || ((title!=null) && (title.equals(b.title))) ) && 
             ( ((isbn==null) && (b.isbn==null)) || ((isbn!=null) && (isbn.equals(b.isbn))) );
   }
}


