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

/**
 * A movie.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class Movie
{
   private Date startDate;
   private Date endDate;
   private String title;

   public Movie()
   {
   }
   
   public Movie(String title, Date startDate, Date endDate)
   {
      setTitle(title);
      setStartDate(startDate);
      setEndDate(endDate);
   }
   
   public Date getStartDate()
   {
      return startDate;
   }
   public void setStartDate(Date startDate)
   {
      this.startDate=startDate;
   }

   public Date getEndDate()
   {
      return endDate;
   }
   public void setEndDate(Date endDate)
   {
      this.endDate=endDate;
   }

   public String getTitle()
   {
      return title;
   }
   public void setTitle(String title)
   {
      this.title=title;
   }

}


