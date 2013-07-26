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

/**
 * Contains reserved words as attributes.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class NameTester
{
   private String select;
   private String on;
   private String table;
   
   public NameTester()
   {
   }

   public String getSelect()
   {
      return select;
   }
   public void setSelect(String select)
   {
      this.select=select;
   }

   public String getOn()
   {
      return on;
   }
   public void setOn(String on)
   {
      this.on=on;
   }

   public String getTable()
   {
      return table;
   }
   public void setTable(String table)
   {
      this.table=table;
   }
   
}


