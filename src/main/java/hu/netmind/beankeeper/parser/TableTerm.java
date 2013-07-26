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

package hu.netmind.beankeeper.parser;

/**
 * This class represents a table identifier.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class TableTerm
{
   private String alias;
   private String tableName;

   public TableTerm(TableTerm source)
   {
      setAlias(source.getAlias());
      setTableName(source.getTableName());
   }

   public TableTerm(String tableName, String alias)
   {
      setTableName(tableName);
      setAlias(alias);
   }

   public String getName()
   {
      return (alias==null)?tableName:alias;
   }

   public String getAlias()
   {
      return alias;
   }
   public void setAlias(String alias)
   {
      this.alias=alias;
   }

   public String getTableName()
   {
      return tableName;
   }
   public void setTableName(String tableName)
   {
      this.tableName=tableName;
   }

   public int hashCode()
   {
      return (tableName+alias).hashCode();
   }

   public boolean equals(Object rhs)
   {
      if ( ! (rhs instanceof TableTerm) )
         return false;
      TableTerm t = (TableTerm) rhs;
      return (tableName.equals(t.tableName)) &&
         ( ((alias==null) && (t.alias==null)) || 
           ((alias!=null) && (alias.equals(t.alias))) );
   }

   public String toString()
   {
      return "[TableTerm: "+tableName+" ("+(alias==null?"no alias":("'"+alias+"'"))+")]";
   }

}

