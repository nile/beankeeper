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

import hu.netmind.beankeeper.type.event.*;
import java.util.*;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.db.SearchResult;
import hu.netmind.beankeeper.db.Limits;
import hu.netmind.beankeeper.db.Database;
import hu.netmind.beankeeper.serial.Serial;
import hu.netmind.beankeeper.query.LazyList;
import hu.netmind.beankeeper.query.QueryService;
import hu.netmind.beankeeper.object.ObjectTracker;
import hu.netmind.beankeeper.schema.SchemaManager;
import org.apache.log4j.Logger;

/**
 * Custom set implementation based on lazy lists. This is <strong>not</strong>
 * thread-safe.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class SetImpl extends AbstractSet implements Container
{
   private static final int INT_BITS = 32;
   private static Logger logger = Logger.getLogger(SetImpl.class);

   private List originalList;
   private TimeControl originalTimeControl;
   private ClassInfo parentInfo;
   private Object parent;
   private String parentAttributeName;
   private ContainerItemClass itemClass;
   private Long lastSerial;

   private Set addedItems;
   private Set removedItems;
   private boolean cleared = false;
   private int modCount = 0;

   private ClassTracker classTracker = null; // Injected
   private QueryService queryService = null; // Injected
   private ObjectTracker objectTracker = null; // Injected
   private Database database = null; // Injected
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
      // Load list. If original list is mutable (is not a lazylist),
      // then leave it, it should be ok.
      if ( getItemClassName() == null )
      {
         originalList = new ArrayList();
      } else {
         if ( originalList instanceof ArrayList )
         {
            // Remove removed items
            Iterator originalIterator = originalList.iterator();
            while ( originalIterator.hasNext() )
            {
               Object item = originalIterator.next();
               if ( removedItems.contains(new ObjectWrapper(item)) )
                 originalIterator.remove(); 
            }
            // Add added items
            Iterator addedIterator = addedItems.iterator();
            while ( addedIterator.hasNext() )
            {
               ObjectWrapper wrapper = (ObjectWrapper) addedIterator.next();
               originalList.add(wrapper.getObject());
            }
         } else {
            originalList = queryService.find(
                  "find item("+getItemClassName()+")"+
                  " where persistence_container_parent("+parentInfo.getSourceEntry().getFullName()+")"+
                  "."+parentAttributeName+" contains item and persistence_container_parent = ?",
                  new Object[] { parent }, originalTimeControl,null);
         }
      }
      // Initialize internal change structures
      modCount++;
      cleared = false;
      addedItems = new HashSet();
      removedItems = new HashSet();
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
         HashMap keyAttributes = new HashMap();
         keyAttributes.put("persistence_id",objectTracker.getIdentifier(parent));
         keyAttributes.put("persistence_end",Serial.getMaxSerial().getValue());
         keyAttributes.put("persistence_txend",Serial.getMaxSerial().getValue());
         HashMap removeAttributes = new HashMap();
         removeAttributes.put("persistence_txend",currentSerial);
         removeAttributes.put("persistence_txendid",transaction.getSerial());
         database.save(transaction,attributeTableName,keyAttributes,removeAttributes);
         removeTables.add(attributeTableName);
         // Notify listeners
         events.add(new ClearedContainerEvent(parent,parentAttributeName));
      }
      // Remove removed items first
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
         keyAttributes.put("value",objectTracker.getIdentifier(obj));
         HashMap removeAttributes = new HashMap();
         removeAttributes.put("persistence_txend",currentSerial);
         removeAttributes.put("persistence_txendid",transaction.getSerial());
         database.save(transaction,attributeTableName,keyAttributes,removeAttributes);
         removeTables.add(attributeTableName);
         // Notify listeners
         events.add(new RemovedItemEvent(parent,parentAttributeName,obj));
      }
      // Add added items
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
         itemAttributes.put("value",objectTracker.getIdentifier(obj));
         database.insert(transaction, attributeTableName, itemAttributes);
         saveTables.add(attributeTableName);
         // Notify listeners
         logger.debug("adding item event to attribute: "+parentAttributeName+", obj: "+obj);
         events.add(new AddedItemEvent(parent,parentAttributeName,obj));
      }
      // Reload the changed list. Note, that the list should not
      // be referenced until the Store.save() operation completes, because
      // the list will not contain nonexisting object currently.
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
    * Implementing Set
    */

   public boolean add(Object item)
   {
      // Check item validity
      if ( item == null )
         throw new IllegalArgumentException("set does not accept null values.");
      ClassTracker.ClassType type = classTracker.getType(item.getClass());
      if ( (type != ClassTracker.ClassType.TYPE_OBJECT) && (type != ClassTracker.ClassType.TYPE_PRIMITIVE) )
         throw new IllegalArgumentException("set only handles object or primitive types, but was: "+item+" ("+item.getClass().getName()+")");
      // Ensure item exists
      if ( ! contains(item) )
      {
         // Object does not exist. If it does not exist because it is
         // in the remove list, then simply remove from remove list.
         ObjectWrapper wrapper = new ObjectWrapper(item);
         if ( removedItems.contains(wrapper) )
         {
            removedItems.remove(wrapper);
         } else {
            itemClass.updateItemClassName(originalList,item.getClass(),addedItems.size()==0);
            addedItems.add(wrapper);
         }
         modCount++;
         return true;
      }
      return false;
   }

   public void clear()
   {
      // Clear content and mark cleared flag
      cleared = true;
      addedItems = new HashSet();
      removedItems = new HashSet();
      originalList = new ArrayList();
      itemClass.clear();
      modCount++;
   }

   public boolean contains(Object item)
   {
      if ( item == null )
         return false;
      ClassTracker.ClassType type = classTracker.getType(item.getClass());
      ObjectWrapper wrapper = new ObjectWrapper(item);
      // If the item is removed, then it is not contained.
      if ( removedItems.contains(wrapper) )
         return false;
      // If it is added, then it is contained.
      if ( addedItems.contains(wrapper) )
         return true;
      // Else, check the list
      if ( (originalList instanceof LazyList) && (!((LazyList) originalList).isIterationCheap()) )
      {
         // This means, that the backing lazy list would page if we
         // were to iterate. So instead, we run a specific query for
         // the given id.
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
               listExpression.add(new ConstantTerm(wrapper.getIdentifier()));
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
            QueryStatement stmt = new QueryStatement(listTableTerm,listExpression,null);
            stmt.getSpecifiedTerms().add(new SpecifiedTableTerm(itemTableTerm));
            stmt.setTimeControl(originalTimeControl);
            stmt.setStaticRepresentation("FIND "+listTableTerm+" WHERE persistence_id = "+
                  objectTracker.getIdentifier(parent)+" and value = "+wrapper.getIdentifier());
            SearchResult result = queryService.find(stmt,new Limits(0,0,-1));
            // Check
            if ( result.getResultSize() > 0 )
               return true; // Object exists
         } finally {
            tx.commit();
         }
      } else {
         // Set is small, so iterate
         for ( int i=0; i<originalList.size(); i++ )
         {
            Object obj = originalList.get(i);
            if ( ((type==ClassTracker.ClassType.TYPE_PRIMITIVE) && (item.equals(obj))) ||
                 ((type!=ClassTracker.ClassType.TYPE_PRIMITIVE) && (objectTracker.getIdentifier(obj).equals(wrapper.getIdentifier()) )) )
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
      if ( ! (obj instanceof Collection) )
         return false;
      // If they are the same size, and all items equal, then the
      // collections equal.
      Collection c = (Collection) obj;
      return (size()==c.size()) && (containsAll(c));
   }

   /**
    * Get iterator.
    */
   public Iterator iterator()
   {
      return new SetImplIteratorImpl();
   }

   /**
    * Remove object from set.
    */
   public boolean remove(Object item)
   {
      // Ensure item exists
      if ( contains(item) )
      {
         // Object does exist. If it does exist because it is
         // in the added list, then simply remove from added list.
         ObjectWrapper wrapper = new ObjectWrapper(item);
         if ( addedItems.contains(wrapper) )
            addedItems.remove(wrapper);
         else
            removedItems.add(wrapper);
         modCount++;
         return true;
      }
      return false;
   }

   public int size()
   {
      return originalList.size() - removedItems.size() + addedItems.size();
   }
   
   public String toString()
   {
      return originalList.toString();
   }

   public class SetImplIteratorImpl implements Iterator
   {
      private int ownModCount;
      private int index;
      private boolean hasNext;
      private Object next;
      private Object current;
      private Iterator addedIterator;

      public SetImplIteratorImpl()
      {
         index = 0;
         ownModCount=modCount;
         addedIterator = addedItems.iterator();
         // preread
         read();
      }

      private void read()
      {
         // Ensure that no modification took place
         if ( ownModCount != modCount )
            throw new java.util.ConcurrentModificationException("set was modified while iterating");
         // Check whether there are elements left in the list
         while ( index<originalList.size() )
         {
            Object obj = originalList.get(index);
            index++; // Next
            ObjectWrapper wrapper = new ObjectWrapper(obj);
            if ( ! removedItems.contains(wrapper) )
            {
               // This is a valid item
               hasNext = true;
               next = obj;
               return;
            } 
         }
         // If there were no elements in the original list, check through
         // the added items.
         if ( addedIterator.hasNext() )
         {
            hasNext = true;
            next = ((ObjectWrapper) addedIterator.next()).getObject();
            return;
         }
         // Fall through
         hasNext = false;
         next = null;
      }

      public void remove()
      {
         // Remove the current item
         if ( index <= originalList.size() )
         {
            // The current item is in the original list, not in the
            // added list, insert into the removed list.
            removedItems.add(new ObjectWrapper(current));
         } else {
            // The current item is in the added list, so remove from it.
            addedIterator.remove();
            // Modify the modcount, because the whole set changed
         }
         modCount++;
         ownModCount = modCount; // The modcount changed, but we're safe
      }

      public boolean hasNext()
      {
         return hasNext;
      }

      public Object next()
      {
         current = next;
         read(); // Pre-read next
         return current;
      }
   }

   public class ObjectWrapper
   {
      private Object obj;
      private Long id;
      private ClassTracker.ClassType type;

      public ObjectWrapper(Object obj)
      {
         this.obj=obj;
         objectTracker.registerObject(obj);
         this.id=objectTracker.getIdentifier(obj);
         this.type=classTracker.getType(obj.getClass());
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
            return obj.equals( ((ObjectWrapper)rhs).obj );
         else
            return id.equals(((ObjectWrapper) rhs).id);
      }

   }
}

