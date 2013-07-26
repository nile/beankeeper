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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Collections;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;
import org.testng.Assert;
import hu.netmind.beankeeper.object.PersistenceMetaData;

/**
 * Test the historical functions if library.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class HistoricalTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(HistoricalTests.class);

   public void testPrimitiveHistory()
      throws Exception
   {
      // Drop
      removeAll(Primitives.class);
      // Create
      Primitives p = new Primitives();
      p.minimize();
      getStore().save(p);
      Date minimumDate = new Date();
      synchronized ( this )
      {
         // We must wait so the modification does not
         // commit in the same millisecond as minimumdate
         wait(100);
      }
      // Update entry
      p.maximize();
      getStore().save(p);
      // Get back
      List primitives = getStore().find("find primitives at ?", new Object[] { minimumDate });
      // Check stuff
      Assert.assertEquals(primitives.size(),1);
      p.minimize();
      Assert.assertEquals(primitives.get(0),p);
   }

   public void testMemberLists()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      removeAll(Author.class);
      // Create
      Book book = new Book("Starship internals","1-3-5-7");
      ArrayList authors1 = new ArrayList();
      authors1.add(new Author("Geordi","LaForge"));
      authors1.add(new Author("Data",""));
      authors1.add(new Author("Scott","Montgomery"));
      book.setAuthors(authors1);
      // Save
      getStore().save(book);
      Date firstDate = new Date();
      synchronized ( this )
      {
         // We must wait so the modification does not
         // commit in the same millisecond as minimumdate
         wait(100);
      }
      // Modify
      ArrayList authors2 = new ArrayList();
      authors2.add(new Author("Torres","B'Elanna")); // Ok, I had to actually look this up, who watches Voyager anyway..
      book.setAuthors(authors2);
      getStore().save(book);
      // Recall
      List result = getStore().find("find book at ?", new Object[] { firstDate });
      // Check
      Assert.assertEquals(result.size(),1);
      Book resultBook = (Book) result.get(0);
      Collections.sort(authors1);
      Collections.sort(authors2);
      Collections.sort(resultBook.getAuthors());
      logger.debug("authors1 list is: "+authors1);
      logger.debug("authors2 list is: "+authors2);
      logger.debug("result authors list is: "+resultBook.getAuthors());
      Assert.assertEquals( resultBook.getAuthors(),authors1);
      Assert.assertFalse(authors2.equals(resultBook.getAuthors()));
   }

   public void testMemberMaps()
      throws Exception
   {
      // Drop
      removeAll(MapHolder.class);
      // Create
      MapHolder mapHolder = new MapHolder();
      Book book = new Book("Book of Bokonon","9");
      HashMap meta1 = new HashMap();
      meta1.put("meta1",new Author("Geordi","LaForge"));
      meta1.put("meta2",new Author("Data",""));
      meta1.put("meta3",new Author("Scott","Montgomery"));
      meta1.put("book",book);
      mapHolder.setMeta(meta1);
      // Save
      getStore().save(mapHolder);
      Date firstDate = new Date();
      synchronized ( this )
      {
         // We must wait so the modification does not
         // commit in the same millisecond as minimumdate
         wait(100);
      }
      // Alter
      HashMap meta2 = new HashMap();
      meta2.put("meta3",new Author("Scott","Montgomery"));
      meta2.put("book",book);
      mapHolder.setMeta(meta2);
      getStore().save(mapHolder);
      // Recall
      List result = getStore().find("find mapholder at ?",new Object[] { firstDate });
      // Check
      Assert.assertEquals(result.size(),1);
      MapHolder resultHolder = (MapHolder) result.get(0);
      Assert.assertEquals(resultHolder.getMeta(),meta1);
      Assert.assertFalse(meta2.equals(resultHolder.getMeta()));
   }

   public void testConcurrentModification()
      throws Exception
   {
      // Drop
      removeAll(Referrer.class);
      // Create referrers
      Referrer refs[] = new Referrer[100];
      for ( int i=0; i<100; i++ )
      {
         refs[i]= new Referrer(i);
         getStore().save(refs[i]);
      }
      // Select
      List result = getStore().find("find referrer order by identity asc");
      // Iterate, and delete
      for ( int i=0; i<100; i++ )
      {
         getStore().remove(refs[i]);
         Assert.assertEquals(result.get(i),refs[i]);
      }
   }
   
   public void testSelectPolymorphicObjectsInPast()
      throws Exception
   {
      // Drop
      removeAll(Referrer.class);
      // Create referrers
      ReferrerSubclass ref = new ReferrerSubclass(1);
      getStore().save(ref);
      Date currentDate = new Date();
      // Select
      List result = getStore().find("find referrer at ?", new Object[] { currentDate });
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),ref);
   }

   public void testNonExistingObjectMetaData()
      throws Exception
   {
      // Drop
      removeAll(Author.class);
      // Nonexisting object
      Author author = new Author("Johnny","Cash");
      PersistenceMetaData metaData = getStore().getPersistenceMetaData(author);
      Assert.assertFalse(metaData.getPersistenceId()==null);
      Assert.assertTrue(metaData.getPersistenceStart()==null);
      Assert.assertTrue(metaData.getPersistenceEnd()==null);
   }

   public void testExistingObjectMetaData()
      throws Exception
   {
      // Drop
      removeAll(Author.class);
      // Create
      Author author = new Author("Johnny","Cash");
      getStore().save(author);
      // Check metadata
      PersistenceMetaData metaData = getStore().getPersistenceMetaData(author);
      Assert.assertEquals(metaData.getPersistenceId(),new Long(author.getPersistenceId()));
      Assert.assertFalse(metaData.getPersistenceStart()==null);
      Assert.assertTrue(metaData.getPersistenceEnd()==null);
   }

   public void testSavedObjectMetaData()
      throws Exception
   {
      // Drop
      removeAll(Author.class);
      // Create
      Author author = new Author("Johnny","Cash");
      getStore().save(author);
      // Check metadata
      PersistenceMetaData metaData = getStore().getPersistenceMetaData(author);
      Long id = metaData.getPersistenceId();
      Long start = metaData.getPersistenceStart();
      Long end = metaData.getPersistenceEnd();
      // Save again
      author.setFirstName("John");
      getStore().save(author);
      // Check
      Assert.assertFalse(start==null);
      Assert.assertTrue(end==null);
      Assert.assertEquals(metaData.getPersistenceId(),id);
      Assert.assertTrue(metaData.getPersistenceStart().longValue() > start.longValue());
      Assert.assertTrue(metaData.getPersistenceEnd()==null);
   }

   public void testRemovedObjectMetaData()
      throws Exception
   {
      // Drop
      removeAll(Author.class);
      // Create
      Author author = new Author("Johnny","Cash");
      getStore().save(author);
      // Check metadata
      PersistenceMetaData metaData = getStore().getPersistenceMetaData(author);
      Long id = metaData.getPersistenceId();
      Long start = metaData.getPersistenceStart();
      Long end = metaData.getPersistenceEnd();
      // Remove
      getStore().remove(author);
      // Check
      Assert.assertFalse(start==null);
      Assert.assertTrue(end==null);
      Assert.assertEquals(metaData.getPersistenceId(),id);
      Assert.assertTrue(metaData.getPersistenceStart().longValue() == start.longValue());
      Assert.assertFalse(metaData.getPersistenceEnd()==null);
   }

}

