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
 * This is simply a subclass of referrer.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ReferrerSubclass extends Referrer
{
   private int subIdentity;
   
   public ReferrerSubclass()
   {
   }

   public ReferrerSubclass(int identity)
   {
      super(identity);
   }

   public ReferrerSubclass(int identity, int subIdentity)
   {
      super(identity);
      setSubIdentity(subIdentity);
   }

   public int getSubIdentity()
   {
      return subIdentity;
   }
   public void setSubIdentity(int subIdentity)
   {
      this.subIdentity=subIdentity;
   }

   public String toString()
   {
      return "[Refsub: "+getIdentity()+"]";
   }

   
}


