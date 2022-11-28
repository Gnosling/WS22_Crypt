package Util;

public class ContainerOfUTXO {

    private int index;
    private boolean isUnspent;

    public ContainerOfUTXO() {}

    public ContainerOfUTXO(int index, boolean isUnspent) {
        this.index = index;
        this.isUnspent = isUnspent;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean getIsUnspent() {
        return isUnspent;
    }

    public void setIsUnspent(boolean unspent) {
        this.isUnspent = unspent;
    }
}
