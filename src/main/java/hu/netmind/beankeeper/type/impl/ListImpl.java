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

import java.util.*;
import hu.netmind.beankeeper.query.LazyList;
import hu.netmind.beankeeper.query.LazyListHooks;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.object.ObjectTracker;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.query.QueryService;
import hu.netmind.beankeeper.type.event.*;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.db.Limits;
import hu.netmind.beankeeper.db.Database;
import hu.netmind.beankeeper.db.SearchResult;
import hu.netmind.beankeeper.serial.Serial;
import hu.netmind.beankeeper.schema.SchemaManager;
import org.apache.log4j.Logger;

/**
 * Custom list implementation based on lazy lists. List is <strong>not</strong>
 * thread-safe.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ListImpl extends AbstractList implements Container, LazyListHooks
{
   private static Logger logger = Logger.getLogger(ListImpl.class);
   private static final long INTERVAL = Integer.MAX_VALUE;
   private static final long INT_BITS = 32;
   
   private List originalList;
   private TimeControl originalTimeControl;
   private ClassInfo parentInfo;
   private Object parent;
   private String parentAttributeName;
   private Long lastSerial;
   private ContainerItemClass itemClass;

   private ArrayList changes; // Change entries of index layout
   private LinkedList addedItems;
   private LinkedList removedItems;
   private boolean cleared = false;

   private ClassTracker classTracker = null; // Injected
   private ObjectTracker objectTracker = null; // Injected
   private Database database = null; // Injected
   private QueryService queryService = null; // Injected
   private TransactionTracker transactionTracker = null; // Injected
   private SchemaManager schemaManager = null; // Injected
   
   /**
    * Initialize with a default list.
    */
   public void init(ClassInfo classInfo, Object obj, 
         String attributeName, String itemClassName, Long lastSerial, TimeControl timeControl)
   {
      this.originalList=null;
      this.originalTimeControl=new TimeControl(timeControl);
      this.parentInfo=classInfo;
      this.parent=obj;
      this.parentAttributeName=attributeName;
      this.lastSerial=lastSerial;
      this.itemClass=new ContainerItemClass(schemaManager,itemClassName);
      // Model
      reload();
   }

   public Object getParent()
   {
      return parent;
   }

   public String getItemClassName()
   {
      return itemClass.getItemClassName();
   }

   public void reload()
   {
      // Clear model
      modCount++;
      changes = new ArrayList();
      addedItems = new LinkedList();
      removedItems = new LinkedList();
      cleared = false;
      // Reload list
      if ( getItemClassName() == null )
      {
         originalList = new ArrayList();
      } else {
         originalList = queryService.find(
               "find item("+getItemClassName()+")"+
               " where persistence_container_parent("+parentInfo.getSourceEntry().getFullName()+")"+
               "."+parentAttributeName+" contains item and persistence_container_parent = ?",
               new Object[] { parent }, originalTimeControl,null);
         // Modify the select statements.
         String subTableName = schemaManager.getTableName(
               parentInfo.getAttributeClassEntry(parentAttributeName),parentAttributeName);
         LazyList lazy = (LazyList) originalList;
         lazy.setHooks(this);
         for ( int i=0; (lazy.getStmts()!=null) && (i<lazy.getStmts().size()); i++ )
         {
            QueryStatement stmt = (QueryStatement) lazy.getStmts().get(i);
            // First search for the table term of the subtable
            Expression expr = stmt.getQueryExpression();
            TableTerm subTableTerm = expr.getTableTerm(subTableName);
            // Add this term to selected terms
            stmt.getSelectTerms().add(new ReferenceTerm(subTableTerm,"container_index"));
            // Add the correct order by
            stmt.getOrderByList().add(0,new OrderBy(new ReferenceTerm(subTableTerm,"container_index"),OrderBy.ASCENDING));
            // Correct static string
            stmt.setStaticRepresentation(stmt.getStaticRepresentation()+" and index");
         }
      }
   }

   public int preIndexing(Map session, int startIndex)
   {
      // Always use all statements
      return 0;
   }
   
   /**
    * This method generates a sub-page select statement which will select
    * the next items. If this is a linear iteration, then we already
    * know the previous index to select from, if not, we have to
    * select the list 'index' just before the page start.
    */
   public QueryStatement preSelect(Map session, QueryStatement stmt, List previousList, 
         Limits limits, Limits pageLimits)
   {
      QueryStatement result = stmt;
      if ( logger.isDebugEnabled() )
         logger.debug("list impl pre select is running, limits is: "+limits+", page limits: "+pageLimits);
      // Determining the index this query should start with.
      Long lastIndex = (Long) session.get("lastIndex");
      if ( (pageLimits.getOffset() > 0) && (lastIndex==null) )
      {
         // If the offset is greater than null, then we need
         // to determine the last item's index from the previous
         // page.
         if ( previousList.size() > 0 )
         {
            // We're lucky, this is a linear iteration, so we can
            // determine the last index by checking the last item
            // in the previous page
            lastIndex = (Long) ((Map) previousList.get(previousList.size()-1)).get("container_index");
         } else {
            if ( logger.isDebugEnabled() )
               logger.debug("preselect is running pre-selecting query.");
            // Well, no luck here. We must select the last index.
            String subTableName = schemaManager.getTableName(
                  parentInfo.getAttributeClassEntry(parentAttributeName),parentAttributeName);
            TableTerm subTableTerm = new TableTerm(subTableName,null);
            ArrayList selectTerms = new ArrayList();
            selectTerms.add(new ReferenceTerm(subTableTerm,"container_index"));
            Expression expr = new Expression();
            expr.add(new ReferenceTerm(subTableTerm,"persistence_id"));
            expr.add("=");
            expr.add(new ConstantTerm(objectTracker.getIdentifier(parent)));
            expr.add("and");
            originalTimeControl.apply(expr,subTableTerm);
            ArrayList orderBys = new ArrayList();
            orderBys.add(new OrderBy(new ReferenceTerm(subTableTerm,"container_index"),OrderBy.ASCENDING));
            QueryStatement indexStmt = new QueryStatement(selectTerms,expr,orderBys);
            Transaction tx = transactionTracker.getTransaction(TransactionTracker.TX_REQUIRED);
            tx.begin();
            try
            {
               SearchResult qresult = database.search(tx,indexStmt,
                     new Limits((int)(pageLimits.getOffset()-1),1,0));
               lastIndex = (Long) ((Map) qresult.getResult().get(0)).get("container_index");
            } finally {
               tx.commit();
            }
         }
         if ( logger.isDebugEnabled() )
            logger.debug("pre select determined previous index to be: "+lastIndex);
         session.put("lastIndex",lastIndex);
      }
      // Insert explicit conditions on the 'index' field, if the last index
      // is known (because then we got to select only greater indexes).
      // If the index is not given, the database will start from the lowest
      // index automatically, because of the sql order by.
      if ( lastIndex != null )
      {
         result = stmt.deepCopy();
         result.setStaticRepresentation(result.getStaticRepresentation()+" and index > "+lastIndex);
         // Get the index's reference term
         ReferenceTerm indexTerm = ((OrderBy) result.getOrderByList().get(0)).getReferenceTerm();
         // Insert new condition
         Expression expr = result.getQueryExpression();
         expr.add("and");
         expr.add(indexTerm);
         expr.add(">");
         expr.add(new ConstantTerm(lastIndex));
      }
      // Offset is always 0, because we alter the select to include
      // just the target items
      limits.setOffset(0);
      // Always get full result set, so limit is always on max. This
      // is because we can not know in advance, whether the following
      // sub-page contains items which by ordering belong to the
      // final list or not. So we have to select all sub-pages
      // with max count, and order and truncate them in the end.
      limits.setLimit(pageLimits.getLimit());
      // Return statement
      return result;
   }

   /**
    * The result list is now unordered, because the result was assembled
    * from possibly multiple query results. So we need to order by index.
    */
   public boolean postSelect(Map session, List list, Limits limits)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("post select running, size is: "+list.size()+", limits: "+limits);
      // Order the result by index ascending
      Collections.sort(list,new Comparator()
            {
               public int compare(Object o1, Object o2)
               {
                  long diff = ((Long) ((Map) o1).get("container_index")).longValue() -
                         ((Long) ((Map) o2).get("container_index")).longValue();
                  if ( diff < 0 )
                     return -1;
                  else if ( diff > 0 )
                     return +1;
                  return 0;
               }
            } );
      // At this point, the list can be larger than specified by limits,
      // because it is possibly assembled from multiple sub-pages which
      // were all selected with batchsize. We simply truncate the list.
      // The items at the end are sure to be _not_ part of the final
      // result anyway.
      if ( limits.getLimit() < list.size() )
         list.subList((int)limits.getLimit(),list.size()).clear();
      // Override always
      return true;
   }

   /**
    * Returns whether the container changes internally since last save().
    */

   public boolean hasChanged()
   {
      return (addedItems.size()>0) || (removedItems.size()>0) || (cleared);
   }

   /**
    * Save the container to database.
    */
   public void save(Transaction transaction, Long currentSerial, Set waitingObjects, 
         Set saveTables, Set removeTables, List events)
   {
      ClassEntry attributeClassEntry = parentInfo.getAttributeClassEntry(parentAttributeName);
      String attributeTableName = schemaManager.getTableName(attributeClassEntry,parentAttributeName);
      // If the list was cleared before, then clear the current values
      if ( cleared )
      {
         logger.debug("clearing list...");
         HashMap keyAttributes = new HashMap();
         keyAttributes.put("persistence_id",objectTracker.getIdentifier(parent));
         keyAttributes.put("persistence_end",Serial.getMaxSerial().getValue());
         keyAttributes.put("persistence_txend",Serial.getMaxSerial().getValue());
         HashMap removeAttributes = new HashMap();
         removeAttributes.put("persistence_txend",currentSerial);
         removeAttributes.put("persistence_txendid",transaction.getSerial());
         database.save(transaction, attributeTableName, keyAttributes,removeAttributes);
         removeTables.add(attributeTableName);
         // Notify listeners
         events.add(new ClearedContainerEvent(parent,parentAttributeName));
      }
      // Remove removed items first
      if ( logger.isDebugEnabled() )
         logger.debug("removing "+removedItems.size()+" items from list...");
      Iterator removedItemsIterator = removedItems.iterator();
      while ( removedItemsIterator.hasNext() )
      {
         ObjectWrapper wrapper = (ObjectWrapper) removedItemsIterator.next();
         Object obj = wrapper.getObject();
         // Remove object from the list
         HashMap keyAttributes = new HashMap();
         keyAttributes.put("persistence_id",objectTracker.getIdentifier(parent));
         keyAttributes.put("persistence_end",Serial.getMaxSerial().getValue());
         keyAttributes.put("persistence_txend",Serial.getMaxSerial().getValue());
         keyAttributes.put("container_index",new Long(wrapper.getOriginalIndex()));
         HashMap removeAttributes = new HashMap();
         removeAttributes.put("persistence_txend",currentSerial);
         removeAttributes.put("persistence_txendid",transaction.getSerial());
         database.save(transaction, attributeTableName, keyAttributes,removeAttributes);
         removeTables.add(attributeTableName);
         // Notify listeners
         events.add(new RemovedItemEvent(parent,parentAttributeName,obj));
      }
      // Re-index list items. This only occurs, if an item addition
      // would get the index of an already existing item. In this case, 
      // all subsequent items are re-indexed. So find the first reindexing
      // point
      ChangeEntry reindexEntry = null;
      int changeIndex = 0;
      while ( (changeIndex<changes.size()) && (reindexEntry==null) )
      {
         ChangeEntry current = (ChangeEntry) changes.get(changeIndex);
         if ( (current.getChange()==ChangeEntry.ADDED) && (current.getReindex()) )
            reindexEntry = current;
         changeIndex++;
      }
      if ( reindexEntry != null )
      {
         changeIndex--; // Back one step, if found
         // Found a reindex entry
         if ( logger.isDebugEnabled() )
            logger.debug("re-indexing range, change: "+reindexEntry);
         // All old items' indexes between this change and the next 
         // are to be re-indexed. Get all the entries after the reindexing
         // item.
         TableTerm subTableTerm = new TableTerm(attributeTableName,null);
         ArrayList selectTerms = new ArrayList();
         selectTerms.add(new ReferenceTerm(subTableTerm,"container_index"));
         selectTerms.add(new ReferenceTerm(subTableTerm,"value"));
         Expression expr = new Expression();
         expr.add(new ReferenceTerm(subTableTerm,"persistence_id"));
         expr.add("=");
         expr.add(new ConstantTerm(objectTracker.getIdentifier(parent)));
         expr.add("and");
         expr.add(new ReferenceTerm(subTableTerm,"container_index"));
         expr.add(">=");
         expr.add(new ConstantTerm(new Long(reindexEntry.getItem().getOriginalIndex())));
         expr.add("and");
         originalTimeControl.apply(expr,subTableTerm);
         ArrayList orderBys = new ArrayList();
         orderBys.add(new OrderBy(new ReferenceTerm(subTableTerm,"container_index"),OrderBy.ASCENDING));
         QueryStatement stmt = new QueryStatement(selectTerms,expr,orderBys);
         SearchResult result = database.search(transaction,stmt,null);
         // Go through each item entry and modify it (first close the old
         // one, and create a new entry with new index)
         // Note, that because this is low-level, the paging mechanism
         // is questionable. We depend here on the fetchsize parameter
         // of the jdbc driver, so it is assumed, that large result sets
         // can be read.
         // The cycle reads the resultset, and the changeset in parallel,
         // each is pre-read.
         // Implementation note: On first iteration, the reindexEntry is
         // always selected, because it has the lowest index.
         long newIndex = reindexEntry.getItem().getOriginalIndex()+INTERVAL-1;
         ChangeEntry currentEntry = reindexEntry;
         int resultIndex = 0;
         Map currentResult = null;
         if ( resultIndex < result.getResult().size() )
            currentResult = (Map) result.getResult().get(resultIndex);
         while ( (currentResult!=null) || (currentEntry!=null) )
         {
            // Determine whether the next item is a change, or a database
            // result.
            if ( (currentResult!=null) && 
                  ( (currentEntry==null) || 
                    ( ((Long)currentResult.get("container_index")).longValue() 
                      < currentEntry.getItem().getOriginalIndex() ) ))
            {
               // The database result entry has a smaller index than
               // the change (if there is a change), this means, the
               // next index belongs to this database entry.
               Long index = (Long) currentResult.get("container_index");
               Long value = (Long) currentResult.get("value");
               // Close last occurence of index
               HashMap keyAttributes = new HashMap();
               keyAttributes.put("persistence_id",objectTracker.getIdentifier(parent));
               keyAttributes.put("persistence_end",Serial.getMaxSerial().getValue());
               keyAttributes.put("persistence_txend",Serial.getMaxSerial().getValue());
               keyAttributes.put("container_index",index);
               HashMap removeAttributes = new HashMap();
               removeAttributes.put("persistence_txend",currentSerial);
               removeAttributes.put("persistence_txendid",transaction.getSerial());
               database.save(transaction, attributeTableName, keyAttributes,removeAttributes);
               removeTables.add(attributeTableName);
               // Insert new version
               HashMap itemAttributes = new HashMap();
               itemAttributes.put("persistence_id",objectTracker.getIdentifier(parent));
               itemAttributes.put("persistence_start",Serial.getMaxSerial().getValue());
               itemAttributes.put("persistence_end",Serial.getMaxSerial().getValue());
               itemAttributes.put("persistence_txendid",new Long(0));
               itemAttributes.put("persistence_txstartid",transaction.getSerial());
               itemAttributes.put("persistence_txstart",currentSerial);
               itemAttributes.put("persistence_txend",Serial.getMaxSerial().getValue());
               itemAttributes.put("value",value);
               itemAttributes.put("container_index",new Long(newIndex));
               database.insert(transaction, attributeTableName, itemAttributes);
               saveTables.add(attributeTableName);
               // Pre-read the next result
               resultIndex++;
               if ( resultIndex < result.getResult().size() )
                  currentResult = (Map) result.getResult().get(resultIndex);
               else
                  currentResult=null;
            } else {
               // This branch is active if an ADDED (yet non existing)
               // item is next. In this case we must re-index the entry
               // which will be already created with this new index later.
               currentEntry.getItem().setOriginalIndex(newIndex);
               // Pre-read next entry
               changeIndex++;
               while ( (changeIndex<changes.size()) &&
                     ( ((ChangeEntry)changes.get(changeIndex)).getChange()!=ChangeEntry.ADDED) )
                  changeIndex++;
               if ( changeIndex < changes.size() )
                  currentEntry = (ChangeEntry) changes.get(changeIndex);
               else
                  currentEntry=null;
            }
            // Increment index
            newIndex += INTERVAL;
         }
      }
      // Add added items
      if ( logger.isDebugEnabled() )
         logger.debug("adding "+addedItems.size()+" items to list...");
      Iterator addedItemsIterator = addedItems.iterator();
      while ( addedItemsIterator.hasNext() )
      {
         ObjectWrapper wrapper = (ObjectWrapper) addedItemsIterator.next();
         Object obj = wrapper.getObject();
         // Item does not exist then put it in the save list
         if ( ! objectTracker.exists(obj) )
            waitingObjects.add(objectTracker.getWrapper(obj));
         // Add item
         HashMap itemAttributes = new HashMap();
         itemAttributes.put("persistence_id",objectTracker.getIdentifier(parent));
         itemAttributes.put("persistence_start",Serial.getMaxSerial().getValue());
         itemAttributes.put("persistence_end",Serial.getMaxSerial().getValue());
         itemAttributes.put("persistence_txendid",new Long(0));
         itemAttributes.put("persistence_txstartid",transaction.getSerial());
         itemAttributes.put("persistence_txstart",currentSerial);
         itemAttributes.put("persistence_txend",Serial.getMaxSerial().getValue());
         itemAttributes.put("container_index",new Long(wrapper.getOriginalIndex()));
         itemAttributes.put("value",objectTracker.getIdentifier(obj));
         database.insert(transaction, attributeTableName, itemAttributes);
         saveTables.add(attributeTableName);
         // Notify listeners
         events.add(new AddedItemEvent(parent,parentAttributeName,obj));
      }
      // Reload the changed list. Note, that the list should not
      // be referenced until the Store.save() operation completes, because
      // the list currently might contain nonexisting objects, which will
      // be inserted _after_ this code.
      originalTimeControl = new TimeControl(currentSerial,transaction.getSerial(),true);
      lastSerial = currentSerial;
   }

   /**
    * Get the serial number of last modification.
    */
   public Long getLastSerial()
   {
      return lastSerial;
   }

   public boolean retainAll(Object c)
   {
      return retainAll( (Collection) c );
   }

   public boolean addAll(Object c)
   {
      return addAll( (Collection) c );
   }

   /*
    * List implementation.
    */
   
   /**
    * Add modification to this list.
    */
   private void addChange(int index, int change, ObjectWrapper item, boolean reindex)
   {
      // Search for the correct entry. The correct entry is the
      // one which is concerned with at least one index higher item.
      int topIndex = 0;
      int prevTopIndex = 0;
      int changesIndex = 0;
      ChangeEntry nextEntry = null;
      while ( (topIndex <= index) && (changesIndex<changes.size()) )
      {
         nextEntry = (ChangeEntry) changes.get(changesIndex);
         changesIndex++;
         // Determine the index of interest of this entry
         prevTopIndex = topIndex;
         topIndex+=nextEntry.getOffset();
         if ( nextEntry.getChange() == ChangeEntry.ADDED )
            topIndex++;
      }
      // Add modification to changes list
      ChangeEntry previousEntry = null;
      if ( changesIndex > 1 )
         previousEntry = (ChangeEntry) changes.get(changesIndex-2);
      ChangeEntry newEntry = null;
      if ( topIndex <= index )
      {
         // This means, that there is no next entry, this is the last.
         newEntry = new ChangeEntry(index-topIndex,change,item,reindex);
         nextEntry = null;
      } else {
         // There are changes after this index, so adjust offsets.
         newEntry = new ChangeEntry(index-prevTopIndex,change,item,reindex);
         nextEntry.setOffset(nextEntry.getOffset()-(index-prevTopIndex));
         changesIndex--;
      }
      if ( (newEntry.getOffset()==0) && (newEntry.getChange()==ChangeEntry.REMOVED) &&
            (previousEntry!=null) && (previousEntry.getChange()==ChangeEntry.ADDED) )
      {
         // The new entry is a REMOVED entry, and the previous one is an
         // ADDED on the same index. This means, the added item was removed
         // even before save, so we remove both.
         changes.remove(changesIndex-2);
         if ( nextEntry != null )
         {
            // We must re-calculate the next entry's offset, with
            // it's previous 2 entries removed
            nextEntry.setOffset(nextEntry.getOffset()+newEntry.getOffset()+previousEntry.getOffset());
         }
      } else {
         // The new entry must be added
         changes.add(changesIndex,newEntry);
         // With the new entry, the indexes after this change changed, so
         // adjust
         int diff = 0;
         if ( newEntry.getChange() == ChangeEntry.ADDED )
            diff = +1;
         else
            diff = -1;
         for ( int i=changesIndex+1; i<changes.size(); i++ )
         {
            ObjectWrapper ow = ((ChangeEntry) changes.get(i)).getItem();
            ow.setIndex(item.getIndex()+1);
         }
      }
   }

   public void add(int index, Object item)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("list impl adding item at index: "+index+", item: "+item);
      // Check item validity
      if ( item == null )
         throw new IllegalArgumentException("list does not accept null values.");
      ClassTracker.ClassType type = classTracker.getType(item.getClass());
      if ( (type != ClassTracker.ClassType.TYPE_OBJECT) && (type != ClassTracker.ClassType.TYPE_PRIMITIVE) )
         throw new IllegalArgumentException("list only handles object or primitive types, but was: "+item+" ("+item.getClass().getName()+")");
      // Determine whether it is in the removed list
      ObjectWrapper wrapper = new ObjectWrapper(index,item);
      boolean reindex = false;
      if ( removedItems.contains(wrapper) )
      {
         // It was removed once, so remove remove entry
         removedItems.remove(wrapper);
      } else {
         // Also, calculate insert index, aka. 'original index'.
         // Algorithm: get the previous item, and the one we're
         // inserting at, if both exist, calculate the middle.
         // If either side do not exist then grow with INTERVAL.
         ObjectWrapper prev = null;
         if ( index > 0 )
            prev = getInternal(index-1);
         ObjectWrapper under = null;
         if ( index < size() ) // Here we must calculate size, shame
            under = getInternal(index);
         if ( (prev!=null) && (under!=null) )
         {
            // This means, there is a previous index and a next one,
            // try to get between them if possible.
            if ( under.getOriginalIndex()-prev.getOriginalIndex() < 2 )
            {
               // No place between them, so we must give the new entry
               // the index of 'under', and mark the change for reindexing.
               wrapper.setOriginalIndex(under.getOriginalIndex());
               reindex=true;
            } else {
               // We have luck, we don't have to reindex the whole
               // list.
               wrapper.setOriginalIndex( (under.getOriginalIndex()+prev.getOriginalIndex())/2 );
            }
         } else if ( (prev==null) && (under!=null) ) {
            // This means we insert at the beginning (no previous item)
            if ( Long.MIN_VALUE + INTERVAL > under.getOriginalIndex() )
               throw new StoreException("sorry, list ran out of useful indexes on the beginning, the first index is: "+under.getOriginalIndex());
            wrapper.setOriginalIndex( under.getOriginalIndex() - INTERVAL );
         } else if ( (prev!=null) && (under==null) ) {
            // The insert occured at the end
            if ( Long.MAX_VALUE - INTERVAL < prev.getOriginalIndex() )
               throw new StoreException("sorry, list ran out of useful indexes on the end, the last index is: "+prev.getOriginalIndex());
            wrapper.setOriginalIndex( prev.getOriginalIndex() + INTERVAL );
         } else {
            // There are no items in the list yet, start at 0
            wrapper.setOriginalIndex(0);
         }
         // Add the item
         addedItems.add(wrapper);  
      }
      // Ensure item exists
      if ( logger.isDebugEnabled() )
         logger.debug("adding list item to list: "+index+", object: "+item);
      itemClass.updateItemClassName(originalList,item.getClass(),addedItems.size()==0);
      addChange(index,ChangeEntry.ADDED,wrapper,reindex);
      modCount++;
   }

   /**
    * Clear all entries from the list.
    */
   public void clear()
   {
      // Clear content and mark cleared flag
      cleared = true;
      addedItems = new LinkedList();
      removedItems = new LinkedList();
      originalList = new ArrayList();
      changes = new ArrayList();
      itemClass.clear();
      modCount++;
   }

   private Long searchInternal(Object item, ArrayList order)
   {
      if ( item == null )
         return null;
      ClassTracker.ClassType type = classTracker.getType(item.getClass());
      Long id = objectTracker.getIdentifier(item);
      Transaction tx = transactionTracker.getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      try
      {
         ClassInfo itemInfo = classTracker.getClassInfo(item.getClass(),item);
         String itemTableName = schemaManager.getTableName(itemInfo.getSourceEntry());
         TableTerm itemTableTerm = new TableTerm(itemTableName,null);
         String listTableName = schemaManager.getTableName(
               parentInfo.getAttributeClassEntry(parentAttributeName),parentAttributeName);
         TableTerm listTableTerm = new TableTerm(listTableName,null);
         Expression listExpression = new Expression();
         listExpression.add(new ReferenceTerm(listTableTerm,"persistence_id"));
         listExpression.add("=");
         listExpression.add(new ConstantTerm(objectTracker.getIdentifier(parent)));
         listExpression.add("and");
         if ( type != ClassTracker.ClassType.TYPE_PRIMITIVE )
         {
            listExpression.add(new ReferenceTerm(listTableTerm,"value"));
            listExpression.add("=");
            listExpression.add(new ConstantTerm(id));
         } else {
            listExpression.add(new ReferenceTerm(itemTableTerm,"value"));
            listExpression.add("=");
            listExpression.add(new ConstantTerm(item));
         }
         listExpression.add("and");
         listExpression.add(new ReferenceTerm(itemTableTerm,"persistence_id"));
         listExpression.add("=");
         listExpression.add(new ReferenceTerm(listTableTerm,"value"));
         listExpression.add("and");
         originalTimeControl.apply(listExpression,itemTableTerm);
         listExpression.add("and");
         originalTimeControl.apply(listExpression,listTableTerm);
         // Execute. If there is a hit, then object exists
         QueryStatement stmt = new QueryStatement(listTableTerm,listExpression,order);
         stmt.getSpecifiedTerms().add(new SpecifiedTableTerm(itemTableTerm));
         stmt.setTimeControl(originalTimeControl);
         stmt.setStaticRepresentation("FIND "+listTableTerm+" WHERE persistence_id = "+
               objectTracker.getIdentifier(parent)+" and value = "+id+", order="+order);
         SearchResult result = queryService.find(stmt,new Limits(0,1,0));
         // Check
         if ( result.getResult().size() > 0 )
         {
            Long index = (Long) ((Map)result.getResult().get(0)).get("container_index");
            if ( logger.isDebugEnabled() )
               logger.debug("search internal result index is: "+index+", for item: "+item);
            return index;
         }
      } finally {
         tx.commit();
      }
         if ( logger.isDebugEnabled() )
            logger.debug("search internal did not found the item given: "+item);
      return null;
   }

   public boolean contains(Object item)
   {
      if ( item == null )
         return false;
      ClassTracker.ClassType type = classTracker.getType(item.getClass());
      // Check the list
      Long id = objectTracker.getIdentifier(item);
      ObjectWrapper wrapper = new ObjectWrapper(item);
      // If it is added, then it is contained.
      if ( addedItems.contains(wrapper) )
         return true;
      // Check the list
      if ( (originalList instanceof LazyList) && (!((LazyList) originalList).isIterationCheap()) )
      {
         // This means, that the backing lazy list would page if we
         // were to iterate. So instead, we run a specific query for
         // the given id.
         Long index = searchInternal(item,null);
         return index != null;
      } else {
         // Set is small, so iterate
         for ( int i=0; i<originalList.size(); i++ )
         {
            Object obj = ((Map)originalList.get(i)).get("object");
            if ( ((type==ClassTracker.ClassType.TYPE_PRIMITIVE) && (item.equals(obj))) ||
                 ((type!=ClassTracker.ClassType.TYPE_PRIMITIVE) && (objectTracker.getIdentifier(obj).equals(id))) )
               return true;
         }
      }
      // Fall through
      return false;
   }

   /**
    * This hash code invokes the parent, which is ok becaue the equals method
    * does the same thing as the superclass', just more efficiently.
    */
   public int hashCode()
   {
      return super.hashCode();
   }

   /**
    * Determine whether list equals another list in content.
    */
   public boolean equals(Object obj)
   {
      // If it is not a collection, then it surely does not equal.
      if ( ! (obj instanceof List) )
         return false;
      if ( obj == this ) 
         return true;  // They equals if they are the same object
      List list = (List) obj;
      if ( list.size() != size() )
         return false;  // They don't equals if size differs
      // They only equals if the items equals one-by-one
      Iterator oneIterator = iterator();
      Iterator twoIterator = list.iterator();
      while ( oneIterator.hasNext() && twoIterator.hasNext() )
      {
         Object one = oneIterator.next();
         Object two = twoIterator.next();
         if ( ! objectTracker.getIdentifier(one).equals(
               objectTracker.getIdentifier(two)) )
            return false; // The two items do not equal
      }
      return true; // All items were equal
   }

   public Object get(int index)
   {
      ObjectWrapper wrapper = getInternal(index);
      if ( wrapper == null )
         return null;
      return wrapper.getObject();
   }
   
   private ObjectWrapper getInternal(int index)
   {
      try
      {
         // Search for the correct entry.
         int topIndex = 0;
         int lastTopIndex = 0;
         int lastBottomIndex = 0;
         int bottomIndex = 0;
         int changesIndex = 0;
         ChangeEntry exactEntry = null;
         while ( (topIndex <= index) && (changesIndex<changes.size()) )
         {
            ChangeEntry nextEntry = (ChangeEntry) changes.get(changesIndex);
            changesIndex++;
            // Determine the index of interest of this entry
            lastTopIndex = topIndex;
            lastBottomIndex = bottomIndex;
            topIndex+=nextEntry.getOffset();
            bottomIndex+=nextEntry.getOffset();
            // If this entry is the exact index, then remember
            if ( topIndex == index )
               exactEntry = nextEntry;
            // Modify top and bottom indexes
            if ( nextEntry.getChange() == ChangeEntry.ADDED )
               topIndex++;
            else
               bottomIndex++;
         }
         if ( topIndex <= index )
            bottomIndex=bottomIndex+(index-topIndex);
         else
            bottomIndex=lastBottomIndex+(index-lastTopIndex);
         // If the last exact match was an ADD, then that is the
         // result, else the backing list is used.
         if ( (exactEntry!=null) && (exactEntry.getChange()==ChangeEntry.ADDED) )
         {
            return exactEntry.getItem();
         } else {
            Object obj = ((Map)originalList.get(bottomIndex)).get("object");
            ObjectWrapper result = new ObjectWrapper(index,obj);
            result.setOriginalIndex(((Long)((Map)originalList.get(bottomIndex)).get("container_index")).longValue());
            return result;
         }
      } catch ( IndexOutOfBoundsException e ) {
         logger.error("array index out-of-bound",e);
         throw e;
      }
   }

   public int indexOf(Object item)
   {
      if ( item == null )
         return -1;
      ClassTracker.ClassType type = classTracker.getType(item.getClass());
      Long itemId = objectTracker.getIdentifier(item);
      if ( (itemId == null) && (type!=ClassTracker.ClassType.TYPE_PRIMITIVE) )
         return -1; // Object does not have identifier, it is not contained.
      if ( (originalList instanceof LazyList) && (!((LazyList) originalList).isIterationCheap()) )
      {
         // The list is large, do a select to determine index
         Transaction tx = transactionTracker.getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         try
         {
            String listTableName = schemaManager.getTableName(
                  parentInfo.getAttributeClassEntry(parentAttributeName),parentAttributeName);
            TableTerm listTableTerm = new TableTerm(listTableName,null);
            ArrayList order = new ArrayList();
            order.add(new OrderBy(new ReferenceTerm(listTableTerm,"container_index"),OrderBy.ASCENDING));
            Long oldIndex = searchInternal(item,order);
            // Check
            if ( oldIndex != null )
            {
               // Determine how many indexes are before this index
               Expression listExpression = new Expression();
               listExpression.add(new ReferenceTerm(listTableTerm,"persistence_id"));
               listExpression.add("=");
               listExpression.add(new ConstantTerm(objectTracker.getIdentifier(parent)));
               listExpression.add("and");
               listExpression.add(new ReferenceTerm(listTableTerm,"container_index"));
               listExpression.add("<");
               listExpression.add(new ConstantTerm(oldIndex));
               listExpression.add("and");
               originalTimeControl.apply(listExpression,listTableTerm);
               QueryStatement stmt = new QueryStatement(listTableTerm,listExpression,null);
               stmt.setTimeControl(originalTimeControl);
               stmt.setStaticRepresentation("FIND "+listTableTerm+" WHERE persistence_id = "+
                     objectTracker.getIdentifier(parent)+" and container_index < "+oldIndex);
               SearchResult result = queryService.find(stmt,new Limits(0,0,-1));
               return (int) result.getResultSize();
            }
         } finally {
            tx.commit();
         }
      } else {
         // Iterate and find the object
         for ( int i=0; i<size(); i++ )
         {
            Object obj = get(i);
            if ( ((type==ClassTracker.ClassType.TYPE_PRIMITIVE) && (item.equals(obj))) ||
                 ((type!=ClassTracker.ClassType.TYPE_PRIMITIVE) && (objectTracker.getIdentifier(obj).equals(itemId))) )
               return i;
         }
      }
      return -1;
   }

   public int lastIndexOf(Object item)
   {
      if ( item == null )
         return -1;
      ClassTracker.ClassType type = classTracker.getType(item.getClass());
      Long itemId = objectTracker.getIdentifier(item);
      if ( (itemId == null) && (type!=ClassTracker.ClassType.TYPE_PRIMITIVE) )
         return -1; // Object does not have identifier, it is not contained.
      if ( (originalList instanceof LazyList) && (!((LazyList) originalList).isIterationCheap()) )
      {
         // The list is large, do a select to determine index
         Transaction tx = transactionTracker.getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         try
         {
            String listTableName = schemaManager.getTableName(
                  parentInfo.getAttributeClassEntry(parentAttributeName),parentAttributeName);
            TableTerm listTableTerm = new TableTerm(listTableName,null);
            ArrayList order = new ArrayList();
            order.add(new OrderBy(new ReferenceTerm(listTableTerm,"container_index"),OrderBy.DESCENDING));
            Long oldIndex = searchInternal(item,order);
            // Check
            if ( oldIndex != null )
            {
               // Determine how many indexes are before this index
               Expression listExpression = new Expression();
               listExpression.add(new ReferenceTerm(listTableTerm,"persistence_id"));
               listExpression.add("=");
               listExpression.add(new ConstantTerm(objectTracker.getIdentifier(parent)));
               listExpression.add("and");
               listExpression.add(new ReferenceTerm(listTableTerm,"container_index"));
               listExpression.add("<");
               listExpression.add(new ConstantTerm(oldIndex));
               listExpression.add("and");
               originalTimeControl.apply(listExpression,listTableTerm);
               QueryStatement stmt = new QueryStatement(listTableTerm,listExpression,null);
               stmt.setTimeControl(originalTimeControl);
               stmt.setStaticRepresentation("FIND "+listTableTerm+" WHERE persistence_id = "+
                     objectTracker.getIdentifier(parent)+" and container_index < "+oldIndex);
               SearchResult result = queryService.find(stmt,new Limits(0,0,-1));
               return (int) result.getResultSize();
            }
         } finally {
            tx.commit();
         }
      } else {
         // Iterate and find the object
         for ( int i=size(); i>0; i++ )
         {
            Object obj = get(i-1);
            if ( ((type==ClassTracker.ClassType.TYPE_PRIMITIVE) && (item.equals(obj))) ||
                 ((type!=ClassTracker.ClassType.TYPE_PRIMITIVE) && (objectTracker.getIdentifier(obj).equals(itemId))) )
               return i-1;
         }
      }
      return -1;
   }

   public Object remove(int index)
   {
      // Get the object at the given index
      ObjectWrapper wrapper = getInternal(index);
      // Object is present, determine, whether it is in the added list
      if ( addedItems.contains(wrapper) )
      {
         // It was added, so remove added entry
         addedItems.remove(wrapper);
      } else {
         // It was not yet added, so add
         removedItems.add(wrapper);
      }
      // Insert modification entry
      if ( logger.isDebugEnabled() )
         logger.debug("removed index: "+index+", object: "+wrapper.getObject());
      addChange(index,ChangeEntry.REMOVED,wrapper,false);
      modCount++;
      return wrapper.getObject();
   }

   public boolean remove(Object item)
   {
      int index = indexOf(item);
      if ( index == -1 )
         return false;
      remove(index);
      return true;
   }

   public Object set(int index, Object item)
   {
      // Simply remove first, then add to the same position
      Object oldItem = remove(index);
      add(index,item);
      return oldItem;
   }

   public int size()
   {
      return originalList.size() - removedItems.size() + addedItems.size();
   }
   
   public String toString()
   {
      return originalList.toString();
   }

   private class ChangeEntry
   {
      public static final int ADDED = 1;
      public static final int REMOVED = 2;
      
      private int offset;
      private int change;
      private boolean reindex;
      private ObjectWrapper item;

      public ChangeEntry(int offset, int change, ObjectWrapper item, boolean reindex)
      {
         this.offset=offset;
         this.change=change;
         this.item=item;
         this.reindex=reindex;
      }

      public boolean getReindex()
      {
         return reindex;
      }
      
      public String toString()
      {
         return "[Change: "+change+", offset: "+offset+", item: "+item+"]";
      }

      public int getOffset()
      {
         return offset;
      }
      public void setOffset(int offset)
      {
         this.offset=offset;
      }

      public int getChange()
      {
         return change;
      }
      public void setChange(int change)
      {
         this.change=change;
      }

      public ObjectWrapper getItem()
      {
         return item;
      }
      public void setItem(ObjectWrapper item)
      {
         this.item=item;
      }
   }
   
   public class ObjectWrapper
   {
      private Object obj;
      private Long id;
      private long originalIndex;
      private int index;
      private boolean indexGiven;
      private ClassTracker.ClassType type;

      public ObjectWrapper(int index, Object obj)
      {
         this.indexGiven=true;
         this.index=index;
         this.obj=obj;
         objectTracker.registerObject(obj);
         this.id=objectTracker.getIdentifier(obj);
         this.type=classTracker.getType(obj.getClass());
      }

      public ObjectWrapper(Object obj)
      {
         this.indexGiven=false;
         this.obj=obj;
         objectTracker.registerObject(obj);
         this.id=objectTracker.getIdentifier(obj);
         this.type=classTracker.getType(obj.getClass());
      }

      public int getIndex()
      {
         return index;
      }

      public void setIndex(int index)
      {
         this.index=index;
      }

      public Long getIdentifier()
      {
         return id;
      }

      public Object getObject()
      {
         return obj;
      }

      public int hashCode()
      {
         if ( type == ClassTracker.ClassType.TYPE_PRIMITIVE )
            return obj.hashCode();
         else
            return (int) (id.longValue()>>INT_BITS);
      }

      public boolean equals(Object rhs)
      {
         if ( ! (rhs instanceof ObjectWrapper) )
            return false;
         if ( type == ClassTracker.ClassType.TYPE_PRIMITIVE )
         {
            if ( ! obj.equals( ((ObjectWrapper)rhs).obj ) )
               return false;
         } else {
            if ( ! id.equals(((ObjectWrapper) rhs).id) )
               return false;
         }
         if ( indexGiven )
            return index == ((ObjectWrapper) rhs).index;
         return true;
      }

      public long getOriginalIndex()
      {
         return originalIndex;
      }
      public void setOriginalIndex(long originalIndex)
      {
         this.originalIndex=originalIndex;
      }

   }
}

