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
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Overall tests for persistence layer. These are more like proof-of-concept
 * test designed to demonstrate concepts of functionality.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
@Test
public class OverallTests extends AbstractPersistenceTest
{
   @Test(groups = { "quick" })
   public void testInsertAndSelectConcept()
      throws Exception
   {
      removeAll(Book.class);
      // Create book
      Book book = new Book();
      book.setTitle("No worries left.");
      book.setIsbn("1-2-3-4");
      // Save in store
      getStore().save(book);
      // Select
      List result = getStore().find("find book where book.title='No worries left.'");
      Assert.assertEquals(result.size(),1);
      Assert.assertEquals(((Book) result.get(0)).getIsbn(),"1-2-3-4");
      Assert.assertEquals(((Book) result.get(0)).getTitle(),"No worries left.");
   }
   
}

