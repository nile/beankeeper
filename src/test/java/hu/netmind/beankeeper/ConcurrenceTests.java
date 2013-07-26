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
import java.util.Date;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;
import org.testng.Assert;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.lock.ConcurrentModificationException;
import hu.netmind.beankeeper.query.impl.LazyListImpl;

/**
 * Test the concurrence capabilities of library.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class ConcurrenceTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(ConcurrenceTests.class);

   public void testSaveBetweenLoadAndSave()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create book
      Book book = new Book("Learn Mutexes in 7 days","1-2-3-5");
      getStore().save(book);
      // Load into list for later save
      List result1 = getStore().find("find book");
      Assert.assertEquals(result1.size(),1);
      Book book1 = (Book) result1.get(0);
      // Save to another form
      book.setTitle("Learn Semaphores in 7 days");
      getStore().save(book);
      // Save old object
      book1.setTitle("Learn Mutexes instead of Semaphores in 7 days.");
      getStore().save(book1);
      // Check whether object is again the old one
      List result2 = getStore().find("find book");
      Assert.assertEquals(result2.size(),1);
      Assert.assertEquals(result2.get(0),book1);
   }

   public void testSaveBetweenSaveAndCommit()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create book
      Book book = new Book("Learn Mutexes in 7 days","1-2-3-5");
      getStore().save(book);
      // Save but try to save in another transaction
      TransactionTracker tt = getStore().getTransactionTracker();
      Transaction tx1 = tt.getTransaction(TransactionTracker.TX_NEW);
      tx1.begin();
      book.setTitle("Learn Semaphores in 7 days");
      getStore().save(book);
      // Try to save again in another transaction
      Transaction tx2 = tt.getTransaction(TransactionTracker.TX_NEW);
      tx2.begin();
      try
      {
         getStore().save(book);
         Assert.fail("could save book while another transaction is currently saving it.");
      } catch ( ConcurrentModificationException e ) {
         logger.debug("expected exception",e);
      } finally {
         tx2.commit();
         tx1.commit();
      }
   }

   public void testTrySaveWhileRemovedAfterSelected()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create book
      Book book = new Book("Learn Mutexes in 7 days","1-2-3-5");
      getStore().save(book);
      // Select another instance
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
      Book book1 = (Book) result.get(0);
      // Now remove it
      getStore().remove(book);
      // Try to save the second instance
      book1.setTitle("Hamlet");
      getStore().save(book1);
      // Select
      result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),book1);
   }

   public void testCrossTransactionList()
      throws Exception
   {
      TransactionTracker tt = getStore().getTransactionTracker();
      // Drop
      removeAll(Book.class);
      // Create book in a transaction
      Transaction bookTx = tt.getTransaction(TransactionTracker.TX_NEW);
      bookTx.begin();
      Book book = new Book("Title","ISBN");
      getStore().save(book);
      List books = getStore().find("find book");
      Assert.assertEquals(books.size(),1); // Should see the book
      bookTx.commit();
      // Now in another transaction remove the book
      bookTx = tt.getTransaction(TransactionTracker.TX_NEW);
      bookTx.begin();
      getStore().remove(book);
      ((LazyListImpl)books).refresh();
      Assert.assertEquals(books.size(),1);
      bookTx.commit();
   }

   public void testCommitWhileSelect()
      throws Exception
   {
      TransactionTracker tt = getStore().getTransactionTracker();
      // Drop
      removeAll(Book.class);
      // Create book in a transaction but do not commit yet
      Transaction bookTx = tt.getTransaction(TransactionTracker.TX_NEW);
      bookTx.begin();
      Book book = new Book("Title","ISBN");
      getStore().save(book);
      // Now in another transaction, make a query for the books
      Transaction queryTx = tt.getTransaction(TransactionTracker.TX_NEW);
      queryTx.begin();
      List books = getStore().find("find book");
      Assert.assertEquals(books.size(),0);
      queryTx.commit(); // Close the query transaction
      // Now commit the book transaction
      bookTx.commit();
      // Now the query should not see the commited book, since
      // it didn't saw it the first time
      ((LazyListImpl) books).refresh();
      Assert.assertEquals(books.size(),0);
   }

   public void testNormalQueryInsensitive()
      throws Exception
   {
      TransactionTracker tt = getStore().getTransactionTracker();
      // Drop
      removeAll(Book.class);
      // Start a transaction
      Transaction bookTx = tt.getTransaction(TransactionTracker.TX_NEW);
      bookTx.begin();
      List books = getStore().find("find book");
      Assert.assertEquals(books.size(),0);
      // Now in another transaction, make a book
      Transaction createTx = tt.getTransaction(TransactionTracker.TX_NEW);
      createTx.begin();
      getStore().save(new Book("Book of Bokonon","10"));
      createTx.commit();
      // Now, the query in the original transaction should not
      // see this new book
      books = getStore().find("find book");
      Assert.assertEquals(books.size(),0);
      bookTx.commit();
   }

   public void testNormalQuerySeesTransaction()
      throws Exception
   {
      TransactionTracker tt = getStore().getTransactionTracker();
      // Drop
      removeAll(Book.class);
      // Start a transaction
      Transaction bookTx = tt.getTransaction(TransactionTracker.TX_NEW);
      bookTx.begin();
      // Now create a book in the same transaction
      getStore().save(new Book("Book of Bokonon","10"));
      // Now, the query in the original transaction should see this new book
      List books = getStore().find("find book");
      Assert.assertEquals(books.size(),1);
      bookTx.commit();
   }

   public void testHistoricalQueryInsensitive()
      throws Exception
   {
      TransactionTracker tt = getStore().getTransactionTracker();
      // Drop
      removeAll(Book.class);
      // Start a transaction
      Transaction bookTx = tt.getTransaction(TransactionTracker.TX_NEW);
      bookTx.begin();
      List books = getStore().find("find book");
      Assert.assertEquals(books.size(),0);
      // Now in another transaction, make a book
      Transaction createTx = tt.getTransaction(TransactionTracker.TX_NEW);
      createTx.begin();
      getStore().save(new Book("Book of Bokonon","10"));
      createTx.commit();
      // Note the date and wait
      Thread.currentThread().sleep(1000);
      Date currentDate = new Date();
      Thread.currentThread().sleep(1000);
      // Now, the query in the original transaction should not
      // see this new book
      books = getStore().find("find book at ?", new Object[] { currentDate });
      Assert.assertEquals(books.size(),0);
      bookTx.commit();
   }

   public void testHistoricalQuerySeesTransaction()
      throws Exception
   {
      TransactionTracker tt = getStore().getTransactionTracker();
      // Drop
      removeAll(Book.class);
      // Start a transaction
      Transaction bookTx = tt.getTransaction(TransactionTracker.TX_NEW);
      bookTx.begin();
      // Now in the same transaction, make a book
      getStore().save(new Book("Book of Bokonon","10"));
      // Note the date and wait
      Thread.currentThread().sleep(1000);
      Date currentDate = new Date();
      Thread.currentThread().sleep(1000);
      // Now, the query in the original transaction should
      // see this new book
      List books = getStore().find("find book at ?", new Object[] { currentDate });
      Assert.assertEquals(books.size(),1);
      bookTx.commit();
   }
}


