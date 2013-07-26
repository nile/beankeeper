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
import java.util.Collections;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.Assert;
import hu.netmind.beankeeper.event.*;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.transaction.event.*;
import hu.netmind.beankeeper.common.StoreException;

/**
 * Test the transaction framework.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class TransactionTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(TransactionTests.class);
   
   private boolean called = false; // Helper

   @AfterMethod
   protected void checkTransaction()
   {
      Transaction tx = getStore().getTransactionTracker().getTransaction(
            TransactionTracker.TX_OPTIONAL);
      Assert.assertNull(tx,"transaction stuck, tests aborting");
   }

   public void testAtomicOperations()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);
      // Create stuff
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         getStore().save(new Book("Wayne's World","1"));
         getStore().save(new Book("Wayne's World II.","2"));
      } finally {
         tx.rollback();
      }
      // Test
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),0);
   }
   
   public void testMultilevelTransactionInnerCommit()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);
      // Create stuff
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         getStore().save(new Book("Wayne's World","1"));
         tx.begin();
         try
         {
            getStore().save(new Book("Wayne's World II.","2"));
         } finally {
            tx.commit();
         }
      } finally {
         tx.rollback();
      }
      // Test
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),0);
   }
   
   public void testMultilevelTransactionOuterCommit()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);
      // Create stuff
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         getStore().save(new Book("Wayne's World","1"));
         tx.begin();
         try
         {
            getStore().save(new Book("Wayne's World II.","2"));
         } finally {
            tx.rollback();
         }
      } finally {
         tx.commit();
      }
      // Test
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),0);
   }
   
   public void testTransactionRequiredSameThread()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);
      // Create stuff
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         getStore().save(new Book("Wayne's World","1"));
         Transaction tx2 = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
         Assert.assertSame(tx,tx2);
         tx2.begin();
         try
         {
            getStore().save(new Book("Wayne's World II.","2"));
         } finally {
            tx2.rollback();
         }
      } finally {
         tx.commit();
      }
      // Test
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),0);
   }
   
   public void testTransactionNewSameThread()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);
      // Create stuff
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         getStore().save(new Book("Wayne's World","1"));
         Transaction tx2 = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
         tx2.begin();
         try
         {
            getStore().save(new Book("Wayne's World II.","2"));
         } finally {
            tx2.rollback();
         }
      } finally {
         tx.commit();
      }
      // Test
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
   }
   
   public void testListenerCommit()
      throws Exception
   {
      // Register listener
      called = false;
      EventDispatcher ed = getStore().getEventDispatcher();
      TransactionTracker tt = getStore().getTransactionTracker();
      PersistenceEventListener listener = new PersistenceEventListener()
            {
               public void handle(PersistenceEvent event)
               {
                  if ( event instanceof TransactionCommittedEvent )
                     called = true;
               }
            };
      ed.registerListener(listener);
      try
      {
         // Do a transaction
         getStore().save(new Book("Book Of Bokonon","1-2-3-4"));
         // Check
         Assert.assertTrue(called);
      } finally {
         ed.unregisterListener(listener);
      }
   }

   public void testListenerRollback()
      throws Exception
   {
      // Register listener
      called = false;
      EventDispatcher ed = getStore().getEventDispatcher();
      TransactionTracker tt = getStore().getTransactionTracker();
      PersistenceEventListener listener = new PersistenceEventListener()
            {
               public void handle(PersistenceEvent event)
               {
                  if ( event instanceof TransactionRolledbackEvent )
                     called = true;
               }
            };
      ed.registerListener(listener);
      try
      {
         // Do a transaction
         Transaction tx = tt.getTransaction(TransactionTracker.TX_REQUIRED);
         tx.begin();
         try
         {
            getStore().save(new Book("Book Of Bokonon","1-2-3-4"));
         } finally {
            tx.rollback();
         }
         // Check
         Assert.assertTrue(called);
      } finally {
         ed.unregisterListener(listener);
      }
   }

   public void testListenerRecursion()
      throws Exception
   {
      called = false;
      // Register listener
      EventDispatcher ed = getStore().getEventDispatcher();
      final TransactionTracker tt = getStore().getTransactionTracker();
      PersistenceEventListener listener = new PersistenceEventListener()
            {
               public void handle(PersistenceEvent event)
               {
                  if ( ! (event instanceof TransactionCommittingEvent) )
                     return;
                  Transaction t = ((TransactionEvent) event).getTransaction();
                  // Determine whether internal
                  logger.debug("received commit for transaction: "+t+", with keys: "+t.keySet());
                  if ( t.get("Internal") != null )
                  {
                     called = true;
                     return; // Avoid recursion
                  }
                  // Do transaction
                  Transaction tx = tt.getTransaction(TransactionTracker.TX_NEW);
                  tx.put("Internal","true");
                  logger.debug("starting internal transaction: "+tx+", with keys: "+tx.keySet());
                  tx.begin();
                  try
                  {
                     getStore().save(new Book("Book Of Bokonon","1-2-3-4"));
                  } finally {
                     tx.commit();
                  }
                  logger.debug("internal transaction ended: "+tx);
               }
               public void transactionRolledback(Transaction t)
               {
               }
            };
      ed.registerListener(listener);
      try
      {
         // Do a transaction
         getStore().save(new Book("Book Of Bokonon","1-2-3-4"));
         // Check
         Assert.assertFalse(called);
      } finally {
         ed.unregisterListener(listener);
      }
   }

   public void testNewVisibility()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);
      // Create stuff
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         getStore().save(new Book("Wayne's World","1"));
         Transaction tx2 = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
         tx2.begin();
         try
         {
            Assert.assertEquals(getStore().find("find book"),Collections.EMPTY_LIST);
         } finally {
            tx2.commit();
         }
      } finally {
         tx.commit();
      }
   }

   public void testNewParameters()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);
      // Create stuff
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         getStore().save(new Book("Wayne's World","1"));
         tx.put("Book","yes");
         Transaction tx2 = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
         tx2.begin();
         try
         {
            Assert.assertNull(tx2.get("Book"));
         } finally {
            tx2.commit();
         }
      } finally {
         tx.commit();
      }
   }

   public void testNewParametersBack()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);
      // Create stuff
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         getStore().save(new Book("Wayne's World","1"));
         Transaction tx2 = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
         tx2.begin();
         tx2.put("Book","no");
         tx2.commit();
         Assert.assertNull(tx.get("Book"));
      } finally {
         tx.commit();
      }
   }
      
   public void testListOutOfActiveTransaction()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);
      // Register class
      getStore().save(new Book("Wayne's World","1"));
      // Get a list which has transaction bindings
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         getStore().save(new Book("Bill and Ted","2"));
         List result = getStore().find("find book");
         // Before closing try to use the list in another transaction
         Transaction tx2 = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
         tx2.begin();
         try
         {
            result.size();
            Assert.fail("could query a list outside it's still open transaction, that should be prohibited");
         } catch ( StoreException e ) {
            // Ok
            logger.debug("expected exception",e);
         } finally {
            tx2.commit();
         }
      } finally {
         // Close all
         tx.commit();
      }
   }

   public void testDoubleSave()
      throws Exception
   {
      // Drop tables
      removeAll(Referrer.class);
      removeAll(ReferrerSubclass.class);
      // Create
      ReferrerSubclass obj = new ReferrerSubclass(1,1);
      getStore().save(obj);
      // Double save
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         obj.setIdentity(2);
         getStore().save(obj);
         obj.setIdentity(3);
         getStore().save(obj);
      } finally {
         tx.commit();
      }
      // Test
      List result = getStore().find("find referrersubclass");
      Assert.assertEquals(result.size(),1);
   }
   
   public void testCombinedSaveInsert()
      throws Exception
   {
      // Drop tables
      removeAll(Referrer.class);
      removeAll(ReferrerSubclass.class);
      // Create
      ReferrerSubclass obj = new ReferrerSubclass(1,1);
      getStore().save(obj);
      // Double save
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         obj.setIdentity(2);
         getStore().save(obj);
         getStore().save(new ReferrerSubclass(2,2));
         obj.setIdentity(3);
         getStore().save(obj);
      } finally {
         tx.commit();
      }
      // Test
      List result = getStore().find("find referrersubclass");
      Assert.assertEquals(result.size(),2);
   }
   
}

