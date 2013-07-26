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
import java.util.HashMap;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.apache.log4j.Logger;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;

/**
 * Test whether the store itself handles transactions well.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class InternalTransactionTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(InternalTransactionTests.class);

   public void testSuccessfulSaveCommit()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create
      Book b = new Book("Learn brain surgery in 7 days","1-2-3-4");

      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      getStore().save(b);
      tx.commit();

      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
   }
   
   public void testSuccessfulSaveRollback()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create
      Book b = new Book("Learn brain surgery in 7 days","1-2-3-4");

      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      getStore().save(b);
      tx.rollback();

      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),0);
   }

   public void testUnsuccessfulSaveCommit()
      throws Exception
   {
      // Drop
      removeAll(MapHolder.class);
      // Create
      MapHolder h = new MapHolder();
      HashMap map = new HashMap();
      map.put("book",new Book("Learn brain surgery in 7 days","1-2-3-4"));
      map.put("wrong",new int[] { 1,2 });
      h.setMeta(map);
      try
      {
         getStore().save(h);
         Assert.fail("saving a map with a reserved type succseeded.");
      } catch ( Exception e ) {
         logger.debug("expected exception",e);
      }

      List result = getStore().find("find mapholder");
      Assert.assertEquals(result.size(),0);
   }
   
   public void testSuccessfulRemoveCommit()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create
      Book b = new Book("Learn brain surgery in 7 days","1-2-3-4");
      getStore().save(b);

      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      getStore().remove(b);
      tx.commit();

      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),0);
   }
   
   public void testSuccessfulRemoveRollback()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create
      Book b = new Book("Learn brain surgery in 7 days","1-2-3-4");
      getStore().save(b);

      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      getStore().remove(b);
      tx.rollback();

      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
   }


}

