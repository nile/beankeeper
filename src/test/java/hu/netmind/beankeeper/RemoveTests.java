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
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Test the remove function of Store.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class RemoveTests extends AbstractPersistenceTest
{
   @Test(groups = { "quick" })
   public void testSimpleRemove()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create
      Book b = new Book("Learn brain surgery in 7 days","1-2-3-4");
      getStore().save(b);
      // Get back
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
      // Remove
      getStore().remove(b);
      result = getStore().find("find book");
      Assert.assertEquals(result.size(),0);
      // Insert again
      getStore().save(b);
      result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
   }
   
   public void testRemoveNonExistentObject()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      // Create
      Book b = new Book("Learn brain surgery in 7 days","1-2-3-4");
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),0);
      getStore().remove(b);
      result = getStore().find("find book");
      Assert.assertEquals(result.size(),0);
   }

   public void testRemoveReferencedObject()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      removeAll(Author.class);
      // Create
      Book b = new Book("Learn brain surgery in 7 days","1-2-3-4");
      Author a = new Author("Freddy","Krueger");
      b.setMainAuthor(a);
      // Save
      getStore().save(b);
      // Now remove
      getStore().remove(a);
      // Check
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),b);
      Assert.assertNull(((Book) result.get(0)).getMainAuthor());
   }

   public void testRemoveListedObject()
      throws Exception
   {
      // Drop
      removeAll(Book.class);
      removeAll(Author.class);
      // Create
      Book b = new Book("Learn brain surgery in 7 days","1-2-3-4");
      Author a = new Author("Freddy","Krueger");
      ArrayList v = new ArrayList();
      v.add(a);
      b.setAuthors(v);
      // Save
      getStore().save(b);
      // Now remove
      getStore().remove(a);
      // Check
      List result = getStore().find("find book");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(result.get(0),b);
      Assert.assertEquals(((Book) result.get(0)).getAuthors().size(),0);
   }
}

