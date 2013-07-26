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

import java.util.List;
import java.util.Map;

/**
 * This class represent a search result from the database implementation.
 * The result contains two attributes. The resultSize attribute indicates
 * the total hit count without applying the limits given. The result
 * list contains the result rows in maps of attribute name-value pairs.
 * Note, that the result does not contain all rows, only those specified
 * with the limits parameter.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class SearchResult
{
   private long resultSize;
   private List<Map> result;

   public long getResultSize()
   {
      return resultSize;
   }
   public void setResultSize(long resultSize)
   {
      this.resultSize=resultSize;
   }

   public List<Map> getResult()
   {
      return result;
   }
   public void setResult(List<Map> result)
   {
      this.result=result;
   }
}


