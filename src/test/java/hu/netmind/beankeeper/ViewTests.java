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
import java.util.Map;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Class tests the view capability of library.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class ViewTests extends AbstractPersistenceTest
{
   public void testNonExistentTable()
      throws Exception
   {
      // Test
      List result = getStore().find("view nonexist.self");
      Assert.assertEquals(result.size(),0);
   }

   @Test(groups = { "quick" })
   public void testViewSimple1()
      throws Exception
   {
      // Drop table
      removeAll(Book.class);
      // Insert two books
      getStore().save(new Book("Title1",""));
      getStore().save(new Book("Title2",""));
      // Get view of titles
      List result = getStore().find("view book.title order by book.title asc");
      // Check
      Assert.assertEquals(result.size(),2);
      for ( int i=0; i<2; i++ )
      {
         Map value = (Map) result.get(i);
         Assert.assertEquals( value.get("title").toString(),"Title"+(i+1));
      }
   }

   public void testViewSimple2()
      throws Exception
   {
      // Drop table
      removeAll(Book.class);
      // Insert two books
      getStore().save(new Book("Title1","12341"));
      getStore().save(new Book("Title2","12342"));
      // Get view of titles
      List result = getStore().find("view book.title,book.isbn order by book.title asc");
      // Check
      Assert.assertEquals(result.size(),2);
      for ( int i=0; i<2; i++ )
      {
         Map value = (Map) result.get(i);
         Assert.assertEquals( value.get("title").toString(),"Title"+(i+1));
         Assert.assertEquals( value.get("isbn").toString(),"1234"+(i+1));
      }
   }

   public void testViewMoreTables()
      throws Exception
   {
      // Drop table
      removeAll(Book.class);
      removeAll(Author.class);
      // Insert two books
      Book book1 = new Book("Java For Dummies","51966");
      book1.setMainAuthor(new Author("Ada","Lovelace"));
      getStore().save(book1);
      getStore().save(new Author("Bjarne","Stroustrup"));
      // Get view of titles
      List result = getStore().find("view book.title,author.firstname where book.mainauthor=author");
      // Check
      Assert.assertEquals(result.size(),1);
      Map value = (Map) result.get(0);
      Assert.assertEquals( value.get("title").toString(),"Java For Dummies");
      Assert.assertEquals( value.get("firstname").toString(),"Ada");
   }

   public void testViewComplexAttribute()
      throws Exception
   {
      // Drop table
      removeAll(Book.class);
      removeAll(Author.class);
      // Insert two books
      Book book1 = new Book("Java For Dummies","51966");
      book1.setMainAuthor(new Author("Ada","Lovelace"));
      getStore().save(book1);
      getStore().save(new Author("Bjarne","Stroustrup"));
      // Get view of titles
      List result = getStore().find("view book.title,book.mainauthor.firstname");
      // Check
      Assert.assertEquals(result.size(),1);
      Map value = (Map) result.get(0);
      Assert.assertEquals( value.get("title").toString(),"Java For Dummies");
      Assert.assertEquals( value.get("firstname").toString(),"Ada");
   }
   
   
   public void testViewComplexAttributeWithAliases()
      throws Exception
   {
      // Drop table
      removeAll(Book.class);
      removeAll(Author.class);
      // Insert two books
      Book book1 = new Book("Java For Dummies","51966");
      book1.setMainAuthor(new Author("Ada","Lovelace"));
      getStore().save(book1);
      getStore().save(new Author("Bjarne","Stroustrup"));
      // Get view of titles
      List result = getStore().find("view book.title a,book.mainauthor.firstname b");
      // Check
      Assert.assertEquals(result.size(),1);
      Map value = (Map) result.get(0);
      Assert.assertEquals( value.get("a").toString(),"Java For Dummies");
      Assert.assertEquals( value.get("b").toString(),"Ada");
   }
   
   public void testTargetSubclass()
      throws Exception
   {
      // Drop tables
      removeAll(Referrer.class);
      removeAll(ReferrerSubclass.class);
      // Insert stuff
      getStore().save(new ReferrerSubclass(1,1));
      getStore().save(new Referrer(2));
      // Select
      List result = getStore().find("view referrersubclass.subidentity,referrersubclass.identity order by referrersubclass.identity");
      // Check
      Assert.assertEquals(result.size(),1);
      Map map1 = (Map) result.get(0);
      Assert.assertEquals(map1.get("identity").toString(),""+1);
      Assert.assertEquals(map1.get("subidentity").toString(),""+1);
   }
  
   public void testTargetSubclassSuperclassFirst()
      throws Exception
   {
      // Drop tables
      removeAll(Referrer.class);
      removeAll(ReferrerSubclass.class);
      // Insert stuff
      getStore().save(new ReferrerSubclass(1,1));
      getStore().save(new Referrer(2));
      // Select
      List result = getStore().find("view referrersubclass.identity,referrersubclass.subidentity order by referrersubclass.identity");
      // Check
      Assert.assertEquals(result.size(),1);
      Map map1 = (Map) result.get(0);
      Assert.assertEquals(map1.get("identity").toString(),""+1);
      Assert.assertEquals(map1.get("subidentity").toString(),""+1);
   }
  
   public void testOrderByNonExistentAttribute()
      throws Exception
   {
      // Drop table
      removeAll(Book.class);
      // Insert two books
      getStore().save(new Book("Title1",""));
      getStore().save(new Book("Title2",""));
      // Get view of titles
      List result = getStore().find("view book.isbn order by book.title asc");
      // Check
      Assert.assertEquals(result.size(),2);
   }

   public void testKeywordAttribute()
      throws Exception
   {
      // Drop table
      removeAll(NameTester.class);
      // Create
      NameTester n = new NameTester();
      n.setSelect("OK");
      getStore().save(n);
      // Select
      List result = getStore().find("view nametester.select");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(((Map)result.get(0)).get("select"),"OK");
   }

   public void testUnspecifiedAttribute()
      throws Exception
   {
      // Drop table
      removeAll(Book.class);
      // Insert book
      getStore().save(new Book("Title","Isbn"));
      // Get view
      List result = getStore().find("view book.title,isbn");
      // Check
      Assert.assertEquals(result.size(),1);
   }

   public void testSimilarNamedAttributes()
      throws Exception
   {
      // Drop table
      removeAll(Book.class);
      // Insert books
      getStore().save(new Book("Title 1","ISBN"));
      getStore().save(new Book("Title 2","ISBN"));
      getStore().save(new Book("Title 3","NONE"));
      // Get joined view of titles
      List result = getStore().find("view book1(book).title a, book2(book).title b "+
            "where book1.isbn=book2.isbn and book1.title<>book2.title order by book1.title asc");
      // Check
      Assert.assertEquals(result.size(),2);
      Map value1 = (Map) result.get(0);
      Assert.assertEquals(value1.get("a"),"Title 1");
      Assert.assertEquals(value1.get("b"),"Title 2");
      Map value2 = (Map) result.get(1);
      Assert.assertEquals(value2.get("a"),"Title 2");
      Assert.assertEquals(value2.get("b"),"Title 1");
   }

}


