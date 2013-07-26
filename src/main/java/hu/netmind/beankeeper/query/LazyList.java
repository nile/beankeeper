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

package hu.netmind.beankeeper.query;

import hu.netmind.beankeeper.parser.QueryStatementList;
import java.util.List;

/**
 * Lazy lists do not usually contain their full content in memory. The hooks
 * help to customize the bahaviour of such a list.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface LazyList extends List
{
   /**
    * Get the hooks currently set.
    */
   LazyListHooks getHooks();

   /**
    * Set the lazy list hooks.
    */
   void setHooks(LazyListHooks hooks);
   
   /**
    * Get the statements that this list is a result of.
    */
   QueryStatementList getStmts();

   /**
    * Returns whether it is cheap (no database operations)
    * to iterate the list. This could have many reasons,
    * like list is small, result window large, etc.
    */
   boolean isIterationCheap();
}



