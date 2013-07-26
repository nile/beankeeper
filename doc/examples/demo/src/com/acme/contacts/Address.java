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

package com.acme.contacts;

public class Address
{
   private String country;
   private String city;
   private int zip;
   private String type;

   public String getType()
   {
      return type;
   }
   public void setType(String type)
   {
      this.type=type;
   }

   public String getCountry()
   {
      return country;
   }
   public void setCountry(String country)
   {
      this.country=country;
   }

   public String getCity()
   {
      return city;
   }
   public void setCity(String city)
   {
      this.city=city;
   }

   public int getZip()
   {
      return zip;
   }
   public void setZip(int zip)
   {
      this.zip=zip;
   }

   public String toString()
   {
      return " - "+country+"/"+city+"("+zip+")";
   }

}


