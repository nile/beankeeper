/**
 * Copyright (C) 2007 NetMind Consulting Bt.
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

/**
 * Test the container framework.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class ListContainerTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(ListContainerTests.class);

   public void testListInitializeWithNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
   }
   
   public void testListInitializeWithExisting()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      Referrer ref1 = new Referrer(1);
      getStore().save(ref1);
      Referrer ref2 = new Referrer(2);
      getStore().save(ref2);
      Book book1 = new Book("Refer collection","1");
      getStore().save(book1);
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(ref1);
      holder.getList().add(ref2);
      holder.getList().add(book1);
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
   }

   public void testListFullToNullAndBack()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      // Set to null
      dbHolder.setList(null);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertNull(dbHolder.getList());
      // Set back
      dbHolder.setList(new ArrayList());
      dbHolder.getList().add(new Referrer(1));
      dbHolder.getList().add(new Referrer(2));
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
   }
  
   public void testListEmptyAddOnceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().add(new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),1);
   }
   
   public void testListNonemptyAddOnceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      dbHolder.getList().add(new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),4);
   }
   
   public void testListEmptyAddOnceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),1);
   }
   
   public void testListNonemptyAddOnceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),4);
   }
   
   public void testListContainsAddOnceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      Book book2 = new Book("Second chance","2");
      holder.getList().add(book2);
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      dbHolder.getList().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),4);
   }
   
   public void testListEmptyAddTwiceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
   }
   
   public void testListNonemptyAddTwiceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),5);
   }
   
   public void testListEmptyAddTwiceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
   }
   
   public void testListNonemptyAddTwiceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),5);
   }
   
   public void testListContainsAddTwiceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      Book book = new Book("Second chance","2");
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(book);
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      getStore().save(book);
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),6);
   }
   
   public void testListEmptyAddOnceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().add(new Book("Second chance","2"));
      // Make previous holder obsolate
      ListHolder currentHolder = (ListHolder) getStore().findSingle("find listholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getList().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getList().remove(modBook);
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),1);
   }
   
   public void testListNonemptyAddOnceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().add(new Book("Second chance","2"));
      // Make previous holder obsolate
      ListHolder currentHolder = (ListHolder) getStore().findSingle("find listholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getList().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getList().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      ListHolder result = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(result.getList().size(),4);
   }
   
   public void testListEmptyAddOnceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().add(book2);
      // Make previous holder obsolate
      ListHolder currentHolder = (ListHolder) getStore().findSingle("find listholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getList().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getList().remove(modBook);
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),1);
   }
   
   public void testListNonemptyAddOnceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().add(book2);
      // Make previous holder obsolate
      ListHolder currentHolder = (ListHolder) getStore().findSingle("find listholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getList().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getList().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      ListHolder result = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(result.getList().size(),4);
   }
   
   public void testListContainsAddOnceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      Book book2 = new Book("Second chance","2");
      holder.getList().add(book2);
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      dbHolder.getList().add(book2);
      // Make previous holder obsolate
      ListHolder currentHolder = (ListHolder) getStore().findSingle("find listholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getList().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getList().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      ListHolder result = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(result.getList().size(),4);
   }
   
   public void testListEmptyAddTwiceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      // Make previous holder obsolate
      ListHolder currentHolder = (ListHolder) getStore().findSingle("find listholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getList().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getList().remove(modBook);
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
   }
   
   public void testListNonemptyAddTwiceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      // Make previous holder obsolate
      ListHolder currentHolder = (ListHolder) getStore().findSingle("find listholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getList().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getList().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      ListHolder result = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(result.getList().size(),5);
   }
   
   public void testListEmptyAddTwiceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      // Make previous holder obsolate
      ListHolder currentHolder = (ListHolder) getStore().findSingle("find listholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getList().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getList().remove(modBook);
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
   }
   
   public void testListNonemptyAddTwiceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      // Make previous holder obsolate
      ListHolder currentHolder = (ListHolder) getStore().findSingle("find listholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getList().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getList().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      ListHolder result = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(result.getList().size(),5);
   }
   
   public void testListContainsAddTwiceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      Book book = new Book("Second chance","2");
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(book);
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      getStore().save(book);
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      // Make previous holder obsolate
      ListHolder currentHolder = (ListHolder) getStore().findSingle("find listholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getList().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getList().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      ListHolder result = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(result.getList().size(),6);
   }
   
   public void testListEmptyAddOnceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.setList(new ArrayList());
      dbHolder.getList().add(new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),1);
   }
   
   public void testListNonemptyAddOnceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.setList(new ArrayList());
      dbHolder.getList().add(new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),1);
   }
  
   public void testListEmptyAddOnceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.setList(new ArrayList());
      dbHolder.getList().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),1);
   }
   
   public void testListNonemptyAddOnceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.setList(new ArrayList());
      dbHolder.getList().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),1);
   }
   
   public void testListContainsAddOnceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      Book book2 = new Book("Second chance","2");
      holder.getList().add(book2);
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      dbHolder.setList(new ArrayList());
      dbHolder.getList().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),1);
   }
   
   public void testListEmptyAddTwiceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      dbHolder.setList(new ArrayList());
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
   }
   
   public void testListNonemptyAddTwiceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      dbHolder.setList(new ArrayList());
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
   }
   
   public void testListEmptyAddTwiceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      dbHolder.setList(new ArrayList());
      getStore().save(book);
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
   }
   
   public void testListNonemptyAddTwiceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.setList(new ArrayList());
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
   }
   
   public void testListContainsAddTwiceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      Book book = new Book("Second chance","2");
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(book);
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.setList(new ArrayList());
      getStore().save(book);
      dbHolder.getList().add(book);
      dbHolder.getList().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
   }

   public void testListAssignForeignListNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      Book book = new Book("Second chance","2");
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(book);
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      // Now assign that list to other listholder
      ListHolder otherHolder = new ListHolder();
      otherHolder.setList(dbHolder.getList());
      getStore().save(otherHolder);
      // Get the result
      ListHolder dbOtherHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbOtherHolder.getList().size(),4);
   }
   
   public void testListAssignForeignListExisting()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      Book book = new Book("Second chance","2");
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(book);
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      // Now assign that list to other listholder
      ListHolder otherHolder = new ListHolder();
      otherHolder.setList(new ArrayList());
      getStore().save(otherHolder);
      otherHolder = (ListHolder) getStore().findSingle("find listholder");
      otherHolder.setList(dbHolder.getList());
      getStore().save(otherHolder);
      // Get the result
      ListHolder dbOtherHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbOtherHolder.getList().size(),4);
   }
   
   public void testListClearOnCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check and clear
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      dbHolder.getList().clear();
      getStore().save(dbHolder);
      // Get
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),0);
   }

   public void testListClearOnObsolete()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      getStore().save(holder);
      // Get
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().clear();
      // Make previous holder obsolate
      ListHolder currentHolder = (ListHolder) getStore().findSingle("find listholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getList().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getList().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      ListHolder result = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(result.getList().size(),0);
   }

   public void testListContainsNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      Assert.assertFalse(dbHolder.getList().contains(new Book("Refer collection","1")));
   }
   
   public void testListContainsExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      Book book = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getList().contains(book));
   }
   
   public void testListContainsExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getList().add(book);
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      Assert.assertTrue(dbHolder.getList().contains(book));
   }

   public void testListContainsAddedExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getList().add(book);
      Assert.assertTrue(dbHolder.getList().contains(book));
   }

   public void testListContainsAddedExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getList().add(book);
      Book book2 = new Book("Refer collection","1");
      getStore().save(book2);
      Assert.assertFalse(dbHolder.getList().contains(book2));
   }

   public void testListContainsAddedNonexistingSame()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
      Book book = new Book("Refer collection","1");
      dbHolder.getList().add(book);
      Assert.assertTrue(dbHolder.getList().contains(book));
   }

   public void testListContainsAddedNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
      Book book = new Book("Refer collection","1");
      dbHolder.getList().add(book);
      Book book2 = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getList().contains(book2));
   }

   public void testListContainsLongNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
      Assert.assertFalse(dbHolder.getList().contains(new Book("Refer collection","1")));
   }
  
   public void testListContainsLongExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
      Book book = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getList().contains(book));
   }
   
   public void testListContainsLongExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      Book book = new Book("Refer collection","1");
      holder.getList().add(book);
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
      Assert.assertTrue(dbHolder.getList().contains(book));
   }

   public void testListContainsLongAddedExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),50);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getList().add(book);
      Book book2 = new Book("Refer collection","1");
      getStore().save(book2);
      Assert.assertFalse(dbHolder.getList().contains(book2));
   }

   public void testListContainsLongAddedExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),50);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getList().add(book);
      Book book2 = new Book("Refer collection","1");
      getStore().save(book2);
      Assert.assertFalse(dbHolder.getList().contains(book2));
   }

   public void testListContainsLongAddedNonexistingSame()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),50);
      Book book = new Book("Refer collection","1");
      dbHolder.getList().add(book);
      Assert.assertTrue(dbHolder.getList().contains(book));
   }

   public void testListContainsLongAddedNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),50);
      Book book = new Book("Refer collection","1");
      dbHolder.getList().add(book);
      Book book2 = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getList().contains(book2));
   }

   public void testListAddExistingTwiceToNewList()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getList().add(book);
      holder.getList().add(0,book);
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),4);
   }

   public void testListsEqualCurrentToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      ListHolder dbHolder2 = (ListHolder) getStore().findSingle("find listholder");
      // Check
      Assert.assertEquals(dbHolder2.getList(),dbHolder.getList());
      Assert.assertEquals(dbHolder.getList(),dbHolder2.getList());
   }

   public void testListsEqualObsoleteToCurrentEqual()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      ListHolder dbHolder2 = (ListHolder) getStore().findSingle("find listholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getList().add(modBook);
      getStore().save(dbHolder);
      dbHolder.getList().remove(modBook);
      getStore().save(dbHolder);
      // Check
      Assert.assertEquals(dbHolder2.getList(),dbHolder.getList());
      Assert.assertEquals(dbHolder.getList(),dbHolder2.getList());
   }

   public void testListsEqualObsoleteToCurrentNonequal()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      ListHolder dbHolder2 = (ListHolder) getStore().findSingle("find listholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getList().add(modBook);
      getStore().save(dbHolder);
      // Check
      Assert.assertFalse(dbHolder.getList().equals(dbHolder2.getList()));
      Assert.assertFalse(dbHolder2.getList().equals(dbHolder.getList()));
   }

   public void testListsEqualCurrentToCustomEqual()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      Referrer ref1 = new Referrer(1);
      getStore().save(ref1);
      Referrer ref2 = new Referrer(2);
      getStore().save(ref2);
      Book book1 = new Book("Refer collection","1");
      getStore().save(book1);
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(ref1);
      holder.getList().add(ref2);
      holder.getList().add(book1);
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      // Make custom which equal
      ArrayList v = new ArrayList();
      v.add(ref1);
      v.add(ref2);
      v.add(book1);
      // Check
      Assert.assertEquals(dbHolder.getList(),v);
   }

   public void testListsEqualCurrentToCustomNonequal()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      Referrer ref1 = new Referrer(1);
      getStore().save(ref1);
      Referrer ref2 = new Referrer(2);
      getStore().save(ref2);
      Book book1 = new Book("Refer collection","1");
      getStore().save(book1);
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(ref1);
      holder.getList().add(ref2);
      holder.getList().add(book1);
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      // Make custom which equal
      ArrayList v = new ArrayList();
      v.add(ref1);
      v.add(ref2);
      // Check
      Assert.assertFalse(dbHolder.getList().equals(v));
   }

   public void testListsEqualsLongCurrentToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
      ListHolder dbHolder2 = (ListHolder) getStore().findSingle("find listholder");
      // Check
      Assert.assertEquals(dbHolder2.getList(),dbHolder.getList());
      Assert.assertEquals(dbHolder.getList(),dbHolder2.getList());
   }

   public void testListsEqualsLongObsoleteToCurrentEqual()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
      ListHolder dbHolder2 = (ListHolder) getStore().findSingle("find listholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getList().add(modBook);
      getStore().save(dbHolder);
      dbHolder.getList().remove(modBook);
      getStore().save(dbHolder);
      // Check
      Assert.assertEquals(dbHolder2.getList(),dbHolder.getList());
      Assert.assertEquals(dbHolder.getList(),dbHolder2.getList());
   }

   public void testListsEqualsLongObsoleteToCurrentNonequal()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
      ListHolder dbHolder2 = (ListHolder) getStore().findSingle("find listholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getList().add(modBook);
      getStore().save(dbHolder);
      // Check
      Assert.assertFalse(dbHolder.getList().equals(dbHolder2.getList()));
      Assert.assertFalse(dbHolder2.getList().equals(dbHolder.getList()));
   }

   public void testListGetOutOfBounds()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      try
      {
         dbHolder.getList().get(4);
         Assert.fail("4th item could be get, but there is no such item.");
      } catch ( IndexOutOfBoundsException e ) {
         logger.debug("expected exception",e);
      }
      try
      {
         dbHolder.getList().get(-1);
         Assert.fail("-1st item could be get, but there is no such item.");
      } catch ( ArrayIndexOutOfBoundsException e ) {
         logger.debug("expected exception",e);
      }
   }

   public void testListRemoveNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      dbHolder.getList().remove(new Book("Noone's Land","1"));
      Assert.assertEquals(dbHolder.getList().size(),3);
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
   }

   public void testListRemoveExisting()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      Book book = new Book("Noone's Land","1");
      getStore().save(book);
      dbHolder.getList().remove(book);
      Assert.assertEquals(dbHolder.getList().size(),3);
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
   }

   public void testListRemoveMember()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getList().add(book);
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      dbHolder.getList().remove(book);
      Assert.assertEquals(dbHolder.getList().size(),2);
      getStore().save(dbHolder);
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),2);
   }

   public void testListRemoveLongNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
      Book book = new Book("Noone's Land","1");
      dbHolder.getList().remove(book);
      Assert.assertEquals(dbHolder.getList().size(),51);
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
   }

   public void testListRemoveLongExisting()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
      Book book = new Book("Noone's Land","1");
      getStore().save(book);
      dbHolder.getList().remove(book);
      Assert.assertEquals(dbHolder.getList().size(),51);
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
   }

   public void testListRemoveLongMember()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      Book book = new Book("Noone's Land","1");
      holder.getList().add(book);
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
      dbHolder.getList().remove(book);
      Assert.assertEquals(dbHolder.getList().size(),50);
      getStore().save(dbHolder);
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),50);
   }

   public void testListRemoveLongOutside()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Insert mixed stuff
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      Book book = new Book("Noone's Land","1");
      holder.getList().add(book);
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),51);
      getStore().remove(book);
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),50);
   }

   public void testListIterationRemove()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),3);
      // Iterate
      Iterator iterator = dbHolder.getList().iterator();
      while ( iterator.hasNext() )
      {
         iterator.next();
         iterator.remove();
      }
      getStore().save(dbHolder);
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),0);
   }

   public void testListIterationNormal()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      // Iterate
      Assert.assertEquals(dbHolder.getList().size(),3);
      Iterator iterator = dbHolder.getList().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testListIterationRemoved()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getList().add(book);
      getStore().save(holder);
      // Get and add a nonexisting object too
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().remove(book);
      // Iterate
      Assert.assertEquals(dbHolder.getList().size(),2);
      Iterator iterator = dbHolder.getList().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testListIterationAdded()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().add(new Book("Second chance","2"));
      // Iterate
      Assert.assertEquals(dbHolder.getList().size(),4);
      Iterator iterator = dbHolder.getList().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testListIterationAddedLong()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Add empty list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<50; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().add(new Book("Second chance","2"));
      // Iterate
      Assert.assertEquals(dbHolder.getList().size(),52);
      Iterator iterator = dbHolder.getList().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testListPrimitives()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Save
      ListHolder holder = new ListHolder();
      ArrayList content = new ArrayList();
      holder.setList(content);
      content.add(new Book("I Am Surrounded By Primitives","1-1-1-1"));
      content.add("Primitive String");
      content.add(new Integer(1));
      content.add(new Short((short) 2));
      content.add(new Byte((byte) 3));
      content.add(new Long(4));
      content.add(new Float(5.5));
      content.add(new Double(6.6));
      content.add(new Character('a'));
      content.add(new Boolean(true));
      content.add(new Date());
      getStore().save(holder);
      // Recall
      ListHolder result = (ListHolder) getStore().findSingle("find listholder");
      List list = result.getList();
      Assert.assertEquals(getCount(list,Book.class),1);
      Assert.assertEquals(getCount(list,String.class),1);
      Assert.assertEquals(getCount(list,Short.class),1);
      Assert.assertEquals(getCount(list,Byte.class),1);
      Assert.assertEquals(getCount(list,Long.class),1);
      Assert.assertEquals(getCount(list,Float.class),1);
      Assert.assertEquals(getCount(list,Double.class),1);
      Assert.assertEquals(getCount(list,Character.class),1);
      Assert.assertEquals(getCount(list,Boolean.class),1);
      Assert.assertEquals(getCount(list,Date.class),1);
      logger.debug("test result list is: "+list);
   }

   public void testIndexOfSmall()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Create object
      Book book = new Book("Second chance","2");
      getStore().save(book);
      // Create list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(book);
      holder.getList().add(new Referrer(3));
      holder.getList().add(new Referrer(4));
      holder.getList().add(book);
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),6);
      Assert.assertEquals(dbHolder.getList().indexOf(book),2);
      Assert.assertEquals(dbHolder.getList().lastIndexOf(book),5);
   }

   public void testIndexOfLarge()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Create object
      Book book = new Book("Second chance","2");
      getStore().save(book);
      // Create list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<20; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(book);
      for ( int i=0; i<20; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(book);
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),42);
      Assert.assertEquals(dbHolder.getList().indexOf(book),20);
      Assert.assertEquals(dbHolder.getList().lastIndexOf(book),41);
   }

   public void testContainsPrimitiveSmall()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Create list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new StringBuffer("String Ni").toString());
      holder.getList().add(new Referrer(3));
      holder.getList().add(new Referrer(4));
      holder.getList().add(new StringBuffer("String Ni").toString());
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),6);
      Assert.assertEquals(dbHolder.getList().indexOf("String Ni"),2);
      Assert.assertEquals(dbHolder.getList().lastIndexOf("String Ni"),5);
      Assert.assertTrue(dbHolder.getList().contains("String Ni"));
      Assert.assertFalse(dbHolder.getList().contains("String No"));
   }

   public void testContainsPrimitiveNonsavedSmall()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Create list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().add(new StringBuffer("String Ni").toString());
      Assert.assertTrue(dbHolder.getList().contains("String Ni"));
      Assert.assertFalse(dbHolder.getList().contains("String No"));
   }

   public void testContainsPrimitiveLarge()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Create object
      Book book = new Book("Second chance","2");
      getStore().save(book);
      // Create list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<20; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(new StringBuffer("String Ni").toString());
      for ( int i=0; i<20; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(new StringBuffer("String Ni").toString());
      getStore().save(holder);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),42);
      Assert.assertEquals(dbHolder.getList().indexOf("String Ni"),20);
      Assert.assertEquals(dbHolder.getList().lastIndexOf("String Ni"),41);
      Assert.assertTrue(dbHolder.getList().contains("String Ni"));
      Assert.assertFalse(dbHolder.getList().contains("String No"));
   }

   public void testContainsDeletedSmall()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Create author
      Author author = new Author("Stephenson","Neal");
      getStore().save(author);
      // Create list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(author);
      getStore().save(holder);
      // Remove author
      getStore().remove(author);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),0);
      Assert.assertEquals(dbHolder.getList().indexOf(author),-1);
      Assert.assertEquals(dbHolder.getList().lastIndexOf(author),-1);
      Assert.assertFalse(dbHolder.getList().contains(author));
   }

   public void testContainsDeletedLarge()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      // Create author
      Author author = new Author("Stephenson","Neal");
      getStore().save(author);
      // Create list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      for ( int i=0; i<40; i++ )
         holder.getList().add(new Referrer(i));
      holder.getList().add(author);
      getStore().save(holder);
      // Remove author
      getStore().remove(author);
      // Check
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      Assert.assertEquals(dbHolder.getList().size(),40);
      Assert.assertEquals(dbHolder.getList().indexOf(author),-1);
      Assert.assertEquals(dbHolder.getList().lastIndexOf(author),-1);
      Assert.assertFalse(dbHolder.getList().contains(author));
   }

   public void testListInsertMiddle()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      removeAll(Referrer.class);
      // Create list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      getStore().save(holder);
      // Insert middle
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      dbHolder.getList().add(1,new Referrer(3));
      getStore().save(dbHolder);
      // Query again, and check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      logger.debug("insert middle list is: "+dbHolder.getList());
      Assert.assertEquals(((Referrer)dbHolder.getList().get(0)).getIdentity(),1);
      Assert.assertEquals(((Referrer)dbHolder.getList().get(1)).getIdentity(),3);
      Assert.assertEquals(((Referrer)dbHolder.getList().get(2)).getIdentity(),2);
   }

   public void testReindexing()
      throws Exception
   {
      // Drop table
      removeAll(ListHolder.class);
      removeAll(Referrer.class);
      // Create list
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      holder.getList().add(new Referrer(3));
      holder.getList().add(new Referrer(4));
      getStore().save(holder);
      // Load
      ListHolder dbHolder = (ListHolder) getStore().findSingle("find listholder");
      // Insert 40 referrers between 2 and 3
      for ( int i=0; i<40; i++ )
         dbHolder.getList().add(2,new Referrer(10+i));
      getStore().save(dbHolder); // Reindexing must occur here
      // Check
      dbHolder = (ListHolder) getStore().findSingle("find listholder");
      logger.debug("reindexing test result: "+dbHolder.getList());
      for ( int i=0; i<40; i++ )
         Assert.assertEquals(((Referrer) dbHolder.getList().get(i+2)).getIdentity(),49-i);
      Assert.assertEquals(((Referrer) dbHolder.getList().get(0)).getIdentity(),1);
      Assert.assertEquals(((Referrer) dbHolder.getList().get(1)).getIdentity(),2);
      Assert.assertEquals(((Referrer) dbHolder.getList().get(42)).getIdentity(),3);
      Assert.assertEquals(((Referrer) dbHolder.getList().get(43)).getIdentity(),4);
   }
}

