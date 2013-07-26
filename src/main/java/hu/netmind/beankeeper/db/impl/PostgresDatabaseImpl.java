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

package hu.netmind.beankeeper.db.impl;

import java.sql.Types;
import java.util.List;
import hu.netmind.beankeeper.db.*;

/**
 * Postgres database implementation.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class PostgresDatabaseImpl extends GenericDatabase implements Database
{
   /**
    * Get the limit component of statement, if it can be expressed in
    * the current database with simple statement part.
    * @param limits The limits to apply.
    */
   protected String getLimitStatement(String statement, Limits limits, List types)
   {
      StringBuffer result = new StringBuffer(statement);
      if ( limits.getLimit() > 0 )
         result.append(" limit "+limits.getLimit());
      if ( limits.getOffset() > 0 )
         result.append(" offset "+limits.getOffset());
      return result.toString();
   }

   /**
    * Get the sql type for a class.
    */
   protected int getSQLType(Class type)
   {
      if ( byte[].class.equals(type) )
         return Types.LONGVARBINARY;
      return super.getSQLType(type);
   }

   /**
    * Get the class for an sql type. Override for blob type.
    */
   protected String getSQLTypeName(int sqltype)
   {
      switch ( sqltype )
      {
         case Types.BLOB:
         case Types.BINARY:
         case Types.VARBINARY:
         case Types.LONGVARBINARY:
            return "bytea";
         default:
            return super.getSQLTypeName(sqltype);
      }
   }

}



