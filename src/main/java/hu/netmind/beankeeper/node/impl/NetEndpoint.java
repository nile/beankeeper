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

package hu.netmind.beankeeper.node.impl;

import hu.netmind.beankeeper.common.StoreException;
import java.net.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * A common implementation of an endpoint that handles socket communication
 * outgoing and incoming objects. The incoming messages are handled by
 * multiple threads.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class NetEndpoint
{
   private static Logger logger = Logger.getLogger(NetEndpoint.class);
   private static final int PROCESSOR_IDLE_TTL = 10*60*1000;
   private static final int RESPONSE_TTL = 60*1000;

   private Object processorMutex = new Object();
   private int processorCount = 0;
   private Socket socket;
   private ObjectOutputStream oOutput;
   private ObjectInputStream oInput;
   private boolean connected = true;
   private List<CommObject> incomingQueue = new LinkedList<CommObject>();
   private List<CommResponse> responseQueue = new LinkedList<CommResponse>();
   private List<Long> responseQueueDates = new LinkedList<Long>();
   private String threadNamePrefix = "";

   /**
    * Create the object with a socket that will be handled.
    */
   public NetEndpoint(Socket socket, String threadNamePrefix)
   {
      this.socket=socket;
      this.threadNamePrefix=threadNamePrefix;
      // Establish streams and listener
      try
      {
         // Streams
         oOutput = new ObjectOutputStream(socket.getOutputStream());
         oInput = new ObjectInputStream(socket.getInputStream());
         // The listener will get new messages and insert them into
         // the incoming queue.
         Thread listenerThread = new Thread(new IncomingHandler());
         listenerThread.setName(threadNamePrefix+" - Incoming");
         listenerThread.setDaemon(true);
         listenerThread.start();
      } catch ( Exception e ) {
         throw new CommException("error establishing net endpoint to: "+socket,e);
      }
   }

   /**
    * Return whether endpoint is supposed to be connected.
    * @return True if endpoint should be connected, false is it should
    * be not yet connected, or it's closed.
    */
   public boolean isConnected()
   {
      return connected;
   }

   /**
    * Close this endpoint and release all resources.
    */
   public void close()
   {
      logger.debug("closing net endpoint");
      if ( ! connected )
         return;
      connected=false;
      try
      {
         // Disconnect
         socket.close();
      } catch ( Exception e ) {
         logger.error("error while closing endpoint",e);
      }
      // Wake up incoming listener
      synchronized ( incomingQueue )
      {
         incomingQueue.notifyAll();
      }
      // Wake up response waiting
      synchronized ( responseQueue )
      {
         responseQueue.notifyAll();
      }
   }

   /**
    * Override this method to handle error conditions in communication.
    */
   public void onError()
   {
   }

   /**
    * Override this method to receive messages. Messages are delivered in
    * a dedicated thread.
    */
   public void onIncoming(CommObject obj)
   {
   }

   /**
    * Send and wait for response.
    */
   public CommResponse sendAndWaitForResponse(CommObject obj)
   {
      send(obj);
      return waitForResponse(obj);
   }

   /**
    * Send a message to peer endpoint without waiting for anwser.
    */
   public void send(CommObject obj)
   {
      try
      {
         if ( logger.isDebugEnabled() )
            logger.debug("sending object: "+obj);
         synchronized ( oOutput )
         {
            oOutput.writeObject(obj);
         }
      } catch ( Exception e ) {
         CommException wrapper = new CommException("communication error while sending: "+obj,e);
         if ( isConnected() )
            onError();
         throw wrapper;
      }
   }

   /**
    * Wait for a response for the given message.
    */
   public CommResponse waitForResponse(CommObject obj)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("waiting for response to object: "+obj);
      try
      {
         synchronized ( responseQueue )
         {
            while ( connected )
            {
               // Search for the appropriate message
               for ( int i=0; i<responseQueue.size(); i++ )
               {
                  CommResponse response = responseQueue.get(i);
                  if ( response.getSessionId() == obj.getSessionId() )
                  {
                     responseQueueDates.remove(i);
                     responseQueue.remove(i);
                     if ( logger.isDebugEnabled() )
                        logger.debug("wait terminated, response: "+response);
                     return response;
                  }
               }
               // If not found, wait for new entries
               responseQueue.wait();
            }
         }
      } catch ( InterruptedException e ) {
         logger.debug("wait interrupted",e);
      }
      throw new CommException("there was no answer from server for message: "+obj);
   }

   /**
    * Handles incoming messages.
    */
   public class IncomingHandler implements Runnable
   {
      public void run()
      {
         try
         {
            CommObject obj = null;
            while ( (connected) && ((obj=(CommObject)oInput.readObject()) != null) )
            {
               if ( logger.isDebugEnabled() )
                  logger.debug("received object: "+obj);
               // Place the message in a queue
               if ( obj instanceof CommResponse )
               {
                  synchronized ( responseQueue )
                  {
                     long currentTime = System.currentTimeMillis();
                     // Check for obsolete entries
                     while ( (!responseQueueDates.isEmpty()) &&
                        (responseQueueDates.get(0) + RESPONSE_TTL < currentTime) )
                     {
                        responseQueue.remove(0);
                        responseQueueDates.remove(0);
                     }
                     // Add response to queue
                     responseQueue.add((CommResponse)obj);
                     responseQueueDates.add(currentTime);
                     responseQueue.notifyAll();
                  }
               } else {
                  synchronized ( incomingQueue )
                  {
                     incomingQueue.add(obj);
                     incomingQueue.notify();
                  }
               }
               // Now see whether we need more threads to handle messages.
               // Note, the message could be at this point already under
               // processing, but it is preferred to have one additional
               // thread than one less than required.
               synchronized ( processorMutex )
               {
                  if ( processorCount <= 0 )
                  {
                     Thread processingThread = new Thread(new ProcessingHandler());
                     processingThread.setName(threadNamePrefix+" - Processor");
                     processingThread.start();
                     processorCount++;
                  }
               }
            }
         } catch ( Exception e ) {
            // If not called from disconnect() then call error handler
            if ( isConnected() )
            {
               logger.warn("error while listening for incoming messages, probably peer closed",e);
               close();
               onError();
            } else {
               logger.debug("endpoint was disconnected: "+e.getMessage());
            }
         }
      }
   }

   /**
    * Process incoming objects.
    */
   public class ProcessingHandler implements Runnable
   {
      public void run()
      {
         try
         {
            while ( connected )
            {
               // Get a message from the queue
               CommObject obj = null;
               synchronized ( incomingQueue )
               {
                  while ( (connected) && (incomingQueue.isEmpty()) )
                     incomingQueue.wait(PROCESSOR_IDLE_TTL);
                  if ( incomingQueue.isEmpty() )
                  {
                     // This means either we are disconnected, or were idle
                     return; // So exit
                  }
                  obj = incomingQueue.remove(0);
               }
               // Indicate less processors for now
               synchronized ( processorMutex )
               {
                  processorCount--;
               }
               // Process message
               try
               {
                  onIncoming(obj);
               } catch ( StoreException e ) {
                  logger.error("processing handler threw",e);
               } finally {
                  // Indicate processor as back to pool
                  synchronized ( processorMutex )
                  {
                     processorCount++;
                  }
               }
            }
         } catch ( InterruptedException e ) {
            logger.debug("endpoint processor interrupted, exiting",e);
         } finally {
            // Finally remove from free processor count
            synchronized ( processorMutex )
            {
               processorCount--;
            }
         }
      }
   }

}


