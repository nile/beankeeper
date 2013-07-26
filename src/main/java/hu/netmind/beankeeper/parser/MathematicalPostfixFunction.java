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

package hu.netmind.beankeeper.parser;

/**
 * This is a mathematical postfix function.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class MathematicalPostfixFunction implements Function
{
   private String function;
   private String operand;

   public MathematicalPostfixFunction(String function, String operand)
   {
      this.function=function;
      this.operand=operand;
   }

   public String getFunction()
   {
      return function;
   }

   public String getOperand()
   {
      return operand;
   }

   public String apply(String referenceTerm)
   {
      return "("+referenceTerm+function+operand+")";
   }
}

