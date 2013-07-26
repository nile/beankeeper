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

package hu.netmind.beankeeper.db;

/**
 * A simple object which holds limits to a selection.
 * Offset means the index of first row returned after applying the 
 * conditions and ordering. Limit means to return at most the given amount
 * of rows. A limit of 0 means no limit.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class Limits
{
   private long offset;
   private long limit;
   private long size;

   public Limits(int offset,int limit, int size)
   {
      setOffset(offset);
      setLimit(limit);
      setSize(size);
   }

   public long getOffset()
   {
      return offset;
   }
   public void setOffset(long offset)
   {
      this.offset=offset;
   }

   public long getLimit()
   {
      return limit;
   }
   public void setLimit(long limit)
   {
      this.limit=limit;
   }

   public boolean isEmpty()
   {
      return (limit<=0);
   }

   public long getSize()
   {
      return size;
   }
   public void setSize(long size)
   {
      this.size=size;
   }

   public String toString()
   {
      return "[Limit: "+offset+"-"+limit+"]";
   }
}


