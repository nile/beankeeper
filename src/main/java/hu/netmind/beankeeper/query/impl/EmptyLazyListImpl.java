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

package hu.netmind.beankeeper.query.impl;

import java.util.AbstractList;
import hu.netmind.beankeeper.query.LazyList;
import hu.netmind.beankeeper.query.LazyListHooks;
import hu.netmind.beankeeper.parser.*;

/**
 * This is an empty lazy list.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class EmptyLazyListImpl extends AbstractList implements LazyList
{
   /**
    * Nop.
    */
   public Object get(int index)
   {
      return null;
   }

   /**
    * Empty list has zero size.
    */
   public int size()
   {
      return 0;
   }

   /**
    * Nop.
    */
   public LazyListHooks getHooks()
   {
      return null;
   }

   /**
    * Nop.
    */
   public void setHooks(LazyListHooks hooks)
   {
   }
   
   /**
    * Nop.
    */
   public QueryStatementList getStmts()
   {
      return null;
   }

   /**
    * Returns true.
    */
   public boolean isIterationCheap()
   {
      return true;
   }
}



