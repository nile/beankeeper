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
import java.util.Date;
import org.testng.annotations.Test;
import org.testng.Assert;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionStatistics;
import hu.netmind.beankeeper.transaction.TransactionTracker;

/**
 * Tests the cache mechanism of the library.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class CacheTests extends AbstractPersistenceTest
{
   public void testCacheHistoricResult()
      throws Exception
   {
      removeAll(Book.class);
      // Memorize
      Date noDate = new Date();
      // Create book
      Book book = new Book("Cache","1-2-3-4");
      // Save in store
      getStore().save(book);
      // Select historically
      Assert.assertEquals(getStore().find("find book at ?",new Object[] { noDate }).size(),0);
      // Select normally
      Assert.assertEquals(getStore().find("find book").size(),1);
   }

   public void testCacheSameTransaction()
      throws Exception
   {
      removeAll(Book.class);
      // Create book
      Book book = new Book("Cache","1-2-3-4");
      // Save in store
      getStore().save(book);
      // Select
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      List result = getStore().find("find book where book.title='Cache'");
      TransactionStatistics stats = new TransactionStatistics();
      stats.add(tx.getStats());
      // Again
      result = getStore().find("find book where book.title='Cache'");
      // Cache must have been invoked
      Assert.assertEquals(tx.getStats().getSelectCount(),stats.getSelectCount());
      // Close transaction
      tx.commit();
   }
   
   public void testCacheOtherTransaction()
      throws Exception
   {
      removeAll(Book.class);
      // Create book
      Book book = new Book("Cache","1-2-3-4");
      // Save in store
      getStore().save(book);
      // Select
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      List result = getStore().find("find book where book.title='Cache'");
      TransactionStatistics stats = new TransactionStatistics();
      stats.add(tx.getStats());
      tx.commit();
      // Again
      tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      result = getStore().find("find book where book.title='Cache'");
      // Cache must have been invoked
      Assert.assertEquals(tx.getStats().getSelectCount(),0);
      tx.commit();
      // Close transaction
   }

   public void testCacheModifiedOtherTablesTransaction()
      throws Exception
   {
      removeAll(Book.class);
      // Create book
      Book book = new Book("Cache","1-2-3-4");
      // Save in store
      getStore().save(book);
      // Select
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      getStore().save(new Author("Spence","Persi")); // Unrelated save
      List result = getStore().find("find book where book.title='Cache'");
      TransactionStatistics stats = new TransactionStatistics();
      stats.add(tx.getStats());
      tx.commit();
      // Again
      tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      result = getStore().find("find book where book.title='Cache'");
      // Cache must have been invoked
      Assert.assertEquals(tx.getStats().getSelectCount(),0);
      tx.commit();
      // Close transaction
   }

   public void testCacheModifiedSameTableTransaction()
      throws Exception
   {
      removeAll(Book.class);
      // Create book
      Book book = new Book("Cache","1-2-3-4");
      // Save in store
      getStore().save(book);
      // Select
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      getStore().save(new Book("Persistence does Cache","1-2-3-4")); // Make tx dirty
      List result = getStore().find("find book where book.title='Cache'");
      TransactionStatistics stats = new TransactionStatistics();
      stats.add(tx.getStats());
      tx.commit();
      // Again
      tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      result = getStore().find("find book where book.title='Cache'");
      result.size(); // Make it load
      // Cache was not invoked
      Assert.assertEquals(tx.getStats().getSelectCount(),1);
      tx.commit();
      // Close transaction
   }

   public void testCacheModifiedRelatedTableTransaction()
      throws Exception
   {
      removeAll(Book.class);
      // Create book
      Book book = new Book("Cache","1-2-3-4");
      book.setMainAuthor(new Author("Spence","Persi"));
      // Save in store
      getStore().save(book);
      // Select
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      getStore().save(new Author("Candy","Cache")); // Make tx dirty
      List result = getStore().find("find book where book.mainauthor.firstName='Spence'");
      TransactionStatistics stats = new TransactionStatistics();
      stats.add(tx.getStats());
      tx.commit();
      // Again
      tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      result = getStore().find("find book where book.mainauthor.firstName='Spence'");
      result.size();
      // Cache was not invoked
      Assert.assertEquals(tx.getStats().getSelectCount(),1);
      tx.commit();
      // Close transaction
   }

}

