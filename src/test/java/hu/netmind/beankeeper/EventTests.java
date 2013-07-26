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
import hu.netmind.beankeeper.event.*;
import hu.netmind.beankeeper.type.*;
import hu.netmind.beankeeper.type.impl.SetImpl;
import hu.netmind.beankeeper.store.event.*;
import hu.netmind.beankeeper.type.event.*;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Tests of all events and of the event dispatcher.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class EventTests extends AbstractPersistenceTest
{
   private PersistenceEvent getEvent(List<PersistenceEvent> events, Class type)
   {
      for ( PersistenceEvent event : events )
      {
         if ( type.isAssignableFrom(event.getClass()) )
            return event;
      }
      return null;
   }

   public void testCreateObject()
      throws Exception
   {
      removeAll(Book.class);
      // Create book
      Book book = new Book();
      book.setTitle("No worries left.");
      book.setIsbn("1-2-3-4");
      // Insert listener and save in store
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      getStore().save(book);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),CreateObjectEvent.class),1);
      CreateObjectEvent event = (CreateObjectEvent) 
         getEvent(listener.getEvents(),CreateObjectEvent.class);
      Assert.assertEquals(event.getObject(),book);
   }
   
   public void testModifyObject()
      throws Exception
   {
      removeAll(Book.class);
      // Create book
      Book book = new Book();
      book.setTitle("No worries left.");
      book.setIsbn("1-2-3-4");
      getStore().save(book);
      book.setTitle("Altered title.");
      // Insert listener and save in store
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      getStore().save(book);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),ModifyObjectEvent.class),1);
      ModifyObjectEvent event = (ModifyObjectEvent) 
         getEvent(listener.getEvents(),ModifyObjectEvent.class);
      Assert.assertEquals(event.getObject(),book);
   }
   
   public void testDeleteObject()
      throws Exception
   {
      removeAll(Book.class);
      // Create book
      Book book = new Book();
      book.setTitle("No worries left.");
      book.setIsbn("1-2-3-4");
      getStore().save(book);
      // Insert listener and save in store
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      getStore().remove(book);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),DeleteObjectEvent.class),1);
      DeleteObjectEvent event = (DeleteObjectEvent) 
         getEvent(listener.getEvents(),DeleteObjectEvent.class);
      Assert.assertEquals(event.getObject(),book);
   }

   public void testMapClearedEvent()
      throws Exception
   {
      removeAll(MapHolder.class);
      // Create mapholder
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ni",new Referrer(1));
      holder.getMeta().put("Ni2",new Referrer(2));
      getStore().save(holder);
      // Select
      holder = (MapHolder) getStore().findSingle("find mapholder");
      // Register
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      // Do
      holder.getMeta().clear();
      getStore().save(holder);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),ClearedContainerEvent.class),1);
      ClearedContainerEvent event = (ClearedContainerEvent) 
         getEvent(listener.getEvents(),ClearedContainerEvent.class);
      Assert.assertEquals(event.getObject(),holder);
      Assert.assertEquals(event.getAttributeName(),"meta");
   }
   
   public void testListClearedEvent()
      throws Exception
   {
      removeAll(ListHolder.class);
      // Create listholder
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      getStore().save(holder);
      // Select
      holder = (ListHolder) getStore().findSingle("find listholder");
      // Register
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      // Do
      holder.getList().clear();
      getStore().save(holder);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),ClearedContainerEvent.class),1);
      ClearedContainerEvent event = (ClearedContainerEvent) 
         getEvent(listener.getEvents(),ClearedContainerEvent.class);
      Assert.assertEquals(event.getObject(),holder);
      Assert.assertEquals(event.getAttributeName(),"list");
   }
   
   public void testSetClearedEvent()
      throws Exception
   {
      removeAll(SetHolder.class);
      // Create setholder
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      getStore().save(holder);
      // Select
      holder = (SetHolder) getStore().findSingle("find setholder");
      // Register
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      // Do
      holder.getSet().clear();
      getStore().save(holder);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),ClearedContainerEvent.class),1);
      ClearedContainerEvent event = (ClearedContainerEvent) 
         getEvent(listener.getEvents(),ClearedContainerEvent.class);
      Assert.assertEquals(event.getObject(),holder);
      Assert.assertEquals(event.getAttributeName(),"set");
   }

   public void testMapAddItemEvent()
      throws Exception
   {
      removeAll(MapHolder.class);
      // Create mapholder
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ni",new Referrer(1));
      holder.getMeta().put("Ni2",new Referrer(2));
      getStore().save(holder);
      // Select
      holder = (MapHolder) getStore().findSingle("find mapholder");
      // Register
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      // Do
      Referrer ref =  new Referrer(3);
      holder.getMeta().put("Ni3",ref);
      getStore().save(holder);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),AddedItemEvent.class),1);
      AddedItemEvent event = (AddedItemEvent) 
         getEvent(listener.getEvents(),AddedItemEvent.class);
      Assert.assertEquals(event.getObject(),holder);
      Assert.assertEquals(event.getAttributeName(),"meta");
      Assert.assertEquals(((Map.Entry)event.getItem()).getKey(),"Ni3");
      Assert.assertEquals(((Map.Entry)event.getItem()).getValue(),ref);
   }
   
   public void testListAddItemEvent()
      throws Exception
   {
      removeAll(ListHolder.class);
      // Create listholder
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      getStore().save(holder);
      // Select
      holder = (ListHolder) getStore().findSingle("find listholder");
      // Register
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      // Do
      Referrer ref = new Referrer(3);
      holder.getList().add(ref);
      getStore().save(holder);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),AddedItemEvent.class),1);
      AddedItemEvent event = (AddedItemEvent) 
         getEvent(listener.getEvents(),AddedItemEvent.class);
      Assert.assertEquals(event.getObject(),holder);
      Assert.assertEquals(event.getAttributeName(),"list");
      Assert.assertEquals(event.getItem(),ref);
   }
   
   public void testSetAddedItemEvent()
      throws Exception
   {
      removeAll(SetHolder.class);
      // Create setholder
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      getStore().save(holder);
      // Select
      holder = (SetHolder) getStore().findSingle("find setholder");
      Assert.assertEquals(holder.getSet().getClass(),SetImpl.class);
      // Register
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      // Do
      Referrer ref = new Referrer(3);
      holder.getSet().add(ref);
      getStore().save(holder);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),AddedItemEvent.class),1);
      AddedItemEvent event = (AddedItemEvent) 
         getEvent(listener.getEvents(),AddedItemEvent.class);
      Assert.assertEquals(event.getObject(),holder);
      Assert.assertEquals(event.getAttributeName(),"set");
      Assert.assertEquals(event.getItem(),ref);
   }

   public void testMapRemoveItemEvent()
      throws Exception
   {
      removeAll(MapHolder.class);
      // Create mapholder
      MapHolder holder = new MapHolder();
      holder.setMeta(new HashMap());
      holder.getMeta().put("Ni",new Referrer(1));
      holder.getMeta().put("Ni2",new Referrer(2));
      Referrer ref =  new Referrer(3);
      holder.getMeta().put("Ni3",ref);
      getStore().save(holder);
      // Select
      holder = (MapHolder) getStore().findSingle("find mapholder");
      // Register
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      // Do
      holder.getMeta().remove("Ni3");
      getStore().save(holder);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),RemovedItemEvent.class),1);
      RemovedItemEvent event = (RemovedItemEvent) 
         getEvent(listener.getEvents(),RemovedItemEvent.class);
      Assert.assertEquals(event.getObject(),holder);
      Assert.assertEquals(event.getAttributeName(),"meta");
      Assert.assertEquals(((Map.Entry)event.getItem()).getKey(),"Ni3");
      Assert.assertEquals(((Map.Entry)event.getItem()).getValue(),ref);
   }
   
   public void testListRemoveItemEvent()
      throws Exception
   {
      removeAll(ListHolder.class);
      // Create listholder
      ListHolder holder = new ListHolder();
      holder.setList(new ArrayList());
      holder.getList().add(new Referrer(1));
      holder.getList().add(new Referrer(2));
      Referrer ref = new Referrer(3);
      holder.getList().add(ref);
      getStore().save(holder);
      // Select
      holder = (ListHolder) getStore().findSingle("find listholder");
      // Register
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      // Do
      holder.getList().remove(ref);
      getStore().save(holder);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),RemovedItemEvent.class),1);
      RemovedItemEvent event = (RemovedItemEvent) 
         getEvent(listener.getEvents(),RemovedItemEvent.class);
      Assert.assertEquals(event.getObject(),holder);
      Assert.assertEquals(event.getAttributeName(),"list");
      Assert.assertEquals(event.getItem(),ref);
   }
   
   public void testSetRemovedItemEvent()
      throws Exception
   {
      removeAll(SetHolder.class);
      // Create setholder
      SetHolder holder = new SetHolder();
      holder.setSet(new HashSet());
      holder.getSet().add(new Referrer(1));
      holder.getSet().add(new Referrer(2));
      Referrer ref = new Referrer(3);
      holder.getSet().add(ref);
      getStore().save(holder);
      // Select
      holder = (SetHolder) getStore().findSingle("find setholder");
      // Register
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      // Do
      holder.getSet().remove(ref);
      getStore().save(holder);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),RemovedItemEvent.class),1);
      RemovedItemEvent event = (RemovedItemEvent) 
         getEvent(listener.getEvents(),RemovedItemEvent.class);
      Assert.assertEquals(event.getObject(),holder);
      Assert.assertEquals(event.getAttributeName(),"set");
      Assert.assertEquals(event.getItem(),ref);
   }

   public void testModifyObjectFeatures()
      throws Exception
   {
      removeAll(Book.class);
      // Create book
      Author author = new Author("Me","Very");
      Book book = new Book();
      book.setTitle("No worries left.");
      book.setIsbn("1-2-3-4");
      book.setMainAuthor(author);
      getStore().save(book);
      Author author2 = new Author("Me","Not");
      book.setTitle("Altered title.");
      book.setMainAuthor(author2);
      // Insert listener and save in store
      MemorizerListener listener = new MemorizerListener();
      getStore().getEventDispatcher().registerListener(listener);
      getStore().save(book);
      // Check
      Assert.assertEquals(getCount(listener.getEvents(),ModifyObjectEvent.class),1);
      ModifyObjectEvent event = (ModifyObjectEvent) 
         getEvent(listener.getEvents(),ModifyObjectEvent.class);
      Assert.assertEquals(event.getObject(),book);
      Assert.assertTrue(event.isOriginalValue("mainauthor",author));
      Book originalBook = (Book) event.getOriginalObject();
      Assert.assertEquals(originalBook.getTitle(),"No worries left.");
      Assert.assertEquals(originalBook.getMainAuthor(),author);
   }
   
   public class MemorizerListener implements PersistenceEventListener
   {
      private List events = new ArrayList();

      public List getEvents()
      {
         return events;
      }
      
      public void handle(PersistenceEvent event)
      {
         events.add(event);
      }
   }
}

