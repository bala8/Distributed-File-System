package common;

import java.io.*;
import java.util.*;

/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Comparable<Path>, Serializable
{
	private static final long serialVersionUID = 1L;
	private String pathString;
	private boolean rootPath = false;
	
    /** Creates a new path which represents the root directory. */
    public Path()
    {
    	rootPath = true;
    	pathString = "/";
    }

    /** Creates a new path by appending the given component to an existing path.

        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
    */
    public Path(Path path, String component)
    {
    	if(component.isEmpty() || component.equals("") || component.contains("/") || component.contains(":")) {
    		throw new IllegalArgumentException("Contains invalid input");
    	} else {
    		String currentPath = path.toString();
    		if(currentPath.endsWith("/")) {
    			currentPath = currentPath + component.trim();
    		} else {
    			currentPath = currentPath + "/" + component.trim();
    		}
    		this.pathString = currentPath;
    		this.rootPath = false;
    	}
    }

    /** Creates a new path from a path string.

        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.

        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
     */
    public Path(String path)
    {
        if(path.isEmpty() || path.equals("") || !path.startsWith("/") || path.contains(":")) {
        	throw new IllegalArgumentException("Invalid path as input");
        } else {
        	pathString = path.replaceAll("[/]+", "/");
        	if(pathString.endsWith("/") && pathString.length() > 1) {
        		pathString = pathString.substring(0, pathString.length()-1);
        	}
        	if(path.equals("/")) {
        		rootPath = true;
        	} else {
        		rootPath = false;
        	}
        }
    }

    /** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
    	if(pathString == null) {
    		return null;
    	}
        String path = this.pathString;
        String[] pathListArray = path.substring(1).split("/");
        return Arrays.asList(pathListArray).iterator();
    }

    /*
        returns individual components of path object string
     */
    public ArrayList<String> getPathComponents()
    {
        ArrayList<String> nodes = new ArrayList<>();
        int skip = 1;
        for(String str: this.pathString.split("/")) {
            if(skip == 1) {
                skip++;
            } else {
                nodes.add(str);
            }
        }

        if(nodes.size() == 0) {
            return null;
        }

        return nodes;
    }

    /** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
		if(directory.isDirectory()) {
			List<Path> pathList = new ArrayList<Path>();
			listRecursiveFiles(directory.getAbsolutePath(), "",pathList);
			Path[] pathArray = new Path[pathList.size()];
			for(int i=0; i<pathList.size(); i++) {
				pathArray[i] = pathList.get(i);
			}
			return pathArray;
		} else {
			throw new IllegalArgumentException(directory + "exists but not a directory");
		}
    }
    
    private static void listRecursiveFiles(String directoryName, String parent, List<Path> pathList) {
    	File directory = new File(directoryName);
    	File[] fList = directory.listFiles();
    	for(File file : fList) {
    		if(file.isFile()) {
    			Path p = new Path(parent + "/" + file.getName());
    			pathList.add(p);
    		} else if(file.isDirectory()) {
    			String parentNew = parent + "/" + file.getName();
    			listRecursiveFiles(file.getPath(), parentNew, pathList);
    		}
    	} 
    }    

    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        return this.rootPath;
    }

    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
    	if(isRoot()) {
    		throw new IllegalArgumentException("Path represents root");
    	} else {
    		Path p = new Path();
    		String parentPath = "";
    		String currentPath = pathString;
    		String[] directories = currentPath.split("/");
    		if(directories.length == 0) {
    			parentPath = "/";
    			p.rootPath = true;
    		} else {
    			for(int i=0;i<directories.length-1;i++) {
    				String pName = directories[i];
    				if(!pName.isEmpty() && !pName.equals("")) {
    					parentPath = parentPath+"/"+pName;
    				}
        		}
    			p.rootPath = false;
    		}
    		p.pathString = parentPath;
    		return p;
    	}
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
    	if(isRoot()) {
    		throw new IllegalArgumentException("Path represents root");
    	} else {
    		String lastPath = "";
    		String currentPath = pathString;
    		String[] directories = currentPath.split("/");
    		lastPath = directories[directories.length - 1];
    		return lastPath;
    	}
    }

    /** Determines if the given path is a subpath of this path.

        <p>
        The other path is a subpath of this path if it is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.

        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
     */
    public boolean isSubpath(Path other)
    {
        if(pathString.isEmpty() || pathString.equals("")){
        	return false;
        } else {
        	if(pathString.startsWith(other.pathString)) {
        		return true;
        	} else {
        		return false;
        	}
        }
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
        File file = new File(root.getAbsolutePath() + this.toString());
        return file;
    }

    /** Compares this path to another.

        <p>
        An ordering upon <code>Path</code> objects is provided to prevent
        deadlocks between applications that need to lock multiple filesystem
        objects simultaneously. By convention, paths that need to be locked
        simultaneously are locked in increasing order.

        <p>
        Because locking a path requires locking every component along the path,
        the order is not arbitrary. For example, suppose the paths were ordered
        first by length, so that <code>/etc</code> precedes
        <code>/bin/cat</code>, which precedes <code>/etc/dfs/conf.txt</code>.

        <p>
        Now, suppose two users are running two applications, such as two
        instances of <code>cp</code>. One needs to work with <code>/etc</code>
        and <code>/bin/cat</code>, and the other with <code>/bin/cat</code> and
        <code>/etc/dfs/conf.txt</code>.

        <p>
        Then, if both applications follow the convention and lock paths in
        increasing order, the following situation can occur: the first
        application locks <code>/etc</code>. The second application locks
        <code>/bin/cat</code>. The first application tries to lock
        <code>/bin/cat</code> also, but gets blocked because the second
        application holds the lock. Now, the second application tries to lock
        <code>/etc/dfs/conf.txt</code>, and also gets blocked, because it would
        need to acquire the lock for <code>/etc</code> to do so. The two
        applications are now deadlocked.

        @param other The other path.
        @return Zero if the two paths are equal, a negative number if this path
                precedes the other path, or a positive number if this path
                follows the other path.
     */
    @Override
    public int compareTo(Path other)
    {
    	if(other == null) {
    		throw new NullPointerException("Input parameter is null");
    	}
        String thisPath = this.toString();
        String otherPath = other.toString();
        String[] thisPathDirs = thisPath.split("/");
        String[] otherPathDirs = otherPath.split("/");
        return (thisPathDirs.length - otherPathDirs.length);
    }

    /** Compares two paths for equality.

        <p>
        Two paths are equal if they share all the same components.

        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
        if(other == null) {
            return false;
        }

        if(this.toString().equals(other.toString())) {
            return true;
        }

        return false;
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
    	return this.pathString.hashCode();
    }

    /** Converts the path to a string.

        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.

        @return The string representation of the path.
     */
    @Override
    public String toString()
    {
        return pathString;
    }
}
