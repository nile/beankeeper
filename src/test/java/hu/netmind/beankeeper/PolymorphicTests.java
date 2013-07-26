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
import hu.netmind.beankeeper.transaction.TransactionStatistics;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.common.StoreException;

/**
 * Test on polymorphic selects and saves.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class PolymorphicTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(PolymorphicTests.class);

   private int countClass(Class clazz, List list)
   {
      int result = 0;
      for ( int i=0; i<list.size(); i++ )
         if ( list.get(i).getClass().equals(clazz) )
            result++;
      return result;
   }

   @Test(groups = { "quick" })
   public void testSimplePolymorphicObjectRetrieve()
      throws Exception
   {
      // Drop tables
      removeAll(Writing.class);
    
      // Save
      Article article = new Article("Persistence","DDJ");
      getStore().save(article);

      // Select
      List result = getStore().find("find article");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),article);
      
      // Select superclass
      result = getStore().find("find writing");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),article);
   }

   public void testMultipleObjectSelect()
      throws Exception
   {
      // Drop book table
      removeAll(Writing.class);
    
      // Save
      for ( int i=0; i<10; i++ )
         getStore().save(new Article("Article #"+i,"Art"));
      for ( int i=0; i<10; i++ )
         getStore().save(new ScreenPlay("Play #"+i,i));
      for ( int i=0; i<10; i++ )
         getStore().save(new MovieScript("Script #"+i,i,i));

      // Select simple
      List result = getStore().find("find writing");
      Assert.assertEquals(result.size(),30);
      Assert.assertEquals(countClass(Article.class,result),10);
      Assert.assertEquals(countClass(ScreenPlay.class,result),10);
      Assert.assertEquals(countClass(MovieScript.class,result),10);

      // Select with a condition
      result = getStore().find("find writing where title='Article #5'");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0).getClass(),Article.class);

      // Select subclass only
      result = getStore().find("find article");
      Assert.assertEquals(result.size(),10);
      Assert.assertEquals(countClass(Article.class,result),10);

      // Select subclass only
      result = getStore().find("find screenplay");
      Assert.assertEquals(result.size(),20);
      Assert.assertEquals(countClass(MovieScript.class,result),10);
      Assert.assertEquals(countClass(ScreenPlay.class,result),10);

      // Select subclass only
      result = getStore().find("find moviescript");
      Assert.assertEquals(result.size(),10);
      Assert.assertEquals(countClass(MovieScript.class,result),10);
   }

   public void testBatchObjectSelect()
      throws Exception
   {
      // Drop book table
      removeAll(Writing.class);
    
      // Save
      for ( int i=0; i<100; i++ )
         getStore().save(new Article("Article #"+i,"Art"));

      // Select simple
      List result = getStore().find("find writing");
      Assert.assertEquals(result.size(),100);
      Assert.assertEquals(countClass(Article.class,result),100);
      Assert.assertEquals(countClass(ScreenPlay.class,result),0);
      Assert.assertEquals(countClass(MovieScript.class,result),0);
   }

   public void testBatchListAttributeSelect()
      throws Exception
   {
      // Drop book table
      removeAll(Writing.class);
      removeAll(ArticleContainer.class);
    
      // Save
      ArticleContainer container = new ArticleContainer();
      for ( int i=0; i<100; i++ )
         container.getArticles().add(new Article("Article #"+i,"Art"));
      getStore().save(container);

      // Select simple
      List result = getStore().find("find articlecontainer");
      Assert.assertEquals(result.size(),1);
      ArticleContainer resultContainer = (ArticleContainer) result.get(0);
      List articles = resultContainer.getArticles();
      Assert.assertEquals(articles.size(),100);
      Assert.assertEquals(countClass(Article.class,articles),100);
      Assert.assertEquals(countClass(ScreenPlay.class,articles),0);
      Assert.assertEquals(countClass(MovieScript.class,articles),0);
   }

   public void testSuperclassAttributeSelectUnspecified()
      throws Exception
   {
      // Delete
      removeAll(ReferrerSubclass.class);

      // Insert one
      getStore().save(new ReferrerSubclass(1));

      // Try to select with superclass attribute
      List result = getStore().find("find referrersubclass where identity=1");
      Assert.assertEquals(result.size(),1);
   }

   public void testSuperclassAttributeInWhere()
      throws Exception
   {
      // Delete
      removeAll(ReferrerSubclass.class);
      removeAll(IdentityStuff.class);

      // Insert one
      getStore().save(new IdentityStuff(1));
      getStore().save(new ReferrerSubclass(1));

      // Try to select with superclass attribute
      List result = getStore().find("find identitystuff where identitystuff.identity=referrersubclass.identity");
      Assert.assertEquals(result.size(),1);
   }

   public void testSuperclassAttributeSelect()
      throws Exception
   {
      // Delete
      removeAll(ReferrerSubclass.class);

      // Insert one
      getStore().save(new ReferrerSubclass(1));

      // Try to select with superclass attribute
      List result = getStore().find("find referrersubclass where referrersubclass.identity=1");
      Assert.assertEquals(result.size(),1);
   }

   public void testSuperclassObjectSelect()
      throws Exception
   {
      // Delete
      removeAll(Subclass.class);
      removeAll(Superclass.class);

      // Insert
      Subclass s = new Subclass();
      s.setObject(s);
      getStore().save(s);

      // Select
      List result = getStore().find("find subclass where object = ?",new Object[] { s });
      Assert.assertEquals(result.size(),1);
   }

   public void testSuperclassMapSelect()
      throws Exception
   {
      // Delete
      removeAll(Subclass.class);
      removeAll(Superclass.class);

      // Insert
      Subclass s = new Subclass();
      Subclass c = new Subclass();
      ArrayList list = new ArrayList();
      list.add(c);
      s.setList(list);
      getStore().save(s);

      // Select
      List result = getStore().find("find subclass where list contains ?",new Object[] { c });
      Assert.assertEquals(result.size(),1);
   }

   public void testSuperclassListSelect()
      throws Exception
   {
      // Delete
      removeAll(Subclass.class);
      removeAll(Superclass.class);

      // Insert
      Subclass s = new Subclass();
      Subclass c = new Subclass();
      HashMap map = new HashMap();
      map.put("contain",c);
      s.setMap(map);
      getStore().save(s);

      // Select
      List result = getStore().find("find subclass where map contains ?",new Object[] { c });
      Assert.assertEquals(result.size(),1);
   }

   public void testSameAttribute()
      throws Exception
   {
      removeAll(SameAttrSuper.class);
      removeAll(SameAttrSub1.class);
      removeAll(SameAttrSub2.class);
      
      // Create two objects with same attributes
      SameAttrSub1 sub1 = new SameAttrSub1(1,"aaa");
      SameAttrSub2 sub2 = new SameAttrSub2(2,"bbb");
      // Save
      getStore().save(sub1);
      // Find
      try
      {
         getStore().save(sub2);
         Assert.fail("store had two columns with same name on classes with a common ancestor, it should throw exception, but did not.");
      } catch ( StoreException e ) {
         // This is good, query was invalid
         logger.debug("expected exception",e);
      }
   }

   public void testPolymorphicListSelect()
      throws Exception
   {
      // Drop book table
      removeAll(Writing.class);
      removeAll(ArticleContainer.class);
    
      // Save
      ArticleContainer container = new ArticleContainer();
      container.getArticles().add(new Article("Article #1","Art"));
      container.getArticles().add(new ScreenPlay("Title",3));
      container.getArticles().add(new Writing("Writing"));
      getStore().save(container);

      // Select simple
      List result = getStore().find("find articlecontainer");
      Assert.assertEquals(result.size(),1);
      ArticleContainer resultContainer = (ArticleContainer) result.get(0);
      List articles = resultContainer.getArticles();
      Assert.assertEquals(articles.size(),3);
      Assert.assertEquals(countClass(Article.class,articles),1);
      Assert.assertEquals(countClass(ScreenPlay.class,articles),1);
      Assert.assertEquals(countClass(Writing.class,articles),1);
   }

   public void testExtremePolymorphismConcept()
      throws Exception
   {
      // Drop all objects
      removeAll(Object.class);
      // Insert a list of different objects into the store
      Book book1 = new Book("Title 1","1-2-3-4");
      getStore().save(book1);
      Book book2 = new Book("Title 2","1-2-3-4");
      getStore().save(book2);
      Referrer ref1 = new Referrer(1);
      getStore().save(ref1);
      ReferrerSubclass refsub1 = new ReferrerSubclass(2,2);
      getStore().save(refsub1);
      Author author1 = new Author("X","Y");
      getStore().save(author1);
      // Query
      List result = getStore().find("find object");
      // Check
      Assert.assertEquals(result.size(),5);
      Assert.assertTrue(result.contains(book1));
      Assert.assertTrue(result.contains(book2));
      Assert.assertTrue(result.contains(ref1));
      Assert.assertTrue(result.contains(refsub1));
      Assert.assertTrue(result.contains(author1));
   }

   public void testNonStorableSelect()
      throws Exception
   {
      // Drop all objects
      removeAll(Object.class);
      // Insert a list of different objects into the store
      Book book1 = new Book("Title 1","1-2-3-4");
      getStore().save(book1);
      Book book2 = new Book("Title 2","1-2-3-4");
      getStore().save(book2);
      Referrer ref1 = new Referrer(1);
      getStore().save(ref1);
      ReferrerSubclass refsub1 = new ReferrerSubclass(2,2);
      getStore().save(refsub1);
      Author author1 = new Author("X","Y");
      getStore().save(author1);
      // Create a list with only the books
      ListHolder holder = new ListHolder();
      ArrayList list = new ArrayList();
      list.add(book1);
      list.add(book2);
      list.add(ref1);
      holder.setList(list);
      getStore().save(holder);
      // Query
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      List result = getStore().find("find item(object) where listholder.list contains item and listholder = ?", new Object[] { holder });
      TransactionStatistics stats = new TransactionStatistics();
      result.size(); // Force load
      stats.add(tx.getStats());
      tx.commit();
      // Check
      logger.debug("test result: "+result);
      Iterator i = result.iterator();
      while ( i.hasNext() )
         logger.debug("test result it: "+i.next());
      Assert.assertEquals(stats.getSelectCount(),3);
      Assert.assertEquals(result.size(),3);
      Assert.assertTrue(result.contains(book1));
      Assert.assertTrue(result.contains(book2));
      Assert.assertTrue(result.contains(ref1));
   }

   public void testManyLeftTerms()
      throws Exception
   {
      // Drop
      removeAll(ManySuper.class);
      // Create a lot of manysuper classes
      for ( int i=0; i<50; i++ )
      {
         ManySuper obj = new ManySuper();
         obj.setPersistenceDynamicName("ManySub"+i);
         obj.put("index",new Integer(i));
         getStore().save(obj);
      }
      // Now save a few other subclasses
      for ( int i=0; i<10; i++ )
      {
         ManySuper obj = new ManySuper();
         obj.setPersistenceDynamicName("ManySubExtra");
         obj.put("index",new Integer(100+i));
         getStore().save(obj);
      }
      // Now select all manysupers above index, which would result
      // in a select with many left tables
      List result = getStore().find("find manysuper where index >= 100");
      Assert.assertEquals(result.size(),10);
   }

   public void testNonStorableMiddleTerm()
      throws Exception
   {
      // Create test setup
      removeAll(Book.class);
      // Create
      Book book1 = new Book("Starship internals","1-3-5-7");
      ArrayList authors = new ArrayList();
      authors.add(new Author("Geordi","LaForge"));
      Author data = new Author("Data","");
      authors.add(data);
      authors.add(new Author("Scott","Montgomery"));
      book1.setAuthors(authors);
      getStore().save(book1);
      Book book2 = new Book("I, Robot","3-3-5-7");
      authors = new ArrayList();
      authors.add(new Author("Spock",""));
      authors.add(data);
      book2.setAuthors(authors);
      getStore().save(book2);
      // Make a select which uses a temporary non-storable term, this
      // is not allowed
      try
      {
         List result = getStore().find("find book where book.authors contains item(object) and originalbook(book).authors contains item and originalbook = ?",
            new Object[] { book2 });
         Assert.fail("could select with temporary non-storable term, which should not be allowed.");
      } catch ( StoreException e ) {
         // This is good
         logger.debug("expected exception",e);
      }
      // Select with author type
      List result = getStore().find("find book where book.authors contains item(author) and originalbook(book).authors contains item and originalbook = ?",
         new Object[] { book2 });
      Assert.assertEquals(result.size(),2);
   }
      
   public void testSubclassAttributeSelect()
      throws Exception
   {
      // Delete
      removeAll(Subclass.class);
      removeAll(Superclass.class);
      // Insert a few objects
      Superclass sup1 = new Superclass(1);
      Superclass sup2 = new Superclass(2);
      Superclass sup3 = new Superclass(3);
      getStore().save(sup1);
      getStore().save(sup2);
      getStore().save(sup3);
      Subclass s1 = new Subclass();
      s1.setPrimitive(5);
      s1.setSubint(1);
      s1.setObject(sup1);
      getStore().save(s1);
      Subclass s2 = new Subclass();
      s2.setPrimitive(6);
      s2.setSubint(2);
      s2.setObject(sup2);
      getStore().save(s2);
      // Select
      List result = getStore().find("find superclass where subclass.object = superclass and subclass.subint = 1");
      Assert.assertEquals(result.size(),1);
   }

   public void testCommonInterfaceclassSameAttributes()
      throws Exception
   {
      // Delete
      removeAll(Vehicle.class);
      // Try to save both
      getStore().save(new Car());
      getStore().save(new Truck());
   }

}


