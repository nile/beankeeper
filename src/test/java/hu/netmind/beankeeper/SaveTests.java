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
import java.io.File;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;
import org.testng.Assert;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.query.impl.LazyListImpl;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;

/**
 * Save and load tests. These tests mainly look at saving and restoring
 * objects.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class SaveTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(SaveTests.class);

   public void testPrimitiveTypesRandom()
      throws Exception
   {
      // Drop
      removeAll(Primitives.class);
      // Create
      Primitives p = new Primitives();
      p.randomize();
      getStore().save(p);
      // Get back
      List primitives = getStore().find("find primitives");
      // Check stuff
      Assert.assertEquals(primitives.size(),1);
      Assert.assertEquals(primitives.get(0),p);
   }

   public void testPrimitiveTypesMaximums()
      throws Exception
   {
      // Drop
      removeAll(Primitives.class);
      // Create
      Primitives p = new Primitives();
      p.maximize();
      getStore().save(p);
      // Get back
      List primitives = getStore().find("find primitives");
      // Check stuff
      Assert.assertEquals(primitives.size(),1);
      Assert.assertEquals(primitives.get(0),p);
   }
   
   public void testPrimitiveTypeNulls()
      throws Exception
   {
      // Drop
      removeAll(Primitives.class);
      // Create
      Primitives p = new Primitives();
      p.maximize();
      getStore().save(p);
      p.nullize();
      logger.debug("saving the second time");
      getStore().save(p);
      logger.debug("second time save completed");
      // Get back
      List primitives = getStore().find("find primitives");
      // Check stuff
      Assert.assertEquals(primitives.size(),1);
      Assert.assertEquals(primitives.get(0),p);
   }
   
   public void testPrimitiveMix()
      throws Exception
   {
      // Drop
      removeAll(Primitives.class);
      // Create
      Primitives p = new Primitives();
      p.minimize();
      getStore().save(p);
      p.maximize();
      getStore().save(p);
      p.nullize();
      getStore().save(p);
      // Get back
      List primitives = getStore().find("find primitives");
      // Check stuff
      Assert.assertEquals(primitives.size(),1);
      Assert.assertEquals(primitives.get(0),p);
   }
   
   public void testPrimitiveTypesMinimums()
      throws Exception
   {
      // Drop
      removeAll(Primitives.class);
      // Create
      Primitives p = new Primitives();
      p.minimize();
      getStore().save(p);
      // Get back
      List primitives = getStore().find("find primitives");
      // Check stuff
      Assert.assertEquals(primitives.size(),1);
      Assert.assertEquals(primitives.get(0),p);
   }
   
   public void testPrimitiveTypesNulls()
      throws Exception
   {
      // Drop
      removeAll(Primitives.class);
      // Create
      Primitives p = new Primitives();
      p.minimize();
      getStore().save(p);
      // Get back
      List primitives = getStore().find("find primitives");
      // Check stuff
      Assert.assertEquals(primitives.size(),1);
      Assert.assertEquals(primitives.get(0),p);
   }

   public void testDisregardTransient()
      throws Exception
   {
      // Drop
      removeAll(TransientAttrObject.class);
      // Insert
      TransientAttrObject t = new TransientAttrObject("test",1);
      t.setTrans("transient field");
      getStore().save(t);
      // Select
      List result = getStore().find("find transientattrobject");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(t,result.get(0));
      Assert.assertNull(((TransientAttrObject) result.get(0)).getTrans());
   }

   public void testObjectType()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create entry
      Book book = new Book("Object test object","5-6-7-8");
      book.setMainAuthor(new Author("John","Doe"));
      // Save
      getStore().save(book);
      // Recall
      List result = getStore().find("find book");
      // Test
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),book);
      Assert.assertEquals(((Book) result.get(0)).getMainAuthor(),book.getMainAuthor());
   }

   public void testObjectTypeNulls()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create entry
      Book book = new Book("Object test object","5-6-7-8");
      book.setMainAuthor(null);
      // Save
      getStore().save(book);
      // Recall
      List result = getStore().find("find book");
      // Test
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),book);
      Assert.assertNull(book.getMainAuthor());
   }

   public void testObjectSelfReference()
      throws Exception
   {
      // Drop
      removeAll(Referrer.class);
      // Create self-referencing object
      Referrer ref = new Referrer(1);
      ref.setRef(ref);
      // Save
      getStore().save(ref);
      // Recall
      List result = getStore().find("find referrer");
      // Test
      Assert.assertEquals(result.size(),1);
      Referrer resultRef = (Referrer) result.get(0);
      Assert.assertEquals(resultRef,ref);
      Assert.assertEquals(resultRef.getRef(),ref);
      Assert.assertEquals(resultRef.getRef(),ref.getRef());
      Assert.assertTrue(resultRef==resultRef.getRef());
   }

   public void testObjectCircularReference()
      throws Exception
   {
      // Drop
      removeAll(Referrer.class);
      // Create self-referencing object
      Referrer ref1 = new Referrer(1);
      Referrer ref2 = new Referrer(2);
      ref1.setRef(ref2);
      ref2.setRef(ref1);
      // Save
      getStore().save(ref1);
      // Recall
      List result = getStore().find("find referrer where referrer.identity=1");
      // Test
      Assert.assertEquals(result.size(),1);
      Referrer resultRef = (Referrer) result.get(0);
      Assert.assertEquals(resultRef,ref1);
      Assert.assertEquals(resultRef.getRef(),ref2);
      Assert.assertEquals(resultRef.getRef().getRef(),ref1);
      Assert.assertTrue(resultRef==resultRef.getRef().getRef());
   }

   public void testObjectSuperReference()
      throws Exception
   {
      // Drop
      removeAll(Referrer.class);
      removeAll(ReferrerSubclass.class);
      // Create self-referencing object
      Referrer ref = new Referrer(1);
      ReferrerSubclass sub = new ReferrerSubclass(2);
      ref.setRef(sub);
      // Save
      getStore().save(ref);
      // Recall
      List result = getStore().find("find referrer where identity<>2");
      // Test
      Assert.assertEquals(result.size(),1);
      Referrer resultRef = (Referrer) result.get(0);
      Assert.assertEquals(resultRef,ref);
      Assert.assertEquals(resultRef.getRef(),sub);
      Assert.assertTrue(resultRef.getRef() instanceof ReferrerSubclass);
   }

   public void testListObjects()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      removeAll(Author.class);
      // Create
      Book book = new Book("Starship internals","1-3-5-7");
      ArrayList authors = new ArrayList();
      authors.add(new Author("Geordi","LaForge"));
      authors.add(new Author("Data",""));
      authors.add(new Author("Scott","Montgomery"));
      book.setAuthors(authors);
      // Save
      getStore().save(book);
      // Recall
      List result = getStore().find("find book");
      // Check
      Assert.assertEquals(result.size(),1);
      Book resultBook = (Book) result.get(0);
      Assert.assertEquals(resultBook,book);
      Assert.assertEquals(resultBook.getAuthors().size(),book.getAuthors().size());
      Assert.assertEquals(book.getAuthors(),resultBook.getAuthors());
   }

   public void testMapObjects()
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
      // Recall
      List result = getStore().find("find mapholder");
      // Check
      Assert.assertEquals(result.size(),1);
      MapHolder resultHolder = (MapHolder) result.get(0);
      Assert.assertEquals(resultHolder.getMeta(),mapHolder.getMeta());
   }

   public void testPrimitiveTypesModify()
      throws Exception
   {
      // Drop
      removeAll(Primitives.class);
      // Create
      Primitives p = new Primitives();
      // Randomize
      p.randomize();
      getStore().save(p);
      // Get back
      List primitives = getStore().find("find primitives");
      // Check stuff
      Assert.assertEquals(primitives.size(),1);
      Assert.assertEquals(primitives.get(0),p);
      // Minimize
      p.minimize();
      getStore().save(p);
      // Get back
      primitives = getStore().find("find primitives");
      // Check stuff
      Assert.assertEquals(primitives.size(),1);
      Assert.assertEquals(primitives.get(0),p);
      // Maximize
      p.maximize();
      getStore().save(p);
      // Get back
      primitives = getStore().find("find primitives");
      // Check stuff
      Assert.assertEquals(primitives.size(),1);
      Assert.assertEquals(primitives.get(0),p);
   }
   
   public void testModifyListObjects()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      removeAll(Author.class);
      // Create
      Book book = new Book("Starship internals","1-3-5-7");
      ArrayList authors = new ArrayList();
      Author geordi = new Author("Geordi","LaForge");
      authors.add(geordi);
      authors.add(new Author("Data",""));
      authors.add(new Author("Scott","Montgomery"));
      book.setAuthors(authors);
      // Save
      getStore().save(book);
      // Recall
      List result = getStore().find("find book");
      // Check
      Assert.assertEquals(result.size(),1);
      Book resultBook = (Book) result.get(0);
      Assert.assertEquals(resultBook,book);
      Assert.assertEquals(resultBook.getAuthors().size(),book.getAuthors().size());
      Assert.assertTrue(resultBook.getAuthors().equals(book.getAuthors()));
      
      // Remove an item from list
      book.getAuthors().remove(geordi);
      // Save
      getStore().save(book);
      // Recall
      result = getStore().find("find book");
      // Check
      Assert.assertEquals(result.size(),1);
      resultBook = (Book) result.get(0);
      Assert.assertEquals(resultBook,book);
      Assert.assertEquals(resultBook.getAuthors().size(),book.getAuthors().size());
      Assert.assertTrue(resultBook.getAuthors().equals(book.getAuthors()));
      
      // Add two items to list
      book.getAuthors().add(geordi);
      book.getAuthors().add(new Author("Sulu","Hikaru"));
      // Save
      getStore().save(book);
      // Recall
      result = getStore().find("find book");
      // Check
      Assert.assertEquals(result.size(),1);
      resultBook = (Book) result.get(0);
      Assert.assertEquals(resultBook,book);
      Assert.assertEquals(resultBook.getAuthors().size(),book.getAuthors().size());
      Assert.assertTrue(resultBook.getAuthors().equals(book.getAuthors()));

      // Empty the list
      book.getAuthors().clear();
      // Save
      getStore().save(book);
      // Recall
      result = getStore().find("find book");
      // Check
      Assert.assertEquals(result.size(),1);
      resultBook = (Book) result.get(0);
      Assert.assertEquals(resultBook,book);
      Assert.assertEquals(book.getAuthors().size(),0);
   }

   public void testModifyMapObjects()
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
      logger.debug("test saving 1...");
      getStore().save(mapHolder);
      logger.debug("test saving 1 done.");
      // Recall
      List result = getStore().find("find mapholder");
      // Check
      Assert.assertEquals(result.size(),1);
      MapHolder resultHolder = (MapHolder) result.get(0);
      logger.debug("test checking equality 1...");
      Assert.assertEquals(resultHolder.getMeta(),mapHolder.getMeta());
      logger.debug("test checking equality 1 done.");

      // Remove an item
      mapHolder.getMeta().remove("meta1");
      // Save
      getStore().save(mapHolder);
      // Recall
      result = getStore().find("find mapholder");
      // Check
      Assert.assertEquals(result.size(),1);
      resultHolder = (MapHolder) result.get(0);
      Assert.assertEquals(resultHolder.getMeta(),mapHolder.getMeta());

      // Add two items
      mapHolder.getMeta().put("meta1",new Author("Geordi","LaForge2"));
      mapHolder.getMeta().put("referrer",new Referrer(1));
      // Save
      getStore().save(mapHolder);
      // Recall
      result = getStore().find("find mapholder");
      // Check
      Assert.assertEquals(result.size(),1);
      resultHolder = (MapHolder) result.get(0);
      Assert.assertEquals(resultHolder.getMeta(),mapHolder.getMeta());

      // Empty the map
      meta = new HashMap();
      mapHolder.setMeta(meta);
      // Save
      getStore().save(mapHolder);
      // Recall
      result = getStore().find("find mapholder");
      // Check
      Assert.assertEquals(result.size(),1);
      resultHolder = (MapHolder) result.get(0);
      Assert.assertEquals(mapHolder.getMeta().size(),0);
   }

   public void testReservedReverseAttributeSave()
      throws Exception
   {
      // Create and save
      try
      {
         getStore().save(new ReverseAttrObject());
         Assert.fail("should not be allowed to save an object with an attribute in the reverse map");
      } catch ( StoreException e ) {
         // All ok
         logger.debug("expected exception",e);
      }
   }

   public void testReplaceMapValue()
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
      // Recall
      List result = getStore().find("find mapholder");
      // Check
      Assert.assertEquals(result.size(),1);
      MapHolder resultHolder = (MapHolder) result.get(0);
      Assert.assertEquals(resultHolder.getMeta(),mapHolder.getMeta());

      // Change an item
      mapHolder.getMeta().put("meta1",new Author("Sulu","Hikaru"));
      // Save
      getStore().save(mapHolder);
      // Recall
      result = getStore().find("find mapholder");
      // Check
      Assert.assertEquals(result.size(),1);
      resultHolder = (MapHolder) result.get(0);
      Assert.assertEquals(resultHolder.getMeta(),mapHolder.getMeta());
   }

   public void testPersistenceIdAccess()
      throws Exception
   {
      // Drop table
      removeAll(Author.class);

      // Create
      getStore().save(new Author("Frank","Herbert"));

      // Check
      List result = getStore().find("find author");
      Assert.assertEquals(result.size(),1);
      Author a = (Author) result.get(0);
      Assert.assertTrue(0!=a.getPersistenceId());

      // Check if save works (it should not)
      a.setPersistenceId(0);
      getStore().save(a);
      Assert.assertTrue(0!=a.getPersistenceId());
      a.setPersistenceId(a.getPersistenceId()+1);
      
      // Reload
      result = getStore().find("find author");
      Author b = (Author) result.get(0);
      Assert.assertEquals(b.getPersistenceId(),a.getPersistenceId()-1);
   }

   public void testPersistenceIdSelect()
      throws Exception
   {
      // Drop table
      removeAll(Author.class);

      // Create
      Author a = new Author("Frank","Herbert");
      getStore().save(a);
      Assert.assertTrue(0!=a.getPersistenceId());

      // Check
      List result = getStore().find("find author where persistenceid="+a.getPersistenceId());
      Assert.assertEquals(result.size(),1);
   }

   public void testReservedAttributeNameRename()
      throws Exception
   {
      // Drop table
      removeAll(NameTester.class);

      // Create
      NameTester n = new NameTester();
      n.setSelect("select");
      getStore().save(n);

      // Check
      List result = getStore().find("find nametester");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(((NameTester) result.get(0)).getSelect(),"select");
      
      // Check query language
      result = getStore().find("find nametester where seLEct = 'select'");
      Assert.assertEquals(result.size(),1);
   }

   public void testNullInList()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      removeAll(Author.class);
      // Create
      Book book = new Book("Starship internals","1-3-5-7");
      ArrayList authors = new ArrayList();
      authors.add(null);
      book.setAuthors(authors);
      // Save
      try
      {
         getStore().save(book);
         Assert.fail("null value was allowed in list");
      } catch ( StoreException e ) {
         // Ok, it threw exception
         logger.debug("expected exception",e);
      }
   }

   public void testNullMapObjects()
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
      meta.put("metanull",null);
      meta.put("metanullagain",null);
      meta.put("book",book);
      mapHolder.setMeta(meta);
      // Save
      try
      {
         getStore().save(mapHolder);
         Assert.fail("null value was allowed in map");
      } catch ( StoreException e ) {
         // Ok, it threw exception
         logger.debug("expected exception",e);
      }
   }

   public void testNullMapKeys()
      throws Exception
   {
      // Drop
      removeAll(MapHolder.class);
      // Create
      MapHolder mapHolder = new MapHolder();
      Book book = new Book("Book of Bokonon","9");
      HashMap meta = new HashMap();
      meta.put(null,new Author("Geordi","LaForge"));
      mapHolder.setMeta(meta);
      // Save
      try
      {
         getStore().save(mapHolder);
         Assert.fail("null values in map key was allowed, it should not be");
      } catch ( StoreException e ) {
         // Good
         logger.debug("expected exception",e);
      }
   }

   public void testMultipleModificationsSameObjectInTransaction()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create
      Book book = new Book("Starship internals","1-3-5-7");
      getStore().save(book);
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      book.setTitle("Another Book");
      getStore().save(book);
      book.setIsbn("1-1-1-1");
      getStore().save(book);
      book.setTitle("Another Book 2");
      getStore().save(book);
      tx.commit();
      // Check
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),book);
   }

   public void testRemoveObjectContainedInResult()
      throws Exception
   {
      // Remove an object contained in a result list in a transaction
      // after the object has been inserted. This tests that the remove's
      // overwrite of persistence_txid will not affect older lists.
      
      // Drop
      removeAll(Book.class);
      // Create and check
      Book book = new Book("Starship internals","1-3-5-7");
      getStore().save(book);
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),book);
      // Remove
      getStore().remove(book);
      // Check again
      ((LazyListImpl)result).refresh();
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),book);
   }

   public void testDefaultConstructor()
      throws Exception
   {
      // try to insert
      try
      {
         getStore().save(new NoDefault(1));
         Assert.fail("library saved an object with no default constructor, it shouldn't");
      } catch ( StoreException e ) {
         // ok
         logger.debug("expected exception",e);
      }
   }
   
   public void testModifyListObjectsInPlace()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      removeAll(Author.class);
      // Create
      Book book = new Book("Starship internals","1-3-5-7");
      ArrayList authors = new ArrayList();
      authors.add(new Author("Geordi","LaForge"));
      authors.add(new Author("Data",""));
      book.setAuthors(authors);
      // Save
      getStore().save(book);
      // Recall
      Book resultBook = (Book) getStore().findSingle("find book");
      Assert.assertEquals(resultBook.getAuthors().size(),2);
      resultBook.getAuthors().add(new Author("Scott","Montgomery"));
      Assert.assertEquals(resultBook.getAuthors().size(),3);
      getStore().save(resultBook);
      // Recall
      Book resultBook2 = (Book) getStore().findSingle("find book");
      Assert.assertEquals(resultBook2.getAuthors().size(),3);
   }

   public void testInnerClasses()
   {
      try
      {
         getStore().save(new Inner());
         Assert.fail("could save object of inner class, but shouldn't");
      } catch ( StoreException e ) {
         // All OK
         logger.debug("expected exception",e);
      }
   }

   public void testAnonymousClasses()
   {
      try
      {
         getStore().save(new Object() { public void nop() {} });
         Assert.fail("could save anonymous class object, but shouldn't");
      } catch ( StoreException e ) {
         // All OK
         logger.debug("expected exception",e);
      }
   }

   public class Inner
   {
      private String field;
   }

   public void testListEmptyingWithNewStore()
      throws Exception
   {
      // Delete all books
      removeAll(Book.class);

      // First, insert a book
      Book originalBook = new Book("New","1111");
      ArrayList authors = new ArrayList();
      authors.add(new Author("Old","Author"));
      originalBook.setAuthors(authors);
      getStore().save(originalBook);

      // Now terminate store and allocate new one
      tearDownStore();
      setUpStore();

      // Now select and try to empty the book's authors
      Book copyBook = (Book) getStore().findSingle("find book");
      copyBook.setAuthors(new ArrayList());
      getStore().save(copyBook);

      // Check
      Book dbBook = (Book) getStore().findSingle("find book");
      Assert.assertEquals(dbBook.getAuthors().size(),0);
   }

   public void testAttributeNullingWithNewStore()
      throws Exception
   {
      // Delete all books
      removeAll(Book.class);

      // First, insert a book
      Book originalBook = new Book("New","1111");
      getStore().save(originalBook);

      // Now terminate store and allocate new one
      tearDownStore();
      setUpStore();

      // Now select and try to empty the book's authors
      Book copyBook = (Book) getStore().findSingle("find book");
      copyBook.setIsbn(null);
      getStore().save(copyBook);

      // Check
      Book dbBook = (Book) getStore().findSingle("find book");
      Assert.assertEquals(dbBook,copyBook);
   }

   public void testAllAttributesAreSaved()
      throws Exception
   {
      // Delete all books
      removeAll(Book.class);
      // Insert a book
      Book first = new Book("First Title","1");
      getStore().save(first);
      // Select the book, and modify
      Book second = (Book) getStore().findSingle("find book");
      second.setTitle("Second Title");
      getStore().save(second);
      // Modify the original object
      first.setIsbn("2");
      getStore().save(first); // All attributes are saved
      // Check
      Book dbBook = (Book) getStore().findSingle("find book");
      Assert.assertEquals(dbBook,first);
   }

   public void testNothingChanged()
      throws Exception
   {
      // Delete all books
      removeAll(Book.class);
      // Insert a book
      Book first = new Book("First Title","1");
      getStore().save(first);
      // Select the book, and do not modify
      Book second = (Book) getStore().findSingle("find book");
      Assert.assertEquals(second,first);
      getStore().save(second);
      // Check
      Book dbBook = (Book) getStore().findSingle("find book");
      Assert.assertEquals(dbBook,second);
   }

   public void testListReferencedInSave()
      throws Exception
   {
      // Delete all books
      removeAll(ListHolder.class);
      // Insert a book
      ListHolder list1 = new ListHolder();
      ArrayList list1content = new ArrayList();
      list1.setList(list1content);
      getStore().save(list1);
      // Select, and update
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      ListHolder holder2 = new ListHolder();
      holder2.setList(dbHolder.getList());
      Author gibson = new Author("Gibson","William");
      dbHolder.getList().add(gibson);
      dbHolder.getList().add(holder2);
      getStore().save(dbHolder);
      // Now select holder 2, and check what list items it has
      ListHolder dbHolder2 = (ListHolder) getStore().findSingle("find listholder where listholder=?",new Object[] { holder2 });
      // The list should contain itself, and gibson too.
      Assert.assertNotNull(dbHolder2);
      Assert.assertEquals(dbHolder2.getList().size(),2);
   }

   public void testReservedTableName()
      throws Exception
   {
      // Drop the table
      removeAll(User.class);
      // Create
      User user = new User();
      user.setName("demon");
      getStore().save(user);
      // Do query
      Assert.assertEquals(getStore().find("find user").size(),1);
   }

   public void testPrimitiveObjectSaves()
      throws Exception
   {
      // Drop the table
      removeAll(ObjectHolder.class);
      // Create
      ObjectHolder holder = new ObjectHolder();
      holder.setObj("I am Primitive");
      getStore().save(holder);
      // Do query
      ObjectHolder result = (ObjectHolder) getStore().findSingle("find objectholder");
      Assert.assertEquals(result,holder);
   }
   
   public void testReservedAttributeSave()
      throws Exception
   {
      // Drop
      removeAll(User.class);
      // Create and save
      User user = new User();
      user.setPassword("xxx");
      user.setPassword2("yyy");
      getStore().save(user);
      // Check
      User dbUser = (User) getStore().findSingle("find user");
      Assert.assertEquals(dbUser.getPassword(),"xxx");
      Assert.assertEquals(dbUser.getPassword2(),"yyy");
   }

   public void testNonchangedSaveTwice()
      throws Exception
   {
      // Drop book
      removeAll(Book.class);
      // Create book with many attributes
      Book book = new Book("The Book of the Unchanged","1-2-3-4");
      Author author = new Author("John","Derek");
      book.setMainAuthor(author);
      ArrayList authors = new ArrayList();
      authors.add(author);
      book.setAuthors(authors);
      getStore().save(book);
      // Save again, and look whether something was done
      logger.debug("insert finished, commencing with save");
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      getStore().save(book);
      tx.commit();
      // Test what happened
      Assert.assertEquals(tx.getStats().getInsertCount()+tx.getStats().getUpdateCount(),0);
   }
  
   public void testNonchangedSaveReload()
      throws Exception
   {
      // Drop book
      removeAll(Book.class);
      // Create book with many attributes
      Book book = new Book("The Book of the Unchanged","1-2-3-4");
      Author author = new Author("John","Derek");
      book.setMainAuthor(author);
      ArrayList authors = new ArrayList();
      authors.add(author);
      book.setAuthors(authors);
      getStore().save(book);
      // Reload
      Book dbBook = (Book) getStore().findSingle("find book");
      // Save again, and look whether something was done
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      getStore().save(dbBook);
      tx.commit();
      // Test what happened
      Assert.assertEquals(tx.getStats().getInsertCount()+tx.getStats().getUpdateCount(),0);
   }
   
   public void testNonstorableAbstractSuperclassWithNoDefaultConstructor()
      throws Exception
   {
      // Try to save an object with an abstract superclass, which
      // is non-storable, and has no default constructor
      getStore().save(new Car());
   }
      
   public void testAbstractSuperclassWithNoDefaultConstructor()
      throws Exception
   {
      // Try to save an object with an abstract superclass, which
      // is storable, and has no default constructor
      getStore().save(new Swallow());
   }

   public void testSaveReattached()
      throws Exception
   {
      // Drop
      removeAll(Author.class);
      // Create one author
      Author original = new Author("Neal","Stephenson");
      getStore().save(original);
      Assert.assertFalse(original.getPersistenceId()==0);
      // Create another one, which re-attaches
      Author copy = new Author();
      copy.setPersistenceId(original.getPersistenceId());
      copy.setFirstName("O'Neill");
      copy.setLastName("Stephenson");
      getStore().save(copy);
      // The copy should not be another instance
      List authors = getStore().find("find author");
      Assert.assertEquals(authors.size(),1);
      Author result = (Author) authors.get(0);
      Assert.assertEquals(result,copy);
   }

   public void testRemoveReattached()
      throws Exception
   {
      // Drop
      removeAll(Author.class);
      // Create one author
      Author original = new Author("Neal","Stephenson");
      getStore().save(original);
      Assert.assertFalse(original.getPersistenceId()==0);
      // Remove it with a copy
      Author copy = new Author();
      copy.setPersistenceId(original.getPersistenceId());
      getStore().remove(copy);
      // Test
      Assert.assertEquals(getStore().find("find author").size(),0);
   }

   public void testSimpleTypeHandler()
      throws Exception
   {
      // Register handler
      getStore().getTypeHandlerTracker().registerHandler(File.class, new FileSimpleTypeHandler());
      // Clear all
      removeAll(FileObject.class);
      // Save an object which got one file field
      FileObject fileObject = new FileObject("/tmp");
      getStore().save(fileObject);
      // Check whether it can be read out
      FileObject readObject = (FileObject) getStore().findSingle("find fileobject");
      Assert.assertEquals(readObject.getFile(), fileObject.getFile());
      // Read attributes only in view select
      Map viewObject = (Map) getStore().findSingle("view fileobject.file");
      Assert.assertEquals(viewObject.get("file"),"/tmp");
   }
      
}

