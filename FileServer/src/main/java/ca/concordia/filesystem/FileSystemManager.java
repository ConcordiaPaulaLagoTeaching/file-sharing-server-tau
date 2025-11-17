package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
 * TODO: Review and comment code properly
 */

public class FileSystemManager {

    // Constants defining file system limits
    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private final int BLOCK_SIZE = 128;

    // Core file system structures
    private final RandomAccessFile disk;
    private final FEntry[] fentries;
    private final FNode[] fnodes;
    private final boolean[] freeBlockList;

    // Synchronization primitives
    private final ReentrantReadWriteLock metadataLock = new ReentrantReadWriteLock(); // protects fentries[] and freeBlockList[]
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> fileLocks = new ConcurrentHashMap<>(); // per-file locks

    // Constructor: initializes disk, metadata, and in-memory structures
    public FileSystemManager(String filename, int totalSize) {
        try {
            disk = new RandomAccessFile(filename, "rw");

            fentries = new FEntry[MAXFILES];
            fnodes = new FNode[MAXBLOCKS];
            freeBlockList = new boolean[MAXBLOCKS];

            for (int i = 0; i < MAXBLOCKS; i++) {
                fnodes[i] = new FNode();
            }

            int metadataBytes = (MAXFILES * 15) + (MAXBLOCKS * 8);
            int metadataBlocks = (int) Math.ceil((double) metadataBytes / BLOCK_SIZE);
            for (int i = 0; i < metadataBlocks; i++) {
                freeBlockList[i] = true;
            }

            loadMetadata();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize file system", e);
        }
    }

    // Loads metadata from disk into memory (fentries and fnodes)
    private void loadMetadata() throws Exception {
        disk.seek(0);

        for (int i = 0; i < MAXFILES; i++) {
            byte[] entryBytes = new byte[15];
            disk.read(entryBytes);
            fentries[i] = FEntry.fromBytes(entryBytes);
            if (!fentries[i].getFilename().isEmpty()) {
                fileLocks.put(fentries[i].getFilename(), new ReentrantReadWriteLock());
            }
        }

        for (int i = 0; i < MAXBLOCKS; i++) {
            byte[] nodeBytes = new byte[8];
            disk.read(nodeBytes);
            FNode node = FNode.fromBytes(nodeBytes);
            fnodes[i] = (node.getBlockIndex() >= 0 && node.getBlockIndex() < MAXBLOCKS) ? node : new FNode();
        }
    }

    // Saves metadata (fentries and fnodes) back to disk
    private void saveMetadata() throws Exception {
        disk.seek(0);
        for (FEntry entry : fentries) {
            disk.write(entry.toBytes());
        }
        for (FNode node : fnodes) {
            disk.write(node.toBytes());
        }
    }

    // Creates a new file entry and initializes its lock
    public void createFile(String filename) throws Exception {
        metadataLock.writeLock().lock();
        try {
            for (FEntry entry : fentries) {
                if (entry.getFilename().equals(filename)) {
                    throw new Exception("ERROR: file already exists");
                }
            }

            for (int i = 0; i < fentries.length; i++) {
                if (fentries[i].getFilename().isEmpty()) {
                    fentries[i].setFilename(filename);
                    fentries[i].setFilesize((short) 0);
                    fentries[i].setFirstBlock((short) -1);
                    fileLocks.put(filename, new ReentrantReadWriteLock());
                    saveMetadata();
                    return;
                }
            }

            throw new Exception("ERROR: no space for new file");
        } finally {
            metadataLock.writeLock().unlock();
        }
    }

    // Deletes a file and frees its blocks and metadata
    public void deleteFile(String filename) throws Exception {
        ReentrantReadWriteLock lock = fileLocks.get(filename);
        if (lock == null) throw new Exception("ERROR: file " + filename + " does not exist");

        lock.writeLock().lock();
        try {
            for (FEntry entry : fentries) {
                if (entry.getFilename().equals(filename)) {
                    int block = entry.getFirstBlock();
                    while (block != -1) {
                        int dataBlockIndex = fnodes[block].getBlockIndex();
                        freeBlockList[dataBlockIndex] = false;
                        fnodes[block].reset();
                        overwriteBlock(dataBlockIndex);
                        block = fnodes[block].getNext();
                    }
                    entry.reset();
                    fileLocks.remove(filename);
                    saveMetadata();
                    return;
                }
            }
            throw new Exception("ERROR: file " + filename + " does not exist");
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Writes content to a file, allocating new blocks and linking them
    public void writeFile(String filename, byte[] contents) throws Exception {
        ReentrantReadWriteLock lock = fileLocks.get(filename);
        if (lock == null) throw new Exception("ERROR: file " + filename + " does not exist");

        lock.writeLock().lock();
        try {
            int blocksNeeded = (int) Math.ceil((double) contents.length / BLOCK_SIZE);
            List<Integer> freeBlocks = new ArrayList<>();
            for (int i = 0; i < freeBlockList.length; i++) {
                if (!freeBlockList[i]) freeBlocks.add(i);
                if (freeBlocks.size() == blocksNeeded) break;
            }
            if (freeBlocks.size() < blocksNeeded) {
                throw new Exception("ERROR: file too large");
            }

            for (FEntry entry : fentries) {
                if (entry.getFilename().equals(filename)) {
                    int prevNode = -1;
                    for (int i = 0; i < blocksNeeded; i++) {
                        int blockIndex = freeBlocks.get(i);
                        int nodeIndex = findFreeNode();
                        fnodes[nodeIndex] = new FNode(blockIndex);
                        if (prevNode != -1) {
                            fnodes[prevNode].setNext(nodeIndex);
                        } else {
                            entry.setFirstBlock((short) nodeIndex);
                        }
                        writeBlock(blockIndex, contents, i * BLOCK_SIZE);
                        freeBlockList[blockIndex] = true;
                        prevNode = nodeIndex;
                    }
                    entry.setFilesize((short) contents.length);
                    saveMetadata();
                    return;
                }
            }
            throw new Exception("ERROR: file " + filename + " does not exist");
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Reads a file's contents by following its block chain
    public byte[] readFile(String filename) throws Exception {
        ReentrantReadWriteLock lock = fileLocks.get(filename);
        if (lock == null) throw new Exception("ERROR: file " + filename + " does not exist");

        lock.readLock().lock();
        try {
            for (FEntry entry : fentries) {
                if (entry.getFilename().equals(filename)) {
                    byte[] data = new byte[entry.getFilesize()];
                    int block = entry.getFirstBlock();
                    int offset = 0;
                    while (block != -1) {
                        int blockIndex = fnodes[block].getBlockIndex();
                        disk.seek(blockIndex * BLOCK_SIZE);
                        int toRead = Math.min(BLOCK_SIZE, data.length - offset);
                        disk.read(data, offset, toRead);
                        offset += toRead;
                        block = fnodes[block].getNext();
                    }
                    return data;
                }
            }
            throw new Exception("ERROR: file " + filename + " does not exist");
        } finally {
            lock.readLock().unlock();
        }
    }

    // Lists all non-empty filenames
    public String[] listFiles() {
        metadataLock.readLock().lock();
        try {
            List<String> files = new ArrayList<>();
            for (FEntry entry : fentries) {
                if (!entry.getFilename().isEmpty()) {
                    files.add(entry.getFilename());
                }
            }
            return files.toArray(new String[0]);
        } finally {
            metadataLock.readLock().unlock();
        }
    }

    // Finds the index of a free FNode
    private int findFreeNode() throws Exception {
        for (int i = 0; i < fnodes.length; i++) {
            if (fnodes[i] == null || fnodes[i].getBlockIndex() < 0) {
                return i;
            }
        }
        throw new Exception("ERROR: no free nodes available");
    }

    // Writes a block of data to the specified block index on disk
    private void writeBlock(int blockIndex, byte[] data, int offset) throws Exception {
        disk.seek(blockIndex * BLOCK_SIZE);
        int length = Math.min(BLOCK_SIZE, data.length - offset);
        disk.write(data, offset, length);
    }

    // Overwrites a block with zeroed bytes (used during deletion)
    private void overwriteBlock(int blockIndex) throws Exception {
        disk.seek(blockIndex * BLOCK_SIZE);
        disk.write(new byte[BLOCK_SIZE]);
    }
}
