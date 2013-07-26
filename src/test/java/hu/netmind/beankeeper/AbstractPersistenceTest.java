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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.util.*;
import org.apache.log4j.Logger;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.Assert;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;

/**
 * Provides database and other methods for simpler test cases.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public abstract class AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(AbstractPersistenceTest.class);
   
   protected void setUpStore()
      throws Exception
   {
      StoreProvider.getInstance().setUpStore();
   }

   @BeforeTest
   public void testStorePresent()
   {
      Assert.assertNotNull(StoreProvider.getInstance().getStore());
   }

   /**
    * After each method, make sure, that there is only one
    * node active. This ensures that the tests behave correctly,
    * and don't mess up the results of other tests.
    */
   @AfterMethod @BeforeMethod
   protected void checkStore()
      throws Exception
   {
      // Must be 1 one always present
      int count = getRowCount("nodes");
      Assert.assertEquals(count,1,"There were "+count+" nodes according to the table after the test.");
      // There should be no active transactions
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_OPTIONAL);
      Assert.assertNull(tx,"There should be no transactions active here, but there is: "+tx);
   }

   public int getRowCount(String table)
      throws Exception
   {
      PreparedStatement pstmt = getConnection().prepareStatement("select * from "+table);
      try
      {
         int count = 0;
         ResultSet rs = pstmt.executeQuery();
         while ( rs.next() )
            count++;
         rs.close();
         return count;
      } finally {
         pstmt.close();
         getConnection().commit();
      }
   }

   protected void tearDownStore()
   {
      StoreProvider.getInstance().tearDownStore();
   }

   public Store getStore()
   {
      return StoreProvider.getInstance().getStore();
   }

   public Connection getConnection()
   {
      return StoreProvider.getInstance().getConnection();
   }

   public Store newStore()
      throws Exception
   {
      return StoreProvider.getInstance().newStore();
   }

   public int getCount(String table)
      throws Exception
   {
      PreparedStatement pstmt = getConnection().prepareStatement("select count(*) from "+table);
      ResultSet rs = pstmt.executeQuery();
      int result = -1;
      if ( rs.next() )
         result = rs.getInt(1);
      rs.close();
      pstmt.close();
      return result;
   }

   public void removeAll(Class cl)
      throws Exception
   {
      removeAll(cl,null);
   }

   public void removeAll(Class cl, String dynamicName)
      throws Exception
   {
      logger.debug("removing class: "+cl);
      Transaction tx = getStore().getTransactionTracker().getTransaction(
            TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         List objects = getStore().find("find item("+cl.getName()+")");
         int size = objects.size();
         logger.debug("removing "+size+" objects or type: "+cl);
         int count = 0;
         for ( Object obj : objects )
         {
            getStore().remove(obj);
            count++;
         }
         logger.debug("removed "+count+" objects");
         Assert.assertEquals(count,size);
      } finally {
         tx.commit();
      }
   }

   public void dropTables(String pattern)
      throws Exception
   {
      DatabaseMetaData dmd = getConnection().getMetaData();
      String databaseName = dmd.getDatabaseProductName();
      ResultSet rs;
      if ( 
           databaseName.equalsIgnoreCase("oracle") ||
           databaseName.equalsIgnoreCase("hsql database engine") ||
           databaseName.equalsIgnoreCase("apache derby") )
         rs = dmd.getTables(null,null,pattern.toUpperCase(),new String[] { "TABLE" });
      else
         rs = dmd.getTables(null,null,pattern, new String[] { "TABLE" });
      logger.debug("dropping tables matching: "+pattern);
      while ( rs.next() )
      {
         // Delete found tables
         String tableName = rs.getString("TABLE_NAME");
         logger.debug("found table: "+tableName+", will drop.");
         try
         {
            PreparedStatement pstmt = getConnection().prepareStatement("drop table "+tableName);
            pstmt.executeUpdate();
            pstmt.close();
         } catch ( Exception e ) {
            logger.debug("could not delete table: "+tableName+", error was: "+e.getMessage());
         }
         getConnection().commit();
      }
      rs.close();
      logger.debug("all tables matching '"+pattern+"' were dropped.");
   }

   /**
    * Utility function, returns the number of objects which are exactly
    * the given class.
    */
   public int getCount(Collection col, Class type)
   {
      int result = 0;
      Iterator it = col.iterator();
      while ( it.hasNext() )
      {
         Object obj = it.next();
         if ( obj.getClass().equals(type) )
            result++;
      }
      return result;
   }

   public boolean isSuccessful(final Runnable r)
   {
      try
      {
         final ArrayList exceptions = new ArrayList();
         Thread thread = new Thread(new Runnable()
               {
                  public void run()
                  {
                     try
                     {
                        r.run();
                     } catch ( Exception e ) {
                        logger.debug("exception while running thread",e);
                        exceptions.add(e);
                     }               
                  }
               });
         thread.start();
         thread.join();
         return exceptions.size() == 0;
      } catch ( Exception e ) {
         Assert.fail("exception caught in thread test: "+e.getMessage());
      }
      return false;
   }
}

