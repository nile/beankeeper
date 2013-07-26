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
import hu.netmind.beankeeper.common.StoreException;
import org.apache.log4j.Logger;

/**
 * An expression is basically a List, which contains the terms, operators
 * and subexpression in order. It can contain these objects (filled by
 * parser):
 * <ul>
 *    <li><strong>Expression</strong>: This indicates a sub-expression.</li>
 *    <li><strong>String</strong>: This is an operator (unary or binary).</li>
 *    <li><strong>ConstantTerm</strong>: Indicates a constant with the given value.</li>
 *    <li><strong>ReferenceTerm</strong>: A reference to a table's attribute.</li>
 * </ul>
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class Expression extends ArrayList
{
   private static Logger logger = Logger.getLogger(Expression.class);

   private boolean markedBlock;
  
   public Expression()
   {
      super();
   }

   public Expression(Expression expr)
   {
      super(expr);
      setMarkedBlock(expr.isMarkedBlock());
   }

   public Expression deepCopy()
   {
      // Copy object
      Expression result = new Expression();
      result.setMarkedBlock(isMarkedBlock());
      // Recursive copy of content
      for ( int i=0; i<size(); i++ )
      {
         Object value = get(i);
         if ( value instanceof Expression )
            result.add( ((Expression) value).deepCopy() );
         else
            result.add( value );
      }
      // Return deep copy
      return result;
   }

   public void replace(TableTerm oldTerm, TableTerm newTerm)
   {
      replace(oldTerm,newTerm,null);
   }

   public void replace(TableTerm oldTerm, TableTerm newTerm,String newColumnName)
   {
      for ( int i=0; i<size(); i++ )
      {
         Object value = get(i);
         if ( value instanceof Expression )
         {
            ((Expression) value).replace(oldTerm,newTerm,newColumnName);
         } else if ( (value instanceof ReferenceTerm) && (value.equals(oldTerm)) ) {
            ReferenceTerm newReferenceTerm = new ReferenceTerm(newTerm,
                     (newColumnName!=null)?newColumnName:(((ReferenceTerm)value).getColumnName()));
            set(i,newReferenceTerm);
            if ( logger.isDebugEnabled() )
               logger.debug("replaced: "+value+" with "+newTerm+"("+newColumnName+"): "+newReferenceTerm);
         }
      }
   }
   
   public TableTerm getTableTerm(String tableName)
   {
      TableTerm result = null;
      for ( int o=0; (o<size()) && (result==null); o++ )
      {
         Object term = get(o);
         if ( (term instanceof TableTerm) && 
               (tableName.equals(((TableTerm)term).getTableName())) )
            result = (TableTerm) term;
         if ( term instanceof Expression )
            result = ((Expression)term).getTableTerm(tableName);
      }
      return result;
   }

   public boolean isMarkedBlock()
   {
      return markedBlock;
   }
   public void setMarkedBlock(boolean markedBlock)
   {
      this.markedBlock=markedBlock;
   }

   public String toString()
   {
      StringBuffer result = new StringBuffer("[Expr:");
      for ( int i=0; i<size(); i++ )
         result.append(get(i).toString());
      result.append("]");
      return result.toString();
   }

}


