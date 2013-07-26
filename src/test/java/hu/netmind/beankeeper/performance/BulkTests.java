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

package hu.netmind.beankeeper.performance;

import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.AbstractPersistenceTest;
import java.util.*;
import java.lang.reflect.Constructor;
import java.sql.*;
import java.text.*;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

/**
 * Run bulk performance tests.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class BulkTests extends AbstractPersistenceTest
{
   private static final int RUN_DURATION = 5000;

   private void writeStats(String line, long startTime, int count)
   {
      writeStats(line,startTime,count,null);
   }

   @BeforeMethod(groups = "performance")
   public void setUpStore()
      throws Exception
   {
      super.setUpStore();
   }

   @AfterMethod(groups = "performance")
   public void tearDownStore()
   {
      super.tearDownStore();
   }

   private void writeStats(String line, long startTime, int count, Transaction tx)
   {
      if ( count > 0 )
      {
         long endTime = System.currentTimeMillis();
         System.out.println(line+": "+(endTime-startTime)+" ms, "+count+
               " ops, "+new DecimalFormat("0.##").format(((double)endTime-startTime)/count)+" ms/op, tx: "+((tx==null)?"none":tx.getStats().toString()));
      }
   }

   private int saveObjects(String line,Class cl)
      throws Exception
   {
      Constructor ctor = cl.getDeclaredConstructor(new Class[] { int.class });
      long startTime = System.currentTimeMillis();
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      int count = 0;
      while ( System.currentTimeMillis() - startTime < RUN_DURATION )
      {
         getStore().save(ctor.newInstance(new Object[] { new Integer(count) }));
         count++;
      }
      tx.commit();
      writeStats(line+" create",startTime,count,tx);
      return count;
   }

   private void selectAndUpdateObjects(String line,int maxCount,Class cl)
      throws Exception
   {
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      // First select the objects
      List objects = findObjects(line,cl);
      // Do modification
      long startTime = System.currentTimeMillis();
      Iterator objectsIterator = objects.iterator();
      int count = 0;
      while ( (objectsIterator.hasNext()) && 
            (System.currentTimeMillis()-startTime<RUN_DURATION) )
      {
         ModifyableObject object = (ModifyableObject) objectsIterator.next();
         object.modify();
         getStore().save(object);
         count++;
      }
      tx.commit();
      writeStats(line+" update",startTime,count,tx);
   }

   private void selectAndRemoveObjects(String line,int maxCount,Class cl)
      throws Exception
   {
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      // Select
      List objects = findObjects(line,cl);
      // Remove
      long startTime = System.currentTimeMillis();
      Iterator objectsIterator = objects.iterator();
      int count = 0;
      while ( (objectsIterator.hasNext()) && 
            (System.currentTimeMillis()-startTime<RUN_DURATION) )
      {
         ModifyableObject object = (ModifyableObject) objectsIterator.next();
         getStore().remove(object);
         count++;
      }
      tx.commit();
      writeStats(line+" remove",startTime,count,tx);
   }

   private List findObjects(String line, Class cl)
   {
      LinkedList objects = new LinkedList();
      long startTime = System.currentTimeMillis();
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      List iterList = getStore().find("find item("+cl.getName()+")");
      Iterator iterIterator = iterList.iterator();
      int count = 0;
      while ( (iterIterator.hasNext()) && 
            (System.currentTimeMillis()-startTime<RUN_DURATION) )
      {
         objects.add(iterIterator.next());
         count++;
      }
      tx.commit();
      writeStats(line+" iteration",startTime,count,tx);
      return objects;
   }

   private void randomObjects(String line,int maxCount,Class cl)
   {
      long startTime = System.currentTimeMillis();
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      List randomList = getStore().find("find item("+cl.getName()+")");
      Random rnd = new Random();
      int count = 0;
      while ( System.currentTimeMillis()-startTime<RUN_DURATION )
      {
         randomList.get(rnd.nextInt(maxCount));
         count++;
      }
      tx.commit();
      writeStats(line+" random",startTime,count,tx);
   }

   @Test(groups = "performance")
   public void testBulkInsertSelectSQL()
      throws Exception
   {
      System.out.println();
      // Drop and create table
      dropTables("bulktable");
      PreparedStatement pstmt = getConnection().prepareStatement("create table bulktable ( id integer, name text, value integer, primary key (id) );");
      pstmt.executeUpdate();
      pstmt.close();
      getConnection().commit();
      // Insert many
      long startTime = System.currentTimeMillis();
      int insertCount = 0;
      while ( System.currentTimeMillis() - startTime < RUN_DURATION )
      {
         PreparedStatement insertStmt = getConnection().prepareStatement(
               "insert into bulktable ( id, name , value) values ( "+insertCount+", 'Name #"+insertCount+"', "+insertCount+");");
         insertStmt.executeUpdate();
         insertStmt.close();
         insertCount++;
      }
      getConnection().commit();
      writeStats("Plain SQL insert.",startTime,insertCount);
      // Select iterate
      startTime = System.currentTimeMillis();
      pstmt = getConnection().prepareStatement("select name,value from bulktable order by name");
      ResultSet rs = pstmt.executeQuery();
      int count = 0;
      while ( rs.next() )
      {
         rs.getString("name");
         rs.getInt("value");
         count++;
      }
      rs.close();
      pstmt.close();
      getConnection().commit();
      writeStats("Plain SQL iteration.",startTime,count);
      // Update many
      startTime = System.currentTimeMillis();
      int updateCount = 0;
      for ( ; (updateCount<insertCount) && 
            (System.currentTimeMillis()-startTime<RUN_DURATION) ; updateCount++ )
      {
         PreparedStatement insertStmt = getConnection().prepareStatement("update bulktable set name = 'Name number "+updateCount+"' where id = "+updateCount);
         insertStmt.executeUpdate();
         insertStmt.close();
      }
      getConnection().commit();
      writeStats("Plain SQL update.",startTime,updateCount);
      // Select random access
      Random rnd = new Random();
      startTime = System.currentTimeMillis();
      int randomCount = 0;
      while ( System.currentTimeMillis() - startTime < RUN_DURATION )
      {
         pstmt = getConnection().prepareStatement("select name,value from bulktable limit 1 offset "+rnd.nextInt(insertCount));
         rs = pstmt.executeQuery();
         rs.next();
         rs.getString("name");
         rs.getInt("value");
         rs.close();
         pstmt.close();
         randomCount++;
      }
      getConnection().commit();
      writeStats("Plain SQL random access.",startTime,randomCount);
      // Remove many
      startTime = System.currentTimeMillis();
      int removeCount = 0;
      for ( ; (removeCount<insertCount)
           && (System.currentTimeMillis()-startTime<RUN_DURATION) ; removeCount++ )
      {
         PreparedStatement insertStmt = getConnection().prepareStatement("delete from bulktable where id = "+removeCount);
         insertStmt.executeUpdate();
         insertStmt.close();
      }
      getConnection().commit();
      writeStats("Plain SQL delete.",startTime,removeCount);
   }

   @Test(groups = "performance")
   public void testBulkInsertSelectWithSimple()
      throws Exception
   {
      System.out.println();
      // Drop tables
      dropTables("simpleobject");
      // Insert many objects
      int count = saveObjects("Simple",SimpleObject.class);
      // Modify the objects
      selectAndUpdateObjects("Simple",count,SimpleObject.class);
      // Select on random access
      randomObjects("Simple",count,SimpleObject.class);
      // Remove objects
      selectAndRemoveObjects("Simple",count,SimpleObject.class); 
   }

   @Test(groups = "performance")
   public void testBulkInsertSelectWithOneSuperclass()
      throws Exception
   {
      System.out.println();
      // Drop tables
      dropTables("onesuperclass");
      dropTables("onesubclass");
      // Insert many objects
      int count = saveObjects("One superclass",OneSubclass.class);
      // Modify the objects
      selectAndUpdateObjects("One superclass",count,OneSubclass.class);
      // Select on random access
      randomObjects("One superclass",count,OneSubclass.class);
      // Remove objects
      selectAndRemoveObjects("One superclass",count,OneSubclass.class); 
   }

   @Test(groups = "performance")
   public void testBulkInsertSelectWithOneObjectAttribute()
      throws Exception
   {
      System.out.println();
      // Drop tables
      dropTables("oneattribute");
      dropTables("simpleobject");
      // Insert many objects
      int count = saveObjects("One attribute",OneAttribute.class);
      // Modify the objects
      selectAndUpdateObjects("One attribute",count,OneAttribute.class);
      // Select on random access
      randomObjects("One attribute",count,OneAttribute.class);
      // Remove objects
      selectAndRemoveObjects("One attribute",count,OneAttribute.class); 
   }
}


