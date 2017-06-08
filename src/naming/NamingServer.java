package naming;

import java.io.*;
import java.net.*;
import java.util.*;

import rmi.*;
import common.*;
import storage.*;

public class NamingServer implements Service, Registration
{
    HashMap<Path, ArrayList<Storage>> storageHashMap;
    HashMap<Path, ArrayList<Command>> commandHashMap;

    // tree structure for keeping track of file system
    Tree namingTree;

    List<Storage> storageList; // for storage servers
    List<Command> commandList; // for clients

    Skeleton<Registration> skeletonRegistration;
    Skeleton<Service> skeletonService;

    public NamingServer()
    {
        storageHashMap = new HashMap<Path, ArrayList<Storage>>();
        commandHashMap = new HashMap<Path, ArrayList<Command>>();

        namingTree = new Tree();

        storageList = new ArrayList<Storage>();
        commandList = new ArrayList<Command>();

        this.skeletonRegistration = new Skeleton<Registration>(
                Registration.class,
                this,
                new InetSocketAddress("localhost", NamingStubs.REGISTRATION_PORT));
        this.skeletonService = new Skeleton<Service>(
                Service.class,
                this,
                new InetSocketAddress("localhost", NamingStubs.SERVICE_PORT));
    }

    public synchronized void start() throws RMIException
    {
        this.skeletonRegistration.start();
        this.skeletonService.start();
    }

    public void stop()
    {
        this.skeletonRegistration.stop();
        this.skeletonService.stop();

        Throwable cause = new Throwable("NamingServer stop called!");
        stopped(cause);
    }

    protected void stopped(Throwable cause)
    {
        if(cause == null) {
            System.out.println("User requested shutdown!");
        } else {
            System.out.println("Shutdown cause: " + cause.toString());
        }
    }

    // The following public methods are documented in Service.java.
    @Override
    public void lock(Path path, boolean exclusive) throws FileNotFoundException
    {
        if(path == null) {
            throw new NullPointerException("path passed is NULL");
        }

        if(path.isRoot()) {
            if(exclusive) {
                namingTree.getRoot().lock.acquireExclusiveLock();
            } else {
                namingTree.getRoot().lock.acquireSharedLock();
            }
            return;
        }

        if(namingTree.getNode(path) == null) {
            throw new FileNotFoundException("file not found");
        }

        ArrayList<String> paths = path.getPathComponents();
        namingTree.getRoot().lock.acquireSharedLock();
        
        Node current = namingTree.getRoot();
        Node leaf = namingTree.getNode(path);
        if(leaf == null) {
            throw new FileNotFoundException("Path not present");
        }
        
        for(int i=0;i<paths.size()-1;i++) {
            // acquire shared lock until parent node
        	String dir = paths.get(i);
        	for (Node n : current.children) {
                if(n.name.equals(dir)) {
                    current = n;
                    n.lock.acquireSharedLock();
                    break;
                }
            }
        }
        if(exclusive) {
        	leaf.lock.acquireExclusiveLock();
        } else {
        	leaf.lock.acquireSharedLock();
            if(leaf.lock.numOfReadRequests == 20)
                replicate(path, leaf);
        }
    }

    @Override
    public void unlock(Path path, boolean exclusive) {
        if(path == null) {
            throw new NullPointerException("Path passed is null!");
        }
        if(path.isRoot())
        {
            if(exclusive)
            {
                namingTree.getRoot().lock.releaseExclusiveLock();
            }
            else
            {
                namingTree.getRoot().lock.releaseSharedLock();
            }
            return;
        }

        ArrayList<String> paths = path.getPathComponents();
        namingTree.getRoot().lock.releaseSharedLock();
        
        Node current = namingTree.getRoot();
        Node leaf = namingTree.getNode(path);
        if(leaf == null) {
            throw new IllegalArgumentException("Path not present");
        }
        
        for(int i=0;i<paths.size()-1;i++) {
            // acquire shared lock until parent node
        	String dir = paths.get(i);
        	for (Node n : current.children) {
                if(n.name.equals(dir)) {
                    current = n;
                    n.lock.releaseSharedLock();
                    break;
                }
            }
        }
        if(exclusive) {
        	leaf.lock.releaseExclusiveLock();
            invalidateStaleCopies(path, leaf);
        } else {
        	leaf.lock.releaseSharedLock();
        }
    }


    private void replicate(Path path, Node node)
    {
        for(Command command: commandList)
        {
            if(!node.commandStubs.contains(command))
            {
                node.commandStubs.add(command);
                try {
                    command.copy(path, node.storageStub);
                } catch (RMIException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        node.lock.numOfReadRequests = 0;
    }


    private void invalidateStaleCopies(Path path, Node node)
    {
        try {
            if(!isDirectory(path) && node.commandStubs.size() > 1)
            {
                for(int i = 1; i < node.commandStubs.size(); i++) {
                    node.commandStubs.get(i).delete(path);
                }
                for(int i = 1; i < node.commandStubs.size(); i++) {
                    node.commandStubs.remove(i);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (RMIException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        if(path == null) {
            throw new NullPointerException("Path passed is null!");
        }

        if(path.isRoot()) {
            return true;
        }

        if(namingTree == null) {
            throw new NullPointerException("Tree is null");
        }

        if(path.toString() == "") {
        	path = new Path("/");
        }
        Node dirNode = namingTree.getNode(path);
        if(dirNode == null) {
        	throw new FileNotFoundException("Path cannot be found!");
        } else {
        	if(dirNode.isDirectory) {
        		return true;
        	} else {
        		return false;
        	}
        }
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {

        Set<String> resultList = new HashSet<String>();
        String[] result = null;

        if(directory == null) {
            throw new NullPointerException("Path passed is null");
        }

        if(commandHashMap.containsKey(directory)) {
            throw new FileNotFoundException("File given as Input");
        }

        for(Path key : commandHashMap.keySet()) {
            if(key.toString().startsWith(directory.toString())) {
                String fileName = key.toString().replaceFirst(directory.toString(), "");
                String[] dirs = fileName.split("/");
                if(dirs[0].equals("") && dirs.length > 1) {
                    resultList.add(dirs[1]);
                } else {
                    resultList.add(dirs[0]);
                }
            }
        }

        if(resultList.size() > 0) {
            result = new String[resultList.size()];
            int count = 0;
            for(String dir : resultList) {
                result[count++] = dir;
            }
        } else {
            throw new FileNotFoundException("Path cannot be found!");
        }

        return result;
    }

    @Override
    public boolean createFile(Path file) throws RMIException, FileNotFoundException
    {

        if(file == null) {
            throw new NullPointerException("Path passed is null");
        }

        if(file.isRoot()) {
            return false;
        }

        
        if(!isDirectory(file.parent())) {
            throw new FileNotFoundException("Parent doesn't exist!");
        }

        if(commandHashMap.isEmpty()) {
            throw new IllegalStateException("command hash map is empty!");
        }
        
        Path par = file.parent();
        if (par != null && par.equals("")) {
            par = new Path("/");
        }

        Node parent = namingTree.getNode(par);
        if(parent == null) {
            throw new FileNotFoundException("parent not found!");
        }
        Node actualNode = namingTree.getNode(file);
        if(actualNode == null) {
            parent.commandStubs.get(0).create(file);
            namingTree.insertNode(file, false, parent.commandStubs.get(0), parent.storageStub);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException, RMIException {
        if (directory == null) {
            throw new NullPointerException("directory is null");
        }

        if (directory.isRoot()) {
            return false;
        }

        // checking if parent exists
        Path par = directory.parent();
        if (par != null && par.equals("")) {
            par = new Path("/");
        }

        Node parent = namingTree.getNode(par);
        if(parent == null) {
            throw new FileNotFoundException("parent not found!");
        }

        if(!parent.isDirectory) {
            throw new FileNotFoundException("Parent is a file!");
        }

        Node actualNode = namingTree.getNode(directory);
        if(actualNode == null) {
            namingTree.insertNode(directory, true, parent.commandStubs.get(0), parent.storageStub);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException, RMIException {

        if (path == null) {
            throw new NullPointerException("path is null");
        }

        if (path.isRoot()) {
            return false;
        }

        if(namingTree.getNode(path) == null) {
            throw new FileNotFoundException("File not present in tree");
        }

        ArrayList<Command> cList = commandHashMap.get(path);
        if(cList != null && cList.size() > 0) {
	        for(Command cStub : cList) {
	        	//cStub.delete(path);
	        	commandList.remove(cStub);
	        }
        }
        
        ArrayList<Storage> sList = storageHashMap.get(path);
        if(sList != null && sList.size() > 0) {
	        for(Storage sStub : sList) {
	        	storageList.remove(sStub);
	        }
        }

        commandHashMap.remove(path);
        storageHashMap.remove(path);

        // update tree by deleting the path
        Node leaf = namingTree.getNode(path);
        if(leaf == null) {
        	throw new FileNotFoundException("File is not found!!");
        }

        List<Command> cList1 = leaf.commandStubs;
		for(Command cStub : cList1) {
			cStub.delete(path);
		}

        Path parentPath = path.parent();
        if(parentPath.equals("")){
        	parentPath = new Path("/");
        }
        Node parent = namingTree.getNode(parentPath);
        parent.children.remove(leaf);
        
        return true;
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
        if(file == null) {
            throw new NullPointerException("path is null");
        }

        if(file.isRoot()) {
            return null;
        }

        if(this.isDirectory(file)) {
            throw new FileNotFoundException("path is a directory");
        }

        if (storageHashMap.containsKey(file)) {
            Node fileNode = namingTree.getNode(file);
            return fileNode.storageStub;
        } else {
            ArrayList<Storage> temp = storageHashMap.get(file);
            int index = (int) (Math.random() % temp.size());

            return temp.get(index);
        }
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
        if(client_stub == null || command_stub == null || files == null) {
            throw new NullPointerException("parameters passed is/are null");
        }
        
        for(Storage a : storageList) {
        	if(a.equals(client_stub)) {
        		throw new IllegalStateException();
        	}
        }
        
        for(Command a : commandList) {
        	if(a.equals(command_stub)) {
        		throw new IllegalStateException();
        	}
        }

        storageList.add(client_stub);
        commandList.add(command_stub);

        ArrayList<Path> resultList = new ArrayList<Path>();
        Path[] result;

        for(Path file : files) {
            if(file.isRoot()) {
                continue;
            }

            if (storageHashMap.containsKey(file) && commandHashMap.containsKey(file)) {
                // if the map already exists, need to return to caller to delete
                resultList.add(file);
            } else {
            	boolean skip = false;
            	for(Path key : storageHashMap.keySet()) {
            		String dirName = key.parent().toString();
            		if(dirName.contains(file.toString())) {
            			resultList.add(file);
            			skip = true;
            			break;
            		}
            	}
            	if(!skip) {
	                ArrayList<Storage> sList = new ArrayList<Storage>();
	                ArrayList<Command> cList = new ArrayList<Command>();
	
	                sList.add(client_stub);
	                cList.add(command_stub);
	
	                storageHashMap.put(file, sList);
	                commandHashMap.put(file, cList);
	                
	                //create file+directory in tree;
	                namingTree.insertNode(file, false, command_stub, client_stub);
            	}
            }
        }

        if(resultList.size() > 0) {
        	result = new Path[resultList.size()];
        	for(int i=0;i<resultList.size();i++) {
        		result[i] = resultList.get(i);
        	}
        } else {
        	result = new Path[0];
        }
        
        return result;
    }
}
