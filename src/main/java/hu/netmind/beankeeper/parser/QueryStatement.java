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

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import org.apache.log4j.Logger;

/**
 * This class represents a query statement. A query statement
 * has four parts: A table name to select from, additional tables to
 * left join, a query expression, and order by parts.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class QueryStatement
{
   private static Logger logger = Logger.getLogger(QueryStatement.class);

   public static final int MODE_FIND = 1;
   public static final int MODE_VIEW = 2;
   
   private int mode = MODE_FIND;
   private List selectTerms = new ArrayList();
   private Set specifiedTerms = new HashSet();
   private Expression queryExpression;
   private List orderByList = new ArrayList();
   private TimeControl timeControl;
   private String originalStatement;   
   private String staticRepresentation;

   public QueryStatement(QueryStatement stmt)
   {
      setSelectTerms(stmt.getSelectTerms());
      setSpecifiedTerms(stmt.getSpecifiedTerms());
      setMode(stmt.getMode());
      setTimeControl(stmt.getTimeControl());
      setQueryExpression(stmt.getQueryExpression());
      setOrderByList(stmt.getOrderByList());
      setOriginalStatement(stmt.getOriginalStatement());
      setStaticRepresentation(stmt.getStaticRepresentation());
   }

   public QueryStatement deepCopy()
   {
      QueryStatement result = new QueryStatement(this);
      if ( getSelectTerms() != null )
         result.setSelectTerms(new ArrayList(getSelectTerms()));
      if ( getQueryExpression() != null )
         result.setQueryExpression(getQueryExpression().deepCopy());
      if ( getOrderByList() != null )
         result.setOrderByList(new ArrayList(getOrderByList()));
      result.setSpecifiedTerms(new HashSet());
      Iterator specifiedTermsIterator = specifiedTerms.iterator();
      while ( specifiedTermsIterator.hasNext() )
         result.getSpecifiedTerms().add(((SpecifiedTableTerm) 
                  specifiedTermsIterator.next()).deepCopy());
      return result;
   }

   public QueryStatement(List selectTerms, Expression queryExpression, List orderByList)
   {
      setSelectTerms(selectTerms);
      Set specifiedTerms = new HashSet();
      for ( int i=0; i<selectTerms.size(); i++ )
      {
         TableTerm term = (TableTerm) selectTerms.get(i);
         if ( term instanceof SpecifiedTableTerm )
            specifiedTerms.add(term);
         else
            specifiedTerms.add(new SpecifiedTableTerm(term));
      }
      setSpecifiedTerms(specifiedTerms);
      setQueryExpression(queryExpression);
      setOrderByList(orderByList);
   }

   public QueryStatement(TableTerm tableTerm, Expression queryExpression, List orderByList)
   {
      ArrayList selectTerms = new ArrayList();
      selectTerms.add(tableTerm);
      setSelectTerms(selectTerms);
      Set specifiedTerms = new HashSet();
      if ( tableTerm instanceof SpecifiedTableTerm )
         specifiedTerms.add(tableTerm);
      else
         specifiedTerms.add(new SpecifiedTableTerm(tableTerm));
      setSpecifiedTerms(specifiedTerms);
      setQueryExpression(queryExpression);
      setOrderByList(orderByList);
   }

   public QueryStatement(String tableName, Expression queryExpression, List orderByList)
   {
      this(new TableTerm(tableName,null),queryExpression,orderByList);
   }

   /**
    * Compute all tables in this statement.
    */
   public Set computeTables()
   {
      Set tables = new HashSet();
      Iterator specifiedTermsIterator = specifiedTerms.iterator();
      while ( specifiedTermsIterator.hasNext() )
      {
         SpecifiedTableTerm term = (SpecifiedTableTerm) specifiedTermsIterator.next();
         tables.add(term.getTableName());
         for ( int i=0; i<term.getRelatedLeftTerms().size(); i++ )
            tables.add(((SpecifiedTableTerm.LeftjoinEntry) 
                     term.getRelatedLeftTerms().get(i)).term.getTableName());
         for ( int i=0; i<term.getReferencedLeftTerms().size(); i++ )
            tables.add(((SpecifiedTableTerm.LeftjoinEntry) 
                     term.getReferencedLeftTerms().get(i)).term.getTableName());
      }
      return tables;
   }

   /**
    * Get the specified term for a given term.
    * @return The specified term for the given term, or an empty
    * specified term, if the given term is not found in the specifiedTerms
    * list.
    */
   public SpecifiedTableTerm getSpecifiedTerm(TableTerm source)
   {
      Iterator specifiedTermsIterator = specifiedTerms.iterator();
      while ( specifiedTermsIterator.hasNext() )
      {
         SpecifiedTableTerm term = (SpecifiedTableTerm) specifiedTermsIterator.next();
         if ( term.equals(source) )
            return term;
      }
      return new SpecifiedTableTerm(source);
   }

   /**
    * Replace all occurences of the given term with the new term.
    */
   public void replace(TableTerm oldTerm, TableTerm newTerm)
   {
      replace(oldTerm,new SpecifiedTableTerm(newTerm),null);
   }

   /**
    * Replace all occurences of the given term with the new term.
    * If the term is inserted into a reference term, then substitute
    * the column name too.
    */
   public void replace(TableTerm oldTerm, SpecifiedTableTerm newTerm, String newColumnName)
   {
      // First, replace in selected terms
      for ( int i=0; i<selectTerms.size(); i++ )
      {
         TableTerm term = (TableTerm) selectTerms.get(i);
         TableTerm replaceTerm = newTerm;
         if ( term.equals(oldTerm) )
         {
            if ( term instanceof ReferenceTerm )
               replaceTerm = new ReferenceTerm(newTerm,
                     (newColumnName!=null)?newColumnName:(((ReferenceTerm) term).getColumnName()),
                     ((ReferenceTerm) term).getColumnAlias());
            selectTerms.set(i,replaceTerm);
            logger.debug("replaced: "+term+" with "+newTerm+"("+newColumnName+")");
         }
      }
      // Replace in specified terms
      Iterator specifiedTermsIterator = new HashSet(specifiedTerms).iterator();
      while ( specifiedTermsIterator.hasNext() )
      {
         SpecifiedTableTerm term = (SpecifiedTableTerm) specifiedTermsIterator.next();
         if ( term.equals(oldTerm) )
         {
            specifiedTerms.remove(oldTerm);
            specifiedTerms.add(newTerm);
         }
      }
      // Replace in expression
      if ( getQueryExpression() != null )
         getQueryExpression().replace(oldTerm,newTerm,newColumnName);
      // Replace in order bys
      for ( int i=0; (getOrderByList()!=null) && (i<getOrderByList().size()); i++ )
      {
         OrderBy orderBy = (OrderBy) getOrderByList().get(i);
         if ( orderBy.getReferenceTerm().equals(oldTerm) )
            getOrderByList().set(i,new OrderBy(
                     new ReferenceTerm(newTerm,
                        newColumnName!=null?newColumnName:orderBy.getReferenceTerm().getColumnName()),
                        orderBy.getDirection()));
      }
   }

   public Expression getQueryExpression()
   {
      return queryExpression;
   }
   public void setQueryExpression(Expression queryExpression)
   {
      this.queryExpression=queryExpression;
   }

   public List getOrderByList()
   {
      return orderByList;
   }
   public void setOrderByList(List orderByList)
   {
      this.orderByList=orderByList;
   }

   public TimeControl getTimeControl()
   {
      return timeControl;
   }
   public void setTimeControl(TimeControl timeControl)
   {
      this.timeControl=timeControl;
   }

   public List getSelectTerms()
   {
      return selectTerms;
   }
   public void setSelectTerms(List selectTerms)
   {
      this.selectTerms=selectTerms;
   }

   public int getMode()
   {
      return mode;
   }
   public void setMode(int mode)
   {
      this.mode=mode;
   }

   public Set getSpecifiedTerms()
   {
      return specifiedTerms;
   }
   public void setSpecifiedTerms(Set specifiedTerms)
   {
      this.specifiedTerms=specifiedTerms;
   }

   public String getStaticRepresentation()
   {
      return staticRepresentation;
   }
   public void setStaticRepresentation(String staticRepresentation)
   {
      this.staticRepresentation=staticRepresentation;
   }

   public String getOriginalStatement()
   {
      return originalStatement;
   }
   public void setOriginalStatement(String originalStatement)
   {
      this.originalStatement=originalStatement;
   }
}


