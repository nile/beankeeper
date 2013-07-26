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

import java.util.ArrayList;

/**
 * This refers to a table's attribute.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ReferenceTerm extends TableTerm
{
   private String columnName;
   private String columnAlias;
   private boolean id = false;
   private Function function;

   public boolean isId()
   {
      return id;
   }

   public void setId()
   {
      this.id=true;
   }

   
   public ReferenceTerm(ReferenceTerm source)
   {
      super(source);
      columnName = source.columnName;
      columnAlias = source.columnAlias;
      id = source.id;
      function = source.function;
   }

   public ReferenceTerm(TableTerm term, String columnName)
   {
      super(term);
      setColumnName(columnName);
   }
   
   public ReferenceTerm(TableTerm term, String columnName, String columnAlias)
   {
      super(term);
      setColumnName(columnName);
      setColumnAlias(columnAlias);
   }

   public ReferenceTerm(TableTerm term, String columnName, String columnAlias, Function function)
   {
      super(term);
      setColumnName(columnName);
      setColumnAlias(columnAlias);
      setFunction(function);
   }
   
   public ReferenceTerm(String tableName, String alias, String columnName)
   {
      super(tableName,alias);
      setColumnName(columnName);
   }
   
   public ReferenceTerm(String tableName, String alias, String columnName, Function function)
   {
      super(tableName,alias);
      setColumnName(columnName);
      setFunction(function);
   }
   
   public String getColumnName()
   {
      return columnName;
   }
   public void setColumnName(String columnName)
   {
      this.columnName=columnName;
   }

   public String getColumnAlias()
   {
      return columnAlias;
   }
   public void setColumnAlias(String columnAlias)
   {
      this.columnAlias=columnAlias;
   }

   public String getColumnFinalName()
   {
      if ( columnAlias == null )
         return columnName;
      else
         return columnAlias;
   }

   public String toString()
   {
      return super.toString()+"."+columnName+" ("+(columnAlias==null?"no alias":columnAlias)+
         ","+(function==null?"no function":function.toString())+")";
   }

   public Function getFunction()
   {
      return function;
   }
   public void setFunction(Function function)
   {
      this.function=function;
   }

}


