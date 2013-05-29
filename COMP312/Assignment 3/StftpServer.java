import java.net.*;
import java.io.*;

/**
 * Super Trivial File Transfer Protocol Server
 * @date 16 May 2013
 * @author Steven Crake 1117696
 */
class StftpServerWorker extends Thread
{
	private DatagramPacket reqPacket = null;
	private DatagramPacket respPacket = null;
	private DatagramSocket sock = null;
	private static final int REQ     = 3;
	private static final int OK      = 1;
	private static final int DATA    = 2;
	private static final int ACK     = 42;
	private static final int NOTOK   = 0;

	public StftpServerWorker(DatagramPacket req)
	{
		this.reqPacket = req;
	}

	public void run()
	{
		try
		{
			sock = new DatagramSocket();
			sock.setSoTimeout(1000); //1 second timeout

			byte[] data = reqPacket.getData();
			if(data[0] != REQ) //ensure that it is a request
			{
				sendNOTOK();
				return;
			}

			//parse the filename from request packet
			String filename = new String(data, 1, data.length-1);
			filename = filename.trim();

			File file = new File(filename);
			
			System.out.print(reqPacket.getAddress().getHostAddress() + " (" + reqPacket.getAddress().getHostName() + ")" + " requested '" + filename + "' (" + file.length() + " bytes): ");
			
			// if the requested file exists 
			// and doesn't have a slash ( '/' or '\' ) character in the path
			if(file.exists() && !(filename.contains("\\") || filename.contains("/"))) 
			{
				System.out.println("Transferring...");
				//send OK packet
				byte[] respData = new byte[9];
				respData[0] = OK;
				respData = storeLong(respData, 1, file.length());

				respPacket = new DatagramPacket(respData, respData.length, reqPacket.getSocketAddress());
				sock.send(respPacket);

				//send the file block by block
				sendFile(file);
			}
			else //invalid file - doesn't exist or illegal path
			{
				System.out.println("File not sent.");
				sendNOTOK();
			}
		}
		catch(Exception ex)
		{
			System.err.println("Exception: " + ex);
		}
		finally
		{
			sock.close(); //close the DatagramSocket
		}
	}

	/**
	 * Open the file and send it to the client one block (1463 bytes) at a time. 
	 * @param file a pointer to the file on disk
	 */
	private void sendFile(File file)
	{
		DataInputStream input = null;

		try
		{
			byte[] fileBuffer = new byte[1472]; //holds packet header and file data
			long offset = 0x0; //offset of first byte in the packet to send to client
			int count = 0; //counts to 1463 to indicate packet is full and send - reset each time

			try
			{
				input = new DataInputStream(new FileInputStream(file));	

				while (true) //will terminate when EOFException thrown
				{
					fileBuffer[count + 9] = input.readByte(); //throws EOFException when EOF reached, IOException if error occurs
					offset++;
					count++;

					if(count == 1463)
					{
						//construct a data packet and send it
						fileBuffer[0] = DATA;
						fileBuffer = storeLong(fileBuffer, 1, offset - count);

						sendData(fileBuffer, offset);

						//reset buffer and count
						fileBuffer = new byte[1472];
						count = 0;
					}
				}
			}
			catch(EOFException eof)
			{
				//end of file reached
				byte[] data = new byte[count + 9]; //the count of bytes read at which EOF occurred (probably < 1463)
				data[0] = DATA;
				data = storeLong(data, 1, offset - count);

				//copy fileBuffer into data with starting from index 9 in data
				for(int i = 9; i < count + 9; i++)
					data[i] = fileBuffer[i];

				sendData(data, offset);
			}
			catch(IOException ioe)
			{
				sendNOTOK();
			}
			finally
			{
				if(input != null) //close input stream
					input.close(); 
			}
			
			if(file.length() % 1463 == 0)
			{
				//send empty data to indicate to client that file is finished
				fileBuffer = new byte[1472];
				fileBuffer[0] = DATA;
				fileBuffer = storeLong(fileBuffer, 1, file.length());
			}
		}
		catch(Exception ex)
		{
			System.err.println("Exception: " + ex);
		}
	}

	/**
	 * Send a NOTOK packet to the client in an error state
	 */
	private void sendNOTOK()
	{
		try 
		{
			respPacket = new DatagramPacket(new byte[] { NOTOK }, 1, reqPacket.getSocketAddress());
			sock.send(respPacket);
		} 
		catch (Exception e) 
		{
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Sends a block of data to the client.
	 * Waits for an acknowledgement packet from the client in response
	 * If none is received within one second, tries again. 
	 * This is attempted a maximum of ten times before giving up.
	 * @param data the DATA packet to send
	 * @param currOffset the offset we are up to in the file
	 */
	private void sendData(byte[] data, long currOffset)
	{
		try 
		{
			respPacket = new DatagramPacket(data, data.length, reqPacket.getSocketAddress());
			DatagramPacket ackPacket = new DatagramPacket(new byte[9], 9);
			boolean resend = true;

			int tries = 0;

			while(tries < 10)
			{
				try
				{
					if(resend) 
						sock.send(respPacket);

					long t0 = System.currentTimeMillis();
					sock.receive(ackPacket);
					long t1 = System.currentTimeMillis();

					//check we received an ACK from client (not some random packet from random sender)
					if(ackPacket.getData()[0] == ACK && ackPacket.getSocketAddress().equals(reqPacket.getSocketAddress())) 
					{
						long ackOffset = extractLong(ackPacket.getData(), 1);

						if(ackOffset != currOffset)
						{
							System.err.println("Unexpected offset received: " + ackOffset + ". Expected: " + currOffset + ".)");
						}

						break; //stop looping - we received and processed the ackPacket successfully
					}
					else
					{
						resend = false; //keep waiting on the ACK, don't send the data again
						sock.setSoTimeout((int) (1000 - (t1 - t0))); //remaining time left to wait for the ACK
					}
				}
				catch(SocketTimeoutException ste)
				{
					tries++;
					sock.setSoTimeout(1000); //reset socket timeout to 1 second
					resend = true;
				}
			}
		}
		catch (Exception e) 
		{
			System.err.println(e.getMessage());
		}
	}

	private static byte[] storeLong(byte[] array, int off, long val)
	{
		array[off + 0] = (byte)((val & 0xff00000000000000L) >> 56);
		array[off + 1] = (byte)((val & 0x00ff000000000000L) >> 48);
		array[off + 2] = (byte)((val & 0x0000ff0000000000L) >> 40);
		array[off + 3] = (byte)((val & 0x000000ff00000000L) >> 32);
		array[off + 4] = (byte)((val & 0x00000000ff000000L) >> 24);
		array[off + 5] = (byte)((val & 0x0000000000ff0000L) >> 16);
		array[off + 6] = (byte)((val & 0x000000000000ff00L) >>  8);
		array[off + 7] = (byte)((val & 0x00000000000000ffL));
		return array;
	}

	private static long extractLong(byte[] array, int off)
	{
		long a = array[off+0] & 0xff;
		long b = array[off+1] & 0xff;
		long c = array[off+2] & 0xff;
		long d = array[off+3] & 0xff;
		long e = array[off+4] & 0xff;
		long f = array[off+5] & 0xff;
		long g = array[off+6] & 0xff;
		long h = array[off+7] & 0xff;
		return (a<<56 | b<<48 | c<<40 | d<<32 | e<<24 | f<<16 | g<<8 | h);
	}
}

public class StftpServer
{
	public void startServer(int port)
	{
		DatagramSocket ds = null;
		
		try {
			ds = new DatagramSocket(port);
			System.out.println("StftpServer running on port " + ds.getLocalPort());

			while (true) {
				byte[] buf = new byte[1472];
				DatagramPacket p = new DatagramPacket(buf, 1472);
				ds.receive(p);

				StftpServerWorker worker = new StftpServerWorker(p);
				worker.start();
			}
		}
		catch(Exception e) {
			System.err.println("Exception: " + e);
		}
		finally
		{
			if(ds != null)
				ds.close();
		}

		return;
	}

	public static void main(String args[])
	{
		int port = 10001;
		
		if(args.length == 1)
		{
			//optional port number provided
			try
			{
				port = Integer.parseInt(args[0]); //check port is numeric
				
				if(port <= 1024 || port > 65535) //check port in correct range
				{
					System.err.println("Port must be in range [1025 - 65535].");
					System.exit(1);
					return;
				}
			}
			catch(NumberFormatException nfe)
			{
				System.err.println("USAGE: 'StftpServer [port <default 10001>]'");
				System.exit(1);
				return;
			}
		}
		
		StftpServer d = new StftpServer();
		d.startServer(port);
	}
}
