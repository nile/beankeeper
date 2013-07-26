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
 * A screenplay is a special form of writing.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ScreenPlay extends Writing
{
   private int numberOfActors;

   public ScreenPlay()
   {
      super();
   }

   public ScreenPlay(String title,int numberOfActors)
   {
      super(title);
      setNumberOfActors(numberOfActors);
   }
   
   public int getNumberOfActors()
   {
      return numberOfActors;
   }
   public void setNumberOfActors(int numberOfActors)
   {
      this.numberOfActors=numberOfActors;
   }

   public int hashCode()
   {
      return numberOfActors ^ super.hashCode();
   }

   public boolean equals(Object rhs)
   {
      if ( (rhs == null) || (!(rhs instanceof ScreenPlay)) )
         return false;
      return numberOfActors == (((ScreenPlay) rhs).numberOfActors) && super.equals(rhs);
   } 
   
}


