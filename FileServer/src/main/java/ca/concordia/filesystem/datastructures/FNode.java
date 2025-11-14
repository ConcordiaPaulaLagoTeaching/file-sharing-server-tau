package ca.concordia.filesystem.datastructures;

import java.nio.ByteBuffer;

public class FNode {

    private int blockIndex;
    private int next;

    public FNode(int blockIndex) {
        this.blockIndex = blockIndex;
        this.next = -1;
    }

    public FNode() {
        this.blockIndex = -1;
        this.next = -1;
    }

    
    public int getBlockIndex() {
        return blockIndex;
    }

    public int getNext() {
        return next;
    }

    
    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public void setNext(int next) {
        this.next = next;
    }

    
    public void reset() {
        this.blockIndex = -1;
        this.next = -1;
    }


    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(blockIndex);
        buffer.putInt(next);
        return buffer.array();
    }

    
    public static FNode fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int blockIndex = buffer.getInt();
        int next = buffer.getInt();
        return new FNode(blockIndex, next);
    }

    @Override
    public String toString() {
        return "FNode{" +
                "blockIndex=" + blockIndex +
                ", next=" + next +
                '}';
    }
}