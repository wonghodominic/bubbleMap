package me.nathanp.bubbledrop;

public class DrawerItem {
    private String mItemName;
    private int mImgResID;
    private float mDistance;

    public DrawerItem(String itemName, int imgResID) {
        mItemName = itemName;
        mImgResID = imgResID;
        mDistance = 0;
    }

    public String getItemName() {
        return mItemName;
    }

    public void setItemName(String itemName) {
        mItemName = itemName;
    }

    public int getImgResID() {
        return mImgResID;
    }

    public void setImgResID(int imgResID) {
        mImgResID = imgResID;
    }

    public void setDistance(float distance) {
        mDistance = distance;
    }

    public String getDistanceString() {
        return mDistance + "ft away";
    }
}
