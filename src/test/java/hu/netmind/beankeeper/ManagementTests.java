/**
 * Copyright (C) 2009 NetMind Consulting Bt.
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

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.apache.log4j.Logger;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionStatistics;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import javax.management.MBeanServer;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.Attribute;
import javax.management.Query;
import javax.management.MBeanAttributeInfo;
import java.lang.management.ManagementFactory;

/**
 * Test for the JMX management beans and the management tracker.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class ManagementTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(ManagementTests.class);

   /**
    * Query MBean names.
    */
   private Set<String> queryBeanNames(String objectName)
      throws Exception
   {
      // Get the platform JMX server (no need for remoting)
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      // Set
      Set<ObjectName> objectNames = (Set<ObjectName>) server.queryNames(
            new ObjectName(""),new ObjectName(objectName));
      Set<String> result = new HashSet<String>();
      for ( ObjectName beanName : objectNames )
         result.add(beanName.getCanonicalName());
      return result;
   }

   /**
    * Read all attributes of a specified MBean into a Map.
    * The keys of the map will be the names of the attributes,
    * and the values are primitiv or complex values of that attribute.
    */
   private Map readMBean(String objectName)
      throws Exception
   {
      // Get full name
      Set<String> names = queryBeanNames(objectName);
      Assert.assertEquals(names.size(),1); // Unambigous
      ObjectName name = new ObjectName(names.iterator().next());
      // Get the platform JMX server (no need for remoting)
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      // Find out all attributes of the specified MBean
      MBeanInfo mBeanInfo = server.getMBeanInfo(name);
      MBeanAttributeInfo[] attributeInfos = mBeanInfo.getAttributes();
      // Read all attributes to the map
      Map result = new HashMap();
      for ( int i=0; i<attributeInfos.length; i++ )
         result.put(attributeInfos[i].getName().toLowerCase(),
               server.getAttribute(name,attributeInfos[i].getName()));
      return result;
   }

   /**
    * Call an operation on the given mbean.
    */
   private Object invokeMBean(String objectName, String operation,
         Object[] params, String signature[])
      throws Exception
   {
      Set<String> names = queryBeanNames(objectName);
      Assert.assertEquals(names.size(),1); // Unambigous
      ObjectName name = new ObjectName(names.iterator().next());
      // Get the platform JMX server (no need for remoting)
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      // Invoke
      return server.invoke(name,operation,params,signature);
   }

   public void testBeansAvailable()
      throws Exception
   {
      Set<String> beanNames = queryBeanNames("");
      logger.debug("beans available: "+beanNames);
      // Check that all beans are available
      Assert.assertFalse(queryBeanNames("hu.netmind.beankeeper:type=Cache,*").isEmpty());
      Assert.assertFalse(queryBeanNames("hu.netmind.beankeeper:type=SQLStatistics,*").isEmpty());
      Assert.assertFalse(queryBeanNames("hu.netmind.beankeeper:type=StoreStatistics,*").isEmpty());
      Assert.assertFalse(queryBeanNames("hu.netmind.beankeeper:type=QueryStatistics,*").isEmpty());
   }

   public void testBeansNodeDisconnect()
      throws Exception
   {
      // Shutdown
      tearDownStore();
      try
      {
         // Check that beans are no longer available
         Assert.assertTrue(queryBeanNames("hu.netmind.beankeeper:type=Cache,*").isEmpty());
         Assert.assertTrue(queryBeanNames("hu.netmind.beankeeper:type=SQLStatistics,*").isEmpty());
         Assert.assertTrue(queryBeanNames("hu.netmind.beankeeper:type=OperationsStatistics,*").isEmpty());
      } finally {
         // Setup for next tests
         setUpStore();
      }
   }

   public void testCacheBeanCounts()
      throws Exception
   {
      removeAll(Author.class);
      getStore().save(new Author("Neal","Stephenson"));
      getStore().save(new Author("John","Tolkien"));
      // Get cache reference data now
      Map referenceData = readMBean("hu.netmind.beankeeper:type=Cache,*");
      // Do some selects and see whether numbers are consistent
      long hits = 0;
      for ( int i=0; i<10; i++ )
      {
         // Do the select and iterate to select
         List<Author> authors = getStore().find("find author");
         for ( Author author : authors )
            author.getLastName();
         // Get current data
         Map currentData = readMBean("hu.netmind.beankeeper:type=Cache,*");
         // Calculate
         if ( ((Long) referenceData.get("hitcount")) + hits ==
               ((Long) currentData.get("hitcount")) )
         {
            // It was a miss, check miss count consistency
            Assert.assertEquals((long) (((Long) currentData.get("misscount")) - (i+1 - hits)),
                  (long) ((Long) referenceData.get("misscount")),
                  "miss count inconsistent, i:"+i+", hits: "+hits+", current:"+currentData+", reference:"+referenceData);
            // Check result consistency if result was added
            if ( ((Long) referenceData.get("resultcount")) < ((Long) currentData.get("resultcount")) )
            {
               // Check if result count is only 1 more
               Assert.assertEquals((long) (Long)currentData.get("resultcount"), 
                     (long) (Long)referenceData.get("resultcount")+1);
               // Check if object cound is 2 more
               Assert.assertEquals((long) (Long)currentData.get("objectcount"), 
                     (long) (Long)referenceData.get("objectcount")+2);
            } else {
               // If not more than both should equals
               Assert.assertEquals(currentData.get("resultcount"),referenceData.get("resultcount"));
               Assert.assertEquals(currentData.get("objectcount"),referenceData.get("objectcount"));
            }
         } else if ( ((Long) referenceData.get("hitcount")) + hits +1 ==
               ((Long) currentData.get("hitcount")) ) {
            // It was a hit
            hits++;
         } else {
            // It was not consistent
            Assert.fail("cache data was not consistent, reference hits: "+
                  referenceData.get("hitcount")+", hits in test: "+hits+
                  ", current hits: "+currentData.get("hitcount"));
         }
      }
   }

   @Test(dependsOnMethods = { "testCacheBeanCounts" })
   public void testCacheBeanClear()
      throws Exception
   {
      // Call clear
      invokeMBean("hu.netmind.beankeeper:type=Cache,*","clear",
            new Object[] {}, new String[] {});
      // Check if everything is cleared
      Map currentData = readMBean("hu.netmind.beankeeper:type=Cache,*");
      Assert.assertEquals((Long)currentData.get("resultcount"),new Long(0));
      Assert.assertEquals((Long)currentData.get("objectcount"),new Long(0));
   }

   public void testStoreAndQueryBeanCounts()
      throws Exception
   {
      removeAll(Book.class);
      // Get reference
      Map referenceStoreData = readMBean("hu.netmind.beankeeper:type=StoreStatistics,*");
      Map referenceQueryData = readMBean("hu.netmind.beankeeper:type=QueryStatistics,*");
      Map currentData = null;
      // Do an insert
      Book starshipsBook = new Book("Famous Starships","NCC-1");
      getStore().save(starshipsBook);
      // Check insert
      currentData = readMBean("hu.netmind.beankeeper:type=StoreStatistics,*");
      Assert.assertEquals((int)(Integer)currentData.get("savecount"),(int)(Integer)referenceStoreData.get("savecount")+1);
      // Do select (but first clear the cache, so we physically select)
      invokeMBean("hu.netmind.beankeeper:type=Cache,*","clear",
            new Object[] {}, new String[] {});
      List<Book> books = getStore().find("find book");
      for ( Book book : books )
         book.getTitle();
      currentData = readMBean("hu.netmind.beankeeper:type=QueryStatistics,*");
      Assert.assertEquals((int)(Integer)currentData.get("querycount"),(int)(Integer)referenceQueryData.get("querycount")+1);
      // Do remove
      getStore().remove(starshipsBook);
      currentData = readMBean("hu.netmind.beankeeper:type=StoreStatistics,*");
      Assert.assertEquals((int)(Integer)currentData.get("removecount"),(int)(Integer)referenceStoreData.get("removecount")+1);
   }

   public void testSQLStatisticsBeanCounts()
      throws Exception
   {
      Transaction tx = getStore().getTransactionTracker().getTransaction(
            TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         logger.debug("starting sql statistics...");
         // Get the reference
         Map referenceData = readMBean("hu.netmind.beankeeper:type=SQLStatistics,*");
         // Do some stuff
         Book starshipsBook = new Book("Famous Starships","NCC-1");
         getStore().save(starshipsBook);
         List<Book> books = getStore().find("find book");
         for ( Book book : books )
            book.getTitle();
         getStore().remove(starshipsBook);
         // Check
         TransactionStatistics stats = tx.getStats();
         Map currentData = readMBean("hu.netmind.beankeeper:type=SQLStatistics,*");
         logger.debug("checking sql statistics...");
         // Values might not exactly match because of class tracker doing stuff in NEW
         // transactions.
         Assert.assertTrue( (int)(Integer)currentData.get("updatecount") >=
               ((int)(Integer)referenceData.get("updatecount")) + (int)stats.getUpdateCount());
         Assert.assertTrue( (int)(Integer)currentData.get("insertcount") >=
               ((int)(Integer)referenceData.get("insertcount")) + (int)stats.getInsertCount());
         Assert.assertTrue( (int)(Integer)currentData.get("selectcount") >=
               ((int)(Integer)referenceData.get("selectcount")) + (int)stats.getSelectCount());
         Assert.assertTrue( (int)(Integer)currentData.get("deletecount") >=
               ((int)(Integer)referenceData.get("deletecount")) + (int)stats.getDeleteCount());
         Assert.assertTrue( (int)(Integer)currentData.get("schemacount") >=
               ((int)(Integer)referenceData.get("schemacount")) + (int)stats.getSchemaCount());
      } finally {
         tx.commit();
      }
   }

   public void testCacheMBeanEntryCount()
      throws Exception
   {
      // Clean
      removeAll(Book.class);
      removeAll(Author.class);
      invokeMBean("hu.netmind.beankeeper:type=Cache,*","clear",
            new Object[] {}, new String[] {});
      // Setup cache to cache everything, so we can test
      getStore().getConfigurationTracker().getConfiguration().
         setProperty("beankeeper.cache.min_free_bytes",0);
      getStore().getConfigurationTracker().getConfiguration().
         setProperty("beankeeper.cache.min_free_rate",100);
      try
      {
         // Create model
         Author neal = new Author("Neal","Stephenson");
         Book book1 = new Book("Snow Crash","123");
         Book book2 = new Book("The Diamond Age","234");
         book1.setMainAuthor(neal);
         book2.setMainAuthor(neal);
         getStore().save(book1);
         getStore().save(book2);
         // Create a query which would refernce two tables
         List<Book> result = getStore().find("find book where book.mainauthor.firstName='Neal'");
         for ( Book book : result )
            logger.debug("recevied book: "+book);
         // Do checks
         Assert.assertEquals(result.size(),2);
         Map currentData = readMBean("hu.netmind.beankeeper:type=Cache,*");
         Assert.assertEquals( (long)(Long)currentData.get("resultcount"), 2);
         Assert.assertEquals( (long)(Long)currentData.get("objectcount"), 3);
         // Now update both tables after eachother
         getStore().save(new Book("The Great Escape","4"));
         getStore().save(new Author("John","McCabe"));
         // Cache should be empty
         currentData = readMBean("hu.netmind.beankeeper:type=Cache,*");
         Assert.assertEquals( (long)(Long)currentData.get("resultcount"), 0);
         Assert.assertEquals( (long)(Long)currentData.get("objectcount"), 0);
      } finally {
         // Reset cache settings
         getStore().getConfigurationTracker().getConfiguration().
            clearProperty("beankeeper.cache.min_free_bytes");
         getStore().getConfigurationTracker().getConfiguration().
            clearProperty("beankeeper.cache.min_free_rate");
      }
   }
}

