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
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.type.event.*;
import hu.netmind.beankeeper.query.LazyList;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.db.Limits;
import hu.netmind.beankeeper.db.SearchResult;
import hu.netmind.beankeeper.serial.Serial;
import hu.netmind.beankeeper.db.Database;
import hu.netmind.beankeeper.object.ObjectTracker;
import hu.netmind.beankeeper.query.QueryService;
import hu.netmind.beankeeper.schema.SchemaManager;
import org.apache.log4j.Logger;

/**
 * Custom map implementation based on lazy lists. Map is <strong>not</strong>
 * thread-safe.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class MapImpl extends AbstractMap implements Container
{
   private static Logger logger = Logger.getLogger(MapImpl.class);
   
   private List originalList;
   private TimeControl originalTimeControl;
   private ClassInfo parentInfo;
   private Object parent;
   private String parentAttributeName;
   private Long lastSerial;
   private ContainerItemClass itemClass;

   private Map addedItems;
   private Map removedItems;
   private boolean cleared = false;
   private int modCount = 0;

   private ClassTracker classTracker = null; // Injected
   private QueryService queryService = null; // Injected
   private Database database = null; // Injected
   private ObjectTracker objectTracker = null; // Injected
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
      // Initialize internal change structures
      modCount++;
      cleared = false;
      addedItems = new HashMap();
      removedItems = new HashMap();
      // Load list
      if ( getItemClassName() == null )
      {
         // No list
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
         for ( int i=0; i<lazy.getStmts().size(); i++ )
         {
            QueryStatement stmt = (QueryStatement) lazy.getStmts().get(i);
            // First search for the table term of the subtable
            Expression expr = stmt.getQueryExpression();
            TableTerm subTableTerm = expr.getTableTerm(subTableName);
            // Add this term to selected terms
            stmt.getSelectTerms().add(new ReferenceTerm(subTableTerm,"container_key"));
            // Add the correct order by
            stmt.getOrderByList().add(0,new OrderBy(new ReferenceTerm(subTableTerm,"container_key"),OrderBy.ASCENDING));
            // Correct static string
            stmt.setStaticRepresentation(stmt.getStaticRepresentation()+" and mapkey");
         }
      }
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
         database.save(transaction, attributeTableName, keyAttributes,removeAttributes);
         removeTables.add(attributeTableName);
         // Notify listeners
         events.add(new ClearedContainerEvent(parent,parentAttributeName));
      }
      // Remove removed items first
      Iterator removedItemsIterator = removedItems.entrySet().iterator();
      while ( removedItemsIterator.hasNext() )
      {
         Map.Entry entry = (Map.Entry) removedItemsIterator.next();
         // Remove object from the list
         HashMap keyAttributes = new HashMap();
         keyAttributes.put("persistence_id",objectTracker.getIdentifier(parent));
         keyAttributes.put("persistence_end",Serial.getMaxSerial().getValue());
         keyAttributes.put("persistence_txend",Serial.getMaxSerial().getValue());
         keyAttributes.put("container_key",entry.getKey().toString());
         HashMap removeAttributes = new HashMap();
         removeAttributes.put("persistence_txend",currentSerial);
         removeAttributes.put("persistence_txendid",transaction.getSerial());
         database.save(transaction, attributeTableName, keyAttributes,removeAttributes);
         removeTables.add(attributeTableName);
         // Notify listeners
         events.add(new RemovedItemEvent(parent,parentAttributeName,new SimpleMapEntry(entry)));
      }
      // Add added items
      Iterator addedItemsIterator = addedItems.entrySet().iterator();
      while ( addedItemsIterator.hasNext() )
      {
         Map.Entry entry = (Map.Entry) addedItemsIterator.next();
         ObjectWrapper wrapper = (ObjectWrapper) entry.getValue();
         Object value = wrapper.getObject();
         // Item does not exist then put it in the save list
         if ( ! objectTracker.exists(value) )
            waitingObjects.add(objectTracker.getWrapper(value));
         // Add item
         HashMap itemAttributes = new HashMap();
         itemAttributes.put("persistence_id",objectTracker.getIdentifier(parent));
         itemAttributes.put("persistence_start",Serial.getMaxSerial().getValue());
         itemAttributes.put("persistence_end",Serial.getMaxSerial().getValue());
         itemAttributes.put("persistence_txendid",new Long(0));
         itemAttributes.put("persistence_txstartid",transaction.getSerial());
         itemAttributes.put("persistence_txstart",currentSerial);
         itemAttributes.put("persistence_txend",Serial.getMaxSerial().getValue());
         itemAttributes.put("container_key",entry.getKey().toString());
         itemAttributes.put("value",objectTracker.getIdentifier(value));
         database.insert(transaction, attributeTableName, itemAttributes);
         saveTables.add(attributeTableName);
         // Notify listeners
         logger.debug("adding item event to attribute: "+parentAttributeName+", entry: "+entry);
         events.add(new AddedItemEvent(parent,parentAttributeName,new SimpleMapEntry(entry)));
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

   /**
    * Retain all container elements inside the other container.
    */
   public boolean retainAll(Object c)
   {
      // Go through all elements, if it's not contained in c,
      // then remove (using the iterator!)
      boolean changed = false;
      Iterator iterator = entrySet().iterator();
      if ( logger.isDebugEnabled() )
         logger.debug("map retain all: "+toString()+" from "+c.toString());
      while ( iterator.hasNext() )
      {
         Map.Entry entry = (Map.Entry) iterator.next();
         if ( ! ((Map)c).containsKey(entry.getKey()) )
         {
            logger.debug("trying to remove key: "+entry.getKey());
            iterator.remove();
            changed=true;
         }
      }
      return changed;
   }

   /**
    * Add all items in the other container.
    */
   public boolean addAll(Object c)
   {
      putAll((Map)c);
      return true;
   }

   /*
    * Map implementation.
    */

   public void clear()
   {
      // Clear content and mark cleared flag
      cleared = true;
      addedItems = new HashMap();
      removedItems = new HashMap();
      originalList = new ArrayList();
      itemClass.clear();
      modCount++;
   }

   /**
    * Return whether this map contains the specified key.
    */
   public boolean containsKey(Object key)
   {
      // If the item is removed, then it is not contained.
      if ( removedItems.containsKey(key) )
         return false;
      // If it is added, then it is contained.
      if ( addedItems.containsKey(key) )
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
            String listTableName = schemaManager.getTableName(
                  parentInfo.getAttributeClassEntry(parentAttributeName),parentAttributeName);
            TableTerm listTableTerm = new TableTerm(listTableName,null);
            Expression listExpression = new Expression();
            listExpression.add(new ReferenceTerm(listTableTerm,"persistence_id"));
            listExpression.add("=");
            listExpression.add(new ConstantTerm(objectTracker.getIdentifier(parent)));
            listExpression.add("and");
            listExpression.add(new ReferenceTerm(listTableTerm,"container_key"));
            listExpression.add("=");
            listExpression.add(new ConstantTerm(key.toString()));
            listExpression.add("and");
            originalTimeControl.apply(listExpression,listTableTerm);
            // Execute. If there is a hit, then object exists
            QueryStatement stmt = new QueryStatement(listTableTerm,listExpression,null);
            stmt.setTimeControl(originalTimeControl);
            stmt.setStaticRepresentation("FIND "+listTableTerm+" WHERE persistence_id = "+
                  objectTracker.getIdentifier(parent)+" and mapkey = "+key);
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
            Map obj = (Map) originalList.get(i);
            if ( key.equals(obj.get("container_key")) )
               return true;
         }
      }
      // Fall through
      return false;
   }

   /**
    * Returns whether this map contains one or more of the 
    * specified value.
    */
   public boolean containsValue(Object value)
   {
      if ( value == null )
         return false;
      ClassTracker.ClassType type = classTracker.getType(value.getClass());
      ObjectWrapper wrapper = new ObjectWrapper(value);
      Long id = objectTracker.getIdentifier(value);
      if ( id == null )
         return false; // Can not contain value with no id
      // If the item is removed, then it is not contained.
      if ( removedItems.containsValue(wrapper) )
         return false;
      // If it is added, then it is contained.
      if ( addedItems.containsValue(wrapper) )
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
            ClassInfo itemInfo = classTracker.getClassInfo(value.getClass(),value);
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
               listExpression.add(new ConstantTerm(value));
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
            stmt.setStaticRepresentation("FIND "+listTableTerm+" WHERE persistence_id = "+objectTracker.getIdentifier(parent)+" and value = "+id);
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
            Map obj = (Map) originalList.get(i);
            if ( ((type==ClassTracker.ClassType.TYPE_PRIMITIVE) && (value.equals(obj.get("object")))) ||
                 ((type!=ClassTracker.ClassType.TYPE_PRIMITIVE) && 
                  (id.equals(objectTracker.getIdentifier(obj.get("object"))))
                 ) )
               return true;
         }
      }
      // Fall through
      return false;
   }

   /**
    * Returns a set view of this map.
    */
   public Set entrySet()
   {
      return new MapImplEntrySet();
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
    * Determine whether map equals another map in content.
    */
   public boolean equals(Object obj)
   {
      // If it is not a map, then it surely does not equal.
      if ( ! (obj instanceof Map) )
         return false;
      // If they are the same size, and all items equal, then the
      // maps equal.
      Map c = (Map) obj;
      return (size()==c.size()) && (entrySet().containsAll(c.entrySet()));
   }

   /**
    * Get the value for a mapkey.
    */
   public Object get(Object key)
   {
      // If contained in the added list, return that
      ObjectWrapper wrapper = (ObjectWrapper) addedItems.get(key);
      if ( wrapper != null )
         return wrapper.getObject();
      // If contained in the remove list, then the result is null.
      if ( removedItems.containsKey(key) )
         return null;
      // Not found until now, so check the list
      if ( (originalList instanceof LazyList) && (!((LazyList) originalList).isIterationCheap()) )
      {
         if ( logger.isDebugEnabled() )
            logger.debug("there are many entries, so finding with select: "+key);
         // The list is too large, so select the item separately
         List result = queryService.find(
               "find item("+getItemClassName()+")"+
               " where persistence_container_parent("+parentInfo.getSourceEntry().getFullName()+")"+
               "."+parentAttributeName+"['"+key.toString()+"']=item and persistence_container_parent = ?",
               new Object[] { parent }, originalTimeControl,null);
         if ( result.isEmpty() )
            return null;
         return result.get(0);               
      } else {
         if ( logger.isDebugEnabled() )
            logger.debug("iterating to find item: "+key);
         // List is small, so iterate
         for ( int i=0; i<originalList.size(); i++ )
         {
            Map obj = (Map) originalList.get(i);
            if ( key.equals(obj.get("container_key")) )
               return obj.get("object"); // Found key
         }
      }
      // Fall through
      return null; // Not found
   }

   /**
    * Put an item into the map.
    */
   public Object put(Object key, Object value)
   {
      // Check value validity
      if ( value == null )
         throw new IllegalArgumentException("map implementation not accepting null values.");
      ClassTracker.ClassType type = classTracker.getType(value.getClass());
      if ( (type != ClassTracker.ClassType.TYPE_OBJECT) && (type != ClassTracker.ClassType.TYPE_PRIMITIVE) )
         throw new IllegalArgumentException("map only handles object or primitive types, but was: "+value+" ("+value.getClass().getName()+")");
      // Determine the old value if there was one
      Object oldValue = get(key);
      // If the old value is the same, then skip
      if ( (oldValue!=null) && (objectTracker.getIdentifier(oldValue)!=null) &&
            (objectTracker.getIdentifier(oldValue).equals(objectTracker.getIdentifier(value))) )
         return oldValue;
      // Remove the key if there is an oldvalue
      if ( oldValue != null )
         remove(key);
      // Then add the entry, now that is it not contained
      itemClass.updateItemClassName(originalList,value.getClass(),addedItems.size()==0);
      addedItems.put(key,new ObjectWrapper(value));
      // Modification took place
      modCount++;
      // Return with the old value of the key
      logger.debug("added key: "+key);
      return oldValue;
   }

   /**
    * Remove a key from the map.
    */
   public Object remove(Object key)
   {
      // Get the old value
      Object oldValue = get(key);
      if ( oldValue == null )
         return null; // Not contained, so no op.
      // Determine whether key is in the added list. If it is,
      // then simply remove it, so it will not be added. Else, add to
      // the removed list.
      if ( addedItems.containsKey(key) )
         addedItems.remove(key);
      else
         removedItems.put(key,new ObjectWrapper(oldValue));
      // Modification took place
      modCount++;
      // Return with old value
      logger.debug("removed key: "+key);
      return oldValue;
   }

   public int size()
   {
      return originalList.size() - removedItems.size() + addedItems.size();
   }

   public String toString()
   {
      return originalList.toString();
   }

   public class MapImplEntrySet extends AbstractSet
   {
      public void clear()
      {
         MapImpl.this.clear();
      }

      public boolean contains(Object o)
      {
         Map.Entry entry = (Map.Entry) o;
         ObjectWrapper wrapper = new ObjectWrapper(entry.getValue());
         Long id = objectTracker.getIdentifier(entry.getValue());
         if ( id == null )
            return false; // Can not contain value with no id
         // If the item is removed, then it is not contained.
         if ( removedItems.containsKey(entry.getKey()) )
            return false;
         // If it is added, then it is contained and is the same value.
         if ( addedItems.containsKey(entry.getKey()) )
            return addedItems.get(entry.getKey()).equals(wrapper);
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
               String listTableName = schemaManager.getTableName(
                     parentInfo.getAttributeClassEntry(parentAttributeName),parentAttributeName);
               TableTerm listTableTerm = new TableTerm(listTableName,null);
               Expression listExpression = new Expression();
               listExpression.add(new ReferenceTerm(listTableTerm,"persistence_id"));
               listExpression.add("=");
               listExpression.add(new ConstantTerm(objectTracker.getIdentifier(parent)));
               listExpression.add("and");
               listExpression.add(new ReferenceTerm(listTableTerm,"value"));
               listExpression.add("=");
               listExpression.add(new ConstantTerm(id));
               listExpression.add("and");
               listExpression.add(new ReferenceTerm(listTableTerm,"container_key"));
               listExpression.add("=");
               listExpression.add(new ConstantTerm(entry.getKey()));
               listExpression.add("and");
               originalTimeControl.apply(listExpression,listTableTerm);
               // Execute. If there is a hit, then object exists
               QueryStatement stmt = new QueryStatement(listTableTerm,listExpression,null);
               stmt.setTimeControl(originalTimeControl);
               stmt.setStaticRepresentation("FIND "+listTableTerm+" WHERE persistence_id = "+
                     objectTracker.getIdentifier(parent)+" and value = "+id+", mapkey = "+entry.getKey());
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
               Map obj = (Map) originalList.get(i);
               if ( obj.get("container_key").equals(entry.getKey()) )
                  return (id.equals(objectTracker.getIdentifier(obj.get("object"))));
            }
         }
         // Fall through
         return false;
      }

      public Iterator iterator()
      {
         return new MapImplEntrySetIterator();
      }

      public boolean remove(Object o)
      {
         return MapImpl.this.remove( ((Map.Entry) o).getKey() ) != null;
      }

      public int size()
      {
         return MapImpl.this.size();
      }

      public String toString()
      {
         return originalList.toString();
      }
   }

   public class MapImplEntrySetIterator implements Iterator
   {
      private int ownModCount;
      private int index;
      private boolean hasNext;
      private Map.Entry next;
      private Map.Entry current;
      private Iterator addedIterator;

      public MapImplEntrySetIterator()
      {
         index = 0;
         ownModCount=modCount;
         addedIterator = addedItems.entrySet().iterator();
         // preread
         read();
      }

      private void read()
      {
         // Ensure that no modification took place
         if ( ownModCount != modCount )
            throw new java.util.ConcurrentModificationException("map was modified while iterating");
         // Check whether there are elements left in the list
         while ( index<originalList.size() )
         {
            Map obj = (Map) originalList.get(index);
            index++; // Next
            Object key = obj.get("container_key");
            if ( ! removedItems.containsKey(key) )
            {
               // This is a valid item, because it is not removed
               hasNext = true;
               next = new MapImplEntry(obj);
               return;
            } 
            if ( addedItems.containsKey(key) )
            {
               // This is a valid item, because it's key is here, but
               // it was altered with another value. In this case we
               // place the pair here, in it's original place.
               hasNext = true;
               next = new MapImplEntry(key,(ObjectWrapper)addedItems.get(key));
               return;
            } 
         }
         // If there were no elements in the original list, check through
         // the added items. Note, that if the added key was already iterated
         // through in the original list, that key has to be skipped here.
         if ( addedIterator.hasNext() )
         {
            Map.Entry entry = (Map.Entry) addedIterator.next();
            if ( ! removedItems.containsKey(entry.getKey()) )
            {
               // The key is not removed, meaning it is really a new key,
               // which was not contained in the original list.
               hasNext = true;
               next = new MapImplEntry(entry);
               return;
            }
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
            removedItems.put(current.getKey(),new ObjectWrapper(current.getValue()));
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

      public class MapImplEntry implements Map.Entry
      {
         private Object key;
         private Object value;

         public MapImplEntry(Map obj)
         {
            key = obj.get("container_key");
            value = obj.get("object");
         }

         public MapImplEntry(Map.Entry entry)
         {
            key = entry.getKey();
            value = ((ObjectWrapper) entry.getValue()).getObject();
         }

         public MapImplEntry(Object key, ObjectWrapper wrapper)
         {
            this.key=key;
            this.value=wrapper.getObject();
         }
         
         public Object getKey()
         {
            return key;
         }

         public Object getValue()
         {
            return value;
         }

         public Object setValue(Object value)
         {
            // This will modify the map, but we're ok, because
            // it won't hurt our iteration.
            Object oldValue = this.value;
            put(key,value);
            this.value=value;
            ownModCount=modCount;
            return oldValue;
         }
      }
   }

   public static class SimpleMapEntry implements Map.Entry
   {
      private Object key;
      private Object value;
      
      public SimpleMapEntry(Map.Entry entry)
      {
         this.key=entry.getKey();
         this.value=((ObjectWrapper) entry.getValue()).getObject();
      }

      public Object getKey()
      {
         return key;
      }

      public Object getValue()
      {
         return value;
      }

      public Object setValue(Object value)
      {
         Object oldValue = this.value;
         this.value=value;
         return oldValue;
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
            return (int) (id.longValue()>>32);
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

