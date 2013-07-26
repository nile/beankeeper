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
import java.io.*;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;
import org.testng.Assert;
import hu.netmind.beankeeper.query.impl.LazyListImpl;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.common.StoreException;

/**
 * Test for find syntax and functionality.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class SelectTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(SelectTests.class);
   private ArrayList refs = null;
   
   private void createReferrers(int size)
      throws Exception
   {
      // Create referrers
      removeAll(Referrer.class);
      refs = new ArrayList();
      for ( int i=0; i<size; i++ )
      {
         Referrer ref = new Referrer(i);
         refs.add(ref);
         getStore().save(ref);
      }
   }

   public void testBatchSelect()
      throws Exception
   {
      createReferrers(100);
      // Select
      List result = getStore().find("find referrer");
      // Check
      Collections.sort(refs);
      ArrayList resultArrayList = new ArrayList(result);
      Collections.sort(resultArrayList);
      Assert.assertEquals(resultArrayList,refs);
   }

   public void testSimpleSelectPlainAttribute()
      throws Exception
   {
      createReferrers(5);
      // Select
      List result = getStore().find("find referrer where identity=1");
      // Check
      Assert.assertEquals(result.size(),1);
   }

   @Test(groups = { "quick" })
   public void testSimpleSelectSpecifiedAttribute()
      throws Exception
   {
      createReferrers(5);
      // Select
      List result = getStore().find("find referrer where referrer.identity=1");
      // Check
      Assert.assertEquals(result.size(),1);
   }

   public void testSimpleSelectAliasedUnspecifiedAttribute()
      throws Exception
   {
      createReferrers(5);
      // Select
      List result = getStore().find("find r(referrer) where identity=1");
      // Check
      Assert.assertEquals(result.size(),1);
   }

   public void testSimpleSelectAliasedSpecifiedAttribute()
      throws Exception
   {
      createReferrers(5);
      // Select
      List result = getStore().find("find r(referrer) where r.identity=1");
      // Check
      Assert.assertEquals(result.size(),1);
   }

   public void testSimpleReferenceSelect()
      throws Exception
   {
      // Create test setup
      removeAll(Referrer.class);
      Referrer ref1 = new Referrer(1);
      Referrer ref2 = new Referrer(2);
      ref1.setRef(ref2);
      getStore().save(ref1);
      // Select ref1
      List result = getStore().find("find referrer where referrer.ref.identity=2");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),ref1);
      // Select nothing
      result = getStore().find("find referrer where referrer.ref.identity=1");
      Assert.assertEquals(result.size(),0);
   }

   public void testMultipleReferencedSelect()
      throws Exception
   {
      // Create test setup
      removeAll(Referrer.class);
      Referrer ref1 = new Referrer(1);
      Referrer ref2 = new Referrer(2);
      ref1.setRef(ref2);
      ref2.setRef(ref1);
      getStore().save(ref1);
      // Select ref1
      List result = getStore().find("find referrer where referrer.ref.ref.ref.ref.identity=2");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),ref2);
   }

   public void testSimpleObjectSelect()
      throws Exception
   {
      // Create test setup
      removeAll(Referrer.class);
      Referrer ref1 = new Referrer(1);
      Referrer ref2 = new Referrer(2);
      ref1.setRef(ref2);
      ref2.setRef(ref1);
      getStore().save(ref1);
      // Select ref1
      List result = getStore().find("find referrer where referrer.ref=?",new Object[] { ref2 });
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),ref1);
   }

   public void testMultipleTermSelect()
      throws Exception
   {
      // Create test setup
      removeAll(Referrer.class);
      Referrer ref1 = new Referrer(1);
      Referrer ref2 = new Referrer(2);
      ref1.setRef(ref2);
      getStore().save(ref1);
      // Select ref1
      List result = getStore().find("find referrer where identity=1 and referrer.ref.identity=2");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),ref1);
   }

   public void testContainsOperatorOnList()
      throws Exception
   {
      // Create test setup
      removeAll(Book.class);
      // Create
      Book book = new Book("Starship internals","1-3-5-7");
      ArrayList authors = new ArrayList();
      authors.add(new Author("Geordi","LaForge"));
      authors.add(new Author("Data",""));
      authors.add(new Author("Scott","Montgomery"));
      book.setAuthors(authors);
      // Save
      getStore().save(book);
      // Select ref1
      List result = getStore().find("find book where book.authors contains author and author.firstname = 'Geordi'");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),book);
   }

   public void testMapSelection()
      throws Exception
   {
      // Drop
      removeAll(MapHolder.class);
      // Create
      MapHolder mapHolder = new MapHolder();
      Book book = new Book("Book of Bokonon","9");
      HashMap meta = new HashMap();
      meta.put("meta1",new Author("Geordi","LaForge"));
      meta.put("meta2",new Author("Data",""));
      meta.put("meta3",new Author("Scott","Montgomery"));
      meta.put("book",book);
      mapHolder.setMeta(meta);
      // Save
      getStore().save(mapHolder);
      // Select
      List result = getStore().find("find mapholder where mapholder.meta['book'](book)=book and book.title like 'Book%'");
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find mapholder where mapholder.meta['book'](book)=book and book.title like '9'");
      Assert.assertEquals(result.size(),0);
   }

   public void testMapContainsOperator()
      throws Exception
   {
      // Drop
      removeAll(MapHolder.class);
      // Create
      MapHolder mapHolder = new MapHolder();
      Book book = new Book("Book of Bokonon","9");
      HashMap meta = new HashMap();
      meta.put("meta1",new Author("Geordi","LaForge"));
      meta.put("meta2",new Author("Data",""));
      meta.put("meta3",new Author("Scott","Montgomery"));
      meta.put("book",book);
      mapHolder.setMeta(meta);
      // Save
      getStore().save(mapHolder);
      // Select
      List result = getStore().find("find mapholder where mapholder.meta contains book and book.isbn='9'");
      Assert.assertEquals(result.size(),1);
   }

   public void testMapSelectionWithoutClassSepcificer()
      throws Exception
   {
      // Drop
      removeAll(MapHolder.class);
      // Create
      MapHolder mapHolder = new MapHolder();
      Book book = new Book("Book of Bokonon","9");
      HashMap meta = new HashMap();
      meta.put("meta1",new Author("Geordi","LaForge"));
      meta.put("meta2",new Author("Data",""));
      meta.put("meta3",new Author("Scott","Montgomery"));
      meta.put("book",book);
      mapHolder.setMeta(meta);
      // Save
      getStore().save(mapHolder);
      // Select
      List result = getStore().find("find mapholder where mapholder.meta['book']=book and book.title like 'Book%'");
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find mapholder where mapholder.meta['book']=book and book.title like '9'");
      Assert.assertEquals(result.size(),0);
   }
   
   public void testKeywordCaseInsensitiveness()
      throws Exception
   {
      createReferrers(2);
      // Select
      List result = getStore().find("FiNd referrer wHeRe identity=1");
      // Check
      Assert.assertEquals(result.size(),1);
   }

   public void testIdentifierCaseInsensitiveness()
      throws Exception
   {
      createReferrers(2);
      // Select
      List result = getStore().find("find rEfeRReR where iDeNtiTy=1");
      // Check
      Assert.assertEquals(result.size(),1);
   }

   public void testStringCaseSensitiveness()
      throws Exception
   {
      // Create
      removeAll(Book.class);
      Book book = new Book("Book of Bokonon","9");
      getStore().save(book);
      // Select
      List result = getStore().find("find book where title='Book of Bokonon'");
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find book where title='boOK oF BokONon'");
      Assert.assertEquals(result.size(),0);
   }
   
   public void testStringCaseSensitivenessInMaps()
      throws Exception
   {
      // Drop
      removeAll(MapHolder.class);
      // Create
      MapHolder mapHolder = new MapHolder();
      Book book = new Book("Book of Bokonon","9");
      HashMap meta = new HashMap();
      meta.put("book",book);
      mapHolder.setMeta(meta);
      getStore().save(mapHolder);
      // Select
      List result = getStore().find("find mapholder where mapholder.meta['book']=book and book.isbn='9'");
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find mapholder where mapholder.meta['bOoK']=book and book.isbn='9'");
      Assert.assertEquals(result.size(),0);
   }
   
   public void testUnspecifiedSimpleOrderBy()
      throws Exception
   {
      createReferrers(10);
      // Select
      List result = getStore().find("find referrer order by identity");
      // Check
      Assert.assertEquals(result.size(),10);
      for ( int i=0; i<10; i++ )
         Assert.assertEquals(((Referrer) result.get(i)).getIdentity(),i);
   }

   public void testSpecifiedSimpleOrderBy()
      throws Exception
   {
      createReferrers(10);
      // Select
      List result = getStore().find("find referrer order by referrer.identity");
      // Check
      Assert.assertEquals(result.size(),10);
      for ( int i=0; i<10; i++ )
         Assert.assertEquals(((Referrer) result.get(i)).getIdentity(),i);
   }

   public void testSpecifiedAliasedSimpleOrderBy()
      throws Exception
   {
      createReferrers(10);
      // Select
      List result = getStore().find("find r(referrer) order by r.identity");
      // Check
      Assert.assertEquals(result.size(),10);
      for ( int i=0; i<10; i++ )
         Assert.assertEquals(((Referrer) result.get(i)).getIdentity(),i);
   }

   public void testSpecifiedAliasedAscendingSimpleOrderBy()
      throws Exception
   {
      createReferrers(10);
      // Select
      List result = getStore().find("find r(referrer) order by r.identity asc");
      // Check
      Assert.assertEquals(result.size(),10);
      for ( int i=0; i<10; i++ )
         Assert.assertEquals(((Referrer) result.get(i)).getIdentity(),i);
   }

   public void testSpecifiedAliasedDescendingSimpleOrderBy()
      throws Exception
   {
      createReferrers(10);
      // Select
      List result = getStore().find("find r(referrer) order by r.identity desc");
      // Check
      Assert.assertEquals(result.size(),10);
      for ( int i=0; i<10; i++ )
         Assert.assertEquals(((Referrer) result.get(i)).getIdentity(),9-i);
   }

   public void testMultipleOrderBy()
      throws Exception
   {
      removeAll(Book.class);
      Book b1 = new Book("Book of Bokonon","1-2-3-4");
      Book b2 = new Book("Book of Bokonon","4-3-2-1");
      Book b3 = new Book("Starhip design","2-2-2-2");
      Book b4 = new Book("NetMind Persistence tutorial","0-0-0-0");
      // Save
      getStore().save(b1);
      getStore().save(b2);
      getStore().save(b3);
      getStore().save(b4);
      // Get
      List result = getStore().find("find book order by title asc,isbn desc");
      Assert.assertEquals(result.size(),4);
      Assert.assertEquals(result.get(0),b2);
      Assert.assertEquals(result.get(1),b1);
      Assert.assertEquals(result.get(2),b4);
      Assert.assertEquals(result.get(3),b3);
   }

   public void testComplexSelectWithOrdering()
      throws Exception
   {
      removeAll(Book.class);
      ArrayList authors = new ArrayList();
      authors.add(new Author("Tom","Petty"));
      authors.add(new Author("Lynyrd","Skynyrd"));
      Book b1 = new Book("Book of Bokonon","1-2-3-4");
      b1.setAuthors(authors);
      b1.setMainAuthor(new Author("Bokonon Jr.",""));
      Book b2 = new Book("Book of Bokonon","4-3-2-1");
      authors = new ArrayList();
      authors.add(new Author("Tom","Petty"));
      b2.setAuthors(authors);
      b2.setMainAuthor(new Author("Bokonon",""));
      Book b3 = new Book("Starhip design","2-2-2-2");
      Book b4 = new Book("NetMind Persistence tutorial","0-0-0-0");
      // Save
      getStore().save(b1);
      getStore().save(b2);
      getStore().save(b3);
      getStore().save(b4);
      // Get
      List result = getStore().find("find book where (book.authors contains tom(author)) "+
            "and tom.firstname='Tom' and book.authors contains lynyrd(author) and lynyrd.firstname='Lynyrd' and book.mainauthor.firstname like 'Bokonon%'");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),b1);
   }

   public void testNegatedContains()
      throws Exception
   {
      removeAll(Book.class);
      ArrayList authors = new ArrayList();
      authors.add(new Author("Tom","Petty"));
      authors.add(new Author("Lynyrd","Skynyrd"));
      Book b1 = new Book("Book of Bokonon","1-2-3-4");
      b1.setAuthors(authors);
      // Save
      getStore().save(b1);
      // Get
      try
      {
         List result = getStore().find("find book where not book.authors contains author and author.firstname='Tom'");
         Assert.fail("negated contains did not throw exception.");
      } catch ( Exception e ) {
         logger.debug("expected exception",e);
      }
   }
   
   public void testComplexNegatedContains()
      throws Exception
   {
      removeAll(Book.class);
      ArrayList authors = new ArrayList();
      authors.add(new Author("Tom","Petty"));
      authors.add(new Author("Lynyrd","Skynyrd"));
      Book b1 = new Book("Book of Bokonon","1-2-3-4");
      b1.setAuthors(authors);
      // Save
      getStore().save(b1);
      // Get
      try
      {
         List result = getStore().find("find book where not (not (not book.authors contains author and author.firstname='Tom'))");
         Assert.fail("negated contains did not throw exception.");
      } catch ( Exception e ) {
         logger.debug("expected exception",e);
      }
   }

   public void testIterationWithFor()
      throws Exception
   {
      createReferrers(150);
      // Search
      List result = getStore().find("find referrer");
      // Iterate
      for ( int i=0; i<result.size(); i++ )
      {
         Referrer ref = (Referrer) result.get(i);
      }
   }

   public void testIterationWithIterator()
      throws Exception
   {
      createReferrers(150);
      // Search
      List result = getStore().find("find referrer");
      // Iterate
      Iterator iterator = result.iterator();
      while ( iterator.hasNext() )
      {
         Referrer ref = (Referrer) iterator.next();
      }
   }

   public void testEmptyIterationWithIterator()
      throws Exception
   {
      createReferrers(0);
      // Search
      List result = getStore().find("find referrer");
      // Iterate
      Iterator iterator = result.iterator();
      while ( iterator.hasNext() )
      {
         Referrer ref = (Referrer) iterator.next();
      }
   }

   public void testRandomIterations()
      throws Exception
   {
      Referrer ref;
      createReferrers(150);
      // Search
      List result = getStore().find("find referrer");
      Assert.assertEquals(result.size(),150);
      // Iterate forward
      for ( int i=0; i<result.size(); i++ )
         ref = (Referrer) result.get(i);
      // Iterate forward again
      for ( int i=0; i<result.size(); i++ )
         ref = (Referrer) result.get(i);
      // Iterate backward
      for ( int i=result.size(); i>0 ; i-- )
         ref = (Referrer) result.get(i-1);
      // Iterate randomly around
      Random rnd = new Random();
      for ( int i=0; i<50 ; i++ )
         ref = (Referrer) result.get(rnd.nextInt(150));
   }

   public void testDistinctResult()
      throws Exception
   {
      // Clear
      removeAll(Referrer.class);

      // Insert
      getStore().save(new Referrer(1));
      getStore().save(new Referrer(1));

      // Select
      List result = getStore().find("find referrer where referrer.identity=second(referrer).identity");
      Assert.assertEquals(result.size(),2); // We should receive 2 instead of 4
   }

   public void testNonDistinctByteArrays()
      throws Exception
   {
      // Clear
      removeAll(ReferredByteArray.class);

      // Insert
      getStore().save(new ReferredByteArray(1));
      getStore().save(new ReferredByteArray(1));

      // Select
      List result = getStore().find("find referredbytearray where referredbytearray.identity=second(referredbytearray).identity");
      Assert.assertEquals(result.size(),4); // We should receive 4
   }
   
   public void testNonDistinctByteArraysInSubclass()
      throws Exception
   {
      // Clear
      removeAll(ByteArraySuper.class);
      removeAll(ByteArrayClass.class);

      // Insert
      getStore().save(new ByteArrayClass(1));
      getStore().save(new ByteArrayClass(1));

      // Select
      List result = getStore().find("find bytearraysuper where bytearraysuper.identity=second(bytearraysuper).identity");
      Assert.assertEquals(result.size(),4); // We should receive 4
   }

   public void testDistinctOrderBysOnOtherClass()
      throws Exception
   {
      // Clear
      removeAll(Referrer.class);
      removeAll(IdentityStuff.class);

      // Create
      getStore().save(new Referrer(1));
      getStore().save(new Referrer(1));
      IdentityStuff i1 = new IdentityStuff(1,new Author("Adam","Smith"));
      getStore().save(i1);
      IdentityStuff i2 = new IdentityStuff(1,new Author("Cecil","Hue"));
      getStore().save(i2);
      IdentityStuff i3 = new IdentityStuff(1,new Author("Eve","Smith"));
      getStore().save(i3);
      IdentityStuff i4 = new IdentityStuff(1,new Author("Wade","Low"));
      getStore().save(i4);

      // Select
      List result = getStore().find("find identitystuff where identitystuff.identity=referrer.identity order by identitystuff.author.firstName asc");
      Assert.assertEquals(result.size(),4); // Every identity stuff is selected twice, but it should be distinct
      Assert.assertEquals(result.get(0),i1);
      Assert.assertEquals(result.get(1),i2);
      Assert.assertEquals(result.get(2),i3);
      Assert.assertEquals(result.get(3),i4);
   }
   
   public void testListsStayNull()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);

      // Create a book
      getStore().save(new Book("Title","Isbn"));

      // Select
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
      Assert.assertNull( ((Book) result.get(0)).getAuthors() );
   }

   public void testMapsStayNull()
      throws Exception
   {
      // Drop tables
      removeAll(MapHolder.class);

      // Create a book
      getStore().save(new MapHolder());

      // Select
      List result = getStore().find("find mapholder");
      Assert.assertEquals(result.size(),1);
      Assert.assertNull( ((MapHolder) result.get(0)).getMeta() );
   }

   public void testLikeOperator()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);

      // Create a book
      getStore().save(new Book("Title","Isbn"));

      // Select
      List result = getStore().find("find book where title like 'ti%'");
      Assert.assertEquals(result.size(),0);
      result = getStore().find("find book where title like 'Ti%'");
      Assert.assertEquals(result.size(),1);
   }

   public void testCaseInsensitiveLikeOperator()
      throws Exception
   {
      // Drop tables
      removeAll(Book.class);

      // Create a book
      getStore().save(new Book("Title","Isbn"));

      // Select
      List result = getStore().find("find book where title ilike 'ti%'");
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find book where title ilike 'Ti%'");
      Assert.assertEquals(result.size(),1);
   }

   public void testUnderscoreClass()
      throws Exception
   {
      // Drop tables
      removeAll(Under_Score.class);
      
      // Create
      getStore().save(new Under_Score());

      // Select
      List result = getStore().find("find under_score");
      Assert.assertEquals(result.size(),1);
   }

   public void testSameClassName()
      throws Exception
   {
      // Drop tables
      removeAll(Empty.class);
      removeAll(hu.netmind.beankeeper.subpackage.Empty.class);

      // Create
      getStore().save(new Empty());
      getStore().save(new hu.netmind.beankeeper.subpackage.Empty());

      // Selects
      List result = getStore().find("find empty");
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find empty(subpackage.empty)");
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find empty(beankeeper.empty)");
      Assert.assertEquals(result.size(),1);
   }

   public void testNowKeyword()
      throws Exception
   {
      // Drop
      removeAll(Movie.class);

      // Create
      getStore().save(new Movie("Raising Arizona",new Date(),new Date()));

      // Select
      List result = getStore().find("find movie where startdate <= now");
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find movie where enddate > now");
      Assert.assertEquals(result.size(),0);
   }

   public void testSelectSymbolTable()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      removeAll(Author.class);

      // Create
      Book book = new Book("Title","Isbn");
      book.setMainAuthor(new Author("Peter","Jackson"));
      getStore().save(book);

      // Select
      List result = getStore().find("find book where book.mainauthor.firstname='Peter' or book.mainauthor.firstname='Peter'");
      Assert.assertEquals(result.size(),1);
   }

   public void testNullParameters()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Create
      Book book = new Book("Title",null);
      getStore().save(book);
      // Select
      List result = getStore().find("find book where isbn = ?", new Object[] { null });
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),book);
   }

   public void testNullParameters2()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Create
      Book book = new Book("Title","1-2-3-4");
      getStore().save(book);
      // Select
      List result = getStore().find("find book where isbn <> ?", new Object[] { null });
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),book);
   }

   public void testSelectInTransaction()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Create and select
      Book book = new Book("Title","1-2-3-4");
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      List result = null;
      try
      {
         getStore().save(book);
         result = getStore().find("find book");
         Assert.assertEquals(result.size(),1);
         Assert.assertEquals(result.get(0),book);
      } finally {
         tx.commit();
      }
      // List should contain the object still
      ((LazyListImpl) result).refresh();
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),book);
   }

   public void testSelectSingleNotExists()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Test
      Assert.assertNull( getStore().findSingle("find book") );
   }

   public void testSelectSingleExists()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Insert
      Book book = new Book("Title","1-2-3-4");
      getStore().save(book);
      // Test
      Assert.assertEquals(getStore().findSingle("find book"),book);
   }

   public void testSelectSingleMultipleExists()
      throws Exception
   {
      // Drop books
      removeAll(Book.class);
      // Insert
      Book book1 = new Book("Title 1","1-2-3-4");
      getStore().save(book1);
      Book book2 = new Book("Title 2","1-2-3-4");
      getStore().save(book2);
      // Test
      logger.debug("finding a book");
      Assert.assertNotNull( getStore().findSingle("find book") );
   }

   public void testPaging()
      throws Exception
   {
      int bigEnoughSize = 70;
      // Create referrers
      createReferrers(bigEnoughSize);
      // Query
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      List result = getStore().find("find referrer");
      // Iterate forward
      for ( int i=0; i<bigEnoughSize; i++ )
         result.get(i);
      // Iterate backward
      for ( int i=bigEnoughSize; i>0; i-- )
         result.get(i-1);
      tx.commit();
      // Check how many selects ran (1 count + 3 selects, or
      // sometimes, 2 selects, if the cache has enough memory)
      Assert.assertTrue( tx.getStats().getSelectCount()<4 );
   }

   public void testReservedTableOrderBy()
      throws Exception
   {
      // Drop the table
      removeAll(User.class);
      // Create
      User user = new User();
      user.setName("demon");
      getStore().save(user);
      User aUser = new User();
      aUser.setName("root");
      getStore().save(aUser);
      // Do query
      List result = getStore().find("find user order by user.name");
      Assert.assertEquals(result.size(),2);
   }

   public void testMemberListIndependentObjects()
      throws Exception
   {
      // Drop
      removeAll(ListHolder.class);
      // Create with many independent objects
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Book("Independent Book","1"));
      getStore().save(holder);
      // Now check
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      try
      {
         ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
         // Check stats: 1 holder, +2 statements
         Assert.assertEquals(dbHolder.getList().size(),2);
         Assert.assertEquals(tx.getStats().getSelectCount(),4);
      } finally {
         tx.commit();
      }
   }
  
   public void testMemberListDependentObjects()
      throws Exception
   {
      // Drop
      removeAll(ListHolder.class);
      // Create with many independent objects
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new ReferrerSubclass(3,3));
      getStore().save(holder);
      // Now check
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      try
      {
         ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
         // Check stats: 1 holder, +1 statement
         Assert.assertEquals(dbHolder.getList().size(),3);
         Assert.assertEquals(tx.getStats().getSelectCount(),2);
      } finally {
         tx.commit();
      }
   }
   
   public void testMemberListSingleClassObjects()
      throws Exception
   {
      // Drop
      removeAll(ListHolder.class);
      // Create with many independent objects
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Referrer(3));
      getStore().save(holder);
      // Now check
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      try
      {
         ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
         // Check stats: 1 holder, +1 statement
         Assert.assertEquals(dbHolder.getList().size(),3);
         Assert.assertEquals(tx.getStats().getSelectCount(),2);
      } finally {
         tx.commit();
      }
   }

   public void testMemberListAddSuperclassObjects()
      throws Exception
   {
      // Drop
      removeAll(ListHolder.class);
      // Create with many independent objects
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new ReferrerSubclass(1,1));
      holder.getList().add(new ReferrerSubclass(2,2));
      holder.getList().add(new ReferrerSubclass(3,3));
      getStore().save(holder);
      // Now check
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      ListHolder dbHolder = null;
      try
      {
         dbHolder = (ListHolder) getStore().findSingle("find listholder");
         // Check stats: 1 holder, +1 statement
         Assert.assertEquals(dbHolder.getList().size(),3);
         Assert.assertEquals(tx.getStats().getSelectCount(),2);
      } finally {
         tx.commit();
      }
      // Now insert superclass
      dbHolder.getList().add(new Referrer(4));
      getStore().save(dbHolder);
      // Now check
      tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      try
      {
         dbHolder = (ListHolder) getStore().findSingle("find listholder");
         // Check stats: 1 holder, +1 statement
         Assert.assertEquals(dbHolder.getList().size(),4);
         Assert.assertEquals(tx.getStats().getSelectCount(),2);
      } finally {
         tx.commit();
      }
   }
   
   public void testMemberListAddSubclassObjects()
      throws Exception
   {
      // Drop
      removeAll(ListHolder.class);
      // Create with many independent objects
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Referrer(3));
      getStore().save(holder);
      // Now check
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      ListHolder dbHolder = null;
      try
      {
         dbHolder = (ListHolder) getStore().findSingle("find listholder");
         // Check stats: 1 holder, +1 statement
         Assert.assertEquals(dbHolder.getList().size(),3);
         Assert.assertEquals(tx.getStats().getSelectCount(),2);
      } finally {
         tx.commit();
      }
      // Now insert superclass
      dbHolder.getList().add(new ReferrerSubclass(4,4));
      getStore().save(dbHolder);
      // Now check
      tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      try
      {
         dbHolder = (ListHolder) getStore().findSingle("find listholder");
         // Check stats: 1 holder, +1 statement
         Assert.assertEquals(dbHolder.getList().size(),4);
         Assert.assertEquals(tx.getStats().getSelectCount(),2);
      } finally {
         tx.commit();
      }
   }
   
   public void testMemberListAddRemoveIndependentObjects()
      throws Exception
   {
      // Drop
      removeAll(ListHolder.class);
      // Create with many independent objects
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Referrer(3));
      getStore().save(holder);
      // Now check
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      ListHolder dbHolder = null;
      try
      {
         dbHolder = (ListHolder) getStore().findSingle("find listholder");
         // Check stats: 1 holder, +1 statement
         Assert.assertEquals(dbHolder.getList().size(),3);
         Assert.assertEquals(tx.getStats().getSelectCount(),2);
      } finally {
         tx.commit();
      }
      // Now insert independent
      Book book = new Book("Independent Object","1");
      dbHolder.getList().add(book);
      getStore().save(dbHolder);
      // Now check
      tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      try
      {
         dbHolder = (ListHolder) getStore().findSingle("find listholder");
         // Check stats: 1 holder, +2 statement
         Assert.assertEquals(dbHolder.getList().size(),4);
         Assert.assertEquals(tx.getStats().getSelectCount(),4);
      } finally {
         tx.commit();
      }
      // Now remove indep object
      dbHolder.getList().remove(book);
      getStore().save(dbHolder);
      // Add same class
      dbHolder.getList().add(new Referrer(6));
      getStore().save(dbHolder);
      // Now check
      tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      try
      {
         dbHolder = (ListHolder) getStore().findSingle("find listholder");
         // Check stats: 1 holder, +2 statement
         Assert.assertEquals(dbHolder.getList().size(),4);
         Assert.assertEquals(tx.getStats().getSelectCount(),3);
      } finally {
         tx.commit();
      }
   }

   public void testReferencesLoading()
      throws Exception
   {
      // Drop
      removeAll(Referrer.class);
      // Create a list of referrers which refer to a single
      // referrer
      Referrer ref = new Referrer(1);
      getStore().save(ref);
      Referrer refs[] = new Referrer[10];
      for ( int i=0; i<refs.length; i++ )
      {
         refs[i] = new Referrer(10+i);
         refs[i].setRef(ref);
         getStore().save(refs[i]);
      }
      // Get
      List result = getStore().find("find referrer");
      // Check
      Assert.assertEquals(result.size(),11);
      for ( int i=0; i<result.size(); i++ )
      {
         Referrer referrer = (Referrer) result.get(i);
         if ( referrer.getIdentity() == 1 )
            continue;
         Assert.assertEquals(referrer.getRef(),ref);
      }
   }
   
   public void testReferencesLoadingWithBadHashCode()
      throws Exception
   {
      // Drop
      removeAll(BadHashCode.class);
      // Create a list of badhashcodes which refer to a single
      // badhashcode
      BadHashCode ref = new BadHashCode(1);
      getStore().save(ref);
      BadHashCode refs[] = new BadHashCode[10];
      for ( int i=0; i<refs.length; i++ )
      {
         refs[i] = new BadHashCode(10+i);
         refs[i].setRef(ref);
         getStore().save(refs[i]);
      }
      // Get
      List result = getStore().find("find badhashcode");
      // Check
      Assert.assertEquals(result.size(),11);
      for ( int i=0; i<result.size(); i++ )
      {
         BadHashCode badhashcode = (BadHashCode) result.get(i);
         if ( badhashcode.getIdentity() == 1 )
            continue;
         Assert.assertNotNull(badhashcode.getRef());
      }
   }

   public void testListToString()
      throws Exception
   {
      removeAll(Book.class);
      // Create
      Book book = new Book("Starship internals","1-3-5-7");
      getStore().save(book);
      // Select ref1
      List result = getStore().find("find book");
      result.toString();
   }

   public void testSelectPrimitiveAttribute()
      throws Exception
   {
      removeAll(ObjectHolder.class);
      // Create
      ObjectHolder holder = new ObjectHolder();
      holder.setObj(new StringBuffer("String Ni").toString());
      getStore().save(holder);
      // Select
      List result = getStore().find("find objectholder where obj = 'String Ni'");
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find objectholder where obj = ?", new Object[] { "String Ni" });
      Assert.assertEquals(result.size(),1);
   }

   public void testSelectPrimitiveContainerItem()
      throws Exception
   {
      removeAll(SetHolder.class);
      // Create
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new StringBuffer("String Ni").toString());
      getStore().save(holder);
      // Select
      List result = getStore().find("find setholder where set contains 'String Ni'");
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find setholder where set contains ?", new Object[] { "String Ni" });
      Assert.assertEquals(result.size(),1);
   }

   public void testSearchWithId()
      throws Exception
   {
      // Create test setup
      removeAll(Author.class);
      // Create
      Author author = new Author("Stephenson","Neal");
      getStore().save(author);
      long id = author.getPersistenceId();
      // Select 
      List result = null;
      result = getStore().find("find author where persistenceid = "+id);
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find author where persistenceid = ?",new Object[] { new Long(id) });
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find author where author.persistenceid = "+id);
      Assert.assertEquals(result.size(),1);
      result = getStore().find("find author where author.persistenceid = ?",new Object[] { new Long(id) });
      Assert.assertEquals(result.size(),1);
   }
      
   public void testMixedSelect()
      throws Exception
   {
      // Create test setup
      removeAll(Book.class);
      // Create
      Book book = new Book("Starship internals","1-3-5-7");
      book.setMainAuthor(new Author("Geordi","LaForge"));
      // Save
      getStore().save(book);
      // Select
      List result = getStore().find("find book,book.mainauthor.firstName");
      logger.debug("result is: "+result);
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(((Map)result.get(0)).get("object"),book);
      Assert.assertEquals(((Map)result.get(0)).get("firstName"),"Geordi");
   }

   public void testMixedSelectWithAlias()
      throws Exception
   {
      // Create test setup
      removeAll(Book.class);
      // Create
      Book book = new Book("Starship internals","1-3-5-7");
      book.setMainAuthor(new Author("Geordi","LaForge"));
      // Save
      getStore().save(book);
      // Select
      List result = getStore().find("find book,book.mainauthor.firstName authorname");
      logger.debug("result is: "+result);
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(((Map)result.get(0)).get("object"),book);
      Assert.assertEquals(((Map)result.get(0)).get("authorname"),"Geordi");
   }

   public void testInOperatorWithList()
      throws Exception
   {
      // Create test setup
      removeAll(Book.class);
      removeAll(Author.class);
      // Create
      Author author1 = new Author("Geordi","LaForge");
      Author author2 = new Author("Data","");
      Author author3 = new Author("Scott","Montgomery");
      Book book = new Book("Starship internals","1-3-5-7");
      book.setMainAuthor(author1);
      // Save
      getStore().save(book);
      getStore().save(author1);
      getStore().save(author2);
      getStore().save(author3);
      // Create list
      ArrayList authorList = new ArrayList();
      authorList.add(author1);
      authorList.add(author2);
      authorList.add(author3);
      // Select
      List result = getStore().find("find book where book.mainauthor in ?",new Object[] { authorList });
      logger.debug("result is: "+result);
      Assert.assertEquals(result.size(),1);
   }

   public void testSubclassSelects()
      throws Exception
   {
      // Drop
      removeAll(Superclass.class);
      // Create
      getStore().save(new Subclass());
      // Select
      List result = getStore().find("view superclass.primitive");
      for ( int i=0; i<result.size(); i++ )
         result.get(i);
   }

   public void testNullAttributeSelect()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      removeAll(Author.class);
      // Create book with null mainauthor
      getStore().save(new Book("The Death of Null","0"));
      // Run the select
      Assert.assertEquals(getStore().find("find book where book.mainauthor is null").size(),1);
      Assert.assertEquals(getStore().find("find book where book.mainauthor is not null").size(),0);
   }

   public void testRemovedAttributeSelect()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      removeAll(Author.class);
      // Create book with mainauthor
      Author author = new Author("Short","Live");
      Book book = new Book("The Death of Null","0");
      book.setMainAuthor(author);
      getStore().save(book);
      // Now remove mainauthor
      getStore().remove(author);
      // Run the select
      Assert.assertEquals(getStore().find("find book where book.mainauthor is null").size(),1);
      Assert.assertEquals(getStore().find("find book where book.mainauthor is not null").size(),0);
   }

   public void testNullAttributesSelect()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create books with null mainauthor
      getStore().save(new Book("Book","1"));
      getStore().save(new Book("Book","2"));
      // Run the select
      Assert.assertEquals(getStore().find("find book where book.isbn='1' and book2(book).isbn='2' and book.mainauthor=book2.mainauthor").size(),1);
   }

   public void testRemovedAttributesSelect()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create bookS
      Author author1 = new Author("One","Author");
      Book book1 = new Book("Book","1");
      book1.setMainAuthor(author1);
      getStore().save(book1);
      Author author2 = new Author("Two","Author");
      Book book2 = new Book("Book","2");
      book2.setMainAuthor(author2);
      getStore().save(book2);
      // Remove both authors
      getStore().remove(author1);
      getStore().remove(author2);
      // Run the select
      Assert.assertEquals(getStore().find("find book where book.isbn='1' and book2(book).isbn='2' and book.mainauthor=book2.mainauthor").size(),1);
   }

   public void testRemovedAndNullAttributesSelect()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create bookS
      Book book1 = new Book("Book","1");
      getStore().save(book1);
      Author author2 = new Author("Two","Author");
      Book book2 = new Book("Book","2");
      book2.setMainAuthor(author2);
      getStore().save(book2);
      // Remove author
      getStore().remove(author2);
      // Run the select
      Assert.assertEquals(getStore().find("find book where book.isbn='1' and book2(book).isbn='2' and book.mainauthor=book2.mainauthor").size(),1);
   }

   public void testRemovedObjectIdSelect()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create book
      Author author = new Author("Joe","Author");
      Book book = new Book("Book","X");
      book.setMainAuthor(author);
      getStore().save(book);
      // Remove author
      getStore().remove(author);
      // Run the select
      Assert.assertEquals(getStore().find("find book where book.mainauthor=?",new Object[] { author }).size(),0);
      Assert.assertEquals(getStore().find("find book where book.mainauthor=?",new Object[] { null }).size(),1);
   }

   public void testRemovedObjectNonstorableAttributeSelect()
      throws Exception
   {
      // Drop
      removeAll(ObjectHolder.class);
      // Create book
      Author author = new Author("Joe","Author");
      ObjectHolder holder = new ObjectHolder();
      holder.setObj(author);
      getStore().save(holder);
      // Remove author
      getStore().remove(author);
      // Run the select
      Assert.assertEquals(getStore().find("find objectholder where objectholder.obj(author)=?",new Object[] { author }).size(),0);
   }

   public void testSyntaxError()
   {
      // Test whether wrong syntax throws error
      try
      {
         getStore().find("fin book");
         Assert.fail("test should have caused syntax error");
      } catch ( StoreException e ) {
         // Ok
         logger.debug("expected exception",e);
      }
   }

   public void testUnfinishedSyntax()
   {
      // Test whether it's an error to have an incomplete sentence
      try
      {
         getStore().find("find");
         Assert.fail("test should have caused syntax error");
      } catch ( StoreException e ) {
         // Ok
         logger.debug("expected exception",e);
      }
   }

   public void testSuperfluousSyntax()
   {
      // Test whether it's an error to have extra characters after
      // valid syntax
      try
      {
         getStore().find("find book order by book.title super");
         Assert.fail("test should have caused syntax error");
      } catch ( StoreException e ) {
         // Ok
         logger.debug("expected exception",e);
      }
   }

   public void testResultListSerializableConcept()
      throws Exception
   {
      // Create books
      removeAll(Book.class);
      for ( int i=0; i<6; i++ )
         getStore().save(new Book("Learning Java 1."+i,""+i));
      // Get the result list
      List result = getStore().find("find book");
      // Now serialize
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
      objectOut.writeObject(result);
      objectOut.close();
      // Deserialize
      ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
      ObjectInputStream objectIn = new ObjectInputStream(byteIn);
      List deList = (List) objectIn.readObject();
      // Test object
      Assert.assertEquals(6,deList.size());
      for ( int i=0; i<6; i++ )
         Assert.assertEquals(""+i,((Book) deList.get(i)).getIsbn());
   }

   public void testResultListSerializableComplex()
      throws Exception
   {
      // Create mutliple objects
      removeAll(Object.class);
      for ( int i=0; i<20; i++ )
         getStore().save(new Book("Learning Java 1."+i,""+i));
      for ( int i=0; i<20; i++ )
         getStore().save(new Referrer(i));
      for ( int i=0; i<20; i++ )
         getStore().save(new User());
      for ( int i=0; i<20; i++ )
         getStore().save(new MapHolder());
      // Get the result list for all objects
      List result = getStore().find("find object");
      Assert.assertEquals(result.size(),80);
      // Now serialize
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
      objectOut.writeObject(result);
      objectOut.close();
      // Close the store, let's pretend we're outside it
      tearDownStore();
      try
      {
         // Deserialize
         ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
         ObjectInputStream objectIn = new ObjectInputStream(byteIn);
         List deList = (List) objectIn.readObject();
         // Test object
         Assert.assertEquals(deList.size(),80);
         Assert.assertEquals(getCount(deList,Book.class),20);
         Assert.assertEquals(getCount(deList,Referrer.class),20);
         Assert.assertEquals(getCount(deList,User.class),20);
         Assert.assertEquals(getCount(deList,MapHolder.class),20);
      } finally {
         // Setup store for next tests
         setUpStore();
      }
   }

   public void testContainsString()
      throws Exception
   {
      // Create test setup
      removeAll(Task.class);
      // Create tasks
      Project bk = new Project("BeanKeeper");
      getStore().save(bk);
      getStore().save(new Task(bk,"Test",new String[] { "bk","test" }));
      getStore().save(new Task(bk,"Implement",new String[] { "bk","implement" }));
      getStore().save(new Task(bk,"Refactor",new String[] { "bk","refactor" }));
      Project ex = new Project("Exorcist");
      getStore().save(ex);
      getStore().save(new Task(ex,"Test",new String[] { "ex","test"}));
      // Select in project
      List result = getStore().find("find task where project = ? and (tags contains ? or tags contains ?)",
            new Object[] { bk, "test", "develop" });
      Assert.assertEquals(result.size(),1);
      // Cross select
      List result2 = getStore().find("find task where tags contains ? and (project.name = ? or project.name = ?)",
            new Object[] { "test", "BeanKeeper", "Exorcist" });
      Assert.assertEquals(result2.size(),2);
   }

   public void testInPrimitiveCollection()
      throws Exception
   {
      // Remove all books
      removeAll(Book.class);
      // Create books
      getStore().save(new Book("Title 1","1"));
      getStore().save(new Book("Title 2","2"));
      getStore().save(new Book("Title 3","3"));
      // Select
      List isbns = new ArrayList();
      isbns.add("1");
      isbns.add("2");
      List result = getStore().find("find book where isbn in ?",new Object[] { isbns });
      Assert.assertEquals(result.size(),2);
   }

}

