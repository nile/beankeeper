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

import java.util.Map;
import java.util.List;
import java.util.Set;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * These methods test the class modification capabilities of
 * the library with dynamic objects. These are all applicable to
 * static class changes too.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class ClassModificationTests extends AbstractPersistenceTest
{
   public void testAllAttributesChanged()
      throws Exception
   {
      DynamicObjectImpl.init();
      removeAll(DynamicObjectImpl.class);
      // Store with some attributes
      getStore().save(new DynamicObjectImpl());
      // Change class runtime, and save
      Map attrs = DynamicObjectImpl.getPersistenceAttributeTypes(null,null);
      attrs.clear();
      attrs.put("other",String.class);
      DynamicObjectImpl obj = new DynamicObjectImpl();
      obj.put("other","value");
      getStore().save(obj);
      // All attributes changed, so previous object should be
      // removed, and last is preserved
      List result = getStore().find("find dynamicobjectimpl");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(((DynamicObjectImpl) result.get(0)).get("other"),"value");
   }

   public void testNumberToString()
      throws Exception
   {
      DynamicObjectImpl.init();
      removeAll(DynamicObjectImpl.class);
      getStore().save(new DynamicObjectImpl());
      // Modify the Integer attribute to String and save one
      Map attrs = DynamicObjectImpl.getPersistenceAttributeTypes(null,null);
      attrs.put("index",String.class);
      DynamicObjectImpl obj = new DynamicObjectImpl();
      obj.put("index","value");
      getStore().save(obj);
      // Try to select with new type
      List result = getStore().find("find dynamicobjectimpl where index = 'value'");
      Assert.assertTrue(result.size()>0);
   }

   public void testStringToNumber()
      throws Exception
   {
      DynamicObjectImpl.init();
      removeAll(DynamicObjectImpl.class);
      getStore().save(new DynamicObjectImpl());
      // Modify the String attribute to Integer and save one
      Map attrs = DynamicObjectImpl.getPersistenceAttributeTypes(null,null);
      attrs.put("name",Integer.class);
      DynamicObjectImpl obj = new DynamicObjectImpl();
      obj.put("name",new Integer(3));
      getStore().save(obj);
      // Try to select with new type
      List result = getStore().find("find dynamicobjectimpl where name = 3");
      Assert.assertTrue(result.size()>0);
   }
   
   public void testPreserveUnchanged()
      throws Exception
   {
      DynamicObjectImpl.init();
      removeAll(DynamicObjectImpl.class);
      DynamicObjectImpl obj1 = new DynamicObjectImpl();
      obj1.put("index",new Integer(5));
      getStore().save(obj1);
      // Delete one attribute and alter the second, preserve index
      Map attrs = DynamicObjectImpl.getPersistenceAttributeTypes(null,null);
      attrs.remove("name");
      attrs.put("male",String.class);
      DynamicObjectImpl obj = new DynamicObjectImpl();
      obj.put("index",new Integer(5));
      getStore().save(obj);
      // Try to select to see the previous is preserved
      List result = getStore().find("find dynamicobjectimpl where index = 5");
      Assert.assertEquals(result.size(),2);
   }

   public void testAttributeDeleted()
      throws Exception
   {
      DynamicObjectImpl.init();
      removeAll(DynamicObjectImpl.class);
      getStore().save(new DynamicObjectImpl());
      // Remove an attribute
      Map attrs = DynamicObjectImpl.getPersistenceAttributeTypes(null,null);
      attrs.remove("male");
      DynamicObjectImpl obj = new DynamicObjectImpl();
      obj.put("name","value");
      getStore().save(obj);
      // Try to select with new type
      List result = getStore().find("find dynamicobjectimpl where name = 'value'");
      Assert.assertTrue(result.size()>0);
   }

   public void testAttributeAdded()
      throws Exception
   {
      DynamicObjectImpl.init();
      removeAll(DynamicObjectImpl.class);
      getStore().save(new DynamicObjectImpl());
      // Add an attribute
      Map attrs = DynamicObjectImpl.getPersistenceAttributeTypes(null,null);
      attrs.put("extra",String.class);
      DynamicObjectImpl obj = new DynamicObjectImpl();
      obj.put("extra","value");
      getStore().save(obj);
      // Try to select with new type
      List result = getStore().find("find dynamicobjectimpl where extra = 'value'");
      Assert.assertTrue(result.size()>0);
   }

   public void testNewPrimitiveAttributes()
      throws Exception
   {
      // TODO: this test can not be executed with dynamic
      // objects, because there are not primitive attributes
      // in a dynamic object, only boxed types.
   }

   public void testContainerModification()
      throws Exception
   {
      DynamicObjectImpl.init();
      removeAll(DynamicObjectImpl.class);
      Map attrs = DynamicObjectImpl.getPersistenceAttributeTypes(null,null);
      attrs.put("authors",Set.class);
      getStore().save(new DynamicObjectImpl());
      // Alter collection type and try again
      attrs.put("authors",List.class);
      getStore().save(new DynamicObjectImpl());
   }

}


