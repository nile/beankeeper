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

package hu.netmind.beankeeper.object;

/**
 * Represents a persistent object identifier. The identifier
 * consists of two parts, one identifies the class the object
 * belongs to, and the rest is for unique identification in
 * that class.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class Identifier
{
   private Long id;
   private Integer classId;

   /**
    * Construct with the two parts given.
    * @param classId The class' id the object belongs to.
    * @param partId The identifier <strong>inside</strong> the class.
    */
   public Identifier(Integer classId, Long partId)
   {
      this.classId=classId;
      this.id = new Long((((long)classId.intValue())<<45)+partId.longValue());
   }

   /**
    * Construct with the full identifier.
    */
   public Identifier(Long id)
   {
      this.id=id;
      this.classId = new Integer((int) (id.longValue()>>45));
   }

   /**
    * Get the class id only.
    */
   public Integer getClassId()
   {
      return classId;
   }

   /**
    * Get the full id this object represents.
    */
   public Long getId()
   {
      return id;
   }

   public String toString()
   {
      return "Id: "+id+" ("+classId+")";
   }
}


