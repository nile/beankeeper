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
import org.testng.annotations.Test;
import org.testng.Assert;
import org.apache.log4j.Logger;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.lock.LockTracker;
import hu.netmind.beankeeper.lock.ConcurrentModificationException;

/**
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class LockTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(LockTests.class);

   public void testLockModifyUnlock()
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
         // Do some modification
         book.setTitle("Locks II.");
         getStore().save(book);
         // Unlock
         lockTracker.unlock(book);
         // Test if we can modify it
         Book dbBook = (Book) getStore().findSingle("find book");
         dbBook.setTitle("Return of the Locks");
         getStore().save(dbBook);
      } finally {
         lockTracker.unlock(book);
      }
   }

   public void testLockUnexistingModifyUnlock()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      lockTracker.lock(book);
      try
      {
         // Do some modification
         book.setTitle("Locks II.");
         getStore().save(book);
         // Unlock
         lockTracker.unlock(book);
         // Test if we can modify it
         Book dbBook = (Book) getStore().findSingle("find book");
         dbBook.setTitle("Return of the Locks");
         getStore().save(dbBook);
      } finally {
         lockTracker.unlock(book);
      }
   }

   public void testLockModifyFromOtherObjectUnlock()
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
         // Do some modification to another instance.
         // Test if we can modify it. We should be
         // able to, because it's the same thread.
         Book dbBook = (Book) getStore().findSingle("find book");
         dbBook.setTitle("Return of the Locks");
         getStore().save(dbBook);
      } finally {
         // Unlock
         lockTracker.unlock(book);
      }
   }

   public void testLockUnlockModifyOther()
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
         // Save
         book.setTitle("Lock II.");
         getStore().save(book);
      } finally {
         // Unlock
         lockTracker.unlock(book);
      }
      // Do some modification to another instance
      Book dbBook = (Book) getStore().findSingle("find book");
      dbBook.setTitle("Return of the Locks");
      getStore().save(dbBook);
   }

   public void testUnlockWithoutLock()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      // Create book
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      getStore().save(book);
      // Unlock
      lockTracker.unlock(book);
   }

   public void testLockTwiceUnlockOnce()
      throws Exception
   {
      // Get the lockmanager
      final LockTracker lockTracker = getStore().getLockTracker();
      // Create book
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      getStore().save(book);
      // Lock and keep
      lockTracker.lock(book);
      try
      {
         // Now lock again in inner transaction
         lockTracker.lock(book);
         book.setTitle("Locks II.");
         getStore().save(book);
         lockTracker.unlock(book);
         // Do the lock still should be there, try it
         Assert.assertFalse(isSuccessful(new Runnable() {
               public void run()
               {
                  Book dbBook = (Book) getStore().findSingle("find book");
                  lockTracker.lock(dbBook);
               }
            }));
      } finally {
         lockTracker.unlock(book);
         lockTracker.unlock(book);
      }
   }

   public void testLockModifyFromOtherThreadWait()
      throws Exception
   {
      // Get the lockmanager
      final LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      getStore().save(book);
      lockTracker.lock(book);
      try
      {
         // Do the lock still should be there, try it
         Assert.assertFalse(isSuccessful(new Runnable() {
               public void run()
               {
                  Book dbBook = (Book) getStore().findSingle("find book");
                  lockTracker.lock(dbBook,100);
               }
            }));
      } finally {
         lockTracker.unlock(book);
      }
   }

   public void testLockModifyFromOtherObjectWaitForUnlock()
      throws Exception
   {
      // Get the lockmanager
      final LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      final Book book = new Book("Locks","1");
      getStore().save(book);
      lockTracker.lock(book);
      try
      {
         Thread newThread = new Thread(new Runnable()
               {
                  public void run()
                  {
                     try
                     {
                        Thread.sleep(500);
                     } catch ( Exception e ) {
                        logger.debug("error while waiting",e);
                     }
                     lockTracker.unlock(book);
                  }
               });
         newThread.start();
         // Try to lock
         Book dbBook = (Book) getStore().findSingle("find book");
         lockTracker.lock(dbBook,1000);
         lockTracker.unlock(dbBook);
         newThread.join();
      } finally {
         lockTracker.unlock(book);
      }
   }

   public void testLockNonexisting()
      throws Exception
   {
      LockTracker lockTracker = getStore().getLockTracker();
      Book book = new Book("Lockbook","1");
      lockTracker.lock(book);
      lockTracker.unlock(book);
   }

   public void testLockCurrentModifyUnlock()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      getStore().save(book);
      lockTracker.lockEnsureCurrent(book);
      try
      {
         // Do some modification
         book.setTitle("Locks II.");
         getStore().save(book);
         // Unlock
         lockTracker.unlock(book);
         // Test if we can modify it
         Book dbBook = (Book) getStore().findSingle("find book");
         dbBook.setTitle("Return of the Locks");
         getStore().save(dbBook);
      } finally {
         lockTracker.unlock(book);
      }
   }

   public void testLockCurrentUnexistingModifyUnlock()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      lockTracker.lockEnsureCurrent(book);
      try
      {
         // Do some modification
         book.setTitle("Locks II.");
         getStore().save(book);
      } finally {
         // Unlock
         lockTracker.unlock(book);
      }
      // Test if we can modify it
      Book dbBook = (Book) getStore().findSingle("find book");
      dbBook.setTitle("Return of the Locks");
      getStore().save(dbBook);
   }

   public void testLockCurrentNonexisting()
      throws Exception
   {
      LockTracker lockTracker = getStore().getLockTracker();
      Book book = new Book("Lockbook","1");
      lockTracker.lockEnsureCurrent(book);
      lockTracker.unlock(book);
   }

   public void testLockEnsureModifyLocalOtherObject()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      getStore().save(book);
      // Do some modification with another instance
      Book dbBook = (Book) getStore().findSingle("find book");
      dbBook.setTitle("Return of the Locks");
      getStore().save(dbBook);
      // Try to lock original version
      try
      {
         lockTracker.lockEnsureCurrent(book);
         Assert.fail("book was modified, original instance should not be current");
      } catch ( Exception e ) {
         // Nothing to do
         logger.debug("expected exception",e);
      }
   }

   public void testLockEnsureRemoveSame()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      getStore().save(book);
      // Remove
      getStore().remove(book);
      // Try to lock original version
      try
      {
         lockTracker.lockEnsureCurrent(book);
         Assert.fail("book was removed, original instance should not be current");
      } catch ( Exception e ) {
         // Nothing to do
         logger.debug("expected exception",e);
      }
   }

   public void testLockEnsureRemoveOther()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      getStore().save(book);
      // Remove
      Book dbBook = (Book) getStore().findSingle("find book");
      getStore().remove(dbBook);
      // Try to lock original version
      try
      {
         lockTracker.lockEnsureCurrent(book);
         Assert.fail("book was removed, original instance should not be current");
      } catch ( Exception e ) {
         // Nothing to do
         logger.debug("expected exception",e);
      }
   }

   public void testLockEnsureInsideTransaction()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      // Lock and object
      removeAll(Book.class);
      Book book = new Book("Locks","1");
      getStore().save(book);
      // Do some operations inside the transaction
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      Book dbBook = (Book) getStore().findSingle("find book");
      dbBook.setTitle("Return of the Locks");
      getStore().save(dbBook);
      dbBook.setTitle("Return of the Locks II.");
      getStore().save(dbBook);
      // Try to lock object
      lockTracker.lockEnsureCurrent(dbBook);
      lockTracker.unlock(dbBook);
      // End tx
      tx.commit();
   }

   public void testCurrentTableNoTransaction()
      throws Exception
   {
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lockEnsureCurrent(Object.class);
      lockTracker.unlock(Object.class);
   }

   public void testCurrentTableNoModifications()
      throws Exception
   {
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lockEnsureCurrent(Object.class);
      // End
      lockTracker.unlock(Object.class);
      tx.commit();
   }

   public void testCurrentTableOtherModifications()
      throws Exception
   {
      // Make some objects
      getStore().save(new Book("New","1-2-3-4"));
      getStore().save(new Car());
      // Transaction
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      // Make other table modifications
      getStore().save(new Car());
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lockEnsureCurrent(Book.class);
      // End
      lockTracker.unlock(Book.class);
      tx.commit();
   }

   public void testCurrentTableExactModifications()
      throws Exception
   {
      // Make some objects
      getStore().save(new Car());
      // Transaction
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      getStore().save(new Book("New","1-2-3-4"));
      // Make other table modifications
      Assert.assertTrue(isSuccessful(new Runnable() 
               {
                  public void run()
                  {
                     getStore().save(new Book("Newer","2-3-4-5"));
                  }
               }));
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      try
      {
         lockTracker.lockEnsureCurrent(Book.class);
         Assert.fail("lock says Book is current, but it is not.");
      } catch ( Exception e ) {
         logger.debug("expected exception",e);
      }
      // End
      tx.commit();
   }

   public void testCurrentTableSubclassModificationsClass()
      throws Exception
   {
      // Make some objects
      getStore().save(new Car());
      // Transaction
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      getStore().save(new Book("Start Transaction","4"));
      // Make other table modifications
      Assert.assertTrue(isSuccessful(new Runnable()
               {
                  public void run()
                  {
                     getStore().save(new Car());
                  }
               }));
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      try
      {
         lockTracker.lockEnsureCurrent(VehicleBase.class);
         Assert.fail("lock says VehicleBase is current, but it is not.");
      } catch ( Exception e ) {
         logger.debug("expected exception",e);
      }
      // End
      tx.commit();
   }

   public void testCurrentTableSubclassModificationsInterface()
      throws Exception
   {
      // Make some objects
      getStore().save(new Car());
      // Transaction
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      getStore().save(new Book("Start Transaction","4"));
      // Make other table modifications
      Assert.assertTrue(isSuccessful(new Runnable()
               {
                  public void run()
                  {
                     getStore().save(new Car());
                  }
               }));
      // Get the lockmanager
      LockTracker lockTracker = getStore().getLockTracker();
      try
      {
         lockTracker.lockEnsureCurrent(Vehicle.class);
         Assert.fail("lock says Vehicle is current, but it is not.");
      } catch ( Exception e ) {
         logger.debug("expected exception",e);
      }
      // End
      tx.commit();
   }

   public void testHierarchicalLockSameClass()
      throws Exception
   {
      // Lock class
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(Book.class);
      try
      {
         // Try to create a book
         Assert.assertFalse(isSuccessful(new Runnable()
                  {
                     public void run()
                     {
                        getStore().save(new Book("Threads","1"));
                     }
                  }));
      } finally {
         // Unlock
         lockTracker.unlock(Book.class);
      }
   }

   public void testHierarchicalLockSuperclass()
      throws Exception
   {
      // Lock class
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(Object.class);
      // Try to create a book
      try
      {
         Assert.assertFalse(isSuccessful(new Runnable()
                  {
                     public void run()
                     {
                        getStore().save(new Book("Threads","1"));
                     }
                  }));
      } finally {
         // Unlock
         lockTracker.unlock(Object.class);
      }
   }

   public void testHierarchicalLockSuperinterface()
      throws Exception
   {
      // Lock class
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(Vehicle.class);
      try
      {
         // Try to create a book
         Assert.assertFalse(isSuccessful(new Runnable()
                  {
                     public void run()
                     {
                        getStore().save(new Car());
                     }
                  }));
      } finally {
         // Unlock
         lockTracker.unlock(Vehicle.class);
      }
   }

   public void testHierarchicalLockSameClassInThread()
      throws Exception
   {
      // Lock class
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(Book.class);
      // Try to create a book
      try
      {
         getStore().save(new Book("Threads","1"));
      } finally {
         // Unlock
         lockTracker.unlock(Book.class);
      }
   }

   public void testHierarchicalLockSuperclassInThread()
      throws Exception
   {
      // Lock class
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(Object.class);
      try
      {
         // Try to create a book
         getStore().save(new Book("Threads","1"));
      } finally {
         // Unlock
         lockTracker.unlock(Object.class);
      }
   }

   public void testHierarchicalLockSuperinterfaceInThread()
      throws Exception
   {
      // Lock class
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(Vehicle.class);
      try
      {
         // Try to create a book
         getStore().save(new Car());
      } finally {
         // Unlock
         lockTracker.unlock(Vehicle.class);
      }
   }

   public void testHierarchicalLockClassInThreadLock()
      throws Exception
   {
      // Lock class
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(Vehicle.class);
      lockTracker.lock(VehicleBase.class);
      // Unlock
      lockTracker.unlock(VehicleBase.class);
      lockTracker.unlock(Vehicle.class);
   }

   public void testHierarchicalLockClassInThreadLock2()
      throws Exception
   {
      // Lock class
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(VehicleBase.class);
      lockTracker.lock(Vehicle.class);
      // Unlock
      lockTracker.unlock(Vehicle.class);
      lockTracker.unlock(VehicleBase.class);
   }

   public void testHierarchicalLockClassLock()
      throws Exception
   {
      // Lock class
      final LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(Vehicle.class);
      try
      {
         Assert.assertFalse(isSuccessful(new Runnable() 
                  {
                     public void run()
                     {
                        lockTracker.lock(VehicleBase.class);
                        }
                  }));
      } finally {
         lockTracker.unlock(Vehicle.class);
      }
   }

   public void testHierarchicalLockClassLock2()
      throws Exception
   {
      // Lock class
      final LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(VehicleBase.class);
      try
      {
         Assert.assertFalse(isSuccessful(new Runnable() 
                  {
                     public void run()
                     {
                        lockTracker.lock(Vehicle.class);
                        }
                  }));
      } finally {
         lockTracker.unlock(VehicleBase.class);
      }
   }

   public void testHierarchicalLockObjectAndClassThread()
      throws Exception
   {
      Book book = new Book("Book of Locks","2");
      getStore().save(book);
      // Lock class
      final LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(book);
      try
      {
         Assert.assertFalse(isSuccessful(new Runnable() 
                  {
                     public void run()
                     {
                        lockTracker.lock(Object.class);
                     }
                  }));
      } finally {
         lockTracker.unlock(book);
      }
   }

   public void testHierarchicalLockClassAndObjectThread()
      throws Exception
   {
      final Book book = new Book("Book of Locks","2");
      getStore().save(book);
      // Lock class
      final LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lock(Object.class);
      try
      {
         Assert.assertFalse(isSuccessful(new Runnable() 
                  {
                     public void run()
                     {
                        lockTracker.lock(book);
                     }
                  }));
      } finally {
         lockTracker.unlock(Object.class);
      }
   }

   public void testReadOnlyLockConcept()
      throws Exception
   {
      final Book book = new Book("Book of Locks","2");
      getStore().save(book);
      // Lock object read only
      final LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lockReadOnly(book);
      try
      {
         Assert.assertTrue(isSuccessful(new Runnable() 
                  {
                     public void run()
                     {
                        lockTracker.lockReadOnly(book);
                        lockTracker.unlock(book);
                     }
                  }));
      } finally {
         lockTracker.unlock(book);
      }
   }

   public void testReadWriteLockConcept()
      throws Exception
   {
      final Book book = new Book("Book of Locks","2");
      getStore().save(book);
      // Lock object read only
      final LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lockReadOnly(book);
      try
      {
         Assert.assertFalse(isSuccessful(new Runnable() 
                  {
                     public void run()
                     {
                        lockTracker.lock(book);
                     }
                  }));
      } finally {
         lockTracker.unlock(book);
      }
   }

   public void testEmptyStoreLockEnsure()
      throws Exception
   {
      // Insert a book
      getStore().save(new Book("Title","ISBN"));
      tearDownStore();
      setUpStore();
      // Test whether an empty store says we can lock
      // an object unmodified
      Book book = (Book) getStore().findSingle("find book");
      LockTracker lockTracker = getStore().getLockTracker();
      lockTracker.lockEnsureCurrent(book);
      lockTracker.unlock(book);
   }

   public void testLockEnsureNonexistingLate()
   {
      // Recalibrate modification expiration to zero
      getStore().getConfigurationTracker().getConfiguration().
         setProperty("beankeeper.cache.modification_max_age",0);
      try
      {
         // Now check whether a nonexistent object can be locked
         LockTracker lockTracker = getStore().getLockTracker();
         Book book = new Book("Title","ISBN");
         lockTracker.lockEnsureCurrent(book);
         lockTracker.unlock(book);
      } finally {
         // Reset configuration
         getStore().getConfigurationTracker().getConfiguration().
            clearProperty("beankeeper.cache.modification_max_age");
      }
   }

   public void testLockEnsureExistingLate()
   {
      // Create a book
      Book book = new Book("Title","ISBN");
      getStore().save(book);
      // Recalibrate modification expiration to zero
      getStore().getConfigurationTracker().getConfiguration().
         setProperty("beankeeper.cache.modification_max_age",0);
      try
      {
         // Now check whether a nonexistent object can be locked
         LockTracker lockTracker = getStore().getLockTracker();
         lockTracker.lockEnsureCurrent(book);
         lockTracker.unlock(book);
      } finally {
         // Reset configuration
         getStore().getConfigurationTracker().getConfiguration().
            clearProperty("beankeeper.cache.modification_max_age");
      }
   }

   public void testLockEnsureExistingModifiedLate()
      throws Exception
   {
      removeAll(Book.class);
      // Create a book
      Book book = new Book("Title","ISBN");
      getStore().save(book);
      // Recalibrate modification expiration to zero
      getStore().getConfigurationTracker().getConfiguration().
         setProperty("beankeeper.cache.modification_max_age",0);
      // Modify it
      Book alterBook = (Book) getStore().findSingle("find book");
      alterBook.setTitle("Title Modified");
      getStore().save(alterBook);
      try
      {
         // Now check whether a existing object can be locked
         LockTracker lockTracker = getStore().getLockTracker();
         lockTracker.lockEnsureCurrent(book);
         Assert.fail("lock should not be successful here, object was modified");
      } catch ( ConcurrentModificationException e ) {
         logger.debug("expected exception",e);
      } finally {
         // Reset configuration
         getStore().getConfigurationTracker().getConfiguration().
            clearProperty("beankeeper.cache.modification_max_age");
      }
   }

   public void testLockEnsureExistingOldModificationLate()
      throws Exception
   {
      removeAll(Book.class);
      // Create a book
      Book book = new Book("Title","ISBN");
      getStore().save(book);
      // Recalibrate modification expiration to zero
      getStore().getConfigurationTracker().getConfiguration().
         setProperty("beankeeper.cache.modification_max_age",0);
      try
      {
         // Modify another object, with it forget the old modification
         Book alterBook = new Book("New Title","ISBN");
         getStore().save(alterBook);
         // Now check whether the object can be locked
         LockTracker lockTracker = getStore().getLockTracker();
         lockTracker.lockEnsureCurrent(book);
         lockTracker.unlock(book);
      } finally {
         // Reset configuration
         getStore().getConfigurationTracker().getConfiguration().
            clearProperty("beankeeper.cache.modification_max_age");
      }
   }

   public void testLockEnsureNonexistingOldModificationLate()
      throws Exception
   {
      removeAll(Book.class);
      // Create a book
      Book newBook = new Book("Title","ISBN");
      getStore().save(newBook);
      // Recalibrate modification expiration to zero
      getStore().getConfigurationTracker().getConfiguration().
         setProperty("beankeeper.cache.modification_max_age",0);
      try
      {
         // Modify another object, with it forget the old modification
         Book alterBook = new Book("New Title","ISBN");
         getStore().save(alterBook);
         // Now check whether the a non-existing object can be locked
         Book book = new Book("Title","ISBN");
         LockTracker lockTracker = getStore().getLockTracker();
         lockTracker.lockEnsureCurrent(book);
         lockTracker.unlock(book);
      } finally {
         // Reset configuration
         getStore().getConfigurationTracker().getConfiguration().
            clearProperty("beankeeper.cache.modification_max_age");
      }
   }

   public void testModificationConcept()
      throws Exception
   {
      // Create one book
      Book book = new Book("Title","1234");
      getStore().save(book);
      // Create another book
      getStore().save(new Book("Interfere","1234"));
      // Determine whether it is current, it should be, and
      // it should not require any operations
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         // Lock and ensure current
         getStore().getLockTracker().lockEnsureCurrent(book);
         // Check that this required an operation
         Assert.assertEquals(tx.getStats().getSelectCount(),0);
      } finally {
         getStore().getLockTracker().unlock(book);
         tx.commit();
      }
   }
   
   public void testModificationLimit()
      throws Exception
   {
      // Recalibrate modification chache size to zero
      getStore().getConfigurationTracker().getConfiguration().
         setProperty("beankeeper.cache.modification_max_items",0);
      try
      {
         // Create one book
         Book book = new Book("Title","1234");
         getStore().save(book);
         // Create another book
         getStore().save(new Book("Interfere","1234"));
         // Determine whether it is current, it should be, but
         // it should require a select because it's not in the
         // cache
         Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
         tx.begin();
         try
         {
            // Lock and ensure current
            getStore().getLockTracker().lockEnsureCurrent(book);
            // Check that this required an operation
            Assert.assertEquals(tx.getStats().getSelectCount(),1);
         } finally {
            getStore().getLockTracker().unlock(book);
            tx.commit();
         }
      } finally {
         // Reset configuration
         getStore().getConfigurationTracker().getConfiguration().
            clearProperty("beankeeper.cache.modification_max_items");
      }
   }
}

