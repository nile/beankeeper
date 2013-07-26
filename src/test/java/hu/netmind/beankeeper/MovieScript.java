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
 * A movie script is a form of screenplay, at least for test purposes.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class MovieScript extends ScreenPlay
{
   private int movieType;

   public MovieScript()
   {
      super();
   }

   public MovieScript(String title,int numberOfActors, int movieType)
   {
      super(title,numberOfActors);
      setMovieType(movieType);
   }
   
   public int getMovieType()
   {
      return movieType;
   }
   public void setMovieType(int movieType)
   {
      this.movieType=movieType;
   }

   public int hashCode()
   {
      return movieType ^ super.hashCode();
   }

   public boolean equals(Object rhs)
   {
      if ( (rhs == null) || (!(rhs instanceof MovieScript)) )
         return false;
      return movieType == (((MovieScript) rhs).movieType) && super.equals(rhs);
   } 
   
}


