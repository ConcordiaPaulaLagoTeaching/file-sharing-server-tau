package ca.concordia.filesystem.datastructures;

import java.nio.ByteBuffer;


public class FNode {

    private int blockIndex;
    private int next;

    // Default constructor
    public FNode() {
        this.blockIndex = -1;
        this.next = -1;
    }

    // One-argument constructor
    public FNode(int blockIndex) {
        this.blockIndex = blockIndex;
        this.next = -1;
    }

    // Two-argument constructor
    public FNode(int blockIndex, int next) {
        this.blockIndex = blockIndex;
        this.next = next;
    }

    // Getters
    public int getBlockIndex() {
        return blockIndex;
    }

    public int getNext() {
        return next;
    }

    // Setters
    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public void setNext(int next) {
        this.next = next;
    }

    // Reset node
    public void reset() {
        this.blockIndex = -1;
        this.next = -1;
    }

    // Serialize to bytes
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(blockIndex);
        buffer.putInt(next);
        return buffer.array();
    }

    // Deserialize from bytes
    public static FNode fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int blockIndex = buffer.getInt();
        int next = buffer.getInt();

        // Treat zeroed node as empty
        if (blockIndex == 0 && next == 0) {
            return new FNode(); // blockIndex = -1, next = -1
        }

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