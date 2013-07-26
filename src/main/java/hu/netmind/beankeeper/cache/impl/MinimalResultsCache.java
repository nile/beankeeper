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

package hu.netmind.beankeeper.cache.impl;

import hu.netmind.beankeeper.config.ConfigurationTracker;
import hu.netmind.beankeeper.config.ExtendedConfigurationListener;
import hu.netmind.beankeeper.management.ManagementTracker;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.event.PersistenceEventListener;
import hu.netmind.beankeeper.event.PersistenceEvent;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.cache.ResultsCache;
import hu.netmind.beankeeper.db.Limits;
import hu.netmind.beankeeper.db.SearchResult;
import hu.netmind.beankeeper.object.Identifier;
import hu.netmind.beankeeper.object.PersistenceMetaData;
import hu.netmind.beankeeper.serial.Serial;
import hu.netmind.beankeeper.schema.SchemaManager;
import hu.netmind.beankeeper.store.event.ObjectsFinalizationEvent;
import hu.netmind.beankeeper.node.event.NodeStateChangeEvent;
import org.apache.log4j.Logger;
import java.util.*;
import org.apache.commons.configuration.event.ConfigurationEvent;

/**
 * This is an implementation of an intelligent, configurationless
 * read-only cache with change detection.<br>
 * The main design point is that it does not require any configuration
 * from the user. It's task is to cache result sets up to a previously
 * given deadline, and when that is reached, clear from cache. When the
 * same result is referenced, the deadline may be moved further into the future.
 * Memory management is dynamic. When a resultset arives into the cache,
 * it is <strong>always</strong> cached, but if the cache detects, that
 * there is "not enough memory" (see below) left, it may clear some entries before their
 * their deadline is reached.<br>
 * So basically one does not have to configure the size of the cache because
 * it assumes that if a resultset was not recalled in a given timeframe, 
 * the overhead of selecting from database is acceptable (rather than
 * always using a predetemined size for the cache, hoping to achieve more
 * cache hits). Also, memory adapts to usage: When the load is low, 
 * it is more likely, that only a few resultsets are in the cache, because
 * they expire, and are not likely to be hit anyway. But if the load rises,
 * more and more results get into the cache, the likelyhood of a hit also
 * rises, together with the memory allocation.<br>
 * The cache determines whether there is enough memory by checking the 
 * raw bytes free, and also computes the ratio of allocated vs. free memory. 
 * If this ratio is below a given threshold, then there is enough memory. The
 * theory is, that the Java VM will allocate more heap when this ratio
 * is sufficiently small (usually around 60-70%). This leaves two cases:<br>
 * <ul>
 *    <li>If the cache's ratio is less than the VM's, than the cache will
 *    not force the VM to allocate more space, which in turn means, that
 *    the cache will not grow, although the VM could allocate more memory.</li>
 *    <li>If this ratio is more than the VM's, than the cache will potentially
 *    force the VM to allocate new memory, potentially eating the memory
 *    away from more important tasks.</li>
 * </ul>
 * The cache uses the first non-agressive algorithm. The cache itself will not
 * cause the VM to allocate more heap, but if the application uses more memory
 * the cache will use proportionally more memory for it's own cause. Note:
 * The VM tries to maintain free/used ratio between appr. 30-70%.<br>
 * This cache specializes to store only current searches' result (rather than
 * current <strong>and</strong> historical results). This is because in the
 * case of current results, the cache can effectively compute the interval
 * the result is valid. When determining whether a statement's result is in
 * the cache, the cache searches all entries (which are all current), and if
 * the statements serial is above or equals to the result's start serial
 * (the serial of the first query which caused the entry to be created),
 * then the result is valid for that query. If a query is received for which
 * the result may depend on changes inside the transaction, which are not
 * yet visible to the other transactions, then this query is not handled. This
 * is mainly because handling transaction-dependent result sets would be
 * a large overhead for the cache, with little benefit if at all.<br>
 * In effect, cache hits will occur mostly, when the same non-historical 
 * query, for a common table (not frequently changed) is run multiple times 
 * in short period of time.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class MinimalResultsCache 
   implements ResultsCache, ExtendedConfigurationListener, PersistenceEventListener
{
   private static Logger logger = Logger.getLogger(MinimalResultsCache.class);
   
   private static int MIN_FREE_BYTES = 512*1024; // Min free memory in bytes
   private static int MIN_FREE_RATE = 60; // Min free memory in percentage to total allocated
   private static int FREE_RATE = 2; // How many entries to free for a single entry if needed
   private static long EXPIRATION_INTERVAL = 1*60*1000; // Expiration in millis
 
   private SortedSet entriesByExpiration; // The cache entries sorted by expiration
   private Map entriesByRepresentation; // Entries by statement representation
   private Map entriesByTables; // Entries by table names
   private Object cacheMutex = new Object(); // Mutex for cache
   private Long startSerial; // The serial on which the cache started
   private Map serialsByTables; // Last modification serials of tables

   private Cache cache = null;
   private ConfigurationTracker configurationTracker = null; // Injected
   private EventDispatcher eventDispatcher = null; // Injected
   private ManagementTracker managementTracker = null; // Injected
   private ClassTracker classTracker = null; // Injected
   private SchemaManager schemaManager = null; // Injected
   
   public void init(Map parameters)
   {
      this.startSerial=null;
      this.cache=new Cache(this);
      clear();
      // Configure
      configurationReload();
      configurationTracker.addListener(this);
      // Initialize
      synchronized ( cacheMutex )
      {
         clear();
         // TODO: this should be refreshed when node manager reconnects
         startSerial = Serial.getSerial(new Date()).getValue();
      }
      // Listen for updates
      eventDispatcher.registerListener(this);
      // Register mbean
      managementTracker.registerBean("Cache",cache);
   }

   public void release()
   {
      clear();
      managementTracker.deregisterBean("Cache");
      eventDispatcher.unregisterListener(this);
      configurationTracker.removeListener(this);
   }

   Cache getCache()
   {
      return cache;
   }

   private String getRepresentation(QueryStatement stmt, Limits limits)
   {
      if ( limits != null )
         return stmt.getStaticRepresentation()+limits.toString();
      else
         return stmt.getStaticRepresentation();
   }
   
   /**
    * Get an entry from the cache.
    * @param stmt The statement to look for.
    * @param limits The limits of the query.
    * @return A SearchResult object if the query was cached, null otherwise.
    */
   public SearchResult getEntry(QueryStatement stmt, Limits limits)
   {
      // Check whether entry was modified in the same transaction. Only
      // those results are cached, which are global.
      if ( stmt.getTimeControl().isApplyTransaction() )
         return null;
      // Get entry
      String rep = getRepresentation(stmt,limits);
      if ( logger.isDebugEnabled() )
         logger.debug("searching in cache for: "+rep+", entries: "+entriesByExpiration.size());
      if ( (rep==null) || ("".equals(rep)) )
         return null;
      CacheEntry entry = null;
      synchronized ( cacheMutex )
      {
         entry = (CacheEntry) entriesByRepresentation.get(rep);
      }
      if ( entry == null )
      {
         // Cache miss
         synchronized ( cache )
         {
            cache.setMissCount(cache.getMissCount()+1);
         }
         return null;
      }
      // Check whether query is after result became active
      if ( entry.startSerial > stmt.getTimeControl().getSerial().longValue() )
         return null;
      // All OK, result is valid set statistics
      synchronized ( cacheMutex )
      {
         entriesByExpiration.remove(entry); // Remove, because it will be re-ordered
         entry.accessCount++;
         entry.lastAccess = System.currentTimeMillis();
         entry.expiration += EXPIRATION_INTERVAL;
         entriesByExpiration.add(entry);
      }
      // Return with cache hit
      logger.debug("cache HIT.");
      synchronized ( cache )
      {
         cache.setHitCount(cache.getHitCount()+1);
      }
      return entry.result;
   }

   /**
    * Remove an entry from cache.
    */
   private void removeEntry(CacheEntry entry)
   {
      synchronized ( cacheMutex )
      {
         entriesByExpiration.remove(entry);
         entriesByRepresentation.remove(entry.representation);
         Iterator tableIterator = entry.tables.iterator();
         while ( tableIterator.hasNext() )
         {
            String tableName = (String) tableIterator.next();
            Set tableEntries = (Set) entriesByTables.get(tableName); // This shouldn't be null
            tableEntries.remove(entry);
            if ( tableEntries.size() == 0 )
               entriesByTables.remove(tableName);
         }
         // Remove from management bean
         synchronized ( cache )
         {
            cache.setResultCount(cache.getResultCount()-1);
            cache.setObjectCount(cache.getObjectCount()-entry.result.getResult().size());
         }
      }
   }

   /**
    * Add an entry to the cache.
    * @param stmt The statement source of result.
    * @param limits The limits of result.
    * @param result The SearchResult object.
    */
   public void addEntry(QueryStatement stmt, Limits limits, SearchResult result)
   {
      // Check whether entry was modified in the same transaction. Only
      // those results are cached, which are global.
      if ( stmt.getTimeControl().isApplyTransaction() )
         return;
      // Rep
      String rep = getRepresentation(stmt,limits);
      if ( logger.isDebugEnabled() )
         logger.debug("adding to cache: "+rep+", entries: "+entriesByExpiration.size());
      if ( (rep==null) || ("".equals(rep)) )
         return;
      // First, determine how many entries to free. By default, all expired
      // entries are freed, but if there is not enough memory, entries
      // can be forced to be removed.
      int forceFreeResultsCount = 0; // By default none are forced
      long freeMem = Runtime.getRuntime().freeMemory();
      long totalMem = Runtime.getRuntime().totalMemory();
      if ( (freeMem<MIN_FREE_BYTES) || (100.0*freeMem/totalMem>MIN_FREE_RATE) )
      {
         if ( logger.isDebugEnabled() )
            logger.debug("not enough memory to cache, free: "+freeMem+", total: "+totalMem);
         // Not enough memory, set force free count
         forceFreeResultsCount = result.getResult().size()*FREE_RATE+1;
      }
      // Free entries
      long currentTime = System.currentTimeMillis();
      long lastExpiration = currentTime;
      while ( ((forceFreeResultsCount>0) || (lastExpiration<currentTime)) && 
         (entriesByExpiration.size()>0) )
      {
         // Get top entry
         CacheEntry entry = null;
         synchronized ( cacheMutex )
         {
            entry = (CacheEntry) entriesByExpiration.first();
         }
         // Set indicators
         lastExpiration = entry.expiration;
         forceFreeResultsCount -= entry.result.getResult().size();
         // Free it
         removeEntry(entry);
      }
      if ( logger.isDebugEnabled() )
         logger.debug("cache entries after free: "+entriesByExpiration.size());
      // Create new entry
      CacheEntry entry = new CacheEntry();
      entry.representation=rep;
      entry.result=result;
      entry.accessCount=0;
      entry.firstAccess=currentTime;
      entry.lastAccess=currentTime;
      entry.expiration=currentTime+EXPIRATION_INTERVAL;
      entry.tables=stmt.computeTables();
      entry.startSerial=stmt.getTimeControl().getSerial().longValue();
      // Add new entry to cache
      synchronized ( cacheMutex )
      {
         // Determine whether entry is current (all table
         // modifications are previous to entry)
         Iterator tableIterator = entry.tables.iterator();
         while ( tableIterator.hasNext() )
         {
            String tableName = (String) tableIterator.next();
            Long lastModificationSerial = (Long) serialsByTables.get(tableName);
            if ( lastModificationSerial == null )
               lastModificationSerial = startSerial;
            if ( lastModificationSerial.longValue() > entry.startSerial )
               return; // Table is newer than query, so query is historical
         }
         // Add to maps
         entriesByExpiration.add(entry);
         entriesByRepresentation.put(entry.representation,entry);
         // Add to table indexed map
         tableIterator = entry.tables.iterator();
         while ( tableIterator.hasNext() )
         {
            String tableName = (String) tableIterator.next();
            Set tableEntries = (Set) entriesByTables.get(tableName);
            if ( tableEntries == null )
            {
               tableEntries = new HashSet();
               entriesByTables.put(tableName,tableEntries);
            }
            tableEntries.add(entry);
         }
         // Add to management bean
         synchronized ( cache )
         {
            cache.setResultCount(cache.getResultCount()+1);
            cache.setObjectCount(cache.getObjectCount()+entry.result.getResult().size());
         }
      }
   }

   /**
    * Clear the cache.
    */
   public void clear()
   {
      logger.debug("clearing the cache");
      synchronized ( cacheMutex )
      {
         entriesByExpiration = new TreeSet();
         entriesByRepresentation = new HashMap();
         entriesByTables = new HashMap();
         serialsByTables = new HashMap();
         // Clear management bean
         synchronized ( cache )
         {
            cache.setResultCount(0);
            cache.setObjectCount(0);
         }
      }
   }

   public void handle(PersistenceEvent event)
   {
      if ( event instanceof ObjectsFinalizationEvent )
      {
         // All object finalizations are handled, even
         // from remote nodes, to maintain a fair cache
         ObjectsFinalizationEvent finEvent = (ObjectsFinalizationEvent) event;
         updateEntries(finEvent.getMetas(),finEvent.getSerial());
      }
      if ( event instanceof NodeStateChangeEvent )
      {
         // To be sure, we clear this cache on each state change
         clear();
      }
   }

   /**
    * Update the tables for given ids.
    */
   public void updateEntries(List<PersistenceMetaData> metas, Long modifySerial)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("cache will be updated because following metas were modified: "+metas);
      // Assemble all tables which changed according
      // to ids.
      Set tableNames = new HashSet();
      Iterator metaIterator = metas.iterator();
      while ( metaIterator.hasNext() )
      {
         Identifier id = new Identifier(((PersistenceMetaData)metaIterator.next()).getPersistenceId());
         ClassEntry entry = classTracker.getClassEntry(id.getClassId());
         ClassInfo info = classTracker.getClassInfo(entry);
         // Get all supertables too, because those all got modified potentially
         while ( (entry!=null) && (info.isStorable()) )
         {
            tableNames.add(schemaManager.getTableName(entry));
            entry = entry.getSuperEntry();
            if ( entry != null )
               info = classTracker.getClassInfo(entry);
         }
      }
      // Go through all tables an update cache
      synchronized ( cacheMutex )
      {
         Iterator tableNameIterator = tableNames.iterator();
         while ( tableNameIterator.hasNext() )
            updateEntries((String) tableNameIterator.next(),modifySerial);
      }
   }

   /**
    * Tell the cache, that a table was updated. If an object is updated,
    * the old resultsets could be theoretically kept, with an other time
    * control, but empirically that does not add to cache hits, because more
    * often, only current resultsets are selected.
    * @param tableName The table to update.
    * @param modifySerial The modification serial of table.
    */
   private void updateEntries(String tableName, Long modifySerial)
   {
      // Update table
      synchronized ( cacheMutex )
      {
         serialsByTables.put(tableName,modifySerial);
         // Get entries
         Set entries = null;
         entries = (Set) entriesByTables.get(tableName);
         if ( entries != null )
         {
            // Remove all entries 
            for ( CacheEntry entry : new HashSet<CacheEntry>(entries) )
               removeEntry(entry);
            entriesByTables.remove(tableName);
            if ( logger.isDebugEnabled() )
               logger.debug("updated cache table '"+tableName+"', entry count: "+entriesByExpiration.size());
         }
      }
   }

   /**
    * This is a single cache entry.
    */
   private class CacheEntry implements Comparable
   {
      // Statistics
      public int accessCount;
      public long firstAccess;
      public long lastAccess;
      public long expiration;

      // Data
      public String representation;
      public Set tables;
      public SearchResult result;

      // Valid markers
      public long startSerial; // Maximum of touched table last changed serials

      public int compareTo(Object obj)
      {
         return (int) (expiration - ((CacheEntry) obj).expiration);
      }
   }

   public void configurationChanged(ConfigurationEvent event)
   {
      if ( (event.getPropertyName()!=null) && 
            (event.getPropertyName().startsWith("beankeeper.cache")) )
         configurationReload();
   }

   public void configurationReload()
   {
      MIN_FREE_BYTES = configurationTracker.getConfiguration().
         getInt("beankeeper.cache.min_free_bytes",512*1024);
      MIN_FREE_RATE = configurationTracker.getConfiguration().
         getInt("beankeeper.cache.min_free_rate",60);
      FREE_RATE = configurationTracker.getConfiguration().
         getInt("beankeeper.cache.force_free_rate",2);
      EXPIRATION_INTERVAL = configurationTracker.getConfiguration().
         getInt("beankeeper.cache.expiration",60*1000);
   }
}


