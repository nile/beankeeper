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
import java.util.ResourceBundle;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;

/**
 * These tests are need to run on an empty database.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class VanillaTests extends AbstractPersistenceTest
{
   @BeforeMethod
   protected void cleanStore()
      throws Exception
   {
      dropTables("%");
      tearDownStore();
      setUpStore();
   }

   public void testSelectFromNonExistentTable()
      throws Exception
   {
      // Select
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),0);

      // Again
      result = getStore().find("find book where book.title='No worries.'");
      Assert.assertEquals(result.size(),0);
   }
   
   public void testReferToNonExistentTable()
      throws Exception
   {
      // Create book but not author
      Book b = new Book("Book of no Author","1");
      getStore().save(b);
     
      // Select
      List result = getStore().find("find book where book.mainauthor.firstName='Bob'");
      Assert.assertEquals(result.size(),0);
   }
   
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

   @Test(dependsOnMethods = { "supportsMultipleStores" })
   public void testModelUpdateFromTwoNodes()
      throws Exception
   {
      Store store2 = newStore();
      try
      {
         // Create a class with the first store
         getStore().save(new Book("First Store","1"));
         // Create a class with the second store
         store2.save(new Referrer(1));
         // Now select the other's class to see whether they know eachother
         List referrers = getStore().find("find referrer");
         List books = store2.find("find book");
         Assert.assertEquals(referrers.size(),1);
         Assert.assertEquals(books.size(),1);
      } finally {
         store2.close();
      }
   }
}


