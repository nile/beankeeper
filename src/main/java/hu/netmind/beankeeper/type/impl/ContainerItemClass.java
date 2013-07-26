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

package hu.netmind.beankeeper.type.impl;

import java.util.List;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.query.LazyList;
import hu.netmind.beankeeper.schema.SchemaManager;
import org.apache.log4j.Logger;

/**
 * Keeps track of a container's items' class.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class ContainerItemClass
{
   private static Logger logger = Logger.getLogger(ContainerItemClass.class);
   
   private String itemClassName;
   private SchemaManager schemaManager = null;

   public ContainerItemClass(SchemaManager schemaManager, String itemClassName)
   {
      this.schemaManager=schemaManager;
      this.itemClassName=itemClassName;
   }

   public void clear()
   {
      itemClassName=null;
   }

   public String getItemClassName()
   {
      return itemClassName;
   }

   public void updateItemClassName(List originalList, Class itemClass, boolean first)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("updating former class: "+itemClassName+", with: "+itemClass+", first: "+first);
      try
      {
         if ( itemClassName == null )
         {
            // There was no previous class
            itemClassName = itemClass.getName();
         } else {
            // If there was a previous class, then search for
            // a common superclass (this is at worst java.lang.Object,
            // but there is one).
            Class commonClass = Class.forName(itemClassName); // Start from old item class
            if ( (originalList!=null) && (originalList instanceof LazyList) &&
                  (((LazyList)originalList).getStmts().size()==1) &&
                  (first) )
            {
               // This means, that the original list has only one
               // real statement. If the itemlist class deteriorated
               // to Object, this is the chance to get back to a
               // normal class.
               QueryStatement stmt = (QueryStatement) ((LazyList)originalList).getStmts().get(0);
               String tableName = ((TableTerm)stmt.getSelectTerms().get(0)).getTableName();
               commonClass = schemaManager.getClassEntry(tableName).getSourceClass();
            }
            while ( ! commonClass.isAssignableFrom(itemClass) )
               commonClass = commonClass.getSuperclass();
            itemClassName = commonClass.getName();
         }
      } catch ( Throwable e ) {
         logger.warn("could not determine item class, previous: "+itemClassName+", new class: "+itemClass,e);
         itemClassName=Object.class.getName();
      }
   }

}


