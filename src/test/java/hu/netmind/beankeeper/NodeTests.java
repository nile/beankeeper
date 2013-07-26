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

import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.HashMap;
import org.apache.log4j.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import hu.netmind.beankeeper.node.*;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.lock.LockTracker;
import hu.netmind.beankeeper.lock.SessionInfo;
import hu.netmind.beankeeper.lock.ConcurrentModificationException;
import hu.netmind.beankeeper.node.impl.NodeManagerImpl;

/**
 * Node tests.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class NodeTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(NodeTests.class);

   private void addNode(int index, String ips, int port)
   {
      try
      {
         PreparedStatement pstmt = getConnection().prepareStatement(
               "insert into nodes (heartbeat,nodeindex,ips,command_port) values (1,"+
               index+",'"+ips+"',"+port+")");
         int rows = pstmt.executeUpdate();
         pstmt.close();
         getConnection().commit();
         Assert.assertEquals(rows,1);
      } catch ( Exception e ) {
         // No problem, node id given was used, but that's not important
         logger.debug("expected exception",e);
      }
   }

   @Test
   public void supportsMultipleStores()
   {
      // Read properties file
      ResourceBundle config = ResourceBundle.getBundle("test");
      // Allocate class (now only postgres supported)
      String driverclass = config.getString("db.driverclass");
      String url = config.getString("db.url");
      // Only hsqldb,derby do not support multiple instances!
      if ( ! ((url.indexOf("hsqldb") < 0) && (url.indexOf("derby") < 0)) )
         Assert.fail("datasource does not support multiple stores, skipping tests");
   }

   /**
    * Restart the store before each test to get a clean store.
    */
   @BeforeMethod
   protected void cleanStore()
      throws Exception
   {
      // Tear down previous store
      super.tearDownStore();
      // Set up new store
      super.setUpStore();
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testNodesConcept()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      Store store2 = newStore();
      try
      {
         // Create
         Book book = new Book("The Concept of Nodes","1-2");
         logger.debug("saving book...");
         getStore().save(book);
         logger.debug("book saved.");
         // Create in the other node too (or else the class will not
         // be known)
         store2.save(new Book("The Class","1"));
         // Get back from the other node
         logger.debug("executing query...");
         List books = store2.find("find book");
         logger.debug("query returned.");
         // Check stuff
         Assert.assertEquals(books.size(),2);
      } finally {
         store2.close();
      }
   }
  
   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testMultipleNodesConcept()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      Store stores[] = new Store[10];
      for ( int i=0; i<stores.length; i++ )
         stores[i] = newStore();
      try
      {
         // Create
         for ( int i=0; i<stores.length; i++ )
         {
            Book book = new Book("The Concept of Nodes","1-2");
            logger.debug("saving book #"+i+"...");
            stores[i].save(book);
            logger.debug("book saved.");
         }
         // Get back from the other node
         logger.debug("executing query...");
         List books = getStore().find("find book");
         logger.debug("query returned.");
         // Check stuff
         Assert.assertEquals(books.size(),stores.length);
      } finally {
         for ( int i=0; i<stores.length; i++ )
            stores[i].close();
      }
   }
   
   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testNodeCleanup()
      throws Exception
   {
      int oldCount = getCount("nodes");
      // Allocate new store, and close it
      Store store2 = newStore();
      Assert.assertEquals(getCount("nodes"),oldCount+1);
      store2.close();
      Assert.assertEquals(getCount("nodes"),oldCount);
   }
   
   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testCacheUpdateEvents()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Get second store to cache the result on books
         Transaction tx = store2.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         List result = store2.find("find book");
         result.size(); // Force load
         tx.commit();
         Assert.assertEquals(tx.getStats().getSelectCount(),1);
         // Second query should not make any phisical statements
         tx = store2.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         result = store2.find("find book");
         result.size(); // Force load
         tx.commit();
         Assert.assertEquals(tx.getStats().getSelectCount(),0);
         // Now the first store will update the books
         getStore().save(new Book("Learn Cache Update In Seven Days","1"));
         // A query after this update should again yield a physical query
         tx = store2.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         result = store2.find("find book");
         result.size(); // Force load
         tx.commit();
         Assert.assertEquals(tx.getStats().getSelectCount(),1);
      } finally { 
         store2.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testCacheWhenReconnect()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Get second store to cache the result on books
         Transaction tx = store2.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         List result = store2.find("find book");
         result.size(); // Force load
         tx.commit();
         Assert.assertEquals(tx.getStats().getSelectCount(),1);
         // Second query should not make any phisical statements
         tx = store2.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         result = store2.find("find book");
         result.size(); // Force load
         tx.commit();
         Assert.assertEquals(tx.getStats().getSelectCount(),0);
         // Now the first store (which is the server) will be shut down,
         // and re-inited. This means, this client should empty the cache
         tearDownStore();
         setUpStore();
         // A query after this update should again yield a physical query
         tx = store2.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         result = store2.find("find book");
         result.size(); // Force load
         tx.commit();
         Assert.assertEquals(tx.getStats().getSelectCount(),1);
      } finally { 
         store2.close();
      }
   }
   
   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testLockFromClientNode()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Insert a book
         Book originalBook = new Book("Necronomicon","999");
         getStore().save(originalBook);
         // Start a transaction to that book in the first store
         Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         Book bookInstance1 = (Book) getStore().findSingle("find book");
         bookInstance1.setTitle("Store 1's Book");
         getStore().save(bookInstance1);
         // Try to modify in the second transaction
         Transaction tx2 = store2.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx2.begin();
         Book bookInstance2 = (Book) store2.findSingle("find book");
         bookInstance2.setTitle("Store 2's Book");
         try
         {
            store2.save(bookInstance2);
            Assert.fail("I could modify the same object in another getStore().");
         } catch ( ConcurrentModificationException e ) {
            // Ok, this should be
            logger.debug("expected exception",e);
         }
         // Close all
         tx2.commit();
         tx.commit();
      } finally { 
         store2.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testLockFromServerNode()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Insert a book, this has to be before
      // the new store is allocated, so it knows the book
      // class.
      Book originalBook = new Book("Necronomicon","999");
      getStore().save(originalBook);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Start a transaction to that book in the first store
         Transaction tx2 = store2.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx2.begin();
         Book bookInstance2 = (Book) store2.findSingle("find book");
         bookInstance2.setTitle("Store 2's Book");
         store2.save(bookInstance2);
         // Try to modify in the second transaction
         Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         Book bookInstance = (Book) getStore().findSingle("find book");
         bookInstance.setTitle("Store 1's Book");
         try
         {
            getStore().save(bookInstance);
            Assert.fail("I could modify the same object in another getStore().");
         } catch ( ConcurrentModificationException e ) {
            // Ok, this should be
            logger.debug("expected exception",e);
         }
         // Close all
         tx2.commit();
         tx.commit();
      } finally { 
         store2.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testLockFromSameRemoteNode()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Insert a book
         Book originalBook = new Book("Necronomicon","999");
         getStore().save(originalBook);
         // Start a transaction to that book in the first store
         Transaction tx = store2.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         Book bookInstance = (Book) store2.findSingle("find book");
         bookInstance.setTitle("Store 2's Book");
         store2.save(bookInstance); // First save
         // Do a second save on the same object
         bookInstance.setTitle("Store 2's Book for Sure");
         store2.save(bookInstance);
         // Close all
         tx.commit();
      } finally { 
         store2.close();
      }
   }
   
   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testModifyObjectFromAnotherNode()
      throws Exception
   {
      // Test object tracker attribute cache. This cache stores
      // object attributes, so the second time it is saved, not
      // all attributes need to be saved.
      removeAll(Book.class);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Insert a book in the first store
         Book originalBook = new Book("Original Title","999");
         getStore().save(originalBook);
         // Select the book in the second store, and re-save
         Book copyBook = (Book) store2.findSingle("find book");
         Assert.assertEquals(copyBook,originalBook);
         copyBook.setTitle("Modified Title");
         store2.save(copyBook);
         Assert.assertEquals(getStore().find("find book").size(),1);
         // Now save in the first getStore(). The book should be
         // totally overridden.
         originalBook.setIsbn("1000");
         getStore().save(originalBook);
         // Select the book
         Book dbBook = (Book) getStore().findSingle("find book");
         Assert.assertEquals(dbBook,originalBook);
      } finally { 
         store2.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testModifyMemberListFromAnotherNode()
      throws Exception
   {
      // Test object tracker attribute cache. This cache stores
      // object attributes, so the second time it is saved, not
      // all attributes need to be saved.
      removeAll(Book.class);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Insert a book in the first store
         Book originalBook = new Book("Original Title","999");
         ArrayList authors = new ArrayList();
         authors.add(new Author("Mr","Original"));
         originalBook.setAuthors(authors);
         getStore().save(originalBook);
         // Select the book in the second store, and re-save
         Book copyBook = (Book) store2.findSingle("find book");
         Assert.assertEquals(copyBook,originalBook);
         copyBook.setAuthors(new ArrayList());
         store2.save(copyBook);
         // Now select
         Book dbBook = (Book) store2.findSingle("find book");
         Assert.assertEquals(dbBook.getAuthors().size(),0);
      } finally { 
         store2.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testAlreadyLockedTransactionInfo()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Insert a book
         Book originalBook = new Book("Necronomicon","999");
         getStore().save(originalBook);
         // Start a transaction to that book in the first store
         Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.put("user","joe");
         tx.put("userid","1");
         tx.begin();
         try
         {
            Book bookInstance1 = (Book) getStore().findSingle("find book");
            bookInstance1.setTitle("Store 1's Book");
            getStore().save(bookInstance1);
            // Try to modify in the second transaction
            Transaction tx2 = store2.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
            tx2.begin();
            Book bookInstance2 = (Book) store2.findSingle("find book");
            bookInstance2.setTitle("Store 2's Book");
            try
            {
               store2.save(bookInstance2);
               Assert.fail("I could modify the same object in another getStore().");
            } catch ( ConcurrentModificationException e ) {
               // Ok, this should be the session info with the data
               SessionInfo locker = e.getSessionInfo();
               Assert.assertEquals(locker.get("user"),"joe");
               Assert.assertEquals(locker.get("userid"),"1");
            } finally {
               tx2.commit();
            }
         } finally {
            tx.commit();
         }
      } finally { 
         store2.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testNormalInitialization()
      throws Exception
   {
      // Tests initialization when no dead nodes are in the nodes table
      Store stores[] = new Store[10];
      for ( int i=0; i<stores.length; i++ )
         stores[i] = newStore();
      for ( int i=0; i<stores.length; i++ )
         stores[i].close();
   }
   
   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testDeadNodeInitialization()
      throws Exception
   {
      // First insert dummy node info
      removeAll(Book.class);
      addNode(2,"1.2.3.4",99);
      // Try to initialize
      tearDownStore();
      setUpStore();
      // Save
      getStore().save(new Book("The Insert to Success","1-1-1-1"));
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testMultipleDeadNodeInitialization()
      throws Exception
   {
      // First insert dummy node info
      removeAll(Book.class);
      for ( int i=2; i<20; i++ )
         addNode(i,"1.2.3.4",99+i);
      // Try to initialize
      tearDownStore();
      setUpStore();
      // Try to insert a book
      getStore().save(new Book("The Insert to Success","1-1-1-1"));
   }
   
   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testMultipleCloses()
      throws Exception
   {
      // Tear down twice
      tearDownStore();
      tearDownStore();
      // Setup 
      setUpStore();
   }
      
   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testDiedOnlyServerReconnect()
      throws Exception
   {
      // Allocate second store
      logger.debug("allocating new store...");
      Store store2 = newStore();
      try
      {
         // Make some operation
         logger.debug("saving first book...");
         store2.save(new Book("Dune","1-2-3-4"));
         // Disconnect server
         logger.debug("disconnecting server...");
         tearDownStore();
         try
         {
            // Next operation should successfully complete,
            // using store2 as server
            logger.debug("second save...");
            store2.save(new Book("Dune II","1-2-3-4-II"));
            logger.debug("second save ok.");
         } finally {
            setUpStore();
         }
      } finally {
         logger.debug("finally closing second store");
         // Close
         store2.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testDiedSomeServerReconnect()
      throws Exception
   {
      // Allocate second store
      Store store2 = newStore();
      // Allocate third store
      Store store3 = newStore();
      try
      {
         // Make some operation
         store3.save(new Book("Dune","1-2-3-4"));
         // Disconnect first server
         logger.debug("closing store");
         tearDownStore();
         try
         {
            // Make again some operation. It should succeed, because
            // store2 should become the server
            logger.debug("saving book from thrid node");
            store3.save(new Book("Dune II","1-2-3-4-II"));
         } finally {
            setUpStore();
         }
      } finally {
         store2.close();
         store3.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testDeadNodeCleanup()
      throws Exception
   {
      // First insert dummy node info
      removeAll(Book.class);
      for ( int i=2; i<20; i++ )
         addNode(i,"1.2.3.4",99+i);
      tearDownStore();
      // Try to initialize
      setUpStore(); // Allocate new store
      // Determine settings
      getStore().save(new Book("The Insert to Success","1-1-1-1"));
      // Check whether dummy nodes were deleted
      Assert.assertEquals(getCount("nodes"),1);
   }
   
   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testClientNodeReconnectTransactions()
      throws Exception
   {
      // Allocate second store
      Store store2 = newStore();
      // Allocate third store
      Store store3 = newStore();
      try
      {
         // Make some operation
         Transaction tx = store3.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         store3.save(new Book("Dune","1-2-3-4"));
         // Disconnect first server
         tearDownStore();
         // Try to finish
         try
         {
            tx.commit();
            Assert.fail("commit was successful, but reconnect should have killed it");
         } catch ( Exception e ) {
            logger.debug("received rollback exception",e);
         } finally {
            // Re-open original store
            setUpStore();
         }
      } finally {
         store2.close();
         store3.close();
      }
   }
   
   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testClientNodeReconnectSelfTransactions()
      throws Exception
   {
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Make some operation
         Transaction tx = store2.getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         store2.save(new Book("Dune","1-2-3-4"));
         // Disconnect server
         tearDownStore();
         // Try to finish
         try
         {
            tx.commit();
            Assert.fail("commit was successful, but reconnect should have killed it");
         } catch ( Exception e ) {
            logger.debug("received rollback exception",e);
         } finally {
            // Re-initialize original store
            setUpStore();
         }
      } finally {
         // Close
         store2.close();
      }
   }
   
   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testClientReconnectWithUnknownNodes()
      throws Exception
   {
      // Insert dummy nodes additionally
      for ( int i=2; i<20; i++ )
         addNode(i,"1.2.3.4",99+i);
      // Try to initialize
      Store store2 = newStore();
      Store store3 = newStore();
      try
      {
         // Make a successful operation (server is store1)
         logger.debug("saving book with first node as server");
         store3.save(new Book("Success","1"));
         // Kill store
         logger.debug("killing first node");
         tearDownStore();
         try
         {
            // Try to insert a book (store3 should reconnect to store2)
            logger.debug("making the reconnect save");
            store3.save(new Book("The Insert to Success","1-1-1-1"));
         } finally {
            // Two nodes should stay in the nodes table, plus one with setup
            setUpStore();
         }
         Assert.assertEquals(getCount("nodes"),3);
      } finally {
         // Close all
         store2.close();
         store3.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testListTwiceAdd()
      throws Exception
   {
      removeAll(Book.class);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Insert author
         Author author = new Author("First","Name");
         getStore().save(author);
         // Insert original book
         Book originalBook = new Book("Original","1");
         getStore().save(originalBook);
         // Insert an item as list
         Book copyBook = (Book) getStore().findSingle("find book");
         Author copyAuthor = (Author) getStore().findSingle("find author");
         ArrayList authors2 = new ArrayList();
         authors2.add(copyAuthor);
         copyBook.setAuthors(authors2);
         store2.save(copyBook);
         // Insert into second also
         ArrayList authors = new ArrayList();
         authors.add(author);
         originalBook.setAuthors(authors);
         getStore().save(originalBook);
         // Check
         Assert.assertEquals(((Book) getStore().findSingle("find book")).getAuthors().size(),1);
         Assert.assertEquals(((Book) store2.findSingle("find book")).getAuthors().size(),1);
      } finally {
         // Close
         store2.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testListTwiceRemove()
      throws Exception
   {
      removeAll(Book.class);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Insert author
         Author author = new Author("First","Name");
         getStore().save(author);
         // Insert original book
         Book originalBook = new Book("Original","1");
         ArrayList authors = new ArrayList();
         authors.add(author);
         originalBook.setAuthors(authors);
         getStore().save(originalBook);
         // Insert an item as list
         Book copyBook = (Book) getStore().findSingle("find book");
         copyBook.getAuthors().clear();
         store2.save(copyBook);
         // Insert into second also
         originalBook.getAuthors().clear();
         getStore().save(originalBook);
         // Check
         Assert.assertEquals(((Book) getStore().findSingle("find book")).getAuthors().size(),0);
         Assert.assertEquals(((Book) store2.findSingle("find book")).getAuthors().size(),0);
      } finally {
         // Close
         store2.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testMapTwiceAdd()
      throws Exception
   {
      removeAll(MapHolder.class);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Insert author
         Author author = new Author("First","Name");
         getStore().save(author);
         // Insert original book
         MapHolder originalHolder = new MapHolder();
         originalHolder.setMeta(new HashMap());
         getStore().save(originalHolder);
         // Insert an item as list
         MapHolder copyHolder = (MapHolder) getStore().findSingle("find mapholder");
         Author copyAuthor = (Author) getStore().findSingle("find author");
         copyHolder.getMeta().put("test",author);
         store2.save(copyHolder);
         // Insert into second also
         originalHolder.getMeta().put("test",author);
         getStore().save(originalHolder);
         // Check
         Assert.assertEquals(((MapHolder) getStore().findSingle("find mapholder")).getMeta().size(),1);
         Assert.assertEquals(((MapHolder) store2.findSingle("find mapholder")).getMeta().size(),1);
      } finally {
         // Close
         store2.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testMapTwiceRemove()
      throws Exception
   {
      removeAll(MapHolder.class);
      // Allocate second store
      Store store2 = newStore();
      try
      {
         // Insert author
         Author author = new Author("First","Name");
         getStore().save(author);
         // Insert original book
         MapHolder originalHolder = new MapHolder();
         originalHolder.setMeta(new HashMap());
         originalHolder.getMeta().put("test",author);
         getStore().save(originalHolder);
         // Insert an item as list
         MapHolder copyHolder = (MapHolder) getStore().findSingle("find mapholder");
         copyHolder.getMeta().clear();
         store2.save(copyHolder);
         // Insert into second also
         originalHolder.getMeta().clear();
         getStore().save(originalHolder);
         // Check
         Assert.assertEquals(((MapHolder) getStore().findSingle("find mapholder")).getMeta().size(),0);
         Assert.assertEquals(((MapHolder) store2.findSingle("find mapholder")).getMeta().size(),0);
      } finally {
         // Close
         store2.close();
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testLocalDeadNode()
      throws Exception
   {
      // First insert dummy node info
      removeAll(Book.class);
      addNode(2,NodeManagerImpl.getHostAddresses(),99);
      // Try to initialize
      tearDownStore();
      setUpStore();
      // Try to insert a book. If this fails, the local
      // dead node was not recognized.
      getStore().save(new Book("The Insert to Success","1-1-1-1"));
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testLockModifyFromOtherNodeUnlock()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      getStore().save(book);
      lockTracker.lock(book);
      try
      {
         // Do some modification from another node
         // Test if we can modify it
         Store store2 = newStore();
         try
         {
            Book dbBook = (Book) store2.findSingle("find book");
            dbBook.setTitle("Return of the Locks");
            store2.save(dbBook);
            Assert.fail("book was modifiable, but it should have been locked.");
         } catch ( Exception e ) {
            // Nothing to do
            logger.debug("expected exception",e);
         } finally {
            store2.close();
         }
      } finally {
         // Unlock
         lockTracker.unlock(book);
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testLockModifyFromOtherNodeWait()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      getStore().save(book);
      lockTracker.lock(book);
      try
      {
         // Do some modification from another node
         // Test if we can modify it
         Store store2 = newStore();
         LockTracker lockTracker2 = store2.getLockTracker();
         try
         {
            Book dbBook = (Book) store2.findSingle("find book");
            lockTracker2.lock(dbBook,100);
            Assert.fail("book was modifiable, but it should have been locked.");
         } catch ( Exception e ) {
            // Nothing to do
            logger.debug("expected exception",e);
         } finally {
            store2.close();
         }
      } finally {
         // Unlock
         lockTracker.unlock(book);
      }
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testLockModifyFromOtherNodeWaitForUnlock()
      throws Exception
   {
      // Get the lockmanager
      final LockTracker lockTracker = getStore().getLockTracker();
      // Lock object
      removeAll(Book.class);
      final Book book = new Book("Locks","1");
      getStore().save(book);
      lockTracker.lock(book);
      Thread lockThread = null;
      final ArrayList exceptions = new ArrayList();
      try
      {
         // Start thread
         lockThread = new Thread(new Runnable()
               {
                  public void run()
                  {
                     Store store2 = null;
                     try
                     {
                        store2 = newStore();
                        LockTracker lockTracker2 = store2.getLockTracker();
                        Book dbBook = (Book) store2.findSingle("find book");
                        lockTracker2.lock(dbBook,1000);
                     } catch ( Exception e ) {
                        exceptions.add(e);
                     } finally {
                        if ( store2 != null )
                           store2.close();
                     }
                  }
               });
         lockThread.start();
         // Wait and unlock
         try
         {
            Thread.sleep(500);
         } catch ( Exception e ) {
            logger.debug("exception while waiting",e);
         }
      } finally {
         lockTracker.unlock(book);
      }
      // Check whether lock thread was successful
      lockThread.join();
      Assert.assertEquals(exceptions.size(),0);
   }

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testLockEnsureModifyOtherNode()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      getStore().save(book);
      // Do some modification in another node
      Store store2 = newStore();
      Book dbBook = (Book) store2.findSingle("find book");
      dbBook.setTitle("Return of the Locks");
      store2.save(dbBook);
      // Try to lock original version
      try
      {
         lockTracker.lockEnsureCurrent(book);
         Assert.fail("book was modified, original instance should not be current");
      } catch ( Exception e ) {
         // Nothing to do
         logger.debug("expected exception",e);
      } finally {
         // Close other instance
         store2.close();
      }
   }

}


