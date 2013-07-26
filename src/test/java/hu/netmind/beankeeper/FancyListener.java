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

package hu.netmind.beankeeper;

import org.testng.ITestListener;
import org.testng.ITestContext;
import org.testng.ITestResult;

/**
 * This is a fancy TestNG listener to print test
 * progress to console using ANSI codes and whatnot.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class FancyListener implements ITestListener
{
   private static final int COL_FIRST = 20;
   private static final int COL_LAST = 70;

   private int column = COL_LAST;

   public void onStart(ITestContext context)
   {
      System.out.println("Starting suite: [1m"+context.getName()+"[0m");
   }

   public void onFinish(ITestContext context)
   {
      System.out.println("Finished suite.[K");
   }

   public void onTestFailedButWithinSuccessPercentage(ITestResult result)
   {
      onTestSuccess(result);
   }

   public void onTestStart(ITestResult result)
   {
      System.out.println("["+COL_FIRST+"G"+result.getName()+"[K");
   }

   private String getPosition()
   {
      String up = null;
      if ( column >= COL_LAST )
      {
         column = COL_FIRST;
         up = "[1A["+column+"G"; // Move 1 up to column
      } else {
         up = "[2A["+column+"G"; // Move 2 up to column
         column++;
      }
      return up;
   }

   public void onTestFailure(ITestResult result)
   {
      System.out.println(getPosition()+"[31mF[0m[K");
   }

   public void onTestSkipped(ITestResult result)
   {
      System.out.println(getPosition()+"[33ms[0m[K");
   }

   public void onTestSuccess(ITestResult result)
   {
      System.out.println(getPosition()+"[32m.[0m[K");
   }
}


