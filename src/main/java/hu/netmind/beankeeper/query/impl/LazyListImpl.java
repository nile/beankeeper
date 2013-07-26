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

import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.query.LazyList;
import hu.netmind.beankeeper.query.LazyListHooks;
import hu.netmind.beankeeper.query.QueryService;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.config.ConfigurationTracker;
import hu.netmind.beankeeper.schema.SchemaManager;
import java.util.*;
import java.io.Serializable;
import java.io.ObjectStreamException;
import org.apache.log4j.Logger;
import org.apache.commons.configuration.event.ConfigurationEvent;
import hu.netmind.beankeeper.db.Limits;
import hu.netmind.beankeeper.db.SearchResult;

/**
 * This list is a lazy-loading list. It receives a query statement, and
 * if an item is queried, the list runs the approriate search statement
 * and loads the referred item (and a given neighbourhood).
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class LazyListImpl extends AbstractList implements LazyList, Serializable
{
   private static Logger logger = Logger.getLogger(LazyListImpl.class);
   public int BATCH_SIZE = 30;
   public int BATCH_SIZE_LINEARMULTIPLIER = 3;
   public int BATCH_SIZE_MAX = 2500;
   public int MAX_JOINS = 16;
   
   private Map unmarshalledObjects = null;
   private LazyListHooks hooks = null;
   private QueryStatementList stmts = null;

   private List list;
   private boolean hasNext = false;
   private long[] stmtOffsets;
   private int offset = 0;
   private boolean initialized;
   private int linearCount = 0;
   private int linearLastIndex = -1;

   private QueryService queryService = null;
   private ClassTracker classTracker = null;
   private ConfigurationTracker config = null;
   private SchemaManager schemaManager = null;

   LazyListImpl(QueryService queryService, ClassTracker classTracker, ConfigurationTracker config,
         SchemaManager schemaManager, QueryStatementList stmts,Map unmarshalledObjects)
   {
      this.queryService=queryService;
      this.classTracker=classTracker;
      this.schemaManager=schemaManager;
      this.config=config;
      this.stmts=stmts;
      this.unmarshalledObjects=unmarshalledObjects;
      this.initialized=false;
      list = null;
      // Load config
      configurationReload();
   }

   public QueryStatementList getStmts()
   {
      return stmts;
   }

   /**
    * If this object is serialized then write a normal array list
    * instead of this one.
    */
   private Object writeReplace()
      throws ObjectStreamException
   {
      logger.debug("assembling serialized version of lazy list...");
      ArrayList serialized = new ArrayList(this);
      if ( logger.isDebugEnabled() )
         logger.debug("writing serialized form with size: "+serialized.size()+", this size: "+size());
      return serialized;
   }

   /**
    * Get the object on the given.
    */
   public Object get(int index)
   {
      initialize();
      updateList(index);
      return list.get(index-offset);
   }

   /**
    * Calculate the offset of a given statement.
    */
   public long getStmtOffset(int index)
   {
      // Initialize
      if ( stmtOffsets == null )
      {
         stmtOffsets = new long[stmts.size()+1];
         stmtOffsets[0]=0;
         for ( int i=0; i<stmts.size(); i++ )
            stmtOffsets[i+1]=-1;
      }
      // Calculate
      if ( logger.isDebugEnabled() )
         logger.debug("calculating size for stmt index: "+index+"/"+stmts.size());
      if ( stmtOffsets[index] < 0 )
      {
         for ( int i=0; i<index; i++ )
         {
            if ( stmtOffsets[i+1] >= 0 )
               continue;
            SearchResult result = queryService.find( (QueryStatement) stmts.get(i),
                  new Limits(0,0,-1),null);
            stmtOffsets[i+1]=stmtOffsets[i]+result.getResultSize();
         }
      }
      // Return
      logger.debug("returning size: "+stmtOffsets[index]);
      return stmtOffsets[index];
   }

   /**
    * Get the predicted size from database. This value <strong>never</strong>
    * changes.
    */
   public int size()
   {
      initialize();
      return (int) getStmtOffset(stmts.size());
   }

   public void refresh()
   {
      list = null;
      stmtOffsets=null;
      offset = 0;
      linearLastIndex = -1;
      linearCount = 0;
      hasNext = false;
   }

   /**
    * Try to load the entries around the given index.
    */
   private void updateList(int index)
   {
      // Initialize lazy list
      initialize();
      // Keep track of linear iterations
      if ( index == linearLastIndex+1 )
         linearCount++;
      else
         linearCount=0;
      linearLastIndex=index;
      // Check bounds
      if ( index < 0 )
         throw new ArrayIndexOutOfBoundsException("lazy list index given was: "+index+", thats < 0 and illegal.");
      if ( (list!=null) && (index<offset+list.size()) && (index>=offset) )
         return; // List has the desired item
      // Determine whether the update will get the next page linearly
      boolean nextPage = false;
      if ( list != null )
         nextPage = ( index == offset+list.size() );
      // Determine the startindex and size of the current select
      int batchSize = BATCH_SIZE;
      if ( list != null )
         batchSize = list.size();
      if ( linearCount >= batchSize )
         batchSize = batchSize * BATCH_SIZE_LINEARMULTIPLIER;
      else
         batchSize = BATCH_SIZE;
      if ( batchSize > BATCH_SIZE_MAX )
         batchSize = BATCH_SIZE_MAX;
      int startIndex = 0;
      if ( index < offset )
         startIndex = (index/batchSize)*batchSize;
      else
         startIndex = index;
      if ( startIndex < 0 )
         startIndex = 0;
      if ( logger.isDebugEnabled() )
         logger.debug("list index: "+index+", startindex: "+startIndex+", batchsize: "+batchSize+", linearcount was: "+linearCount);
      linearCount = 0;
      linearLastIndex = index;
      // Determine the statement to use for given index
      HashMap session = new HashMap();
      getStmtOffset(0); // Initialize offsets
      int stmtIndex = 0;
      long realOffset = 0;
      if ( hooks!=null )
         stmtIndex = hooks.preIndexing(session,startIndex);
      if ( stmtIndex < 0 )
      {
         stmtIndex = 0;
         while ( (stmtIndex<stmts.size()) && (stmtOffsets[stmtIndex]>=0) &&
               (stmtOffsets[stmtIndex+1]>=0) && (stmtOffsets[stmtIndex+1]<=startIndex) )
         {
            realOffset=stmtOffsets[stmtIndex];
            stmtIndex++;
         }
      }
      if ( stmtIndex >= stmts.size() )
         throw new ArrayIndexOutOfBoundsException("Tried to reach index: "+index+", but that was not available.");
      if ( logger.isDebugEnabled() )
         logger.debug("asked index is: "+index+", real offset: "+realOffset+", stmt index: "+stmtIndex+", start index: "+startIndex);
      // Now load the result set, which is possibly distributed in multiple
      // queries.
      offset = startIndex;
      List previousList = new ArrayList();
      if ( nextPage )
         previousList = new ArrayList(list);
      list = new ArrayList(batchSize);
      // Load the already unmarshalled objects. Load until
      // list vector is full, or out of result entries. Note: we load
      // plus one entry, so we know, that there is a next entry.
      boolean override = false;
      while ( (stmtIndex<stmts.size()) && ((list.size()<=batchSize)||(override)) )
      {
         if ( logger.isDebugEnabled() )
            logger.debug("lazy list statement iteration: "+stmtIndex+"/"+stmts.size()+
                  ", current size: "+list.size()+"/"+batchSize);
         override=false;
         // Get the query, and optimize it
         QueryStatement stmt = (QueryStatement) stmts.get(stmtIndex);
         Limits limits = new Limits((int) (startIndex-stmtOffsets[stmtIndex]),batchSize+1-list.size(),0);
         if ( limits.getOffset() < 0 )
            limits.setOffset(0);
         // Compute the total join count of the selected term
         int totalJoinCount = 0;
         SpecifiedTableTerm mainTerm = stmt.getSpecifiedTerm(
               (TableTerm) stmt.getSelectTerms().get(0));
         if ( mainTerm.getRelatedLeftTerms().size() > MAX_JOINS )
         {
            // Modify limits, so it does not select more rows than left
            // table terms. This ensures, that the select will not contain
            // more left table terms.
            if ( limits.getLimit() > MAX_JOINS )
            {
               limits.setLimit(MAX_JOINS+1);
               batchSize = list.size() + MAX_JOINS;
               logger.debug("adjusting limits, so max joins can be suited, new batch size: "+batchSize+", list size is: "+list.size());
            }
            // If there are many left table terms, then optimize this select
            // locally. This means, eliminate all left table terms, which will
            // not be used.
            stmt = optimizeLocalStatement(stmt,limits);
         }
         // Make query
         if ( hooks != null )
            stmt = hooks.preSelect(session,stmt,previousList,limits,new Limits(offset,batchSize+1,0));
         SearchResult result = queryService.find(stmt,limits,unmarshalledObjects);
         // Set for next iteration
         startIndex+=result.getResult().size();
         list.addAll(result.getResult());
         if ( hooks != null )
            override = hooks.postSelect(session,list,new Limits(offset,batchSize+1,0));
         // Postoperation adjustments
         if ( list.size() > batchSize )
         {
            logger.debug("list size "+list.size()+" is greater than batchsize "+batchSize+", iteration complete");
            // This means, that the list contained enough items for
            // the query, which means return only the exact results.
            // The size can not be determined now.
            list = list.subList(0,batchSize);
            hasNext = true;
            // List is ok for now, we don't need more
            if ( logger.isDebugEnabled() )
               logger.debug("updated list with full window, size is: "+list.size()+", index was: "+index);
            return;
         } else {
            hasNext = false;
            // Compute statement length if the length is not yet known
            if ( stmtOffsets[stmtIndex+1] < 0 )
            {
               if ( list.size() == 0 )
               {
                  logger.debug("list size was 0 for this iteration");
                  // There is no result. This can be caused by two things:
                  // - This statement is really 0 length
                  // - Statement interval ends before this start index is reached,
                  // but may contain items.
                  // Let's just calculate the sizes up until now
                  getStmtOffset(stmtIndex+1);
               } else if ( list.size() <= batchSize ) {
                  logger.debug("list size "+list.size()+" is not greater than batchsize "+batchSize);
                  // This means, that the list does not contain enough items,
                  // so the size can be exactly determined.
                  stmtOffsets[stmtIndex+1]=startIndex;
               }
            }
         }
         // Decrease cycle invariant function (in english: increase index)
         stmtIndex++;
      }
      if ( logger.isDebugEnabled() )
         logger.debug("updated list, size is: "+list.size()+", index was: "+index);
   }

   private void initialize()
   {
      if ( initialized )
         return;
      initialized=true;
      // If not yet queried, and the number of statements is big,
      // then do a pre-select, to determine which statements will be
      // used anyway, and drop those statements, which will have no
      // results.
      if ( stmts.size()>2 )
         optimizeStatements();
   }

   private Set getUsedTables(SearchResult result)
   {
      HashSet usedTableNames = new HashSet();
      for ( int i=0; i<result.getResult().size(); i++ )
      {
         Integer classId = new Integer(((Map)result.getResult().get(i)).get("classid").toString());
         ClassEntry entry = classTracker.getClassEntry(classId);
         ClassInfo info = classTracker.getClassInfo(entry);
         while ( (entry!=null) && (info.isStorable()) )
         {
            // Insert it's table name into the set
            String usedTableName = schemaManager.getTableName(entry);
            usedTableNames.add(usedTableName);
            // Goto super
            entry = entry.getSuperEntry();
            if ( entry != null )
               info = classTracker.getClassInfo(entry);
         }
      }
      return usedTableNames;
   }

   /**
    * The task of this method is to temporary remove those joined left table terms,
    * which will not be used in this page of the resultset.
    */
   private QueryStatement optimizeLocalStatement(QueryStatement stmt,Limits limits)
   {
      logger.debug("executing local optimization");
      if ( stmt.getMode() != QueryStatement.MODE_FIND )
         return stmt;
      // First, copy the statement, so it won't affect later use
      QueryStatement copyStmt = stmt.deepCopy();
      // Then modify the statement to select only the tables the statement
      // reaches with the given limits. (So remove all left terms for now)
      copyStmt.setMode(QueryStatement.MODE_VIEW);
      TableTerm selectTerm = (TableTerm) copyStmt.getSelectTerms().get(0);
      SpecifiedTableTerm specifiedSelectTerm = (SpecifiedTableTerm) copyStmt.getSpecifiedTerm(selectTerm);
      logger.debug("local optimization detects "+specifiedSelectTerm.getRelatedLeftTerms().size()+" left table terms.");
      specifiedSelectTerm.getRelatedLeftTerms().clear();
      copyStmt.getSelectTerms().set(0,new ReferenceTerm(selectTerm,
               "persistence_id","classid",new MathematicalPostfixFunction(">>","45")));
      copyStmt.setStaticRepresentation(copyStmt.getStaticRepresentation()+"LazyModifiedForTableSet");
      copyStmt.getOrderByList().clear();
      // Execute query
      SearchResult result = queryService.find(copyStmt,limits,null);
      // Get all the tables this query will reach
      Set usedTableNames = getUsedTables(result);
      if ( logger.isDebugEnabled() )
         logger.debug("determined, that used tables of given page are: "+usedTableNames);
      // Remove all left table terms from the original statement which are
      // not used.
      copyStmt = stmt.deepCopy();
      selectTerm = (TableTerm) copyStmt.getSelectTerms().get(0);
      specifiedSelectTerm = (SpecifiedTableTerm) copyStmt.getSpecifiedTerm(selectTerm);
      Iterator leftTermIterator = specifiedSelectTerm.getRelatedLeftTerms().iterator();
      while ( leftTermIterator.hasNext() )
      {
         SpecifiedTableTerm.LeftjoinEntry entry = (SpecifiedTableTerm.LeftjoinEntry) leftTermIterator.next();
         if ( ! usedTableNames.contains(entry.term.getTableName()) )
            leftTermIterator.remove();
      }
      copyStmt.setStaticRepresentation(null);
      return copyStmt;
   }

   /**
    * The task of this method is to permanently remove those statements from the
    * list which will yield an empty resultset on execution. Because removing
    * these statments from the set of statements this list contains will not
    * alter the result set itself, the statement can be removed.
    */
   private void optimizeStatements()
   {
      logger.debug("executing optimizing statement");
      // To do this, we do the following:
      // - Get the root statmenet from which all statements come, but which
      //   is based on a possibly non-storable class
      // - Change the main term to classes table, to select class ids
      // - Run the altered query which will not result in objects, but in
      //   classes that would be returned by the original.
      QueryStatement classIdsStmt = stmts.getRoot();
      if ( classIdsStmt.getMode()!=QueryStatement.MODE_FIND )
         return;
      TableTerm selectTerm = (TableTerm) classIdsStmt.getSelectTerms().get(0);
      SpecifiedTableTerm newTerm = new SpecifiedTableTerm("persistence_classes",selectTerm.getAlias());
      classIdsStmt.replace(selectTerm,newTerm,"id");
      classIdsStmt.setSelectTerms(new ArrayList());
      classIdsStmt.getSelectTerms().add(new ReferenceTerm(newTerm,"id","classid"));
      classIdsStmt.setOrderByList(null);
      classIdsStmt.setMode(QueryStatement.MODE_VIEW);
      classIdsStmt.setStaticRepresentation(classIdsStmt.getStaticRepresentation()+"LazyModifiedForTableSet");
      // Now, the statement is almost ready, but now we need to walk the
      // expression tree, and determine which conditions bound the classes
      fixBoundingTerms(classIdsStmt.getQueryExpression(),newTerm,true);
      // Execute statement
      SearchResult result = queryService.find(classIdsStmt, null,null);
      // Now assemble each and every table name that will be used by the
      // main selects. These are not only those returned by previous select,
      // but all 'supertables' of them also.
      Set usedTableNames = getUsedTables(result);
      if ( logger.isDebugEnabled() )
         logger.debug("determined, that used tables are: "+usedTableNames);
      // Now go through all statements, and determine whether they will be
      // used or not. If a statement has a main select term which is not
      // used, then delete the whole statement. Check the left terms too,
      // and remove all non-used left terms.
      Iterator stmtIterator = stmts.iterator();
      while ( stmtIterator.hasNext() )
      {
         QueryStatement stmt = (QueryStatement) stmtIterator.next();
         // Check main term
         TableTerm mainTerm = (TableTerm) stmt.getSelectTerms().get(0);
         SpecifiedTableTerm specifiedMainTerm = (SpecifiedTableTerm) stmt.getSpecifiedTerm(mainTerm);
         if ( ! usedTableNames.contains(mainTerm.getTableName()) )
         {
            // Main term is not used, so remove
            stmtIterator.remove();
            if ( logger.isDebugEnabled() )
               logger.debug("removing statement with main term: "+mainTerm);
         } else {
            // Main term is used, but check it's left terms
            Iterator leftTermIterator = specifiedMainTerm.getRelatedLeftTerms().iterator();
            while ( leftTermIterator.hasNext() )
            {
               SpecifiedTableTerm.LeftjoinEntry joinEntry = (SpecifiedTableTerm.LeftjoinEntry) leftTermIterator.next();
               if ( ! usedTableNames.contains(joinEntry.term.getTableName()) )
               {
                  // Left term will not be used ever, so remove it, but
                  // don't forget the expressions for this left term.
                  leftTermIterator.remove();
                  if ( logger.isDebugEnabled() )
                     logger.debug("removing left table '"+joinEntry.term+"' from statement with main term: "+mainTerm);
               }
            }
         }
      }
   }

   /**
    * Search for operators which are around the given term, and change
    * the term with which it is in relation to classid, if it's a bounding
    * relation.
    */
   private void fixBoundingTerms(Expression expr, TableTerm mainTerm, boolean positive)
   {
      boolean localPositive = true;
      for ( int i=0; i<expr.size(); i++ )
      {
         Object atom = expr.get(i);
         if ( atom instanceof String )
         {
            if ( ("or".equalsIgnoreCase((String) atom)) ||
                 ("and".equalsIgnoreCase((String) atom)) )
               localPositive = true;
            if ( "not".equalsIgnoreCase((String) atom) )
               localPositive = false;
         } else if ( atom instanceof Expression ) {
            fixBoundingTerms((Expression) atom, mainTerm, !(positive ^ localPositive));
         } else if ( atom instanceof TableTerm ) {
            if ( mainTerm.equals(atom) )
            {
               // Found the term, now if it's bounded, then change
               // the other term
               int direction = 0;
               if ( (i+1<expr.size()) && (
                        ("!=".equals(expr.get(i+1))) || ("<>".equals(expr.get(i+1))) ||
                        ("=".equals(expr.get(i+1)))  || ("in".equals(expr.get(i+1)))  ) )
                  direction = 1;
               if ( (i-1>=0) && (
                        ("!=".equals(expr.get(i-1))) || ("<>".equals(expr.get(i-1))) ||
                        ("=".equals(expr.get(i-1)))  || ("in".equals(expr.get(i-1))) ) )
                  direction = -1;
               if ( direction == 0 )
                  continue; // Potential bounding operator not found
               // Determine whether it's really bound
               if (!( (!((positive) ^ (localPositive))) ^ 
                    (("=".equals(expr.get(i+direction))) || ("in".equals(expr.get(i+direction))) ) ))
               {
                  Object term = expr.get(i+2*direction);
                  logger.debug("found bounding term: "+term);
                  if ( term instanceof ReferenceTerm )
                  {
                     // It's bound to another referenceterm or constantterm
                     ReferenceTerm newTerm = new ReferenceTerm((ReferenceTerm) term);
                     newTerm.setFunction(new MathematicalPostfixFunction(">>","45"));
                     expr.set(i+2*direction,newTerm);
                  } else if ( term instanceof ConstantTerm ) {
                     // Bound to constant term, so convert to classid
                     Object value = ((ConstantTerm)term).getValue();
                     if ( value instanceof Long )
                     {
                        expr.set(i+2*direction,new ConstantTerm(
                                 new Long(((Long)value).longValue()>>45)));
                     } else if ( value instanceof Collection ) {
                        ArrayList newValue = new ArrayList();
                        Iterator oldIterator = ((Collection)value).iterator();
                        while ( oldIterator.hasNext() )
                           newValue.add(new Long(((Long)oldIterator.next()).longValue()>>45));
                        expr.set(i+2*direction,new ConstantTerm(newValue));
                     }
                  }
               }
            }
         }
      }
   }

   public Iterator iterator()
   {
      return listIterator();
   }

   public ListIterator listIterator()
   {
      return new LazyListIterator();
   }
   
   public String toString()
   {
      return super.toString();
   }

   /**
    * This is not an optimal implementation, it only checks whether the list 
    * is smaller than the smallest window.
    */
   public boolean isIterationCheap()
   {
      return size() < BATCH_SIZE;
   }

   /**
    * This internal iterator avoids using size() method for
    * optimized iteration.
    */
   public class LazyListIterator implements ListIterator
   {
      public int currentIndex = 0;
      public boolean currentHasNext = false;

      public LazyListIterator()
      {
         // Pre-read if there is a first item
         currentIndex=0;
         if ( list == null )
         {
            try
            {
               get(currentIndex);
               currentHasNext = true;
            } catch ( IndexOutOfBoundsException e ) {
               currentHasNext = false;
            }
         } else {
            currentHasNext = offset+list.size() > 0;
         }
      }
      
      public void add(Object o)
      {
         throw new UnsupportedOperationException("LazyList does not support addition.");
      }

      public boolean hasNext()
      {
         return currentHasNext;
      }

      public boolean hasPrevious()
      {
         return currentIndex > 0; // All items have previous except first
      }

      public Object next()
      {
         Object result = get(currentIndex);
         currentIndex++;
         if ( currentIndex < offset+list.size() )
            currentHasNext=true;
         else
            currentHasNext=hasNext;
         return result;
      }
      
      public int nextIndex()
      {
         return currentIndex;
      }

      public Object previous()
      {
         Object result = get(currentIndex-1);
         currentIndex--;
         currentHasNext=true;
         return result;
      }

      public int previousIndex()
      {
         return currentIndex-1;
      }

      public void remove()
      {
         throw new UnsupportedOperationException("LazyList does not support remove.");
      }

      public void set(Object o)
      {
         throw new UnsupportedOperationException("LazyList does not support set.");
      }
   }

   public LazyListHooks getHooks()
   {
      return hooks;
   }
   public void setHooks(LazyListHooks hooks)
   {
      this.hooks=hooks;
   }

   public void configurationReload()
   {
      BATCH_SIZE = config.getConfiguration().
         getInt("beankeeper.list.batch_size",30);
      BATCH_SIZE_MAX = config.getConfiguration().
         getInt("beankeeper.list.batch_size_max",2500);
      BATCH_SIZE_LINEARMULTIPLIER = config.getConfiguration().
         getInt("beankeeper.list.batch_size_linearmultiplier",3);
      MAX_JOINS = config.getConfiguration().
         getInt("beankeeper.list.max_joins",16);
   }
}



