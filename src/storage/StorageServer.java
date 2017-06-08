package storage;

import java.io.*;
import java.net.*;
import common.*;
import rmi.*;
import naming.*;

/** Storage server.

 <p>
 Storage servers respond to client file access requests. The files accessible
 through a storage server are those accessible under a given directory of the
 local filesystem.
 */
public class StorageServer implements Storage, Command
{

    /**
     * to test the functions
     * @param args
     */

	/*
    public static void main(String args[])
    {
        File f = new File("/Users/nikhi/Desktop/UCSD/Second Quarter/CSE 291D/Assignment2");
        Storage s = new StorageServer(f);
        Command c = new StorageServer(f);
        try {
        	//Create/Delete directories & subdirectories
            Path path = new Path("/Users/nikhi/Desktop/UCSD/Second Quarter/CSE 291D/Assignment2/test/a/b");
            Path path1 = new Path("/Users/nikhi/Desktop/UCSD/Second Quarter/CSE 291D/Assignment2/test/a/b");
            Path path2 = new Path("/Users/nikhi/Desktop/UCSD/Second Quarter/CSE 291D/Assignment2/test/a");
            Path path3 = new Path("/Users/nikhi/Desktop/UCSD/Second Quarter/CSE 291D/Assignment2/test");
            System.out.println(c.create(path1));
            System.out.println(c.delete(path));
            System.out.println(c.create(path1));
            System.out.println(c.delete(path2));
            System.out.println(c.create(path1));
            System.out.println(c.delete(path3));

            //Read, Write test
            byte[] result = s.read( path, 0, 2);
            for(byte b: result)
            {
                System.out.println(String.valueOf(b));
            }
            System.out.println("r: " + result);
            System.out.println("len: " + s.size(path));
            s.write(path, 3, result);*//**//*
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RMIException e) {
            s.write(path, 3, result);

        } catch (Exception e) {
            e.printStackTrace();
        }*//*
    }*/
    String rootDirectory = "/";

    Skeleton<Storage> skeleton1 = null;
    Skeleton<Command> skeleton2 = null;
    boolean isStarted = false;

    public static final int CLIENT_PORT = 5000;
    public static final int COMMAND_PORT = 5001;

    /** Creates a storage server, given a directory on the local filesystem, and
     ports to use for the client and command interfaces.

     <p>
     The ports may have to be specified if the storage server is running
     behind a firewall, and specific ports are open.

     @param root Directory on the local filesystem. The contents of this
     directory will be accessible through the storage server.
     @param client_port Port to use for the client interface, or zero if the
     system should decide the port.
     @param command_port Port to use for the command interface, or zero if
     the system should decide the port.
     @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root, int client_port, int command_port)
    {
    	if(root == null) {
    		throw new NullPointerException("Root is null");
    	}
        rootDirectory = root.getPath();
    }

    /** Creats a storage server, given a directory on the local filesystem.

     <p>
     This constructor is equivalent to
     <code>StorageServer(root, 0, 0)</code>. The system picks the ports on
     which the interfaces are made available.

     @param root Directory on the local filesystem. The contents of this
     directory will be accessible through the storage server.
     @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root)
    {
    	if(root == null) {
    		throw new NullPointerException("root is null");
    	}
        rootDirectory = root.getPath();
    }

    /** Starts the storage server and registers it with the given naming
     server.

     @param hostname The externally-routable hostname of the local host on
     which the storage server is running. This is used to
     ensure that the stub which is provided to the naming
     server by the <code>start</code> method carries the
     externally visible hostname or address of this storage
     server.
     @param naming_server Remote interface for the naming server with which
     the storage server is to register.
     @throws UnknownHostException If a stub cannot be created for the storage
     server because a valid address has not been
     assigned.
     @throws FileNotFoundException If the directory with which the server was
     created does not exist or is in fact a
     file.
     @throws RMIException If the storage server cannot be started, or if it
     cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
            throws RMIException, UnknownHostException, FileNotFoundException
    {
        if(!isStarted) {
			InetSocketAddress inetSocketAddress1 = new InetSocketAddress(hostname, CLIENT_PORT);
			skeleton1 = new Skeleton<Storage>(Storage.class, this, inetSocketAddress1);
			skeleton1.start();
            Storage client_stub = Stub.create(Storage.class, skeleton1, hostname);

            InetSocketAddress inetSocketAddress2 = new InetSocketAddress(hostname, COMMAND_PORT);
            skeleton2 = new Skeleton<Command>(Command.class, this, inetSocketAddress2);
            skeleton2.start();
            Command command_stub = Stub.create(Command.class, skeleton2, hostname);

            Path[] file_paths = Path.list(new File(rootDirectory));
            
            Path[] delete_file_paths = naming_server.register(client_stub, command_stub, file_paths);
            for(Path p : delete_file_paths) {
            	this.delete(p);
            }
            this.deleteEmptyFolders(rootDirectory);
            isStarted = true;
        }
        else
        {
            throw new RMIException("Storage server is already started");
        }
    }

    /** Stops the storage server.

     <p>
     The server should not be restarted.
     */
    public void stop()
    {
        if(isStarted)
        {
            skeleton1.stop();
            skeleton2.stop();
            isStarted = false;
        }
    }

    /** Called when the storage server has shut down.

     @param cause The cause for the shutdown, if any, or <code>null</code> if
     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
    	if(file == null) {
    		throw new NullPointerException("file is null");
    	}
        String filepath = rootDirectory + file.toString();
        File f = new File(filepath);
        if(f.exists() && !f.isDirectory())
        {
        	return f.length();
        }
        else
        {
            throw new FileNotFoundException("File not found in path: " + filepath);
        }
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
            throws FileNotFoundException, IOException
    {
    	if(file == null) {
    		throw new NullPointerException("File is null");
    	}
        String filepath = rootDirectory + file.toString();
        File f = new File(filepath);
        if(!f.exists() || f.isDirectory())
        {
            throw new FileNotFoundException("File not found in path: " + filepath);
        }
        if(offset+length > f.length() || offset < 0 || length < 0)
        {
            throw new IndexOutOfBoundsException();
        }
        RandomAccessFile randomAccessFile = null;
        byte[] result = new byte[length];
        try {
        	randomAccessFile = new RandomAccessFile(f, "r");
            randomAccessFile.seek(offset);
            randomAccessFile.read(result, 0, length);
        } catch(Exception e) {
        	e.printStackTrace();
        } finally {
        	if(randomAccessFile != null) {
        		randomAccessFile.close();
        	}
        }
    	return result;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
            throws FileNotFoundException, IOException
    {
    	if(file == null || data == null) {
    		throw new NullPointerException("File/Data is null");
    	}
        String filepath = rootDirectory + file.toString();
        File f = new File(filepath);
        if(!f.exists() || f.isDirectory())
        {
            throw new FileNotFoundException("File not found in path: " + filepath);
        }
        if(offset < 0)
        {
            throw new IndexOutOfBoundsException();
        }
        RandomAccessFile randomAccessFile = null;
        try {
        	randomAccessFile = new RandomAccessFile(f, "rw");
            randomAccessFile.seek(offset);
            randomAccessFile.write(data);
        } catch(Exception e) {
        	e.printStackTrace();
        } finally {
        	if(randomAccessFile != null) {
        		randomAccessFile.close();
        	}
        }

    }

    // The following methods are documented in Command.java./*
    @Override
    public synchronized boolean create(Path file)
    {
    	if(file == null) {
    		throw new NullPointerException("Input is null");
    	} else {
    		if(file.toString().equals("/")) {
    			return false;
    		} else {
    			String hierarchy = rootDirectory + file.toString();
    			File f = new File(hierarchy);
    			if(f.exists()) {
    				return false;
    			} else {
    				String directoryName = file.parent().toString();
    				File dir = new File(rootDirectory + directoryName);
    				dir.mkdirs();
    				dir.setWritable(true);
    				File newFile = new File(hierarchy);
    				try {
						return newFile.createNewFile();
					} catch (IOException e) {
						return false;
					}
    			}
    		}
    	}
    }

    @Override
    public synchronized boolean delete(Path path)
    {
    	if(path == null || path.toString() == null || path.toString().equals("")){
    		throw new NullPointerException("Path is empty");
    	} else {
    		if(path.isRoot()) {
    			return false;
    		} else {
    			boolean deleted = false;
    			try {
	    			File file = new File(rootDirectory+"/"+path.toString());
	    			deleted = this.deleteFile(file);
	    			
	    			Path parentName = path.parent();
	    			File parent = new File(rootDirectory+"/"+parentName.toString());
	    			if(parent.listFiles().length == 0) {
	    				parent.delete();
	    			}
    			} catch(Exception e) {
    				return false;
    			}
    			return deleted;
    		}
    	}
    }

    public synchronized boolean deleteFile(File f) {
    	try {
    		if (f.isDirectory()) {
    		    for (File c : f.listFiles())
    		      this.deleteFile(c);
    		  }
    		return f.delete();
    	} catch (Exception e) {
    		throw e;
    	}
    }
    
    private synchronized void deleteEmptyFolders(String fileLocation) {
        File folder = new File(fileLocation);
        File[] listofFiles = folder.listFiles();
        if (listofFiles.length == 0) {
            folder.delete();
        } else {
            for (int j = 0; j < listofFiles.length; j++) {
                File file = listofFiles[j];
                if (file.isDirectory()) {
                	deleteEmptyFolders(file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * @param file Path to the file to be copied.
        @param server Storage server from which the file is to be downloaded.
        @return <code>true</code> if the file is successfully copied;
                <code>false</code> otherwise.
        @throws FileNotFoundException If the file is not present on the remote
                                      storage server, or the path refers to a
                                      directory.
        @throws IOException If an I/O exception occurs either on the remote or
                            on this storage server.
        @throws RMIException If the call cannot be completed due to a network
                             error, whether between the caller and this storage
                             server, or between the two storage servers.
     */
    @Override
    public synchronized boolean copy(Path file, Storage server)
            throws RMIException, FileNotFoundException, IOException {
    	if(file == null || server == null) {
    		throw new NullPointerException("Input parameters are null");
    	} else {
    		boolean copied = false;
    		try {
    			int fileLength = (int)server.size(file);
    			Path newFile = new Path(file.toString());
    			byte[] fileContents = server.read(file, 0, fileLength);
    			File newF = new File(rootDirectory + newFile.toString());
    			if(newF.exists()) {
    				newF.delete();
    			}
    			boolean created = this.create(newFile);
    			if(created) {
    				this.write(newFile, 0, fileContents);
    				copied = true;
    			} else {
    				copied = false;
    			}
    		} catch(FileNotFoundException fe) {
    			throw fe;
    		} catch(IOException ioe) {
    			throw ioe;
    		} catch(RMIException rmie) {
    			throw rmie;
    		}
    		return copied;
    	}
    }
}
