/**
 * Copyright (C) 2008 NetMind Consulting Bt.
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

package hu.netmind.beankeeper.parser;

import java.util.List;
import java.util.ArrayList;

/**
 * This term represents a table entry which is exactly specified
 * with all parameters needed to be included into an sql statement.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class SpecifiedTableTerm extends TableTerm
{
   private List relatedLeftTerms; // To related classes (super-, or sub-classes)
   private List referencedLeftTerms; // To referenced classes (x.y)

   public SpecifiedTableTerm(SpecifiedTableTerm source)
   {
      super(source);
      relatedLeftTerms = new ArrayList(source.getRelatedLeftTerms());
      referencedLeftTerms = new ArrayList(source.getReferencedLeftTerms());
   }

   public SpecifiedTableTerm(TableTerm source)
   {
      super(source);
      relatedLeftTerms = new ArrayList();
      referencedLeftTerms = new ArrayList();
   }

   public SpecifiedTableTerm(String tableName, String alias)
   {
      super(tableName,alias);
      relatedLeftTerms = new ArrayList();
      referencedLeftTerms = new ArrayList();
   }

   public SpecifiedTableTerm deepCopy()
   {
      SpecifiedTableTerm result = new SpecifiedTableTerm(this);
      result.setRelatedLeftTerms(new ArrayList(getRelatedLeftTerms()));
      result.setReferencedLeftTerms(new ArrayList(getReferencedLeftTerms()));
      return result;
   }

   public List getRelatedLeftTerms()
   {
      return relatedLeftTerms;
   }
   public void setRelatedLeftTerms(List relatedLeftTerms)
   {
      this.relatedLeftTerms=relatedLeftTerms;
   }

   public List getReferencedLeftTerms()
   {
      return referencedLeftTerms;
   }
   public void setReferencedLeftTerms(List referencedLeftTerms)
   {
      this.referencedLeftTerms=referencedLeftTerms;
   }

   public String toString()
   {
      return super.toString()+relatedLeftTerms+referencedLeftTerms;
   }

   public static class LeftjoinEntry
   {
      public TableTerm term;
      public Expression expression;

      public String toString()
      {
         return " (join "+term+" on "+expression.toString()+")";
      }
   }
}


