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
 * This class has a transient field.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class TransientAttrObject
{
   private String str;
   private int i;
   private transient String trans = null;

   public TransientAttrObject()
   {
   }
   
   public TransientAttrObject(String str, int i)
   {
      this.str=str;
      this.i=i;
   }
   
   public int hashCode()
   {
      return (str!=null?str.hashCode():0) ^ i;
   }

   public boolean equals(Object raw)
   {
      if ( ! (raw instanceof TransientAttrObject) )
         return false;
      TransientAttrObject t = (TransientAttrObject) raw;
      return (i==t.i) && ( ((str==null) && (t.str==null)) || 
            ((str!=null) && (str.equals(t.str))) );
   }

   public String getStr()
   {
      return str;
   }
   public void setStr(String str)
   {
      this.str=str;
   }

   public int getI()
   {
      return i;
   }
   public void setI(int i)
   {
      this.i=i;
   }

   public String getTrans()
   {
      return trans;
   }
   public void setTrans(String trans)
   {
      this.trans=trans;
   }


}


