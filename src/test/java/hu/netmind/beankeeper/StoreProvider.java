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

import java.sql.Connection;
import java.util.ResourceBundle;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;

/**
 * Provides the store object to the tests.
 */
public class StoreProvider
{
   private static StoreProvider instance;
   private Store store;
   private Connection connection;

   private StoreProvider()
      throws Exception
   {
      setUpStore();
   }

   public static StoreProvider getInstance()
   {
      return instance;
   }

   public Store getStore()
   {
      return store;
   }
   public Connection getConnection()
   {
      return connection;
   }

   public void setUpStore()
      throws Exception
   {
      // Allocate objects
      store = newStore();
      // Allocate a connection that will not influence
      // this thread
      ConnectionEstablisher establisher = new ConnectionEstablisher();
      Thread establisherThread = new Thread(establisher);
      establisherThread.start();
      establisherThread.join();
      connection = establisher.getConnection();
   }

   public Store newStore()
      throws Exception
   {
      // Read properties file
      ResourceBundle config = ResourceBundle.getBundle("test");
      // Allocate class (now only postgres supported)
      String driverclass = config.getString("db.driverclass");
      String url = config.getString("db.url");
      // Return
      return new Store(driverclass,url);
   }

   public void tearDownStore()
   {
      if ( store != null )
         getStore().close();
      connection = null;
   }


   /**
    * Establishes a connection in another thread.
    */
   private class ConnectionEstablisher implements Runnable
   {
      private Connection connection = null;

      public Connection getConnection()
      {
         return connection;
      }

      public void run()
      {
         Transaction tx = getStore().getTransactionTracker().getTransaction(
               TransactionTracker.TX_REQUIRED);
         tx.begin();
         connection = tx.getConnection();
      }
   }

   static
   {
      try
      {
         instance = new StoreProvider();
      } catch ( Exception e ) {
         e.printStackTrace();
      }
   }
}
