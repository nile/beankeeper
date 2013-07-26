/**
 * Copyright (C) 2009 NetMind Consulting Bt.
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
import java.util.Arrays;

/**
 * This is a task object that has tags.
 */
public class Task
{
   private List<String> tags;
   private String name;
   private Project project;

   public Task()
   {
   }

   public Task(String name)
   {
      this.name=name;
   }

   public Task(Project project, String name, String[] tags)
   {
      this.project=project;
      this.name=name;
      this.tags = Arrays.asList(tags);
   }

   public List<String> getTags()
   {
      return tags;
   }
   public void setTags(List<String> tags)
   {
      this.tags=tags;
   }

   public String getName()
   {
      return name;
   }
   public void setName(String name)
   {
      this.name=name;
   }

   public Project getProject()
   {
      return project;
   }
   public void setProject(Project project)
   {
      this.project=project;
   }
}
