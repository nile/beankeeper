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

package hu.netmind.beankeeper.store.impl;

import org.apache.log4j.Logger;
import java.util.*;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.store.StoreService;
import hu.netmind.beankeeper.serial.SerialTracker;
import hu.netmind.beankeeper.lock.LockTracker;
import hu.netmind.beankeeper.config.ConfigurationTracker;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.node.*;
import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.event.PersistenceEvent;
import hu.netmind.beankeeper.event.PersistenceEventListener;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionStatistics;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.db.SearchResult;
import hu.netmind.beankeeper.db.Database;
import hu.netmind.beankeeper.object.PersistenceMetaData;
import hu.netmind.beankeeper.object.ObjectTracker;
import hu.netmind.beankeeper.object.Identifier;
import hu.netmind.beankeeper.serial.Serial;
import hu.netmind.beankeeper.type.TypeHandler;
import hu.netmind.beankeeper.type.TypeHandlerTracker;
import hu.netmind.beankeeper.management.ManagementTracker;
import hu.netmind.beankeeper.node.NodeManager;
import hu.netmind.beankeeper.query.QueryService;
import hu.netmind.beankeeper.schema.SchemaManager;
import hu.netmind.beankeeper.store.event.*;
import hu.netmind.beankeeper.transaction.event.*;
import javax.sql.DataSource;

/**
 * This store class is the entry point to the persistence library. To
 * store, remove or select given objects, just use the appropriate
 * methods.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class StoreServiceImpl implements StoreService
{
   private static Logger logger = Logger.getLogger(StoreServiceImpl.class);
   private static Logger operationsLogger = Logger.getLogger("hu.netmind.beankeeper.operations");

   private StoreStatistics storeStatistics = null;
   private Map<Transaction,TransactionContentImpl> transactionContent = new HashMap<Transaction,TransactionContentImpl>();

   private EventDispatcher eventDispatcher = null; // Injected
   private ManagementTracker managementTracker = null; // Injected
   private TransactionTracker transactionTracker = null; // Injected
   private ObjectTracker objectTracker = null; // Injected
   private NodeManager nodeManager = null; // Injected
   private SerialTracker serialTracker = null; // Injected
   private Database database = null; // Injected
   private ClassTracker classTracker = null; // Injected
   private LockTracker lockTracker = null; // Injected
   private TypeHandlerTracker typeHandlerTracker = null; // Injected
   private ConfigurationTracker config = null; // Injected
   private QueryService queryService = null; // Injected
   private SchemaManager schemaManager = null; // Injected

   /**
    * Construct this store with the given parameters.
    */
   public void init(Map parameters)
   {
      // {{{ Init the store
      // Register as event listener
      eventDispatcher.registerListener(new HighTransactionListener(),
            EventDispatcher.PRI_SYSTEM_HIGH);
      eventDispatcher.registerListener(new LowTransactionListener(),
            EventDispatcher.PRI_SYSTEM_LOW);
      // Create the operations mbean and register
      storeStatistics = new StoreStatistics();
      managementTracker.registerBean("StoreStatistics",storeStatistics);      
      // }}}
   }

   /**
    * Release resources.
    */
   public void release()
   {
      // {{{
      managementTracker.deregisterBean("StoreStatistics");      
      // }}}
   }

   /**
    * Get or allocate the transaction content.
    * @return A transaction content for the transaction given.
    */
   public TransactionContentImpl getTransactionContent(Transaction transaction)
   {
      TransactionContentImpl content = null;
      synchronized ( transactionContent )
      {
         content = transactionContent.get(transaction);
         if ( content == null )
         {
            content = new TransactionContentImpl();
            transactionContent.put(transaction,content);
         }
      }
      return content;
   }

   /**
    * Save the given object to the store. 
    * @param obj The object to save.
    * @throws StoreException If save is not successfull.
    */
   public void save(Object obj)
   {
      // {{{ Save
      // Transaction
      Transaction transaction = transactionTracker.getTransaction(TransactionTracker.TX_REQUIRED);
      transaction.begin();
      TransactionStatistics startStats = new TransactionStatistics();
      startStats.add(transaction.getStats());
      long startTime = System.currentTimeMillis();
      try
      {
         // Get transaction content
         TransactionContentImpl content = getTransactionContent(transaction);
         // Get serials
         Long currentSerial = serialTracker.getNextSerial();
         Long currentTxSerial = transaction.getSerial();
         logger.debug("save called, using serial: "+currentSerial+", tx serial: "+currentTxSerial);
         // Save object or insert, possibly recursively.
         // The waitingObjects list is an ordered list which
         // contains all objects currently waiting for saving.
         // First it contains the object to be saved, then if
         // it referenced more objects, those will be appended to the
         // list. An object can only be saved, when all referencing objects
         // are all at least created.
         LinkedList events = new LinkedList();
         LinkedList savedHandledObjects = new LinkedList();
         HashSet waitingObjects = new HashSet();
         waitingObjects.add(objectTracker.getWrapper(obj));
         while ( waitingObjects.size() > 0 )
         {
            if ( logger.isDebugEnabled() )
               logger.debug("saving object, waiting list: "+waitingObjects.size()+", memory: "+Runtime.getRuntime().freeMemory());
            // {{{ Selecting objects which wait for saving
            // Get the last object (the bottom of dependency tree)
            ObjectTracker.ObjectWrapper currentWrapper = (ObjectTracker.ObjectWrapper) waitingObjects.iterator().next();
            Object current = currentWrapper.getObject();
            // Now get object's class info. This method does not
            // return null. If the class info is not available, it
            // creates it, if it is available it checks whether to
            // update the info in database (new attibutes, etc)
            ClassInfo classInfo = classTracker.getClassInfo(current.getClass(),current);
            logger.debug("object class info is: "+classInfo);
            // Makeing sure schema is up to date
            schemaManager.ensureSchema(classInfo.getSourceEntry());
            // If object does not exists, register it with object
            // tracker. Note, that object tracker only leases an id
            // and exists() will return false until object commited.
            // Also, if object already has an id, this will do nothing.
            objectTracker.registerObject(current);
            Long currentId = objectTracker.getIdentifier(current);
            logger.debug("saving object with id: "+currentId);
            // Lock object
            lockTracker.lock(current);
            content.addSavedObject(currentWrapper);
            // Assemble changed attributes of this object into a Map.
            // If an attribute is not a primitive type it is checked,
            // if it exists. If it does not, it is appended to waitingObjects
            // and we start from beginning with this object. If it exists,
            // then the reference id will be inserted into the change Map.
            Map changedAttributes = new HashMap();
            // }}}
            // {{{ Assemble previous state of object
            Map originalAttributes = objectTracker.getCurrentAttributes(current);
            TimeControl originalTimeControl = new TimeControl(currentSerial,currentTxSerial,true);
            if ( (objectTracker.exists(current)) && (originalAttributes==null) )
            {
               // First, get all original attributes from 
               // the database, because we'll have to create a full row, 
               // even when a single attribute changed.
               ClassEntry selectClassEntry = classInfo.getSourceEntry();
               SpecifiedTableTerm selectTerm = new SpecifiedTableTerm(schemaManager.getTableName(selectClassEntry),null);
               List relatedClassEntries = classTracker.getRelatedClassEntries(selectClassEntry);
               for ( int u=0; u<relatedClassEntries.size(); u++ )
               {
                  ClassEntry relatedClassEntry = (ClassEntry) relatedClassEntries.get(u);
                  ClassInfo relatedClassInfo = classTracker.getClassInfo(relatedClassEntry);
                  if ( relatedClassInfo == null )
                     throw new ParserException(ParserException.ABORT,"object class not found for loading: '"+relatedClassEntry+"'");
                  // Create the term and add to mainterm
                  TableTerm leftTerm = new TableTerm(schemaManager.getTableName(relatedClassEntry),null);
                  SpecifiedTableTerm.LeftjoinEntry joinEntry = new SpecifiedTableTerm.LeftjoinEntry();
                  Expression joinExpr = new Expression();
                  joinExpr.add(new ReferenceTerm(selectTerm,"persistence_id"));
                  joinExpr.add("=");
                  joinExpr.add(new ReferenceTerm(leftTerm,"persistence_id"));
                  joinExpr.add("and");
                  originalTimeControl.apply(joinExpr,leftTerm);
                  joinEntry.expression=joinExpr;
                  joinEntry.term=leftTerm;
                  selectTerm.getRelatedLeftTerms().add(joinEntry);
               }
               Expression expr = new Expression();
               expr.add(new ReferenceTerm(selectTerm,"persistence_id"));
               expr.add("=");
               expr.add(new ConstantTerm(currentId));
               expr.add("and");
               originalTimeControl.apply(expr,selectTerm);
               QueryStatement referredStatement = new QueryStatement(selectTerm,expr,null);
               referredStatement.setStaticRepresentation("FIND "+selectClassEntry+" where persistence_id = "+currentId);
               referredStatement.setTimeControl(originalTimeControl);
               SearchResult referredResult = queryService.find(referredStatement,null);
               if ( referredResult.getResultSize() > 1 )
               {
                  throw new StoreException("object's last state was ambigous, results for object id '"+currentId+"' was: "+referredResult.getResult());
               } else if ( referredResult.getResultSize() == 1 ) {
                  // Set original attributes from this last state of object
                  originalAttributes = (Map) referredResult.getResult().get(0);
                  if ( logger.isDebugEnabled() )
                     logger.debug("object original attributes: "+originalAttributes);
               } else {
                  // Object was not found, it most likely was deleted
                  // so mark object as non-existent. This is a trick to
                  // again save all attributes to database. For example,
                  // when selecting an old instance, it is possible, the
                  // instance is deleted in present time, so we must save
                  // all attributes.
                  objectTracker.makeUnexist(current);
               }
            } else {
               if ( logger.isDebugEnabled() )
                  logger.debug("object did not exist, or original attributes known: "+originalAttributes);
            }
            // Set attribute maps correctly:
            // - nonchangedAttributes will be modified to contain only modified attrs
            // - originalAttributes won't be modified
            // - newAttributes will be the current state
            Map nonchangedAttributes = new HashMap();
            Map newAttributes = new HashMap();
            if ( originalAttributes != null )
            {
               nonchangedAttributes = new HashMap(originalAttributes);
               newAttributes = new HashMap(originalAttributes);
            } else {
               originalAttributes = new HashMap();
            }
            // }}}
            // {{{ Assembling changed attributes
            List attributeNames = classInfo.getAttributeNames();
            if ( logger.isDebugEnabled() )
               logger.debug("class tracker reported object to save has following attributes: "+
                     attributeNames+", it has id: "+currentId+", object tracker says it exists: "+objectTracker.exists(current));
            for ( int i=0; i<attributeNames.size(); i++ )
            {
               String attributeName = (String) attributeNames.get(i);
               String attributeNameLowerCase = attributeName.toLowerCase();
               // Do not handle special attributes
               if ( ("persistence_id".equals(attributeNameLowerCase)) || 
                     ("persistenceid".equals(attributeNameLowerCase)) )
               {
                  // We found a persistence id, fill it with id
                  classInfo.setAttributeValue(current,attributeName,currentId);
                  continue;
               }
               // Skip reserved prefix'd attributes
               if ( attributeNameLowerCase.startsWith("persistence") )
                  continue;
               // Handle non-special attributes
               Object attributeValue = classInfo.getAttributeValue(current,attributeName);
               logger.debug("saving attribute: "+attributeName+", if changed.");
               if ( objectTracker.hasChanged(classInfo,current,attributeName,nonchangedAttributes,currentSerial) )
               {
                  // Current object's current attribute has changed,
                  // so arrange it's save.
                  // This is a switch for the type, this should be
                  // replaced with something more adequate. For
                  // example a type manager who will handle types
                  // and is extensible.
                  Class attributeType = classInfo.getAttributeType(attributeName);
                  switch ( classTracker.getType(attributeType) )
                  {
                     case TYPE_PRIMITIVE:
                        logger.debug("changing primitive attribute: "+attributeName+", value: "+current);
                        // Primitive type, just add to attributes
                        changedAttributes.put(attributeName,attributeValue);
                        break;
                     case TYPE_HANDLED:
                        logger.debug("changing handled attribute: "+attributeName+", type: "+attributeType);
                        TypeHandler handler = typeHandlerTracker.getHandler(attributeType);
                        HashSet saveTables = new HashSet();
                        HashSet removeTables = new HashSet();
                        // Call handler save
                        Object newValue = handler.save(classInfo,current,attributeName,
                              transaction,currentSerial,
                              attributeValue,waitingObjects,saveTables,removeTables,events,
                              changedAttributes,nonchangedAttributes);
                        // Save the handled objects for later use, they need to
                        // be notified when all objects are saved
                        if ( newValue != null )
                        {
                           savedHandledObjects.add(handler);
                           savedHandledObjects.add(newValue);
                        }
                        // Add it's saved and remove tables to the transaction content
                        for ( Object table : saveTables )
                           content.addSaveTable((String) table);
                        for ( Object table : removeTables )
                           content.addRemoveTable((String) table);
                        break;
                     case TYPE_OBJECT:
                        logger.debug("attribute decided as custom object '"+attributeName+
                              "', type is: "+classInfo.getAttributeType(attributeName));
                        // Object type
                        if ( attributeValue == null )
                        {
                           // Object type, but null
                           changedAttributes.put(attributeName,null);
                        } else {
                           if ( ! objectTracker.exists(attributeValue) )
                           {
                              // Object referred does not exists, so it will
                              // have to be created after we're done
                              waitingObjects.add(objectTracker.getWrapper(attributeValue));
                           }
                           objectTracker.registerObject(attributeValue);
                           Long objectId = objectTracker.getIdentifier(attributeValue);
                           // Got id, so give it to the man
                           changedAttributes.put(attributeName,objectId);
                        }
                        break;
                     default:
                        throw new StoreException("unknown type in attribute: "+attributeName+", value was: "+attributeValue);
                  }
               }
            } // End of iterating over attributes
            newAttributes.putAll(changedAttributes);
            // }}}
            // {{{ Saving the object's changed attributes
            // Now do the database thing on the assembled changed 
            // attributes, since the waitingObjects list did not
            // change, meaning no new dependencies were discovered.
            // All changes are saved in multiple save/inserts according
            // to class structure.
            if ( logger.isDebugEnabled() )
               logger.debug("object (of entry: "+classInfo+") will be saved into entries: "+classInfo.getClassEntries());
            Iterator strictClassEntriesIterator = classInfo.getClassEntries().iterator();
            while ( strictClassEntriesIterator.hasNext() )
            {
               // Assemble changes for this strict class
               ClassEntry entry = (ClassEntry) strictClassEntriesIterator.next();
               HashMap strictChanges = new HashMap();
               HashMap strictNonChanges = new HashMap();
               List strictAttributeNames = classInfo.getAttributeNames(entry);
               if ( logger.isDebugEnabled() )
                  logger.debug("assembling changed for class: "+entry+", strict attributes: "+strictAttributeNames);
               for ( int i=0; i<strictAttributeNames.size() ; i++ )
               {
                  String attributeName = (String) strictAttributeNames.get(i);
                  Class attributeType = classInfo.getAttributeType(attributeName);
                  TypeHandler handler = typeHandlerTracker.getHandler(attributeType);
                  if ( handler == null )
                  {
                     // No handler, this is a simple attribute
                     if ( changedAttributes.containsKey(attributeName) )
                        strictChanges.put(attributeName,changedAttributes.remove(attributeName));
                     if ( nonchangedAttributes.containsKey(attributeName) )
                        strictNonChanges.put(attributeName,nonchangedAttributes.remove(attributeName));
                  } else {
                     // Got handler, then iterate on it's attributes
                     Map embeddedAttributes = handler.getAttributeTypes(attributeName);
                     if ( logger.isDebugEnabled() )
                        logger.debug("attribute: "+attributeName+", was an embedded attribute, adding: "+embeddedAttributes);
                     Iterator embeddedAttributeNamesIterator = embeddedAttributes.keySet().iterator();
                     while ( embeddedAttributeNamesIterator.hasNext() )
                     {
                        attributeName = (String) embeddedAttributeNamesIterator.next();
                        if ( changedAttributes.containsKey(attributeName) )
                           strictChanges.put(attributeName,changedAttributes.remove(attributeName));
                        if ( nonchangedAttributes.containsKey(attributeName) )
                           strictNonChanges.put(attributeName,nonchangedAttributes.remove(attributeName));
                     }
                  }
               }
               // Make changes
               if ( objectTracker.exists(current) )
               {
                  // Save
                  logger.debug("changing object with following attributes: "+strictChanges+", not changed: "+strictNonChanges);
                  if ( strictChanges.size() > 0 )
                  {
                     // First set enddate on used entry
                     HashMap removeChanges = new HashMap();
                     HashMap keys = new HashMap();
                     keys.put("persistence_id",currentId);
                     keys.put("persistence_end",Serial.getMaxSerial().getValue());
                     keys.put("persistence_txend",Serial.getMaxSerial().getValue());
                     removeChanges.put("persistence_txend",currentSerial);
                     removeChanges.put("persistence_txendid",currentTxSerial);
                     database.save(transaction,schemaManager.getTableName(entry),
                           keys, removeChanges);
                     content.addRemoveTable(schemaManager.getTableName(entry));
                     // Create new entry
                     strictNonChanges.putAll(strictChanges);
                     strictNonChanges.put("persistence_id", currentId);
                     strictNonChanges.put("persistence_start", Serial.getMaxSerial().getValue());
                     strictNonChanges.put("persistence_end", Serial.getMaxSerial().getValue());
                     strictNonChanges.put("persistence_txendid",new Long(0));
                     strictNonChanges.put("persistence_txstart", currentSerial);
                     strictNonChanges.put("persistence_txstartid", currentTxSerial);
                     strictNonChanges.put("persistence_txend", Serial.getMaxSerial().getValue());
                     database.insert(transaction,schemaManager.getTableName(entry),
                           strictNonChanges);
                     // Add to modified tables list
                     content.addSaveTable(schemaManager.getTableName(entry));
                  }
               } else {
                  // Insert
                  logger.debug("inserting object with following attributes: "+strictChanges);
                  strictChanges.put("persistence_id", currentId);
                  strictChanges.put("persistence_start", Serial.getMaxSerial().getValue());
                  strictChanges.put("persistence_end", Serial.getMaxSerial().getValue());
                  strictChanges.put("persistence_txendid",new Long(0));
                  strictChanges.put("persistence_txstart", currentSerial);
                  strictChanges.put("persistence_txstartid", currentTxSerial);
                  strictChanges.put("persistence_txend", Serial.getMaxSerial().getValue());
                  database.insert(transaction,schemaManager.getTableName(entry),
                        strictChanges);
                  // Add to modified tables list
                  content.addSaveTable(schemaManager.getTableName(entry));
               }
            }
            if ( changedAttributes.size() != 0 )
               logger.warn("there are attributes that do not belong in any superclass of object to be saved, classinfo: "+
                     classInfo+", attributes: "+changedAttributes);
            logger.debug("saving object with id: "+currentId+" updating meta-data.");
            // Remove object from waiting list
            waitingObjects.remove(currentWrapper);
            // Notify event listeners
            if ( objectTracker.exists(current) )
            {
               // Send modify event. For this, we have to re-create
               // the original state of the object.
               events.add(new ModifyObjectEvent(classTracker,objectTracker,typeHandlerTracker,
                        queryService,originalAttributes,originalTimeControl,current));
            } else {
               // Create
               events.add(new CreateObjectEvent(current));
            }
            // Update object tracker
            objectTracker.makeExist(current); // Exists for this transaction
            objectTracker.updateObject(current,newAttributes);
            // }}}
            logger.debug("saving object with id: "+currentId+" finished.");
         }
         // {{{ Go through handled objects, and notify that save ended
         for ( int i=0; i<savedHandledObjects.size(); )
         {
            TypeHandler handler = (TypeHandler) savedHandledObjects.get(i++);
            Object value = (Object) savedHandledObjects.get(i++);
            handler.postSave(value);
         }
         // }}}
         // Debug code
         if ( operationsLogger.isDebugEnabled() )
         {
            TransactionStatistics stats = new TransactionStatistics();
            stats.add(transaction.getStats());
            stats.substract(startStats);
            operationsLogger.debug("operation save: "+obj+", "+stats);
            if ( operationsLogger.isDebugEnabled() )
               operationsLogger.debug("previous operation trace:",new Exception("trace"));
         }
         // {{{ Notify dispatcher of events occured during save
         for ( int i=0; i<events.size(); i++ )
            eventDispatcher.notify((PersistenceEvent) events.get(i));
         // }}}
         // "Happy, happy, joy, joy". Object saved.
      } catch ( StoreException e ) {
         transaction.markRollbackOnly();
         logger.error("throwing store exception",e);
         throw e;
      } catch ( Throwable e ) {
         transaction.markRollbackOnly();
         logger.error("throwing unexpected exception",e);
         throw new StoreException("unexpected exception",e);
      } finally {
         transaction.commit();
         // Add to statistics
         long endTime = System.currentTimeMillis();
         synchronized ( storeStatistics )
         {
            storeStatistics.setSaveCount(storeStatistics.getSaveCount()+1);
            storeStatistics.setSaveTime(storeStatistics.getSaveTime()+(endTime-startTime));
         }
      }
      /// }}}
   }

   /**
    * Remove the object given. If the object is not stored yet, no
    * operation will take place.
    * @param obj The object to remove.
    * @throws StoreException If remove is not successfull.
    */
   public void remove(Object obj)
   {
      // {{{ Remove object
      // Transaction
      Transaction transaction = transactionTracker.getTransaction(TransactionTracker.TX_REQUIRED);
      transaction.begin();
      TransactionStatistics startStats = new TransactionStatistics();
      startStats.add(transaction.getStats());
      long startTime = System.currentTimeMillis();
      try
      {
         // Get transaction content
         TransactionContentImpl content = getTransactionContent(transaction);
         // Get id, and at same time register into object tracker,
         // later attempt remove only if object really exists
         Long id = objectTracker.getMetaData(obj).getPersistenceId();
         if ( ! objectTracker.exists(obj) )
         {
            logger.debug("object does not exists, so remove will not be attempted");
            return;
         }
         Long currentSerial = serialTracker.getNextSerial();
         Long currentTxSerial = transaction.getSerial();
         // Lock object
         lockTracker.lock(obj);
         content.addRemovedObject(objectTracker.getWrapper(obj));
         // Get class info
         ClassInfo classInfo = classTracker.getClassInfo(obj.getClass(),obj);
         // Assemble attributes
         Map removeChanges = new HashMap();
         logger.debug("remove called, using serial: "+currentSerial);
         removeChanges.put("persistence_txend", currentSerial);
         removeChanges.put("persistence_txendid", currentTxSerial);
         Map keys = new HashMap();
         keys.put("persistence_id",id);
         keys.put("persistence_end",Serial.getMaxSerial().getValue());
         // Execute remove on class and all superclasses, et voila'
         for ( int i=0; i<classInfo.getClassEntries().size(); i++ )
         {
            ClassEntry entry = (ClassEntry) classInfo.getClassEntries().get(i);
            database.save(transaction,schemaManager.getTableName(entry),keys , removeChanges);
            // Add changed table
            content.addRemoveTable(schemaManager.getTableName(entry));
         }
         // Notify object tracker
         objectTracker.makeUnexist(obj);
         objectTracker.updateObject(obj,null); // Update to null cached attributes
         // Notify event listeners
         eventDispatcher.notify(new DeleteObjectEvent(obj));
         // Debug code
         if ( operationsLogger.isDebugEnabled() )
         {
            TransactionStatistics stats = new TransactionStatistics();
            stats.add(transaction.getStats());
            stats.substract(startStats);
            operationsLogger.debug("operation remove: "+obj+" (id: "+id+"), "+stats);
            if ( operationsLogger.isDebugEnabled() )
               operationsLogger.debug("previous operation trace:",new Exception("trace"));
         }
      } catch ( StoreException e ) {
         transaction.markRollbackOnly();
         logger.error("throwing store exception",e);
         throw e;
      } catch ( Throwable e ) {
         transaction.markRollbackOnly();
         logger.error("throwing unexpected exception",e);
         throw new StoreException("unexpected exception",e);
      } finally {
         transaction.commit();
         // Add to statistics
         long endTime = System.currentTimeMillis();
         synchronized ( storeStatistics )
         {
            storeStatistics.setRemoveCount(storeStatistics.getRemoveCount()+1);
            storeStatistics.setRemoveTime(storeStatistics.getRemoveTime()+(endTime-startTime));
         }
      }
      /// }}}
   }

   // {{{ Helper classes
   /**
    * This listener finishes a transaction by unlocking all objects.
    */
   private class HighTransactionListener implements PersistenceEventListener
   {
      public void handle(PersistenceEvent event)
      {
         if ( ! (event instanceof TransactionEvent) )
            return; // Quick exit
         Transaction transaction = ((TransactionEvent) event).getTransaction();
         if ( (event instanceof TransactionRolledbackEvent) ||
           (event instanceof TransactionCommittedEvent) )
         {
            // Remove transaction contents.
            TransactionContentImpl content = getTransactionContent(transaction);
            if ( content.isEmpty() )
               return; // No need to do anything, if there were no modifications
            // Unlock objects. Unlock event must come after everything about a
            // transaction is complete, because as soon as objects are unlocked,
            // other threads or nodes might cause interference with commit or
            // committed listeners.
            // If this event does not reach the server, the communication
            // error will cause the server to unlock all objects anyway.
            // TODO: do NOT send update event to object tracker, let it listen
            // for events.
            ArrayList objects = new ArrayList();
            Iterator removedObjectsIterator = content.getRemovedObjects().iterator();
            while ( removedObjectsIterator.hasNext() )
            {
               Object obj = ((ObjectTracker.ObjectWrapper)removedObjectsIterator.next()).getObject();
               objectTracker.updateObject(
                     obj,transaction.getEndSerial(),null,transaction.getEndSerial());
               objects.add(obj);
            }
            Iterator savedObjectsIterator = content.getSavedObjects().iterator();
            while ( savedObjectsIterator.hasNext() )
            {
               Object obj = ((ObjectTracker.ObjectWrapper)savedObjectsIterator.next()).getObject();
               objectTracker.updateObject(
                     obj,transaction.getEndSerial(),transaction.getEndSerial(),null);
               objects.add(obj);
            }
            lockTracker.unlock(objects.toArray());
         }
      }
   }

   /**
    * This listener is here for finishing a committed transactions by
    * finalizing changes to the ens serial, and modifying their metadata when successful.
    */
   private class LowTransactionListener implements PersistenceEventListener
   {
      public void handle(PersistenceEvent event)
         throws Exception
      {
         if ( ! (event instanceof TransactionEvent) )
            return; // Quick exit
         Transaction transaction = ((TransactionEvent) event).getTransaction();
         // Get transaction contents.
         TransactionContentImpl content = getTransactionContent(transaction);
         if ( content.isEmpty() )
            return; // No need to do anything, if there were no modifications
         List objects = new ArrayList();
         objects.addAll(content.getRemovedObjects());
         objects.addAll(content.getSavedObjects());
         Long serial = transaction.getEndSerial();
         // Commit object updates
         if ( event instanceof TransactionCommittedEvent )
         {
            // We must update objects to reflect the changes we made
            Iterator objectsIterator = objects.iterator();
            while ( objectsIterator.hasNext() )
            {
               Object obj = ((ObjectTracker.ObjectWrapper)objectsIterator.next()).getObject();
               objectTracker.updateCommit(obj,serial);
            }
         }
         if ( event instanceof TransactionRolledbackEvent )
         {
            // We must update objects to reflect the changes we made
            Iterator objectsIterator = objects.iterator();
            while ( objectsIterator.hasNext() )
            {
               Object obj = ((ObjectTracker.ObjectWrapper)objectsIterator.next()).getObject();
               objectTracker.updateRollback(obj,serial);
            }
         }
         // Make all changes permanent and visible here
         if ( event instanceof TransactionCommitEndingEvent )
         {
            // All operations done must be finalized now. If an exception occurs,
            // the transaction tracker will roll back the transaction anyway,
            // so we don't have to worry about that.
            // Save: set startdates where startdate is maxdate
            List saveTables = content.getSaveTables();
            HashMap keys = new HashMap();
            keys.put("persistence_txstartid",transaction.getSerial());
            HashMap changes = new HashMap();
            changes.put("persistence_start",transaction.getEndSerial());
            for ( int i=0; i<saveTables.size(); i++ )
            {
               String tableName = (String) saveTables.get(i);
               logger.debug("fixing save table: "+tableName);
               database.save(transaction,tableName,keys,changes);
            }
            // Remove: set enddates where txenddate is not maxdate
            List removeTables = content.getRemoveTables();
            keys = new HashMap();
            keys.put("persistence_txendid",transaction.getSerial());
            changes = new HashMap();
            changes.put("persistence_end",transaction.getEndSerial());
            for ( int i=0; i<removeTables.size(); i++ )
            {
               String tableName = (String) removeTables.get(i);
               logger.debug("fixing remove table: "+tableName);
               database.save(transaction,tableName,keys,changes);
            }
            // Notify the server of all objects that changed. This operation
            // must be before the commit physically occurs, because this notification
            // will cause the server to know which objects are modified.
            List metas = new ArrayList();
            for ( int i=0; i<objects.size(); i++ )
               metas.add(objectTracker.getMetaData(((ObjectTracker.ObjectWrapper)objects.get(i)).getObject()));
            if ( logger.isDebugEnabled() )
               logger.debug("sending changed objects' meta to all: "+metas);
            // TODO (DODGY): We should ensure that this call does not return until
            // all nodes finished processing of event. If we don't, it's possible that
            // a client does not clear cache for example but after the commit lock is
            // freed. Which would mean it might allow for an inconsistent query.
            nodeManager.callAll(StoreService.class.getName(),"notifyChange",
                  new Class[] { List.class, Long.class, Long.class },
                  new Object[] { metas, transaction.getEndSerial(), transaction.getSerial() });
            logger.debug("sending changed objects finished.");
         }
      }
   }

   public void notifyChange(List<PersistenceMetaData> metas, Long serial, Long txSerial)
   {
      eventDispatcher.notifyAll(new ObjectsFinalizationEvent(metas,serial,txSerial));
   }

   public static class TransactionContentImpl
   {
      // Store's attributes
      private LinkedList savedObjectList;
      private LinkedList removedObjectList;
      private HashSet saveTables;
      private HashSet removeTables;

      private TransactionContentImpl()
      {
         savedObjectList = new LinkedList();
         removedObjectList = new LinkedList();
         saveTables = new HashSet();
         removeTables = new HashSet();
      }

      private void addSavedObject(Object obj)
      {
         savedObjectList.add(obj);
      }

      /**
       * Get the objects saved in this transaction as a list.
       */
      public List getSavedObjects()
      {
         return Collections.unmodifiableList(savedObjectList);
      }

      private void addRemovedObject(Object obj)
      {
         removedObjectList.add(obj);
      }

      /**
       * Get the objects removed in this transaction as a list.
       */
      public List getRemovedObjects()
      {
         return Collections.unmodifiableList(removedObjectList);
      }

      public boolean isEmpty()
      {
         return savedObjectList.isEmpty() && removedObjectList.isEmpty();
      }

      private void addSaveTable(String table)
      {
         saveTables.add(table);
      }
      private void setSaveTables(List saveTables)
      {
         this.saveTables = new HashSet(saveTables);
      }
      private List getSaveTables()
      {
         return new ArrayList(saveTables);
      }
      private void addRemoveTable(String table)
      {
         removeTables.add(table);
      }
      private void setRemoveTables(List removeTables)
      {
         this.removeTables = new HashSet(removeTables);
      }
      private List getRemoveTables()
      {
         return new ArrayList(removeTables);
      }

   }
   // }}}
}



