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
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;

/**
 * Test the container framework.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class MapContainerTests extends AbstractPersistenceTest
{
   public void testMapInitializeWithNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
   }
   
   public void testMapInitializeWithExisting()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      Referrer ref1 = new Referrer(1);
      getStore().save(ref1);
      Referrer ref2 = new Referrer(2);
      getStore().save(ref2);
      Book book1 = new Book("Refer collection","1");
      getStore().save(book1);
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref1",ref1);
      holder.getMeta().put("Ref2",ref2);
      holder.getMeta().put("book1",book1);
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
   }

   public void testMapFullToNullAndBack()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      // Map to null
      dbHolder.setMeta(null);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertNull(dbHolder.getMeta());
      // Map back
      dbHolder.setMeta(new HashMap());
      dbHolder.getMeta().put("Ref"+1,new Referrer(1));
      dbHolder.getMeta().put("Ref"+2,new Referrer(2));
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),2);
   }
  
   public void testMapEmptyAddOnceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().put("secchance",new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddOnceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().put("secchance",new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),4);
   }
   
   public void testMapEmptyAddOnceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().put("book2",book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddOnceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().put("book2",book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),4);
   }
   
   public void testMapContainsAddOnceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      Book book2 = new Book("Second chance","2");
      holder.getMeta().put("book2",book2);
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      dbHolder.getMeta().put("book2",book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
   }
   
   public void testMapEmptyAddTwiceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddTwiceNonexistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),4);
   }
   
   public void testMapEmptyAddTwiceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddTwiceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),4);
   }
   
   public void testMapContainsAddTwiceExistingToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      Book book = new Book("Second chance","2");
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("book",book);
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),4);
   }
   
   public void testMapEmptyAddOnceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().put("secchance",new Book("Second chance","2"));
      // Make previous holder obsolate
      MapHolder currentHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getMeta().put("modBook",modBook);
      getStore().save(currentHolder);
      currentHolder.getMeta().remove("modBook");
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddOnceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().put("secchance",new Book("Second chance","2"));
      // Make previous holder obsolate
      MapHolder currentHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getMeta().put("modBook",modBook);
      getStore().save(currentHolder);
      currentHolder.getMeta().remove("modBook");
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      MapHolder result = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(result.getMeta().size(),4);
   }
   
   public void testMapEmptyAddOnceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().put("book2",book2);
      // Make previous holder obsolate
      MapHolder currentHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getMeta().put("modBook",modBook);
      getStore().save(currentHolder);
      currentHolder.getMeta().remove("modBook");
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddOnceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().put("book2",book2);
      // Make previous holder obsolate
      MapHolder currentHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getMeta().put("modBook",modBook);
      getStore().save(currentHolder);
      currentHolder.getMeta().remove("modBook");
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      MapHolder result = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(result.getMeta().size(),4);
   }
   
   public void testMapContainsAddOnceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      Book book2 = new Book("Second chance","2");
      holder.getMeta().put("book2",book2);
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      dbHolder.getMeta().put("book2",book2);
      // Make previous holder obsolate
      MapHolder currentHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getMeta().put("modBook",modBook);
      getStore().save(currentHolder);
      currentHolder.getMeta().remove("modBook");
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      MapHolder result = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(result.getMeta().size(),3);
   }
   
   public void testMapEmptyAddTwiceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      // Make previous holder obsolate
      MapHolder currentHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getMeta().put("modBook",modBook);
      getStore().save(currentHolder);
      currentHolder.getMeta().remove("modBook");
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddTwiceNonexistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      // Make previous holder obsolate
      MapHolder currentHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getMeta().put("modBook",modBook);
      getStore().save(currentHolder);
      currentHolder.getMeta().remove("modBook");
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      MapHolder result = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(result.getMeta().size(),4);
   }
   
   public void testMapEmptyAddTwiceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      // Make previous holder obsolate
      MapHolder currentHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getMeta().put("modBook",modBook);
      getStore().save(currentHolder);
      currentHolder.getMeta().remove("modBook");
      getStore().save(currentHolder);
      // Operation
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddTwiceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      // Make previous holder obsolate
      MapHolder currentHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getMeta().put("modBook",modBook);
      getStore().save(currentHolder);
      currentHolder.getMeta().remove("modBook");
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      MapHolder result = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(result.getMeta().size(),4);
   }
   
   public void testMapContainsAddTwiceExistingToObsolete()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      Book book = new Book("Second chance","2");
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("book",book);
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      // Make previous holder obsolate
      MapHolder currentHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getMeta().put("modBook",modBook);
      getStore().save(currentHolder);
      currentHolder.getMeta().remove("modBook");
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      MapHolder result = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(result.getMeta().size(),4);
   }
   
   public void testMapEmptyAddOnceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.setMeta(new HashMap());
      dbHolder.getMeta().put("secchance",new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddOnceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.setMeta(new HashMap());
      dbHolder.getMeta().put("secchance",new Book("Second chance","2"));
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
  
   public void testMapEmptyAddOnceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.setMeta(new HashMap());
      dbHolder.getMeta().put("book2",book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddOnceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      Book book2 = new Book("Second chance","2");
      getStore().save(book2);
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.setMeta(new HashMap());
      dbHolder.getMeta().put("book2",book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapContainsAddOnceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      Book book2 = new Book("Second chance","2");
      holder.getMeta().put("book2",book2);
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      dbHolder.setMeta(new HashMap());
      dbHolder.getMeta().put("book2",book2);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapEmptyAddTwiceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      dbHolder.setMeta(new HashMap());
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddTwiceNonexistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      dbHolder.setMeta(new HashMap());
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapEmptyAddTwiceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      dbHolder.setMeta(new HashMap());
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapNonemptyAddTwiceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book book = new Book("Second chance","2");
      getStore().save(book);
      dbHolder.setMeta(new HashMap());
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }
   
   public void testMapContainsAddTwiceExistingToCustom()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      Book book = new Book("Second chance","2");
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("book",book);
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.setMeta(new HashMap());
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      dbHolder.getMeta().put("book",book);
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),1);
   }

   public void testMapAssignForeignMapNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      Book book = new Book("Second chance","2");
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("book",book);
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      // Now assign that map to other mapholder
      MapHolder otherHolder = new MapHolder();
      otherHolder.setMeta(dbHolder.getMeta());
      getStore().save(otherHolder);
      // Get the result
      MapHolder dbOtherHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbOtherHolder.getMeta().size(),4);
   }
   
   public void testMapAssignForeignMapExisting()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      Book book = new Book("Second chance","2");
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("book",book);
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      // Now assign that map to other mapholder
      MapHolder otherHolder = new MapHolder();
      otherHolder.setMeta(new HashMap());
      getStore().save(otherHolder);
      otherHolder = (MapHolder) getStore().findSingle("find mapholder");
      otherHolder.setMeta(dbHolder.getMeta());
      getStore().save(otherHolder);
      // Get the result
      MapHolder dbOtherHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbOtherHolder.getMeta().size(),4);
   }
   
   public void testMapClearOnCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check and clear
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      dbHolder.getMeta().clear();
      getStore().save(dbHolder);
      // Get
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),0);
   }

   public void testMapClearOnObsolete()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      getStore().save(holder);
      // Get
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().clear();
      // Make previous holder obsolate
      MapHolder currentHolder = (MapHolder) getStore().findSingle("find mapholder");
      Book modBook = new Book("Modification Book","2");
      currentHolder.getMeta().put("modBook",modBook);
      getStore().save(currentHolder);
      currentHolder.getMeta().remove("modBook");
      getStore().save(currentHolder);
      // Check
      getStore().save(dbHolder);
      MapHolder result = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(result.getMeta().size(),0);
   }

   public void testMapContainsNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      Assert.assertFalse(dbHolder.getMeta().containsValue(new Book("Refer collection","1")));
   }
   
   public void testMapContainsExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      Book book = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getMeta().containsValue(book));
   }
   
   public void testMapContainsExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getMeta().put("book",book);
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      Assert.assertTrue(dbHolder.getMeta().containsValue(book));
      Assert.assertTrue(dbHolder.getMeta().containsKey("book"));
   }

   public void testMapContainsAddedExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),2);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      Assert.assertTrue(dbHolder.getMeta().containsValue(book));
      Assert.assertTrue(dbHolder.getMeta().containsKey("book"));
   }

   public void testMapContainsAddedExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),2);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      Book book2 = new Book("Refer collection","1");
      getStore().save(book2);
      Assert.assertFalse(dbHolder.getMeta().containsValue(book2));
      Assert.assertFalse(dbHolder.getMeta().containsKey("book2"));
   }

   public void testMapContainsAddedNonexistingSame()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),2);
      Book book = new Book("Refer collection","1");
      dbHolder.getMeta().put("book",book);
      Assert.assertTrue(dbHolder.getMeta().containsValue(book));
      Assert.assertTrue(dbHolder.getMeta().containsKey("book"));
   }

   public void testMapContainsAddedNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),2);
      Book book = new Book("Refer collection","1");
      dbHolder.getMeta().put("book",book);
      Book book2 = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getMeta().containsValue(book2));
   }

   public void testMapContainsLongNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      Assert.assertFalse(dbHolder.getMeta().containsValue(new Book("Refer collection","1")));
   }
   
   public void testMapContainsLongExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      Book book = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getMeta().containsValue(book));
   }
   
   public void testMapContainsLongExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      Book book = new Book("Refer collection","1");
      holder.getMeta().put("book",book);
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      Assert.assertTrue(dbHolder.getMeta().containsValue(book));
      Assert.assertTrue(dbHolder.getMeta().containsKey("book"));
   }

   public void testMapContainsLongAddedExistingSame()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),50);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      Book book2 = new Book("Refer collection","1");
      getStore().save(book2);
      Assert.assertFalse(dbHolder.getMeta().containsValue(book2));
      Assert.assertFalse(dbHolder.getMeta().containsKey("book2"));
   }

   public void testMapContainsLongAddedExistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),50);
      Book book = new Book("Refer collection","1");
      getStore().save(book);
      dbHolder.getMeta().put("book",book);
      Book book2 = new Book("Refer collection","1");
      getStore().save(book2);
      Assert.assertFalse(dbHolder.getMeta().containsValue(book2));
      Assert.assertFalse(dbHolder.getMeta().containsKey("book2"));
   }

   public void testMapContainsLongAddedNonexistingSame()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),50);
      Book book = new Book("Refer collection","1");
      dbHolder.getMeta().put("book",book);
      Assert.assertTrue(dbHolder.getMeta().containsValue(book));
      Assert.assertTrue(dbHolder.getMeta().containsKey("book"));
   }

   public void testMapContainsLongAddedNonexistingEquals()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),50);
      Book book = new Book("Refer collection","1");
      dbHolder.getMeta().put("book",book);
      Book book2 = new Book("Refer collection","1");
      Assert.assertFalse(dbHolder.getMeta().containsValue(book2));
      Assert.assertFalse(dbHolder.getMeta().containsKey("book2"));
   }

   public void testMapAddExistingTwiceToNewMap()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getMeta().put("book",book);
      holder.getMeta().put("book",book);
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
   }

   public void testMapsEqualCurrentToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      MapHolder dbHolder2 = (MapHolder) getStore().findSingle("find mapholder");
      // Check
      Assert.assertEquals(dbHolder2.getMeta(),dbHolder.getMeta());
      Assert.assertEquals(dbHolder.getMeta(),dbHolder2.getMeta());
   }

   public void testMapsEqualObsoleteToCurrentEqual()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      MapHolder dbHolder2 = (MapHolder) getStore().findSingle("find mapholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getMeta().put("modBook",modBook);
      getStore().save(dbHolder);
      dbHolder.getMeta().remove("modBook");
      getStore().save(dbHolder);
      // Check
      Assert.assertEquals(dbHolder2.getMeta(),dbHolder.getMeta());
      Assert.assertEquals(dbHolder.getMeta(),dbHolder2.getMeta());
   }

   public void testMapsEqualObsoleteToCurrentNonequal()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      MapHolder dbHolder2 = (MapHolder) getStore().findSingle("find mapholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getMeta().put("modBook",modBook);
      getStore().save(dbHolder);
      // Check
      Assert.assertFalse(dbHolder.getMeta().equals(dbHolder2.getMeta()));
      Assert.assertFalse(dbHolder2.getMeta().equals(dbHolder.getMeta()));
   }

   public void testMapsEqualCurrentToCustomEqual()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      Referrer ref1 = new Referrer(1);
      getStore().save(ref1);
      Referrer ref2 = new Referrer(2);
      getStore().save(ref2);
      Book book1 = new Book("Refer collection","1");
      getStore().save(book1);
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref1",ref1);
      holder.getMeta().put("Ref2",ref2);
      holder.getMeta().put("book1",book1);
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      // Make custom which equal
      HashMap v = new HashMap();
      v.put("Ref1",ref1);
      v.put("Ref2",ref2);
      v.put("book1",book1);
      // Check
      Assert.assertEquals(v,dbHolder.getMeta());
   }

   public void testMapsEqualCurrentToCustomNonequal()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      Referrer ref1 = new Referrer(1);
      getStore().save(ref1);
      Referrer ref2 = new Referrer(2);
      getStore().save(ref2);
      Book book1 = new Book("Refer collection","1");
      getStore().save(book1);
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref1",ref1);
      holder.getMeta().put("Ref2",ref2);
      holder.getMeta().put("book1",book1);
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      // Make custom which equal
      HashMap v = new HashMap();
      v.put("Ref1",ref1);
      v.put("Ref2",ref2);
      // Check
      Assert.assertFalse(dbHolder.getMeta().equals(v));
   }

   public void testMapsEqualsLongCurrentToCurrent()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      MapHolder dbHolder2 = (MapHolder) getStore().findSingle("find mapholder");
      // Check
      Assert.assertEquals(dbHolder2.getMeta(),dbHolder.getMeta());
      Assert.assertEquals(dbHolder.getMeta(),dbHolder2.getMeta());
   }

   public void testMapsEqualsLongObsoleteToCurrentEqual()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      MapHolder dbHolder2 = (MapHolder) getStore().findSingle("find mapholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getMeta().put("modBook",modBook);
      getStore().save(dbHolder);
      Assert.assertTrue(dbHolder.getMeta().containsKey("modBook"));
      Assert.assertTrue(dbHolder.getMeta().get("modBook")!=null);
      dbHolder.getMeta().remove("modBook");
      getStore().save(dbHolder);
      Assert.assertFalse(dbHolder.getMeta().containsKey("modBook"));
      // Check
      Assert.assertEquals(dbHolder2.getMeta(),dbHolder.getMeta());
      Assert.assertEquals(dbHolder.getMeta(),dbHolder2.getMeta());
   }

   public void testMapsEqualsLongObsoleteToCurrentNonequal()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      MapHolder dbHolder2 = (MapHolder) getStore().findSingle("find mapholder");
      // Make obsolete
      Book modBook = new Book("Modification Book","2");
      dbHolder.getMeta().put("modBook",modBook);
      getStore().save(dbHolder);
      // Check
      Assert.assertFalse(dbHolder.getMeta().equals(dbHolder2.getMeta()));
      Assert.assertFalse(dbHolder2.getMeta().equals(dbHolder.getMeta()));
   }

   public void testMapRemoveNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      dbHolder.getMeta().remove("noone");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
   }

   public void testMapRemoveExisting()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      Book book = new Book("Noone's Land","1");
      getStore().save(book);
      dbHolder.getMeta().remove("book");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
   }

   public void testMapRemoveMember()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getMeta().put("book",book);
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      dbHolder.getMeta().remove("book");
      Assert.assertEquals(dbHolder.getMeta().size(),2);
      getStore().save(dbHolder);
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),2);
   }

   public void testMapRemoveLongNonexisting()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      dbHolder.getMeta().remove("book");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
   }

   public void testMapRemoveLongExisting()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      dbHolder.getMeta().remove("book");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
   }

   public void testMapRemoveLongMember()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      Book book = new Book("Noone's Land","1");
      holder.getMeta().put("book",book);
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      dbHolder.getMeta().remove("book");
      Assert.assertEquals(dbHolder.getMeta().size(),50);
      getStore().save(dbHolder);
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),50);
   }

   public void testMapRemoveLongOutside()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Insert mixed stuff
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      Book book = new Book("Noone's Land","1");
      holder.getMeta().put("book",book);
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),51);
      getStore().remove(book);
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),50);
   }

   public void testMapIterationRemove()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      // Iterate
      Iterator iterator = dbHolder.getMeta().entrySet().iterator();
      while ( iterator.hasNext() )
      {
         iterator.next();
         iterator.remove();
      }
      getStore().save(dbHolder);
      // Check
      dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),0);
   }

   public void testMapIterationNormal()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      // Iterate
      Assert.assertEquals(dbHolder.getMeta().size(),3);
      Iterator iterator = dbHolder.getMeta().entrySet().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testMapIterationRemoved()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      Book book = new Book("Refer collection","1");
      holder.getMeta().put("book",book);
      getStore().save(holder);
      // Get and add a nonexisting object too
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().remove("book");
      // Iterate
      Assert.assertEquals(dbHolder.getMeta().size(),2);
      Iterator iterator = dbHolder.getMeta().entrySet().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testMapIterationAdded()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ref"+1,new Referrer(1));
      holder.getMeta().put("Ref"+2,new Referrer(2));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().put("secchance",new Book("Second chance","2"));
      // Iterate
      Assert.assertEquals(dbHolder.getMeta().size(),4);
      Iterator iterator = dbHolder.getMeta().entrySet().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testMapIterationAddedLong()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Add empty map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<50; i++ )
         holder.getMeta().put("Ref"+i,new Referrer(i));
      holder.getMeta().put("refcol",new Book("Refer collection","1"));
      getStore().save(holder);
      // Get and add a nonexisting object too
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().put("secchance",new Book("Second chance","2"));
      // Iterate
      Assert.assertEquals(dbHolder.getMeta().size(),52);
      Iterator iterator = dbHolder.getMeta().entrySet().iterator();
      while ( iterator.hasNext() )
         iterator.next();
   }

   public void testMapPrimitives()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Save
      MapHolder holder = new MapHolder();
      HashMap content = new HashMap();
      holder.setMeta(content);
      content.put("a",new Book("I Am Surrounded By Primitives","1-1-1-1"));
      content.put("b","Primitive String");
      content.put("c",new Integer(1));
      content.put("d",new Short((short) 2));
      content.put("e",new Byte((byte) 3));
      content.put("f",new Long(4));
      content.put("g",new Float(5.5));
      content.put("h",new Double(6.6));
      content.put("i",new Character('a'));
      content.put("j",new Boolean(true));
      content.put("k",new Date());
      getStore().save(holder);
      // Recall
      MapHolder result = (MapHolder) getStore().findSingle("find mapholder");
      Collection map = result.getMeta().values();
      Assert.assertEquals(getCount(map,Book.class),1);
      Assert.assertEquals(getCount(map,String.class),1);
      Assert.assertEquals(getCount(map,Short.class),1);
      Assert.assertEquals(getCount(map,Byte.class),1);
      Assert.assertEquals(getCount(map,Long.class),1);
      Assert.assertEquals(getCount(map,Float.class),1);
      Assert.assertEquals(getCount(map,Double.class),1);
      Assert.assertEquals(getCount(map,Character.class),1);
      Assert.assertEquals(getCount(map,Boolean.class),1);
      Assert.assertEquals(getCount(map,Date.class),1);
   }

   public void testContainsPrimitiveSmall()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Create map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("r1",new Referrer(1));
      holder.getMeta().put("r2",new Referrer(2));
      holder.getMeta().put("r3",new Referrer(3));
      holder.getMeta().put("r4",new Referrer(4));
      holder.getMeta().put("r5",new StringBuffer("String Ni").toString());
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),5);
      Assert.assertTrue(dbHolder.getMeta().containsValue("String Ni"));
      Assert.assertFalse(dbHolder.getMeta().containsValue("String No"));
   }

   public void testContainsPrimitiveNonsavedSmall()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Create map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("r1",new Referrer(1));
      holder.getMeta().put("r2",new Referrer(2));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      dbHolder.getMeta().put("s1",new StringBuffer("String Ni").toString());
      Assert.assertTrue(dbHolder.getMeta().containsValue("String Ni"));
      Assert.assertFalse(dbHolder.getMeta().containsValue("String No"));
   }

   public void testContainsPrimitiveLarge()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Create object
      Book book = new Book("Second chance","2");
      getStore().save(book);
      // Create map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<20; i++ )
         holder.getMeta().put("r"+i,new Referrer(i));
      holder.getMeta().put("ni",new StringBuffer("String Ni").toString());
      for ( int i=0; i<20; i++ )
         holder.getMeta().put("rr"+i,new Referrer(i));
      getStore().save(holder);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),41);
      Assert.assertTrue(dbHolder.getMeta().containsValue("String Ni"));
      Assert.assertFalse(dbHolder.getMeta().containsValue("String No"));
   }
   
   public void testContainsDeletedSmall()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Create author
      Author author = new Author("Stephenson","Neal");
      getStore().save(author);
      // Create map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("author",author);
      getStore().save(holder);
      // Remove author
      getStore().remove(author);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),0);
      Assert.assertFalse(dbHolder.getMeta().containsValue(author));
   }

   public void testContainsDeletedLarge()
      throws Exception
   {
      // Drop table
      removeAll(MapHolder.class);
      // Create author
      Author author = new Author("Stephenson","Neal");
      getStore().save(author);
      // Create map
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      for ( int i=0; i<40; i++ )
         holder.getMeta().put("ref"+i,new Referrer(i));
      holder.getMeta().put("author",author);
      getStore().save(holder);
      // Remove author
      getStore().remove(author);
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),40);
      Assert.assertFalse(dbHolder.getMeta().containsValue(author));
   }

   public void testMultipleOperationsInTransaction()
      throws Exception
   {
      // Drop
      removeAll(MapHolder.class);
      // Create tx and save many times
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         MapHolder holder = new MapHolder();
         holder.setMeta(new HashMap());
         holder.getMeta().put("1","Ni");
         holder.getMeta().put("2","Nu");
         getStore().save(holder);
         holder.getMeta().put("3","Nana");
         holder.getMeta().put("4","Nunu");
         getStore().save(holder);
      } finally {
         tx.commit();
      }
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),4);
   }
   
   public void testMultipleOperationsInSavedTransaction()
      throws Exception
   {
      // Drop
      removeAll(MapHolder.class);
      // Pre-save
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      getStore().save(holder);
      // Reload
      holder = (MapHolder) getStore().findSingle("find mapholder");
      // Create tx and save many times
      Transaction tx = getStore().getTransactionTracker().getTransaction(TransactionTracker.TX_NEW);
      tx.begin();
      try
      {
         holder.getMeta().put("1","Ni");
         holder.getMeta().put("2","Nu");
         getStore().save(holder);
         holder.getMeta().put("3","Nana");
         holder.getMeta().put("4","Nunu");
         getStore().save(holder);
      } finally {
         tx.commit();
      }
      // Check
      MapHolder dbHolder = (MapHolder) getStore().findSingle("find mapholder");
      Assert.assertEquals(dbHolder.getMeta().size(),4);
   }
}

