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
 * This listener mimics JUnit output of dots and
 * letters to indicate individual test results.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class DotterListener implements ITestListener
{
   private static final int LINE_SIZE = 50;
   private StringBuffer outputLine = new StringBuffer();

   public void onStart(ITestContext context)
   {
      System.out.println("Starting test: "+context.getName());
   }

   public void onFinish(ITestContext context)
   {
      if ( outputLine.length() > 0 )
         System.out.println(outputLine);
      System.out.println("Finished suite.");
   }

   public void onTestFailedButWithinSuccessPercentage(ITestResult result)
   {
      onTestSuccess(result);
   }

   public void onTestStart(ITestResult result)
   {
   }

   public void onTestFailure(ITestResult result)
   {
      output("F");
   }

   public void onTestSkipped(ITestResult result)
   {
      output("s");
   }

   public void onTestSuccess(ITestResult result)
   {
      output(".");
   }

   private void output(String sign)
   {
      outputLine.append(sign);
      if ( outputLine.length() >= LINE_SIZE )
      {
         System.out.println(outputLine);
         outputLine = new StringBuffer();
      }
   }
}


