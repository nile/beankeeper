/**
 * Copyright (C) 2007 NetMind Consulting Bt.
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

import java.util.*;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.db.Limits;

/**
 * Implement this interface to plug in hooks into the LazyList.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public interface LazyListHooks
{
   /**
    * Called before selecting the result set. Note: if method
    * modifies the statement, it should copy the statement first.
    * @param stmt The statement that is about to be executed.
    * @param previousList The previous page of the whole list, this
    * may be null, which means the previous page is not known.
    * @param limits The limits as determined by the automatic algorithm of
    * the sub-page to be selected.
    * @param pageLimits The limits of the page to be selected.
    */
   QueryStatement preSelect(Map session, QueryStatement stmt, List previousList, Limits limits, Limits pageLimits);

   /**
    * Called after selecting. Note: if the method should not modify
    * the result list size!
    */
   boolean postSelect(Map session, List list, Limits limits);

   /**
    * Called before selecting the statement index to use.
    */
   int preIndexing(Map session, int startIndex);
}


