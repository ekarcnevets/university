import java.net.*;
import java.io.*;

/**
 * Super Trivial File Transfer Protocol Client
 * @date 16 May 2013
 * @author Steven Crake 1117696
 */
public class StftpClient {
	private static DatagramPacket reqPacket = null;
	private static DatagramPacket respPacket = null;
	private static DatagramSocket sock = null;
	private static DataOutputStream output = null;
	private static final int REQ     = 3;
	private static final int OK      = 1;
	private static final int DATA    = 2;
	private static final int ACK     = 42;
	private static final int NOTOK   = 0;
	
	public static void main(String[] args)
	{
		if(args.length != 3)
		{
			printUsage();
			System.exit(1);
			return;
		}
		
		String sIP = args[0];
		String sPort = args[1];
		String filename = args[2];
		InetAddress addr;
		int iPort;
		
		//Validate inputs
		try
		{
			addr = InetAddress.getByName(sIP); //valid IP/host-name
			iPort = Integer.parseInt(sPort); //port numerical
			
			if(iPort <= 1024 || iPort > 65535) //port out of range
			{
				System.err.println("Port must be in range [1025 - 65535].");
				System.exit(1);
				return;
			}
			
			if(filename.contains("/") || filename.contains("\\")) //illegal filename
			{
				System.err.println("Filename must not contain slashes (/,\\).");
				System.exit(1);
				return;
			}
			
			sock = new DatagramSocket();
			sock.setSoTimeout(1000); //one second timeout
			
			//send request
			sendRequest(addr, iPort, filename);
		}
		catch(UnknownHostException uhe)
		{
			System.err.println("Provided IP/hostname was invalid or unable to be parsed. Supply only a hostname or a valid IP address");
			System.exit(1);
		}
		catch(NumberFormatException nfe)
		{
			System.err.println("Port must be numeric.");
			System.exit(1);
		} 
		catch (SocketException se) 
		{
			System.err.println("An error occured while creating the socket for the connection. " + se.getMessage());
			System.exit(1);
		}
	}
	
	private static void sendRequest(InetAddress addr, int port, String filename)
	{
		try
		{
			//get byte array representation of filename
			byte[] filenameBytes = filename.getBytes();
			
			//create the REQ packet
			byte[] data = new byte[filenameBytes.length + 1];
			data[0] = REQ;
			
			//copy byte array representation of filename into data packet
			for(int i = 0; i < filenameBytes.length; i++)
				data[i+1] = filenameBytes[i];
			
			reqPacket = new DatagramPacket(data, data.length, addr, port);
			sock.send(reqPacket);
			receiveData(addr.getHostAddress(), filename);
		}
		catch(Exception e)
		{
			System.err.println("An error occured " + e.getStackTrace() + e.getMessage() + ". ");
			System.exit(1);
		}
	}

	/**
	 * Listen for an OK/DATA packet to begin the transfer
	 * If an OK packet is received first, the client will be notified of transfer progress
	 * If a DATA packet is received first, no progress will be indicated. 
	 * If no packets are received after 10 timeouts (of one second length) the transfer will abort with an error
	 * @param serverIP the IP or hostname of the server
	 * @param filename the name of the requested file
	 */
	private static void receiveData(String serverIP, String filename)
	{
		int tries = 1;						//number of times a request was retransmitted, up to 10 at failure
		long fileSize = -1;					//size of the file, as specified in an OK packet if it arrived
		long numBytes = 0;					//number of bytes received from server
		long serverOffset = 0;				//offset received from server
		float progress;						//used to show progress to user
		float percentageProgress = 0.1f;	//
		
		try
		{
			output = new DataOutputStream(new FileOutputStream(filename));
		}
		catch(FileNotFoundException fnfe)
		{
			System.err.println("Could not create output file. " + fnfe.getMessage());
			System.exit(1);
			return;
		}
		
		while(tries < 10)
		{
			try
			{
				respPacket = new DatagramPacket(new byte[1472], 1472);
				sock.receive(respPacket);
				
				//if packet was not from server, keep receiving
				if(!respPacket.getAddress().getHostAddress().equals(serverIP))
				{
					System.err.println("Packet received from unexpected sender. IP was " + respPacket.getAddress().getHostAddress() + ".");
					continue;
				}
				
				byte[] recvBytes = respPacket.getData();

				if(recvBytes[0] == DATA) //DATA received
				{
					serverOffset = extractLong(recvBytes,1);
					
					if(serverOffset == numBytes) //offsets match what we expect - continue
					{
						numBytes += respPacket.getLength() - 9;
						
						if(fileSize != -1) //display percentage progress to user
						{
							progress = ((float)fileSize * percentageProgress); 
							if(progress <= numBytes)
							{
								System.out.print(String.format("%.0f",(progress/(float)fileSize)*100) + "%...");
								percentageProgress += 0.1f;
							}
						}
						
						//add this block to file and keep receiving
						for(int i = 9; i < respPacket.getLength(); i++)
							output.write(recvBytes[i]);		
					}
					else if(serverOffset > numBytes) //error state
					{
						//something crazy went wrong, error and abort
						System.err.println("Unexpected value of server offset '" + serverOffset + "'. Expected <= '" + numBytes + "'.");
						System.exit(1);
						return;
					}
					//else we already have this data, just send ACK
					
					sendACK(numBytes, respPacket.getSocketAddress());
					
					if((fileSize != -1 && numBytes == fileSize) || respPacket.getLength() < 1472)
					{
						System.out.println("100%");
						break; //stop receiving
					}
				}
				else if(recvBytes[0] == OK) //OK received
				{
					//store file size
					fileSize = extractLong(recvBytes, 1);
					System.out.println("Incoming file size: " + fileSize + " bytes");
				}
				else if(recvBytes[0] == NOTOK) //NOTOK received
				{
					System.out.println("Server returned NOTOK. File does not exist or access denied.");
					output.close();
					
					File f = new File(filename);
					
					if(f.exists()) //delete the partially formed file if it exists
						f.delete();
					
					System.exit(1);
					break;
				}
				else
				{
					System.err.println("Packet had unexpected OPCODE '" + recvBytes[0] + "'. Discarding...");
					continue;
				}
				
				//received OK or DATA so successful, reset tries
				tries = 0;
			}
			catch(SocketTimeoutException ste) //the socket.receive() timed out after 1 second
			{
				try 
				{
					if(reqPacket.getData()[0] == REQ) //only retransmit REQ packets (not ACKs)
					{
						tries++;
						sock.send(reqPacket);
					}
				} 
				catch (IOException ioe) {
					System.err.println("An error occured while attempting to resend the REQ packet in StftpClient.beginReceiving(). " + ioe.getMessage());
					System.exit(1);
					break;				
				}
			}
			catch(IOException ioe)
			{
				System.err.println("An error occured while attempting to receive OK/DATA packets in StftpClient.beginReceiving(). " + ioe.getMessage());
				System.exit(1);
				break;
			}
		}
		
		//if the server sent an OK packet to indicate file size and it does not match the number of received bytes, generate an error notification
		if(fileSize != -1 && fileSize != numBytes)
		{
			System.err.println("Server specified a fileSize which did not match number of received data bytes.\nFilesize: " + fileSize + ", Received bytes: " + numBytes + ".");
		}
		
		try
		{
			if(output != null)
				output.close();
		}
		catch(IOException ioe)
		{
			System.err.println("Failed to close the output stream. " + ioe.getMessage());
			System.exit(1);
		}
		
		//connection timed out, notify user
		if(tries == 10)
		{
			System.err.println("Connection timed out after 10 attempts.");
			
			File f = new File(filename);
			
			if(f.exists()) //delete the empty file
				f.delete();
		}
	}
	
	/**
	 * Send an acknowledgement packet to the server
	 * @param offset the next byte expected from the server
	 * @param serverAddress the InetAddress and Port number of the server in a SocketAddress wrapper
	 */
	private static void sendACK(long offset, SocketAddress serverAddress)
	{
		try 
		{
			byte[] data = new byte[9];
			data[0] = ACK;
			data = storeLong(data, 1, offset); //put offset into the packet
		
			reqPacket = new DatagramPacket(data, 9, serverAddress);
			sock.send(reqPacket);
		} 
		catch (Exception ex) //no retransmit necessary
		{
			System.err.println("An error occured while attempting to send an ACK. " + ex.getMessage());
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
	
	/**
	 * Print a usage message to the user if arguments were incorrect
	 */
	private static void printUsage()
	{
		System.err.println("USAGE: StftpClient [ ip/hostname  port  filename ]");
		System.exit(1);
	}
}
