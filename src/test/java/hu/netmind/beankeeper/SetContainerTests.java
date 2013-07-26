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
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Test the container framework.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class SetContainerTests extends AbstractPersistenceTest
{
   public void testSetInitializeWithNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
   }
   
   public void testSetInitializeWithExisting()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      Referrer ref1 = new Referrer(1);
      getStore().save(ref1);
      Referrer ref2 = new Referrer(2);
      getStore().save(ref2);
      Book book1 = new Book("Refer collection","1");
      getStore().save(book1);
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(ref1);
      holder.getSet().add(ref2);
      holder.getSet().add(book1);
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
   }

   public void testSetFullToNullAndBack()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      // Set to null
      dbHolder.setSet(null);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertNull(dbHolder.getSet());
      // Set back
      dbHolder.setSet(new HashSet());
      dbHolder.getSet().add(new Referrer(1));
      dbHolder.getSet().add(new Referrer(2));
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),2);
   }
  
   public void testSetEmptyAddOnceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().add(new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddOnceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().add(new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),4);
   }
   
   public void testSetEmptyAddOnceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddOnceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),4);
   }
   
   public void testSetContainsAddOnceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      Book book2 = new Book("Second chance","2");
      holder.getSet().add(book2);
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      dbHolder.getSet().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
   }
   
   public void testSetEmptyAddTwiceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddTwiceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),4);
   }
   
   public void testSetEmptyAddTwiceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddTwiceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),4);
   }
   
   public void testSetContainsAddTwiceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      Book book = new Book("Second chance","2");
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(book);
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      getStore().save(book);
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),4);
   }
   
   public void testSetEmptyAddOnceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().add(new Book("Second chance","2"));
      // Make previous holder obsolate
      SetHolder currentHolder = (SetHolder) getStore().findSingle("find setholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getSet().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getSet().remove(modBook);
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddOnceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().add(new Book("Second chance","2"));
      // Make previous holder obsolate
      SetHolder currentHolder = (SetHolder) getStore().findSingle("find setholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getSet().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getSet().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      SetHolder result = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(result.getSet().size(),4);
   }
   
   public void testSetEmptyAddOnceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().add(book2);
      // Make previous holder obsolate
      SetHolder currentHolder = (SetHolder) getStore().findSingle("find setholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getSet().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getSet().remove(modBook);
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddOnceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().add(book2);
      // Make previous holder obsolate
      SetHolder currentHolder = (SetHolder) getStore().findSingle("find setholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getSet().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getSet().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      SetHolder result = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(result.getSet().size(),4);
   }
   
   public void testSetContainsAddOnceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      Book book2 = new Book("Second chance","2");
      holder.getSet().add(book2);
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      dbHolder.getSet().add(book2);
      // Make previous holder obsolate
      SetHolder currentHolder = (SetHolder) getStore().findSingle("find setholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getSet().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getSet().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      SetHolder result = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(result.getSet().size(),3);
   }
   
   public void testSetEmptyAddTwiceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      // Make previous holder obsolate
      SetHolder currentHolder = (SetHolder) getStore().findSingle("find setholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getSet().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getSet().remove(modBook);
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddTwiceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      // Make previous holder obsolate
      SetHolder currentHolder = (SetHolder) getStore().findSingle("find setholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getSet().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getSet().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      SetHolder result = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(result.getSet().size(),4);
   }
   
   public void testSetEmptyAddTwiceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      // Make previous holder obsolate
      SetHolder currentHolder = (SetHolder) getStore().findSingle("find setholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getSet().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getSet().remove(modBook);
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddTwiceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      // Make previous holder obsolate
      SetHolder currentHolder = (SetHolder) getStore().findSingle("find setholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getSet().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getSet().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      SetHolder result = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(result.getSet().size(),4);
   }
   
   public void testSetContainsAddTwiceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      Book book = new Book("Second chance","2");
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(book);
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      getStore().save(book);
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      // Make previous holder obsolate
      SetHolder currentHolder = (SetHolder) getStore().findSingle("find setholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getSet().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getSet().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      SetHolder result = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(result.getSet().size(),4);
   }
   
   public void testSetEmptyAddOnceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.setSet(new HashSet());
      dbHolder.getSet().add(new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddOnceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.setSet(new HashSet());
      dbHolder.getSet().add(new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
  
   public void testSetEmptyAddOnceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.setSet(new HashSet());
      dbHolder.getSet().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddOnceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.setSet(new HashSet());
      dbHolder.getSet().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetContainsAddOnceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      Book book2 = new Book("Second chance","2");
      holder.getSet().add(book2);
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      dbHolder.setSet(new HashSet());
      dbHolder.getSet().add(book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetEmptyAddTwiceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      dbHolder.setSet(new HashSet());
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddTwiceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      dbHolder.setSet(new HashSet());
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetEmptyAddTwiceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      dbHolder.setSet(new HashSet());
      getStore().save(book);
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetNonemptyAddTwiceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.setSet(new HashSet());
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }
   
   public void testSetContainsAddTwiceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      Book book = new Book("Second chance","2");
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(book);
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.setSet(new HashSet());
      getStore().save(book);
      dbHolder.getSet().add(book);
      dbHolder.getSet().add(book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),1);
   }

   public void testSetAssignForeignSetNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      Book book = new Book("Second chance","2");
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(book);
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      // Now assign that set to other setholder
      SetHolder otherHolder = new SetHolder();
      otherHolder.setSet(dbHolder.getSet());
      getStore().save(otherHolder);
      // Get the result
      SetHolder dbOtherHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbOtherHolder.getSet().size(),4);
   }
   
   public void testSetAssignForeignSetExisting()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      Book book = new Book("Second chance","2");
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(book);
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      // Now assign that set to other setholder
      SetHolder otherHolder = new SetHolder();
      otherHolder.setSet(new HashSet());
      getStore().save(otherHolder);
      otherHolder = (SetHolder) getStore().findSingle("find setholder");
      otherHolder.setSet(dbHolder.getSet());
      getStore().save(otherHolder);
      // Get the result
      SetHolder dbOtherHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbOtherHolder.getSet().size(),4);
   }
   
   public void testSetClearOnCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check and clear
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      dbHolder.getSet().clear();
      getStore().save(dbHolder);
      // Get
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),0);
   }

   public void testSetClearOnObsolete()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      getStore().save(holder);
      // Get
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().clear();
      // Make previous holder obsolate
      SetHolder currentHolder = (SetHolder) getStore().findSingle("find setholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getSet().add(modBook);
      getStore().save(currentHolder);
      currentHolder.getSet().remove(modBook);
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      SetHolder result = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(result.getSet().size(),0);
   }

   public void testSetContainsNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      Assert.assertFalse(dbHolder.getSet().contains(new Book("Refer collection","1")));
   }
   
   public void testSetContainsExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      Book book = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getSet().contains(book));
   }
   
   public void testSetContainsExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getSet().add(book);
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      Assert.assertTrue(dbHolder.getSet().contains(book));
   }

   public void testSetContainsAddedExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),2);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getSet().add(book);
      Assert.assertTrue(dbHolder.getSet().contains(book));
   }

   public void testSetContainsAddedExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),2);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getSet().add(book);
      Book book2 = new Book("Refer collection","1");
      getStore().save(book2);
      Assert.assertFalse(dbHolder.getSet().contains(book2));
   }

   public void testSetContainsAddedNonexistingSame()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),2);
      Book book = new Book("Refer collection","1");
      dbHolder.getSet().add(book);
      Assert.assertTrue(dbHolder.getSet().contains(book));
   }

   public void testSetContainsAddedNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),2);
      Book book = new Book("Refer collection","1");
      dbHolder.getSet().add(book);
      Book book2 = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getSet().contains(book2));
   }

   public void testSetContainsLongNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
      Assert.assertFalse(dbHolder.getSet().contains(new Book("Refer collection","1")));
   }
   
   public void testSetContainsLongExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
      Book book = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getSet().contains(book));
   }
   
   public void testSetContainsLongExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      Book book = new Book("Refer collection","1");
      holder.getSet().add(book);
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
      Assert.assertTrue(dbHolder.getSet().contains(book));
   }

   public void testSetContainsLongAddedExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),50);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getSet().add(book);
      Book book2 = new Book("Refer collection","1");
      getStore().save(book2);
      Assert.assertFalse(dbHolder.getSet().contains(book2));
   }

   public void testSetContainsLongAddedExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),50);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getSet().add(book);
      Book book2 = new Book("Refer collection","1");
      getStore().save(book2);
      Assert.assertFalse(dbHolder.getSet().contains(book2));
   }

   public void testSetContainsLongAddedNonexistingSame()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),50);
      Book book = new Book("Refer collection","1");
      dbHolder.getSet().add(book);
      Assert.assertTrue(dbHolder.getSet().contains(book));
   }

   public void testSetContainsLongAddedNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),50);
      Book book = new Book("Refer collection","1");
      dbHolder.getSet().add(book);
      Book book2 = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getSet().contains(book2));
   }

   public void testSetAddExistingTwiceToNewSet()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getSet().add(book);
      holder.getSet().add(book);
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
   }

   public void testSetsEqualCurrentToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      SetHolder dbHolder2 = (SetHolder) getStore().findSingle("find setholder");
      // Check
      Assert.assertTrue(dbHolder2.getSet().equals(dbHolder.getSet()));
      Assert.assertTrue(dbHolder.getSet().equals(dbHolder2.getSet()));
   }

   public void testSetsEqualObsoleteToCurrentEqual()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      SetHolder dbHolder2 = (SetHolder) getStore().findSingle("find setholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getSet().add(modBook);
      getStore().save(dbHolder);
      dbHolder.getSet().remove(modBook);
      getStore().save(dbHolder);
      // Check
      Assert.assertTrue(dbHolder2.getSet().equals(dbHolder.getSet()));
      Assert.assertTrue(dbHolder.getSet().equals(dbHolder2.getSet()));
   }

   public void testSetsEqualObsoleteToCurrentNonequal()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      SetHolder dbHolder2 = (SetHolder) getStore().findSingle("find setholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getSet().add(modBook);
      getStore().save(dbHolder);
      // Check
      Assert.assertFalse(dbHolder.getSet().equals(dbHolder2.getSet()));
      Assert.assertFalse(dbHolder2.getSet().equals(dbHolder.getSet()));
   }

   public void testSetsEqualCurrentToCustomEqual()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      Referrer ref1 = new Referrer(1);
      getStore().save(ref1);
      Referrer ref2 = new Referrer(2);
      getStore().save(ref2);
      Book book1 = new Book("Refer collection","1");
      getStore().save(book1);
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(ref1);
      holder.getSet().add(ref2);
      holder.getSet().add(book1);
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      // Make custom which equal
      HashSet v = new HashSet();
      v.add(ref1);
      v.add(ref2);
      v.add(book1);
      // Check
      Assert.assertTrue(dbHolder.getSet().equals(v));
   }

   public void testSetsEqualCurrentToCustomNonequal()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      Referrer ref1 = new Referrer(1);
      getStore().save(ref1);
      Referrer ref2 = new Referrer(2);
      getStore().save(ref2);
      Book book1 = new Book("Refer collection","1");
      getStore().save(book1);
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(ref1);
      holder.getSet().add(ref2);
      holder.getSet().add(book1);
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      // Make custom which equal
      HashSet v = new HashSet();
      v.add(ref1);
      v.add(ref2);
      // Check
      Assert.assertFalse(dbHolder.getSet().equals(v));
   }

   public void testSetsEqualsLongCurrentToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
      SetHolder dbHolder2 = (SetHolder) getStore().findSingle("find setholder");
      // Check
      Assert.assertTrue(dbHolder2.getSet().equals(dbHolder.getSet()));
      Assert.assertTrue(dbHolder.getSet().equals(dbHolder2.getSet()));
   }

   public void testSetsEqualsLongObsoleteToCurrentEqual()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
      SetHolder dbHolder2 = (SetHolder) getStore().findSingle("find setholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getSet().add(modBook);
      getStore().save(dbHolder);
      dbHolder.getSet().remove(modBook);
      getStore().save(dbHolder);
      // Check
      Assert.assertTrue(dbHolder2.getSet().equals(dbHolder.getSet()));
      Assert.assertTrue(dbHolder.getSet().equals(dbHolder2.getSet()));
   }

   public void testSetsEqualsLongObsoleteToCurrentNonequal()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
      SetHolder dbHolder2 = (SetHolder) getStore().findSingle("find setholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getSet().add(modBook);
      getStore().save(dbHolder);
      // Check
      Assert.assertFalse(dbHolder.getSet().equals(dbHolder2.getSet()));
      Assert.assertFalse(dbHolder2.getSet().equals(dbHolder.getSet()));
   }

   public void testSetRemoveNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      dbHolder.getSet().remove(new Book("Noone's Land","1"));
      Assert.assertEquals(dbHolder.getSet().size(),3);
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
   }

   public void testSetRemoveExisting()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      Book book = new Book("Noone's Land","1");
      getStore().save(book);
      dbHolder.getSet().remove(book);
      Assert.assertEquals(dbHolder.getSet().size(),3);
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
   }

   public void testSetRemoveMember()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getSet().add(book);
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      dbHolder.getSet().remove(book);
      Assert.assertEquals(dbHolder.getSet().size(),2);
      getStore().save(dbHolder);
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),2);
   }

   public void testSetRemoveLongNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
      Book book = new Book("Noone's Land","1");
      dbHolder.getSet().remove(book);
      Assert.assertEquals(dbHolder.getSet().size(),51);
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
   }

   public void testSetRemoveLongExisting()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
      Book book = new Book("Noone's Land","1");
      getStore().save(book);
      dbHolder.getSet().remove(book);
      Assert.assertEquals(dbHolder.getSet().size(),51);
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
   }

   public void testSetRemoveLongMember()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      Book book = new Book("Noone's Land","1");
      holder.getSet().add(book);
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
      dbHolder.getSet().remove(book);
      Assert.assertEquals(dbHolder.getSet().size(),50);
      getStore().save(dbHolder);
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),50);
   }

   public void testSetRemoveLongOutside()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Insert mixed stuff
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      Book book = new Book("Noone's Land","1");
      holder.getSet().add(book);
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),51);
      getStore().remove(book);
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),50);
   }

   public void testSetIterationRemove()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),3);
      // Iterate
      Iterator iterator = dbHolder.getSet().iterator();
      while ( iterator.hasNext() )
      {
         iterator.next();
         iterator.remove();
      }
      getStore().save(dbHolder);
      // Check
      dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),0);
   }

   public void testSetIterationNormal()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      // Iterate
      Assert.assertEquals(dbHolder.getSet().size(),3);
      Iterator iterator = dbHolder.getSet().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testSetIterationRemoved()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getSet().add(book);
      getStore().save(holder);
      // Get and add a nonexisting object too
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().remove(book);
      // Iterate
      Assert.assertEquals(dbHolder.getSet().size(),2);
      Iterator iterator = dbHolder.getSet().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testSetIterationAdded()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().add(new Book("Second chance","2"));
      // Iterate
      Assert.assertEquals(dbHolder.getSet().size(),4);
      Iterator iterator = dbHolder.getSet().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testSetIterationAddedLong()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Add empty set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<50; i++ )
         holder.getSet().add(new Referrer(i));
      holder.getSet().add(new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().add(new Book("Second chance","2"));
      // Iterate
      Assert.assertEquals(dbHolder.getSet().size(),52);
      Iterator iterator = dbHolder.getSet().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testSetPrimitives()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Save
      SetHolder holder = new SetHolder();
      HashSet content = new HashSet();
      holder.setSet(content);
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
      SetHolder result = (SetHolder) getStore().findSingle("find setholder");
      Set set = result.getSet();
      Assert.assertEquals(getCount(set,Book.class),1);
      Assert.assertEquals(getCount(set,String.class),1);
      Assert.assertEquals(getCount(set,Short.class),1);
      Assert.assertEquals(getCount(set,Byte.class),1);
      Assert.assertEquals(getCount(set,Long.class),1);
      Assert.assertEquals(getCount(set,Float.class),1);
      Assert.assertEquals(getCount(set,Double.class),1);
      Assert.assertEquals(getCount(set,Character.class),1);
      Assert.assertEquals(getCount(set,Boolean.class),1);
      Assert.assertEquals(getCount(set,Date.class),1);
   }

   public void testContainsPrimitiveSmall()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Create set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      holder.getSet().add(new Referrer(3));
      holder.getSet().add(new Referrer(4));
      holder.getSet().add(new StringBuffer("String Ni").toString());
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),5);
      Assert.assertTrue(dbHolder.getSet().contains("String Ni"));
      Assert.assertFalse(dbHolder.getSet().contains("String No"));
   }

   public void testContainsPrimitiveNonsavedSmall()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Create set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      dbHolder.getSet().add(new StringBuffer("String Ni").toString());
      Assert.assertTrue(dbHolder.getSet().contains("String Ni"));
      Assert.assertFalse(dbHolder.getSet().contains("String No"));
   }

   public void testContainsPrimitiveLarge()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Create object
      Book book = new Book("Second chance","2");
      getStore().save(book);
      // Create set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<20; i++ )
         holder.getSet().add(new Referrer(i));
      holder.getSet().add(new StringBuffer("String Ni").toString());
      for ( int i=0; i<20; i++ )
         holder.getSet().add(new Referrer(20+i));
      getStore().save(holder);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),41);
      Assert.assertTrue(dbHolder.getSet().contains("String Ni"));
      Assert.assertFalse(dbHolder.getSet().contains("String No"));
   }
   
   public void testContainsDeletedSmall()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Create author
      Author author = new Author("Stephenson","Neal");
      getStore().save(author);
      // Create set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(author);
      getStore().save(holder);
      // Remove author
      getStore().remove(author);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),0);
      Assert.assertFalse(dbHolder.getSet().contains(author));
   }

   public void testContainsDeletedLarge()
      throws Exception
   {
      // Drop table
      removeAll(SetHolder.class);
      // Create author
      Author author = new Author("Stephenson","Neal");
      getStore().save(author);
      // Create set
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      for ( int i=0; i<40; i++ )
         holder.getSet().add(new Referrer(i));
      holder.getSet().add(author);
      getStore().save(holder);
      // Remove author
      getStore().remove(author);
      // Check
      SetHolder dbHolder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(dbHolder.getSet().size(),40);
      Assert.assertFalse(dbHolder.getSet().contains(author));
   }
}

