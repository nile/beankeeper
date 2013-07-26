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

package hu.netmind.beankeeper;

import hu.netmind.beankeeper.service.impl.StoreContextImpl;
import hu.netmind.beankeeper.service.StoreContext;
import java.util.*;
import org.apache.log4j.Logger;
import javax.sql.DataSource;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.lock.LockTracker;
import hu.netmind.beankeeper.config.ConfigurationTracker;
import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.object.PersistenceMetaData;
import hu.netmind.beankeeper.object.ObjectTracker;
import hu.netmind.beankeeper.store.StoreService;
import hu.netmind.beankeeper.query.QueryService;
import hu.netmind.beankeeper.type.TypeHandlerTracker;

/**
 * This store class is the entry point to the persistence library. To
 * store, remove or select given objects, just use the appropriate
 * methods.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class Store
{
   private static Logger logger = Logger.getLogger(Store.class);

   private StoreContext context = null;
   private StoreService store = null;
   private LockTracker lockTracker = null;
   private ConfigurationTracker config = null;
   private EventDispatcher eventDispatcher = null;
   private TransactionTracker transactionTracker = null;
   private ObjectTracker objectTracker = null;
   private QueryService queryService = null;
   private TypeHandlerTracker typeHandlerTracker = null;

   /**
    * Instantiate a store. Note that you should only make one
    * store instance for each datasource you might want to use, all
    * methods are thread-safe, so you can use the single instance 
    * in more threads.
    * @param driverClass The driver class to register.
    * @param url The driver's jdbc url (including username and password).
    */
   public Store(String driverClass, String url)
   {
      Map parameters = new HashMap();
      parameters.put(StoreContext.PARAM_DRIVERCLASS,driverClass);
      parameters.put(StoreContext.PARAM_DRIVERURL,url);
      init(parameters);
   }

   /**
    * Instantiate a store. Note that you should only make one
    * store instance for each datasource you might want to use, all
    * methods are thread-safe, so you can use the single instance 
    * in more threads.
    */
   public Store(DataSource dataSource)
   {
      Map parameters = new HashMap();
      parameters.put(StoreContext.PARAM_DATASOURCE,dataSource);
      init(parameters);
   }

   /**
    * Construct this store with the given parameters.
    */
   private void init(Map parameters)
   {
      // Create context
      context = new StoreContextImpl();
      context.init(parameters);
      // Get services
      context.injectServices(this);
   }

   /**
    * Close the store, and release all resources. This method is automatically
    * invoked as a JVM shutdown hook, so it does not have to be called at all.
    */
   public void close()
   {
      context.release();
   }
   
   /**
    * Get the transaction tracker associated with this store. The transaction
    * tracker can be used to create transactions, and listen for transactional
    * events.
    * @return The transaction tracker.
    */
   public TransactionTracker getTransactionTracker()
   {
      return transactionTracker;
   }

   /**
    * Get the configuration tracker for this Store.
    */
   public ConfigurationTracker getConfigurationTracker()
   {
      return config;
   }

   /**
    * Get the lock tracker. This tracker can be used to lock and unlock
    * objects for exclusive operations.
    */
   public LockTracker getLockTracker()
   {
      return lockTracker;
   }

   /**
    * Get the type handler which is responisble for saving fields
    * other than primitive types.
    */
   public TypeHandlerTracker getTypeHandlerTracker()
   {
      return typeHandlerTracker;
   }

   /**
    * Get the event dispatcher in which the caller may register
    * listeners.
    */
   public EventDispatcher getEventDispatcher()
   {
      return eventDispatcher;
   }

   /**
    * Get the persistence id for an object. This method always returns
    * a valid id, even if the object is not saved, or otherwise has
    * no persistence id. If the object is later saved, this persistence id
    * is always preserved.<br>
    * Same as: <code>getPersistenceMetaData(obj).getPersistenceId()</code>.
    * @return A persistence id, and never null.
    */
   public Long getPersistenceId(Object obj)
   {
      return getPersistenceMetaData(obj).getPersistenceId();
   }

   /**
    * Get the persistence meta-data object for a given object. Metadata
    * is always available, even for non-saved objects.
    */
   public PersistenceMetaData getPersistenceMetaData(Object obj)
   {
      return objectTracker.getMetaData(obj);
   }


   /**
    * Save the given object to the store. The given object's all private
    * non-transient fields will be saved. If the object  was not selected
    * from the store, and not yet saved, it will be created in the store,
    * and a unique id will be assigned, so all subsequent calls to save
    * the given object will only modify the already existing instance in
    * store. A few tips:<br>
    * <ul>
    *    <li>Use simple beans. Although this library does not scan
    *    methods to determine the attributes to save, it is a good
    *    idea to simplify work with them.</li>
    *    <li>If you do not use simple beans, watch out that your
    *    object does not reference unnecessary objects, because
    *    if it does, all will be saved/inserted and tracked.</li>
    *    <li>You CAN use objects which reference other beans though.
    *    But beware, that all objects which are directly referenced
    *    will be loaded when the parent object loads.</li>
    *    <li>You CAN use Map, and List types in your
    *    beans. Check the documentation.</li>
    * </ul>
    * <i>Note:</i>A save operation is not recursive. It does not traverse
    * the object hierarchy, only saves the object given, and does not save
    * objects referenced, except when referenced object does not exist
    * yet. If a referenced object does not yet exist, it will be inserted
    * into database (and recursively all referenced objects of that object).
    * <i>Implementation note:</i>This class is the intelligent part of
    * the framework, an intentionally so. This is the class that coordinates
    * all other classes, trackers and functions together.
    * @param obj The object to save.
    * @throws StoreException If save is not successfull.
    */
   public void save(Object obj)
   {
      store.save(obj);
   }

   /**
    * Remove the object given. If the object is not stored yet, no
    * operation will take place.
    * @param obj The object to remove.
    * @throws StoreException If remove is not successfull.
    */
   public void remove(Object obj)
   {
      store.remove(obj);
   }

   /**
    * Query an object from the datastore. The List returned is 
    * a lazy list, the implementation tries to limit the communication
    * with the database layer, as much as possible and pratical. Only
    * parts of the list will be loaded when an item is referenced, not
    * the whole list. Some features of the query language:<br>
    * <pre>find book where book.name='Snow Crash'</pre>
    * The statement always starts with the keyword 'find', and all
    * keywords and parts of the statement not between apostrophs are
    * case in-sensitive.<br>
    * The second word of the statement determines
    * the class you are trying to find. You can abbreviate the classname
    * (strip the package) if it is unique, but you can use the full
    * name (com.acme.book) if you wish, but then you MUST provide an
    * alias name (see below)<br>
    * The following parts are all optional. First, there can be
    * a select statement, to specify which objects to select which are
    * instances of given class. If you want to have a where part, the
    * third word should be 'where'. After it there should be an
    * expression almost as in SQL. Parts of the expression can be:<br>
    * <ul>
    *    <li><strong>Member attributes of classes.</strong> The class given previously
    *    (the target of statement) is always available as declared. If
    *    you wish to use other classes, they can be referenced by
    *    class (abbreviated or fully declared). For example
    *    <pre>find book where book.author=author and author.birthdate=1959</pre>
    *    Of course Book, and Author classes must exits in the store with
    *    given attributes.
    *    Note also, that the above statement can be simply written:
    *    <pre>find book where book.author.birthdate=1959</pre>
    *    You can name the classes you are referencing for later
    *    use (handy if you must "join" the class with itself):
    *    <pre>find book where book.author=otherbook(book).author and otherbook.name='Snow Crash'</pre>
    *    Here, the "otherbook" does not refer to a class, it is only another
    *    name for the class book, and means, that is should be not the same
    *    instance as the one referenced with "book".
    *    </li>
    *    <li><strong>"Now" constant</strong>: For easy use the current date/time
    *    is represented with the special word 'now'. So you can write:
    *    <pre>find movie where movie.startdate &gt; now</pre>
    *    This also means, that attributes named 'now' must be escaped (prefixed
    *    with it's table name).
    *    <li><strong>Constants</strong>: Numbers and strings, see examples above. 
    *    Dates and objects can be given by using the question mark (?), and adding the object
    *    as parameters (same as in jdbc).</li>
    *    <li><strong>The operators</strong>: &lt;, &gt;, =, !=, like, is null, not null, is not null.
    *    Note however, that not all database backends are required to support
    *    all of these. If they are not supported, an exception will be thrown.</li>
    *    <li><strong>Logical operators</strong>: or, and, not. See examples above.</li>
    *    <li><strong>Grouping with parenthesis.</strong> As usual in expressions,
    *    you can use grouping:
    *    <pre>find book where ((book.author.firstname='Neal') and (book.author.lastname='Stephenson'))</pre>
    *    </li>
    *    <li><strong>Special container operator</strong>: contains. 
    *    These are used in conjunction with container types such as Map and List:
    *    <pre>find book where book.genres contains genre and genre.name='postcyberpunk'</pre>
    *    <pre>find author where author.books contains book and book.name='Snow Crash'</pre>
    *    A container operator can not be negated, an exception will be thrown,
    *    if the expression would try to do that.
    *    </li>
    *    <li><strong>Special map operator</strong>: [, ]. These are used when
    *    referencing a Map. Note also, that [] can only contain strings.
    *    <pre>find book where book.metadata['author']=author and author.name='Neal Stephenson</pre>
    *    However, if after a map operator an attribute is referenced, 
    *    there is a mandatory class specifier:
    *    <pre>find book where book.metadata['author'](author).name='Neal Stephenson</pre>
    *    </li>
    * </ul><br>
    * You can also sort the result list with the 'order by' command:
    * <pre>find book order by book.name asc</pre>
    * The order by command takes attributes as aguments. 
    * You can give more than one attribute separated by commas.
    * Also you can append 'asc' (ascending) or 'desc' (descending) to
    * mark the direction of sort.
    * <pre>find book order by book.author.name asc, book.name desc</pre>
    * Note, that method will silently return an empty list, if the
    * specified table or one specified in where clause does not exist.<br>
    * For more detailed information, check the documentation.
    * @param statement The query statement to select.
    */
   public List find(String statement)
   {
      return queryService.find(statement,null);
   }

   /**
    * Same as <code>find(statement)</code>. When a statement contains
    * the question mark (?), the object which should be in the place of
    * the mark should be given as parameters (parameters are usually of
    * Date, or custom classes).
    * @param statement The query statement to execute.
    * @param parameters The parameters.
    */
   public List find(String statement, Object[] parameters)
   {
      return queryService.find(statement,parameters);
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
      return queryService.findSingle(statement,parameters);
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
      return queryService.findSingle(statement,null);
   }

}



