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

package hu.netmind.beankeeper.serial;

import java.io.Serializable;
import java.util.Date;
import java.util.Calendar;

/**
 * A serial number is a unique number based on time, but it is
 * <strong>not</strong> completely based on time. It has two parts,
 * one defines the millisecond the serial was created in, and the other
 * is a sub-millisecond index that makes it possible to have at 
 * maximum 10.000 distinct serials in every given millisecond.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class Serial implements Serializable
{
   private Long serial;

   /**
    * Construct a serial with it's value.
    */
   public Serial(Long serial)
   {
      this.serial=serial;
   }

   /**
    * Get the serial value.
    */
   public Long getValue()
   {
      return serial;
   }

   /**
    * Get the first serial that is created on 
    * the given date.
    */
   public static Serial getSerial(Date date)
   {
      return new Serial(date.getTime()*10000L);
   }

   /**
    * Get the millisecond this serial was created on.
    */
   public Date getDate()
   {
      return new Date(serial/10000L+1);
   }

   /**
    * Get the maximal serial value possible.
    */
   public static Serial getMaxSerial()
   {
      return new Serial(Long.MAX_VALUE);
   }
}


