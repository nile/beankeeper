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

import java.util.*;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;
import org.testng.Assert;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.transaction.TransactionStatistics;

/**
 * Tests for minimal performance statistics.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class PerformanceTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(PerformanceTests.class);

   public void testContainerAdd()
      throws Exception
   {
      removeAll(SetHolder.class);
      removeAll(Referrer.class);
      getStore().save(new SetHolder()); // Init tables
      getStore().save(new Referrer(99)); // Init tables
      // Create holder with items
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Referrer(3));
      holder.getSet().add(new Referrer(4));
      holder.getSet().add(new Referrer(5));
      // Save in store
      logger.debug("inserting first batch");
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      getStore().save(holder);
      logger.debug("first batch completed");
      TransactionStatistics stats = new TransactionStatistics();
      stats.add(tx.getStats());
      Assert.assertEquals(stats.getUpdateCount(),0); // Update nothing
      Assert.assertEquals(stats.getInsertCount(),11); // Insert holder + 5 items + 5 relations
      Assert.assertEquals(stats.getSelectCount(),0); // No selects
      tx.commit();
      // Insert the item so it won't be inserted
      Referrer newRef = new Referrer(6);
      getStore().save(newRef);
      // Insert an item
      logger.debug("starting to insert new item into container");
      tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      holder.getSet().add(newRef);
      getStore().save(holder);
      stats = new TransactionStatistics();
      stats.add(tx.getStats());
      logger.debug("finished insert new item");
      tx.commit();
      // Check
      Assert.assertEquals(stats.getUpdateCount(),1); // Update holder
      Assert.assertEquals(stats.getInsertCount(),2); // Insert into list, and new holder version
      Assert.assertEquals(stats.getSelectCount(),0); // No selects
   }
   
}

