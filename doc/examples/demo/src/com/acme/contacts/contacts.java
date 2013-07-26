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

import hu.netmind.beankeeper.Store;
import java.util.List;
import java.util.Vector;

public class contacts
{
   public static void main(String argv[])
   {
      if ( argv.length == 0 )
         listPersons();
      else if ( "create".equals(argv[0]) )
         createPerson(argv[1],argv[2],argv[3],argv[4],Integer.valueOf(argv[5]).intValue());
   }

   private static void listPersons()
   {
      List persons = StoreUtil.getStore().find("find person");
      System.out.println("Persons: "+persons);
   }

   private static void createPerson(String firstName, String lastName, String country, String city, int zip)
   {
      Address address = new Address();
      address.setCountry(country);
      address.setCity(city);
      address.setZip(zip);
      
      Person person = (Person) StoreUtil.getStore().findSingle(
            "find person where firstname='"+firstName+"' and lastname='"+lastName+"'");
      if ( person == null )
      {
         person = new Person();
         person.setFirstName(firstName);
         person.setLastName(lastName);
      }
      if ( person.getAddresses() == null )
         person.setAddresses(new Vector());
      person.getAddresses().add(address);
         
      StoreUtil.getStore().save(person);
   }
}

