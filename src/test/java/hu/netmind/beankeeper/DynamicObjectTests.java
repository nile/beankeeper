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

import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Tests dynamic object, which can dynamically alter it's attribute
 * set runtime.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class DynamicObjectTests extends AbstractPersistenceTest
{
   private static Logger logger = Logger.getLogger(DynamicObjectTests.class);
   
   @Test(groups = { "quick" })
   public void testConcept()
      throws Exception
   {
      // Clear
      removeAll(DynamicObjectImpl.class);
      DynamicObjectImpl.init();
      // Create dynamic object, and save it
      DynamicObjectImpl obj = new DynamicObjectImpl();
      obj.put("index",new Integer(1));
      obj.put("name","Jim");
      obj.put("male",Boolean.TRUE);
      getStore().save(obj);
      // Select and check
      DynamicObjectImpl result = (DynamicObjectImpl) getStore().findSingle("find dynamicobjectimpl");
      Assert.assertEquals(result,obj);
   }

   public void testAddAttributeRuntime()
      throws Exception
   {
      // Clear
      removeAll(DynamicObjectImpl.class);
      DynamicObjectImpl.init();
      // Create dynamic object, and save it
      DynamicObjectImpl obj = new DynamicObjectImpl();
      obj.put("index",new Integer(1));
      obj.put("name","Jim");
      obj.put("male",Boolean.TRUE);
      getStore().save(obj);
      // Increase schema, and add again
      DynamicObjectImpl.getPersistenceAttributeTypes(DynamicObjectImpl.class,null).put("addr",String.class);
      DynamicObjectImpl obj2 = new DynamicObjectImpl();
      obj2.put("index",new Integer(2));
      obj2.put("name","Jack");
      obj2.put("male",Boolean.TRUE);
      obj2.put("addr","Here");
      getStore().save(obj2);
      // Select and check
      DynamicObjectImpl result = (DynamicObjectImpl) getStore().findSingle("find dynamicobjectimpl where index=1");
      obj.remove("addr");
      Assert.assertEquals(result,obj);
      result = (DynamicObjectImpl) getStore().findSingle("find dynamicobjectimpl where index=2");
      Assert.assertEquals(result,obj2);
   }
   
   public void testRemoveAttributeRuntime()
      throws Exception
   {
      // Clear
      removeAll(DynamicObjectImpl.class);
      DynamicObjectImpl.init();
      // Create dynamic object, and save it
      DynamicObjectImpl obj = new DynamicObjectImpl();
      obj.put("index",new Integer(1));
      obj.put("name","Jim");
      obj.put("male",Boolean.TRUE);
      getStore().save(obj);
      // Increase schema, and add again
      DynamicObjectImpl.getPersistenceAttributeTypes(DynamicObjectImpl.class,null).remove("male");
      DynamicObjectImpl obj2 = new DynamicObjectImpl();
      obj2.put("index",new Integer(2));
      obj2.put("name","Jack");
      getStore().save(obj2);
      // Select and check
      DynamicObjectImpl result = (DynamicObjectImpl) getStore().findSingle("find dynamicobjectimpl where index=1");
      obj.remove("male");
      Assert.assertEquals(result,obj);
      result = (DynamicObjectImpl) getStore().findSingle("find dynamicobjectimpl where index=2");
      Assert.assertEquals(result,obj2);
   }
   
   public void testDynamicTypes()
      throws Exception
   {
      // Clear
      removeAll(DynamicObjectTypes.class);
      // Create dynamic object, and save it
      DynamicObjectTypes obj = new DynamicObjectTypes();
      obj.put("index",new Integer(1));
      obj.put("name","Jim");
      obj.put("male",Boolean.TRUE);
      Calendar cal = Calendar.getInstance();
      cal.setTime(new Date());
      cal.set(Calendar.MILLISECOND,0);
      Date date = cal.getTime();
      obj.put("birthdate",date);
      obj.put("ttl",new Long(123));
      obj.put("initial",new Character('X'));
      obj.put("iq",new Byte((byte)-1));
      getStore().save(obj);
      // Select and check
      DynamicObjectTypes result = (DynamicObjectTypes) getStore().findSingle("find dynamicobjecttypes");
      logger.debug("birthdate class: "+result.get("birthdate").getClass());
      Assert.assertEquals(result,obj);
   }

   public void testDynamicCaseInsensitive()
      throws Exception
   {
      // Clear
      removeAll(DynamicObjectCase.class);
      // Create dynamic object, and save it
      DynamicObjectCase obj = new DynamicObjectCase();
      obj.put("IndeX",new Integer(1));
      obj.put("Name","Jim");
      getStore().save(obj);
      // Select and check
      DynamicObjectCase result = (DynamicObjectCase) getStore().findSingle("find dynamicobjectcase");
      Assert.assertEquals(result,obj);
      // Check case insensitive queries
      result = (DynamicObjectCase) getStore().findSingle("find dynamicobjectcase where index = 1");
      Assert.assertEquals(result,obj);
      result = (DynamicObjectCase) getStore().findSingle("find dynamicobjectcase where INDEX = 1");
      Assert.assertEquals(result,obj);
      result = (DynamicObjectCase) getStore().findSingle("find dynamicobjectcase where name = 'Jim'");
      Assert.assertEquals(result,obj);
   }

   public void testDynamicClassObjectWithNoName()
      throws Exception
   {
      removeAll(DynamicObjectWithClass.class);
      // Create object
      DynamicObjectWithClass dyn = new DynamicObjectWithClass();
      dyn.setPersistenceDynamicName(null);
      dyn.put("index",new Integer(1));
      dyn.put("name","Jim");
      dyn.put("male",Boolean.TRUE);
      // Save
      getStore().save(dyn);
      // Select
      DynamicObjectWithClass result = (DynamicObjectWithClass)
         getStore().findSingle("find dynamicobjectwithclass");
      Assert.assertEquals(result,dyn);
      Assert.assertNull(dyn.getPersistenceDynamicName());
   }
   
   public void testDynamicClassObjectWithName()
      throws Exception
   {
      removeAll(DynamicObjectWithClass.class,"CarClass");
      removeAll(DynamicObjectWithClass.class);
      // Create object
      DynamicObjectWithClass dyn = new DynamicObjectWithClass();
      dyn.setPersistenceDynamicName("CarClass");
      dyn.put("model","Jaguar");
      dyn.put("doors",new Integer(4));
      dyn.put("index",new Integer(1));
      dyn.put("name","Jim");
      dyn.put("male",Boolean.TRUE);
      // Save
      getStore().save(dyn);
      // Select
      DynamicObjectWithClass result = (DynamicObjectWithClass)
         getStore().findSingle("find carclass");
      Assert.assertEquals(result,dyn);
      Assert.assertEquals(dyn.getPersistenceDynamicName(),"CarClass");
      // Try selection for dynamic class
      result = (DynamicObjectWithClass) 
         getStore().findSingle("find dynamicobjectwithclass");
      Assert.assertEquals(result,dyn);
      Assert.assertEquals(dyn.getPersistenceDynamicName(),"CarClass");
   }
   
   public void testDynamicAttributeWithNoName()
      throws Exception
   {
      removeAll(DynamicAttributeObject.class);
      // Create object
      DynamicAttributeObject obj = new DynamicAttributeObject();
      obj.setIndex(1);
      DynamicObjectWithClass dyn = new DynamicObjectWithClass();
      dyn.setPersistenceDynamicName(null);
      dyn.put("index",new Integer(1));
      dyn.put("name","Jim");
      dyn.put("male",Boolean.TRUE);
      obj.setObj(dyn);
      // Save
      getStore().save(obj);
      // Select
      DynamicAttributeObject result = (DynamicAttributeObject)
         getStore().findSingle("find dynamicattributeobject");
      Assert.assertEquals(result.getIndex(),obj.getIndex());
      Assert.assertNull(result.getObj().getPersistenceDynamicName());
      Assert.assertEquals(result.getObj(),dyn);
   }
   
   public void testDynamicAttributeWithName()
      throws Exception
   {
      removeAll(DynamicAttributeObject.class);
      // Create object
      DynamicAttributeObject obj = new DynamicAttributeObject();
      obj.setIndex(1);
      DynamicObjectWithClass dyn = new DynamicObjectWithClass();
      dyn.setPersistenceDynamicName("CarClass");
      dyn.put("model","Jaguar");
      dyn.put("doors",new Integer(4));
      dyn.put("index",new Integer(1));
      dyn.put("name","Jim");
      dyn.put("male",Boolean.TRUE);
      obj.setObj(dyn);
      // Save
      getStore().save(obj);
      // Select
      DynamicAttributeObject result = (DynamicAttributeObject)
         getStore().findSingle("find dynamicattributeobject");
      Assert.assertEquals(result.getIndex(),obj.getIndex());
      Assert.assertEquals(result.getObj().getPersistenceDynamicName(),"CarClass");
      Assert.assertEquals(result.getObj(),dyn);
   }

   public void testDynamicAttributeMultipleObjects()
      throws Exception
   {
      removeAll(DynamicAttributeObject.class);
      // Create object
      DynamicAttributeObject objs[] = new DynamicAttributeObject[10];
      for ( int i=0; i<10; i++ )
      {
         // Create object
         DynamicAttributeObject obj = new DynamicAttributeObject();
         objs[i]=obj;
         obj.setIndex(i);
         DynamicObjectWithClass dyn = new DynamicObjectWithClass();
         obj.setObj(dyn);
         if ( i % 2 == 0 )
         {
            dyn.setPersistenceDynamicName("CarClass");
            dyn.put("model","Jaguar");
            dyn.put("doors",new Integer(4));
            dyn.put("index",new Integer(2));
            dyn.put("name","Jimbo");
            dyn.put("male",Boolean.TRUE);
         } else {
            dyn.put("index",new Integer(1));
            dyn.put("name","Jim");
            dyn.put("male",Boolean.TRUE);
         }
         // Save
         getStore().save(obj);
      }
      // Select
      List result = getStore().find("find dynamicattributeobject order by index asc");
      // Check
      for ( int i=0; i<10; i++ )
         Assert.assertEquals(((DynamicAttributeObject) result.get(i)).getObj(),objs[i].getObj());
   }

   public void testDynamicListAttribute()
      throws Exception
   {
      // Clear
      removeAll(ListHolder.class);
      // Make object
      ListHolder obj = new ListHolder();
      ArrayList list = new ArrayList();
      DynamicObjectWithClass dyn1 = new DynamicObjectWithClass();
      dyn1.setPersistenceDynamicName("CarClass");
      dyn1.put("model","Jaguar");
      dyn1.put("doors",new Integer(4));
      dyn1.put("index",new Integer(2));
      dyn1.put("name","Jimba");
      dyn1.put("male",Boolean.FALSE);
      DynamicObjectWithClass dyn2 = new DynamicObjectWithClass();
      dyn2.setPersistenceDynamicName(null);
      dyn2.put("index",new Integer(1));
      dyn2.put("name","Jim");
      dyn2.put("male",Boolean.TRUE);
      list.add(dyn1);
      list.add(dyn2);
      obj.setList(list);
      // Insert
      getStore().save(obj);
      // Select
      ListHolder result = (ListHolder) getStore().findSingle("find listholder");
      result.getList().size(); // Force load
      // Check
      logger.debug("listholder result list: "+result.getList());
      Assert.assertTrue(result.getList().contains(dyn1));
      Assert.assertTrue(result.getList().contains(dyn2));
   }

   public void testDynamicMapAttribute()
      throws Exception
   {
      // Clear
      removeAll(MapHolder.class);
      // Make object
      MapHolder obj = new MapHolder();
      HashMap map = new HashMap();
      DynamicObjectWithClass dyn1 = new DynamicObjectWithClass();
      dyn1.setPersistenceDynamicName("CarClass");
      dyn1.put("model","Jaguar");
      dyn1.put("doors",new Integer(4));
      DynamicObjectWithClass dyn2 = new DynamicObjectWithClass();
      dyn2.setPersistenceDynamicName(null);
      dyn2.put("index",new Integer(1));
      dyn2.put("name","Jim");
      dyn2.put("male",Boolean.TRUE);
      map.put("Dynamic 1",dyn1);
      map.put("Dynamic 2",dyn2);
      obj.setMeta(map);
      // Insert
      getStore().save(obj);
      // Select
      MapHolder result = (MapHolder) getStore().findSingle("find mapholder");
      // Check
      Assert.assertEquals(result.getMeta(),map);
   }
   
   public void testDynamicAttributeInSelectWithClassSpec()
      throws Exception
   {
      removeAll(DynamicAttributeObject.class);
      // Create object
      DynamicAttributeObject obj = new DynamicAttributeObject();
      obj.setIndex(1);
      DynamicObjectWithClass dyn = new DynamicObjectWithClass();
      dyn.setPersistenceDynamicName("CarClass");
      dyn.put("model","Jaguar");
      dyn.put("doors",new Integer(4));
      dyn.put("index",new Integer(1));
      dyn.put("name","Jim");
      dyn.put("male",Boolean.TRUE);
      obj.setObj(dyn);
      // Save
      getStore().save(obj);
      // Select
      DynamicAttributeObject result = (DynamicAttributeObject)
         getStore().findSingle("find dynamicattributeobject where dynamicattributeobject.obj(carclass).model='Jaguar'");
      Assert.assertEquals(result.getObj(),dyn);
   }
   
   public void testDynamicAttributeInSelectWithNoClassSpec()
      throws Exception
   {
      removeAll(DynamicAttributeObject.class);
      // Create object
      DynamicAttributeObject obj = new DynamicAttributeObject();
      obj.setIndex(1);
      DynamicObjectWithClass dyn = new DynamicObjectWithClass();
      dyn.setPersistenceDynamicName(null);
      dyn.put("index",new Integer(1));
      dyn.put("name","Jim");
      dyn.put("male",Boolean.TRUE);
      obj.setObj(dyn);
      // Save
      getStore().save(obj);
      // Select
      DynamicAttributeObject result = (DynamicAttributeObject)
         getStore().findSingle("find dynamicattributeobject where dynamicattributeobject.obj.index=1");
      Assert.assertEquals(result.getObj(),dyn);
   }

   public void testDynamicObjectWithStaticAttributes()
      throws Exception
   {
      // Clear
      removeAll(DynamicObjectWithStaticAttributes.class);
      DynamicObjectWithStaticAttributes.init();
      // Create dynamic object, and save it
      DynamicObjectWithStaticAttributes obj = new DynamicObjectWithStaticAttributes();
      obj.put("index",new Integer(1));
      obj.put("name","Jim");
      obj.put("male",Boolean.TRUE);
      obj.setOwnAttribute("something");
      getStore().save(obj);
      // Select and check
      DynamicObjectWithStaticAttributes result = (DynamicObjectWithStaticAttributes) getStore().findSingle("find dynamicobjectwithstaticattributes");
      Assert.assertEquals(result,obj);
      Assert.assertEquals(result.getOwnAttribute(),"something");
   }
   
   public void testSaveMutualReferrers()
      throws Exception
   {
      // Clear
      removeAll(Group.class);
      removeAll(Worker.class);
      // Create objects
      Group group = new Group();
      Worker worker = new Worker();
      worker.put("groups",new ArrayList());
      ((List) worker.get("groups")).add(group);
      group.put("workers",new ArrayList());
      ((List) group.get("workers")).add(worker);
      // Save
      getStore().save(group);
      // Load
      Group loadGroup = (Group) getStore().findSingle("find group");
      Assert.assertEquals(((List) loadGroup.get("workers")).size(),1);
      Worker loadWorker = (Worker) getStore().findSingle("find worker");
      Assert.assertEquals(((List) loadWorker.get("groups")).size(),1);
   }
}

