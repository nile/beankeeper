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
import java.util.Random;
import java.util.Arrays;
import org.apache.log4j.Logger;

/**
 * A simple object with all supported primitive types.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class Primitives
{
   private static Logger logger = Logger.getLogger(Primitives.class);
   
   private int integerAttr;
   private short shortAttr;
   private long longAttr;
   private char charAttr;
   private byte byteAttr;
   private boolean booleanAttr;
   private double doubleAttr;
   private float floatAttr;

   private byte[] byteArray;

   private Integer integerObjAttr;
   private Short shortObjAttr;
   private Long longObjAttr;
   private Character charObjAttr;
   private Byte byteObjAttr;
   private Boolean booleanObjAttr;
   private Double doubleObjAttr;
   private Float floatObjAttr;

   private String stringAttr;
   private Date dateAttr;

   public Primitives()
   {
   }
   
   public void maximize()
   {
      integerAttr = Integer.MAX_VALUE;
      shortAttr = Short.MAX_VALUE;
      longAttr = Long.MAX_VALUE;
      byteAttr = Byte.MAX_VALUE;
      // floating point is unreliable floatAttr = Float.MAX_VALUE;
      // floating point is unreliable doubleAttr = Double.MAX_VALUE;
      
      byteArray = new byte[256];
      for ( int i=0; i<256; i++ )
         byteArray[i] = Byte.MAX_VALUE;

      integerObjAttr = new Integer(Integer.MAX_VALUE);
      shortObjAttr = new Short(Short.MAX_VALUE);
      longObjAttr = new Long(Long.MAX_VALUE);
      byteObjAttr = new Byte(Byte.MAX_VALUE);
      // floating point is unreliable floatObjAttr = new Float(Float.MAX_VALUE);
      // floating point is unreliable doubleObjAttr = new Double(Double.MAX_VALUE);

      charAttr = 'A';
   }

   public void nullize()
   {
      integerObjAttr = null;
      shortObjAttr = null;
      longObjAttr = null;
      charObjAttr = null;
      byteObjAttr = null;
      booleanObjAttr = null;
      floatObjAttr = null;
      doubleObjAttr = null;
      stringAttr = null;
      dateAttr = null;

      byteArray = null;

      charAttr = 'A';
   }

   public void minimize()
   {
      integerAttr = Integer.MIN_VALUE;
      shortAttr = Short.MIN_VALUE;
      longAttr = Long.MIN_VALUE;
      byteAttr = Byte.MIN_VALUE;
      // floating point is unreliable floatAttr = Float.MIN_VALUE;
      // floating point is unreliable doubleAttr = Double.MIN_VALUE;
      
      byteArray = new byte[256];
      for ( int i=0; i<256; i++ )
         byteArray[i] = Byte.MIN_VALUE;

      integerObjAttr = new Integer(Integer.MIN_VALUE);
      shortObjAttr = new Short(Short.MIN_VALUE);
      longObjAttr = new Long(Long.MIN_VALUE);
      byteObjAttr = new Byte(Byte.MIN_VALUE);
      // floating point is unreliable floatObjAttr = new Float(Float.MIN_VALUE);
      // floating point is unreliable doubleObjAttr = new Double(Double.MIN_VALUE);

      charAttr = 'A';
   }

   public void randomize()
   {
      Random rnd = new Random();

      integerAttr = rnd.nextInt();
      shortAttr = (short) rnd.nextInt();
      longAttr = rnd.nextLong();
      charAttr = 'A';
      byteAttr = (byte) rnd.nextInt();
      booleanAttr = rnd.nextBoolean();
      floatAttr = rnd.nextFloat();
      doubleAttr = rnd.nextDouble();

      byteArray = new byte[rnd.nextInt(2048)];
      for ( int i=0; i<byteArray.length; i++ )
         byteArray[i]=(byte) rnd.nextInt();

      integerObjAttr = new Integer(rnd.nextInt());
      shortObjAttr = new Short((short) rnd.nextInt());
      longObjAttr = new Long(rnd.nextLong());
      charObjAttr = new Character('B');
      byteObjAttr = new Byte((byte) rnd.nextInt());
      booleanObjAttr = new Boolean(rnd.nextBoolean());
      floatObjAttr = new Float(rnd.nextFloat());
      doubleObjAttr = new Double(rnd.nextDouble());

      dateAttr = new Date();
      stringAttr = "Test String";
   }

   public int hashCode()
   {
      return 0; // You could compute a hashcode from properties, but there's no point
   }

   public boolean equals(Object obj)
   {
      if ( ! (obj instanceof Primitives) )
         return false;
      Primitives p = (Primitives) obj;
      return
         ((integerAttr == p.integerAttr) || 
          (notEqual("integerAttr",new Integer(integerAttr),new Integer(p.integerAttr)))) &&
         ((shortAttr == p.shortAttr) || 
          (notEqual("shortAttr",new Short(shortAttr),new Short(p.shortAttr)))) &&
         ((longAttr == p.longAttr) || 
          (notEqual("longAttr",new Long(longAttr),new Long(p.longAttr)))) &&
         ((charAttr == p.charAttr) || 
          (notEqual("charAttr",new Character(charAttr),new Character(p.charAttr)))) &&
         ((byteAttr == p.byteAttr) || 
          (notEqual("byteAttr",new Byte(byteAttr),new Byte(p.byteAttr)))) &&
         ((booleanAttr == p.booleanAttr) || 
          (notEqual("booleanAttr",new Boolean(booleanAttr),new Boolean(p.booleanAttr)))) &&
         // floating point is unstable ((floatAttr == p.floatAttr) || 
            // (notEqual("floatAttr",new Float(floatAttr),new Float(p.floatAttr)))) &&
         // floating point is unstable ((doubleAttr == p.doubleAttr) || 
            // (notEqual("doubleAttr",new Double(doubleAttr),new Double(p.doubleAttr)))) &&

         (( ((byteArray==null) && (p.byteArray==null)) || 
            ((byteArray!=null) && (Arrays.equals(byteArray,p.byteArray))) ) || (notEqual("byteArray",byteArray,p.byteArray))) &&

         (( ((integerObjAttr==null) && (p.integerObjAttr==null)) || 
            ((integerObjAttr!=null) && (integerObjAttr.equals(p.integerObjAttr))) ) || (notEqual("integerObjAttr",integerObjAttr,p.integerObjAttr))) &&
         (( ((shortObjAttr==null) && (p.shortObjAttr==null)) || 
            ((shortObjAttr!=null) && (shortObjAttr.equals(p.shortObjAttr))) ) || (notEqual("shortObjAttr",shortObjAttr,p.shortObjAttr))) &&
         (( ((longObjAttr==null) && (p.longObjAttr==null)) || 
            ((longObjAttr!=null) && (longObjAttr.equals(p.longObjAttr))) ) || (notEqual("longObjAttr",longObjAttr,p.longObjAttr))) &&
         (( ((charObjAttr==null) && (p.charObjAttr==null)) || 
            ((charObjAttr!=null) && (charObjAttr.equals(p.charObjAttr))) ) || (notEqual("charObjAttr",charObjAttr,p.charObjAttr))) &&
         (( ((byteObjAttr==null) && (p.byteObjAttr==null)) || 
            ((byteObjAttr!=null) && (byteObjAttr.equals(p.byteObjAttr))) ) || (notEqual("byteObjAttr",byteObjAttr,p.byteObjAttr))) &&
         (( ((booleanObjAttr==null) && (p.booleanObjAttr==null)) || 
            ((booleanObjAttr!=null) && (booleanObjAttr.equals(p.booleanObjAttr))) ) || (notEqual("booleanObjAttr",booleanObjAttr,p.booleanObjAttr))) &&
         // floating point is unstable (( ((floatObjAttr==null) && (p.floatObjAttr==null)) || 
           // ((floatObjAttr!=null) && (floatObjAttr.equals(p.floatObjAttr))) ) || (notEqual("floatObjAttr",floatObjAttr,p.floatObjAttr))) &&
         // floating point is unstable (( ((doubleObjAttr==null) && (p.doubleObjAttr==null)) || 
           // ((doubleObjAttr!=null) && (doubleObjAttr.equals(p.doubleObjAttr))) ) || (notEqual("doubleObjAttr",doubleObjAttr,p.doubleObjAttr))) &&

         (( ((dateAttr==null) && (p.dateAttr==null)) || 
            ((dateAttr!=null) && (isSameDate(dateAttr,p.dateAttr))) ) || (notEqual("dateAttr",dateAttr,p.dateAttr))) &&
         (( ((stringAttr==null) && (p.stringAttr==null)) || 
            ((stringAttr!=null) && (stringAttr.equals(p.stringAttr))) ) || (notEqual("stringAttr",stringAttr,p.stringAttr)));
   }

   /**
    * Two dates are considered the same, if they match to the second. Milliseconds
    * are not required.
    */
   private boolean isSameDate(Date lhs, Date rhs)
   {
      return Math.abs(lhs.getDate()-rhs.getDate())<1000;
   }

   private boolean notEqual(String attribute, Object v1, Object v2)
   {
      if ( byte[].class.equals(v1.getClass()) )
      {
         logger.debug("equality check failed, the two array lengths are: "+((byte[])v1).length+" vs. "+((byte[])v2).length);
         if ( ((byte[])v1).length == ((byte[])v2).length )
         {
            StringBuffer buf = new StringBuffer("contents are:");
            for ( int i=0; i<((byte[]) v1).length; i++ )
               buf.append(" "+((byte[])v1)[i]+"|"+((byte[])v2)[i]);
            logger.debug(buf.toString());
         }
      }
      throw new RuntimeException("attribute '"+attribute+"' does not equals in primitives class ("+v1+" expected, but was: "+v2+")");
   }
}


