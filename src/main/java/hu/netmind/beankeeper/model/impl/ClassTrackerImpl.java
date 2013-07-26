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

package hu.netmind.beankeeper.model.impl;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;
import java.lang.reflect.Modifier;
import hu.netmind.beankeeper.parser.QueryStatement;
import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.event.PersistenceEventListener;
import hu.netmind.beankeeper.event.PersistenceEvent;
import hu.netmind.beankeeper.transaction.event.TransactionEvent;
import hu.netmind.beankeeper.transaction.event.TransactionCommittedEvent;
import hu.netmind.beankeeper.transaction.event.TransactionRolledbackEvent;
import hu.netmind.beankeeper.node.event.NodeStateChangeEvent;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.type.TypeHandler;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.InternalTransactionTracker;
import hu.netmind.beankeeper.db.Database;
import hu.netmind.beankeeper.db.Limits;
import hu.netmind.beankeeper.db.SearchResult;
import hu.netmind.beankeeper.object.Identifier; 
import hu.netmind.beankeeper.node.NodeManager;
import hu.netmind.beankeeper.type.TypeHandlerTracker;
import org.apache.log4j.Logger;

/**
 * This class keeps track of different classes and objects. It's main
 * purpose is to implement object reflection based logic, and provide
 * type information.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ClassTrackerImpl implements ClassTracker, PersistenceEventListener
{
   private static Logger logger = Logger.getLogger(ClassTrackerImpl.class);
   private static final long ALLOCATE_COUNT = 1000;
   private static final long MAX_OBJECT_COUNT = Long.MAX_VALUE>>16;

   private Map currentIds = new HashMap(); // Entry -> Current object Id
   
   private Map classInfos; // Entry->Info mapping (locally managed)
   private Map relatedClassEntries; // Entry->Entry list (locally managed)
   private Map rootClassEntries; // Entry->Entry list (locally managed)
   private Map entriesById; // Id->Entry (synced)
   private Map maxIdByEntry; // Entry->Max Id (synced)
   private Map classIdsByEntry; // Entry->Id (synced)

   private int classMaxId = 0;

   private EventDispatcher eventDispatcher = null; // Injected
   private InternalTransactionTracker transactionTracker = null; // Injected
   private TypeHandlerTracker typeHandlerTracker = null; // Injected
   private Database database = null; // Injected
   private NodeManager nodeManager = null; // Injected

   public void init(Map parameters)
   {
      eventDispatcher.registerListener(this);
      // Init local stuff
      classInfos = new HashMap();
      relatedClassEntries = new HashMap();
      rootClassEntries = new HashMap();
      // Load everything
      reload();
   }

   public void release()
   {
      eventDispatcher.unregisterListener(this);
   }

   /**
    * Return whether the given class is a primitive type.
    */
   private boolean isPrimitive(Class clazz)
   {
      if ( clazz == null )
         return false;
      return (clazz.equals(int.class)) || 
            (clazz.equals(short.class)) || 
            (clazz.equals(byte.class)) || 
            (clazz.equals(long.class)) || 
            (clazz.equals(float.class)) || 
            (clazz.equals(double.class)) || 
            (clazz.equals(char.class)) ||
            (clazz.equals(boolean.class)) ||
            (clazz.equals(Integer.class)) || 
            (clazz.equals(Short.class)) || 
            (clazz.equals(Byte.class)) || 
            (clazz.equals(Long.class)) || 
            (clazz.equals(Float.class)) || 
            (clazz.equals(Double.class)) || 
            (clazz.equals(Character.class)) ||
            (clazz.equals(String.class)) || (clazz.equals(Boolean.class)) ||
            (clazz.equals(byte[].class)) ||
            (clazz.equals(Date.class)) || clazz.equals(Character.class);
   }
   
   /**
    * Get the type id of given class.
    */
   public ClassType getType(Class clazz)
   {
      if ( clazz == null )
         return ClassType.TYPE_NONE;
      if ( typeHandlerTracker.isHandled(clazz) )
         return ClassType.TYPE_HANDLED;
      if ( isPrimitive(clazz) )
         return ClassType.TYPE_PRIMITIVE;
      // Array types not allowed
      if ( clazz.getName().startsWith("[") )
         return ClassType.TYPE_RESERVED;
      // The rest is custom objects.
      return ClassType.TYPE_OBJECT;
   }

   /**
    * Get class info for a class entry.
    */
   public ClassInfo getClassInfo(Class clazz, String dynamicName)
   {
      if ( clazz == null )
         return null;
      return getClassInfo(new ClassEntry(clazz,dynamicName));
   }

   /**
    * Get class info for an object and it's class.
    */
   public ClassInfo getClassInfo(Class clazz, Object obj)
   {
      if ( (obj instanceof DynamicObject) && (obj.getClass().equals(clazz)) )
         return getClassInfo(clazz, ((DynamicObject) obj).getPersistenceDynamicName());
      else
         return getClassInfo(clazz, null);
   }

   /**
    * Get class info from a string.
    */
   public ClassInfo getClassInfo(String className, String dynamicName)
   {
      if ( className == null )
         return null;
      try
      {
         Class clazz = Class.forName(className);
         return getClassInfo(clazz, dynamicName);
      } catch ( StoreException e ) {
         throw e;
      } catch ( Exception e ) {
         throw new StoreException("class cannot be located with name '"+className+"'",e);
      }
   }

   /**
    * Get all related classes to the given entry. Related class entries are
    * all given class' super- and sub-classes which are all storable.
    * Calling this method on non-storable entry will result in an undefined
    * result.
    */
   public List getRelatedClassEntries(ClassEntry entry)
   {
      getClassInfo(entry); // Register
      synchronized ( this )
      {
         List result = (List) relatedClassEntries.get(entry);
         if ( result == null )
            return new ArrayList();
         return new ArrayList(result);
      }
   }

   /**
    * Get all subclasses of given entry, including itself.
    */
   public List getSubClasses(ClassEntry entry)
   {
      List relatedEntries = getRelatedClassEntries(entry);
      ArrayList result = new ArrayList();
      result.add(entry.getSourceClass());
      for ( int i=0; i<relatedEntries.size(); i++ )
      {
         ClassEntry subEntry = (ClassEntry) relatedEntries.get(i);
         if ( (entry.getSourceClass().isAssignableFrom(subEntry.getSourceClass())) &&
               (! result.contains(subEntry.getSourceClass())) )
            result.add(subEntry.getSourceClass());
      }
      if ( logger.isDebugEnabled() )
         logger.debug("returning subclasses: "+result+", for: "+entry);
      return result;
   }

   /**
    * Get all storable roots for given entry. A storable root for a
    * storable entry is itself. A non-storable entry (such as java.lang.Object)
    * will have potentially a lot of storable roots: All classes in the
    * class hierarchy which are storable, but have non-storable superclasses.
    * So, storable roots are the first storable entry in a class hierarchy
    * path (roots of the storable sub-forest). When a query is received 
    * for a non-stored class, the query will split into queries for all
    * storable roots.
    */
   public List getStorableRootClassEntries(ClassEntry entry)
   {
      getClassInfo(entry); // Register
      synchronized ( this )
      {
         List result = (List) rootClassEntries.get(entry);
         if ( result == null )
            return new ArrayList();
         return new ArrayList(result);
      }
   }

   /**
    * Get a class entry for a class id.
    */
   public ClassEntry getClassEntry(Integer id)
   {
      synchronized ( this )
      {
         return (ClassEntry) entriesById.get(id);
      }
   }

   /**
    * Get the id for a class entry.
    */
   private Integer getClassEntryId(ClassEntry entry)
   {
      getClassInfo(entry); // Create classinfo if not yet present
      synchronized ( this )
      {
         return (Integer)classIdsByEntry.get(entry);
      }
   }

   /**
    * Get the next class id.
    */
   public Integer registerClassEntry(ClassEntry entry)
   {
      // Make it a server call
      if ( nodeManager.getRole() == NodeManager.NodeRole.CLIENT )
      {
         return (Integer) nodeManager.callServer(ClassTracker.class.getName(),
               "registerClassEntry",new Class[] { ClassEntry.class }, new Object[] { entry });
      }
      synchronized ( classInfos )
      {
         // Try to get class id from an already registered entry, and if
         // id is there return it immediately.
         Integer classId = (Integer) classIdsByEntry.get(entry);
         if ( classId != null )
            return classId; // Entry was already known to server
         // Create new id
         if ( classMaxId > 65534 )
            throw new StoreException("can not handle this many classes, out of ids, max is 65535.");
         classMaxId++;
         classId = classMaxId;
         // Now create the class entry in the classes table
         Transaction transaction = transactionTracker.getTransaction();
         transaction.begin();
         try
         {
            // Add new class to the classes table.
            logger.debug("entry: "+entry+" was not present in database, so adding.");
            Map attrs = new HashMap();
            attrs.put("classname",entry.getSourceClass().getName());
            attrs.put("dynamic",entry.getDynamicName());
            attrs.put("id",new Integer(classId));
            attrs.put("maxId",new Long(0));
            database.insert(transaction,"persistence_classes",attrs);
         } catch ( StoreException e ) {
            transaction.markRollbackOnly();
            throw e;
         } catch ( Throwable e ) {
            transaction.markRollbackOnly();
            throw new StoreException("unexpected error while trying to create class entry for: "+entry,e);
         } finally {
            transaction.commit();
         }
         // After class row is inserted, return the new id
         return classId;
      }
   }

   /**
    * Get the next usable id for the given entry, which
    * is not yet allocated. These ids can be used to number
    * objects of this entry, be sure to "allocate" the ids before
    * you use them.
    */
   public Long getNextId(ClassEntry entry)
   {
      getClassInfo(entry); // Create classinfo if not yet present
      // Get the beginning of the next interval which is not yet allocated
      Long currentId = null;
      synchronized ( this )
      {
         Long maxId = (Long) maxIdByEntry.get(entry);
         // Get the next id which should be given out
         currentId = (Long) currentIds.get(entry);
         if ( currentId == null )
            currentId = maxId;
         if ( logger.isDebugEnabled() )
            logger.debug("generating id for entry: "+entry+", max value: "+maxId+", current: "+currentId);
         // If the id is not yet allocated, then allocate a bunch again. Note:
         // this is a remote call here, so we have to get the start of the new
         // interval too, which is assigned to current id here.
         if ( currentId.longValue() >= maxId.longValue() )
         {
            // Set the new allocated interval
            currentId = allocateObjectIds(entry,ALLOCATE_COUNT);
            maxIdByEntry.put(entry,currentId+ALLOCATE_COUNT);
         }
         // The id is ok, so this should be the id, but also put the
         // next one into the free ids.
         if ( currentId.longValue()+1 >= MAX_OBJECT_COUNT )
            throw new StoreException("maximum number of objects exceeded for a single table");
         currentIds.put(entry,new Long(currentId.longValue()+1));
      }
      // Generate id
      Integer classId = getClassEntryId(entry);
      Identifier result = new Identifier(classId, currentId);
      if ( logger.isDebugEnabled() )
         logger.debug("generated id '"+result+"' for class '"+classId+"' which is: "+entry);
      return result.getId();
   }

   /**
    * Allocate a given number of ids for the entry. When this method
    * returns without exceptions, the given number of ids are successfully
    * allocated. This means that now, you have a number range, which is
    * guaranteed to be exclusively yours for this entry.
    * @return The beginning of the interval allocated.
    */
   public long allocateObjectIds(ClassEntry entry, long count)
   {
      // Make it a server call
      if ( nodeManager.getRole() == NodeManager.NodeRole.CLIENT )
      {
         return (Long) nodeManager.callServer(ClassTracker.class.getName(),
               "allocateObjectIds",new Class[] { ClassEntry.class, long.class },
               new Object[] { entry, count });
      }
      // Local method
      if ( logger.isDebugEnabled() )
         logger.debug("allocating "+count+" object ids for: "+entry);
      getClassInfo(entry); // Initialize class if not yet initialized
      synchronized ( this )
      {
         Integer classId = (Integer) classIdsByEntry.get(entry);
         Long lastId = (Long) maxIdByEntry.get(entry);
         if ( (lastId == null) || (classId == null) )
            throw new StoreException("entry '"+entry+"' had no ids yet, it needs to be created first.");
         Transaction transaction = transactionTracker.getTransaction();
         transaction.begin();
         try
         {
            Map keys = new HashMap();
            keys.put("id",getClassEntryId(entry));
            Map attrs = new HashMap();
            attrs.put("maxId",new Long(lastId.longValue()+count));
            database.save(transaction,"persistence_classes",keys,attrs);
         } catch ( StoreException e ) {
            transaction.markRollbackOnly();
            throw e;
         } catch ( Throwable e ) {
            transaction.markRollbackOnly();
            throw new StoreException("unexpected error.",e);
         } finally {
            transaction.commit();
         }
         if ( logger.isDebugEnabled() )
            logger.debug("allocated "+count+" ids from "+lastId+" for "+entry);
         maxIdByEntry.put(entry,new Long(lastId.longValue()+count));
         return lastId;
      }
   }
   
   /**
    * Get class information object of given class. Also, if class 
    * information does not exist, it will be created.
    */
   public ClassInfo getClassInfo(ClassEntry entry)
   {
      if ( (entry == null) || (entry.getSourceClass()==null) )
         return null;
      // Compute full name
      String fullClassName = entry.getFullName();
      // Do class searching and creating in a synchronized block
      ClassInfo result = null;
      synchronized ( classInfos )
      {
         // Check whether we can return it quickly from cache
         result = (ClassInfo) classInfos.get(fullClassName);
         if ( result != null )
         {
            if ( logger.isDebugEnabled() )
               logger.debug("class info for entry '"+entry+"' present, returning.");
            return result;
         }
         logger.debug("class info for entry '"+entry+"' is not present");
         // If not, then first make sure the superclass exists recursively
         Class clazz = entry.getSourceClass();
         if ( entry.getDynamicName() == null  )
         {
            // A non-dynamic class has it's superclass as 
            // superclass (no surprises here)
            if ( getType(clazz.getSuperclass()) == ClassType.TYPE_PRIMITIVE )
               throw new StoreException("class "+clazz+" subclassed a primitive type ("+clazz.getSuperclass()+"), this is not allowed.");
            if ( clazz.getSuperclass() != null )
            {
               getClassInfo(clazz.getSuperclass(),null);
               // Now register all interface classes also, so
               // we can later select this class with it's interfaces
               // also
               Class interfaces[] = clazz.getInterfaces();
               for ( int i=0;i<interfaces.length; i++ )
                  getClassInfo(interfaces[i],null);
            }
         } else {
            // Dynamic class has the class with 'null' dynamic name
            // as superclass. It has no interface classes directly
            // associated.
            getClassInfo(clazz,null);
         }
         // Now really create the class info, at this point it is guaranteed
         // that the superclasses and superinterfaces are already there.
         result = new ClassInfoImpl(this, entry);
         // Check whether the class is valid. Note, that check valid
         // assumes the class is already registered. So temporaryly
         // register it.
         try
         {
            classInfos.put(fullClassName,result);
            checkValid(result);
         } finally {
            classInfos.remove(fullClassName);
         }
         // Get the class' id from server. Note, if class is not known, 
         // server will create it.
         int classId = registerClassEntry(entry);
         // If table operation was successful, update our own data model too
         classInfos.put(fullClassName,result);
         updateClassEntryRelations(result.getSourceEntry());
         entriesById.put(new Integer(classId),entry);
         classIdsByEntry.put(entry,new Integer(classId));
         if ( ! maxIdByEntry.containsKey(entry) )
            maxIdByEntry.put(entry,new Long(0));
      } // End of synchronized block
      // Return class info
      logger.debug("returning classinfo for entry '"+entry+"': "+result);
      return result;
   }

   /**
    * Determine whether a class is valid. The current algorithm checks:
    * <ul>
    *    <li>Not an inner of anonymous class</li>
    *    <li>Class can't be empty (abstract and no attributes) and have dynamic attributes.</li>
    *    <li>Class can't have an attribute which is contained in a related
    *    class.</li>
    * </ul>
    * If the class is invalid, a StoreException is thrown with the description
    * of the error.
    */
   private void checkValid(ClassInfo info)
   {
      // Non-storables are always valid
      if ( ! info.isStorable() )
         return;
      // Check that this is not an inner or anonymous class, which are
      // not handled, so an error must be generated.
      Class clazz = info.getSourceEntry().getSourceClass();
      if ( clazz.getDeclaringClass() != null )
         throw new StoreException("class '"+clazz+"' is an inner or anonymous class, it won't be handled.");
      // Check that class has a package
      if ( clazz.getPackage() == null )
         throw new StoreException("class '"+clazz+"' had no package given, please define at least one level of package for each class");
      // Check that the object can be constructed, meaning it has a default
      // constructor (if it's non-abstract)
      if ( (!Modifier.isAbstract(clazz.getModifiers())) && 
               (getType(clazz)==ClassType.TYPE_OBJECT) )
      {
         try
         {
            clazz.getConstructor(new Class[0]);
         } catch ( NoSuchMethodException e ) {
            throw new StoreException("class invalid, it had no default constructor: "+clazz);
         }
      }
      // Check emptyness against dynamic attributes
      if ( (info.isEmpty()) && (info.hasDynamicAttributes()) )
         throw new StoreException("class info: "+info+" is empty (abstract and has no member fields attributes), but has dynamic attributes. "+
               "This is not allowed, because it could be mistaken for a non-storable class. If it should be stored, please declare it non-abstract, "+
               "or if it has important attributes, declare them as normal fields.");
      // Search for common attributes
      List<String> attributeNames = info.getAttributeNames(info.getSourceEntry());
      // Validate attribute name format
      for ( String attributeName : attributeNames )
         if ( attributeName.endsWith("_underscore") )
            throw new StoreException("attribute can't end with '_underscore', it's reserved, attribute was: "+attributeName);
      // Search for the root superentry. If there is no storable superentries,
      // then this class obviously has no common attributes with other classes, except
      // it's subclasses.
      ClassEntry entry = info.getSourceEntry();
      ClassEntry superEntry = entry.getSuperEntry();
      ClassInfo superInfo = getClassInfo(superEntry);
      while ( (superEntry!=null) && (superInfo.isStorable()) )
      {
         entry = superEntry;
         superEntry = entry.getSuperEntry();
         if ( superEntry != null )
            superInfo = getClassInfo(superEntry);
      }
      // Now check all related classes and ensure, that classes with common
      // storable ancestors don't have common attributes. Only if we have a 
      // storable super.
      if ( ! entry.equals(info.getSourceEntry()) )
      {
         List relatedEntries = getRelatedClassEntries(entry);
         logger.debug("making sure, that no common attributes are present for entry: "+info.getSourceEntry()+", with: "+relatedEntries);
         for ( int i=0; i<relatedEntries.size(); i++ )
         {
            ClassEntry relatedEntry = (ClassEntry) relatedEntries.get(i);
            ClassInfo check = null;
            synchronized ( classInfos )
            {
               check = (ClassInfo) classInfos.get(relatedEntry.getFullName());
            }
            if ( check == null )
               continue; // Classinfo is not yet in, so don't bother
            if ( ! check.isStorable() )
               continue;
            if ( check.equals(info) )
               continue;
            for ( int o=0; o<attributeNames.size(); o++ )
            {
               String attributeName = (String) attributeNames.get(o);
               if ( check.getAttributeNames(relatedEntry).contains(attributeName) )
                  throw new StoreException("class info: "+info+" had a common attribute '"+
                        attributeName+"' with '"+check+"', and common attributes in related classes are currently not allowed");
            }
         }
      }
   }

   /**
    * Reload the max ids from database. This is called when the connection
    * to the server is lost, and the state of the ids is unknown.
    */
   private synchronized void reload()
   {
      // Clear all internal structures
      entriesById = new HashMap();
      maxIdByEntry = new HashMap();
      classIdsByEntry = new HashMap();
      // Try to load everything from scratch again
      Transaction transaction = transactionTracker.getTransaction();
      transaction.begin();
      try
      {
         // First ensure that table exists
         HashMap tableAttrs = new HashMap();
         tableAttrs.put("classname",String.class);
         tableAttrs.put("dynamic",String.class);
         tableAttrs.put("id",Integer.class);
         tableAttrs.put("maxid",Long.class);
         ArrayList tableKeys = new ArrayList();
         tableKeys.add("id");
         database.ensureTable(transaction,"persistence_classes",
               tableAttrs,tableKeys,true);
         // Load table
         logger.debug("reloading all max ids");
         QueryStatement stmt = new QueryStatement("persistence_classes",null,null);
         SearchResult result = database.search(transaction,stmt,null);
         for ( int i=0; i<result.getResult().size(); i++ )
         {
            Map attributes = (Map) result.getResult().get(i);
            String className = (String) attributes.get("classname");
            String dynamicName = (String) attributes.get("dynamic");
            Integer id = (Integer) attributes.get("id");
            Long maxId = (Long) attributes.get("maxid");
            if ( id.intValue() > classMaxId )
               classMaxId = id.intValue();
            try
            {
               Class clazz = Class.forName(className);
               ClassEntry entry = new ClassEntry(clazz,dynamicName);
               entriesById.put(id,entry);
               classIdsByEntry.put(entry,id);
               maxIdByEntry.put(entry,maxId);
               updateClassEntryRelations(entry);
               logger.debug("class tables loaded known class name: "+className+" ("+dynamicName+"), max id: "+maxId);
            } catch ( StoreException e ) {
               throw e;
            } catch ( Exception e ) {
               logger.warn("could not find class '"+className+"'. "+
                     "This may only mean that a class was removed, but there are traces in the database "+
                     "for it, in this case, you may disregard this message: "+e.getMessage()+" ("+e.getClass().getName()+")");
            }
         }
      } catch ( StoreException e ) {
         transaction.markRollbackOnly();
         throw e;
      } catch ( Throwable e ) {
         transaction.markRollbackOnly();
         throw new StoreException("unexpected error.",e);
      } finally {
         transaction.commit();
      }
   }

   /**
    * Update class relations for a given class. This means insert this class
    * as related to all subclasses, and mark all subclasses related for this
    * class.
    */
   private void updateClassEntryRelations(ClassEntry entry)
   {
      // The first superclass differs when handling dynamic classes,
      // because then, the superclass is the ordinary class of dynamic class
      ClassEntry superEntry = entry.getSuperEntry();
      // Enter into graph walker as initial node
      Stack openEntries = new Stack();
      if ( superEntry != null )
         openEntries.push(superEntry);
      // This class will be potentially a storable root for all it's
      // interfaces. This is not yet sure, but we will check later.
      ArrayList storableSuperEntries = new ArrayList();
      ArrayList nonstorableSuperEntries = new ArrayList();
      for ( int i=0; i<entry.getSourceClass().getInterfaces().length; i++ )
         nonstorableSuperEntries.add(
               new ClassEntry(entry.getSourceClass().getInterfaces()[i],null));
      // Go through all superclasses and insert this class
      // as related to them. Insert to interfaces and reserved classes
      // too, because we need to know if they are related.
      // The algorithm is a simple graph walker. All not yet visited
      // nodes are stored in openEntries, and visited one-by-one. Note, that
      // this works, because there is no cycle in the class hierarchy.
      while ( ! openEntries.empty() )
      {
         // Get top
         superEntry = (ClassEntry) openEntries.pop();
         // Add entry to superclass' related entries
         ArrayList classEntries = (ArrayList) relatedClassEntries.get(superEntry);
         if ( classEntries == null )
         {
            classEntries = new ArrayList();
            relatedClassEntries.put(superEntry,classEntries);
         }
         if ( ! classEntries.contains(entry) )
            classEntries.add(entry);
         // Insert into storable super entries if this entry
         // is storable, to non-storable super if it's not.
         if ( isStorable(superEntry) )
         {
            // Entry is storable, so we will enter this as a related
            // class.
            storableSuperEntries.add(superEntry);
         } else {
            // Entry is not storable, so potentially it's first
            // storable root is this entry. Of course, if this entry
            // has a storable root, then this is not the real root,
            // but this will be detected later.
            nonstorableSuperEntries.add(superEntry);
         }
         // Add superclass to open nodes
         if ( superEntry.getSourceClass().getSuperclass() != null )
            openEntries.push(new ClassEntry(superEntry.getSourceClass().getSuperclass(),null));
         // Also add all interfaces as not visited
         for ( int i=0; i<superEntry.getSourceClass().getInterfaces().length; i++ )
            openEntries.add(new ClassEntry(superEntry.getSourceClass().getInterfaces()[i],null));
      }
      if ( logger.isDebugEnabled() )
         logger.debug("class tracker determined superclasses for "+entry+", non-storable: "+nonstorableSuperEntries+", storable: "+storableSuperEntries);
      // Now insert all storable superclasses as related to this class.
      // If this class is not storable, this should be empty anyway.
      ArrayList classEntries = (ArrayList) relatedClassEntries.get(entry);
      if ( classEntries == null )
      {
         classEntries = new ArrayList();
         relatedClassEntries.put(entry,classEntries);
      }
      classEntries.removeAll(storableSuperEntries);
      classEntries.addAll(storableSuperEntries);
      // Now add this class as a storable root to all non-storable
      // superclasses and superinterfaces, which do not have a storable
      // root which is a superclass to this entry.
      // If following condition is false, then this is a non-storable entry, 
      // so of course non of the non-storable superclasses will have this as storable root.
      if ( isStorable(entry) )
      {
         // This entry is storable, so it's only storable root is itself
         List rootEntries = new ArrayList();
         rootEntries.add(entry);
         rootClassEntries.put(entry,rootEntries);
         // Now check it's non-storable supers one-by-one to see
         // if this class is really their storable root.
         for ( int i=0; i<nonstorableSuperEntries.size(); i++ )
         {
            ClassEntry nonstorableSuperEntry = (ClassEntry) nonstorableSuperEntries.get(i);
            rootEntries = (List) rootClassEntries.get(nonstorableSuperEntry);
            if ( rootEntries == null )
            {
               // Entry did not have a storable root yet, so this is
               // surely a storable root.
               rootEntries = new ArrayList();
               rootEntries.add(entry);
               rootClassEntries.put(nonstorableSuperEntry,rootEntries);
            } else {
               // Entry has some storable roots, so check them.
               // If a root is a superclass of this class, then this
               // class is not the storable root.
               // If a root is a subclass of this class, that should not
               // happen, because superclasses are always handled first!
               boolean hasSuperRoot = false;
               for ( int o=0; (o<rootEntries.size()) && (!hasSuperRoot); o++ )
               {
                  ClassEntry rootEntry = (ClassEntry) rootEntries.get(o);
                  if ( rootEntry.getSourceClass().equals(entry.getSourceClass()) )
                  {
                     // The class is present as root. If this entry is
                     // a dynamic subclass of this class, then it can't
                     // be the root, because it superclass is already root.
                     hasSuperRoot = true;
                  } else {
                     if ( rootEntry.getSourceClass().isAssignableFrom(
                              entry.getSourceClass()) )
                        hasSuperRoot = true; // Real superclass found
                     if ( entry.getSourceClass().isAssignableFrom(
                              rootEntry.getSourceClass()) )
                     {
                        // Subclass found, so replace it
                        logger.debug("replacing storable root "+rootEntry+", with: "+entry);
                        rootEntries.remove(o--); // Remove this, we found a more suitable root
                     }
                  }
               }
               if ( ! hasSuperRoot )
               {
                  rootEntries.add(entry);
                  if ( logger.isDebugEnabled() )
                     logger.debug("added "+entry+" as storable root for: "+nonstorableSuperEntry+", roots until now: "+rootEntries);
               }
            }
         }
      }
   }

   /**
    * This determines whether an entry is storable.
    * Note: this is also implemented by ClassInfoImpl, but this implementation
    * can be used during initialization.
    */
   private boolean isStorable(ClassEntry entry)
   {
      if ( getType(entry.getSourceClass()) == ClassType.TYPE_PRIMITIVE )
         return true;
      if ( (entry.getSourceClass().isInterface()) 
            || (entry.getSourceClass().getName().startsWith("java")) )
         return false; // Intefaces or java.** classes are non-storable
      if ( (Modifier.isAbstract(entry.getSourceClass().getModifiers())) 
            && (!StrictStaticHandler.hasStaticAttributes(entry)) )
         return false; // Abstract superclasses which have no attributes
      return true;
   }

   /**
    * Get a Class instance for a class name postfix. The given parameter
    * is treated as a postfix for a fully qualified class name. The postfix
    * is considered matching, when it contains whole class of package
    * qualifiers. For example: "book" matches "hu.netmind.beankeeper.Book"
    * class, but does not match "hu.netmind.beankeeper.CookBook". Also
    * "persistence.book" matches "hu.netmind.beankeeper.Book", but 
    * "tence.book" does not match to previous class.<br>
    * If no classes are found null is returned. If more than one matching
    * class is present, then one of them is returned (no guarantees which
    * one is picked).
    * @param postfix The class name postfix.
    * @return The class info for which the postfix applies, or null.
    */
   public ClassEntry getMatchingClassEntry(String postfix)
   {
      // Get a list of all known classes from database
      HashSet entries;
      synchronized ( this )
      {
         entries = new HashSet(entriesById.values());
      }
      // Search for given prefix
      postfix = postfix.toLowerCase();
      Iterator iterator = entries.iterator();
      while ( iterator.hasNext() )
      {
         // Check class
         ClassEntry entry = (ClassEntry) iterator.next();
         String className = entry.getFullName().toLowerCase();
         if ( (className.endsWith(postfix)) && 
              ( (className.length()==postfix.length()) ||
                (className.charAt(className.length()-postfix.length()-1)=='.') )
            )
            return entry;
      }
      // None found
      return null;
   }

   /**
    * Activate or discard table names added in the transaction.
    */
   public void handle(PersistenceEvent event)
   {
      if ( (event instanceof NodeStateChangeEvent) &&
        (((NodeStateChangeEvent)event).getNewState()==NodeManager.NodeState.CONNECTED) )
      {
         // On each connect reload the database tables
         reload();
      }
   }

}


