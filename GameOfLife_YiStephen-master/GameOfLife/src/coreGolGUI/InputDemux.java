package coreGolGUI;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.ReentrantLock;

// Combines two streams into one: The original stdin stream and the
// list of "supplemental commands" that get sent by the GUI controls.
// Intentionally not public--only classes in this package should need to use this
class InputDemux extends InputStream
{
	private final ReentrantLock supplementalCommandBufferLock;
	private String supplementalCommandBuffer;
	private InputStream origIn;

	public InputDemux()
	{
		// These allow other components to send us commands to mix into the stream 
		supplementalCommandBuffer = "";
		supplementalCommandBufferLock = new ReentrantLock();
		
		// We now take ownership of stdin
		origIn = System.in;
		System.setIn(this);
	}

	// This runs in the GUI's thread.  GUI calls this to
	// add a supplemental command to the stream
	public void sendSupplementalCommand(String command)
	{
		System.out.println(command);
		supplementalCommandBufferLock.lock();
		try
		{
			supplementalCommandBuffer += (command + "\r\n");
		}
		finally
		{
			supplementalCommandBufferLock.unlock();
		}
	}
	
	// Helper called by read(), which runs in an arbitrary thread trying to
	// read from this (combined) stream.  This helper handles just checking
	// the supplemental command buffer.  If there's a character to get from the
	// supplemental command buffer, it's returned as an int; else -1.
	private int readFromSupplementalBuffer()
	{
		char retAsChar = (char) -1;
		supplementalCommandBufferLock.lock();
		try
		{
			if (supplementalCommandBuffer.isEmpty())
			{
				return -1;
			}

			retAsChar = supplementalCommandBuffer.charAt(0);
			if (supplementalCommandBuffer.length() == 1)
			{
				supplementalCommandBuffer = "";
			}
			else
			{
				supplementalCommandBuffer = supplementalCommandBuffer.substring(1);
			}
		}
		finally
		{
			supplementalCommandBufferLock.unlock();
		}
		return (int) retAsChar;		
	}

	// Runs in an arbitrary thread trying to read from this (combined) stream.
	// Give precedence to supplemental command buffer (sent by GUI); otherwise,
	// just read from the original stdin.
	@Override
	public int read() throws IOException
	{
		// Note: There is technically a bug here:
		// if somehow stdin started to buffer up characters, and we managed
		// to read them before the newline WHILE the supplemental buffer
		// got filled, we could intermix characters from a single stdin
		// line and the supplemental buffer.  I don't think I care, because
		// no one's hands are that fast.  Let's see if I change my mind.
		
		while (true)
		{
			int ret = readFromSupplementalBuffer();
			if (ret != -1)
			{
				return ret;
			}
			
			// Only block on the original stdin if there's something
			// there.  Otherwise, this while loop acts as our blocking, so
			// we can ensure supplemental commands get precedence if they arrive
			if (origIn.available() != 0)
			{
				return origIn.read();
			}
			
			// We've got nothing to offer, so sleep a tad before
			// trying again 
			try
			{
				Thread.sleep(5);
			}
			catch (InterruptedException e)
			{

			}			
		}
	}
	
	// Runs in an arbitrary thread trying to read from this stream.
	// The default implementation blocks unnecessarily rather than reading what
	// it can and returning, which in turn causes Scanner nextLine to never return.
	// Our implementation blocks on the first character (cuz for some reason you
	// have to), but will not block after that--it buffers what it can and then
	// returns.
	@Override
	public int read(byte[] b,
			int off,
			int len)
					throws IOException
	{
		int cBytesRead = 0;
		for (; cBytesRead < len; cBytesRead++)
		{
			// Bail if there's nothing left to read, but ONLY if we've at least read
			// our first byte.  Supposed to try really hard to read at least one byte 
			if (cBytesRead > 0 && available() == 0)
			{
				break;
			}
			b[off + cBytesRead] = (byte) read();
		}
		return cBytesRead;
	}
	
	// TODO: SHould probably override EVERYTHING from InputStream

	// Runs in an arbitrary thread trying to read from this stream.
	// Just counts the characters available from the two streams and returns the sum
	@Override
	public int available() throws IOException
	{
		int ret = 0;
		supplementalCommandBufferLock.lock();
		try
		{
			ret += supplementalCommandBuffer.length();
		}
		finally
		{
			supplementalCommandBufferLock.unlock();
		}
		
		ret += origIn.available();
		return ret;
	}
}