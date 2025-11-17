package ca.concordia.filesystem.datastructures;

import java.nio.ByteBuffer;

/*
 * TODO: Review and comment code properly
 */

public class FEntry {

    private String filename;
    private short filesize;
    private short firstBlock;

    public FEntry(String filename, short filesize, short firstBlock) {
        setFilename(filename);
        setFilesize(filesize);
        this.firstBlock = firstBlock;
    }

    public FEntry() {
        this.filename = "";
        this.filesize = 0;
        this.firstBlock = -1;
    }

    // Getters
    public String getFilename() {
        return filename;
    }

    public short getFilesize() {
        return filesize;
    }

    public short getFirstBlock() {
        return firstBlock;
    }

    // Setters
    public void setFilename(String filename) {
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
    }

    public void setFilesize(short filesize) {
        if (filesize < 0) {
            throw new IllegalArgumentException("Filesize cannot be negative.");
        }
        this.filesize = filesize;
    }

    public void setFirstBlock(short firstBlock) {
        this.firstBlock = firstBlock;
    }

    
    public void reset() {
        this.filename = "";
        this.filesize = 0;
        this.firstBlock = -1;
    }

    
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(15);
        byte[] nameBytes = new byte[11];
        byte[] rawName = filename.getBytes();
        System.arraycopy(rawName, 0, nameBytes, 0, Math.min(rawName.length, 11));
        buffer.put(nameBytes);
        buffer.putShort(filesize);
        buffer.putShort(firstBlock);
        return buffer.array();
    }

    
    public static FEntry fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte[] nameBytes = new byte[11];
        buffer.get(nameBytes);
        String name = new String(nameBytes).trim();
        short size = buffer.getShort();
        short firstBlock = buffer.getShort();
        return new FEntry(name, size, firstBlock);
    }

    @Override
    public String toString() {
        return "FEntry{" +
                "filename='" + filename + '\'' +
                ", filesize=" + filesize +
                ", firstBlock=" + firstBlock +
                '}';
    }
}