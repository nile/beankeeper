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

import org.apache.log4j.Logger;
import java.util.*;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.query.LazyList;
import hu.netmind.beankeeper.query.LazyListHooks;
import hu.netmind.beankeeper.query.QueryService;
import hu.netmind.beankeeper.serial.SerialTracker;
import hu.netmind.beankeeper.lock.LockTracker;
import hu.netmind.beankeeper.config.ConfigurationTracker;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.node.*;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionStatistics;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.db.Limits;
import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.event.PersistenceEventListener;
import hu.netmind.beankeeper.event.PersistenceEvent;
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
import hu.netmind.beankeeper.operation.OperationTracker;
import hu.netmind.beankeeper.cache.ResultsCache;
import hu.netmind.beankeeper.schema.SchemaManager;
import hu.netmind.beankeeper.store.event.*;
import hu.netmind.beankeeper.transaction.event.*;
import javax.sql.DataSource;

/**
 * Implements the query interface to offer query methods based on
 * programmatic and freetext queries.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class QueryServiceImpl implements QueryService, PersistenceEventListener
{
   private static Logger logger = Logger.getLogger(QueryServiceImpl.class);
   private static Logger operationsLogger = Logger.getLogger("hu.netmind.beankeeper.operations");

   private QueryStatistics queryStatistics = null;
   private Map<Transaction,Set<String>> transactionTables = Collections.synchronizedMap(new HashMap<Transaction,Set<String>>());

   private ManagementTracker managementTracker = null; // Injected
   private TransactionTracker transactionTracker = null; // Injected
   private ObjectTracker objectTracker = null; // Injected
   private SerialTracker serialTracker = null; // Injected
   private Database database = null; // Injected
   private ClassTracker classTracker = null; // Injected
   private TypeHandlerTracker typeHandlerTracker = null; // Injected
   private OperationTracker operationTracker = null; // Injected
   private ResultsCache cache = null; // Injected
   private ConfigurationTracker config = null; // Injected
   private EventDispatcher eventDispatcher = null; // Injected
   private SchemaManager schemaManager = null; // Injected

   /**
    * Construct this store with the given parameters.
    */
   public void init(Map parameters)
   {
      // Register
      eventDispatcher.registerListener(this);
      // Create the operations mbean and register
      queryStatistics = new QueryStatistics();
      managementTracker.registerBean("QueryStatistics",queryStatistics);      
   }

   /**
    * Release resources.
    */
   public void release()
   {
      eventDispatcher.unregisterListener(this);
      managementTracker.deregisterBean("QueryStatistics");      
   }

   /**
    * Query an object from the datastore.
    * @param statement The query statement to select.
    */
   public LazyList find(String statement)
   {
      return find(statement,null,null,null);
   }

   /**
    * Same as <code>find(statement)</code>. When a statement contains
    * the question mark (?), the object which should be in the place of
    * the mark should be given as parameters (parameters are usually of
    * Date, or custom classes).
    * @param statement The query statement to execute.
    * @param parameters The parameters.
    */
   public LazyList find(String statement, Object[] parameters)
   {
      return find(statement,parameters,null,null);
   }

   /**
    * This method in addition to all usual parameters can define the
    * exact time of the query with the timeControl parameter. If the
    * query is a historical query, this control will be overridden.
    * @param statement The query statement to execute.
    * @param parameters The parameters.
    * @param timeControl The exact default time of the query.
    */
   public LazyList find(String statement, Object[] parameters, TimeControl timeControl, Map unmarshalledObjects)
   {
      // {{{ Parse statement, and create result list
      // Convert object parameters. If they contain
      // objects, substitute with object id
      Transaction transaction = transactionTracker.getTransaction(TransactionTracker.TX_REQUIRED);
      transaction.begin();
      long startTime = System.currentTimeMillis();
      try
      {
         Object[] realParameters = null;
         if ( parameters != null )
         {
            realParameters = new Object[parameters.length];
            for ( int i=0; i<parameters.length; i++ )
            {
               if ( parameters[i] == null )
               {
                  // Handle null parameters
                  realParameters[i]=null;
               } else {
                  // If parameter is not null, translate object parameters
                  // to persistent id, leave others
                  ClassTracker.ClassType type = classTracker.getType(parameters[i].getClass());
                  if ( type == ClassTracker.ClassType.TYPE_RESERVED )
                     throw new StoreException("parameter at position: "+i+", value: "+parameters[i]+" is of unsupported type.");
                  if ( (type == ClassTracker.ClassType.TYPE_OBJECT) && 
                        (!(parameters[i] instanceof Collection)) && 
                        (!(parameters[i] instanceof Identifier)) && 
                        (!(parameters[i] instanceof TimeControl)) )
                  {
                     // If object has no id, that's not a problem. It will
                     // receive an id of 0, and no object should match
                     // that id anyway.
                     realParameters[i]=new Identifier(objectTracker.getIdentifier(parameters[i]));
                  } else if ( parameters[i] instanceof Collection ) {
                     // Check, whether the collection's items are objects,
                     // in which case translate them to their ids
                     ArrayList result = new ArrayList();
                     Iterator itemIterator = ((Collection) parameters[i]).iterator();
                     while ( itemIterator.hasNext() )
                     {
                        Object item = itemIterator.next();
                        if ( classTracker.getType(item.getClass()) == ClassTracker.ClassType.TYPE_OBJECT )
                           result.add(objectTracker.getIdentifier(item));
                        else
                           result.add(item);
                     }
                     realParameters[i]=result;
                  } else {
                     realParameters[i]=parameters[i];
                  }
               }
               if ( logger.isDebugEnabled() )
                  logger.debug("parameter: "+parameters[i]+" -> real parameter #"+i+":"+realParameters[i]);
            }
         }
         // Process it, get expression
         QueryStatementList stmts = null;
         try
         {
            // Only apply in-transaction search conditions if there is a transaction
            // and something changed during transaction. Only calculate if
            // no 'default default' is given.
            Set modifiedTables = transactionTables.get(transaction);
            if ( modifiedTables == null )
               modifiedTables = new HashSet();
            if ( timeControl == null )
            {
               Long serial = serialTracker.getNextSerial();
               Long txSerial = transaction.getSerial();
               timeControl = new TimeControl(serial,txSerial,false);
            }
            // Parse statement
            if ( logger.isDebugEnabled() )
               logger.debug("executing parser, serial: "+timeControl.getSerial()+", tx serial: "+timeControl.getTxSerial());
            stmts = new QueryParser(statement,realParameters, new WhereResolver(classTracker,typeHandlerTracker,
                     schemaManager),timeControl,modifiedTables).query();
            // Wait now for all commits() before the given serial to finish.
            // If this would not be the case, the lazy list might not
            // contain the data from previously initiated commits. Which
            // would mean, once those finished, the lazy list would change.
            operationTracker.waitForQuery(timeControl.getSerial());
         } catch ( ParserException e ) {
            if ( e.getCode() == ParserException.ABORT )
            {
               logger.error("aborting query, because of parser exception.");
               throw new StoreException(e.getMessage(),e);
            } else {
               logger.info("returning empty result list because of non-fatal symbol error. Parser said: "+e.getMessage()+", statement was: "+statement);
               return new EmptyLazyListImpl(); // Return empty list on non-fatal symbol errors
            }
         } catch ( StoreException e ) {
            throw e;
         } catch ( Exception e ) {
            throw new StoreException("unknown exception while select",e);
         }
         // Return list
         return new LazyListImpl(this,classTracker,config,schemaManager,stmts,unmarshalledObjects);
      } catch ( StoreException e ) {
         transaction.markRollbackOnly();
         throw e;
      } catch ( Throwable e ) {
         transaction.markRollbackOnly();
         throw new StoreException("unexpected exception",e);
      } finally {
         transaction.commit();
         // Add to statistics
         long endTime = System.currentTimeMillis();
         synchronized ( queryStatistics )
         {
            queryStatistics.setQueryCount(queryStatistics.getQueryCount()+1);
            queryStatistics.setQueryTime(queryStatistics.getQueryTime()+(endTime-startTime));
         }
      }
      // }}}
   }

   /**
    * Same as <code>find(statement,parameters)</code>, but the result should be
    * a single object.
    * @param statement The query statement to execute.
    * @param parameters The parameters to the statement.
    * @return The object selected, or null if no such object exists. If
    * the result contains more objects, an arbitrary one is selected.
    */
   public Object findSingle(String statement, Object[] parameters)
   {
      // {{{ Call normal find and evaluate result
      Iterator iterator = find(statement,parameters).iterator();
      if ( iterator.hasNext() )
         return iterator.next();
      return null;
      // }}}
   }

   /**
    * Same as <code>find(statement)</code>, but the result should be
    * a single object.
    * @param statement The query statement to execute.
    * @return The object selected, or null if no such object exists. If
    * the result contains more objects, an arbitrary one is selected.
    */
   public Object findSingle(String statement)
   {
      return findSingle(statement,null);
   }

   /**
    * Internal raw loading. All finder methods sooner or later call into
    * this method to get real results in form of attribute name-value maps.
    */
   public SearchResult find(QueryStatement stmt, Limits limits)
   {
      // {{{ Do physical query
      Transaction transaction = transactionTracker.getTransaction(
            TransactionTracker.TX_OPTIONAL);
      if ( transaction == null )
         throw new StoreException("there was not transaction in find");
      // First, check if statement is visible from this transaction
      TimeControl timeControl = stmt.getTimeControl();
      if ( (timeControl.isApplyTransaction()) && (timeControl.getTxSerial()!=null) &&
           (!timeControl.getTxSerial().equals(transaction.getSerial())) &&
           (transactionTracker.hasTransaction(timeControl.getTxSerial())) )
         throw new StoreException("tried to do a query which was outside it's transaction '"+
               timeControl.getTxSerial()+"', which was still open. Current tx was: "+transaction.getSerial());
      // Get resultset from cache or database
      SearchResult result = cache.getEntry(stmt,limits);
      if ( result == null )
      {
         result = database.search(transaction,stmt,limits);
         cache.addEntry(stmt,limits,result);
      }
      return result;
      // }}}
   }
  
   /**
    * Unmarshall an object. This means create an object of given class
    * and set attributes from a given map of attributes. All referred
    * objects are assumed to be in the already allocated list, indexed
    * by object id.
    * @param classInfo The class info of the object that needs to be instantiated.
    * @param marshalledValues The attributes values.
    * @param unmarshalledObjects The already unmarshalled objects.
    */
   private Object unmarshallObject(ClassInfo classInfo, Map marshalledValues, 
         Map unmarshalledObjects, Map missingAttributes, QueryStatement stmt)
      throws InstantiationException, IllegalAccessException
   {
      // {{{ Umarshall object
      Object obj = classInfo.newInstance(marshalledValues);
      if ( obj == null )
         return null;
      // Important! Register object into tracker!
      // Note: all attributes are updated into the object tracker.
      // Previously this was not done, because object's had a shared
      // state (instances of the same database row), but now no shared
      // state exists (at least, not with attributes).
      objectTracker.registerObject(obj,(Long) marshalledValues.get("persistence_id"),
            stmt.getTimeControl().getSerial(),
            ((Long) marshalledValues.get("persistence_start")),
            ((Long) marshalledValues.get("persistence_end")));
      objectTracker.updateObject(obj,marshalledValues);
      unmarshalledObjects.put(marshalledValues.get("persistence_id"),obj);
      // Set properties in obj, go through object attributes
      // (except is the object is of primitive type)
      List attributeNames = classInfo.getAttributeNames();
      for ( int o=0; (o<attributeNames.size()) && 
            (!classInfo.isPrimitive()); o++ )
      {
         String attributeName = attributeNames.get(o).toString();
         // Handle peristence_id specially. If this is an attribute
         // named something like persistenceId, then fill in the id.
         if ( ("persistence_id".equalsIgnoreCase(attributeName)) || 
               ("persistenceid".equalsIgnoreCase(attributeName)) )
         {
            // Fill attribute with persistence id
            classInfo.setAttributeValue(obj,attributeName,marshalledValues.get("persistence_id"));
            continue;
         }
         // Handle other (normal) attributes
         Object attributeValue = marshalledValues.get(attributeName.toLowerCase());
         if ( logger.isDebugEnabled() )
            logger.debug("setting object property: "+attributeName+", value: "+attributeValue);
         Class attributeClass = classInfo.getAttributeType(attributeName);
         if ( attributeClass == null )
         {
            logger.error("object property '"+attributeName+"' cannot set, object has no such property.");
            continue;
         }
         switch ( classTracker.getType(attributeClass) )
         {
            case TYPE_PRIMITIVE:
               classInfo.setAttributeValue(obj,attributeName,attributeValue);
               break;
            case TYPE_HANDLED:
               TypeHandler handler = typeHandlerTracker.getHandler(attributeClass);
               Object value = handler.unmarshallType(
                     classInfo,obj,attributeName,marshalledValues, stmt.getTimeControl());
               classInfo.setAttributeValue(obj,attributeName,value);
               break;
            case TYPE_OBJECT:
               // Handle null
               if ( attributeValue == null )
               {
                  classInfo.setAttributeValue(obj,attributeName,null);
                  break;
               }
               // Get object from list
               Object relatedObj = unmarshalledObjects.get(attributeValue);
               classInfo.setAttributeValue(obj,attributeName,relatedObj);
               if ( relatedObj == null )
               {
                  if ( logger.isDebugEnabled() )
                     logger.debug("referred object of id: "+attributeValue+" not found, currently unmarshalled objects: "+unmarshalledObjects);
                  // Remember with ids entry. An entry holds all necessary
                  // information for a single attribute to reconstruct
                  // it's objects for all referrers:
                  // - classinfo: of attribute declared type (for select)
                  // - ids: all ids of referred objects
                  // - objects: indexed by referred object id, hold a set
                  //   of objects which need the specified referred object
                  IdsEntry idsEntry = (IdsEntry) missingAttributes.get(attributeName);
                  if ( idsEntry == null )
                  {
                     idsEntry = new IdsEntry(new HashMap(),new HashSet(),classInfo);
                     missingAttributes.put(attributeName,idsEntry);
                  }
                  idsEntry.ids.add(attributeValue);
                  List objectsSet = (List) idsEntry.objects.get(attributeValue);
                  if ( objectsSet == null )
                  {
                     objectsSet = new ArrayList();
                     idsEntry.objects.put(attributeValue,objectsSet);
                  }
                  objectsSet.add(obj);
               }
               break;
            default:
               throw new StoreException("attribute: "+attributeName+"'s type was not valid.");
         }
      } // Iteration over attributes
      return obj;
      // }}}
   }

   public void handle(PersistenceEvent event)
   {
      if ( (event instanceof ModifyObjectEvent) || 
            (event instanceof CreateObjectEvent) ||
            (event instanceof DeleteObjectEvent) )
      {
         // Get the table name for the object
         Object object = ((ObjectEvent) event).getObject();
         ClassInfo classInfo = classTracker.getClassInfo(object.getClass(),object);
         String tableName = schemaManager.getTableName(classInfo.getSourceEntry());
         // Get transaction
         Transaction tx = transactionTracker.getTransaction(TransactionTracker.TX_OPTIONAL);
         // Object was modified, enter into the transaction map
         Set<String> modifiedTables = transactionTables.get(tx);
         if ( modifiedTables == null )
         {
            modifiedTables = new HashSet<String>();
            transactionTables.put(tx,modifiedTables);
         }
         modifiedTables.add(tableName);
      }
      if ( (event instanceof TransactionCommittedEvent) ||
            (event instanceof TransactionRolledbackEvent) )
      {
         Transaction tx = ((TransactionEvent) event).getTransaction();
         transactionTables.remove(tx);
      }
   }
   
   /**
    * Internal loading.
    */
   public SearchResult find(QueryStatement stmt, Limits limits, Map unmarshalledObjects)
   {
      // {{{ Internal loading code
      if ( unmarshalledObjects == null )
         unmarshalledObjects = new HashMap();
      Transaction transaction = transactionTracker.getTransaction(TransactionTracker.TX_REQUIRED);
      transaction.begin();
      TransactionStatistics startStats = new TransactionStatistics();
      startStats.add(transaction.getStats());
      long startTime = System.currentTimeMillis();
      logger.debug("called store internal find.");
      try
      {
         // If the query is a view query, then return raw map format,
         // else get the main term which is in this case always the
         // class which to select.
         if ( stmt.getMode() == QueryStatement.MODE_VIEW )
            return find(stmt,limits);
         TableTerm mainTerm = (TableTerm) stmt.getSelectTerms().get(0);
         SpecifiedTableTerm specifiedMainTerm = stmt.getSpecifiedTerm(mainTerm);
         ClassInfo classInfo = classTracker.getClassInfo(
               schemaManager.getClassEntry(mainTerm.getTableName()));
         if ( classInfo == null )
            throw new StoreException("no class found for table name: "+mainTerm.getTableName());
         // Run the real database search
         logger.debug("find running real select statement.");
         SearchResult rawResult = find(stmt,limits);
         // Take the raw data and umarshall them into objects
         logger.debug("find unmarshalling objects.");
         SearchResult cookedResult = new SearchResult();
         HashMap missingAttributes = new HashMap();
         cookedResult.setResultSize(rawResult.getResultSize());
         ArrayList cookedResultList = new ArrayList();
         cookedResult.setResult(cookedResultList);
         for ( int i=0; i<rawResult.getResult().size(); i++ )
         {
            // Get values
            Map marshalledValues = (Map) rawResult.getResult().get(i);
            // If object already unmarshalled, then get from list,
            // else instantiate
            Object obj = unmarshalledObjects.get(marshalledValues.get("persistence_id"));
            if ( obj == null )
            {
               if ( logger.isDebugEnabled() )
                  logger.debug("got marshalled values: "+marshalledValues);
               // Instantiate and unmarshall object
               ClassInfo localClassInfo = classInfo;
               if ( specifiedMainTerm.getRelatedLeftTerms().size() > 0 )
               {
                  // If there were left join tables, try to get the
                  // correct class. This means, that the object
                  // can be the subclass of queried class, and not
                  // exactly that. We can determine the exact class
                  // from the persistence id.
                  Long persistenceId = (Long) marshalledValues.get("persistence_id");
                  ClassEntry localClassEntry = classTracker.getClassEntry(new Identifier(persistenceId).getClassId());
                  localClassInfo = classTracker.getClassInfo(localClassEntry);
               }
               // Unmarshall, with the exact class info given
               obj = unmarshallObject(localClassInfo,marshalledValues,
                     unmarshalledObjects,missingAttributes,stmt);
            }
            // Add object to result list. The 'obj' is an umarshalled full
            // object, which is gethered from the main table of the query.
            // How ever, if there are other referenced attributes the
            // query should return, then we do the whole thing into a Map.
            // The object itself will have the key 'object' in this case.
            if ( stmt.getSelectTerms().size() > 1 )
            {
               // There are other attributes the caller wants, so
               // do the whole thing into a Map. Insert all wanted attributes,
               // and the unmarshalled main object too.
               HashMap resultObj  = new HashMap();
               for ( int o=1; o<stmt.getSelectTerms().size(); o++ )
               {
                  ReferenceTerm refTerm = (ReferenceTerm) stmt.getSelectTerms().get(o);
                  resultObj.put(refTerm.getColumnFinalName(),marshalledValues.get(refTerm.getColumnFinalName().toLowerCase()));
               }
               resultObj.put("object",obj);
               cookedResultList.add(resultObj);
            } else {
               // There was just the object, so insert it into the result
               // list, just in itself.
               cookedResultList.add(obj);
            }
         }
         // Load all referred objects for all classes which were unmarshalled,
         // the missing list was assembled in the unmarshall code.
         Iterator missingAttributesIterator = missingAttributes.entrySet().iterator();
         while ( missingAttributesIterator.hasNext() )
         {
            // Get all necessary meta-data
            Map.Entry entry = (Map.Entry) missingAttributesIterator.next();
            String attributeName = entry.getKey().toString();
            IdsEntry idsEntry = (IdsEntry) entry.getValue();
            Class selectClass = idsEntry.classInfo.getAttributeType(attributeName);
            // We got the class of the object, so we select all objects to
            // this attribute into a map keyed with the persistence id.
            HashMap referredObjects = new HashMap();
            if ( logger.isDebugEnabled() )
               logger.debug("getting member attribute: "+selectClass+", for ids: "+idsEntry.ids);
            List referredObjectList = find("find member("+selectClass.getName()+") where member in ?",new Object[] {idsEntry.ids},null,unmarshalledObjects);
            Iterator referredObjectIterator = referredObjectList.iterator();
            while ( referredObjectIterator.hasNext() )
            {
               Object referredObject = referredObjectIterator.next();
               referredObjects.put(objectTracker.getIdentifier(referredObject),referredObject);
            }
            // Now fill in this attribute with the ready referred objects
            Iterator objectEntryIterator = idsEntry.objects.entrySet().iterator();
            while ( objectEntryIterator.hasNext() )
            {
               Map.Entry objectEntry = (Map.Entry) objectEntryIterator.next();
               Object referredObject = referredObjects.get(objectEntry.getKey());
               // Now set to all referring objects
               Iterator objectIterator = ((List) objectEntry.getValue()).iterator();
               while ( objectIterator.hasNext() )
               {
                  Object referrerObject = objectIterator.next();
                  ClassInfo referrerClassInfo = classTracker.getClassInfo(referrerObject.getClass(),referrerObject);
                  referrerClassInfo.setAttributeValue(referrerObject,attributeName,referredObject);
               }
            }
         }
         // Debug code
         if ( operationsLogger.isDebugEnabled() )
         {
            TransactionStatistics stats = new TransactionStatistics();
            stats.add(transaction.getStats());
            stats.substract(startStats);
            operationsLogger.debug("operation find: "+stmt.getOriginalStatement()+", "+stats);
            if ( operationsLogger.isTraceEnabled() )
               operationsLogger.trace("previous operation trace:",new Exception("trace"));
         }
         // Return result
         logger.debug("find returning result list");
         return cookedResult;
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
         synchronized ( queryStatistics )
         {
            queryStatistics.setQueryTime(queryStatistics.getQueryTime()+(endTime-startTime));
         }
      }
      // }}}
   }

   private static class IdsEntry
   {
      public Set ids;
      public ClassInfo classInfo;
      public Map objects;
      
      public IdsEntry(Map objects, Set ids, ClassInfo classInfo)
      {
         this.objects=objects;
         this.ids=ids;
         this.classInfo=classInfo;
      }
   }

}



